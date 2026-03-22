package com.coworking.dto;

import lombok.Data;

@Data
public class RescheduleRequest {
    private String startDateTime;
    private String endDateTime;
}
