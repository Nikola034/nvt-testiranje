import { VehicleImageDTO } from './VehicleImageDTO';

export interface VehicleResponseDTO {
  id: number;
  licensePlate: string;
  weightLimit: number;
  brandId: number;
  brandName: string;
  modelId: number;
  modelName: string;
  createdAt: string;
  updatedAt: string;
  images: VehicleImageDTO[];
}
