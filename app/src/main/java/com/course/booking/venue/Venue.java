package com.course.booking.venue;

import com.course.booking.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Venue extends BaseEntity {
    private String name;
    private String address;
    private String city;
}
