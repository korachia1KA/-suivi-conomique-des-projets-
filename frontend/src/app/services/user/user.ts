import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UpdatePersonalPayload  } from '../../models/updatePersonal.model';
import { UpdateContactsPayload } from '../../models/updateContacts.model';
import { UpdateBankPayload } from '../../models/updateBank.model';
import { UpdateResidencePayload } from '../../models/updateResidence.model';
import { UpdatePasswordPayload } from '../../models/updatePassword.model';
import { UserProfile } from '../../models/userProfile.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiBaseUrl}/user`;

  currentUser = signal<UserProfile | null>(null);

  loadProfile() {
    this.getMe().subscribe(profile => this.currentUser.set(profile));
  }

  updateLocalName(newName: string) {
    const current = this.currentUser();
    if (current) {
      this.currentUser.set({ ...current, firstName: newName });
    }
  }

  getMe(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.API_URL}/profile`);
  }

  updatePersonal(payload: UpdatePersonalPayload) {
    return this.http.put<void>(`${this.API_URL}/profile/personal`, payload)
  }

  updateContacts(payload: UpdateContactsPayload) {
    return this.http.put<void>(`${this.API_URL}/profile/contacts`, payload);
  }

  updateBank(payload: UpdateBankPayload) {
    return this.http.put<void>(`${this.API_URL}/profile/bank`, payload);
  }

  updateResidence(payload: UpdateResidencePayload) {
    return this.http.put<void>(`${this.API_URL}/profile/residence`, payload);
  }

  updatePassword(payload: UpdatePasswordPayload) {
    return this.http.post<void>(`${this.API_URL}/profile/change-password`, payload);
  }
}