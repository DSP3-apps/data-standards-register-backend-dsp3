package uk.gov.register.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.register.configuration.IndexFunctionConfiguration.IndexNames;
import uk.gov.register.db.Index;
import uk.gov.register.exceptions.AppendEntryException;
import uk.gov.register.exceptions.FieldDefinitionException;
import uk.gov.register.exceptions.NoSuchFieldException;
import uk.gov.register.exceptions.IndexingException;
import uk.gov.register.exceptions.BlobValidationException;
import uk.gov.register.exceptions.NoSuchBlobException;
import uk.gov.register.exceptions.RegisterDefinitionException;
import uk.gov.register.exceptions.NoSuchRegisterException;
import uk.gov.register.indexer.function.IndexFunction;
import uk.gov.register.service.EnvironmentValidator;
import uk.gov.register.service.BlobValidator;
import uk.gov.register.util.HashValue;
import uk.gov.register.views.ConsistencyProof;
import uk.gov.register.views.EntryProof;
import uk.gov.register.views.RegisterProof;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class PostgresRegister implements Register {
    private static ObjectMapper mapper = new ObjectMapper();
    private final Index index;
    private final RegisterId registerId;
    private final EntryLog entryLog;
    private final BlobStore blobStore;
    private final Map<EntryType,Collection<IndexFunction>> indexFunctionsByEntryType;
    private final BlobValidator blobValidator;
    private final EnvironmentValidator environmentValidator;

    private RegisterMetadata registerMetadata;
    private Map<String, Field> fieldsByName;

    private final String defaultIndexForTypeUser = IndexNames.RECORD;
    private final String defaultIndexForTypeSystem = IndexNames.METADATA;

    public PostgresRegister(RegisterId registerId,
                            EntryLog entryLog,
                            BlobStore blobStore,
                            Index index,
                            Map<EntryType,Collection<IndexFunction>> indexFunctionsByEntryType,
                            BlobValidator blobValidator,
                            EnvironmentValidator environmentValidator) {
        this.registerId = registerId;
        this.entryLog = entryLog;
        this.blobStore = blobStore;
        this.index = index;
        this.indexFunctionsByEntryType = indexFunctionsByEntryType;
        this.blobValidator = blobValidator;
        this.environmentValidator = environmentValidator;
    }

    //region Blobs

    @Override
    public void addBlob(Blob blob) {
        blobStore.addBlob(blob);
    }

    @Override
    public Optional<Blob> getBlob(HashValue hash) {
        return blobStore.getBlob(hash);
    }

    @Override
    public Collection<Blob> getAllBlobs() {
        return blobStore.getAllBlobs();
    }

    @Override
    public Iterator<Blob> getBlobIterator() {
        return blobStore.getUserBlobIterator();
    }

    @Override
    public Iterator<Blob> getBlobIterator(int start, int end) {
        return blobStore.getUserBlobIterator(start, end);
    }

    @Override
    public Iterator<Blob> getSystemBlobIterator() {
        return blobStore.getSystemBlobIterator();
    }

    //endregion

    //region Entries

    @Override
    public void appendEntry(final Entry entry) throws AppendEntryException {
        try {
            List<Blob> referencedBlobs = getReferencedBlobs(entry);

            referencedBlobs.forEach(i -> {
                if (entry.getEntryType() == EntryType.user) {
                    blobValidator.validateBlob(i.getContent(), this.getFieldsByName(), this.getRegisterMetadata());
                } else if (entry.getKey().startsWith("field:")) {
                    Field field = extractObjectFromBlob(i, Field.class);
                    environmentValidator.validateFieldAgainstEnvironment(field);
                } else if (entry.getKey().startsWith("register:")) {
                    RegisterMetadata localRegisterMetadata = this.extractObjectFromBlob(i, RegisterMetadata.class);
                    // will throw exception if field not present
                    localRegisterMetadata.getFields().forEach(this::getField);

                    environmentValidator.validateRegisterAgainstEnvironment(localRegisterMetadata);
                }
            });

            entryLog.appendEntry(entry);
        } catch (IndexingException | BlobValidationException | FieldDefinitionException | RegisterDefinitionException |
                NoSuchRegisterException | NoSuchFieldException | NoSuchBlobException exception) {
            throw new AppendEntryException(entry, exception);
        }
    }

    @Override
    public Optional<Entry> getEntry(int entryNumber) {
        return entryLog.getEntry(entryNumber);
    }

    @Override
    public Collection<Entry> getEntries(int start, int limit) {
        return entryLog.getEntries(start, limit);
    }

    @Override
    public Collection<Entry> getAllEntries() {
        return entryLog.getAllEntries();
    }

    @Override
    public int getTotalEntries() {
        return entryLog.getTotalEntries();
    }

    @Override
    public int getTotalEntries(EntryType entryType) {
        return entryLog.getTotalEntries(entryType);
    }

    @Override
    public Collection<Entry> allEntriesOfRecord(String key) {
        return index.findAllEntriesOfRecordBy(key);
    }

    @Override
    public Optional<Instant> getLastUpdatedTime() {
        return entryLog.getLastUpdatedTime();
    }

    @Override
    public Iterator<Entry> getEntryIterator() {
        return entryLog.getEntryIterator(defaultIndexForTypeUser);
    }

    @Override
    public Iterator<Entry> getEntryIterator(int totalEntries1, int totalEntries2) {
        return entryLog.getEntryIterator(defaultIndexForTypeUser, totalEntries1, totalEntries2);
    }

    @Override
    public Iterator<Entry> getEntryIterator(String indexName) {
        return entryLog.getEntryIterator(indexName);
    }

    @Override
    public Iterator<Entry> getEntryIterator(String indexName, int totalEntries1, int totalEntries2) {
        return entryLog.getEntryIterator(indexName, totalEntries1, totalEntries2);
    }

    //endregion

    //region Indexes

    @Override
    public Optional<Record> getRecord(String key) {
        return index.getRecord(key, defaultIndexForTypeUser);
    }

    @Override
    public List<Record> getRecords(int limit, int offset) {
        return index.getRecords(limit, offset, defaultIndexForTypeUser);
    }

    @Override
    public int getTotalRecords() {
        return getTotalRecords(defaultIndexForTypeUser);
    }

    @Override
    public List<Record> max100RecordsFacetedByKeyValue(String key, String value) throws NoSuchFieldException {
        if (!getRegisterMetadata().getFields().contains(key)) {
            throw new NoSuchFieldException(registerId, key);
        }

        return index.findMax100RecordsByKeyValue(key, value);
    }

    @Override
    public Optional<Record> getRecord(String key, String indexName) {
        return index.getRecord(key, indexName);
    }

    @Override
    public List<Record> getRecords(int limit, int offset, String indexName) {
        return index.getRecords(limit, offset, indexName);
    }

    @Override
    public int getTotalRecords(String indexName) {
        return index.getTotalRecords(indexName);
    }

    //endregion

    //region Verifiable Log

    @Override
    public RegisterProof getRegisterProof() {
        return entryLog.getRegisterProof();
    }

    @Override
    public RegisterProof getRegisterProof(int totalEntries) {
        return entryLog.getRegisterProof(totalEntries);
    }

    @Override
    public EntryProof getEntryProof(int entryNumber, int totalEntries) {
        return entryLog.getEntryProof(entryNumber, totalEntries);
    }

    @Override
    public ConsistencyProof getConsistencyProof(int totalEntries1, int totalEntries2) {
        return entryLog.getConsistencyProof(totalEntries1, totalEntries2);
    }

    //endregion

    //region Metadata

    @Override
    public RegisterId getRegisterId() {
        return registerId;
    }

    @Override
    public Optional<String> getRegisterName() {
        return getMetadataField("register-name");
    }

    @Override
    public Optional<String> getCustodianName() {
        return getMetadataField("custodian");
    }

    @Override
    public RegisterMetadata getRegisterMetadata() throws NoSuchRegisterException {
        if (registerMetadata == null) {
            registerMetadata = getRecord("register:" + registerId.value(), defaultIndexForTypeSystem)
                    .map(r -> extractObjectFromRecord(r, RegisterMetadata.class))
                    .orElseThrow(() -> new NoSuchRegisterException(registerId));
        }

        return registerMetadata;
    }

    @Override
    public Map<String, Field> getFieldsByName() throws NoSuchRegisterException, NoSuchFieldException {
        if (fieldsByName == null) {
            RegisterMetadata registerMetadata = getRegisterMetadata();
            List<String> fieldNames = registerMetadata.getFields();
            fieldsByName = new LinkedHashMap<>();
            fieldNames.forEach(fieldName -> fieldsByName.put(fieldName, getField(fieldName)));
        }
        return fieldsByName;
    }

    //endregion

    private Field getField(String fieldName) throws NoSuchFieldException {
        return getRecord("field:" + fieldName, defaultIndexForTypeSystem)
                .map(record -> extractObjectFromRecord(record, Field.class))
                .orElseThrow(() -> new NoSuchFieldException(registerId, fieldName));
    }

    private <T> T extractObjectFromRecord(Record record, Class<T> clazz) {
        return extractObjectFromBlob(record.getBlobs().get(0), clazz);
    }

    private <T> T extractObjectFromBlob(Blob blob, Class<T> clazz) {
        try {
            JsonNode content = blob.getContent();
            return mapper.treeToValue(content, clazz);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Optional<String> getMetadataField(String fieldName) {
        return getRecord(fieldName, defaultIndexForTypeSystem).map(r -> r.getBlobs().get(0).getValue(fieldName).get());
    }

    public Map<EntryType, Collection<IndexFunction>> getIndexFunctionsByEntryType() {
        return indexFunctionsByEntryType;
    }

    private List<Blob> getReferencedBlobs(Entry entry) throws NoSuchBlobException {
        return entry.getBlobHashes().stream()
                .map(h -> blobStore.getBlob(h).orElseThrow(
                        () -> new NoSuchBlobException(h)))
                .collect(Collectors.toList());
    }
}
