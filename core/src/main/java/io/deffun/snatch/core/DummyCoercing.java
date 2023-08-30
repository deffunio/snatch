/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.deffun.snatch.core;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

// from http://github.com/friatech/lilo
public class DummyCoercing implements Coercing<Object, Object> {
    @Override
    public @Nullable Object parseLiteral(
            @NotNull Value<?> input,
            @NotNull CoercedVariables variables,
            @NotNull GraphQLContext graphQLContext,
            @NotNull Locale locale)
            throws CoercingParseLiteralException {
        return input;
    }

    @Override
    public @Nullable Object parseValue(
            @NotNull Object input,
            @NotNull GraphQLContext graphQLContext,
            @NotNull Locale locale)
            throws CoercingParseValueException {
        return input;
    }

    @Override
    public @Nullable Object serialize(
            @NotNull Object dataFetcherResult,
            @NotNull GraphQLContext graphQLContext,
            @NotNull Locale locale)
            throws CoercingSerializeException {
        return dataFetcherResult;
    }
}
