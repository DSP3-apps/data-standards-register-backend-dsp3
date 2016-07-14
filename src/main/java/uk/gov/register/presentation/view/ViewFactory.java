package uk.gov.register.presentation.view;

import org.jvnet.hk2.annotations.Service;
import uk.gov.organisation.client.GovukOrganisation;
import uk.gov.organisation.client.GovukOrganisationClient;
import uk.gov.register.presentation.ItemConverter;
import uk.gov.register.presentation.RegisterData;
import uk.gov.register.configuration.PublicBodiesConfiguration;
import uk.gov.register.configuration.PublicBody;
import uk.gov.register.configuration.RegisterDomainConfiguration;
import uk.gov.register.presentation.dao.Entry;
import uk.gov.register.presentation.dao.Item;
import uk.gov.register.presentation.dao.Record;
import uk.gov.register.presentation.resource.IPagination;
import uk.gov.register.presentation.resource.Pagination;
import uk.gov.register.presentation.resource.RequestContext;
import uk.gov.register.thymeleaf.ThymeleafView;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class ViewFactory {
    private final RequestContext requestContext;
    private final ItemConverter itemConverter;
    private final PublicBodiesConfiguration publicBodiesConfiguration;
    private final GovukOrganisationClient organisationClient;
    private final String registerDomain;
    private final RegisterData registerData;
    private final RegisterDomainConfiguration registerDomainConfiguration;

    @Inject
    public ViewFactory(RequestContext requestContext,
                       ItemConverter itemConverter,
                       PublicBodiesConfiguration publicBodiesConfiguration,
                       GovukOrganisationClient organisationClient,
                       RegisterDomainConfiguration domainConfiguration,
                       RegisterData registerData) {
        this.requestContext = requestContext;
        this.itemConverter = itemConverter;
        this.publicBodiesConfiguration = publicBodiesConfiguration;
        this.organisationClient = organisationClient;
        this.registerDomain = domainConfiguration.getRegisterDomain();
        this.registerDomainConfiguration = domainConfiguration;
        this.registerData = registerData;
    }

    public ThymeleafView thymeleafView(String templateName) {
        return new ThymeleafView(requestContext, templateName, registerData, registerDomainConfiguration);
    }

    public BadRequestExceptionView badRequestExceptionView(BadRequestException e) {
        return new BadRequestExceptionView(requestContext, e, registerDomainConfiguration, registerData);
    }

    public HomePageView homePageView(int totalRecords, int totalEntries, Instant lastUpdated) {
        return new HomePageView(getCustodian(), getBranding(), requestContext, totalRecords, totalEntries, lastUpdated, registerDomainConfiguration, registerData);
    }

    public RegisterDetailView registerDetailView(int totalRecords, int totalEntries, int totalItems, Instant lastUpdated) {
        return new RegisterDetailView(totalRecords, totalEntries, totalItems, lastUpdated, registerData, registerDomain);
    }

    public ItemView getItemView(Item item) {
        return new ItemView(requestContext, getCustodian(), getBranding(), itemConverter, item, registerDomainConfiguration, registerData);
    }

    public EntryView getEntryView(Entry entry) {
        return new EntryView(requestContext, getCustodian(), getBranding(), entry, registerDomainConfiguration, registerData);
    }

    public EntryListView getEntriesView(Collection<Entry> entries, IPagination pagination) {
        return new EntryListView(requestContext, pagination, getCustodian(), getBranding(), entries, registerDomainConfiguration, registerData);
    }

    public EntryListView getRecordEntriesView(String recordKey, Collection<Entry> entries, IPagination pagination) {
        return new EntryListView(requestContext, pagination, getCustodian(), getBranding(), entries, recordKey, registerDomainConfiguration, registerData);
    }

    public RecordView getRecordView(Record record) {
        return new RecordView(requestContext, getCustodian(), getBranding(), itemConverter, record, registerDomainConfiguration, registerData);
    }

    public RecordListView getRecordListView(List<Record> records, Pagination pagination) {
        return new RecordListView(requestContext, getCustodian(), getBranding(), pagination, itemConverter, records, registerDomainConfiguration, registerData);
    }

    private PublicBody getCustodian() {
        return publicBodiesConfiguration.getPublicBody(registerData.getRegister().getRegistry());
    }

    private Optional<GovukOrganisation.Details> getBranding() {
        Optional<GovukOrganisation> organisation = organisationClient.getOrganisation(registerData.getRegister().getRegistry());
        return organisation.map(GovukOrganisation::getDetails);
    }
}
