package com.example.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final JdbcTemplate jdbcTemplate;

    public OrderController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Value("${handler.base-url:http://127.0.0.1:8080}")
    private String handlerBaseUrl;

    @PostMapping("/create")
    public Map<String, Object> create(
            @RequestParam String orderNo,
            @RequestParam(defaultValue = "0") long slowMs,
            @RequestParam(defaultValue = "0") double failRate
    ) {
        postForm(handlerBaseUrl + "/api/lab/record", "action=create-order&detail=" + orderNo);
        if (slowMs > 0) {
            restTemplate.postForObject(handlerBaseUrl + "/api/lab/slow?ms=" + slowMs, null, String.class);
        }
        if (failRate > 0) {
            try {
                restTemplate.postForObject(handlerBaseUrl + "/api/lab/unstable?failRate=" + failRate, null, String.class);
            } catch (Exception ignored) {
                // expected in error-rate test
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        jdbcTemplate.update(
                "INSERT INTO orders(order_no, slow_ms, fail_rate, created_at) VALUES (?, ?, ?, NOW())",
                orderNo,
                slowMs,
                failRate
        );
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM orders", Integer.class);
        result.put("status", "ok");
        result.put("orderNo", orderNo);
        result.put("handler", handlerBaseUrl);
        result.put("totalInDb", total == null ? 0 : total);
        return result;
    }

    private void postForm(String url, String formBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        restTemplate.postForEntity(url, new HttpEntity<String>(formBody, headers), String.class);
    }
}
