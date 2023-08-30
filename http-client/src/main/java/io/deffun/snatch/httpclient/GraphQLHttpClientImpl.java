package io.deffun.snatch.httpclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import io.deffun.snatch.httpclient.jackson.JacksonBodyHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GraphQLHttpClientImpl implements GraphQLHttpClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final URI uri;

    GraphQLHttpClientImpl(URI uri) {
        this.uri = uri;
    }

    @Override
    public ExecutionResult post(ExecutionInput executionInput) {
        return post(executionInput, Collections.emptyList());
    }

    @Override
    public ExecutionResult post(ExecutionInput executionInput, List<Pair<String, String>> headers) {
        HttpRequest httpRequest = prepareRequest(uri, headers, executionInput);
        HttpClient httpClient = HttpClient.newHttpClient();
        try {
            HttpResponse<Map<String, Object>> httpResponse = httpClient.send(httpRequest, JacksonBodyHandler.ofTypReference(new TypeReference<>() {}));
            Map<String, Object> body = httpResponse.body();
            return ExecutionResult.newExecutionResult()
                    .data(body.get("data"))
                    .build();
        } catch (IOException e) {
            throw new HttpClientException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpClientException(e);
        }
    }

    private static HttpRequest prepareRequest(URI uri, List<Pair<String, String>> headers, ExecutionInput executionInput) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json");
        for (Pair<String, String> header : headers) {
            builder.header(header.getKey(), header.getValue());
        }
        return builder
                .POST(HttpRequest.BodyPublishers.ofString(inputToString(executionInput)))
                .build();
    }

    private static String inputToString(ExecutionInput executionInput) {
        Map<String, Object> map = inputToMap(executionInput);
        try {
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new HttpClientException(e);
        }
    }

    private static Map<String, Object> inputToMap(ExecutionInput executionInput) {
        Map<String, Object> request = new HashMap<>();
        request.put("query", executionInput.getQuery());
        if (executionInput.getOperationName() != null) {
            request.put("operationName", executionInput.getOperationName());
        }
        if (executionInput.getVariables() != null) {
            request.put("variables", executionInput.getVariables());
        }
        return request;
    }
}
