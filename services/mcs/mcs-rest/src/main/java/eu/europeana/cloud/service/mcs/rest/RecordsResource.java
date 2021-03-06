package eu.europeana.cloud.service.mcs.rest;

import eu.europeana.cloud.common.model.Record;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.service.mcs.RecordService;
import eu.europeana.cloud.service.mcs.exception.RecordNotExistsException;
import static eu.europeana.cloud.service.mcs.rest.ParamConstants.P_GID;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Resource representing records.
 */
@Path("/records/{" + P_GID + "}")
@Component
public class RecordsResource {

    private static final Logger log = LoggerFactory.getLogger(RecordsResource.class);

    @Autowired
    private RecordService recordService;

    @Context
    private UriInfo uriInfo;

    @PathParam(P_GID)
    private String globalId;


    /**
     * Returns record with all its latest persistent representations.
     * 
     * @return record.
     * @throws RecordNotExistsException
     *             provided id is not known to Unique Identifier Service.
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Record getRecord()
            throws RecordNotExistsException {
        Record record = recordService.getRecord(globalId);
        prepare(record);
        return record;
    }


    /**
     * Deletes record with all its representations in all versions. Does not remove mapping from Unique Identifier
     * Service.
     * 
     * @throws RecordNotExistsException
     *             provided id is not known to Unique Identifier Service.
     */
    @DELETE
    public void deleteRecord()
            throws RecordNotExistsException {
        recordService.deleteRecord(globalId);
    }


    /**
     * Removes unimportant (at this point) information from record to reduce response size.
     * 
     * @param record
     */
    private void prepare(Record record) {
        EnrichUriUtil.enrich(uriInfo, record);
        for (Representation representation : record.getRepresentations()) {
            representation.setFiles(null);
            representation.setRecordId(null);
        }
    }
}
