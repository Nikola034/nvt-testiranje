import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { CompanyRegistrationService } from '../../../services/company/company-registration.service';
import { RegistrationRequestListDTO } from '../../../dto/company/RegistrationRequestListDTO';
import { PageResponseDTO } from '../../../dto/common/PageResponseDTO';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { PaginatorModule } from 'primeng/paginator';

@Component({
  selector: 'app-registration-requests',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule, 
    CardModule, 
    TableModule, 
    ButtonModule, 
    TagModule,
    ProgressSpinnerModule,
    MessageModule,
    PaginatorModule
  ],
  templateUrl: './registration-requests.html',
  styleUrl: './registration-requests.scss'
})
export class RegistrationRequestsComponent implements OnInit {
  requests: RegistrationRequestListDTO[] = [];
  loading = false;
  error = '';
  showAll = false;
  
  currentPage = 0;
  pageSize = 20;
  totalRecords = 0;
  sortField = 'createdAt';
  sortOrder = 'desc';

  constructor(private companyService: CompanyRegistrationService) {}

  ngOnInit(): void {
    this.loadRequests();
  }

  loadRequests(): void {
    this.loading = true;
    this.error = '';
    
    const request$ = this.showAll 
      ? this.companyService.getAllRequestsPaged(this.currentPage, this.pageSize, this.sortField, this.sortOrder)
      : this.companyService.getPendingRequestsPaged(this.currentPage, this.pageSize, this.sortField, this.sortOrder);

    request$.subscribe({
      next: (response: PageResponseDTO<RegistrationRequestListDTO>) => {
        this.requests = response.content;
        this.totalRecords = response.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load registration requests';
        this.loading = false;
        console.error(err);
      }
    });
  }

  onPageChange(event: any): void {
    this.currentPage = event.page;
    this.pageSize = event.rows;
    this.loadRequests();
  }

  toggleFilter(): void {
    this.showAll = !this.showAll;
    this.currentPage = 0;
    this.loadRequests();
  }

  getStatusSeverity(status: string): 'warn' | 'success' | 'danger' | 'info' {
    switch (status) {
      case 'PENDING': return 'warn';
      case 'APPROVED': return 'success';
      case 'REJECTED': return 'danger';
      default: return 'info';
    }
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
