import { Component, OnInit } from '@angular/core';
import { SpaceService } from '../../services/space.service';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CalendarComponent } from '../calendar/calendar';

@Component({
  selector: 'app-manager-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, CalendarComponent],
  templateUrl: './manager-dashboard.html'
})
export class ManagerDashboardComponent implements OnInit {
  spaces: any[] = [];
  reservations: any[] = [];
  activeTab: 'spaces' | 'reservations' | 'calendar' | 'import' = 'spaces';
  loading = false;
  importFile: File | null = null;
  importMessage = '';
  reportMonth: string = new Date().toISOString().slice(0, 7);

  constructor(private spaceService: SpaceService, private router: Router) { }

  ngOnInit() {
    this.loadSpaces();
    this.loadReservations();
  }

  loadSpaces() {
    this.loading = true;
    this.spaceService.getManagerSpaces().subscribe({
      next: (data) => {
        this.spaces = data;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  loadReservations() {
    this.spaceService.getManagerReservations().subscribe({
      next: (data) => this.reservations = data,
      error: (err) => console.error(err)
    });
  }

  canManageAttendance(reservation: any): boolean {
    const start = new Date(reservation.startDateTime).getTime();
    const now = new Date().getTime();
    return now >= start + 10 * 60 * 1000;
  }

  confirmReservation(id: string) {
    this.spaceService.confirmReservation(id).subscribe({
      next: () => this.loadReservations(),
      error: (err) => alert(err.error?.message || 'Confirm failed')
    });
  }

  checkoutReservation(id: string) {
    this.spaceService.checkoutReservation(id).subscribe({
      next: () => this.loadReservations(),
      error: (err) => alert(err.error?.message || 'Check-out failed')
    });
  }

  deleteSpace(id: string) {
    if (confirm('Are you sure you want to delete this space?')) {
      this.spaceService.deleteSpace(id).subscribe({
        next: () => {
          this.loadSpaces();
          alert('Space deleted');
        },
        error: (err) => alert(err.error?.message || 'Delete failed')
      });
    }
  }

  editSpace(id: string) {
    this.router.navigate(['/manager/space', id]);
  }

  formatNames(items: any[] | null | undefined, key: string): string {
    if (!items || items.length === 0) return 'None';
    return items.map(item => item[key]).join(', ');
  }

  exportPDF() {
    this.spaceService.downloadManagerReport(this.reportMonth).subscribe({
      next: (blob) => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `capacity_report_${this.reportMonth}.pdf`;
        link.click();
        URL.revokeObjectURL(link.href);
      },
      error: (err) => alert(err.error?.message || 'Report failed')
    });
  }

  onImportFileSelected(event: any) {
    if (event.target.files.length > 0) {
      this.importFile = event.target.files[0];
    }
  }

  importSpaces() {
    if (!this.importFile) return;
    this.spaceService.importSpaces(this.importFile).subscribe({
      next: (res) => {
        this.importMessage = res.message || 'Import completed.';
        this.loadSpaces();
      },
      error: (err) => {
        this.importMessage = err.error?.message || 'Import failed.';
      }
    });
  }
}
