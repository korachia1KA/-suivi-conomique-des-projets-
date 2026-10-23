export interface ProjectUpdatedEvent {
  projectId: string;
  name: string;
  businessUnitId: string | null;
  currentMargin: number | null;
  changeKind: string;   // HOURS_ADDED, HOURS_REMOVED, EXTRA_COST_ADDED, ...
}