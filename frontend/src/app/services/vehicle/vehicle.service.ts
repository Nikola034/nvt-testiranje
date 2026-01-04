import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { VehicleBrandDTO } from '../../dto/vehicle/VehicleBrandDTO';
import { VehicleModelDTO } from '../../dto/vehicle/VehicleModelDTO';
import { VehicleResponseDTO } from '../../dto/vehicle/VehicleResponseDTO';
import { VehicleListDTO } from '../../dto/vehicle/VehicleListDTO';
import { CreateVehicleDTO } from '../../dto/vehicle/CreateVehicleDTO';
import { UpdateVehicleDTO } from '../../dto/vehicle/UpdateVehicleDTO';
import { VehicleLocationDTO } from '../../dto/vehicle/VehicleLocationDTO';
import { DistanceStatisticsDTO } from '../../dto/vehicle/DistanceStatisticsDTO';
import { AvailabilityStatisticsDTO } from '../../dto/vehicle/AvailabilityStatisticsDTO';
import { PageResponseDTO } from '../../dto/common/PageResponseDTO';

@Injectable({
  providedIn: 'root'
})
export class VehicleService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAllBrands(): Observable<VehicleBrandDTO[]> {
    return this.http.get<VehicleBrandDTO[]>(`${this.baseUrl}vehicles/brands`);
  }

  getModelsByBrand(brandId: number): Observable<VehicleModelDTO[]> {
    return this.http.get<VehicleModelDTO[]>(`${this.baseUrl}vehicles/brands/${brandId}/models`);
  }

  getAll(): Observable<VehicleListDTO[]> {
    return this.http.get<VehicleListDTO[]>(`${this.baseUrl}vehicles`);
  }

  getAllPaged(page: number = 0, size: number = 20, sortBy: string = 'id', sortDir: string = 'asc'): Observable<PageResponseDTO<VehicleListDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    return this.http.get<PageResponseDTO<VehicleListDTO>>(`${this.baseUrl}vehicles/paged`, { params });
  }

  getById(id: number): Observable<VehicleResponseDTO> {
    return this.http.get<VehicleResponseDTO>(`${this.baseUrl}vehicles/${id}`);
  }

  search(query: string): Observable<VehicleListDTO[]> {
    return this.http.get<VehicleListDTO[]>(`${this.baseUrl}vehicles/search`, {
      params: { query }
    });
  }

  searchPaged(query: string, page: number = 0, size: number = 20): Observable<PageResponseDTO<VehicleListDTO>> {
    const params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponseDTO<VehicleListDTO>>(`${this.baseUrl}vehicles/search/paged`, { params });
  }

  create(data: CreateVehicleDTO, images: File[]): Observable<VehicleResponseDTO> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    images.forEach((image) => {
      formData.append('images', image, image.name);
    });
    return this.http.post<VehicleResponseDTO>(`${this.baseUrl}vehicles`, formData);
  }

  update(id: number, data: UpdateVehicleDTO, newImages?: File[]): Observable<VehicleResponseDTO> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (newImages && newImages.length > 0) {
      newImages.forEach((image) => {
        formData.append('images', image, image.name);
      });
    }
    return this.http.put<VehicleResponseDTO>(`${this.baseUrl}vehicles/${id}`, formData);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}vehicles/${id}`);
  }

  getVehicleImages(vehicleId: number): Observable<{
    images: { id: number; originalName: string; url: string }[];
  }> {
    return this.http.get<{
      images: { id: number; originalName: string; url: string }[];
    }>(`${this.baseUrl}files/vehicle/${vehicleId}/images`);
  }

  getLastLocation(vehicleId: number): Observable<VehicleLocationDTO> {
    return this.http.get<VehicleLocationDTO>(`${this.baseUrl}vehicles/${vehicleId}/location`);
  }

  getDistanceStatsByPeriod(vehicleId: number, period: string): Observable<DistanceStatisticsDTO> {
    const params = new HttpParams().set('period', period);
    return this.http.get<DistanceStatisticsDTO>(`${this.baseUrl}vehicles/${vehicleId}/distance/stats`, { params });
  }

  getDistanceStatsByDateRange(vehicleId: number, startDate: string, endDate: string): Observable<DistanceStatisticsDTO> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get<DistanceStatisticsDTO>(`${this.baseUrl}vehicles/${vehicleId}/distance/stats`, { params });
  }

  getAvailabilityStatsByPeriod(vehicleId: number, period: string): Observable<AvailabilityStatisticsDTO> {
    const params = new HttpParams().set('period', period);
    return this.http.get<AvailabilityStatisticsDTO>(`${this.baseUrl}vehicles/${vehicleId}/availability/stats`, { params });
  }

  getAvailabilityStatsByDateRange(vehicleId: number, startTime: string, endTime: string): Observable<AvailabilityStatisticsDTO> {
    const params = new HttpParams()
      .set('startTime', startTime)
      .set('endTime', endTime);
    return this.http.get<AvailabilityStatisticsDTO>(`${this.baseUrl}vehicles/${vehicleId}/availability/stats`, { params });
  }
}
