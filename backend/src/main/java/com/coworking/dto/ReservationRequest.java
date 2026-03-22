package com.coworking.dto;

import lombok.Data;

@Data
public class ReservationRequest {
    private Long spaceId;
    private String startDateTime;
    private String endDateTime;
    private String type;
    private String itemId; // name of office or conference room
}
