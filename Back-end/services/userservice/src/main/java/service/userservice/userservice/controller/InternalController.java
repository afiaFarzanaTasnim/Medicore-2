package service.userservice.userservice.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import service.userservice.userservice.model.DoctorProfile;
import service.userservice.userservice.repository.DoctorProfileRepository;

@RestController
@RequestMapping("/api/v1/internal")
public class InternalController {

    @Autowired
    private DoctorProfileRepository doctorRepo;

    @PostMapping("/doctor-profile/{userId}")
    public ResponseEntity<?> createDoctorProfile(@PathVariable String userId) {
        if (doctorRepo.findById(userId).isPresent()) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Doctor profile already exists."));
        }

        DoctorProfile profile = DoctorProfile.builder()
                .userId(userId)
                .approval(false)
                .rating(5.0)
                .build();
        doctorRepo.save(profile);

        return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "message", "Doctor profile created.",
                "data", Map.of("userId", userId)
        ));
    }
}
