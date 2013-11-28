package eu.europeana.cloud.service.mcs.persistent;

import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.service.mcs.exception.SolrDocumentNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import org.apache.solr.common.SolrException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(value = { "classpath:/solrTestContext.xml" })
public class SolrDAOTest
{

	@Autowired
	SolrDAO solrDAO;


	@Test
	public void shouldInsertAndReturnRepresentation()
		throws Exception
	{
		Representation rep = new Representation("cloud1", "schema1", "version1", null, null, "dataProvider", null,
				true, new Date());
		ArrayList<String> dataSets = new ArrayList();
		dataSets.add("dataSet1");
		dataSets.add("dataSet2");
		solrDAO.insertRepresentation(rep, dataSets);
		RepresentationSolrDocument doc = solrDAO.getDocumentById(rep.getVersion());
		assertEquals(rep.getVersion(), doc.getVersion());
		assertEquals(rep.getSchema(), doc.getSchema());
		assertEquals(rep.getDataProvider(), doc.getProviderId());
		assertEquals(rep.getCreationDate(), doc.getCreationDate());
		assertTrue(doc.getDataSets().containsAll(dataSets));
	}


	@Test(expected = SolrException.class)
	public void shouldThrowExceptionWhenRequiredFieldMissing()
		throws Exception
	{
		Representation rep = new Representation(null, "schema", "version", null, null, "dataProvider", null, true,
				new Date());
		solrDAO.insertRepresentation(rep, null);
	}


	@Test
	public void shouldAddAssignment()
		throws Exception
	{
		Representation rep = new Representation("cloud2", "schema1", "version2", null, null, "dataProvider", null,
				true, new Date());
		ArrayList<String> dataSets = new ArrayList();
		dataSets.add("dataSet1");
		dataSets.add("dataSet2");

		//insert representation with 2 datasets
		solrDAO.insertRepresentation(rep, dataSets);
		RepresentationSolrDocument doc = solrDAO.getDocumentById(rep.getVersion());
		assertTrue(doc.getDataSets().containsAll(dataSets));

		//add assigment to representation
		String dataSet = "dataSet3";
		solrDAO.addAssignment(rep.getVersion(), dataSet);
		RepresentationSolrDocument updatedDoc = solrDAO.getDocumentById(rep.getVersion());
		assertTrue(updatedDoc.getDataSets().containsAll(dataSets));
		assertTrue(updatedDoc.getDataSets().contains(dataSet));
	}


	@Test
	public void shouldRemoveRepresentation()
		throws Exception
	{
		Representation rep = new Representation("cloud1", "schema1", "version4", null, null, "dataProvider", null,
				true, new Date());
		solrDAO.insertRepresentation(rep, null);
		//check if doc got inserted
		RepresentationSolrDocument doc = solrDAO.getDocumentById(rep.getVersion());
		assertEquals(rep.getVersion(), doc.getVersion());

		solrDAO.removeRepresentation(rep.getVersion());

		//try to access removed doc
		SolrDocumentNotFoundException ex = null;
		try {
			doc = solrDAO.getDocumentById(rep.getVersion());
		}
		catch (SolrDocumentNotFoundException e) {
			ex = e;
		}
		assertNotNull(ex);
	}


	@Test
	public void shouldRemoveAssignment()
		throws Exception
	{
		String dataSet1 = "dataSet1";
		String dataSet2 = "dataSet2";
		String dataSet3 = "dataSet3";

		Representation rep = new Representation("cloud2", "schema1", "version123", null, null, "dataProvider", null,
				true, new Date());
		ArrayList<String> dataSets = new ArrayList();
		dataSets.add(dataSet1);
		dataSets.add(dataSet2);
		dataSets.add(dataSet3);

		//insert representation with 2 datasets
		solrDAO.insertRepresentation(rep, dataSets);
		RepresentationSolrDocument doc = solrDAO.getDocumentById(rep.getVersion());
		assertTrue(doc.getDataSets().containsAll(dataSets));

		//add assigment to representation

		solrDAO.removeAssignment(rep.getVersion(), dataSet2);
		RepresentationSolrDocument updatedDoc = solrDAO.getDocumentById(rep.getVersion());
		assertTrue(updatedDoc.getDataSets().contains(dataSet1));
		assertTrue(updatedDoc.getDataSets().contains(dataSet3));
		assertFalse(updatedDoc.getDataSets().contains(dataSet2));
		assertEquals(updatedDoc.getDataSets().size(), 2);
	}

}