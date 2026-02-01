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
        this.resetLink = res.resetLink || null;
      },
      error: (err) => {
        this.message = err.error?.message || 'Unable to process reset request.';
      }
    });
  }
}
