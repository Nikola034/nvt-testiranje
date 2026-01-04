export interface DistanceDataPoint {
  label: string;
  startDate: string;
  endDate: string;
  distance: number;
}

export interface DistanceStatisticsDTO {
  vehicleId: number;
  licensePlate: string;
  startDate: string;
  endDate: string;
  aggregationType: string;
  totalDistance: number;
  dataPoints: DistanceDataPoint[];
}
