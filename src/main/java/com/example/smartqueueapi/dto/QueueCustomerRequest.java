package com.example.smartqueueapi.dto;

import jakarta.validation.constraints.NotBlank;

public class QueueCustomerRequest {
    @NotBlank(message = "Name cannot be blank")
    private String name;

    public QueueCustomerRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}