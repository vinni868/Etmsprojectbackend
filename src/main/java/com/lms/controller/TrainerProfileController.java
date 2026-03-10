package com.lms.controller;

import com.lms.entity.Trainer;
import com.lms.repository.TrainerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@RestController
@RequestMapping("/api/trainer")
@CrossOrigin(origins = "http://localhost:3000") // Adjust if your React port is different
@Slf4j
public class TrainerProfileController {

    @Autowired
    private TrainerRepository trainerRepository;

    /**
     * GET: Fetch profile data by email
     * URL: http://localhost:8080/api/trainer/profile/{email}
     */
    @GetMapping("/profile/{email}")
    public ResponseEntity<Trainer> getProfile(@PathVariable String email) {
        log.info("Fetching profile for: {}", email);
        
        Optional<Trainer> trainer = trainerRepository.findByEmail(email);
        
        if (trainer.isPresent()) {
            return ResponseEntity.ok(trainer.get());
        } else {
            log.warn("Profile not found for email: {}", email);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PUT: Update existing profile or create if not exists
     * URL: http://localhost:8080/api/trainer/update-profile
     */
    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody Trainer updatedData) {
        log.info("Received update request for: {}", updatedData.getEmail());

        try {
            return trainerRepository.findByEmail(updatedData.getEmail())
                .map(existingTrainer -> {
                    // Mapping fields from React request to Database Entity
                    existingTrainer.setName(updatedData.getName());
                    existingTrainer.setPhone(updatedData.getPhone());
                    existingTrainer.setGender(updatedData.getGender());
                    existingTrainer.setSpecialization(updatedData.getSpecialization());
                    existingTrainer.setExperience(updatedData.getExperience());
                    existingTrainer.setQualification(updatedData.getQualification());
                    existingTrainer.setBio(updatedData.getBio());
                    existingTrainer.setProfilePic(updatedData.getProfilePic());
                    existingTrainer.setAddress(updatedData.getAddress());
                    existingTrainer.setCity(updatedData.getCity());
                    existingTrainer.setState(updatedData.getState());
                    existingTrainer.setPincode(updatedData.getPincode());

                    trainerRepository.save(existingTrainer);
                    return ResponseEntity.ok("Profile updated successfully ✅");
                })
                .orElseGet(() -> {
                    // If the user logs in but hasn't created a profile row yet
                    trainerRepository.save(updatedData);
                    return ResponseEntity.ok("New profile created successfully ✅");
                });
        } catch (Exception e) {
            log.error("Error updating profile: ", e);
            return ResponseEntity.internalServerError().body("Failed to update profile ❌");
        }
    }
}