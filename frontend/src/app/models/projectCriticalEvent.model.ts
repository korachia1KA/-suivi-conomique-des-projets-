export interface ProjectCriticalEvent {
  projectId: string;
  name: string;
  businessUnitId: string;
  marginPct: number;
  severity: 'CRITICAL';
}