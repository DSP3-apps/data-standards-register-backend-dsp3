package uk.gov.register.views;

import uk.gov.organisation.client.GovukOrganisation;
import uk.gov.register.configuration.RegisterTrackingConfiguration;
import uk.gov.register.core.PublicBody;
import uk.gov.register.core.RegisterData;
import uk.gov.register.core.RegisterResolver;
import uk.gov.register.resources.RequestContext;
import uk.gov.register.views.representations.CsvRepresentation;

import java.util.Optional;

public abstract class CsvRepresentationView<T> extends AttributionView {

    public CsvRepresentationView(RequestContext requestContext, PublicBody custodian, Optional<GovukOrganisation.Details> custodianBranding, String templateName, RegisterData registerData, RegisterTrackingConfiguration registerTrackingConfiguration, RegisterResolver registerResolver) {
        super(requestContext, custodian, custodianBranding, templateName, registerData, registerTrackingConfiguration, registerResolver);
    }

    public abstract CsvRepresentation<T> csvRepresentation();
}
