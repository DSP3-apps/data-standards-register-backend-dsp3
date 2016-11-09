package uk.gov.register.serialization;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.register.core.Entry;
import uk.gov.register.core.Item;
import uk.gov.register.exceptions.SerializedRegisterParseException;
import uk.gov.register.util.SerializedRegisterParser;
import uk.gov.register.views.RegisterProof;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;

public class CommandParser {
    private static final Logger LOG = LoggerFactory.getLogger(SerializedRegisterParser.class);

    public RegisterCommand newCommand(String s){
        String[] parts = s.split("\t");
        String commandName = parts[0];
        switch (commandName) {
            case "add-item":
                if (parts.length == 2) {
                    try {
                        Item item = new Item(new ObjectMapper().readTree(parts[1]));
                        return new AddItemCommand(item);
                    } catch (JsonParseException jpe){
                        LOG.error("failed to parse json: " + parts[1]);
                        throw new SerializedRegisterParseException("failed to parse json: " + parts[1], jpe);
                    } catch (IOException e) {
                        LOG.error("",e);
                        throw new UncheckedIOException(e);
                    }
                } else {
                    LOG.error("add item line must have 2 elements, was: " + s);
                    throw new SerializedRegisterParseException("add item line must have 2 elements, was: " + s);
                }
            case "append-entry":
                if (parts.length == 3) {
                    Entry entry = new Entry(0, stripPrefix(parts[2]), Instant.parse(parts[1]));
                    return new AppendEntryCommand(entry);
                } else {
                    LOG.error("append entry line must have 3 elements, was : " + s);
                    throw new SerializedRegisterParseException("append entry line must have 3 elements, was : " + s);
                }
            case "assert-root-hash":
                LOG.error("assert-root-hash not yet supported");
                throw new NotImplementedException("assert-root-hash not yet supported");
            default:
                LOG.error("line must begin with legal command not:" + commandName);
                throw new SerializedRegisterParseException("line must begin with legal command not: " + commandName);
        }
    }

    private  String stripPrefix(String hashField) {
        if (!hashField.startsWith("sha-256:")) {
            LOG.error("hash field must start with sha-256: not:" + hashField);
            throw new SerializedRegisterParseException("hash field must start with sha-256: not: " + hashField);
        } else {
            return hashField.substring(8);
        }
    }

    public String serialise(Entry e){
        return e.toString();
    }
    public String serialise(Item i){
        return i.toString();
    }
    public String serialise(RegisterProof registerProof){
        return registerProof.toString();
    }


}
