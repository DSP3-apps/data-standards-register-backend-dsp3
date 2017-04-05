package uk.gov.register.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import uk.gov.register.core.Field;
import uk.gov.register.util.ResourceYamlFileReader;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class FieldsConfiguration {

    private final Collection<Field> fields;

    public FieldsConfiguration(String fieldsResourceYamlPath) {
        Collection<ConfigRecord<Field>> configRecords = new ResourceYamlFileReader().readResourceFromPath(fieldsResourceYamlPath, new TypeReference<Map<String, ConfigRecord<Field>>>() {
        });
        fields = configRecords.stream().map(ConfigRecord::getSingleItem).collect(toList());
    }

    public Field getField(String fieldName) {
        return fields.stream().filter(f -> Objects.equals(f.fieldName, fieldName)).findFirst().get();
    }

    public Collection<Field> getAllFields() {
        return fields;
    }
}
