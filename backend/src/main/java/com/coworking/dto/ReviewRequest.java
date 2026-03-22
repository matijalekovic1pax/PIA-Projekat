package com.coworking.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private Long spaceId;
    private String comment;
    private Boolean isLike;
}
