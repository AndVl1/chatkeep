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

### Local Development

To start the monitoring stack locally:

```bash
docker compose -f docker-compose.monitoring.yml up -d
```

### Production Deployment

The monitoring stack connects to the existing `chatkeep_default` network:

```bash
# On production server
cd /root/chatkeep
docker compose -f docker-compose.monitoring.yml up -d
```

### Access

- **Grafana**: http://localhost:3000 (default: admin/admin)
  - Production: http://89.125.243.104:3000
- **Prometheus**: http://localhost:9090
  - Production: http://89.125.243.104:9090

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

### Pre-configured Dashboard

A basic "Chatkeep - Spring Boot Metrics" dashboard is automatically created with:
- Uptime monitoring
- JVM Memory usage
- HTTP request rate
- GC pause time
- Thread count

### Additional Dashboards

You can import more comprehensive dashboards:

1. **Spring Boot Dashboard**: Import ID `11378` for comprehensive Spring Boot metrics
2. **JVM Dashboard**: Import ID `4701` for JVM metrics

Or create custom dashboards using the Prometheus datasource.

## Production Notes

In production, configure proper retention and storage:

- Prometheus: 30 days default (configurable via `--storage.tsdb.retention.time`)
- Grafana: Persistent volume for dashboards
- Alerting: Configure Alertmanager for notifications (not included in minimal setup)
