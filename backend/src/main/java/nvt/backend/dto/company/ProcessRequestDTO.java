package nvt.backend.dto.company;

import lombok.Data;

@Data
public class ProcessRequestDTO {
    private boolean approved;
    private String rejectionReason;
}
