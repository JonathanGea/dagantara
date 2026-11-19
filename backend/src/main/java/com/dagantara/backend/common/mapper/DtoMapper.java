package com.dagantara.backend.common.mapper;

/** Small contract for explicit entity-to-DTO mappings, not an automatic entity serializer. */
@FunctionalInterface
public interface DtoMapper<S, T> {
    T toDto(S source);
}
