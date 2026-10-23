export interface TopItem {
  id: string;
  label: string;
  value: number;
}

export interface RiskCounts {
  ok: number;
  warning: number;
  critical: number;
}

export interface BusinessUnitCard {
  id: string;
  name: string;
  totalProjects: number;
  totalEmployees: number;
  managerName: string | null;
  currentAverageMargin: number | null;
}

export interface EmployeeCard {
  name: string;
  email: string | null;
  role: string | null;
  seniorityLevel: string | null;
  businessUnit: string | null;
}

export interface DashboardResponse {
  userRole: string;
  businessUnitName: string | null;
  seniorityLevel: string | null;

  totalProjects: number;
  activeProjects: number;
  scheduledProjects: number;

  totalRevenue: number;
  totalCost: number;
  averageMargin: number;

  expectedRevenue: number;
  expectedCost: number;
  expectedMargin: number;

  topProjects: TopItem[];
  topBusinessUnits: TopItem[];

  projectRisk: RiskCounts;
  businessUnitRisk: RiskCounts;

  businessUnits: BusinessUnitCard[];
  employees: EmployeeCard[];        

  
  upcomingDeadlines?: DeadlineItem[];
  deadlineCounts?: DeadlineCounts;

  projects?: ProjectCard[];    

}

export interface DeadlineItem {
  id: string;
  label: string;
  date: string;   
  days: number;    
  status?: string | null;
}

export interface DeadlineCounts {
  overdue: number;
  due7: number;
  due30: number;
}

export interface ProjectCard {
  id: string;
  name: string;
  status?: string | null;
  businessUnitName?: string | null;
  businessUnit?: string | null;
  startDate?: string | null; 
  endDate?: string | null;  
  marginPct?: number | null;
}