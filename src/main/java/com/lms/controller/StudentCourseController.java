package com.lms.controller;

import com.lms.entity.CourseMaster;
import com.lms.repository.CourseRepository;
import com.lms.repository.StudentBatchesRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
// Match your React port and allow session cookies
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class StudentCourseController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentBatchesRepository studentBatchRepository;

    // ================= HELPER METHOD =================
    // This pulls the ID from the session created in your AuthController
    private Long getLoggedInStudentId(HttpSession session) {
        Object userIdObj = session.getAttribute("USER_ID");
        if (userIdObj == null) {
            throw new RuntimeException("User not logged in. Please login again.");
        }
        return Long.valueOf(userIdObj.toString());
    }

    // ================= 1. GET MY ASSIGNED COURSES =================
    // Logic: Student -> StudentBatches -> Batch -> Course
    @GetMapping("/my-assigned")
    public ResponseEntity<?> getMyAssignedCourses(HttpSession session) {
        try {
            Long studentId = getLoggedInStudentId(session);

            List<CourseMaster> courses = studentBatchRepository.findByStudent_Id(studentId)
                    .stream()
                    .map(sb -> sb.getBatch().getCourse())
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (courses.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(courses);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // ================= 2. GET SINGLE COURSE DETAILS =================
    @GetMapping("/{id}")
    public ResponseEntity<CourseMaster> getCourseById(@PathVariable Long id, HttpSession session) {
        // Optional: verify user is logged in before showing details
        getLoggedInStudentId(session); 
        
        Optional<CourseMaster> course = courseRepository.findById(id);
        return course.map(ResponseEntity::ok)
                     .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ================= 3. GET ALL AVAILABLE COURSES (Optional) =================
    @GetMapping("/all")
    public ResponseEntity<List<CourseMaster>> getAllCourses() {
        return ResponseEntity.ok(courseRepository.findAll());
    }
}