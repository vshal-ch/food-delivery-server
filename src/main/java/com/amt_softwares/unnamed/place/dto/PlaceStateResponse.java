package com.amt_softwares.unnamed.place.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlaceStateResponse {
    private PlaceResponse approvedDetails;
    private PlaceResponse unapprovedDetails;
}
