package com.course.app.validation;

import java.time.LocalDateTime;

public interface TimeRangeSource {

    LocalDateTime getStartTime();

    LocalDateTime getEndTime();
}
