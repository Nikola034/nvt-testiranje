package nvt.backend.dto.user.auth;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RegisterResponseDTO {
    private int id;
    private String message;
    private String username;
    private String name;
    private String surname;
    private String phoneNumber;
    private String accessToken;
    private String refreshToken;

    public RegisterResponseDTO(int id, String message, String username, String name, String surname, String phoneNumber, String accessToken, String refreshToken) {
        this.id = id;
        this.message = message;
        this.username = username;
        this.name = name;
        this.surname = surname;
        this.phoneNumber = phoneNumber;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}