package service.userservice.userservice.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import service.userservice.userservice.model.Prescription;
@Repository public interface PrescriptionRepository extends JpaRepository<Prescription, String> {
    List<Prescription> findByPatientId(String patientId);
    List<Prescription> findByPatientIdAndDoctorId(String patientId, String doctorId);
    List<Prescription> findByDoctorIdAndPatientId(String doctorId, String patientId);
}