export interface Project {
  id_project: string;
  name: string;
  start_date: string;
  planned_end_date: string;
  end_date?: string;
  estimated_revenue: number;
  estimated_cost: number;
  id_business_unit: string;
  id_status: string;
}

export interface CreateProjectPayload {
  name: string;
  businessUnitId?: string | null;
  startDate: string;
  plannedEndDate: string;
  estimatedRevenue: number;
  estimatedCost: number;

  users?: Array<{
    userId: string;
    hourlyCost: number;
  }>;
}