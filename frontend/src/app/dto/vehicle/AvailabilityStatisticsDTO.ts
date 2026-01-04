export interface AvailabilityDataPoint {
  label: string;
  startTime: string;
  endTime: string;
  onlineSeconds: number;
  offlineSeconds: number;
  onlinePercentage: number;
  offlinePercentage: number;
}

export interface AvailabilityStatisticsDTO {
  vehicleId: number;
  licensePlate: string;
  startTime: string;
  endTime: string;
  aggregationType: string;
  totalOnlineSeconds: number;
  totalOfflineSeconds: number;
  onlinePercentage: number;
  offlinePercentage: number;
  dataPoints: AvailabilityDataPoint[];
}
