export interface BusinessUnitOption {
  id: string;
  name: string;
  currentAverageMargin?: number | null;
  expectedAnnualMargin?: number | null;
  employeeCount?: number | null;
  managerName?: string | null;
}