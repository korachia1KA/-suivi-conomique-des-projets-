export interface TimesheetEntry {
  idUser: string;
  fullName: string | null;
  idProject: string;
  projectName: string;
  date: string;            // ISO yyyy-mm-dd (day 1 of the month)
  hoursWorked: number | null;
  hourlyCost: number | null;
  cost: number | null;
}