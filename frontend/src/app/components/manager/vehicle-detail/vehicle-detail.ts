import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { VehicleService } from '../../../services/vehicle/vehicle.service';
import { VehicleResponseDTO } from '../../../dto/vehicle/VehicleResponseDTO';
import { VehicleLocationDTO } from '../../../dto/vehicle/VehicleLocationDTO';
import { DistanceStatisticsDTO } from '../../../dto/vehicle/DistanceStatisticsDTO';
import { AvailabilityStatisticsDTO } from '../../../dto/vehicle/AvailabilityStatisticsDTO';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';
import { DividerModule } from 'primeng/divider';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';
import { ToastModule } from 'primeng/toast';
import { ChartModule } from 'primeng/chart';
import { MessageService } from 'primeng/api';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../../../environments/environment';
import * as L from 'leaflet';

interface PeriodOption {
  label: string;
  value: string;
}

interface AvailabilityPeriodOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-vehicle-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    CardModule,
    ButtonModule,
    TagModule,
    ProgressSpinnerModule,
    MessageModule,
    DividerModule,
    SelectModule,
    DatePickerModule,
    ToastModule,
    ChartModule
  ],
  providers: [MessageService],
  templateUrl: './vehicle-detail.html',
  styleUrl: './vehicle-detail.scss'
})
export class VehicleDetailComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('mapContainer') mapContainer!: ElementRef;

  vehicleId!: number;
  vehicle: VehicleResponseDTO | null = null;
  location: VehicleLocationDTO | null = null;
  statistics: DistanceStatisticsDTO | null = null;
  availability: AvailabilityStatisticsDTO | null = null;
  imageUrls: { id: number; originalName: string; url: string }[] = [];

  loading = false;
  locationLoading = false;
  statsLoading = false;
  availabilityLoading = false;
  locationError = '';

  periodOptions: PeriodOption[] = [
    { label: 'Last Week', value: 'week' },
    { label: 'Last Month', value: 'month' },
    { label: 'Last 3 Months', value: '3months' },
    { label: 'Last 6 Months', value: '6months' },
    { label: 'Last Year', value: 'year' },
    { label: 'Custom Range', value: 'custom' }
  ];

  availabilityPeriodOptions: AvailabilityPeriodOption[] = [
    { label: 'Last 1 Hour', value: '1h' },
    { label: 'Last 3 Hours (Real-time)', value: '3h' },
    { label: 'Last 12 Hours', value: '12h' },
    { label: 'Last 24 Hours', value: '24h' },
    { label: 'Last Week', value: 'week' },
    { label: 'Last Month', value: 'month' },
    { label: 'Last 3 Months', value: '3months' },
    { label: 'Last Year', value: 'year' },
    { label: 'Custom Range', value: 'custom' }
  ];

  selectedPeriod: PeriodOption = this.periodOptions[0];
  selectedAvailabilityPeriod: AvailabilityPeriodOption = this.availabilityPeriodOptions[3];
  customStartDate: Date | null = null;
  customEndDate: Date | null = null;
  availabilityCustomStart: Date | null = null;
  availabilityCustomEnd: Date | null = null;
  maxDate = new Date();
  minStartDate = new Date(new Date().setFullYear(new Date().getFullYear() - 1));

  chartData: any;
  chartOptions: any;
  availabilityChartData: any;
  availabilityChartOptions: any;

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
  private stompClient: Client | null = null;
  isRealTimeMode = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private vehicleService: VehicleService,
    private messageService: MessageService,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.vehicleId = +id;
      this.loadVehicle();
      this.loadLocation();
      this.loadStatistics();
      this.loadAvailability();
    }
    this.initChartOptions();
    this.initAvailabilityChartOptions();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.initMap(), 100);
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
    this.disconnectWebSocket();
  }

  private connectWebSocket(): void {
    if (this.stompClient?.connected) return;

    const wsUrl = environment.apiUrl.replace('/api/v1/', '/ws');

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      debug: (str) => console.log('STOMP:', str)
    });

    this.stompClient.onConnect = () => {
      console.log('WebSocket connected');
      
      this.stompClient?.publish({
        destination: `/app/vehicle/${this.vehicleId}/availability/subscribe`
      });

      this.stompClient?.subscribe(
        `/topic/vehicle/${this.vehicleId}/availability`,
        (message: IMessage) => {
          this.ngZone.run(() => {
            const stats = JSON.parse(message.body) as AvailabilityStatisticsDTO;
            this.availability = stats;
            this.updateAvailabilityChart();
            console.log('Received real-time availability update');
          });
        }
      );
    };

    this.stompClient.onStompError = (frame) => {
      console.error('STOMP error:', frame.headers['message'], frame.body);
    };

    this.stompClient.onWebSocketError = (event) => {
      console.error('WebSocket error:', event);
    };

    this.stompClient.activate();
    this.isRealTimeMode = true;
  }

  private disconnectWebSocket(): void {
    if (this.stompClient?.connected) {
      this.stompClient.publish({
        destination: `/app/vehicle/${this.vehicleId}/availability/unsubscribe`
      });
      this.stompClient.deactivate();
    }
    this.isRealTimeMode = false;
  }

  private initMap(): void {
    if (!this.mapContainer?.nativeElement) return;
    if (this.map) return; // Already initialized

    this.map = L.map(this.mapContainer.nativeElement).setView([44.8176, 20.4633], 10);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    // If location data already loaded, place marker now
    if (this.location) {
      this.updateMapMarker();
    }
  }

  private updateMapMarker(): void {
    if (!this.map || !this.location) return;

    if (this.marker) {
      this.map.removeLayer(this.marker);
    }

    const icon = L.divIcon({
      className: 'vehicle-marker',
      html: `<div style="background-color: ${this.location.online ? '#22c55e' : '#ef4444'}; 
             width: 24px; height: 24px; border-radius: 50%; border: 3px solid white; 
             box-shadow: 0 2px 5px rgba(0,0,0,0.3);"></div>`,
      iconSize: [24, 24],
      iconAnchor: [12, 12]
    });

    this.marker = L.marker([this.location.latitude, this.location.longitude], { icon })
      .addTo(this.map)
      .bindPopup(`<b>${this.location.licensePlate}</b><br>
                  Status: ${this.location.online ? 'Online' : 'Offline'}<br>
                  State: ${this.location.currentState || 'Unknown'}`);

    this.map.setView([this.location.latitude, this.location.longitude], 14);
  }

  private initChartOptions(): void {
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--p-text-color') || '#ffffff';
    const surfaceBorder = documentStyle.getPropertyValue('--p-content-border-color') || '#374151';

    this.chartOptions = {
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
        }
      },
      scales: {
        x: {
          ticks: { color: textColor },
          grid: { color: surfaceBorder }
        },
        y: {
          ticks: { color: textColor },
          grid: { color: surfaceBorder },
          title: {
            display: true,
            text: 'Distance (km)',
            color: textColor
          }
        }
      }
    };
  }

  private initAvailabilityChartOptions(): void {
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--p-text-color') || '#ffffff';
    const surfaceBorder = documentStyle.getPropertyValue('--p-content-border-color') || '#374151';

    this.availabilityChartOptions = {
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: true,
          labels: { color: textColor }
        },
        tooltip: {
          callbacks: {
            label: (context: any) => {
              const dataPoint = this.availability?.dataPoints[context.dataIndex];
              if (dataPoint) {
                const onlineTime = this.formatDuration(dataPoint.onlineSeconds);
                const offlineTime = this.formatDuration(dataPoint.offlineSeconds);
                if (context.dataset.label === 'Online') {
                  return `Online: ${dataPoint.onlinePercentage}% (${onlineTime})`;
                } else {
                  return `Offline: ${dataPoint.offlinePercentage}% (${offlineTime})`;
                }
              }
              return `${context.dataset.label}: ${context.raw}%`;
            }
          }
        }
      },
      scales: {
        x: {
          stacked: true,
          ticks: { color: textColor },
          grid: { color: surfaceBorder }
        },
        y: {
          stacked: true,
          min: 0,
          max: 100,
          ticks: { color: textColor },
          grid: { color: surfaceBorder },
          title: {
            display: true,
            text: 'Availability (%)',
            color: textColor
          }
        }
      }
    };
  }

  formatDuration(seconds: number): string {
    if (seconds < 60) return `${seconds}s`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
    const hours = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    return `${hours}h ${mins}m`;
  }

  loadVehicle(): void {
    this.loading = true;
    this.vehicleService.getById(this.vehicleId).subscribe({
      next: (data) => {
        this.vehicle = data;
        this.loading = false;
        this.loadImages();
        // Initialize map after view updates (container is inside *ngIf)
        setTimeout(() => this.initMap(), 100);
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load vehicle' });
        this.loading = false;
        console.error(err);
      }
    });
  }

  loadImages(): void {
    this.vehicleService.getVehicleImages(this.vehicleId).subscribe({
      next: (data) => {
        this.imageUrls = data.images;
      },
      error: (err) => console.error('Failed to load images', err)
    });
  }

  loadLocation(): void {
    this.locationLoading = true;
    this.locationError = '';
    this.vehicleService.getLastLocation(this.vehicleId).subscribe({
      next: (data) => {
        this.location = data;
        this.locationLoading = false;
        // Try to update marker, retry if map not ready yet
        this.tryUpdateMapMarker();
      },
      error: (err) => {
        this.locationError = 'No location data available';
        this.locationLoading = false;
      }
    });
  }

  private tryUpdateMapMarker(retries = 5): void {
    if (this.map && this.location) {
      this.updateMapMarker();
    } else if (retries > 0) {
      setTimeout(() => this.tryUpdateMapMarker(retries - 1), 200);
    }
  }

  loadStatistics(): void {
    this.statsLoading = true;

    let request$;
    if (this.selectedPeriod.value === 'custom' && this.customStartDate && this.customEndDate) {
      const startStr = this.formatDate(this.customStartDate);
      const endStr = this.formatDate(this.customEndDate);
      request$ = this.vehicleService.getDistanceStatsByDateRange(this.vehicleId, startStr, endStr);
    } else if (this.selectedPeriod.value !== 'custom') {
      request$ = this.vehicleService.getDistanceStatsByPeriod(this.vehicleId, this.selectedPeriod.value);
    } else {
      this.statsLoading = false;
      return;
    }

    request$.subscribe({
      next: (data) => {
        this.statistics = data;
        this.updateChart();
        this.statsLoading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load statistics' });
        this.statsLoading = false;
        console.error(err);
      }
    });
  }

  private updateChart(): void {
    if (!this.statistics) return;

    const labels = this.statistics.dataPoints.map(dp => dp.label);
    const data = this.statistics.dataPoints.map(dp => dp.distance);

    this.chartData = {
      labels,
      datasets: [
        {
          label: 'Distance (km)',
          data,
          backgroundColor: 'rgba(124, 58, 237, 0.5)',
          borderColor: 'rgb(124, 58, 237)',
          borderWidth: 2,
          fill: true,
          tension: 0.4
        }
      ]
    };
  }

  onPeriodChange(): void {
    if (this.selectedPeriod.value !== 'custom') {
      this.loadStatistics();
    }
  }

  onAvailabilityPeriodChange(): void {
    if (this.selectedAvailabilityPeriod.value === '3h') {
      this.connectWebSocket();
    } else {
      this.disconnectWebSocket();
    }

    if (this.selectedAvailabilityPeriod.value !== 'custom') {
      this.loadAvailability();
    }
  }

  loadAvailability(): void {
    this.availabilityLoading = true;

    let request$;
    if (this.selectedAvailabilityPeriod.value === 'custom' && this.availabilityCustomStart && this.availabilityCustomEnd) {
      const startStr = this.availabilityCustomStart.toISOString();
      const endStr = this.availabilityCustomEnd.toISOString();
      request$ = this.vehicleService.getAvailabilityStatsByDateRange(this.vehicleId, startStr, endStr);
    } else if (this.selectedAvailabilityPeriod.value !== 'custom') {
      request$ = this.vehicleService.getAvailabilityStatsByPeriod(this.vehicleId, this.selectedAvailabilityPeriod.value);
    } else {
      this.availabilityLoading = false;
      return;
    }

    request$.subscribe({
      next: (data) => {
        this.availability = data;
        this.updateAvailabilityChart();
        this.availabilityLoading = false;
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to load availability statistics' });
        this.availabilityLoading = false;
        console.error(err);
      }
    });
  }

  private updateAvailabilityChart(): void {
    if (!this.availability) return;

    const labels = this.availability.dataPoints.map(dp => dp.label);
    const onlineData = this.availability.dataPoints.map(dp => dp.onlinePercentage);
    const offlineData = this.availability.dataPoints.map(dp => dp.offlinePercentage);

    this.availabilityChartData = {
      labels,
      datasets: [
        {
          label: 'Online',
          data: onlineData,
          backgroundColor: 'rgba(34, 197, 94, 0.7)',
          borderColor: 'rgb(34, 197, 94)',
          borderWidth: 1
        },
        {
          label: 'Offline',
          data: offlineData,
          backgroundColor: 'rgba(239, 68, 68, 0.7)',
          borderColor: 'rgb(239, 68, 68)',
          borderWidth: 1
        }
      ]
    };
  }

  applyAvailabilityCustomRange(): void {
    if (!this.availabilityCustomStart || !this.availabilityCustomEnd) {
      this.messageService.add({ severity: 'warn', summary: 'Warning', detail: 'Please select both start and end dates' });
      return;
    }

    if (this.availabilityCustomEnd < this.availabilityCustomStart) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'End date must be after start date' });
      return;
    }

    const daysDiff = Math.floor((this.availabilityCustomEnd.getTime() - this.availabilityCustomStart.getTime()) / (1000 * 60 * 60 * 24));
    if (daysDiff > 365) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Date range cannot exceed one year' });
      return;
    }

    this.loadAvailability();
  }

  applyCustomRange(): void {
    if (!this.customStartDate || !this.customEndDate) {
      this.messageService.add({ severity: 'warn', summary: 'Warning', detail: 'Please select both start and end dates' });
      return;
    }

    if (this.customEndDate < this.customStartDate) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'End date must be after start date' });
      return;
    }

    const daysDiff = Math.floor((this.customEndDate.getTime() - this.customStartDate.getTime()) / (1000 * 60 * 60 * 24));
    if (daysDiff > 365) {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Date range cannot exceed one year' });
      return;
    }

    this.loadStatistics();
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  formatDateTime(dateString: string): string {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  goBack(): void {
    this.router.navigate(['/app/manager/vehicles']);
  }

  editVehicle(): void {
    this.router.navigate(['/app/manager/vehicles', this.vehicleId, 'edit']);
  }
}
