package com.lms.repository;

import com.lms.entity.ScheduledClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledClassRepository extends JpaRepository<ScheduledClass, Long> {

    // existing method (keep this)
    List<ScheduledClass> findByBatchIdAndClassDate(Long batchId, LocalDate classDate);

    // ✅ ADD THIS METHOD
    Optional<ScheduledClass> findByBatchIdAndClassDateAndTrainerIdAndStatus(
            Long batchId,
            LocalDate classDate,
            Long trainerId,
            String status
    );
    List<ScheduledClass> findByCourseIdAndBatchId(Long courseId, Long batchId);
}