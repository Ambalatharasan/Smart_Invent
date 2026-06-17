package com.stockwise.api.controller;

import com.stockwise.api.dto.ManagerDtos.ManagerRequest;
import com.stockwise.api.dto.ManagerDtos.ManagerResponse;
import com.stockwise.api.service.ManagerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/managers")
public class ManagerController {
    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping
    public List<ManagerResponse> list(@RequestParam(required = false) String department) {
        return managerService.list(department);
    }

    @GetMapping("/{id}")
    public ManagerResponse get(@PathVariable Long id) {
        return managerService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ManagerResponse create(@Valid @RequestBody ManagerRequest request, Authentication authentication) {
        return managerService.create(request, actor(authentication));
    }

    @PutMapping("/{id}")
    public ManagerResponse update(@PathVariable Long id, @Valid @RequestBody ManagerRequest request, Authentication authentication) {
        return managerService.update(id, request, actor(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        managerService.delete(id, actor(authentication));
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "system" : authentication.getName();
    }
}
