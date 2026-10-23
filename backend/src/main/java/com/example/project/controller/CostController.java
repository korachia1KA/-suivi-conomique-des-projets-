package com.example.project.controller;

import com.example.project.DTO.costs.CostLineDto;
import com.example.project.service.CostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/costs")
@RequiredArgsConstructor
public class CostController {

    private final CostService costService;

    @GetMapping
    public ResponseEntity<List<CostLineDto>> listCosts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String buId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(costService.listCosts(q, buId, status));
    }
}