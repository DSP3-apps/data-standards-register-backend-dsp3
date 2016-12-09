package uk.gov.register.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.register.configuration.RegisterFieldsConfiguration;
import uk.gov.register.db.InMemoryEntryQueryDAO;
import uk.gov.register.exceptions.NoSuchFieldException;
import uk.gov.register.exceptions.NoSuchItemForEntryException;
import uk.gov.register.service.ItemValidator;
import uk.gov.register.store.BackingStoreDriver;
import uk.gov.register.util.HashValue;

import java.time.Instant;
import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
import static uk.gov.register.db.InMemoryStubs.inMemoryEntryLog;
import static uk.gov.register.db.InMemoryStubs.inMemoryItemStore;

public class PostgresRegisterTest {
    private final InMemoryEntryQueryDAO entryQueryDAO = new InMemoryEntryQueryDAO(new ArrayList<>());
    private BackingStoreDriver backingStoreDriver;
    private ItemValidator itemValidator;
    private RegisterFieldsConfiguration registerFieldsConfiguration;

    @Before
    public void setup() {
        backingStoreDriver = mock(BackingStoreDriver.class);
        itemValidator = mock(ItemValidator.class);
        registerFieldsConfiguration = mock(RegisterFieldsConfiguration.class);
    }

    @Test(expected = NoSuchFieldException.class)
    // TODO: Move to RecordIndexTest
    public void findMax100RecordsByKeyValueShouldFailWhenKeyDoesNotExist() {
        PostgresRegister register = new PostgresRegister(() -> "register", backingStoreDriver, registerFieldsConfiguration, inMemoryEntryLog(entryQueryDAO), inMemoryItemStore(itemValidator, entryQueryDAO));
        register.max100RecordsFacetedByKeyValue("citizen-name", "British");
    }

    @Test
    // TODO: Move to RecordIndexTest
    public void findMax100RecordsByKeyValueShouldReturnValueWhenKeyExists() {
        when(registerFieldsConfiguration.containsField("name")).thenReturn(true);
        PostgresRegister register = new PostgresRegister(() -> "register", backingStoreDriver, registerFieldsConfiguration, inMemoryEntryLog(entryQueryDAO), inMemoryItemStore(itemValidator, entryQueryDAO));
        register.max100RecordsFacetedByKeyValue("name", "United Kingdom");
        verify(backingStoreDriver, times(1)).findMax100RecordsByKeyValue("name", "United Kingdom");
    }

    @Test(expected = NoSuchItemForEntryException.class)
    public void appendEntryShouldThrowExceptionIfNoCorrespondingItemExists() {
        Entry entryDangling = new Entry(106, new HashValue(HashingAlgorithm.SHA256, "item-hash-2"), Instant.now(), "key");

        PostgresRegister register = new PostgresRegister(() -> "register", backingStoreDriver, registerFieldsConfiguration, inMemoryEntryLog(entryQueryDAO), inMemoryItemStore(itemValidator, entryQueryDAO));
        register.appendEntry(entryDangling);
    }

    @Test
    public void appendEntryShouldNotInsertDanglingEntry() {
        Item item = new Item(new HashValue(HashingAlgorithm.SHA256, "item-hash-1"), new ObjectMapper().createObjectNode());
        Entry entryNotDangling = new Entry(105, new HashValue(HashingAlgorithm.SHA256, "item-hash-1"), Instant.now(), "key-1");
        Entry entryDangling = new Entry(106, new HashValue(HashingAlgorithm.SHA256, "item-hash-2"), Instant.now(), "key-2");

        EntryLog entryLog = inMemoryEntryLog(entryQueryDAO);
        PostgresRegister register = new PostgresRegister(() -> "register", backingStoreDriver, registerFieldsConfiguration, entryLog, inMemoryItemStore(itemValidator, entryQueryDAO));

        try {
            register.putItem(item);
            register.appendEntry(entryNotDangling);
            register.appendEntry(entryDangling);
        } catch (NoSuchItemForEntryException ex) {
        }

        assertThat(entryLog.getAllEntries(), equalTo(ImmutableList.of(entryNotDangling)));
    }

    @Test
    public void appendEntryShouldNotInsertRecordForDanglingEntry() {
        Item item = new Item(new HashValue(HashingAlgorithm.SHA256, "item-hash-1"), new ObjectMapper().createObjectNode());
        Entry entryNotDangling = new Entry(105, new HashValue(HashingAlgorithm.SHA256, "item-hash-1"), Instant.now(), "key-1");
        Entry entryDangling = new Entry(106, new HashValue(HashingAlgorithm.SHA256, "item-hash-2"), Instant.now(), "key-2");

        RegisterMetadata registerMetadata = mock(RegisterMetadata.class);
        when(registerMetadata.getRegisterName()).thenReturn("country");

        PostgresRegister register = new PostgresRegister(() -> "country", backingStoreDriver, registerFieldsConfiguration, inMemoryEntryLog(entryQueryDAO), inMemoryItemStore(itemValidator, entryQueryDAO));

        try {
            register.putItem(item);
            register.appendEntry(entryNotDangling);
            register.appendEntry(entryDangling);
        } catch (NoSuchItemForEntryException ex) {
        }

        verify(backingStoreDriver, times(1)).insertRecord(any(), anyString());

        ArgumentCaptor<Record> argumentCaptor = ArgumentCaptor.forClass(Record.class);
        verify(backingStoreDriver, times(1)).insertRecord(argumentCaptor.capture(), eq("country"));
        assertThat(argumentCaptor.getValue().entry, is(entryNotDangling));
        assertThat(argumentCaptor.getValue().item, is(item));
    }
}
