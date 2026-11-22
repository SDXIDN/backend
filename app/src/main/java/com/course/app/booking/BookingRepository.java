package com.course.app.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

    @Query("select b from BookingEntity b "
            + "where b.reminderSent = false and b.status in :statuses "
            + "and b.startTime > :from and b.startTime <= :to")
    List<BookingEntity> findDueForReminder(@Param("statuses") Collection<BookingStatus> statuses,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    @Modifying(clearAutomatically = true)
    @Query("update BookingEntity b set b.status = :newStatus "
            + "where b.status = :oldStatus and b.endTime < :threshold")
    int updateStatusForEndedBefore(@Param("oldStatus") BookingStatus oldStatus,
                                   @Param("newStatus") BookingStatus newStatus,
                                   @Param("threshold") LocalDateTime threshold);
}
