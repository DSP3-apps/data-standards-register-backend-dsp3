package uk.gov.register.presentation.representations;

import org.apache.commons.lang3.StringEscapeUtils;
import uk.gov.register.presentation.EntryView;
import uk.gov.register.presentation.FieldValue;
import uk.gov.register.presentation.ListValue;
import uk.gov.register.presentation.config.Register;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Produces(ExtraMediaType.TEXT_CSV)
public class CsvWriter extends RepresentationWriter {
    @Override
    protected void writeEntriesTo(OutputStream entityStream, Register register, List<EntryView> entries) throws IOException, WebApplicationException {
        Iterable<String> fields = register.getFields();
        entityStream.write(("entry," + String.join(",", fields) + "\r\n").getBytes("utf-8"));
        for (EntryView entry : entries) {
            writeRow(entityStream, fields, entry);
        }
    }

    private void writeRow(OutputStream entityStream, Iterable<String> fields, EntryView entry) throws IOException {
        String row = StreamSupport.stream(fields.spliterator(),false)
                .map(field -> escape(entry.getField(field).map(this::renderField).orElse("")))
                .collect(Collectors.joining(",", entry.getSerialNumber() + ",", "\r\n"));
        entityStream.write(row.getBytes("utf-8"));
    }

    private String renderField(FieldValue fieldValue) {
        if (fieldValue.isList()) {
            return renderList((ListValue) fieldValue);
        }
        return fieldValue.getValue();
    }

    private String renderList(ListValue listValue) {
        return listValue.stream()
                .map(FieldValue::getValue)
                .collect(Collectors.joining(";"));
    }

    private String escape(String data) {
        return StringEscapeUtils.escapeCsv(data);
    }

}
