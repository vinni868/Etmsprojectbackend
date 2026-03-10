package com.lms.controller;

import java.util.*;
import java.util.stream.Collectors;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import com.lms.entity.*;
import com.lms.repository.*;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class StudentController {

    @Autowired
    private StudentBatchesRepository studentBatchRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ScheduledClassRepository scheduledClassRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ----------------- COMMON METHOD -----------------
    private Long getLoggedInStudentId(HttpSession session) {
        if (session == null)
            throw new RuntimeException("Session expired. Please login again.");
        Object userIdObj = session.getAttribute("USER_ID");
        if (userIdObj != null)
            return Long.valueOf(userIdObj.toString());
        Object userObj = session.getAttribute("user");
        if (userObj != null && userObj instanceof User)
            return ((User) userObj).getId();
        throw new RuntimeException("User not logged in. Please login again.");
    }

    // ----------------- MY COURSES -----------------
    @GetMapping("/my-courses")
    public ResponseEntity<?> getMyCourses(HttpSession session) {
        try {
            Long studentId = getLoggedInStudentId(session);
            List<Map<String, Object>> courseList = studentBatchRepository.findByStudent_Id(studentId)
                    .stream().map(sb -> {
                        Map<String, Object> map = new HashMap<>();
                        Batches batch = sb.getBatch();
                        CourseMaster course = batch.getCourse();

                        map.put("id", course.getId());
                        map.put("courseName", course.getCourseName());
                        map.put("description", course.getDescription());
                        map.put("duration", course.getDuration());
                        map.put("syllabusFileName", course.getSyllabusFileName());

                        map.put("batchId", batch.getId());
                        map.put("batchName", batch.getBatchName());
                        map.put("batchStatus", sb.getStatus());

                        return map;
                    }).collect(Collectors.toList());

            return ResponseEntity.ok(courseList);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // ----------------- STUDENT ATTENDANCE -----------------
    @GetMapping("/attendance/details/{studentId}")
    public ResponseEntity<?> getStudentAttendance(
            @PathVariable Long studentId,
            @RequestParam Long batchId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        try {
            String sql = "SELECT id, student_id, batch_id, attendance_date, status, topic " +
                         "FROM trainer_marked_attendance " +
                         "WHERE student_id = ? AND batch_id = ?";
            List<Object> params = new ArrayList<>();
            params.add(studentId);
            params.add(batchId);

            if (from != null && !from.isEmpty()) {
                sql += " AND attendance_date >= ?";
                params.add(java.sql.Date.valueOf(from));
            }
            if (to != null && !to.isEmpty()) {
                sql += " AND attendance_date <= ?";
                params.add(java.sql.Date.valueOf(to));
            }

            sql += " ORDER BY attendance_date DESC";

            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, params.toArray());

            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ----------------- DOWNLOAD ATTENDANCE CSV -----------------
    @GetMapping("/attendance/download/{studentId}")
    public ResponseEntity<?> downloadAttendanceReport(
            @PathVariable Long studentId,
            @RequestParam Long batchId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        try {
            // Fetch attendance data directly from DB
            String sql = "SELECT attendance_date, topic, status FROM trainer_marked_attendance " +
                         "WHERE student_id = ? AND batch_id = ?";
            List<Object> params = new ArrayList<>();
            params.add(studentId);
            params.add(batchId);

            if (from != null && !from.isEmpty()) {
                sql += " AND attendance_date >= ?";
                params.add(java.sql.Date.valueOf(from));
            }
            if (to != null && !to.isEmpty()) {
                sql += " AND attendance_date <= ?";
                params.add(java.sql.Date.valueOf(to));
            }
            sql += " ORDER BY attendance_date DESC";

            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, params.toArray());

            // Build CSV content
            StringBuilder csv = new StringBuilder();
            csv.append("Date,Topic,Status\n");
            for (Map<String, Object> r : records) {
                csv.append(r.get("attendance_date")).append(",");
                csv.append(r.get("topic") != null ? r.get("topic") : "").append(",");
                csv.append(r.get("status")).append("\n");
            }

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"attendance.csv\"")
                    .body(csv.toString());

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}