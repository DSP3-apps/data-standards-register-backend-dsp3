package uk.gov.register.resources.v2;

import com.codahale.metrics.annotation.Timed;
import uk.gov.register.core.EntryType;
import uk.gov.register.core.Field;
import uk.gov.register.core.HashingAlgorithm;
import uk.gov.register.core.Item;
import uk.gov.register.core.RegisterReadOnly;
import uk.gov.register.exceptions.FieldConversionException;
import uk.gov.register.service.ItemConverter;
import uk.gov.register.util.HashValue;
import uk.gov.register.views.v2.BlobListView;
import uk.gov.register.views.ItemView;
import uk.gov.register.views.ViewFactory;
import uk.gov.register.views.representations.ExtraMediaType;
import uk.gov.register.views.v2.BlobView;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/next/blobs")
public class BlobResource {
    protected final RegisterReadOnly register;
    protected final ViewFactory viewFactory;
    protected final ItemConverter itemConverter;

    @Inject
    public BlobResource(RegisterReadOnly register, ViewFactory viewFactory, ItemConverter itemConverter) {
        this.register = register;
        this.viewFactory = viewFactory;
        this.itemConverter = itemConverter;
    }

    @GET
    @Path("/sha-256:{blob-hash}")
    @Produces({
            MediaType.APPLICATION_JSON,
            ExtraMediaType.TEXT_CSV,
    })
    @Timed
    public BlobView getBlobDataByHex(@PathParam("blob-hash") String blobHash) throws FieldConversionException {
        return getBlob(blobHash).map(blob -> buildBlobView(blob))
                .orElseThrow(() -> new NotFoundException("No blob found with blob hash: " + blobHash));
    }

    @GET
    @Path("/")
    @Produces({
            MediaType.APPLICATION_JSON,
            ExtraMediaType.TEXT_CSV,
    })
    @Timed
    public BlobListView listBlobs() throws FieldConversionException {
        Collection<Item> items = register.getAllItems(EntryType.user);

        // TODO: allow this resource to be paginated
        return buildBlobListView(items.stream().limit(100).collect(Collectors.toList()));
    }

    protected Optional<Item> getBlob(String blobHash) {
        HashValue hash = new HashValue(HashingAlgorithm.SHA256, blobHash);
        return register.getItem(hash);
    }

    protected Map<String, Field> getFieldsByName() {
        return register.getFieldsByName();
    }

    protected BlobView buildBlobView(Item item) {
        return new BlobView(item, register.getFieldsByName(), this.itemConverter);
    }

    protected BlobListView buildBlobListView(Collection<Item> items) {
        return new BlobListView(items, getFieldsByName());
    }
}
