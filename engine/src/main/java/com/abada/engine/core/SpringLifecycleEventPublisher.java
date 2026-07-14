package com.abada.engine.core;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
public class SpringLifecycleEventPublisher implements LifecycleEventPublisher {
    private final ApplicationEventPublisher publisher;
    private final RestClient restClient;
    private final List<String> webhookUrls;

    public SpringLifecycleEventPublisher(ApplicationEventPublisher publisher,
            @Value("${abada.outbox.webhook-urls:}") String webhookUrls) {
        this.publisher = publisher;
        this.restClient = RestClient.create();
        this.webhookUrls = Arrays.stream(webhookUrls.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).toList();
    }

    @Override
    public void publish(PublishedLifecycleEvent event) {
        publisher.publishEvent(event);
        for (String webhookUrl : webhookUrls) {
            restClient.post()
                    .uri(webhookUrl)
                    .header("X-Abada-Event-Id", event.id())
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
        }
    }
}
