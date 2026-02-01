import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({
    providedIn: 'root'
})
export class AdminService {
    private apiUrl = '/api/admin';

    constructor(private http: HttpClient, private authService: AuthService) { }

    getUsers(status?: string): Observable<any[]> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        const params: any = status ? { status } : {};
        return this.http.get<any[]>(`${this.apiUrl}/users`, { headers, params });
    }

    updateUser(id: string, payload: any): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.put<any>(`${this.apiUrl}/users/${id}`, payload, { headers });
    }

    updateUserStatus(id: string, status: string): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.put<any>(`${this.apiUrl}/users/${id}/status`, { status }, { headers });
    }

    deleteUser(id: string): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.delete<any>(`${this.apiUrl}/users/${id}`, { headers });
    }

    getSpaces(status?: string): Observable<any[]> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        const params: any = status ? { status } : {};
        return this.http.get<any[]>(`${this.apiUrl}/spaces`, { headers, params });
    }

    updateSpaceStatus(id: string, status: string): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.put<any>(`${this.apiUrl}/spaces/${id}/status`, { status }, { headers });
    }

    getStats(): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.get<any>(`${this.apiUrl}/stats`, { headers });
    }
}
