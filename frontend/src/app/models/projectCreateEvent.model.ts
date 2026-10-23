export interface ProjectCreatedEvent {
  projectId: string;
  name: string;
  businessUnitId: string;
  startDate: string;
  plannedEndDate: string;
}