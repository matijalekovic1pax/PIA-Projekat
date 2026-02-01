import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login';
import { RegisterComponent } from './components/register/register';
import { NotFoundComponent } from './components/not-found/not-found';
import { HomeComponent } from './components/home/home';
import { SpaceDetailsComponent } from './components/space-details/space-details';
import { ProfileComponent } from './components/profile/profile';

import { MemberSearchComponent } from './components/member-search/member-search';

import { ManagerDashboardComponent } from './components/manager-dashboard/manager-dashboard';
import { SpaceFormComponent } from './components/space-form/space-form';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard';
import { ForgotPasswordComponent } from './components/forgot-password/forgot-password';
import { ResetPasswordComponent } from './components/reset-password/reset-password';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
    { path: '', redirectTo: 'home', pathMatch: 'full' },
    { path: 'home', component: HomeComponent },
    { path: 'login', component: LoginComponent },
    { path: 'forgot-password', component: ForgotPasswordComponent },
    { path: 'reset-password/:token', component: ResetPasswordComponent },
    { path: 'register', component: RegisterComponent },
    { path: 'admin-login', component: LoginComponent, data: { isAdmin: true } },
    { path: 'profile', component: ProfileComponent, canActivate: [authGuard], data: { roles: ['member', 'manager'] } },
    { path: 'search', component: MemberSearchComponent, canActivate: [authGuard], data: { roles: ['member'] } },
    { path: 'spaces/:id', component: SpaceDetailsComponent },

    // Manager Routes
    { path: 'manager/dashboard', component: ManagerDashboardComponent, canActivate: [authGuard], data: { roles: ['manager'] } },
    { path: 'manager/space/new', component: SpaceFormComponent, canActivate: [authGuard], data: { roles: ['manager'] } },
    { path: 'manager/space/:id', component: SpaceFormComponent, canActivate: [authGuard], data: { roles: ['manager'] } },

    // Admin Routes
    { path: 'admin/dashboard', component: AdminDashboardComponent, canActivate: [authGuard], data: { roles: ['admin'] } },

    { path: '**', component: NotFoundComponent }
];
