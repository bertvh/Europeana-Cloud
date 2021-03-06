package eu.europeana.cloud.service.mcs.rest;

import eu.europeana.cloud.common.model.DataSet;
import eu.europeana.cloud.common.response.ResultSlice;
import eu.europeana.cloud.service.mcs.DataSetService;
import eu.europeana.cloud.service.mcs.exception.DataSetAlreadyExistsException;
import eu.europeana.cloud.service.mcs.exception.ProviderNotExistsException;
import static eu.europeana.cloud.service.mcs.rest.ParamConstants.F_DATASET;
import static eu.europeana.cloud.service.mcs.rest.ParamConstants.F_DESCRIPTION;
import static eu.europeana.cloud.service.mcs.rest.ParamConstants.F_START_FROM;
import static eu.europeana.cloud.service.mcs.rest.ParamConstants.P_PROVIDER;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resource to get and create data set.
 */
@Path("/data-providers/{" + P_PROVIDER + "}/data-sets")
@Component
public class DataSetsResource {

    private static final Logger log = LoggerFactory.getLogger(DataSetsResource.class);

    @Autowired
    private DataSetService dataSetService;

    @Context
    private UriInfo uriInfo;

    @PathParam(P_PROVIDER)
    private String providerId;

    @Value("${numberOfElementsOnPage}")
    private int numberOfElementsOnPage;


    /**
     * Returns all data sets for a provider. Result is returned in slices.
     * 
     * @param startFrom
     *            reference to next slice of result. If not provided, first slice of result will be returned.
     * @return slice of data sets for given provider.
     * @throws ProviderNotExistsException
     *             data provider does not exist.
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public ResultSlice<DataSet> getDataSets(@QueryParam(F_START_FROM) String startFrom)
            throws ProviderNotExistsException {
        return dataSetService.getDataSets(providerId, startFrom, numberOfElementsOnPage);
    }


    /**
     * Creates new data set. *
     * 
     * @param dataSetId
     *            identifier of data set (required).
     * @param description
     *            description of data set.
     * @return URI to newly created data set in content-location.
     * @throws ProviderNotExistsException
     *             data provider * does not exist.
     * @throws eu.europeana.cloud.service.mcs.exception.DataSetAlreadyExistsException
     *             data set with this id already exists
     * @statuscode 204 object has been updated.
     */
    @POST
    public Response createDataSet(@FormParam(F_DATASET) String dataSetId, @FormParam(F_DESCRIPTION) String description)
            throws ProviderNotExistsException, DataSetAlreadyExistsException {
        ParamUtil.require(F_DATASET, dataSetId);

        DataSet dataSet = dataSetService.createDataSet(providerId, dataSetId, description);
        EnrichUriUtil.enrich(uriInfo, dataSet);
        return Response.created(dataSet.getUri()).build();
    }
}
