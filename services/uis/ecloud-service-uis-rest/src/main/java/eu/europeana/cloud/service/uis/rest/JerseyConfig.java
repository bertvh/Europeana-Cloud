package eu.europeana.cloud.service.uis.rest;

import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;

import eu.europeana.cloud.service.uis.exception.CloudIdDoesNotExistExceptionMapper;
import eu.europeana.cloud.service.uis.exception.DatabaseConnectionExceptionMapper;
import eu.europeana.cloud.service.uis.exception.IdHasBeenMappedExceptionMapper;
import eu.europeana.cloud.service.uis.exception.ProviderDoesNotExistExceptionMapper;
import eu.europeana.cloud.service.uis.exception.RecordDatasetEmptyExceptionMapper;
import eu.europeana.cloud.service.uis.exception.RecordDoesNotExistExceptionMapper;
import eu.europeana.cloud.service.uis.exception.RecordExistsExceptionMapper;
import eu.europeana.cloud.service.uis.exception.RecordIdDoesNotExistExceptionMapper;

public class JerseyConfig extends ResourceConfig {

	public JerseyConfig(){
		super();
        register(RequestContextFilter.class);
        register(LoggingFilter.class);
		register(CloudIdDoesNotExistExceptionMapper.class);
		register(DatabaseConnectionExceptionMapper.class);
		register(IdHasBeenMappedExceptionMapper.class);
		register(ProviderDoesNotExistExceptionMapper.class);
		register(RecordDatasetEmptyExceptionMapper.class);
		register(RecordDoesNotExistExceptionMapper.class);
		register(RecordExistsExceptionMapper.class);
		register(RecordIdDoesNotExistExceptionMapper.class);
		register(BasicUniqueIdResource.class);
		
	}
}
