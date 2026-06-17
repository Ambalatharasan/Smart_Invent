package com.stockwise.api.controller;

import com.stockwise.api.dto.InventoryDtos.InventoryCsvImportResponse;
import com.stockwise.api.dto.InventoryDtos.InventoryRequest;
import com.stockwise.api.dto.InventoryDtos.InventoryResponse;
import com.stockwise.api.dto.InventoryDtos.StockAdjustmentRequest;
import com.stockwise.api.entity.StockStatus;
import com.stockwise.api.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) StockStatus status
    ) {
        return inventoryService.list(search, category, status);
    }

    @GetMapping("/{id}")
    public InventoryResponse get(@PathVariable Long id) {
        return inventoryService.get(id);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return inventoryService.categories();
    }

    @GetMapping(value = "/template.csv", produces = "text/csv")
    public ResponseEntity<String> templateCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=smart-invent-inventory-template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(inventoryService.inventoryTemplateCsv());
    }

    @GetMapping(value = "/items.csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=smart-invent-inventory-items.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(inventoryService.exportItemsCsv());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponse create(@Valid @RequestBody InventoryRequest request, Authentication authentication) {
        return inventoryService.create(request, actor(authentication));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InventoryCsvImportResponse importItems(@RequestPart("file") MultipartFile file, Authentication authentication) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose an inventory CSV or Excel template file to upload");
        }
        try {
            return inventoryService.importItems(file.getBytes(), file.getOriginalFilename(), actor(authentication));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not read uploaded inventory file");
        }
    }

    @PutMapping("/{id}")
    public InventoryResponse update(@PathVariable Long id, @Valid @RequestBody InventoryRequest request, Authentication authentication) {
        return inventoryService.update(id, request, actor(authentication));
    }

    @PatchMapping("/{id}/stock")
    public InventoryResponse adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustmentRequest request, Authentication authentication) {
        return inventoryService.adjustStock(id, request, actor(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        inventoryService.delete(id, actor(authentication));
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "system" : authentication.getName();
    }
}
