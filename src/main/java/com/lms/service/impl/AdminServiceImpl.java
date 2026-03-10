package com.lms.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lms.dto.BatchDetailsDTO;
import com.lms.dto.CourseFullDetailsDTO;
import com.lms.dto.DashboardResponse;
import com.lms.entity.*;
import com.lms.enums.Status;
import com.lms.repository.*;
import com.lms.service.AdminService;

import java.util.*;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private StudentBatchesRepository studentBatchesRepository;

    @Autowired
    private StudentCourseRepository studentCourseRepository;

    // ================= DASHBOARD =================
    @Override
    public DashboardResponse getDashboardData() {

        long totalCourses = courseRepository.count();
        long totalTrainers = userRepository.countByRole_RoleNameAndStatus("TRAINER", Status.ACTIVE);
        long totalStudents = userRepository.countByRole_RoleNameAndStatus("STUDENT", Status.ACTIVE);
        long activeBatches = batchRepository.countByStatus("ONGOING");

        return new DashboardResponse(
                totalCourses,
                totalTrainers,
                totalStudents,
                activeBatches,
                Collections.emptyList()
        );
    }

    // ================= COURSE =================
    @Override
    public void createCourse(CourseMaster course) {
        courseRepository.save(course);
    }

    @Override
    public List<CourseMaster> getAllCourses() {
        return courseRepository.findAll();
    }

    @Override
    public void updateCourse(Long id, CourseMaster updatedCourse) {
        CourseMaster course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        course.setCourseName(updatedCourse.getCourseName());
        course.setDuration(updatedCourse.getDuration());
        course.setDescription(updatedCourse.getDescription());

        courseRepository.save(course);
    }

    // ✅ SOFT DELETE ONLY
    @Override
    public void deleteCourse(Long id) {

        CourseMaster course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        course.setStatus("INACTIVE");

        courseRepository.save(course);
    }

    // ================= COURSE FULL DETAILS =================
    @Override
    public List<CourseFullDetailsDTO> getAllCoursesWithDetails() {

        List<CourseMaster> courses = courseRepository.findAll();

        return courses.stream().map(course -> {

            List<Batches> batches = batchRepository.findByCourse_Id(course.getId());

            List<BatchDetailsDTO> batchDTOs = batches.stream().map(batch -> {

                String trainerName = "Not Assigned";
                String trainerEmail = "-";

                if (batch.getTrainer() != null) {
                    trainerName = batch.getTrainer().getName();
                    trainerEmail = batch.getTrainer().getEmail();
                }

                List<String> students = batch.getStudentBatches() != null
                        ? batch.getStudentBatches()
                            .stream()
                            .map(sb -> sb.getStudent().getName())
                            .toList()
                        : List.of();

                return new BatchDetailsDTO(
                        batch.getId(),
                        batch.getBatchName(),
                        trainerName,
                        trainerEmail,
                        students
                );

            }).toList();

            return new CourseFullDetailsDTO(
                    course.getId(),
                    course.getCourseName(),
                    course.getDuration(),
                    course.getDescription(),
                    batchDTOs
            );

        }).toList();
    }
  

 
    @Override
    public List<Map<String, Object>> getStudentCourseMappings() {

        List<StudentCourse> mappings = studentCourseRepository.findAll();

        return mappings.stream().map(sc -> {

            Map<String, Object> map = new HashMap<>();

            map.put("mappingId", sc.getId());

            map.put("studentId", sc.getStudent().getId());
            map.put("studentName", sc.getStudent().getName());
            map.put("studentEmail", sc.getStudent().getEmail());

            map.put("courseId", sc.getCourse().getId());
            map.put("courseName", sc.getCourse().getCourseName());
            map.put("courseStatus", sc.getCourse().getStatus());

            return map;

        }).toList();
    }

    @Override
    public List<Map<String, Object>> getStudentBatchMappings() {

        List<StudentBatches> mappings = studentBatchesRepository.findAll();

        return mappings.stream().map(sb -> {

            Map<String, Object> map = new HashMap<>();

            map.put("mappingId", sb.getId());

            map.put("studentId", sb.getStudent().getId());
            map.put("studentName", sb.getStudent().getName());
            map.put("studentEmail", sb.getStudent().getEmail());

            map.put("batchId", sb.getBatch().getId());
            map.put("batchName", sb.getBatch().getBatchName());
            map.put("batchStatus", sb.getBatch().getStatus());

            map.put("courseId", sb.getBatch().getCourse().getId());
            map.put("courseName", sb.getBatch().getCourse().getCourseName());
            map.put("courseStatus", sb.getBatch().getCourse().getStatus());

            return map;

        }).toList();
    }
 // Replace these methods in your AdminServiceImpl.java
    @Override
    public void mapStudentToCourse(Long studentId, Long courseId) {
        // 1. Check for Duplicate Course Mapping
        Optional<StudentCourse> existing = studentCourseRepository.findByStudent_IdAndCourse_Id(studentId, courseId);
        if (existing.isPresent()) {
            throw new RuntimeException("Student is already enrolled in this course.");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        CourseMaster course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        StudentCourse mapping = new StudentCourse();
        mapping.setStudent(student);
        mapping.setCourse(course);
        studentCourseRepository.save(mapping);
    }

    @Override
    public void mapStudentToBatch(Long studentId, Long batchId) {
        Batches batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        // 2. REAL-WORLD VALIDATION: Check if student is assigned to the parent course first
        Long courseId = batch.getCourse().getId();
        Optional<StudentCourse> courseEnrolled = studentCourseRepository.findByStudent_IdAndCourse_Id(studentId, courseId);
        
        if (courseEnrolled.isEmpty()) {
            throw new RuntimeException("Flow Violation: Student must be enrolled in the course '" 
                                        + batch.getCourse().getCourseName() + "' before batch allotment.");
        }

        // 3. Check for Duplicate Batch Mapping
        Optional<StudentBatches> existingBatch = studentBatchesRepository.findByStudent_IdAndBatch_Id(studentId, batchId);
        if (existingBatch.isPresent()) {
            throw new RuntimeException("Student is already assigned to this batch.");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        StudentBatches mapping = new StudentBatches();
        mapping.setStudent(student);
        mapping.setBatch(batch);
        studentBatchesRepository.save(mapping);
    }
    
}