package uk.gov.register.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterables;
import org.jvnet.hk2.annotations.Service;
import uk.gov.register.presentation.config.Field;
import uk.gov.register.presentation.config.FieldsConfiguration;
import uk.gov.register.presentation.resource.RequestContext;

import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static uk.gov.register.presentation.Cardinality.ONE;

@Service
public class EntryConverter {
    private final FieldsConfiguration fieldsConfiguration;
    private final RequestContext requestContext;

    @Inject
    public EntryConverter(FieldsConfiguration fieldsConfiguration, RequestContext requestContext) {
        this.fieldsConfiguration = fieldsConfiguration;
        this.requestContext = requestContext;
    }

    public EntryView convert(DbEntry dbEntry) {
        Iterable<Map.Entry<String, JsonNode>> fields = () -> dbEntry.getContent().getContent().fields();
        Stream<Map.Entry<String, JsonNode>> fieldStream = StreamSupport.stream(fields.spliterator(), false);
        return new EntryView(
                dbEntry.getSerialNumber(),
                dbEntry.getContent().getHash(),
                requestContext.getRegisterPrimaryKey(),
                fieldStream.collect(Collectors.toMap(Map.Entry::getKey, this::convert))
        );
    }

    private FieldValue convert(Map.Entry<String, JsonNode> mapEntry) {
        String fieldName = mapEntry.getKey();
        JsonNode value = mapEntry.getValue();
        FieldConverter fieldConverter = new FieldConverter(fieldsConfiguration.getField(fieldName));
        return fieldConverter.convert(value);
    }

    private static class FieldConverter {
        private final Field field;

        public FieldConverter(Field field) {
            this.field = field;
        }

        public FieldValue convert(JsonNode value) {
            Cardinality cardinality = field.getCardinality();
            if (cardinality == ONE) {
                return convertScalar(value);
            } else {
                return convertArray(value);
            }
        }

        private FieldValue convertArray(JsonNode value) {
            return new ListValue(
                    Iterables.transform(value, this::convertScalar)
            );
        }

        private FieldValue convertScalar(JsonNode value) {
            if (field.getRegister().isPresent()) {
                return new LinkValue(field.getRegister().get(), value.textValue());
                //Note: the equals check below must be replaced with the specified datatype, instead of doing string comparision
                // We should replace this once the datatype register is available
            } else if (field.getDatatype().equals("curie")) {
                return new LinkValue.CurieValue(value.textValue());
            } else {
                return new StringValue(value.textValue());
            }
        }
    }
}
