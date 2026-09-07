
package com.securevault.secure_vault.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest (
        @NotBlank @Size(min = 4, max = 30) String username,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        @Email @NotBlank String email
) 
{

}
