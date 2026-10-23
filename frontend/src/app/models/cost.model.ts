export interface CostLine {
  idProject: string;
  projectName: string;
  businessUnitId: string | null;
  businessUnitName: string | null;
  statusName: string | null;

  startDate: string | null;
  plannedEndDate: string | null;

  estimatedRevenue: number | null;
  laborCost: number | null;
  extraCost: number | null;
  totalCost: number | null;
  marginPct: number | null;
}