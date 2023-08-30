package io.deffun.snatch.httpclient;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import org.apache.commons.lang3.tuple.Pair;

import java.net.URI;
import java.util.List;

public interface GraphQLHttpClient {
    ExecutionResult post(ExecutionInput executionInput);

    ExecutionResult post(ExecutionInput executionInput, List<Pair<String, String>> headers);

    static GraphQLHttpClient createClient(URI uri) {
        return new GraphQLHttpClientImpl(uri);
    }
}
