package eu.europeana.cloud.service.mcs.service.inmemory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import eu.europeana.cloud.common.model.File;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.service.mcs.exception.FileNotExistsException;
import eu.europeana.cloud.service.mcs.service.ContentService;

/**
 * InMemoryContentService
 */
@Service
public class InMemoryContentService implements ContentService {

    private Map<String, byte[]> content = new HashMap<>();

    @Override
    public void insertContent(Representation rep, File file, InputStream data) throws IOException {
        if (file.getFileName() == null || file.getFileName().isEmpty()) {
            file.setFileName(UUID.randomUUID().toString());
            rep.getFiles().add(file);
        } else {
            boolean alreadyExists = false;
            for (File f : rep.getFiles()) {
                if (f.getFileName().equals(file.getFileName())) {
                    alreadyExists = true;
                    break;
                }
            }
            if (!alreadyExists) {
                rep.getFiles().add(file);
            }
        }

        content.put(generateKey(rep, file), consume(data));
    }

    @Override
    public void writeContent(Representation rep, File file, long rangeStart, long rangeEnd,
            OutputStream os) throws IOException {
        byte[] data = content.get(generateKey(rep, file));
        if (data == null) { throw new FileNotExistsException(); }
        if (rangeStart != -1 && rangeEnd != -1) {
            data = Arrays.copyOfRange(data, (int)rangeStart, (int)rangeEnd);
        }
        os.write(data);
    }

    @Override
    public void writeContent(Representation rep, File file, OutputStream os) throws IOException {
        writeContent(rep, file, -1, -1, os);
    }

    private String generateKey(Representation r, File f) {
        return r.getRecordId() + "|" + r.getSchema() + "|" + r.getVersion() + "|" + f.getFileName();
    }

    private byte[] consume(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }
}