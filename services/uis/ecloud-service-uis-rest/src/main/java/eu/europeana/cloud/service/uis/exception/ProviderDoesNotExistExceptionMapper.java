package eu.europeana.cloud.service.uis.exception;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ProviderDoesNotExistExceptionMapper extends UISExceptionMapper implements
		ExceptionMapper<ProviderDoesNotExistException> {

}
