@echo off
echo ===================================================
echo Running Smart Healthcare Selenium E2E Tests
echo ===================================================
pytest tests/selenium/test_e2e_smarthealth.py -v -s
pause
