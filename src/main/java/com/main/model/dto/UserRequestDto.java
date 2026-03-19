package com.main.model.dto;


import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDto {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;



    private String password;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Invalid phone number")
    private String phone;

    private String role;
}
