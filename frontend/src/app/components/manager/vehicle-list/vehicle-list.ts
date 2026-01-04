import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { VehicleService } from '../../../services/vehicle/vehicle.service';
import { VehicleListDTO } from '../../../dto/vehicle/VehicleListDTO';
import { PageResponseDTO } from '../../../dto/common/PageResponseDTO';
import { CardModule } from 'primeng/card';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { TooltipModule } from 'primeng/tooltip';
import { PaginatorModule } from 'primeng/paginator';

@Component({
  selector: 'app-vehicle-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    CardModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    ProgressSpinnerModule,
    MessageModule,
    ConfirmDialogModule,
    ToastModule,
    IconFieldModule,
    InputIconModule,
    TooltipModule,
    PaginatorModule
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './vehicle-list.html',
  styleUrl: './vehicle-list.scss'
})
export class VehicleListComponent implements OnInit {
  vehicles: VehicleListDTO[] = [];
  loading = false;
  searchQuery = '';
  private searchTimeout: any;
  
  currentPage = 0;
  pageSize = 20;
  totalRecords = 0;
  sortField = 'id';
  sortOrder = 'asc';

  constructor(
    private vehicleService: VehicleService,
    private confirmationService: ConfirmationService,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.loadVehicles();
  }

  loadVehicles(): void {
    this.loading = true;
    this.vehicleService.getAllPaged(this.currentPage, this.pageSize, this.sortField, this.sortOrder).subscribe({
      next: (response: PageResponseDTO<VehicleListDTO>) => {
        this.vehicles = response.content;
        this.totalRecords = response.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load vehicles' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  onPageChange(event: any): void {
    this.currentPage = event.page;
    this.pageSize = event.rows;
    if (this.searchQuery.trim()) {
      this.performSearch();
    } else {
      this.loadVehicles();
    }
  }

  onSearch(): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }

    this.searchTimeout = setTimeout(() => {
      this.currentPage = 0;
      if (this.searchQuery.trim()) {
        this.performSearch();
      } else {
        this.loadVehicles();
      }
    }, 300);
  }

  private performSearch(): void {
    this.loading = true;
    this.vehicleService.searchPaged(this.searchQuery, this.currentPage, this.pageSize).subscribe({
      next: (response: PageResponseDTO<VehicleListDTO>) => {
        this.vehicles = response.content;
        this.totalRecords = response.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Search failed' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.currentPage = 0;
    this.loadVehicles();
  }

  confirmDelete(vehicle: VehicleListDTO): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete vehicle "${vehicle.licensePlate}"?`,
      header: 'Confirm Delete',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.deleteVehicle(vehicle.id);
      }
    });
  }

  deleteVehicle(id: number): void {
    this.vehicleService.delete(id).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Vehicle deleted successfully' });
        this.loadVehicles();
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete vehicle' });
        console.error(err);
      }
    });
  }

  formatDate(dateString: string): string {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}
