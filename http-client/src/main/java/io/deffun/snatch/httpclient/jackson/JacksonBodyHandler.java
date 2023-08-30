package io.deffun.snatch.httpclient.jackson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;

public final class JacksonBodyHandler<T> implements HttpResponse.BodyHandler<T> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TypeReference<T> typeReference;

    private JacksonBodyHandler(TypeReference<T> typeReference) {
        this.typeReference = typeReference;
    }

    public static <T> JacksonBodyHandler<T> ofTypReference(TypeReference<T> typeReference) {
        return new JacksonBodyHandler<>(typeReference);
    }

    @Override
    public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
        HttpResponse.BodySubscriber<InputStream> upstream = HttpResponse.BodySubscribers.ofInputStream();
        return HttpResponse.BodySubscribers.mapping(
                upstream,
                inputStream -> {
                    try (InputStream stream = inputStream) {
                        return MAPPER.readValue(stream, typeReference);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }
}
