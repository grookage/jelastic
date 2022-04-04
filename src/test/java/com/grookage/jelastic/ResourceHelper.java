package com.grookage.jelastic;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;

public class ResourceHelper {

    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T getResource(String path, Class<T> klass) throws IOException {
        final InputStream data = ResourceHelper.class.getClassLoader().getResourceAsStream(path);
        return objectMapper.readValue(data, klass);
    }
}
