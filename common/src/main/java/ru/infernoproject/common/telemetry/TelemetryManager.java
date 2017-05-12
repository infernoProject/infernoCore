package ru.infernoproject.common.telemetry;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.config.ConfigFile;
import ru.infernoproject.common.utils.ErrorUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TelemetryManager<T> {

    private final InfluxDB telemetryDataBase;

    private final String telemetryInstanceName;
    private final String telemetryDataBaseName;

    private final T telemetrySource;

    private final List<Method> metrics;

    private static final Logger logger = LoggerFactory.getLogger(TelemetryManager.class);

    public TelemetryManager(ConfigFile config, T source) {
        if (config.hasKey("telemetry.db.user") && config.hasKey("telemetry.db.password")) {
            telemetryDataBase = InfluxDBFactory.connect(
                config.getString("telemetry.db.url", "http://localhost:8086"),
                config.getString("telemetry.db.user", "user"),
                config.getString("telemetry.db.password", "password")
            );
        } else {
            telemetryDataBase = InfluxDBFactory.connect(
                config.getString("telemetry.db.url", "http://localhost:8086")
            );
        }

        telemetryDataBaseName = config.getString("telemetry.db.name", "inferno");
        telemetryInstanceName = config.getString("telemetry.instance_name", "unknown");

        telemetrySource = source;
        metrics = new ArrayList<>();

        registerMetrics();
    }


    private boolean validateMetric(Method metric) {
        return metric.isAnnotationPresent(TelemetryCollector.class) &&
            metric.getReturnType().equals(Point[].class) &&
            metric.getParameterCount() == 0;
    }

    private void registerMetrics() {
        logger.info("Looking for collectible metrics in {}", telemetrySource.getClass().getSimpleName());
        for (Method metric: telemetrySource.getClass().getDeclaredMethods()) {
            if (!validateMetric(metric))
                continue;

            logger.debug(String.format("MetricCollector: %s", metric.getName()));
            metrics.add(metric);
        }
        logger.info("{} metrics registered for {}", metrics.size(), telemetrySource.getClass().getSimpleName());
    }

    public void sendMetrics() {
        BatchPoints batchPoints = BatchPoints.database(telemetryDataBaseName)
            .tag("async", "true")
            .tag("instance", telemetryInstanceName)
            .retentionPolicy("autogen")
            .consistency(InfluxDB.ConsistencyLevel.ALL)
            .build();

        metrics.stream().map(metric -> {
            try {
                return (Point[]) metric.invoke(telemetrySource);
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                ErrorUtils.logger(logger).error("Unable to collect metric", e);
            }

            return null;
        }).filter(Objects::nonNull).forEach(points -> {
            for (Point point: points) {
                batchPoints.point(point);
            }
        });

        telemetryDataBase.write(batchPoints);
    }


    public Point.Builder buildMetric(String measurement) {
        return Point.measurement(measurement)
            .tag("instance", telemetryInstanceName)
            .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }
}
