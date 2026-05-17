package com.amt_softwares.unnamed.menu.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FlagMenuItemRequest {

    @NotNull
    private Boolean flagged;

    @Size(max = 255)
    private String flaggedReason;
}
