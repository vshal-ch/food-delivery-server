package com.amt_softwares.unnamed.menu.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetMenuItemAvailabilityRequest {

    @NotNull
    private Boolean available;
}
