package com.amt_softwares.unnamed.admin.menu.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FlagMenuItemRequest {

    @NotNull
    private String flaggedStatus;

    @Size(max = 255)
    private String flaggedReason;
}
