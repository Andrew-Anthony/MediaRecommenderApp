package ro.anthony.mediarecommenderapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TmdbConfig {

    @Value("${tmdb.api.base-url}")
    private String baseUrl;

    @Value("${tmdb.api.read-access-token}")
    private String token;

    @Bean
    public RestClient tmdbRestClient(){
        return RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + token)
            .build();
    }

}
