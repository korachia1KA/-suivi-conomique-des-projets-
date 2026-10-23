export interface AdminUser {
  idUser: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  roleName: string | null;
  businessUnitId: string | null;
  businessUnitName: string | null;
  seniorityLevelName: string | null;
  enabled: boolean;
}