
package com.securevault.secure_vault.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest (@NotBlank String username, @NotBlank String password){

}
