package service.userservice.userservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Required Servlet & HTTP Core Imports for JWT forwarding
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.beans.factory.annotation.Autowired;

// Service Data Imports
import service.userservice.userservice.context.UserContext;
import service.userservice.userservice.model.DoctorProfile;
import service.userservice.userservice.model.PharmacistProfile;
import service.userservice.userservice.repository.DoctorProfileRepository;
import service.userservice.userservice.repository.PharmacistProfileRepository;

@RestController 
@RequestMapping("/api/v1/user")
public class UserController {
    
    @Autowired private DoctorProfileRepository doctorRepo;
    @Autowired private PharmacistProfileRepository pharmRepo;
    @Autowired private RestTemplate restTemplate;

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> payload) {
        String role = UserContext.getRole();
        String userId = UserContext.getUserId();

        if ("doctor".equalsIgnoreCase(role)) {
            DoctorProfile doc = doctorRepo.findById(userId).orElse(new DoctorProfile());
            doc.setUserId(userId);
            if (payload.containsKey("specialization")) doc.setSpecialization((String) payload.get("specialization"));
            if (payload.containsKey("qualification")) doc.setQualification((String) payload.get("qualification"));
            if (payload.containsKey("location")) doc.setLocation((String) payload.get("location"));
            if (payload.containsKey("visiting_fee")) doc.setVisitingFee(Double.valueOf(payload.get("visiting_fee").toString()));
            if (doc.getRating() == null) doc.setRating(5.0);
            
            DoctorProfile saved = doctorRepo.save(doc);
            
            Map<String, Object> data = new HashMap<>();
            data.put("userId", saved.getUserId());
            data.put("role", role);
            data.put("specialization", saved.getSpecialization());
            data.put("rating", saved.getRating());
            data.put("qualification", saved.getQualification());
            data.put("location", saved.getLocation());
            data.put("visiting_fee", saved.getVisitingFee());
            data.put("updatedAt", saved.getUpdatedAt());
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Profile updated successfully.", "data", data));
        } else if ("pharmacist".equalsIgnoreCase(role)) {
            PharmacistProfile pharm = pharmRepo.findById(userId).orElse(new PharmacistProfile());
            pharm.setUserId(userId);
            if (payload.containsKey("pharmacy_name")) pharm.setPharmacyName((String) payload.get("pharmacy_name"));
            return ResponseEntity.ok(Map.of("success", true, "message", "Profile updated successfully.", "data", pharmRepo.save(pharm)));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Role profile logged with no extra fields required."));
    }

    @GetMapping("/doctors")
    public ResponseEntity<?> getApprovedDoctors() {
        List<DoctorProfile> approvedDoctors = doctorRepo.findByApprovalTrue();
        String jwtToken = null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            jwtToken = request.getHeader("Authorization"); 
        }
        HttpHeaders headers = new HttpHeaders();
        if (jwtToken != null) {
            headers.set("Authorization", jwtToken);
        }
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        String finalJwtToken = jwtToken;
        List<Map<String, Object>> enrichedDoctors = approvedDoctors.stream().map(doctor -> {
            Map<String, Object> docMap = new HashMap<>();
            docMap.put("userId", doctor.getUserId());
            docMap.put("specialization", doctor.getSpecialization());
            docMap.put("qualification", doctor.getQualification());
            docMap.put("location", doctor.getLocation());
            docMap.put("visitingFee", doctor.getVisitingFee());
            docMap.put("rating", doctor.getRating());
            docMap.put("approval", doctor.getApproval());
            docMap.put("updatedAt", doctor.getUpdatedAt());

            String doctorName = "Unknown Doctor"; // Fallback

            if (finalJwtToken != null) {
                try {
                    String authServiceUrl = "http://localhost:8001/api/v1/auth/user/" + doctor.getUserId();

                    // Exchange request with the authorization headers attached
                    ResponseEntity<Map> response = restTemplate.exchange(
                            authServiceUrl,
                            HttpMethod.GET,
                            requestEntity,
                            Map.class
                    );

                    Map<?, ?> responseBody = response.getBody();
                    if (responseBody != null && responseBody.containsKey("data")) {
                        Map<?, ?> nestedData = (Map<?, ?>) responseBody.get("data");
                        if (nestedData != null && nestedData.containsKey("name")) {
                            doctorName = (String) nestedData.get("name");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Could not fetch name for userId " + doctor.getUserId() + ": " + e.getMessage());
                }
            } else {
                System.err.println("Skipping auth-service call: No Bearer JWT token found in request context.");
            }

            docMap.put("name", doctorName);
            return docMap;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "data", enrichedDoctors));
    }
}