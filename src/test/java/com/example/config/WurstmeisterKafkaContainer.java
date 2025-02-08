package com.example.config;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class WurstmeisterKafkaContainer extends GenericContainer<WurstmeisterKafkaContainer> {
    private static final int KAFKA_PORT = 9092;
    private static final String DEFAULT_IMAGE = "wurstmeister/kafka:latest";
    private final GenericContainer<?> zookeeper;
    private final Network network;

    public WurstmeisterKafkaContainer() {
        this(DEFAULT_IMAGE);
    }

    public WurstmeisterKafkaContainer(String image) {
        super(DockerImageName.parse(image));
        
        network = Network.newNetwork();
        
        // Créer et configurer le conteneur Zookeeper
        zookeeper = new GenericContainer<>(DockerImageName.parse("wurstmeister/zookeeper:latest"))
            .withNetwork(network)
            .withNetworkAliases("zookeeper")
            .withExposedPorts(2181)
            .waitingFor(Wait.forListeningPort());
        
        // Configurer le conteneur Kafka
        withNetwork(network)
        .withExposedPorts(KAFKA_PORT)
        .withEnv("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://" + getHost() + ":" + KAFKA_PORT)
        .withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:" + KAFKA_PORT)
        .withEnv("KAFKA_ZOOKEEPER_CONNECT", "zookeeper:2181")
        .withEnv("KAFKA_CREATE_TOPICS", "vehicle-data:1:1")
        .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
        .dependsOn(zookeeper)
        .waitingFor(Wait.forListeningPort());
    }

    @Override
    public void start() {
        zookeeper.start();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        zookeeper.stop();
        network.close();
    }

    public String getBootstrapServers() {
        return String.format("%s:%d", getHost(), getMappedPort(KAFKA_PORT));
    }
} 