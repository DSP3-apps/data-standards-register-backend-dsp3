package uk.gov.register.thymeleaf;

import org.markdownj.MarkdownProcessor;
import uk.gov.register.presentation.config.PublicBodiesConfiguration;
import uk.gov.register.presentation.config.PublicBody;
import uk.gov.register.presentation.resource.RequestContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HomePageView extends ThymeleafView {
    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:ss");

    private final PublicBodiesConfiguration publicBodiesConfiguration;
    private final MarkdownProcessor markdownProcessor = new MarkdownProcessor();

    private final LocalDateTime lastUpdated;
    private final int totalRecords;

    public HomePageView(
            PublicBodiesConfiguration publicBodiesConfiguration,
            RequestContext requestContext,
            int totalRecords,
            LocalDateTime lastUpdated
    ) {
        super(requestContext, "home.html");
        this.publicBodiesConfiguration = publicBodiesConfiguration;
        this.totalRecords = totalRecords;
        this.lastUpdated = lastUpdated;
    }

    @SuppressWarnings("unused, used from template")
    public PublicBody getPublicBody() {
        return publicBodiesConfiguration.getPublicBody(getRegister().getRegistry());
    }

    @SuppressWarnings("unused, used from template")
    public String getRegisterText() {
        return markdownProcessor.markdown(getRegister().getText());
    }

    @SuppressWarnings("unused, used from template")
    public int getTotalRecords(){
        return totalRecords;
    }

    @SuppressWarnings("unused, used from template")
    public String getLastUpdatedTime(){
        return DATE_TIME_FORMATTER.format(lastUpdated);
    }
}
