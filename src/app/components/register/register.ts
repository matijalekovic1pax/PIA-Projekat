import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.html'
})
export class RegisterComponent {
  registerForm: FormGroup;
  selectedFile: File | null = null;
  errorMessage: string = '';
  isManager: boolean = false;
  imageError: string = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', [Validators.required, Validators.pattern(/^(?=[A-Za-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,12}$/)]],
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^\+?[0-9\s-]{6,20}$/)]],
      type: ['member', Validators.required],

      // Manager fields
      companyName: [''],
      companyAddress: [''],
      companyRegNumber: [''],
      companyTaxId: ['']
    });

    this.registerForm.get('type')?.valueChanges.subscribe(val => {
      this.isManager = val === 'manager';
      const managerFields = ['companyName', 'companyAddress', 'companyRegNumber', 'companyTaxId'];

      if (this.isManager) {
        managerFields.forEach(field => this.registerForm.get(field)?.setValidators(Validators.required));
        this.registerForm.get('companyRegNumber')?.setValidators([Validators.required, Validators.pattern(/^\d{8}$/)]);
        this.registerForm.get('companyTaxId')?.setValidators([Validators.required, Validators.pattern(/^[1-9]\d{8}$/)]);
      } else {
        managerFields.forEach(field => {
          const control = this.registerForm.get(field);
          control?.clearValidators();
        });
      }

      managerFields.forEach(field => this.registerForm.get(field)?.updateValueAndValidity());
    });
  }

  onFileSelected(event: any) {
    this.imageError = '';
    if (!event.target.files.length) {
      this.selectedFile = null;
      return;
    }

    const file: File = event.target.files[0];
    const validTypes = ['image/png', 'image/jpeg'];
    if (!validTypes.includes(file.type)) {
      this.imageError = 'Profile picture must be a JPG or PNG file.';
      this.selectedFile = null;
      return;
    }

    const img = new Image();
    img.onload = () => {
      const width = img.width;
      const height = img.height;
      if (width < 100 || height < 100 || width > 300 || height > 300) {
        this.imageError = 'Image dimensions must be between 100x100 and 300x300 pixels.';
        this.selectedFile = null;
      } else {
        this.selectedFile = file;
      }
    };
    img.onerror = () => {
      this.imageError = 'Unable to read the selected image.';
      this.selectedFile = null;
    };
    img.src = URL.createObjectURL(file);
  }

  onSubmit() {
    if (this.registerForm.valid) {
      if (this.imageError) return;

      const formData = new FormData();
      Object.keys(this.registerForm.value).forEach(key => {
        formData.append(key, this.registerForm.get(key)?.value);
      });
      if (this.selectedFile) {
        formData.append('profilePicture', this.selectedFile);
      }

      this.authService.register(formData).subscribe({
        next: () => {
          alert('Registration successful! Waiting for admin approval.');
          this.router.navigate(['/login']);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Registration failed';
        }
      });
    } else {
      this.registerForm.markAllAsTouched();
    }
  }
}
