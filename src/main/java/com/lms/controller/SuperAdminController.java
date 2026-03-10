package com.lms.controller;

import com.lms.dto.AdminRequest;
import com.lms.dto.DashboardResponse;
import com.lms.entity.User;
import com.lms.service.SuperAdminCreateAdminService;
import com.lms.service.SuperAdminService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class SuperAdminController {

    @Autowired
    private SuperAdminCreateAdminService superAdminCreateAdminService;

    @Autowired
    private SuperAdminService superAdminService;

    // =====================================================
    // CREATE ADMIN
    // =====================================================
    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin(
            @RequestBody AdminRequest request,
            HttpSession session) {

        if (!isSuperAdmin(session)) {
            return unauthorizedResponse(session);
        }

        superAdminCreateAdminService.createAdmin(request);

        return ResponseEntity.ok("Admin created successfully");
    }

    // =====================================================
    // GET ALL ADMINS
    // =====================================================
    @GetMapping("/admins")
    public ResponseEntity<?> getAllAdmins(HttpSession session) {

        if (!isSuperAdmin(session)) {
            return unauthorizedResponse(session);
        }

        List<User> admins = superAdminCreateAdminService.getAllAdmins();

        return ResponseEntity.ok(admins);
    }

    // =====================================================
    // UPDATE ADMIN
    // =====================================================
    @PutMapping("/update-admin/{id}")
    public ResponseEntity<?> updateAdmin(
            @PathVariable Long id,
            @RequestBody AdminRequest request,
            HttpSession session) {

        if (!isSuperAdmin(session)) {
            return unauthorizedResponse(session);
        }

        superAdminCreateAdminService.updateAdmin(id, request);

        return ResponseEntity.ok("Admin updated successfully");
    }

    // =====================================================
    // DELETE ADMIN
    // =====================================================
    @DeleteMapping("/delete-admin/{id}")
    public ResponseEntity<?> deleteAdmin(
            @PathVariable Long id,
            HttpSession session) {

        if (!isSuperAdmin(session)) {
            return unauthorizedResponse(session);
        }

        superAdminCreateAdminService.deleteAdmin(id);

        return ResponseEntity.ok("Admin deleted successfully");
    }

    // =====================================================
    // DASHBOARD API
    // =====================================================
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(HttpSession session) {

        if (!isSuperAdmin(session)) {
            return unauthorizedResponse(session);
        }

        DashboardResponse dashboard =
                superAdminService.getDashboardStats();

        return ResponseEntity.ok(dashboard);
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    private boolean isSuperAdmin(HttpSession session) {
        String role = (String) session.getAttribute("ROLE");
        return role != null && role.equals("SUPER_ADMIN");
    }

    private ResponseEntity<String> unauthorizedResponse(HttpSession session) {

        String role = (String) session.getAttribute("ROLE");

        if (role == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Please login first.");
        }

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("Access Denied. Only Super Admin allowed.");
    }
}
