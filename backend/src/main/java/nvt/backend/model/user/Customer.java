package nvt.backend.model.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@DiscriminatorValue("Customer")
public class Customer extends User {
    // Additional properties specific to AuthenticatedUser

    // Getters and Setters
}