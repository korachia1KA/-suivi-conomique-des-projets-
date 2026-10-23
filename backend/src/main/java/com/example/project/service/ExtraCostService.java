package com.example.project.service;

import com.example.project.DTO.project.CreateExtraCostRequest;
import com.example.project.DTO.project.ExtraCostDto;

import java.util.List;

public interface ExtraCostService {
    ExtraCostDto create(String idProject, CreateExtraCostRequest req);
    List<ExtraCostDto> listByProject(String idProject);
}