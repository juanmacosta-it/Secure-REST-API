package com.securevault.secure_vault.entity;

// Enum en vez de String suelto: evita errores de tipeo y le da al compilador la 
// capacidad de detectar roles inválidos en tiempo de compilación, no en runtime

public enum Role {
     ADMIN,
    CLIENT

}
