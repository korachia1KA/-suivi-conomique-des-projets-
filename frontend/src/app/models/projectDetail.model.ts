export interface ExtraCost {
  idExtraCost: string;
  description: string | null;
  cost: number;
  startDate: string | null;
  endDate: string | null;
  idProject: string;
}

export interface ProjectUserCost {
  idUser: string;
  fullName: string;
  role: string | null;
  seniorityLevel: string | null;
  hourlyCost: number | null;
  hoursWorked: number | null;
}

export interface ProjectDetail {
  idProject: string;
  name: string;
  statusName: string | null;
  businessUnitId: string | null;
  businessUnitName: string | null;
  startDate: string | null;
  plannedEndDate: string | null;
  endDate: string | null;
  estimatedRevenue: number | null;
  estimatedCost: number | null;
  currentRevenue: number | null;
  currentCost: number | null;
  currentMargin: number | null;
  critical: boolean;
  extraCosts: ExtraCost[];
  users: ProjectUserCost[];
}