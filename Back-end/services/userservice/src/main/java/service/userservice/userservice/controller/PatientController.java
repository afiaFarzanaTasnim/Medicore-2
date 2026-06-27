package service.userservice.userservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import service.userservice.userservice.context.UserContext;
import service.userservice.userservice.model.Appointment;
import service.userservice.userservice.model.DoctorProfile;
import service.userservice.userservice.model.Prescription;
import service.userservice.userservice.repository.AppointmentRepository;
import service.userservice.userservice.repository.DoctorProfileRepository;
import service.userservice.userservice.repository.PrescriptionRepository;
import service.userservice.userservice.service.DoctorNameResolver;

@RestController
@RequestMapping("/api/v1/patient")
public class PatientController {

    @Autowired private AppointmentRepository apptRepo;
    @Autowired private PrescriptionRepository prescRepo;
    @Autowired private DoctorProfileRepository docRepo;
    @Autowired private DoctorNameResolver doctorNameResolver;
    @Autowired private RestTemplate restTemplate;

    /**
     * Helper method to validate if the user is actually a patient.
     */
    private boolean isNotPatient() {
        return !"patient".equalsIgnoreCase(UserContext.getRole());
    }

    @PostMapping("/appointments")
    public ResponseEntity<?> bookAppointment(@RequestBody Map<String, Object> payload) {
        try {
            // 1. Role Validation
            if (isNotPatient()) {
                return ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden: Only patients can book appointments."));
            }

            // 2. Validate payload exists
            if (!payload.containsKey("doctor_id") || payload.get("doctor_id") == null) {
                return ResponseEntity.status(400).body(Map.of("success", false, "message", "Bad Request: 'doctor_id' is required."));
            }

            String doctorId = String.valueOf(payload.get("doctor_id"));
            
            // 3. Create Appointment
            String doctorNameInput = payload.get("doctor_name") != null
                    ? String.valueOf(payload.get("doctor_name"))
                    : null;

            Appointment appt = Appointment.builder()
                    .patientId(UserContext.getUserId())
                    .patientName(UserContext.getName())
                    .patientPhone(UserContext.getPhone())
                    .doctorId(doctorId)
                    .doctorName(doctorNameInput)
                    .date(payload.get("date") != null ? String.valueOf(payload.get("date")) : "N/A")
                    .symptoms(payload.get("symptoms") != null ? String.valueOf(payload.get("symptoms")) : "N/A")
                    .transactionId(payload.get("transaction_id") != null ? String.valueOf(payload.get("transaction_id")) : "N/A")
                    .isComplete(false)
                    .build();
            
            apptRepo.save(appt);

            // 4. Create Prescription Shell
            Prescription presc = Prescription.builder()
                    .patientId(appt.getPatientId())
                    .doctorId(appt.getDoctorId())
                    .symptoms(appt.getSymptoms())
                    .transactionId(appt.getTransactionId())
                    .build();
                    
            prescRepo.save(presc);

            // 5. Fetch Doctor Profile
            DoctorProfile docProfile = docRepo.findById(doctorId).orElse(new DoctorProfile());
            // DoctorProfile.name is @Transient (null on a fresh fetch). Try the input
            // from the booking form first, then fall back to auth-service for old bookings.
            String doctorName = doctorNameInput != null
                    ? doctorNameInput
                    : doctorNameResolver.resolve(doctorId);
            if (doctorName == null) doctorName = "Unknown";

            // 6. Return Success Response
            return ResponseEntity.status(201).body(Map.of(
                "success", true,
                "message", "Appointment booked successfully.",
                "data", Map.of(
                    "prescriptionID", presc.getPrescriptionId(),
                    "patient_id", appt.getPatientId(),
                    "doctor_name", doctorName,
                    "doctor_info", Map.of(
                            "name", doctorName,
                            "doctorId", doctorId,
                            "specialization", docProfile.getSpecialization() != null ? docProfile.getSpecialization() : ""
                    ),
                    "location", docProfile.getLocation() != null ? docProfile.getLocation() : "",
                    "date", appt.getDate(),
                    "serial_no", appt.getSerialNo(),
                    "symptoms", appt.getSymptoms()
                )
            ));

        } catch (Exception e) {
            // IF IT CRASHES NOW, POSTMAN WILL TELL YOU EXACTLY WHY!
            e.printStackTrace(); // This prints the error to your IDE console
            return ResponseEntity.status(500).body(Map.of(
                "success", false, 
                "message", "Server Error: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/myallappointments")
    public ResponseEntity<?> getMyAllAppointments() {
        if (isNotPatient()) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "Forbidden: Only patients can access this resource."
            ));
        }

        List<Map<String, Object>> appointments = apptRepo.findByPatientId(UserContext.getUserId())
                .stream()
                .map(this::mapPatientAppointmentResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "data", appointments));
    }

    @GetMapping("/prescriptions")
    public ResponseEntity<?> getPrescriptions() {
        if (isNotPatient()) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden"));
        // Hide empty "appointment stub" rows — a prescription only counts for the patient
        // once the doctor has actually written something (description OR at least one medicine).
        List<Prescription> filled = prescRepo.findByPatientId(UserContext.getUserId()).stream()
                .filter(p -> (p.getDescription() != null && !p.getDescription().isBlank())
                          || (p.getMedicineDetails() != null && !p.getMedicineDetails().isEmpty()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", filled));
    }

    @GetMapping("/appointments")
    public ResponseEntity<?> getMyAppointments() {
        if (isNotPatient()) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "Forbidden: Only patients can view their appointments."
            ));
        }

        List<Appointment> mine = apptRepo.findByPatientId(UserContext.getUserId());

        // Enrich each appointment with the doctor's name, specialization and location
        // so the patient-side My Appointments table can render without extra calls.
        // Doctor names that weren't snapshotted at booking time are resolved live from
        // auth-service via DoctorNameResolver (with an in-memory cache).
        List<Map<String, Object>> data = mine.stream()
                .sorted((a, b) -> {
                    String ad = a.getDate() != null ? a.getDate() : "";
                    String bd = b.getDate() != null ? b.getDate() : "";
                    return bd.compareTo(ad); // newest date first
                })
                .map(appt -> {
                    DoctorProfile doc = docRepo.findById(appt.getDoctorId()).orElse(new DoctorProfile());
                    // Priority: snapshotted name from booking -> auth-service lookup -> fallback.
                    // DoctorProfile.name is @Transient so we can't rely on the local fetch.
                    String doctorName = appt.getDoctorName() != null && !appt.getDoctorName().isBlank()
                            ? appt.getDoctorName()
                            : doctorNameResolver.resolve(appt.getDoctorId());
                    if (doctorName == null) doctorName = "Unknown";
                    String specialization = doc.getSpecialization() != null ? doc.getSpecialization() : "";
                    String location = doc.getLocation() != null ? doc.getLocation() : "";
                    String status = Boolean.TRUE.equals(appt.getIsComplete()) ? "Completed" : "Booked";

                    Map<String, Object> doctorInfo = new java.util.LinkedHashMap<>();
                    doctorInfo.put("name", doctorName);
                    doctorInfo.put("specialization", specialization);

                    Map<String, Object> row = new java.util.LinkedHashMap<>();
                    row.put("appointmentId", appt.getSerialNo());
                    row.put("doctor_info", doctorInfo);
                    row.put("specialization", specialization);
                    row.put("date", appt.getDate());
                    row.put("serial_no", appt.getSerialNo());
                    row.put("location", location);
                    row.put("status", status);
                    row.put("symptoms", appt.getSymptoms());
                    return row;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", data
        ));
    }

    @GetMapping("/prescriptions/doctor/{doctorId}")
    public ResponseEntity<?> getPrescriptionsByDoctor(@PathVariable String doctorId) {
        if (isNotPatient()) return ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden"));
        // Same filter as /prescriptions: drop appointment stubs.
        List<Prescription> filled = prescRepo.findByPatientIdAndDoctorId(UserContext.getUserId(), doctorId).stream()
                .filter(p -> (p.getDescription() != null && !p.getDescription().isBlank())
                          || (p.getMedicineDetails() != null && !p.getMedicineDetails().isEmpty()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", filled));
    }

    private Map<String, Object> mapPatientAppointmentResponse(Appointment appointment) {
        DoctorProfile docProfile = docRepo.findById(appointment.getDoctorId()).orElse(new DoctorProfile());

        Map<String, Object> item = new HashMap<>();
        item.put("appointmentId", String.valueOf(appointment.getSerialNo()));
        item.put("doctorName", fetchDoctorName(appointment.getDoctorId()));
        item.put("department", docProfile.getSpecialization() != null ? docProfile.getSpecialization() : "");
        item.put("date", appointment.getDate());
        item.put("serialNo", appointment.getSerialNo());
        item.put("serial_no", appointment.getSerialNo());
        item.put("status", Boolean.TRUE.equals(appointment.getIsComplete()) ? "COMPLETED" : "CONFIRMED");
        return item;
    }

    private String fetchDoctorName(String doctorId) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "Unknown Doctor";
        }

        String jwtToken = attributes.getRequest().getHeader("Authorization");
        if (jwtToken == null) {
            return "Unknown Doctor";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", jwtToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://localhost:8001/api/v1/auth/user/" + doctorId,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );
            Map<?, ?> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("data")) {
                Map<?, ?> nestedData = (Map<?, ?>) responseBody.get("data");
                if (nestedData != null && nestedData.containsKey("name")) {
                    return (String) nestedData.get("name");
                }
            }
        } catch (Exception e) {
            System.err.println("Could not fetch name for doctorId " + doctorId + ": " + e.getMessage());
        }

        return "Unknown Doctor";
    }
}