package com.lms.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import com.lms.dto.TrainerBatchDTO;
import com.lms.dto.TrainerCourseDTO;
import com.lms.dto.TrainerProfileDTO;
import com.lms.dto.TrainerStudentDTO;

import com.lms.service.AttendanceService;
import com.lms.service.TrainerService;

@RestController
@RequestMapping("/api/teacher")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class TrainerController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private TrainerService trainerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // =====================================================
    // ✅ TRAINER PROFILE
    // =====================================================

    @GetMapping("/profile/{email}")
    public TrainerProfileDTO getProfile(@PathVariable String email) {
        return trainerService.getProfile(email);
    }

    @PutMapping("/profile/{email}")
    public String updateProfile(@PathVariable String email,
                                @RequestBody TrainerProfileDTO dto) {

        trainerService.updateProfile(email, dto);
        return "Profile Updated Successfully";
    }

   

    // =========================
    // DASHBOARD STATS
    // =========================

    @GetMapping("/dashboard/{trainerId}")
    public Map<String, Object> getDashboard(@PathVariable int trainerId) {

        Map<String, Object> data = new HashMap<>();

        Integer totalBatches = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM batches WHERE trainer_id = ?",
                Integer.class, trainerId);

        Integer totalStudents = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT sb.student_id) " +
                        "FROM student_batches sb " +
                        "JOIN batches b ON sb.batch_id = b.id " +
                        "WHERE b.trainer_id = ? AND sb.status = 'ACTIVE'",
                Integer.class, trainerId);

        Integer todayClasses = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM scheduled_classes " +
                        "WHERE trainer_id = ? AND class_date = CURDATE()",
                Integer.class, trainerId);

        data.put("totalBatches", totalBatches);
        data.put("totalStudents", totalStudents);
        data.put("todayClasses", todayClasses);

        return data;
    }

    // =========================
    // TRAINER SCHEDULE
    // =========================

    @GetMapping("/schedule/{trainerId}")
    public List<Map<String, Object>> getSchedule(@PathVariable int trainerId) {

        return jdbcTemplate.queryForList(
                "SELECT sc.id, sc.class_date, sc.start_time, sc.end_time, " +
                        "b.batch_name, c.course_name " +
                        "FROM scheduled_classes sc " +
                        "JOIN batches b ON sc.batch_id = b.id " +
                        "JOIN course_master c ON sc.course_id = c.id " +
                        "WHERE sc.trainer_id = ? " +
                        "ORDER BY sc.class_date DESC",
                trainerId);
    }

    // =======================
    // TRAINER COURSES
    // =======================

    @GetMapping("/courses/{trainerId}")
    public List<TrainerCourseDTO> getCourses(@PathVariable Integer trainerId) {
        return trainerService.getTrainerCourses(trainerId);
    }

    @GetMapping("/courses/{trainerId}/{courseId}/batches")
    public List<TrainerBatchDTO> getBatches(
            @PathVariable Integer trainerId,
            @PathVariable Integer courseId) {
        return trainerService.getBatchesByCourse(trainerId, courseId);
    }

    @GetMapping("/batches/{batchId}/students")
    public List<TrainerStudentDTO> getStudents(@PathVariable Integer batchId) {
        return trainerService.getStudentsByBatch(batchId);
    }

    @GetMapping("/students/{trainerId}")
    public List<TrainerStudentDTO> getAllStudents(@PathVariable Integer trainerId) {
        return trainerService.getAllStudentsUnderTrainer(trainerId);
    }
}