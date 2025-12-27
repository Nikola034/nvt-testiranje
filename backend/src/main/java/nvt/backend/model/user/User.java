package nvt.backend.model.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.auth.Role;
import nvt.backend.model.common.Address;

import java.util.Date;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // All subclasses stored in one table
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@Table(name = "\"user\"")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @TableGenerator(
            name = "user_gen",
            table = "id_generator",
            pkColumnName = "sequence_name",
            valueColumnName = "next_val"
    )
    @GeneratedValue(strategy = GenerationType.TABLE,generator = "user_gen")
    private int id;

    private String name;
    private String surname;
    private String phoneNumber;
    private String username;
    private String password;
    private String photo;
    private Role role;
    private String authorities;

    private boolean active = false;
    private String activationToken;
    private Date tokenExpiration;
}
