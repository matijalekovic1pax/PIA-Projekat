import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { AdminService } from '../../services/admin.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Chart } from 'chart.js/auto';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.html'
})
export class AdminDashboardComponent implements OnInit {
  @ViewChild('popularityChart', { static: false }) popularityChartRef?: ElementRef<HTMLCanvasElement>;
  @ViewChild('revenueChart', { static: false }) revenueChartRef?: ElementRef<HTMLCanvasElement>;

  users: any[] = [];
  spaces: any[] = [];
  statusFilter: string = '';
  loading = false;
  activeTab: 'users' | 'spaces' | 'stats' = 'users';
  spaceStatusFilter: string = 'pending';
  stats: any = null;
  private popularityChart: Chart | null = null;
  private revenueChart: Chart | null = null;
  editingUser: any | null = null;
  message = '';

  constructor(private adminService: AdminService) { }

  ngOnInit() {
    this.loadUsers();
    this.loadSpaces();
    this.loadStats();
  }

  setTab(tab: 'users' | 'spaces' | 'stats') {
    this.activeTab = tab;
    if (tab === 'spaces') {
      this.loadSpaces();
    }
    if (tab === 'stats') {
      this.loadStats();
    }
  }

  loadUsers() {
    this.loading = true;
    this.adminService.getUsers(this.statusFilter).subscribe({
      next: (data) => {
        this.users = data;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  loadSpaces() {
    this.adminService.getSpaces(this.spaceStatusFilter).subscribe({
      next: (data) => this.spaces = data,
      error: (err) => console.error(err)
    });
  }

  loadStats() {
    this.adminService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
        setTimeout(() => this.renderCharts(), 0);
      },
      error: (err) => console.error(err)
    });
  }

  private renderCharts() {
    const popularityCanvas = this.popularityChartRef?.nativeElement;
    const revenueCanvas = this.revenueChartRef?.nativeElement;

    if (!popularityCanvas || !revenueCanvas || !this.stats) return;

    const popularityLabels = (this.stats.popularity || []).map((item: any) => item.spaceName);
    const popularityValues = (this.stats.popularity || []).map((item: any) => item.likes);
    const revenueLabels = (this.stats.revenue || []).map((item: any) => item.spaceName);
    const revenueValues = (this.stats.revenue || []).map((item: any) => item.revenue);

    if (this.popularityChart) {
      this.popularityChart.destroy();
    }
    if (this.revenueChart) {
      this.revenueChart.destroy();
    }

    this.popularityChart = new Chart(popularityCanvas, {
      type: 'bar',
      data: {
        labels: popularityLabels,
        datasets: [{ label: 'Likes', data: popularityValues, backgroundColor: '#38bdf8' }]
      },
      options: { responsive: true, maintainAspectRatio: false }
    });

    this.revenueChart = new Chart(revenueCanvas, {
      type: 'bar',
      data: {
        labels: revenueLabels,
        datasets: [{ label: 'Revenue', data: revenueValues, backgroundColor: '#0ea5e9' }]
      },
      options: { responsive: true, maintainAspectRatio: false }
    });
  }

  updateStatus(id: string, status: string) {
    if (confirm(`Are you sure you want to set status to ${status}?`)) {
      this.adminService.updateUserStatus(id, status).subscribe({
        next: () => {
          this.loadUsers();
          alert('User updated');
        },
        error: (err) => alert('Failed to update user')
      });
    }
  }

  startEdit(user: any) {
    this.editingUser = { ...user };
  }

  cancelEdit() {
    this.editingUser = null;
  }

  saveUser() {
    if (!this.editingUser) return;
    this.adminService.updateUser(this.editingUser.id, this.editingUser).subscribe({
      next: () => {
        this.loadUsers();
        this.editingUser = null;
      },
      error: (err) => alert(err.error?.message || 'Failed to update user')
    });
  }

  deleteUser(id: string) {
    if (!confirm('Delete this user?')) return;
    this.adminService.deleteUser(id).subscribe({
      next: () => this.loadUsers(),
      error: (err) => alert(err.error?.message || 'Delete failed')
    });
  }

  updateSpaceStatus(id: string, status: string) {
    if (!confirm(`Set space status to ${status}?`)) return;
    this.adminService.updateSpaceStatus(id, status).subscribe({
      next: () => this.loadSpaces(),
      error: (err) => alert(err.error?.message || 'Failed to update space')
    });
  }
}
