package com.example.gateway.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class HeartbeatMonitor {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatMonitor.class);

    private final ServiceHealthRegistry registry;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public HeartbeatMonitor(ServiceHealthRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedDelayString = "${heartbeat.interval-ms}")
    public void checkServices() {
        registry.all().values().forEach(this::checkService);
    }

    private void checkService(ServiceHealthRegistry.ServiceStatus service) {
        boolean wasAvailable = service.available();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(service.healthUrl()))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                service.markSuccess();
                if (!wasAvailable) {
                    log.info("[{}] Servico {} recuperado", LocalDateTime.now(), service.name());
                }
                return;
            }
            service.markFailure();
        } catch (Exception error) {
            service.markFailure();
        }

        if (wasAvailable && !service.available()) {
            log.error("[{}] Servico {} indisponivel apos {} falhas", LocalDateTime.now(), service.name(), service.failures());
        }
    }
}
