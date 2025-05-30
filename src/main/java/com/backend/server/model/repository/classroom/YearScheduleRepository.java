package com.backend.server.model.repository.classroom;

import com.backend.server.model.entity.classroom.YearSchedule;
import io.lettuce.core.dynamic.annotation.Param;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface YearScheduleRepository extends JpaRepository<YearSchedule, Long> {

    @Query("SELECT y FROM YearSchedule y " +
            "JOIN FETCH y.classroom " +
            "WHERE y.date BETWEEN :startDate AND :endDate " +
            "ORDER BY y.date ASC")
    List<YearSchedule> findWithClassroomBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}