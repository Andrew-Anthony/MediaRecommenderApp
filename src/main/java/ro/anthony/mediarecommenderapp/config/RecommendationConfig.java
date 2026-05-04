package ro.anthony.mediarecommenderapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RecommendationConfig {
    @Value("${recommendation.service.url}")
    private String baseUrl;

    @Bean
    public RestClient recommendationRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
