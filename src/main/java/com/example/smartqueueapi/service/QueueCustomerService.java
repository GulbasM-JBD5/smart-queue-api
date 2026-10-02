package com.example.smartqueueapi.service;

import com.example.smartqueueapi.dto.QueueCustomerRequest;
import com.example.smartqueueapi.dto.QueueCustomerResponse;
import com.example.smartqueueapi.entity.QueueStatus;
import com.example.smartqueueapi.entity.QueueCustomer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.example.smartqueueapi.repository.QueueCustomerRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class QueueCustomerService {

    private final QueueCustomerRepository queueCustomerRepository;

    public QueueCustomerService(QueueCustomerRepository queueCustomerRepository) {
        this.queueCustomerRepository = queueCustomerRepository;
    }
    //add
    public QueueCustomerResponse addCustomer(QueueCustomerRequest request) {
        QueueCustomer customer=new QueueCustomer();
        customer.setName(request.getName());
        customer.setCreatedAt(LocalDateTime.now());
        customer.setStatus(QueueStatus.WAITING);
        QueueCustomer savedCustomer= queueCustomerRepository.save(customer);
        int position = findPosition(savedCustomer.getId());
        QueueCustomerResponse response=new QueueCustomerResponse();
        response.setId(savedCustomer.getId());
        response.setName(savedCustomer.getName());
        response.setCreatedAt(savedCustomer.getCreatedAt());
        response.setStatus(savedCustomer.getStatus());
        response.setPosition(position);
        return response;
    }
   //get
   public List<QueueCustomerResponse> getWaitingCustomers() {
       List<QueueCustomer> customers = queueCustomerRepository
               .findByStatusOrderByCreatedAtAscIdAsc(QueueStatus.WAITING);

       List<QueueCustomerResponse> responses = new ArrayList<>();

       int position = 1;
       for (QueueCustomer customer : customers) {
           QueueCustomerResponse response = new QueueCustomerResponse();
           response.setId(customer.getId());
           response.setName(customer.getName());
           response.setCreatedAt(customer.getCreatedAt());
           response.setStatus(customer.getStatus());
           response.setPosition(position);
           responses.add(response);
           position++;
       }
       return responses;
   }
    //get but for id
    public QueueCustomerResponse getCustomerById(Long id) {
        QueueCustomer customer = queueCustomerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found"
                ));
        QueueCustomerResponse response = new QueueCustomerResponse();
        response.setId(customer.getId());
        response.setName(customer.getName());
        response.setCreatedAt(customer.getCreatedAt());
        response.setStatus(customer.getStatus());
        if (customer.getStatus() == QueueStatus.WAITING) {
            List<QueueCustomer> waitingCustomers = queueCustomerRepository
                    .findByStatusOrderByCreatedAtAscIdAsc(QueueStatus.WAITING);
            for (QueueCustomer waitingCustomer : waitingCustomers) {
                if (waitingCustomer.getId().equals(customer.getId())) {
                    response.setPosition(findPosition(customer.getId()));
                    break;
                }
            }
        } else {
            response.setPosition(null);
        }
        return response;
    }

    // Helper method
    private int findPosition(Long customerId) {

        List<QueueCustomer> waitingCustomers =
                queueCustomerRepository.findByStatusOrderByCreatedAtAscIdAsc(
                        QueueStatus.WAITING);

        int position = 1;

        for (QueueCustomer customer : waitingCustomers) {

            if (customer.getId().equals(customerId)) {
                return position;
            }

            position++;
        }

        return position;
    }
    @Transactional
    public QueueCustomerResponse nextCustomer() {

        QueueCustomer customer = queueCustomerRepository
                .findFirstByStatusOrderByCreatedAtAscIdAsc(QueueStatus.WAITING)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No waiting customers"
                ));

        customer.setStatus(QueueStatus.SERVING);

        QueueCustomer savedCustomer = queueCustomerRepository.save(customer);

        QueueCustomerResponse response = new QueueCustomerResponse();

        response.setId(savedCustomer.getId());
        response.setName(savedCustomer.getName());
        response.setCreatedAt(savedCustomer.getCreatedAt());
        response.setStatus(savedCustomer.getStatus());
        response.setPosition(null);

        return response;
    }
    //delete
    public void removeCustomer(Long id) {

        QueueCustomer customer = queueCustomerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer not found"
                ));

        queueCustomerRepository.delete(customer);
    }
}


