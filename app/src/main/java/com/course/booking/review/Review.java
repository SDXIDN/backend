package com.course.booking.review;

import com.course.booking.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review extends BaseEntity {
    private Long roomId;
    private Long customerId;
    private Integer rating;
    private String comment;
}
