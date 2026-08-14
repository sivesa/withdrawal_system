package com.enviro.assessment.junior.sive.dto;

import java.time.LocalDate;

public class InvestorDto {

    private Long id;
    private String fullName;
    private String email;
    private LocalDate dateOfBirth;
    private int age;

    public InvestorDto() {
    }

    public InvestorDto(Long id, String fullName, String email, LocalDate dateOfBirth, int age) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.age = age;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
