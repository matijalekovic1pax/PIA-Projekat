import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { SpaceService } from '../../services/space.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.html'
})
export class ProfileComponent implements OnInit {
  profileForm: FormGroup;
  user: any;
  reservations: any[] = [];
  managerSpaces: any[] = [];
  selectedFile: File | null = null;
  message: string = '';
  messageType: 'success' | 'error' = 'success';
  imageError: string = '';
  sortField: string = 'startDateTime';
  sortDirection: 'asc' | 'desc' = 'desc';

  // Edit mode state
  isEditing = false;
  editingPassword = false;
  previewImage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private authService: AuthService,
    private spaceService: SpaceService,
    private cdr: ChangeDetectorRef
  ) {
    this.profileForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      oldPassword: [''],
      newPassword: ['', Validators.pattern(/^(?=[A-Za-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,12}$/)],
      // Manager fields
      companyName: [''],
      companyAddress: [''],
      companyRegNumber: [''],
      companyTaxId: ['']
    });
  }

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.userService.getProfile().subscribe({
      next: (data) => {
        this.user = data.user;
        this.reservations = data.reservations;

        // Populate form with loaded data
        this.profileForm.patchValue({
          firstName: this.user.firstName || '',
          lastName: this.user.lastName || '',
          email: this.user.email || '',
          phone: this.user.phone || '',
          companyName: this.user.companyName || '',
          companyAddress: this.user.companyAddress || '',
          companyRegNumber: this.user.companyRegNumber || '',
          companyTaxId: this.user.companyTaxId || '',
          oldPassword: '',
          newPassword: ''
        });

        if (this.user?.type === 'manager') {
          this.spaceService.getManagerSpaces().subscribe({
            next: (spaces) => {
              this.managerSpaces = spaces;
              this.cdr.detectChanges();
            },
            error: (err) => console.error(err)
          });
        }
        this.cdr.detectChanges();
      },
      error: (err) => console.error(err)
    });
  }

  resetForm() {
    this.profileForm.patchValue({
      firstName: this.user?.firstName || '',
      lastName: this.user?.lastName || '',
      email: this.user?.email || '',
      phone: this.user?.phone || '',
      companyName: this.user?.companyName || '',
      companyAddress: this.user?.companyAddress || '',
      companyRegNumber: this.user?.companyRegNumber || '',
      companyTaxId: this.user?.companyTaxId || '',
      oldPassword: '',
      newPassword: ''
    });
    this.selectedFile = null;
    this.previewImage = null;
    this.imageError = '';
  }

  startEditing() {
    this.isEditing = true;
    this.editingPassword = false;
    // Populate form with current user data
    if (this.user) {
      this.profileForm.patchValue({
        firstName: this.user.firstName || '',
        lastName: this.user.lastName || '',
        email: this.user.email || '',
        phone: this.user.phone || '',
        companyName: this.user.companyName || '',
        companyAddress: this.user.companyAddress || '',
        companyRegNumber: this.user.companyRegNumber || '',
        companyTaxId: this.user.companyTaxId || ''
      });
    }
    this.cdr.detectChanges();
  }

  cancelEditing() {
    this.isEditing = false;
    this.editingPassword = false;
    // Reset form to original values
    this.resetForm();
    this.cdr.detectChanges();
  }

  togglePasswordEdit() {
    this.editingPassword = !this.editingPassword;
    if (!this.editingPassword) {
      this.profileForm.patchValue({ oldPassword: '', newPassword: '' });
    }
  }

  onFileSelected(event: any) {
    this.imageError = '';
    if (!event.target.files.length) {
      this.selectedFile = null;
      this.previewImage = null;
      return;
    }

    const file: File = event.target.files[0];
    const validTypes = ['image/png', 'image/jpeg'];
    if (!validTypes.includes(file.type)) {
      this.imageError = 'Profile picture must be a JPG or PNG file.';
      this.selectedFile = null;
      this.previewImage = null;
      return;
    }

    const img = new Image();
    img.onload = () => {
      const width = img.width;
      const height = img.height;
      if (width < 100 || height < 100 || width > 300 || height > 300) {
        this.imageError = 'Image dimensions must be between 100x100 and 300x300 pixels.';
        this.selectedFile = null;
        this.previewImage = null;
      } else {
        this.selectedFile = file;
        this.previewImage = URL.createObjectURL(file);
      }
      this.cdr.detectChanges();
    };
    img.onerror = () => {
      this.imageError = 'Unable to read the selected image.';
      this.selectedFile = null;
      this.previewImage = null;
      this.cdr.detectChanges();
    };
    img.src = URL.createObjectURL(file);
  }

  onUpdate() {
    if (this.profileForm.valid) {
      if (this.imageError) return;
      const formData = new FormData();
      Object.keys(this.profileForm.value).forEach(key => {
        const val = this.profileForm.get(key)?.value;
        if (val) formData.append(key, val);
      });
      if (this.selectedFile) {
        formData.append('profilePicture', this.selectedFile);
      }

      this.userService.updateProfile(formData).subscribe({
        next: (user) => {
          this.user = user;
          this.authService.setCurrentUser(user);
          this.message = 'Profile updated successfully!';
          this.messageType = 'success';
          this.isEditing = false;
          this.editingPassword = false;
          this.resetForm();
          this.cdr.detectChanges();
          setTimeout(() => {
            this.message = '';
            this.cdr.detectChanges();
          }, 3000);
        },
        error: (err) => {
          this.message = err.error?.message || 'Update failed';
          this.messageType = 'error';
          this.cdr.detectChanges();
        }
      });
    }
  }

  cancelReservation(id: string) {
    if (confirm('Are you sure you want to cancel this reservation?')) {
      this.userService.cancelReservation(id).subscribe({
        next: () => {
          this.loadData();
          this.message = 'Reservation cancelled';
          this.messageType = 'success';
          setTimeout(() => {
            this.message = '';
            this.cdr.detectChanges();
          }, 3000);
        },
        error: (err) => {
          this.message = err.error?.message || 'Cancel failed';
          this.messageType = 'error';
          this.cdr.detectChanges();
        }
      });
    }
  }

  canCancel(startDateTime: string): boolean {
    const start = new Date(startDateTime).getTime();
    const now = new Date().getTime();
    return (start - now) > 12 * 60 * 60 * 1000;
  }

  sortReservations(field: string) {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'desc';
    }

    this.reservations.sort((a, b) => {
      let valA = field === 'startDateTime' ? new Date(a[field]).getTime() : a[field];
      let valB = field === 'startDateTime' ? new Date(b[field]).getTime() : b[field];

      if (typeof valA === 'string') valA = valA.toLowerCase();
      if (typeof valB === 'string') valB = valB.toLowerCase();

      if (valA < valB) return this.sortDirection === 'asc' ? -1 : 1;
      if (valA > valB) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }

  formatOffices(offices: any[] | null | undefined): string {
    if (!offices || offices.length === 0) return 'None';
    return offices.map(office => `${office.name} (${office.deskCount})`).join(', ');
  }

  formatConferenceRooms(rooms: any[] | null | undefined): string {
    if (!rooms || rooms.length === 0) return 'None';
    return rooms.map(room => room.name).join(', ');
  }

  getProfileImage(): string {
    if (this.previewImage) return this.previewImage;
    return '/uploads/' + (this.user?.profilePicture || 'default.png');
  }

  getUserTypeLabel(): string {
    if (!this.user?.type) return '';
    return this.user.type.charAt(0).toUpperCase() + this.user.type.slice(1);
  }
}
