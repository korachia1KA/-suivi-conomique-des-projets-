package com.example.project.repository.projections;

import java.math.BigDecimal;

/**
 * Projection interface for aggregating labor costs by project.
 * This interface is used to retrieve specific fields related to project labor costs
 * without loading entire entities, improving performance for read operations.
 */

public interface ProjectLaborCostAgg {

    String getIdProject();
    BigDecimal getLaborCost();
}