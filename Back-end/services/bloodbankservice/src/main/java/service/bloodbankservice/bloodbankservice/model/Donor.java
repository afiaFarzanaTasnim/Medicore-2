package service.bloodbankservice.bloodbankservice.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "donors")
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String bloodBankId;   // primary key

    @Column(nullable = false)
    private String donorId;       // user ID from auth service

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String contactNo;

    @Column(nullable = false)
    private String bloodgroup;

    private LocalDate lastdate;   // last donation date, can be null

    // Constructors, getters, setters
    public Donor() {}

    public Donor(String donorId, String name, String contactNo, String bloodgroup, LocalDate lastdate) {
        this.donorId = donorId;
        this.name = name;
        this.contactNo = contactNo;
        this.bloodgroup = bloodgroup;
        this.lastdate = lastdate;
    }

    // Getters and Setters
    public String getBloodBankId() { return bloodBankId; }
    public void setBloodBankId(String bloodBankId) { this.bloodBankId = bloodBankId; }

    public String getDonorId() { return donorId; }
    public void setDonorId(String donorId) { this.donorId = donorId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContactNo() { return contactNo; }
    public void setContactNo(String contactNo) { this.contactNo = contactNo; }

    public String getBloodgroup() { return bloodgroup; }
    public void setBloodgroup(String bloodgroup) { this.bloodgroup = bloodgroup; }

    public LocalDate getLastdate() { return lastdate; }
    public void setLastdate(LocalDate lastdate) { this.lastdate = lastdate; }
}