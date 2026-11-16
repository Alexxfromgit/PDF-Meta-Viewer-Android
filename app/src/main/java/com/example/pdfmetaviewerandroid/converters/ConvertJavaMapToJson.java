package com.example.pdfmetaviewerandroid.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.TreeMap;

public final class ConvertJavaMapToJson {
    public String convertMapToJson(Map<String, String> values) {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(new TreeMap<>(values));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Metadata could not be converted to JSON.", exception);
        }
    }
}
