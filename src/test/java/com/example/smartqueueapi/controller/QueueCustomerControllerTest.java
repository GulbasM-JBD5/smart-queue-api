package com.example.smartqueueapi.controller;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.springframework.test.web.servlet.MvcResult;
import com.jayway.jsonpath.JsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
@AutoConfigureMockMvc
@SpringBootTest
class QueueCustomerControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void addCustomer_withValidName_returnsCreated() throws Exception {
        mockMvc.perform(
                        post("/api/queue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "Test Customer"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Customer"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void addCustomer_withBlankName_returnsBadRequest() throws Exception {

        mockMvc.perform(
                        post("/api/queue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWaitingCustomers_returnsOk() throws Exception {

        mockMvc.perform(
                        get("/api/queue")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getCustomerById_withExistingId_returnsCustomer() throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/api/queue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "Get By Id Test"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andReturn();

        String id = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.id"
        ).toString();

        mockMvc.perform(
                        get("/api/queue/" + id)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Integer.parseInt(id)))
                .andExpect(jsonPath("$.name").value("Get By Id Test"))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void getCustomerById_withNonExistingId_returnsNotFound() throws Exception {

        mockMvc.perform(
                        get("/api/queue/999999")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Customer not found"));
    }

    @Test
    void nextCustomer_withWaitingCustomer_returnsServingCustomer() throws Exception {

        mockMvc.perform(
                        post("/api/queue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "Next Customer Test"
                                        }
                                        """)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/queue/next")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SERVING"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.position").doesNotExist());
    }

    @Test
    void removeCustomer_withExistingId_returnsNoContent() throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/api/queue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "Delete Test Customer"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andReturn();

        String id = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.id"
        ).toString();

        mockMvc.perform(
                        delete("/api/queue/" + id)
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void removeCustomer_withNonExistingId_returnsNotFound() throws Exception {

        mockMvc.perform(
                        delete("/api/queue/999999")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Customer not found"));
    }

    @Test
    void getCustomerById_returnsCorrectPosition() throws Exception {

        MvcResult firstCustomer = mockMvc.perform(
                        post("/api/queue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "Position Test 1"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult secondCustomer = mockMvc.perform(
                        post("/api/queue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "name": "Position Test 2"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andReturn();

        String firstId = JsonPath.read(
                firstCustomer.getResponse().getContentAsString(),
                "$.id"
        ).toString();

        String secondId = JsonPath.read(
                secondCustomer.getResponse().getContentAsString(),
                "$.id"
        ).toString();

        mockMvc.perform(
                        get("/api/queue/" + firstId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Position Test 1"))
                .andExpect(jsonPath("$.position").isNumber());

        mockMvc.perform(
                        get("/api/queue/" + secondId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Position Test 2"))
                .andExpect(jsonPath("$.position").isNumber());
    }
    @Test
    void nextCustomer_concurrentRequests_doNotServeSameCustomerTwice() throws Exception {

        // Ensure there are at least two waiting customers
        mockMvc.perform(
                        post("/api/queue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {
                            "name": "Concurrency Customer 1"
                        }
                        """)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/queue")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {
                            "name": "Concurrency Customer 2"
                        }
                        """)
                )
                .andExpect(status().isCreated());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);

        Future<String> firstResult = executorService.submit(() -> {
            startSignal.await();

            return mockMvc.perform(
                            post("/api/queue/next")
                    )
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
        });

        Future<String> secondResult = executorService.submit(() -> {
            startSignal.await();

            return mockMvc.perform(
                            post("/api/queue/next")
                    )
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
        });

        // Start both requests at approximately the same time
        startSignal.countDown();

        String firstResponse = firstResult.get();
        String secondResponse = secondResult.get();

        executorService.shutdown();

        Integer firstId = JsonPath.read(firstResponse, "$.id");
        Integer secondId = JsonPath.read(secondResponse, "$.id");

        org.junit.jupiter.api.Assertions.assertNotEquals(
                firstId,
                secondId
        );
    }
}
