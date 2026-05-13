package com.amt_softwares.unnamed.admin.places.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminPlaceReviewRequest {

    @NotBlank
    private String approvalStatus;

    @Size(max = 2000)
    private String reviewComments;
}
