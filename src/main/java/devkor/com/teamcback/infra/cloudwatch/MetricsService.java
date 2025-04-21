package devkor.com.teamcback.infra.cloudwatch;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class MetricsService {
    private final CloudWatchAsyncClient cloudWatchAsyncClient;
    private final Map<String, AtomicInteger> uriCountMap = new ConcurrentHashMap<>();

    public void recordApiRequest(String uri) {

        uriCountMap.computeIfAbsent(uri, k -> new AtomicInteger(0)).incrementAndGet();

    }

    @Scheduled(fixedRate = 60_000)
    public void sendMetricsToCloudWatch() {
        List<MetricDatum> metricDataList = uriCountMap.entrySet().stream()
                        .map(entry -> {
                            String uri = entry.getKey();
                            int count = entry.getValue().getAndSet(0);

                            if(count > 0) {
                                return MetricDatum.builder()
                                        .metricName("ApiRequestCount")
                                        .dimensions(
                                                Dimension.builder().name("URI").value(uri).build()
                                        )
                                        .unit(StandardUnit.COUNT)
                                        .value((double) count)
                                        .timestamp(Instant.now())
                                        .build();
                            }
                            return null;
                        })
                .filter(Objects::nonNull)
                .toList();

        if(!metricDataList.isEmpty()) {
            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace("MyApp/Metrics")
                    .metricData(metricDataList)
                    .build();

            cloudWatchAsyncClient.putMetricData(request).whenComplete((resp, err) -> {
                if (err != null) {
                    System.err.println("Failed to send metric: " + err.getMessage());
                }
            });
        }
    }
}
