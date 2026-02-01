import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({
    providedIn: 'root'
})
export class SpaceService {
    private apiUrl = '/api/spaces';

    constructor(private http: HttpClient, private authService: AuthService) { }

    getTopSpaces(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/top`);
    }

    getAllCities(): Observable<string[]> {
        return this.http.get<string[]>(`${this.apiUrl}/cities`);
    }

    getSpaceSummary(): Observable<any> {
        return this.http.get<any>(`${this.apiUrl}/summary`);
    }

    searchSpaces(name: string, cities: string[], type?: string, capacity?: string): Observable<any[]> {
        let params = new HttpParams();
        if (name) params = params.set('name', name);
        if (cities.length > 0) params = params.set('cities', cities.join(','));
        if (type) params = params.set('type', type);
        if (capacity) params = params.set('capacity', capacity);

        return this.http.get<any[]>(`${this.apiUrl}/search`, { params });
    }

    getSpaceDetails(id: string): Observable<any> {
        return this.http.get<any>(`${this.apiUrl}/${id}`);
    }

    getAvailability(id: string, date: string, type: string, startTime: string, endTime: string): Observable<any> {
        let params = new HttpParams()
            .set('date', date)
            .set('type', type)
            .set('startTime', startTime)
            .set('endTime', endTime);
        return this.http.get<any>(`${this.apiUrl}/${id}/availability`, { params });
    }

    getCalendarEvents(spaceId: string, type: string, itemId: string | null, start: string, end: string): Observable<any[]> {
        let params = new HttpParams()
            .set('type', type)
            .set('start', start)
            .set('end', end);

        if (itemId) {
            params = params.set('itemId', itemId);
        }

        return this.http.get<any[]>(`${this.apiUrl}/${spaceId}/calendar`, { params });
    }

    createReservation(data: any): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.post<any>(`${this.apiUrl}/reserve`, data, { headers });
    }

    addReview(data: any): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.post<any>(`${this.apiUrl}/review`, data, { headers });
    }

    getManagerSpaces(): Observable<any[]> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.get<any[]>(`${this.apiUrl}/manager/my-spaces`, { headers });
    }

    getManagerSpace(id: string): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.get<any>(`${this.apiUrl}/manager/spaces/${id}`, { headers });
    }

    getManagerReservations(): Observable<any[]> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.get<any[]>(`${this.apiUrl}/manager/reservations`, { headers });
    }

    confirmReservation(id: string): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.put<any>(`${this.apiUrl}/manager/reservations/${id}/confirm`, {}, { headers });
    }

    checkoutReservation(id: string): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.put<any>(`${this.apiUrl}/manager/reservations/${id}/checkout`, {}, { headers });
    }

    rescheduleReservation(id: string, startDateTime: string, endDateTime: string): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.put<any>(`${this.apiUrl}/manager/reservations/${id}/reschedule`, { startDateTime, endDateTime }, { headers });
    }

    createSpace(data: FormData): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.post<any>(`${this.apiUrl}/manager/spaces`, data, { headers });
    }

    updateSpace(id: string, data: FormData): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.put<any>(`${this.apiUrl}/manager/spaces/${id}`, data, { headers });
    }

    deleteSpace(id: string): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        return this.http.delete<any>(`${this.apiUrl}/manager/spaces/${id}`, { headers });
    }

    importSpaces(file: File): Observable<any> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        const formData = new FormData();
        formData.append('file', file);
        return this.http.post<any>(`${this.apiUrl}/manager/import`, formData, { headers });
    }

    downloadManagerReport(month: string): Observable<Blob> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        const params = new HttpParams().set('month', month);
        return this.http.get(`${this.apiUrl}/manager/report`, { headers, params, responseType: 'blob' });
    }

    getManagerCalendarEvents(spaceId: string, elementType: string, elementName: string | null, start: string, end: string): Observable<any[]> {
        const headers = { Authorization: `Bearer ${this.authService.getToken()}` };
        let params = new HttpParams()
            .set('spaceId', spaceId)
            .set('elementType', elementType)
            .set('start', start)
            .set('end', end);

        if (elementName) {
            params = params.set('elementName', elementName);
        }

        return this.http.get<any[]>(`${this.apiUrl}/manager/calendar`, { headers, params });
    }
}
