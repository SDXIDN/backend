package com.course.booking.room;

import com.course.booking.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Room extends BaseEntity {
    private String name;
    private Integer capacity;
    private BigDecimal hourlyPrice;
    private Long venueId;
}
