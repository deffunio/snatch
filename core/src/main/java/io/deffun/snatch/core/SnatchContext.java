package io.deffun.snatch.core;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public record SnatchContext(List<Pair<String, String>> headers) {
}
