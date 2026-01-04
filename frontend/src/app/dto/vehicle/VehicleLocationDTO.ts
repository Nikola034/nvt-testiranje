export interface VehicleLocationDTO {
  vehicleId: number;
  licensePlate: string;
  latitude: number;
  longitude: number;
  totalDistance: number;
  lastHeartbeat: string;
  lastTelemetryUpdate: string;
  online: boolean;
  currentState: string;
}
