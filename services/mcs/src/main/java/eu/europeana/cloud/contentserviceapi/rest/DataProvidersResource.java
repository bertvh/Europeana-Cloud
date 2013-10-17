package eu.europeana.cloud.contentserviceapi.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.europeana.cloud.contentserviceapi.service.DataProviderService;
import eu.europeana.cloud.definitions.model.DataProvider;

/**
 * Resource for DataProviders
 *
 */
@Path("/data-providers")
@Component
public class DataProvidersResource {

    @Autowired
    DataProviderService providerService;
    
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public List<DataProvider> getProviders() {
        return providerService.getProviders();
    } 
}