package com.lms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "student_course")
public class StudentCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many students can map to one course
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // Many students can map to one course
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private CourseMaster course;

    // ===== GETTERS & SETTERS =====

    public Long getId() {
        return id;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public CourseMaster getCourse() {
        return course;
    }

    public void setCourse(CourseMaster course) {
        this.course = course;
    }
}