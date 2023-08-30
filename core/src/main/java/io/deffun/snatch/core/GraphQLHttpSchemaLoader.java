package io.deffun.snatch.core;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.Scalars;
import graphql.introspection.IntrospectionQueryBuilder;
import graphql.introspection.IntrospectionResultToSchema;
import graphql.language.Document;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.UnionTypeDefinition;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.deffun.snatch.httpclient.GraphQLHttpClient;
import org.apache.commons.lang3.tuple.Pair;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GraphQLHttpSchemaLoader implements GraphQLSchemaLoader {
    private static final Set<String> PREDEFINED_SCALARS = Set.of(
            Scalars.GraphQLInt.getName(), Scalars.GraphQLFloat.getName(), Scalars.GraphQLString.getName(),
            Scalars.GraphQLBoolean.getName(), Scalars.GraphQLID.getName()
    );
    private static final TypeResolver INTERFACE_TYPE_RESOLVER = env -> null;
    private static final TypeResolver UNION_TYPE_RESOLVER = env -> {
        Map<String, Object> result = env.getObject();
        Object o = result.get("__typename");
        if (!(o instanceof String typename)) {
            throw new IllegalArgumentException("Please provide __typename for union types");
        }
        return env.getSchema().getObjectType(typename);
    };

    private final URI uri;
    private final List<Pair<String, String>> headers;

    public GraphQLHttpSchemaLoader(URI uri, List<Pair<String, String>> headers) {
        this.uri = uri;
        this.headers = headers;
    }

    @Override
    public GraphQLSchema load() {
        IntrospectionQueryBuilder.Options options = IntrospectionQueryBuilder.Options.defaultOptions()
                .inputValueDeprecation(false)
                .directiveIsRepeatable(false);
        String introspectionQuery = IntrospectionQueryBuilder.build(options);
        ExecutionInput executionInput = ExecutionInput.newExecutionInput(introspectionQuery)
                .operationName("IntrospectionQuery")
                .build();
        GraphQLHttpClient httpClient = GraphQLHttpClient.createClient(uri);
        ExecutionResult result = httpClient.post(executionInput, headers);
        IntrospectionResultToSchema introspectionResultToSchema = new IntrospectionResultToSchema();
        Document document = introspectionResultToSchema.createSchemaDefinition(result);
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.buildRegistry(document);
        RuntimeWiring runtimeWiring = dummyWiring(typeDefinitionRegistry);
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
    }

    private static RuntimeWiring dummyWiring(TypeDefinitionRegistry typeDefinitionRegistry) {
        RuntimeWiring.Builder newRuntimeWiring = RuntimeWiring.newRuntimeWiring();
        typeDefinitionRegistry.scalars().values()
                .forEach(sd -> {
                    if (!PREDEFINED_SCALARS.contains(sd.getName())) {
                        GraphQLScalarType dummyScalarType = GraphQLScalarType.newScalar()
                                .name(sd.getName())
                                .coercing(new DummyCoercing())
                                .build();
                        newRuntimeWiring.scalar(dummyScalarType);
                    }
                });
        typeDefinitionRegistry.types().values()
                .forEach(t -> {
                    if (t instanceof InterfaceTypeDefinition) {
                        newRuntimeWiring.type(t.getName(), wiring -> wiring.typeResolver(INTERFACE_TYPE_RESOLVER));
                    } else if (t instanceof UnionTypeDefinition) {
                        newRuntimeWiring.type(t.getName(), wiring -> wiring.typeResolver(UNION_TYPE_RESOLVER));
                    }
                });
        return newRuntimeWiring.build();
    }
}
