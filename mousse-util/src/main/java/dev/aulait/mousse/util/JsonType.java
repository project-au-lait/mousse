package dev.aulait.mousse.util;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Type token for generic JSON deserialization.
 *
 * <p>Use this class to specify generic types when deserializing JSON, similar to {@link
 * com.fasterxml.jackson.core.type.TypeReference}. This avoids exposing Jackson types in the public
 * API.
 *
 * <pre>{@code
 * List<Item> items = JsonUtils.str2obj(json, new JsonType<List<Item>>() {});
 * }</pre>
 */
public class JsonType<T> extends TypeReference<T> {}
