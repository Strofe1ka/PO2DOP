package com.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ApiController {

    private static final Map<String, String> USERS = Map.of(
            "1", "Alice",
            "2", "Bob",
            "3", "Charlie"
    );

    @GetMapping("/")
    public String index() {
        return "<h1>Demo App for Fuzz Testing</h1><p>Endpoints: /api/users, /api/search, /api/login</p>";
    }

    @GetMapping("/api/users")
    public Map<String, String> getUsers(@RequestParam(defaultValue = "1") String id) {
        return Map.of("user", USERS.getOrDefault(id, "Unknown"));
    }

    @GetMapping("/api/search")
    public Map<String, Object> searchGet(@RequestParam(required = false) String q) {
        String query = q != null ? q : "";
        return Map.of("results", new String[]{"Result for: " + query});
    }

    @PostMapping("/api/search")
    public Map<String, Object> searchPost(@RequestBody(required = false) Map<String, String> body) {
        String query = body != null && body.containsKey("query") ? body.get("query") : "";
        return Map.of("results", new String[]{"Result for: " + query});
    }

    @PostMapping("/api/login")
    public ResponseEntity<?> login(@RequestBody(required = false) Map<String, String> body) {
        if (body == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        if ("admin".equals(username) && "secret".equals(password)) {
            return ResponseEntity.ok(Map.of("token", "fake-jwt-token", "status", "ok"));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
