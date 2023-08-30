# Snatch

A simple stitching library for Java.

## Usage

Add dependency via JitPack:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.deffunio.snatch</groupId>
    <artifactId>snatch-core</artifactId>
    <version>0.2.0</version>
</dependency>
```

Then add the following GraphQL directive:
```graphql
input Header {
  name: String!, value: String!
}
input Renames {
  operationName: String!
  typePrefix: String!
}
directive @graphql(
  renames: Renames!
  endpoint: String!
  headers: [Header!]
) repeatable on OBJECT
```

Use this directive in your schema:
```graphql
type Query
@graphql(
  renames: {
    operationName: "rickAndMorty"
    typePrefix: "RM"
  }
  endpoint: "https://rickandmortyapi.com/graphql"
) {
```

And finally a bit of a code to wire all things up:
```java
GraphQLSchema transformed = new SchemaTransformer().transform(graphQLSchema, new GraphQLDirectiveVisitor());
GraphQL graphQL = GraphQL.newGraphQL(transformed).build();
```

Now you can execute a query like this:
```java
ExecutionResult executionResult = graphQL.execute(executionInput);
```

Use `localContext` to pass additional headers:
```java
ExecutionInput executionInput = ExecutionInput.newExecutionInput()
        .localContext(new SnatchContext(List.of(Pair.of("key", "val"))))
        .query(...)
        .build();
```
