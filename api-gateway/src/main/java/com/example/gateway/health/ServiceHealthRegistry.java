package com.example.gateway.health;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ServiceHealthRegistry {
    private final Map<String, ServiceStatus> services = new ConcurrentHashMap<>();

    public ServiceHealthRegistry() {
        services.put("users", new ServiceStatus("users", "http://localhost:5001/health"));
        services.put("products", new ServiceStatus("products", "http://localhost:5002/health"));
        services.put("orders", new ServiceStatus("orders", "http://localhost:5003/health"));
    }

    public Map<String, ServiceStatus> all() {
        return services;
    }

    public boolean isAvailable(String serviceName) {
        ServiceStatus status = services.get(serviceName);
        return status != null && status.available();
    }

    public ServiceStatus get(String serviceName) {
        return services.get(serviceName);
    }

    public record ServiceStatus(String name, String healthUrl) {
        private static final Map<String, MutableStatus> STATE = new ConcurrentHashMap<>();

        public ServiceStatus {
            STATE.putIfAbsent(name, new MutableStatus());
        }

        public boolean available() {
            return STATE.get(name).available;
        }

        public int failures() {
            return STATE.get(name).failures;
        }

        public void markSuccess() {
            MutableStatus state = STATE.get(name);
            state.failures = 0;
            state.available = true;
        }

        public void markFailure() {
            MutableStatus state = STATE.get(name);
            state.failures++;
            if (state.failures >= 2) {
                state.available = false;
            }
        }

        private static class MutableStatus {
            private volatile boolean available = true;
            private volatile int failures = 0;
        }
    }
}
