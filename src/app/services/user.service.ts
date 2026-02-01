import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({
    providedIn: 'root'
})
export class UserService {
    private apiUrl = '/api/users';

    constructor(private http: HttpClient, private authService: AuthService) { }

    getProfile(): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.get<any>(`${this.apiUrl}/profile`, { headers });
    }

    updateProfile(formData: FormData): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.put<any>(`${this.apiUrl}/profile`, formData, { headers });
    }

    cancelReservation(id: string): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.put<any>(`${this.apiUrl}/reservations/${id}/cancel`, {}, { headers });
    }
}
