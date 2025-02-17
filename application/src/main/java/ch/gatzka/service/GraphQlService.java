package ch.gatzka.service;

import com.apollographql.apollo.api.ApolloResponse;
import com.apollographql.apollo.api.Query;
import com.apollographql.java.client.ApolloClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphQlService {

    private final ApolloClient apolloClient;

    public <R extends Query.Data> R runQuery(Query<R> query) {
        log.info("Running GraphQL query {}", query.name());

        CompletableFuture<ApolloResponse<R>> future = new CompletableFuture<>();

        final long startTime = System.currentTimeMillis();
        apolloClient.query(query).enqueue(response -> {
            if (response.exception != null) {
                log.error("GraphQL query {} failed after {}ms", query.name(), System.currentTimeMillis() - startTime, response.exception);
                future.completeExceptionally(response.exception);
                return;
            } else {
                log.info("GraphQL query {} finished after {}ms", query.name(), System.currentTimeMillis() - startTime);
            }
            future.complete(response);
        });

        log.debug("Waiting for GraphQL query {} to finish", query.name());
        return future.join().data;
    }

}
