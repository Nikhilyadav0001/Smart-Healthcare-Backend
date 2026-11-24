import os
import time
import pytest
import requests
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from webdriver_manager.chrome import ChromeDriverManager

BASE_URL = os.environ.get("BASE_URL", "http://localhost:5173")
GATEWAY_URL = os.environ.get("GATEWAY_URL", "http://localhost:8080")
STANDARD_API_KEY = "smarthealth-dev-secret-api-key-2026"
ADMIN_API_KEY = "smarthealth-admin-secret-key-2026"

@pytest.fixture(scope="session")
def driver():
    chrome_options = Options()
    chrome_options.add_argument("--headless=new")
    chrome_options.add_argument("--no-sandbox")
    chrome_options.add_argument("--disable-dev-shm-usage")
    chrome_options.add_argument("--disable-gpu")
    chrome_options.add_argument("--window-size=1920,1080")
    chrome_options.add_argument("--disable-blink-features=AutomationControlled")

    chrome_driver_path = ChromeDriverManager().install()
    if "THIRD_PARTY_NOTICES" in chrome_driver_path or not chrome_driver_path.endswith(".exe"):
        driver_dir = os.path.dirname(chrome_driver_path)
        potential_exe = os.path.join(driver_dir, "chromedriver.exe")
        if os.path.exists(potential_exe):
            chrome_driver_path = potential_exe

    service = Service(executable_path=chrome_driver_path)
    driver_instance = webdriver.Chrome(service=service, options=chrome_options)
    driver_instance.implicitly_wait(10)
    yield driver_instance
    driver_instance.quit()

def test_01_landing_page_loads(driver):
    """Test 1: Smart Healthcare landing page loads and brand is visible."""
    driver.get(BASE_URL)
    WebDriverWait(driver, 15).until(
        lambda d: "Smart Healthcare" in d.title or "smart.health" in d.page_source
    )
    assert "smart" in driver.page_source.lower()

def test_02_auth_swagger_single_api_key_endpoint():
    """Test 2: Single unified API Key endpoint (/api/auth/demo/api-key) validates both standard and admin keys via clean JSON."""
    endpoint = f"{GATEWAY_URL}/api/auth/demo/api-key"

    # 1. Test GET with Header (Standard Key)
    resp_get = requests.get(endpoint, headers={"X-API-KEY": STANDARD_API_KEY})
    assert resp_get.status_code == 200, f"Expected 200 for valid standard key, got {resp_get.status_code}: {resp_get.text}"
    data_get = resp_get.json()
    assert data_get.get("status") == "SUCCESS"
    assert data_get.get("valid") is True
    assert "ROLE_API_CLIENT" in data_get.get("roles", [])

    # 2. Test POST with JSON Body (Admin Key)
    resp_post_admin = requests.post(
        endpoint,
        headers={"Content-Type": "application/json"},
        json={"apiKey": ADMIN_API_KEY}
    )
    assert resp_post_admin.status_code == 200, f"Expected 200 for valid admin key, got {resp_post_admin.status_code}: {resp_post_admin.text}"
    data_post_admin = resp_post_admin.json()
    assert data_post_admin.get("status") == "SUCCESS"
    assert data_post_admin.get("valid") is True
    assert data_post_admin.get("keyType") == "Admin API Key"
    assert "ROLE_ADMIN" in data_post_admin.get("roles", [])

    # 3. Test POST with Invalid Key returns 401 with JSON
    resp_post_invalid = requests.post(
        endpoint,
        headers={"Content-Type": "application/json"},
        json={"apiKey": "unregistered-dummy-key"}
    )
    assert resp_post_invalid.status_code == 401
    data_post_invalid = resp_post_invalid.json()
    assert data_post_invalid.get("status") == "FAILED"
    assert data_post_invalid.get("valid") is False
    assert "error" in data_post_invalid

def test_03_admin_user_role_management_api():
    """Test 3: Admin User & Role Management API lists registered users and allows role modifications."""
    admin_users_url = f"{GATEWAY_URL}/api/auth/admin/users"

    # List all users
    resp = requests.get(admin_users_url, headers={"X-API-KEY": ADMIN_API_KEY})
    assert resp.status_code == 200, f"Failed to fetch users: {resp.status_code}"
    users = resp.json()
    if isinstance(users, dict) and "value" in users:
        users = users["value"]
    assert len(users) >= 1, "Expected at least 1 registered user (admin)"

    admin_user = next((u for u in users if u.get("email") == "admin@smarthealth.internal"), None)
    assert admin_user is not None, "admin@smarthealth.internal user not found in admin directory"
    assert "ADMIN" in admin_user.get("roles", [])

    # Find non-admin user or test updating role
    target_user = next((u for u in users if u.get("email") != "admin@smarthealth.internal"), None)
    if target_user:
        user_id = target_user["id"]
        update_url = f"{admin_users_url}/{user_id}/roles"

        # Update role to DOCTOR
        put_resp = requests.put(
            update_url,
            headers={"Content-Type": "application/json", "X-API-KEY": ADMIN_API_KEY},
            json={"roles": ["DOCTOR"]}
        )
        assert put_resp.status_code == 200, f"Role update failed: {put_resp.text}"
        data = put_resp.json()
        assert "DOCTOR" in data.get("roles", [])

        # Revert back to PATIENT / USER
        revert_resp = requests.put(
            update_url,
            headers={"Content-Type": "application/json", "X-API-KEY": ADMIN_API_KEY},
            json={"roles": ["PATIENT"]}
        )
        assert revert_resp.status_code == 200
        data_revert = revert_resp.json()
        assert "PATIENT" in data_revert.get("roles", [])

def test_04_admin_login_ui_and_roles_page(driver):
    """Test 4: Admin login in Frontend UI, navigates to User Roles & Access management page."""
    driver.get(BASE_URL)
    time.sleep(2)

    # Click 'Sign In' on the landing page
    portal_btn = WebDriverWait(driver, 15).until(
        EC.presence_of_element_located((By.XPATH, "//button[contains(., 'Sign In')]"))
    )
    driver.execute_script("arguments[0].click();", portal_btn)

    # Wait for login form text inputs (username / email)
    username_input = WebDriverWait(driver, 10).until(
        EC.visibility_of_element_located((By.XPATH, "//input[@placeholder='e.g. maya.shah@example.com' or @type='text']"))
    )
    password_input = driver.find_element(By.XPATH, "//input[@type='password']")

    username_input.clear()
    username_input.send_keys("admin@smarthealth.internal")
    password_input.clear()
    password_input.send_keys("admin123")

    submit_btn = driver.find_element(By.XPATH, "//button[@type='submit']")
    driver.execute_script("arguments[0].click();", submit_btn)
    time.sleep(2)

    # Wait for sidebar navigation to load
    nav_btns = WebDriverWait(driver, 15).until(
        EC.presence_of_all_elements_located((By.XPATH, "//button[contains(@class, 'nav-item')]"))
    )
    roles_btn = next((b for b in nav_btns if "User Roles" in b.text), None)
    assert roles_btn is not None, "User Roles & Access navigation item not found in admin sidebar"
    driver.execute_script("arguments[0].click();", roles_btn)
    time.sleep(2)

    # Verify User Roles management table loaded with rows
    table = WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.TAG_NAME, "table"))
    )
    assert table is not None
    rows = driver.find_elements(By.TAG_NAME, "tr")
    assert len(rows) >= 2, f"Expected at least 2 table rows, got {len(rows)}"
    assert "admin@smarthealth.internal" in driver.page_source
