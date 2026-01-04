export interface UpdateVehicleDTO {
  licensePlate: string;
  weightLimit: number;
  brandId: number;
  modelId: number;
  imagesToDelete: number[];
}
