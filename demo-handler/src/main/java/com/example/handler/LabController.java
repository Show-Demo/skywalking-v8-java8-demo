package com.example.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/lab")
public class LabController {
    private static final Logger log = LoggerFactory.getLogger(LabController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${QYWX_WEBHOOK_URL:}")
    private String qywxWebhookUrl;

    @PostMapping("/record")
    public Map<String, Object> record(
            @RequestParam(defaultValue = "unknown") String action,
            @RequestParam(defaultValue = "") String detail
    ) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("status", "ok");
        result.put("action", action);
        result.put("detail", detail);
        return result;
    }

    @PostMapping("/slow")
    public Map<String, Object> slow(@RequestParam(defaultValue = "500") long ms) throws InterruptedException {
        long sleepMs = Math.max(0, Math.min(ms, 8000));
        Thread.sleep(sleepMs);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("status", "ok");
        result.put("sleepMs", sleepMs);
        return result;
    }

    @PostMapping("/unstable")
    public Map<String, Object> unstable(@RequestParam(defaultValue = "0.2") double failRate) {
        if (failRate < 0 || failRate > 1) {
            throw new IllegalArgumentException("failRate must be between 0 and 1");
        }
        if (ThreadLocalRandom.current().nextDouble() < failRate) {
            throw new RuntimeException("simulated failure");
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("status", "ok");
        result.put("failRate", failRate);
        return result;
    }

    @PostMapping("/skywalking-alarm")
    public Map<String, Object> receiveAlarm(@RequestBody String payload) {
        log.warn("Received SkyWalking alarm: {}", payload);
        forwardToWeCom(payload);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("status", "received");
        return result;
    }

    private void forwardToWeCom(String payload) {
        if (qywxWebhookUrl == null || qywxWebhookUrl.trim().isEmpty()) {
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String content = "[SkyWalking告警]\n" + payload;
            Map<String, Object> text = new LinkedHashMap<String, Object>();
            text.put("content", content);
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("msgtype", "text");
            body.put("text", text);
            String jsonBody = OBJECT_MAPPER.writeValueAsString(body);
            ResponseEntity<String> resp = restTemplate.postForEntity(qywxWebhookUrl, new HttpEntity<String>(jsonBody, headers), String.class);
            log.info("Forwarded alarm to WeCom, status={}, body={}", resp.getStatusCodeValue(), resp.getBody());
        } catch (Exception e) {
            log.error("Failed to forward alarm to WeCom webhook", e);
        }
    }
}
