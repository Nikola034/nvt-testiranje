package nvt.backend.model.user;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@DiscriminatorValue("Manager")
@Data
public class Manager extends User {

}
