export interface RegistrationRequestListDTO {
  id: number;
  companyName: string;
  countryName: string;
  cityName: string;
  street: string;
  streetNumber: string;
  latitude: number;
  longitude: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  rejectionReason?: string;
  createdAt: string;
  processedAt?: string;
  ownerName: string;
  ownerEmail: string;
}
