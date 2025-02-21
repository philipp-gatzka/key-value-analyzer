package ch.gatzka.configuration;

import com.apollographql.java.client.ApolloClient;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
public class ApplicationConfiguration {

    private ApolloClient apolloClient;

    private static final String ENDPOINT = "https://api.tarkov.dev/graphql";

    @Bean
    public ApolloClient apolloClient() {
        this.apolloClient = new ApolloClient.Builder().serverUrl(ENDPOINT).build();
        return this.apolloClient;
    }

    @PreDestroy
    public void cleanUp() {
        if (apolloClient != null) {
            apolloClient.close();
        }
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
