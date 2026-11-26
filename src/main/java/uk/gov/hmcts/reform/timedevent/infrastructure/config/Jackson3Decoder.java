package uk.gov.hmcts.reform.timedevent.infrastructure.config;

import feign.Response;
import feign.Util;
import feign.codec.Decoder;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class Jackson3Decoder implements Decoder {

    private final JsonMapper mapper;

    public Jackson3Decoder(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException {
        if (response.status() == 404 || response.status() == 204 || response.body() == null) {
            return Util.emptyValueOf(type);
        }
        if (type == String.class) {
            try (Reader reader = response.body().asReader(StandardCharsets.UTF_8)) {
                return Util.toString(reader);
            }
        }
        if (type == byte[].class) {
            try (InputStream in = response.body().asInputStream()) {
                return in.readAllBytes();
            }
        }
        try (InputStream in = response.body().asInputStream()) {
            return mapper.readValue(in, mapper.constructType(type));
        }
    }
}