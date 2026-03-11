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
    
 // =========================
 // DASHBOARD STATS
 // =========================
 @GetMapping("/dashboard/{trainerId}")
 public Map<String, Object> getDashboard(@PathVariable int trainerId) {
     Map<String, Object> data = new HashMap<>();

     Integer totalCourses = jdbcTemplate.queryForObject(
             "SELECT COUNT(DISTINCT course_id) FROM batches WHERE trainer_id = ?",
             Integer.class, trainerId);

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
    	        "WHERE trainer_id = ? " +
    	        "AND status = 'ACTIVE' " +
    	        "AND CURDATE() BETWEEN class_date AND end_date",
    	        Integer.class, trainerId);

     data.put("totalCourses", totalCourses);
     data.put("totalBatches", totalBatches);
     data.put("totalStudents", totalStudents);
     data.put("todayClasses", todayClasses);

     return data;
 }

//=========================
//TRAINER SCHEDULE (TODAY + UPCOMING)
//WITH PAGINATION
//=========================
@GetMapping("/schedule/{trainerId}")
public Map<String, Object> getSchedule(
      @PathVariable int trainerId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "5") int size) {

  int offset = page * size;

  // MAIN QUERY
  List<Map<String, Object>> data = jdbcTemplate.queryForList(

      "SELECT sc.id, " +
      "DATE_ADD(sc.class_date, INTERVAL seq.n DAY) AS class_date, " +
      "sc.start_time, sc.end_time, " +
      "b.batch_name, c.course_name " +

      "FROM scheduled_classes sc " +

      // NUMBER GENERATOR (0-365 days)
      "JOIN ( " +
      " SELECT a.N + b.N * 10 + c.N * 100 AS n " +
      " FROM (SELECT 0 N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a " +
      " CROSS JOIN (SELECT 0 N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b " +
      " CROSS JOIN (SELECT 0 N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) c " +
      ") seq " +

      "JOIN batches b ON sc.batch_id = b.id " +
      "JOIN course_master c ON sc.course_id = c.id " +

      "WHERE sc.trainer_id = ? " +
      "AND sc.status = 'ACTIVE' " +

      // GENERATE DATE RANGE
      "AND DATE_ADD(sc.class_date, INTERVAL seq.n DAY) <= sc.end_date " +

      // TODAY + FUTURE
      "AND DATE_ADD(sc.class_date, INTERVAL seq.n DAY) >= CURDATE() " +

      "ORDER BY class_date ASC, sc.start_time ASC " +
      "LIMIT ? OFFSET ?",

      trainerId, size, offset
  );

  // TOTAL COUNT
  Integer total = jdbcTemplate.queryForObject(

      "SELECT COUNT(*) FROM ( " +
      "SELECT DATE_ADD(sc.class_date, INTERVAL seq.n DAY) AS class_date " +
      "FROM scheduled_classes sc " +

      "JOIN ( " +
      " SELECT a.N + b.N * 10 + c.N * 100 AS n " +
      " FROM (SELECT 0 N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a " +
      " CROSS JOIN (SELECT 0 N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b " +
      " CROSS JOIN (SELECT 0 N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) c " +
      ") seq " +

      "WHERE sc.trainer_id = ? " +
      "AND sc.status = 'ACTIVE' " +
      "AND DATE_ADD(sc.class_date, INTERVAL seq.n DAY) <= sc.end_date " +
      "AND DATE_ADD(sc.class_date, INTERVAL seq.n DAY) >= CURDATE() " +
      ") x",

      Integer.class,
      trainerId
  );

  Map<String, Object> response = new HashMap<>();
  response.put("content", data);
  response.put("total", total);
  response.put("page", page);
  response.put("size", size);

  return response;
}
}