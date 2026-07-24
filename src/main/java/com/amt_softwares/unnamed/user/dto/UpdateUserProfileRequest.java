package com.amt_softwares.unnamed.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {

    @Size(max = 100)
    private String name;

    @Email
    @Size(max = 100)
    private String email;
}
