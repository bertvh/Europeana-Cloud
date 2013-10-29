package eu.europeana.cloud.service.mcs.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import eu.europeana.cloud.common.model.File;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.service.mcs.ApplicationContextUtils;
import eu.europeana.cloud.service.mcs.exception.FileNotExistsException;
import eu.europeana.cloud.service.mcs.ContentService;
import eu.europeana.cloud.service.mcs.RecordService;
import eu.europeana.cloud.test.ChunkedHttpUrlConnector;

/**
 * FileResourceTest
 */
public class HugeFileResourceUploadIT extends JerseyTest {

    private static RecordService recordService;

    private static ContentService contentService;

    private Representation recordRepresentation;

    private static final int HUGE_FILE_SIZE = 1 << 30;


    @Before
    public void mockUp() {
        ApplicationContext applicationContext = ApplicationContextUtils.getApplicationContext();
        recordService = applicationContext.getBean(RecordService.class);
        contentService = applicationContext.getBean(ContentService.class);

        recordRepresentation = recordService.createRepresentation("1", "1", "1");
    }


    @After
    public void cleanUp() {
        reset(recordService);
        recordService.deleteRepresentation(recordRepresentation.getRecordId(), recordRepresentation.getSchema());
    }


    @Override
    public Application configure() {
        return new JerseyConfig().property("contextConfigLocation", "classpath:spiedServicesTestContext.xml");
    }


    @Override
    protected void configureClient(ClientConfig config) {
        config.register(MultiPartFeature.class);
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 1024);
        config.connector(new ChunkedHttpUrlConnector(config));
    }


    @Test
    public void testUploadingHugeFile()
            throws IOException, NoSuchAlgorithmException {

        MockPutContentMethod mockPutContent = new MockPutContentMethod();
        doAnswer(mockPutContent).when(contentService).putContent(any(Representation.class), any(File.class), any(InputStream.class));
        WebTarget webTarget = target("/records/{ID}/representations/{REPRESENTATION}/versions/{VERSION}/files/{FILE}")
                .resolveTemplates(ImmutableMap.<String, Object>of(
                ParamConstants.P_GID, recordRepresentation.getRecordId(),
                ParamConstants.P_REP, recordRepresentation.getSchema(),
                ParamConstants.P_VER, recordRepresentation.getVersion(),
                ParamConstants.P_FILE, "terefere"));

        MessageDigest md = MessageDigest.getInstance("MD5");
        DigestInputStream inputStream = new DigestInputStream(new DummyStream(HUGE_FILE_SIZE), md);

        FormDataMultiPart multipart = new FormDataMultiPart()
                .field(ParamConstants.F_FILE_MIME, MediaType.APPLICATION_OCTET_STREAM)
                .field(ParamConstants.F_FILE_DATA, inputStream, MediaType.APPLICATION_OCTET_STREAM_TYPE);

        Response response = webTarget.request().put(Entity.entity(multipart, multipart.getMediaType()));
        assertEquals("Unsuccessful request", Response.Status.Family.SUCCESSFUL, response.getStatusInfo().getFamily());
        assertEquals("Wrong size of read content", HUGE_FILE_SIZE, mockPutContent.totalBytes);

        String contentMd5Hex = BaseEncoding.base16().lowerCase().encode(md.digest());
        assertEquals("Content hash mismatch", contentMd5Hex, response.getEntityTag().getValue());
    }

    /**
     * Mock answer for {@link ContentService#putContent(eu.europeana.cloud.common.model.Representation, 
     * eu.europeana.cloud.common.model.File, java.io.InputStream) putContent} method. Only counts bytes in input stream.     
     */
    static class MockPutContentMethod implements Answer<Object> {

        int totalBytes;


        @Override
        public Object answer(InvocationOnMock invocation)
                throws Throwable {
            Object[] args = invocation.getArguments();
            File file = (File) args[1];
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream inputStream = new DigestInputStream((InputStream) args[2], md);
            consume(inputStream);
            file.setMd5(BaseEncoding.base16().lowerCase().encode(md.digest()));
            return null;
        }


        private void consume(InputStream is)
                throws IOException {

            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                totalBytes += nRead;
            }
        }
    }

    /**
     * Input stream that generates unspecified bytes. The total length of stream is specified in constuctor.
     */
    static class DummyStream extends InputStream {

        /**
         * Total number of bytes that will be produced in this stream.
         */
        private final int totalLength;

        /**
         * Number of bytes that have already been read from this stream.
         */
        private int readLength = 0;


        public DummyStream(int totalLength) {
            this.totalLength = totalLength;
        }


        @Override
        public int read()
                throws IOException {
            if (readLength >= totalLength) {
                return -1;
            } else {
                readLength++;
                return 1;
            }
        }
    }
}