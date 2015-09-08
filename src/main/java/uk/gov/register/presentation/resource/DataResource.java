package uk.gov.register.presentation.resource;

import io.dropwizard.views.View;
import uk.gov.register.presentation.dao.RecentEntryIndexQueryDAO;
import uk.gov.register.presentation.representations.ExtraMediaType;
import uk.gov.register.thymeleaf.ThymeleafView;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

@Path("/")
public class DataResource extends ResourceBase {
    private final RecentEntryIndexQueryDAO queryDAO;

    @Inject
    public DataResource(RecentEntryIndexQueryDAO queryDAO) {
        this.queryDAO = queryDAO;
    }

    @GET
    @Path("/download")
    @Produces(MediaType.TEXT_HTML)
    public View download() {
        return new ThymeleafView(httpServletRequest, httpServletResponse, servletContext, "download.html");
    }

    @GET
    @Path("/download.torrent")
    @Produces(MediaType.TEXT_HTML)
    public Response downloadTorrent() {
        return Response
                .status(Response.Status.NOT_IMPLEMENTED)
                .entity(new ThymeleafView(httpServletRequest, httpServletResponse, servletContext, "not-implemented.html"))
                .build();
    }

    @GET
    @Path("/feed")
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, ExtraMediaType.TEXT_CSV, ExtraMediaType.TEXT_TSV, ExtraMediaType.TEXT_TTL})
    public ListResultView feed() {
        return new ListResultView("entries.html", queryDAO.getFeeds(ENTRY_LIMIT));
    }

    @GET
    @Path("/current")
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, ExtraMediaType.TEXT_CSV, ExtraMediaType.TEXT_TSV, ExtraMediaType.TEXT_TTL})
    public ListResultView current() {
        return new ListResultView("entries.html", queryDAO.getAllRecords(getRegisterPrimaryKey(), ENTRY_LIMIT));
    }

    @GET
    @Path("/all")
    public Response all() {
        return create301Response("current");
    }

    @GET
    @Path("/latest")
    public Response latest() {
        return create301Response("feed");
    }

    private Response create301Response(String locationMethod) {
        String requestURI = httpServletRequest.getRequestURI();
        String representation = requestURI.substring(requestURI.lastIndexOf("/")).replaceAll("[^\\.]+(.*)", "$1");

        URI uri = UriBuilder
                .fromUri(requestURI)
                .replacePath(null)
                .path(locationMethod + representation)
                .build();

        return Response
                .status(Response.Status.MOVED_PERMANENTLY)
                .location(uri)
                .build();
    }

}
