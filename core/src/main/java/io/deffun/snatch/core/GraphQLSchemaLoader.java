package io.deffun.snatch.core;

import graphql.schema.GraphQLSchema;

public interface GraphQLSchemaLoader {
    GraphQLSchema load();
}
