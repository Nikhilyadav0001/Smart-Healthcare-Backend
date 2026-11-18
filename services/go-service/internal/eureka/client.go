package eureka

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"net"
	"net/http"
	"strings"
	"time"
)

type Client struct {
	serverURL  string
	app        string
	instanceID string
	hostName   string
	ipAddr     string
	port       int
	httpClient *http.Client
	stopChan   chan struct{}
}

func getLocalIP() string {
	addrs, err := net.InterfaceAddrs()
	if err == nil {
		for _, addr := range addrs {
			if ipNet, ok := addr.(*net.IPNet); ok && !ipNet.IP.IsLoopback() {
				if ipNet.IP.To4() != nil {
					return ipNet.IP.String()
				}
			}
		}
	}
	return "go-service"
}

func NewClient(serverURL, app, hostName, ipAddr string, port int) *Client {
	url := strings.TrimRight(serverURL, "/")

	if ipAddr == "" {
		ipAddr = getLocalIP()
	}
	if hostName == "" {
		hostName = ipAddr
	}

	instanceID := fmt.Sprintf("%s:%s:%d", hostName, strings.ToLower(app), port)

	return &Client{
		serverURL:  url,
		app:        strings.ToUpper(app),
		instanceID: instanceID,
		hostName:   hostName,
		ipAddr:     ipAddr,
		port:       port,
		httpClient: &http.Client{Timeout: 5 * time.Second},
		stopChan:   make(chan struct{}),
	}
}

func (c *Client) buildRegistrationPayload() ([]byte, error) {
	homePageURL := fmt.Sprintf("http://%s:%d/", c.hostName, c.port)
	statusPageURL := fmt.Sprintf("http://%s:%d/actuator/info", c.hostName, c.port)
	healthCheckURL := fmt.Sprintf("http://%s:%d/actuator/health", c.hostName, c.port)

	payload := map[string]any{
		"instance": map[string]any{
			"instanceId":       c.instanceID,
			"hostName":         c.hostName,
			"app":              c.app,
			"ipAddr":           c.ipAddr,
			"status":           "UP",
			"overriddenStatus": "UNKNOWN",
			"port": map[string]any{
				"$":        c.port,
				"@enabled": "true",
			},
			"securePort": map[string]any{
				"$":        443,
				"@enabled": "false",
			},
			"countryId": 1,
			"dataCenterInfo": map[string]any{
				"@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
				"name":   "MyOwn",
			},
			"leaseInfo": map[string]any{
				"renewalIntervalInSecs": 30,
				"durationInSecs":        90,
			},
			"metadata": map[string]string{
				"management.port": fmt.Sprintf("%d", c.port),
			},
			"homePageUrl":      homePageURL,
			"statusPageUrl":    statusPageURL,
			"healthCheckUrl":   healthCheckURL,
			"vipAddress":       strings.ToLower(c.app),
			"secureVipAddress": strings.ToLower(c.app),
		},
	}
	return json.Marshal(payload)
}

func (c *Client) Register(ctx context.Context) error {
	data, err := c.buildRegistrationPayload()
	if err != nil {
		return err
	}
	reqURL := fmt.Sprintf("%s/apps/%s", c.serverURL, c.app)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, reqURL, bytes.NewReader(data))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusNoContent && resp.StatusCode != http.StatusOK {
		return fmt.Errorf("unexpected status from eureka register: %d", resp.StatusCode)
	}
	return nil
}

func (c *Client) SendHeartbeat(ctx context.Context) error {
	reqURL := fmt.Sprintf("%s/apps/%s/%s", c.serverURL, c.app, c.instanceID)
	req, err := http.NewRequestWithContext(ctx, http.MethodPut, reqURL, nil)
	if err != nil {
		return err
	}
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode == http.StatusNotFound {
		return c.Register(ctx)
	}
	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusNoContent {
		return fmt.Errorf("unexpected status from eureka heartbeat: %d", resp.StatusCode)
	}
	return nil
}

func (c *Client) Deregister(ctx context.Context) error {
	reqURL := fmt.Sprintf("%s/apps/%s/%s", c.serverURL, c.app, c.instanceID)
	req, err := http.NewRequestWithContext(ctx, http.MethodDelete, reqURL, nil)
	if err != nil {
		return err
	}
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	return nil
}

func (c *Client) Start(ctx context.Context) {
	go func() {
		// Initial registration retry loop
		for {
			select {
			case <-ctx.Done():
				return
			case <-c.stopChan:
				return
			default:
			}

			regCtx, regCancel := context.WithTimeout(context.Background(), 5*time.Second)
			err := c.Register(regCtx)
			regCancel()
			if err != nil {
				slog.Warn("Failed to register with Eureka, retrying in 5s...", "error", err)
				time.Sleep(5 * time.Second)
			} else {
				slog.Info("Successfully registered with Eureka discovery server", "app", c.app, "instanceId", c.instanceID)
				break
			}
		}

		ticker := time.NewTicker(25 * time.Second)
		defer ticker.Stop()

		for {
			select {
			case <-ctx.Done():
				return
			case <-c.stopChan:
				return
			case <-ticker.C:
				hbCtx, hbCancel := context.WithTimeout(context.Background(), 5*time.Second)
				if err := c.SendHeartbeat(hbCtx); err != nil {
					slog.Warn("Eureka heartbeat failed", "error", err)
				}
				hbCancel()
			}
		}
	}()
}

func (c *Client) Stop() {
	close(c.stopChan)
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()
	if err := c.Deregister(ctx); err != nil {
		slog.Warn("Failed to deregister from Eureka", "error", err)
	} else {
		slog.Info("Deregistered from Eureka cleanly")
	}
}
