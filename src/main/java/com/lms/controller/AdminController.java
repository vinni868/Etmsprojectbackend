package com.lms.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.lms.service.AdminService;

import com.lms.dto.*;
import com.lms.entity.*;
import com.lms.enums.Status;
import com.lms.repository.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {
	@Autowired
	private AttendanceRepository attendanceRepository;

	
    @Autowired
    private AdminService adminService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseTrainerRepository courseTrainerRepository;
    
    @Autowired
    private ScheduledClassRepository scheduledClassRepository;
    @Autowired
    private RoleRepository roleRepository;

    // ================= DASHBOARD =================
    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        return adminService.getDashboardData();
    }

    @PostMapping("/course")
    public ResponseEntity<?> createCourse(
            @RequestParam("courseName") String courseName,
            @RequestParam("duration") String duration,
            @RequestParam("description") String description,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        try {

            CourseMaster course = new CourseMaster();
            course.setCourseName(courseName);
            course.setDuration(duration);
            course.setDescription(description);
            course.setStatus("ACTIVE");

            // ===== UPDATED FILE UPLOAD LOGIC =====
            if (file != null && !file.isEmpty()) {

                String uploadDir = System.getProperty("user.dir") + "/uploads/syllabus/";

                java.io.File directory = new java.io.File(uploadDir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                String filePath = uploadDir + fileName;

                file.transferTo(new java.io.File(filePath));

                course.setSyllabusFileName(fileName);
                course.setSyllabusFilePath(filePath);
            }

            courseRepository.save(course);

            return ResponseEntity.ok("Course Created Successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/courses")
    public List<CourseMaster> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .filter(course -> "ACTIVE".equals(course.getStatus()))
                .toList();
    }

    // ================= USERS =================
    @GetMapping("/students")
    public List<User> getAllStudents() {
        return userRepository.findByRole_RoleNameAndStatus("STUDENT", Status.ACTIVE);
    }

    @GetMapping("/trainers")
    public List<User> getAllActiveTrainers() {
        return userRepository.findByRole_RoleNameAndStatus("TRAINER", Status.ACTIVE);
    }

    @GetMapping("/all-users")
    public List<User> getAllUsers() {
        return userRepository.findByRole_RoleName("STUDENT");
    }

    @PutMapping("/approve-user/{id}")
    public String approveUser(@PathVariable Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() == Status.ACTIVE) {
            return "User is already approved";
        }

        user.setStatus(Status.ACTIVE);
        userRepository.save(user);

        return "User Approved Successfully";
    }
    // ================= BATCH =================
    @PostMapping("/create-batch")
    public ResponseEntity<?> createBatch(@RequestBody Map<String, Object> payload) {
        try {

            Batches batch = new Batches();
            batch.setBatchName(payload.get("batchName").toString());
            batch.setStatus("ONGOING");

            Long courseId = Long.valueOf(payload.get("courseId").toString());
            CourseMaster course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            batch.setCourse(course);

            Long trainerId = Long.valueOf(payload.get("trainerId").toString());
            User trainer = userRepository.findById(trainerId)
                    .orElseThrow(() -> new RuntimeException("Trainer not found"));
            batch.setTrainer(trainer);

            batch.setStartDate(java.time.LocalDate.parse(payload.get("startDate").toString()));
            batch.setEndDate(java.time.LocalDate.parse(payload.get("endDate").toString()));

            batchRepository.save(batch);

            return ResponseEntity.ok("Batch created successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/batches")
    public List<Batches> getAllBatches() {
        return batchRepository.findAll();
    }

 

    // ================= ASSIGNMENTS =================
    @GetMapping("/batch-assignments")
    public List<Map<String, Object>> getBatchAssignments() {

        return batchRepository.findAll().stream().map(batch -> {

            Map<String, Object> map = new HashMap<>();
            map.put("batchId", batch.getId());
            map.put("batchName", batch.getBatchName());
            map.put("courseName", batch.getCourse().getCourseName());

            if (batch.getTrainer() != null) {
                map.put("trainerName", batch.getTrainer().getName());
                map.put("trainerEmail", batch.getTrainer().getEmail());
            } else {
                map.put("trainerName", "Not Assigned");
                map.put("trainerEmail", "-");
            }

            return map;

        }).collect(Collectors.toList());
    }

    @GetMapping("/all-assignments")
    public List<Map<String, Object>> getAllAssignments() {

        return courseTrainerRepository.findAll().stream().map(ct -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ct.getId());
            map.put("courseName", ct.getCourse().getCourseName());
            map.put("trainerName", ct.getTrainer().getName());
            map.put("trainerEmail", ct.getTrainer().getEmail());
            return map;
        }).collect(Collectors.toList());
    }
 // ================= COURSE DETAILS (FIXED) =================
    @GetMapping("/courses/details")
    public List<Map<String, Object>> getAllCoursesDetails() {

        return courseRepository.findAll().stream().map(course -> {

            Map<String, Object> courseMap = new HashMap<>();
            courseMap.put("id", course.getId());
            courseMap.put("courseName", course.getCourseName());
            courseMap.put("description", course.getDescription());
            courseMap.put("duration", course.getDuration());

            List<Map<String, Object>> batchList =
                    batchRepository.findByCourse_Id(course.getId())
                            .stream()
                            .map(batch -> {

                                Map<String, Object> batchMap = new HashMap<>();
                                batchMap.put("batchId", batch.getId());
                                batchMap.put("batchName", batch.getBatchName());

                                if (batch.getTrainer() != null) {
                                    batchMap.put("trainerName", batch.getTrainer().getName());
                                    batchMap.put("trainerEmail", batch.getTrainer().getEmail());
                                }

                                // Students
                                List<String> students = batch.getStudentBatches() == null
                                        ? new ArrayList<>()
                                        : batch.getStudentBatches().stream()
                                                .map(sb -> sb.getStudent().getName())
                                                .toList();

                                batchMap.put("students", students);

                                return batchMap;

                            }).toList();

            courseMap.put("batches", batchList);

            return courseMap;

        }).toList();
    }
    

   
    	//================= SCHEDULED CLASSES (UPDATED) =================
    @GetMapping("/schedule-classes")
    public List<Map<String, Object>> getAllScheduledClasses() {

     return scheduledClassRepository.findAll().stream().map(schedule -> {

         Map<String, Object> map = new HashMap<>();

         map.put("id", schedule.getId());

         map.put("startDate", schedule.getClassDate());

         /* FIXED */
         map.put("endDate", schedule.getEndDate());

         map.put("startTime", schedule.getStartTime());
         map.put("endTime", schedule.getEndTime());

         map.put("status", schedule.getStatus());

         CourseMaster course = courseRepository.findById(schedule.getCourseId()).orElse(null);
         Batches batch = batchRepository.findById(schedule.getBatchId()).orElse(null);
         User trainer = userRepository.findById(schedule.getTrainerId()).orElse(null);

         map.put("course", Map.of(
                 "id", course != null ? course.getId() : 0,
                 "course_name", course != null ? course.getCourseName() : "N/A"
         ));

         map.put("batch", Map.of(
                 "id", batch != null ? batch.getId() : 0,
                 "batch_name", batch != null ? batch.getBatchName() : "N/A"
         ));

         map.put("trainer", Map.of(
                 "id", trainer != null ? trainer.getId() : 0,
                 "trainer_name", trainer != null ? trainer.getName() : "N/A"
         ));

         return map;

     }).collect(Collectors.toList());
    }
 // ================= ASSIGN TRAINER TO BATCH =================
    @PostMapping("/assign-trainer-batch")
    public ResponseEntity<?> assignTrainerToBatch(@RequestBody Map<String, Long> payload) {
        try {

            Long batchId = payload.get("batchId");
            Long trainerId = payload.get("trainerId");

            Batches batch = batchRepository.findById(batchId)
                    .orElseThrow(() -> new RuntimeException("Batch not found"));

            User trainer = userRepository.findById(trainerId)
                    .orElseThrow(() -> new RuntimeException("Trainer not found"));

            batch.setTrainer(trainer);
            batchRepository.save(batch);

            return ResponseEntity.ok("Trainer assigned successfully");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PutMapping("/reject-user/{id}")
    public String rejectUser(@PathVariable Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() == Status.REJECTED) {
            return "User is already rejected";
        }

        user.setStatus(Status.REJECTED);
        userRepository.save(user);

        return "User Rejected Successfully";
    }
 // ================= BATCH CRUD OPERATIONS =================



    @PutMapping("/schedule-classes/{id}")
    public ResponseEntity<?> updateSchedule(@PathVariable Long id,
                                            @RequestBody Map<String, Object> payload) {

        try {

            ScheduledClass schedule = scheduledClassRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Schedule not found"));

            Long courseId = Long.parseLong(payload.get("courseId").toString());
            Long batchId = Long.parseLong(payload.get("batchId").toString());

            LocalDate startDate = LocalDate.parse(payload.get("startDate").toString());
            LocalDate endDate = LocalDate.parse(payload.get("endDate").toString());

            LocalTime startTime = LocalTime.parse(payload.get("startTime").toString());
            LocalTime endTime = LocalTime.parse(payload.get("endTime").toString());

            // 🔴 CHECK FOR CONFLICT (IGNORE CURRENT ID)
            List<ScheduledClass> existingSchedules =
                    scheduledClassRepository.findByCourseIdAndBatchId(courseId, batchId);

            for (ScheduledClass existing : existingSchedules) {

                if (existing.getId().equals(id)) continue;

                boolean dateOverlap =
                        !(endDate.isBefore(existing.getClassDate())
                        || startDate.isAfter(existing.getEndDate()));

                boolean timeOverlap =
                        !(endTime.isBefore(existing.getStartTime())
                        || startTime.isAfter(existing.getEndTime()));

                if (dateOverlap && timeOverlap) {

                    return ResponseEntity.badRequest()
                            .body("Schedule conflict: Class already exists for this date and time.");
                }
            }

            schedule.setCourseId(courseId);
            schedule.setBatchId(batchId);

            if (payload.get("trainerId") != null) {
                schedule.setTrainerId(Long.parseLong(payload.get("trainerId").toString()));
            }

            schedule.setClassDate(startDate);
            schedule.setEndDate(endDate);

            schedule.setStartTime(startTime);
            schedule.setEndTime(endTime);

            String newStatus = payload.get("status").toString().toUpperCase();
            schedule.setStatus(newStatus);

            scheduledClassRepository.save(schedule);

            return ResponseEntity.ok("Schedule updated successfully");

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(e.getMessage());

        }
    }
 // 3. TOGGLE STATUS (PATCH)
 @PatchMapping("/batches/{id}/status")
 public ResponseEntity<?> updateBatchStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
     try {
         Batches batch = batchRepository.findById(id)
                 .orElseThrow(() -> new RuntimeException("Batch not found"));
         
         batch.setStatus(payload.get("status"));
         batchRepository.save(batch);
         return ResponseEntity.ok("Status updated");
     } catch (Exception e) {
         return ResponseEntity.badRequest().body(e.getMessage());
     }
 }
//================= BATCH CRUD OPERATIONS =================

//1. UPDATE BATCH (Including Status)
@PutMapping("/batches/{id}")
public ResponseEntity<?> updateBatch(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
  try {
      Batches batch = batchRepository.findById(id)
              .orElseThrow(() -> new RuntimeException("Batch not found"));

      batch.setBatchName(payload.get("batchName").toString());
      
      // Sanitize status input to match Enum
      String statusInput = payload.get("status").toString().toUpperCase();
      batch.setStatus(statusInput); 

      batch.setStartDate(java.time.LocalDate.parse(payload.get("startDate").toString()));
      batch.setEndDate(java.time.LocalDate.parse(payload.get("endDate").toString()));

      Long courseId = Long.valueOf(payload.get("courseId").toString());
      CourseMaster course = courseRepository.findById(courseId)
              .orElseThrow(() -> new RuntimeException("Course not found"));
      batch.setCourse(course);

      Long trainerId = Long.valueOf(payload.get("trainerId").toString());
      User trainer = userRepository.findById(trainerId)
              .orElseThrow(() -> new RuntimeException("Trainer not found"));
      batch.setTrainer(trainer);

      batchRepository.save(batch);
      return ResponseEntity.ok("Batch updated successfully");
  } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error: " + e.getMessage());
  }
}

//2. SOFT DELETE BATCH (Mark as INACTIVE)
@DeleteMapping("/batches/{id}")
public ResponseEntity<?> softDeleteBatch(@PathVariable Long id) {
  try {
      Batches batch = batchRepository.findById(id)
              .orElseThrow(() -> new RuntimeException("Batch not found"));
      
      // Ensure INACTIVE is added to your MySQL ENUM list
      batch.setStatus("INACTIVE"); 
      batchRepository.save(batch);
      return ResponseEntity.ok("Batch marked as Inactive");
  } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error: " + e.getMessage());
  }
}
@PostMapping("/schedule-classes")
public ResponseEntity<?> scheduleClass(@RequestBody Map<String, String> payload) {

    try {

        Long courseId = Long.parseLong(payload.get("courseId"));
        Long batchId = Long.parseLong(payload.get("batchId"));
        Long trainerId = Long.parseLong(payload.get("trainerId"));

        LocalDate startDate = LocalDate.parse(payload.get("startDate"));
        LocalDate endDate = LocalDate.parse(payload.get("endDate"));

        LocalTime startTime = LocalTime.parse(payload.get("startTime"));
        LocalTime endTime = LocalTime.parse(payload.get("endTime"));

        // ================= DUPLICATE VALIDATION =================

        List<ScheduledClass> existingSchedules =
                scheduledClassRepository.findByCourseIdAndBatchId(courseId, batchId);

        for (ScheduledClass existing : existingSchedules) {

            LocalDate existingStart = existing.getClassDate();
            LocalDate existingEnd = existing.getEndDate();

            LocalTime existingStartTime = existing.getStartTime();
            LocalTime existingEndTime = existing.getEndTime();

            // DATE OVERLAP CHECK
            boolean dateOverlap =
                    (startDate.isEqual(existingStart) || startDate.isAfter(existingStart)) &&
                    (startDate.isBefore(existingEnd) || startDate.isEqual(existingEnd))
                    ||
                    (existingStart.isEqual(startDate) || existingStart.isAfter(startDate)) &&
                    (existingStart.isBefore(endDate) || existingStart.isEqual(endDate));

            // TIME OVERLAP CHECK
            boolean timeOverlap =
                    startTime.isBefore(existingEndTime) &&
                    endTime.isAfter(existingStartTime);

            if (dateOverlap && timeOverlap) {

                return ResponseEntity.badRequest()
                        .body("Duplicate schedule not allowed: This batch already has a class scheduled for this date and time.");

            }
        }

        // ================= SAVE SCHEDULE =================

        ScheduledClass schedule = new ScheduledClass();

        schedule.setCourseId(courseId);
        schedule.setBatchId(batchId);
        schedule.setTrainerId(trainerId);

        schedule.setClassDate(startDate);
        schedule.setEndDate(endDate);

        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);

        schedule.setStatus("ACTIVE");

        scheduledClassRepository.save(schedule);

        return ResponseEntity.ok("Class scheduled successfully");

    } catch (Exception e) {

        return ResponseEntity.badRequest().body(e.getMessage());

    }
}



//================= SOFT DELETE SCHEDULED CLASS (Mark as INACTIVE) =================
@DeleteMapping("/schedule-classes/{id}")
public ResponseEntity<?> deleteSchedule(@PathVariable Long id) {
 try {
     ScheduledClass schedule = scheduledClassRepository.findById(id)
             .orElseThrow(() -> new RuntimeException("Schedule not found"));

     // Soft Delete Logic: Change status to INACTIVE instead of removing from DB
     schedule.setStatus("INACTIVE");
     
     scheduledClassRepository.save(schedule);
     return ResponseEntity.ok("Schedule marked as Inactive successfully");
 } catch (Exception e) {
     return ResponseEntity.badRequest().body("Error updating status: " + e.getMessage());
 }
}
@PostMapping("/create-trainer")
public ResponseEntity<?> createTrainer(@RequestBody Map<String, String> payload) {
    try {

        String email = payload.get("email");

        // ✅ CHECK DUPLICATE EMAIL
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Email already exists"));
        }

        User trainer = new User();
        trainer.setName(payload.get("name"));
        trainer.setEmail(email);
        trainer.setPhone(payload.get("phone"));
        trainer.setPassword(payload.get("password")); // plaintext because admin wants to see
        trainer.setStatus(Status.ACTIVE);

        RoleMaster role = roleRepository.findByRoleName("TRAINER");

        if (role == null) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "TRAINER role missing in DB"));
        }

        trainer.setRole(role);

        userRepository.save(trainer);

        return ResponseEntity.ok(Map.of(
                "message", "Trainer created successfully",
                "password", trainer.getPassword()   // ✅ admin can see
        ));

    } catch (Exception e) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
    }
}

@PutMapping("/reset-trainer-password/{id}")
public ResponseEntity<?> resetTrainerPassword(
        @PathVariable Long id,
        @RequestBody Map<String, String> payload) {

    try {
        User trainer = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trainer not found"));

        trainer.setPassword(payload.get("password"));
        userRepository.save(trainer);

        // Simulated SMS trigger
        System.out.println("Send SMS to: " + trainer.getPhone());
        System.out.println("New Password: " + payload.get("password"));

        return ResponseEntity.ok("Password updated and sent to trainer phone.");
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error: " + e.getMessage());
    }
}
@PutMapping("/inactivate-trainer/{id}")
public ResponseEntity<?> inactivateTrainer(@PathVariable Long id) {
    try {
        User trainer = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trainer not found"));

        trainer.setStatus(Status.INACTIVE);
        userRepository.save(trainer);

        return ResponseEntity.ok("Trainer marked as INACTIVE");
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
@GetMapping("/all-trainers")
public List<User> getAllTrainers() {
    return userRepository.findByRole_RoleName("TRAINER");
}
@PutMapping("/update-trainer/{id}")
public ResponseEntity<?> updateTrainer(@PathVariable Long id,
                                       @RequestBody User updatedTrainer) {

    User trainer = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Trainer not found"));

    trainer.setName(updatedTrainer.getName());
    trainer.setEmail(updatedTrainer.getEmail());
    trainer.setPhone(updatedTrainer.getPhone());
    trainer.setPassword(updatedTrainer.getPassword());

    userRepository.save(trainer);

    return ResponseEntity.ok(Map.of("message", "Trainer updated successfully"));
}
//================= TOGGLE TRAINER STATUS =================
@PutMapping("/toggle-trainer-status/{id}")
public ResponseEntity<?> toggleTrainerStatus(@PathVariable Long id) {

 try {
     User trainer = userRepository.findById(id)
             .orElseThrow(() -> new RuntimeException("Trainer not found"));

     // Toggle logic
     if (trainer.getStatus() == Status.ACTIVE) {
         trainer.setStatus(Status.INACTIVE);
     } else {
         trainer.setStatus(Status.ACTIVE);
     }

     userRepository.save(trainer);

     return ResponseEntity.ok(
             Map.of(
                     "message", "Trainer status updated successfully",
                     "newStatus", trainer.getStatus()
             )
     );

 } catch (Exception e) {
     return ResponseEntity.badRequest().body(
             Map.of("message", e.getMessage())
     );
 }
}
@PutMapping("/courses/{id}")
public ResponseEntity<?> updateCourse(
        @PathVariable Long id,
        @RequestParam("courseName") String courseName,
        @RequestParam("duration") String duration,
        @RequestParam("description") String description,
        @RequestParam(value = "file", required = false) MultipartFile file
) {

    try {

        CourseMaster course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        course.setCourseName(courseName);
        course.setDuration(duration);
        course.setDescription(description);

        // ===== UPDATED FILE UPLOAD LOGIC =====
        if (file != null && !file.isEmpty()) {

            String uploadDir = System.getProperty("user.dir") + "/uploads/syllabus/";

            java.io.File directory = new java.io.File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String filePath = uploadDir + fileName;

            file.transferTo(new java.io.File(filePath));

            course.setSyllabusFileName(fileName);
            course.setSyllabusFilePath(filePath);
        }

        courseRepository.save(course);

        return ResponseEntity.ok("Course updated successfully");

    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}


//================= SOFT DELETE COURSE =================
@DeleteMapping("/courses/{id}")
public ResponseEntity<?> softDeleteCourse(@PathVariable Long id) {
 try {
     CourseMaster course = courseRepository.findById(id)
             .orElseThrow(() -> new RuntimeException("Course not found"));

     course.setStatus("INACTIVE");
     courseRepository.save(course);

     return ResponseEntity.ok("Course marked as INACTIVE");

 } catch (Exception e) {
     return ResponseEntity.badRequest().body(e.getMessage());
 }
}
//================= REACTIVATE COURSE =================
@PutMapping("/courses/reactivate/{id}")
public ResponseEntity<?> reactivateCourse(@PathVariable Long id) {
 try {
     CourseMaster course = courseRepository.findById(id)
             .orElseThrow(() -> new RuntimeException("Course not found"));

     course.setStatus("ACTIVE");
     courseRepository.save(course);

     return ResponseEntity.ok("Course reactivated successfully");

 } catch (Exception e) {
     return ResponseEntity.badRequest().body(e.getMessage());
 }
}
@GetMapping("/courses/inactive")
public List<CourseMaster> getInactiveCourses() {
    return courseRepository.findAll()
            .stream()
            .filter(course -> "INACTIVE".equals(course.getStatus()))
            .toList();
}
@GetMapping("/courses/download/{id}")
public ResponseEntity<?> downloadSyllabus(@PathVariable Long id) {

    try {
        CourseMaster course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (course.getSyllabusFilePath() == null) {
            return ResponseEntity.badRequest().body("No syllabus available");
        }

        java.io.File file = new java.io.File(course.getSyllabusFilePath());

        if (!file.exists()) {
            return ResponseEntity.badRequest().body("File not found on server");
        }

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=\"" + course.getSyllabusFileName() + "\"")
                .body(new org.springframework.core.io.FileSystemResource(file));

    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}



@GetMapping("/student-course-mappings")
public List<Map<String, Object>> getStudentCourseMappings() {
    return adminService.getStudentCourseMappings();
}



@GetMapping("/student-batch-mappings")
public List<Map<String, Object>> getStudentBatchMappings() {
    return adminService.getStudentBatchMappings();
}



@PostMapping("/student-course-mappings")
public ResponseEntity<?> mapStudentToCourse(
        @RequestParam Long studentId,
        @RequestParam Long courseId) {

    try {
        adminService.mapStudentToCourse(studentId, courseId);
        return ResponseEntity.ok("Mapped successfully");
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}

@PostMapping("/student-batch-mappings")
public ResponseEntity<?> mapStudentToBatch(
        @RequestParam Long studentId,
        @RequestParam Long batchId) {

    try {
        adminService.mapStudentToBatch(studentId, batchId);
        return ResponseEntity.ok("Mapped successfully");
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
//Update this specific method in AdminController.java
@GetMapping("/batches/course/{courseId}")
public List<Map<String, Object>> getBatchesByCourse(@PathVariable Long courseId) {
 return batchRepository.findByCourse_Id(courseId).stream().map(batch -> {
     Map<String, Object> map = new HashMap<>();
     map.put("id", batch.getId());
     map.put("batchName", batch.getBatchName());
     map.put("status", batch.getStatus()); // Critical for frontend filtering
     
     Map<String, Object> trainerMap = new HashMap<>();
     if (batch.getTrainer() != null) {
         trainerMap.put("id", batch.getTrainer().getId());
         trainerMap.put("name", batch.getTrainer().getName());
     }
     map.put("trainer", trainerMap);
     return map;
 }).collect(Collectors.toList());
}
//================= ATTENDANCE MARKING =================

//1. Fetch Students by Batch for Marking
@GetMapping("/attendance/students/{batchId}")
public ResponseEntity<?> getStudentsByBatch(@PathVariable Long batchId) {
 try {
     Batches batch = batchRepository.findById(batchId)
             .orElseThrow(() -> new RuntimeException("Batch not found"));

     // Assuming StudentBatch entity connects students to batches
     List<Map<String, Object>> students = batch.getStudentBatches().stream()
             .map(sb -> {
                 Map<String, Object> map = new HashMap<>();
                 map.put("studentId", sb.getStudent().getId());
                 map.put("studentName", sb.getStudent().getName());
                 return map;
             }).collect(Collectors.toList());

     return ResponseEntity.ok(students);
 } catch (Exception e) {
     return ResponseEntity.badRequest().body("Error: " + e.getMessage());
 }
}



}