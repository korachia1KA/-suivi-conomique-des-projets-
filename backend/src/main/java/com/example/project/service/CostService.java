package com.example.project.service;

import com.example.project.DTO.costs.CostLineDto;

import java.util.List;

public interface CostService {
    List<CostLineDto> listCosts(String q, String buId, String status);
}