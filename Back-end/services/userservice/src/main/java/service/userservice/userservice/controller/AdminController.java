package service.userservice.userservice.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import service.userservice.userservice.model.DoctorProfile;
import service.userservice.userservice.repository.DoctorProfileRepository;

@RestController @RequestMapping("/api/v1/admin")
public class AdminController {
    @Autowired private DoctorProfileRepository doctorRepo;

    @PatchMapping("/approve-doctor/{id}")
    public ResponseEntity<?> approveDoctor(@PathVariable String id) {
        DoctorProfile doc = doctorRepo.findById(id).orElseGet(() -> {
            DoctorProfile profile = new DoctorProfile();
            profile.setUserId(id);
            profile.setRating(5.0);
            return profile;
        });

        doc.setApproval(true);
        DoctorProfile saved = doctorRepo.save(doc);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Doctor status has been updated to Approved.",
                "data", Map.of(
                        "doctorId", saved.getUserId(),
                        "role", "doctor",
                        "approval", true,
                        "updatedAt", saved.getUpdatedAt()
                )
        ));
    }
}