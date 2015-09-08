package uk.gov.register.presentation.config;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import io.dropwizard.jackson.Jackson;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class FieldsConfiguration {

    private final List<Field> fields;

    public FieldsConfiguration() throws IOException {
        InputStream fieldsStream = this.getClass().getClassLoader().getResourceAsStream("fields.yaml");
        ObjectMapper yamlObjectMapper = Jackson.newObjectMapper(new YAMLFactory());
        List<FieldData> rawFields = yamlObjectMapper.readValue(fieldsStream, new TypeReference<List<FieldData>>() {
        });
        fields = Lists.transform(rawFields, m -> m.entry);
    }

    public Field getField(String fieldName) {
        return fields.stream().filter(f -> Objects.equals(f.fieldName, fieldName)).findFirst().get();
    }

    @JsonIgnoreProperties({"hash", "last-updated"})
    private static class FieldData{
        @JsonProperty
        Field entry;
    }
}
