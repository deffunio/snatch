package io.deffun.snatch.core;

import graphql.language.ArrayValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLAppliedDirective;
import graphql.schema.GraphQLAppliedDirectiveArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.schema.SchemaTransformer;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GraphQLDirectiveVisitor extends GraphQLTypeVisitorStub {
    private static final StringSubstitutor ENV_VARS_SUBSTITUTOR = new StringSubstitutor(s -> System.getenv().getOrDefault(s, ""));

    @Override
    public TraversalControl visitGraphQLObjectType(GraphQLObjectType node, TraverserContext<GraphQLSchemaElement> context) {
        GraphQLCodeRegistry.Builder codeRegistryBuilder = context.getVarFromParents(GraphQLCodeRegistry.Builder.class);
        if (node.hasAppliedDirective("graphql") &&
            ("Query".equals(node.getName()) || "Mutation".equals(node.getName()) || "Subscription".equals(node.getName()))) {
            List<GraphQLAppliedDirective> graphqlDirectives = node.getAppliedDirectives("graphql");
            List<GraphQLFieldDefinition> operationFields = new ArrayList<>();
            for (GraphQLAppliedDirective graphqlDirective : graphqlDirectives) {
                GraphQLAppliedDirectiveArgument renamesArg = graphqlDirective.getArgument("renames");
                if (renamesArg == null || !renamesArg.hasSetValue()) {
                    throw new IllegalArgumentException("'renames' is not set");
                }
                if (!(renamesArg.getArgumentValue().getValue() instanceof ObjectValue ov)) {
                    throw new IllegalArgumentException("'renames' is not an Object");
                }
                ObjectField operationNameField = ov.getObjectFields().stream()
                        .filter(f -> "operationName".equals(f.getName()))
                        .findAny()
                        .orElseThrow(() -> new IllegalArgumentException("'renames.operationName' is not set"));
                if (!(operationNameField.getValue() instanceof StringValue operationName)) {
                    throw new IllegalArgumentException("'renames.operationName' is not a String");
                }
                ObjectField typePrefixField = ov.getObjectFields().stream()
                        .filter(f -> "typePrefix".equals(f.getName()))
                        .findAny()
                        .orElseThrow(() -> new IllegalArgumentException("'renames.typePrefix' is not set"));
                if (!(typePrefixField.getValue() instanceof StringValue typePrefix)) {
                    throw new IllegalArgumentException("'renames.typePrefix' is not a String");
                }
                GraphQLAppliedDirectiveArgument endpointArg = graphqlDirective.getArgument("endpoint");
                if (endpointArg == null || !endpointArg.hasSetValue()) {
                    throw new IllegalArgumentException("'endpoint' is not set");
                }
                URI endpointUri = URI.create(endpointArg.getValue());
                List<Pair<String, String>> headers = collectHeaders(graphqlDirective);
                GraphQLSchema remote = new GraphQLHttpSchemaLoader(endpointUri, headers).load();
                String name = operationName.getValue();
                String prefix = typePrefix.getValue();
                GraphQLSchema transformedRemote = transformTypeNames(remote, prefix);
                operationFields.add(GraphQLFieldDefinition.newFieldDefinition()
                        .name(name)
                        .type(transformedRemote.getObjectType(prefix + node.getName()))
                        .build());
                GraphQLHttpDataFetcher dataFetcher = GraphQLHttpDataFetcher.headersAuth(endpointUri, headers);
                codeRegistryBuilder.dataFetcher(FieldCoordinates.coordinates(node.getName(), name), dataFetcher);
            }
            GraphQLObjectType transformedNode = node.transform(builder -> {
                for (GraphQLFieldDefinition operationField : operationFields) {
                    builder.field(operationField);
                }
            });
            return changeNode(context, transformedNode);
        }
        return super.visitGraphQLObjectType(node, context);
    }

    private GraphQLSchema transformTypeNames(GraphQLSchema remote, String prefix) {
        return new SchemaTransformer()
                .transform(remote, new GraphQLTypeVisitorStub() {
                    @Override
                    public TraversalControl visitGraphQLObjectType(GraphQLObjectType node, TraverserContext<GraphQLSchemaElement> context) {
                        GraphQLObjectType transform = node.transform(builder -> builder.name(prefix + node.getName()));
                        return changeNode(context, transform);
                    }

                    @Override
                    public TraversalControl visitGraphQLInputObjectType(GraphQLInputObjectType node, TraverserContext<GraphQLSchemaElement> context) {
                        GraphQLInputObjectType transform = node.transform(builder -> builder.name(prefix + node.getName()));
                        return changeNode(context, transform);
                    }
                });
    }

    @SuppressWarnings("rawtypes")
    @NotNull
    private static List<Pair<String, String>> collectHeaders(GraphQLAppliedDirective graphqlDirective) {
        GraphQLAppliedDirectiveArgument headersArg = graphqlDirective.getArgument("headers");
        if (headersArg == null || !headersArg.hasSetValue()) {
            return Collections.emptyList();
        }
        if (!(headersArg.getArgumentValue().getValue() instanceof ArrayValue av)) {
            throw new IllegalArgumentException("'headers' is not a List");
        }
        List<Pair<String, String>> headers = new ArrayList<>();
        for (Value v : av.getValues()) {
            if (!(v instanceof ObjectValue ov)) {
                throw new IllegalArgumentException("'headers' elements are not Objects");
            }
            ObjectField nameField = ov.getObjectFields().stream()
                    .filter(f -> "name".equals(f.getName()))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("'header.name' is not set"));
            ObjectField valueField = ov.getObjectFields().stream()
                    .filter(f -> "value".equals(f.getName()))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("'header.value' is not set"));
            if (!(nameField.getValue() instanceof StringValue name)) {
                throw new IllegalArgumentException("'name' is not a String");
            }
            if (!(valueField.getValue() instanceof StringValue value)) {
                throw new IllegalArgumentException("'value' is not a String");
            }
            headers.add(Pair.of(name.getValue(), ENV_VARS_SUBSTITUTOR.replace(value.getValue())));
        }
        return headers;
    }
}
