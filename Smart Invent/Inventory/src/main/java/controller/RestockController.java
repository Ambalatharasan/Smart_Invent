package com.stockwise.api.controller;

import com.stockwise.api.dto.RestockDtos.CreateRestockOrderRequest;
import com.stockwise.api.dto.RestockDtos.RestockCsvImportResponse;
import com.stockwise.api.dto.RestockDtos.RestockCandidateResponse;
import com.stockwise.api.dto.RestockDtos.RestockOrderResponse;
import com.stockwise.api.dto.RestockDtos.RestockSummaryResponse;
import com.stockwise.api.dto.RestockDtos.UpdateRestockStatusRequest;
import com.stockwise.api.service.RestockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/restock")
public class RestockController {
    private final RestockService restockService;

    public RestockController(RestockService restockService) {
        this.restockService = restockService;
    }

    @GetMapping("/summary")
    public RestockSummaryResponse summary() {
        return restockService.summary();
    }

    @GetMapping("/candidates")
    public List<RestockCandidateResponse> candidates() {
        return restockService.candidates();
    }

    @GetMapping("/orders")
    public List<RestockOrderResponse> orders(@RequestParam(required = false) String status) {
        return restockService.orders(status);
    }

    @GetMapping(value = "/orders.csv", produces = "text/csv")
    public ResponseEntity<String> exportOrdersCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=smart-invent-restock-orders.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(restockService.exportOrdersCsv());
    }

    @GetMapping(value = "/orders/template.csv", produces = "text/csv")
    public ResponseEntity<String> importTemplateCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=smart-invent-restock-upload-template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(restockService.importTemplateCsv());
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public RestockOrderResponse createOrder(@Valid @RequestBody CreateRestockOrderRequest request, Authentication authentication) {
        return restockService.createOrder(request, actor(authentication));
    }

    @PostMapping(value = "/orders/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RestockCsvImportResponse importOrders(@RequestParam("file") MultipartFile file, Authentication authentication) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a CSV file to upload");
        }
        try {
            return restockService.importOrdersCsv(new String(file.getBytes(), StandardCharsets.UTF_8), actor(authentication));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not read uploaded CSV file");
        }
    }

    @PatchMapping("/orders/{id}/status")
    public RestockOrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateRestockStatusRequest request, Authentication authentication) {
        return restockService.updateStatus(id, request, actor(authentication));
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "system" : authentication.getName();
    }
}
