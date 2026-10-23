export interface UserProfile {
  email: string; 
  roleName?: string | null;
  businessUnitId?: string | null;
  businessUnit?: { idBusinessUnit?: string | null } | null;
  role?: { name?: string | null } | null;

  firstName?: string | null;
  lastName?: string | null;
  fiscalCode?: string | null;
  idCardNumber?: string | null;

  birthDay?: string | null;   
  birthPlace?: string | null;

  address?: string | null;
  city?: string | null;

  phoneNumber?: string | null;

  iban?: string | null;
  ibanHolder?: string | null;
}