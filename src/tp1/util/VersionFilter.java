package tp1.util;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import tp1.api.service.rest.RestSpreadsheetsReplicated;
import tp1.kafka.sync.SyncPoint;

import java.io.IOException;

@Provider
public class VersionFilter implements ContainerResponseFilter {

    public VersionFilter() {
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        response.getHeaders().add(RestSpreadsheetsReplicated.HEADER_VERSION, SyncPoint.getVersion());
    }
}
