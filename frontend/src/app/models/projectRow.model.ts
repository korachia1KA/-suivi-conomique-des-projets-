export interface ProjectRow {
  idProject: string;
  name: string;
  statusName: string;
  businessUnitName: string;
  startDate: string | null;
  endDate: string | null;
  currentRevenue: number | null;
  currentCost: number | null;
  currentMargin: number | null;
}