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
    private String organization;
    private String accessToken;
    private String refreshToken;

    public RegisterResponseDTO(int id, String message, String username, String name, String surname, String organization, String accessToken, String refreshToken) {
        this.id = id;
        this.message = message;
        this.username = username;
        this.name = name;
        this.surname = surname;
        this.organization = organization;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}