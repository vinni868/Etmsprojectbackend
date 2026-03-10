package com.lms.repository;

import com.lms.entity.StudentBatches;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentBatchesRepository extends JpaRepository<StudentBatches, Long> {

    Optional<StudentBatches> findByStudent_IdAndBatch_Id(Long studentId, Long batchId);

    void deleteByStudent_IdAndBatch_Id(Long studentId, Long batchId);

    List<StudentBatches> findByStudent_Id(Long studentId);

    List<StudentBatches> findByBatch_Id(Long batchId);
    
    
}