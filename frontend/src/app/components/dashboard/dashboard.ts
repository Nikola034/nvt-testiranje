import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth/auth-service';
import { MenuModule } from 'primeng/menu';
import { ButtonModule } from 'primeng/button';
import { MenuItem } from 'primeng/api';

@Component({
  selector: 'app-dashboard',
  imports: [RouterModule, CommonModule, MenuModule, ButtonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  menuItems: MenuItem[] = [];

  constructor(private router: Router, public authService: AuthService) {
    this.buildMenu();
  }

  buildMenu(): void {
    this.menuItems = [
      {
        label: 'Navigation',
        items: [
          {
            label: 'Home',
            icon: 'pi pi-home',
            routerLink: '/app/home'
          }
        ]
      }
    ];

    if (this.authService.IsManager()) {
      this.menuItems[0].items?.push({
        label: 'Registration Requests',
        icon: 'pi pi-list',
        routerLink: '/app/manager/requests'
      });
      this.menuItems[0].items?.push({
        label: 'Vehicles',
        icon: 'pi pi-truck',
        routerLink: '/app/manager/vehicles'
      });
    }
  }

  logout(): void {
    this.authService.logout();
  }
}
