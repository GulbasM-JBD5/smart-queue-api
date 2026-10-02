package com.example.smartqueueapi.controller;

import com.example.smartqueueapi.dto.QueueCustomerRequest;
import com.example.smartqueueapi.dto.QueueCustomerResponse;
import com.example.smartqueueapi.service.QueueCustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queue")
public class QueueCustomerController {

    private final QueueCustomerService queueCustomerService;

    public QueueCustomerController(QueueCustomerService queueCustomerService) {
        this.queueCustomerService = queueCustomerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QueueCustomerResponse addCustomer(
            @Valid @RequestBody QueueCustomerRequest request) {

        return queueCustomerService.addCustomer(request);
    }

    @GetMapping
    public List<QueueCustomerResponse> getWaitingCustomers() {

        return queueCustomerService.getWaitingCustomers();
    }

    @GetMapping("/{id}")
    public QueueCustomerResponse getCustomerById(@PathVariable Long id) {

        return queueCustomerService.getCustomerById(id);
    }

    @PostMapping("/next")
    public QueueCustomerResponse nextCustomer() {

        return queueCustomerService.nextCustomer();
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCustomer(@PathVariable Long id) {

        queueCustomerService.removeCustomer(id);
    }
}
