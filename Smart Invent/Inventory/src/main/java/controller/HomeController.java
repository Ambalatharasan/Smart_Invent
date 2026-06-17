package com.stockwise.api.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HomeController {
    @GetMapping(value = "/api/home", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Smart Invent API</title>
                    <style>
                        body { margin: 0; font-family: Arial, sans-serif; background: #f3f6f8; color: #10202c; }
                        main { max-width: 960px; margin: 0 auto; padding: 48px 24px; }
                        .panel { background: #fff; border: 1px solid #d7e0e7; border-radius: 8px; padding: 28px; box-shadow: 0 12px 30px rgba(15, 35, 50, .08); }
                        h1 { margin: 0 0 8px; font-size: 32px; }
                        p { color: #516273; line-height: 1.6; }
                        code { background: #edf2f5; border-radius: 4px; padding: 3px 6px; }
                        .grid { display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); margin-top: 24px; }
                        a { display: block; color: #006b78; text-decoration: none; font-weight: 700; border: 1px solid #cbd8df; border-radius: 6px; padding: 14px; background: #fbfdfe; }
                        a:hover { border-color: #008c9b; }
                    </style>
                </head>
                <body>
                    <main>
                        <section class="panel">
                            <h1>Smart Invent Backend API</h1>
                            <p>The Spring Boot backend is running. API endpoints are secured, so use <code>/api/auth/login</code> first and pass the JWT token as a Bearer token.</p>
                            <div class="grid">
                                <a href="/">Open Dashboard</a>
                                <a href="/api/auth/login">Browser Login</a>
                                <a href="/api/health">Health Check</a>
                                <a href="/api">API Endpoint List</a>
                            </div>
                        </section>
                    </main>
                </body>
                </html>
                """;
    }

    @GetMapping("/api")
    public Map<String, Object> api() {
        return Map.of(
                "name", "Smart Invent Inventory Manager API",
                "status", "running",
                "login", "POST /api/auth/login",
                "securedEndpoints", Map.of(
                        "inventory", "GET /api/inventory",
                        "restock", "GET /api/restock/summary",
                        "managers", "GET /api/managers",
                        "activity", "GET /api/activity",
                        "currentUser", "GET /api/users/me",
                        "currentProfile", "GET /api/users/me/profile",
                        "loginHistory", "GET /api/users/login-events"
                )
        );
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "smart-invent-backend",
                "timestamp", Instant.now().toString()
        );
    }
}
