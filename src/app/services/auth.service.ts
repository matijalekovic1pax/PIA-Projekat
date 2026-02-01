import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private apiUrl = '/api/auth';
    private currentUserSubject = new BehaviorSubject<any>(null);
    public currentUser$ = this.currentUserSubject.asObservable();

    constructor(private http: HttpClient) {
        const storedUser = localStorage.getItem('user');
        if (storedUser) {
            this.currentUserSubject.next(JSON.parse(storedUser));
        }
    }

    register(formData: FormData): Observable<any> {
        return this.http.post(`${this.apiUrl}/register`, formData);
    }

    login(credentials: any): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/login`, credentials).pipe(
            tap(response => {
                if (response.token) {
                    localStorage.setItem('token', response.token);
                    localStorage.setItem('user', JSON.stringify(response.user));
                    this.currentUserSubject.next(response.user);
                }
            })
        );
    }

    requestPasswordReset(payload: { usernameOrEmail: string }): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/forgot-password`, payload);
    }

    resetPassword(payload: { token: string, newPassword: string }): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/reset-password`, payload);
    }

    logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        this.currentUserSubject.next(null);
    }

    getToken() {
        return localStorage.getItem('token');
    }

    getCurrentUser() {
        return this.currentUserSubject.value;
    }

    setCurrentUser(user: any) {
        localStorage.setItem('user', JSON.stringify(user));
        this.currentUserSubject.next(user);
    }
}
