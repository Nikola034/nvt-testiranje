import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CountryDTO } from '../../dto/company/CountryDTO';
import { CityDTO } from '../../dto/company/CityDTO';
import { CreateRequestDTO } from '../../dto/company/CreateRequestDTO';
import { RegistrationRequestDTO } from '../../dto/company/RegistrationRequestDTO';
import { RegistrationRequestListDTO } from '../../dto/company/RegistrationRequestListDTO';
import { ProcessRequestDTO } from '../../dto/company/ProcessRequestDTO';
import { PageResponseDTO } from '../../dto/common/PageResponseDTO';

@Injectable({
  providedIn: 'root'
})
export class CompanyRegistrationService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getCountries(): Observable<CountryDTO[]> {
    return this.http.get<CountryDTO[]>(`${this.baseUrl}locations/countries`);
  }

  getCitiesByCountry(countryId: number): Observable<CityDTO[]> {
    return this.http.get<CityDTO[]>(`${this.baseUrl}locations/countries/${countryId}/cities`);
  }

  createRegistrationRequest(
    data: CreateRequestDTO,
    images: File[],
    documents: File[]
  ): Observable<number> {
    const formData = new FormData();
    
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    
    images.forEach((image) => {
      formData.append('images', image, image.name);
    });
    
    documents.forEach((document) => {
      formData.append('documents', document, document.name);
    });

    return this.http.post<number>(`${this.baseUrl}registration-requests`, formData);
  }

  getPendingRequests(): Observable<RegistrationRequestListDTO[]> {
    return this.http.get<RegistrationRequestListDTO[]>(`${this.baseUrl}registration-requests/pending`);
  }

  getPendingRequestsPaged(page: number = 0, size: number = 20, sortBy: string = 'createdAt', sortDir: string = 'desc'): Observable<PageResponseDTO<RegistrationRequestListDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    return this.http.get<PageResponseDTO<RegistrationRequestListDTO>>(`${this.baseUrl}registration-requests/pending/paged`, { params });
  }

  getPendingRequestsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}registration-requests/pending/count`);
  }

  getAllRequests(): Observable<RegistrationRequestListDTO[]> {
    return this.http.get<RegistrationRequestListDTO[]>(`${this.baseUrl}registration-requests/all`);
  }

  getAllRequestsPaged(page: number = 0, size: number = 20, sortBy: string = 'createdAt', sortDir: string = 'desc'): Observable<PageResponseDTO<RegistrationRequestListDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    return this.http.get<PageResponseDTO<RegistrationRequestListDTO>>(`${this.baseUrl}registration-requests/all/paged`, { params });
  }

  getRequestById(id: number): Observable<RegistrationRequestDTO> {
    return this.http.get<RegistrationRequestDTO>(`${this.baseUrl}registration-requests/${id}`);
  }

  processRequest(id: number, dto: ProcessRequestDTO): Observable<RegistrationRequestDTO> {
    return this.http.put<RegistrationRequestDTO>(`${this.baseUrl}registration-requests/${id}/process`, dto);
  }

  getImagePresignedUrl(imageId: number): Observable<{ url: string; originalName: string }> {
    return this.http.get<{ url: string; originalName: string }>(`${this.baseUrl}files/image/${imageId}/url`);
  }

  getDocumentPresignedUrl(documentId: number): Observable<{ url: string; originalName: string; contentType: string }> {
    return this.http.get<{ url: string; originalName: string; contentType: string }>(`${this.baseUrl}files/document/${documentId}/url`);
  }

  getRequestFiles(requestId: number): Observable<{
    images: { id: number; originalName: string; url: string }[];
    documents: { id: number; originalName: string; contentType: string; url: string }[];
  }> {
    return this.http.get<{
      images: { id: number; originalName: string; url: string }[];
      documents: { id: number; originalName: string; contentType: string; url: string }[];
    }>(`${this.baseUrl}files/request/${requestId}/files`);
  }
}
