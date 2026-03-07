import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './forgot-password.html'
})
export class ForgotPasswordComponent {
  form: FormGroup;
  message = '';
  resetLink: string | null = null;

  constructor(private fb: FormBuilder, private authService: AuthService) {
    this.form = this.fb.group({
      usernameOrEmail: ['', Validators.required]
    });
  }

  onSubmit() {
    if (this.form.invalid) return;

    this.authService.requestPasswordReset(this.form.value).subscribe({
      next: (res) => {
        this.message = res.message || 'If the account exists, a reset link was sent.';
        // Remove leading slash if present to make it work with Angular routing
        this.resetLink = res.resetLink ? res.resetLink.replace(/^\//, '') : null;
      },
      error: (err) => {
        this.message = err.error?.message || 'Unable to process reset request.';
      }
    });
  }

  getFullResetLink(): string {
    if (!this.resetLink) return '';
    return window.location.origin + '/' + this.resetLink;
  }
}
