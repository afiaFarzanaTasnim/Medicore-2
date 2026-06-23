package service.userservice.userservice.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import service.userservice.userservice.model.Medicine;
import service.userservice.userservice.repository.MedicineRepository;

@RestController @RequestMapping("/api/v1/pharmacist/medicines")
public class PharmacistController {
    @Autowired private MedicineRepository medRepo;

    @GetMapping
    public ResponseEntity<?> getAllMedicines() { 
        return ResponseEntity.ok(Map.of("success", true, "data", medRepo.findAll())); 
    }

    @PostMapping
    public ResponseEntity<?> addMedicine(@RequestBody Medicine medicine) {
        if (medicine.getMedicineName() == null || medicine.getMedicineName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Medicine name cannot be null or empty."));
        }
        return ResponseEntity.status(201).body(Map.of(
            "success", true, 
            "message", "Medicine added successfully to catalog.", 
            "data", medRepo.save(medicine)
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMedicineStock(@PathVariable String id, @RequestBody Medicine payload) {
        return medRepo.findById(id).map(m -> {
            if (payload.getMedicineName() != null && !payload.getMedicineName().trim().isEmpty()) {
                m.setMedicineName(payload.getMedicineName());
            }
            if (payload.getPrice() != null) m.setPrice(payload.getPrice());
            if (payload.getQuantity() != null) m.setQuantity(payload.getQuantity());
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Medicine inventory details altered successfully.", 
                "data", medRepo.save(m)
            ));
        }).orElse(ResponseEntity.status(404).body(Map.of("success", false, "message", "Medicine not found.")));
    }
}