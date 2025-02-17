package ch.gatzka.configuration;

import com.apollographql.java.client.ApolloClient;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ApplicationConfiguration {

    private ApolloClient apolloClient;

    @Value("${spring.graphql.endpoint}")
    private String endpoint;

    @Bean
    public ApolloClient apolloClient() {
        this.apolloClient = new ApolloClient.Builder().serverUrl(endpoint).build();
        return this.apolloClient;
    }

    @PreDestroy
    public void cleanUp() {
        if (apolloClient != null) {
            apolloClient.close();
        }
    }
}
