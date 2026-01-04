package nvt.backend.controllers.vehicle;

import lombok.RequiredArgsConstructor;
import nvt.backend.services.vehicle.VehicleAvailabilityWebSocketService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class VehicleAvailabilityWebSocketController {

    private final VehicleAvailabilityWebSocketService availabilityWebSocketService;

    @MessageMapping("/vehicle/{vehicleId}/availability/subscribe")
    public void subscribeToAvailability(@DestinationVariable Long vehicleId) {
        availabilityWebSocketService.subscribeToVehicle(vehicleId);
    }

    @MessageMapping("/vehicle/{vehicleId}/availability/unsubscribe")
    public void unsubscribeFromAvailability(@DestinationVariable Long vehicleId) {
        availabilityWebSocketService.unsubscribeFromVehicle(vehicleId);
    }
}
