package io.deffun.snatch.core;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.execution.DataFetcherResult;
import graphql.language.AstPrinter;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.deffun.snatch.httpclient.GraphQLHttpClient;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public final class GraphQLHttpDataFetcher implements DataFetcher<Object> {
    private final URI uri;
    private final List<Pair<String, String>> headers;

    private GraphQLHttpDataFetcher(URI uri, List<Pair<String, String>> headers) {
        this.uri = uri;
        this.headers = headers;
    }

    public static GraphQLHttpDataFetcher headersAuth(URI uri, List<Pair<String, String>> headers) {
        return new GraphQLHttpDataFetcher(uri, headers);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        List<Selection> selections = environment.getField().getSelectionSet().getSelections();
        OperationDefinition operationDefinition = environment.getOperationDefinition();
        String operation = operationDefinition.getOperation().name().toLowerCase();
        String query = selections.stream()
                .map(AstPrinter::printAstCompact)
                .collect(Collectors.joining(" ", operation + " { ", " }"));
        query = StringEscapeUtils.escapeJson(query);
        ExecutionInput.Builder newExecutionInput = ExecutionInput.newExecutionInput()
                .query(query);
        ExecutionInput executionInput = newExecutionInput
                .variables(environment.getVariables())
                .build();
        GraphQLHttpClient httpClient = GraphQLHttpClient.createClient(uri);
        ExecutionResult result = httpClient.post(executionInput, headers);
        return DataFetcherResult.newResult()
                .data(result.getData())
                .errors(result.getErrors())
                .build();
    }
}
