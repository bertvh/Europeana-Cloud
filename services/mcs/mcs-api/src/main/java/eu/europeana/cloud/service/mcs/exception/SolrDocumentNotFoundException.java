package eu.europeana.cloud.service.mcs.exception;

public class SolrDocumentNotFoundException
	extends Exception
{

	public SolrDocumentNotFoundException(String query)
	{
		super(String.format("Solr document not found for query %s", query));
	}
}
