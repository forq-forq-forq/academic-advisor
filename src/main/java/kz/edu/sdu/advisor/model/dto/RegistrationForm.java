package kz.edu.sdu.advisor.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationForm {

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 254, message = "Email must be 254 characters or fewer.")
    @Pattern(regexp = "(?i)^[A-Z0-9._%+-]+@sdu\\.edu\\.kz$", message = "Use a university email ending in @sdu.edu.kz.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d])[\\x21-\\x7E]{12,64}$",
            message = "Use 12-64 characters with uppercase, lowercase, a number, and a symbol.")
    private String password;

    @NotBlank(message = "Confirm your password.")
    private String confirmPassword;
}