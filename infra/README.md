# Infrastructure Configuration

This directory contains infrastructure-related configuration files for Chatkeep.

## Directory Structure

```
infra/
├── prometheus/
│   └── prometheus.yml      # Prometheus scrape configuration
└── grafana/
    └── provisioning/
        └── datasources/
            └── prometheus.yml  # Grafana datasource config
```

## Monitoring Stack

To start the monitoring stack:

```bash
docker compose -f docker-compose.monitoring.yml up -d
```

### Access

- **Grafana**: http://localhost:3000 (default: admin/admin)
- **Prometheus**: http://localhost:9090

### Metrics

The application exposes Prometheus metrics at `/actuator/prometheus`.

Prometheus scrapes metrics every 15 seconds from:
- Spring Boot application (port 8080)
- Prometheus itself (port 9090)

### Custom Metrics

To add custom metrics to your Spring Boot application:

```kotlin
import io.micrometer.core.instrument.MeterRegistry

@Service
class MyService(private val registry: MeterRegistry) {

    fun trackEvent(eventType: String) {
        registry.counter("chatkeep.events", "type", eventType).increment()
    }
}
```

## Grafana Dashboards

After starting Grafana, you can import dashboards:

1. **Spring Boot Dashboard**: Import ID `11378` for comprehensive Spring Boot metrics
2. **JVM Dashboard**: Import ID `4701` for JVM metrics

Or create custom dashboards using the Prometheus datasource.

## Production Notes

In production, configure proper retention and storage:

- Prometheus: 30 days default (configurable via `--storage.tsdb.retention.time`)
- Grafana: Persistent volume for dashboards
- Alerting: Configure Alertmanager for notifications (not included in minimal setup)
