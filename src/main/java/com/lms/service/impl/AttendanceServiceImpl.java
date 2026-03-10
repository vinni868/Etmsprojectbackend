package com.lms.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.dto.AttendanceHistoryDTO;
import com.lms.entity.TrainerMarkedAttendance;
import com.lms.repository.AttendanceRepository;
import com.lms.service.AttendanceService;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    /**
     * Handles both saving new records and updating existing ones.
     * JPA saveAll checks if the 'id' field is present. 
     * If id exists, it performs an UPDATE. If id is null, it performs an INSERT.
     */
    @Override
    @Transactional
    public void saveBulkAttendance(List<TrainerMarkedAttendance> attendanceList) {
        if (attendanceList != null && !attendanceList.isEmpty()) {
            attendanceRepository.saveAll(attendanceList);
        }
    }

    /**
     * Retrieves existing records for a specific batch and date.
     * Updated to return Map<String, Object> to include Student Name and Email 
     * fetched via the JOIN query in the repository.
     */
    @Override
    public List<Map<String, Object>> getExistingAttendance(Integer batchId, LocalDate date) {
        // This calls the new JOIN query we added to the Repository
        return attendanceRepository.findExistingAttendanceWithDetails(batchId, date);
    }

    /**
     * Retrieves historical attendance records for a specific batch and date range.
     */
    @Override
    public List<AttendanceHistoryDTO> getAttendanceHistory(Integer batchId, LocalDate from, LocalDate to) {
        return attendanceRepository.findAttendanceHistory(batchId, from, to);
    }

    /**
     * Fetches all attendance records marked for a specific student.
     */
    @Override
    public List<TrainerMarkedAttendance> getAttendanceByStudentId(Integer studentId) {
        if (studentId == null) {
            return List.of();
        }
        return attendanceRepository.findByStudentId(studentId);
    }
    @Override
    public List<TrainerMarkedAttendance> getAttendanceByStudentAndBatch(
            Integer studentId,
            Integer batchId) {

        if (studentId == null || batchId == null) {
            return List.of();
        }

        return attendanceRepository
                .findByStudentIdAndBatchId(studentId, batchId);
    }
}