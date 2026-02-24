package com.example.service;

import com.example.config.JacksonConfig;
import com.example.config.TestKafkaConfig;
import com.example.model.VehicleData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import({TestKafkaConfig.class, JacksonConfig.class})
class VehicleDataConsumerIntegrationTest {

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer(DockerImageName.parse("mongo:5.0"))
            .withExposedPorts(27017);

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VehicleDataConsumer vehicleDataConsumer;

    @MockBean
    private VehicleDataService vehicleDataService;

    private static final String TOPIC = "vehicle-data";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @DynamicPropertySource
    static void registerContainersProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongodb.getConnectionString() + "/vehicledb");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeEach
    void setUp() {
        when(vehicleDataService.processVehicleData(any()))
            .thenAnswer(invocation -> {
                VehicleData data = invocation.getArgument(0);
                return Mono.just(data);
            });
    }

    @Test
    void shouldConsumeAndProcessVehicleData() throws Exception {
        // Arrange
        VehicleData testData = createTestVehicleData();
        String payload = objectMapper.writeValueAsString(testData);

        // Start consumer
        CountDownLatch consumeLatch = new CountDownLatch(1);
        vehicleDataConsumer.consumeVehicleData()
            .doOnNext(data -> consumeLatch.countDown())
            .subscribe();

        // Wait for consumer to be ready and Kafka to be fully usable
        Thread.sleep(3000);

        // Act – use longer timeout for first send (metadata + connection)
        kafkaTemplate.send(TOPIC, payload).get(30, TimeUnit.SECONDS);

        // Assert
        boolean messageConsumed = consumeLatch.await(15, TimeUnit.SECONDS);
        assert messageConsumed : "Message was not consumed within timeout";
    }



    private VehicleData createTestVehicleData() {
        VehicleData data = new VehicleData();
        data.setVehicleId("TEST-001");
        data.setContractId("CONTRACT-001");
        data.setTimestamp(Instant.now());
        
        VehicleData.SensorData sensorData = new VehicleData.SensorData();
        sensorData.setSpeed(createMeasurement(60.0, "km/h"));
        sensorData.setRpm(createMeasurement(2500.0, "rpm"));
        sensorData.setTimestamp(Instant.now());
        
        data.setData(sensorData);
        return data;
    }

    private VehicleData.Measurement createMeasurement(double value, String unit) {
        VehicleData.Measurement measurement = new VehicleData.Measurement();
        measurement.setValue(value);
        measurement.setUnit(unit);
        return measurement;
    }
} 