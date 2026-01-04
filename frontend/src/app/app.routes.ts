import { Routes } from '@angular/router';
import { Login } from './components/auth/login/login';
import { Register } from './components/auth/register/register';
import { Dashboard } from './components/dashboard/dashboard';
import { AuthGuard } from './services/auth/auth-guard';
import { MapComponent } from './components/map/map.component';
import { RegisterRequestForm } from './components/register-request-form/register-request-form';
import { ManagerGuard } from './services/auth/manager-guard';
import { RegistrationRequestsComponent } from './components/manager/registration-requests/registration-requests';
import { RequestDetailComponent } from './components/manager/request-detail/request-detail';
import { VehicleListComponent } from './components/manager/vehicle-list/vehicle-list';
import { VehicleFormComponent } from './components/manager/vehicle-form/vehicle-form';
import { VehicleDetailComponent } from './components/manager/vehicle-detail/vehicle-detail';

export const routes: Routes = [
    { path: '', component: Login },
    { path: 'register', component: Register },
    {
        path: 'app',
        component: Dashboard,
        canActivate: [AuthGuard],
        children: [
            { path: '', redirectTo: 'home', pathMatch: 'full' },
            {
                path: 'home',
                component: RegisterRequestForm
            },
            {
                path: 'manager/requests',
                component: RegistrationRequestsComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/requests/:id',
                component: RequestDetailComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/vehicles',
                component: VehicleListComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/vehicles/new',
                component: VehicleFormComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/vehicles/:id',
                component: VehicleDetailComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/vehicles/:id/edit',
                component: VehicleFormComponent,
                canActivate: [ManagerGuard]
            }
        ]
    },
];
