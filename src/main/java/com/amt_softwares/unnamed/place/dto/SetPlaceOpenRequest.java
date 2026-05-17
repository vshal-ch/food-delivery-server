package com.amt_softwares.unnamed.place.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetPlaceOpenRequest {

    @NotNull
    private Boolean open;
}
