package com.lms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "student_profile")
public class StudentProfile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  private int userId;
  private String course;
  private int year;
  private String phone;
  private String address;
  public int getId() {
	return id;
  }
  public void setId(int id) {
	this.id = id;
  }
  public int getUserId() {
	return userId;
  }
  public void setUserId(int userId) {
	this.userId = userId;
  }
  public String getCourse() {
	return course;
  }
  public void setCourse(String course) {
	this.course = course;
  }
  public int getYear() {
	return year;
  }
  public void setYear(int year) {
	this.year = year;
  }
  public String getPhone() {
	return phone;
  }
  public void setPhone(String phone) {
	this.phone = phone;
  }
  public String getAddress() {
	return address;
  }
  public void setAddress(String address) {
	this.address = address;
  }

  // getters & setters
}
