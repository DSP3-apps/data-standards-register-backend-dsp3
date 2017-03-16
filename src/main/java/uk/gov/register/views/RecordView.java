package uk.gov.register.views;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Iterables;
import io.dropwizard.jackson.Jackson;
import uk.gov.register.core.Entry;
import uk.gov.register.core.Field;
import uk.gov.register.core.FieldValue;
import uk.gov.register.core.Record;
import uk.gov.register.views.representations.CsvRepresentation;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RecordView implements CsvRepresentationView {
    private final Entry entry;
    private final Collection<ItemView> itemViews;
    private final Iterable<Field> fields;

    public RecordView(Entry entry, Collection<ItemView> itemViews, Iterable<Field> fields) {
        this.entry = entry;
        this.itemViews = itemViews;
        this.fields = fields;
    }

    public String getPrimaryKey() {
        return entry.getKey();
    }

    @SuppressWarnings("unused, used to create the json representation of this class")
    @JsonValue
    public ObjectNode getRecordJson() {
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        ObjectNode jsonNodes = objectMapper.convertValue(entry, ObjectNode.class);
        jsonNodes.remove("key");
        ArrayNode items = jsonNodes.putArray("items");
        itemViews.forEach( iv -> items.add(objectMapper.convertValue(iv, ObjectNode.class)));
        return jsonNodes;
    }

    ArrayNode getFlatRecordJson() {
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        itemViews.forEach( iv -> {
            ObjectNode jsonNodes = objectMapper.convertValue(entry, ObjectNode.class);
            jsonNodes.remove("key");
            jsonNodes.set("item-hashes", TextNode.valueOf(iv.getItemHash().encode()));
            jsonNodes.setAll(objectMapper.convertValue(iv, ObjectNode.class));
            arrayNode.add(jsonNodes);
        });

        return arrayNode;
    }

    public Set<Map<String, FieldValue>> getContent() {
        return itemViews.stream().map(iv -> iv.getContent()).collect(Collectors.toSet());
    }

    @Override
    public CsvRepresentation<ArrayNode> csvRepresentation() {
        Iterable<String> fieldNames = Iterables.transform(fields, f -> f.fieldName);
        return new CsvRepresentation<>(Record.csvSchema(fieldNames), getFlatRecordJson());
    }

    public Collection<ItemView> getItemViews() {
        return itemViews;
    }

    public Entry getEntry() {
        return entry;
    }

    public Iterable<Field> getFields() {
        return fields;
    }
}
