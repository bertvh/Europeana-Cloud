package eu.europeana.cloud.service.mcs.persistent;

import eu.europeana.cloud.common.model.File;
import eu.europeana.cloud.common.model.Record;
import eu.europeana.cloud.common.model.Representation;
import eu.europeana.cloud.common.response.ResultSlice;
import eu.europeana.cloud.service.mcs.RecordService;
import eu.europeana.cloud.service.mcs.RepresentationSearchParams;
import eu.europeana.cloud.service.mcs.exception.CannotModifyPersistentRepresentationException;
import eu.europeana.cloud.service.mcs.exception.CannotPersistEmptyRepresentationException;
import eu.europeana.cloud.service.mcs.exception.FileAlreadyExistsException;
import eu.europeana.cloud.service.mcs.exception.FileNotExistsException;
import eu.europeana.cloud.service.mcs.exception.ProviderNotExistsException;
import eu.europeana.cloud.service.mcs.exception.RecordNotExistsException;
import eu.europeana.cloud.service.mcs.exception.RepresentationNotExistsException;
import eu.europeana.cloud.service.mcs.exception.WrongContentRangeException;
import eu.europeana.cloud.service.mcs.persistent.exception.SystemException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of record service using Cassandra as storage.
 */
@Service
public class CassandraRecordService implements RecordService {

    private final static Logger log = LoggerFactory.getLogger(CassandraRecordService.class);

    @Autowired
    private CassandraRecordDAO recordDAO;

    @Autowired
    private SwiftContentDAO contentDAO;

    @Autowired
    private SolrDAO solrDAO;

    @Autowired
    private CassandraDataProviderDAO dataProviderDAO;

    @Autowired
    private SolrRepresentationIndexer representationIndexer;


    /**
     * @inheritDoc
     */
    @Override
    public Record getRecord(String globalId)
            throws RecordNotExistsException {
        return recordDAO.getRecord(globalId);
    }


    /**
     * @inheritDoc
     */
    @Override
    public void deleteRecord(String globalId)
            throws RecordNotExistsException {
        List<Representation> allRecordRepresentationsInAllVersions = recordDAO.listRepresentationVersions(globalId);
        representationIndexer.removeRecordRepresentations(globalId);
        for (Representation repVersion : allRecordRepresentationsInAllVersions) {
            for (File f : repVersion.getFiles()) {
                try {
                    contentDAO.deleteContent(generateKeyForFile(globalId, repVersion.getSchema(),
                        repVersion.getVersion(), f.getFileName()));
                } catch (FileNotExistsException ex) {
                    log.warn("File {} was found in representation {}-{}-{} but no content of such file was found",
                        f.getFileName(), globalId, repVersion.getSchema(), repVersion.getVersion());
                }
            }
        }
        recordDAO.deleteRecord(globalId);
    }


    /**
     * @inheritDoc
     */
    @Override
    public void deleteRepresentation(String globalId, String schema)
            throws RepresentationNotExistsException {
        representationIndexer.removeRepresentation(globalId, schema);
        for (Representation rep : recordDAO.listRepresentationVersions(globalId, schema)) {
            for (File f : rep.getFiles()) {
                try {
                    contentDAO.deleteContent(generateKeyForFile(globalId, schema, rep.getVersion(), f.getFileName()));
                } catch (FileNotExistsException ex) {
                    log.warn("File {} was found in representation {}-{}-{} but no content of such file was found",
                        f.getFileName(), globalId, rep.getSchema(), rep.getVersion());
                }
            }
        }
        recordDAO.deleteRepresentation(globalId, schema);
    }


    /**
     * @inheritDoc
     */
    @Override
    public Representation createRepresentation(String globalId, String representationName, String providerId)
            throws ProviderNotExistsException {
        Date now = new Date();
        // check if data provider exists
        if (dataProviderDAO.getProvider(providerId) == null) {
            throw new ProviderNotExistsException();
        }
        Representation rep = recordDAO.createRepresentation(globalId, representationName, providerId, now);
        representationIndexer.insertRepresentation(rep);
        return rep;
    }


    /**
     * @inheritDoc
     */
    @Override
    public Representation getRepresentation(String globalId, String schema)
            throws RepresentationNotExistsException {
        Representation rep = recordDAO.getLatestPersistentRepresentation(globalId, schema);
        if (rep == null) {
            throw new RepresentationNotExistsException();
        } else {
            return rep;
        }
    }


    /**
     * @inheritDoc
     */
    @Override
    public Representation getRepresentation(String globalId, String schema, String version)
            throws RepresentationNotExistsException {
        Representation rep = recordDAO.getRepresentation(globalId, schema, version);
        if (rep == null) {
            throw new RepresentationNotExistsException();
        } else {
            return rep;
        }
    }


    /**
     * @inheritDoc
     */
    @Override
    public void deleteRepresentation(String globalId, String schema, String version)
            throws RepresentationNotExistsException, CannotModifyPersistentRepresentationException {
        Representation rep = recordDAO.getRepresentation(globalId, schema, version);
        if (rep == null) {
            throw new RepresentationNotExistsException();
        }
        if (rep.isPersistent()) {
            throw new CannotModifyPersistentRepresentationException();
        }
        representationIndexer.removeRepresentationVersion(version);

        for (File f : rep.getFiles()) {
            try {
                contentDAO.deleteContent(generateKeyForFile(globalId, schema, version, f.getFileName()));
            } catch (FileNotExistsException ex) {
                log.warn("File {} was found in representation {}-{}-{} but no content of such file was found",
                    f.getFileName(), globalId, rep.getSchema(), rep.getVersion());
            }
        }
        recordDAO.deleteRepresentation(globalId, schema, version);
    }


    /**
     * @inheritDoc
     */
    @Override
    public Representation persistRepresentation(String globalId, String schema, String version)
            throws RepresentationNotExistsException, CannotModifyPersistentRepresentationException,
            CannotPersistEmptyRepresentationException {
        Date now = new Date();
        Representation rep = recordDAO.getRepresentation(globalId, schema, version);
        if (rep == null) {
            throw new RepresentationNotExistsException();
        } else if (rep.isPersistent()) {
            throw new CannotModifyPersistentRepresentationException();
        }
        List<File> recordFiles = recordDAO.getFilesForRepresentation(globalId, schema, version);
        if (recordFiles == null) {
            throw new RepresentationNotExistsException();

        } else if (recordFiles.isEmpty()) {
            throw new CannotPersistEmptyRepresentationException();
        }

        recordDAO.persistRepresentation(globalId, schema, version, now);

        rep.setPersistent(true);
        rep.setCreationDate(now);

        representationIndexer.insertRepresentation(rep);

        return rep;
    }


    /**
     * @inheritDoc
     */
    @Override
    public List<Representation> listRepresentationVersions(String globalId, String schema)
            throws RepresentationNotExistsException {
        return recordDAO.listRepresentationVersions(globalId, schema);
    }


    /**
     * @inheritDoc
     */
    @Override
    public boolean putContent(String globalId, String schema, String version, File file, InputStream content)
            throws CannotModifyPersistentRepresentationException {
        DateTime now = new DateTime();
        Representation representation = recordDAO.getRepresentation(globalId, schema, version);
        if (representation.isPersistent()) {
            throw new CannotModifyPersistentRepresentationException();
        }

        boolean isCreate = true; // if it is create file operation or update content
        if (file.getFileName() == null) {
            file.setFileName(UUID.randomUUID().toString());
        } else {
            for (File f : representation.getFiles()) {
                if (f.getFileName().equals(file.getFileName())) {
                    isCreate = false;
                    break;
                }
            }
        }

        String keyForFile = generateKeyForFile(globalId, schema, version, file.getFileName());
        PutResult result;
        try {
            result = contentDAO.putContent(keyForFile, content);
        } catch (IOException ex) {
            throw new SystemException(ex);
        }
        file.setMd5(result.getMd5());
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        file.setDate(fmt.print(now));
        file.setContentLength(result.getContentLength());
        recordDAO.addOrReplaceFileInRepresentation(globalId, schema, version, file);
        return isCreate;
    }


    /**
     * @inheritDoc
     */
    @Override
    public void getContent(String globalId, String schema, String version, String fileName, long rangeStart,
            long rangeEnd, OutputStream os)
            throws FileNotExistsException, WrongContentRangeException, RepresentationNotExistsException {
        File file = getFile(globalId, schema, version, fileName);
        if (rangeStart > file.getContentLength() - 1) {
            throw new WrongContentRangeException("Start range must be less than file length");
        }
        try {
            contentDAO.getContent(generateKeyForFile(globalId, schema, version, fileName), rangeStart, rangeEnd, os);
        } catch (IOException ex) {
            throw new SystemException(ex);
        }
    }


    /**
     * @inheritDoc
     */
    @Override
    public String getContent(String globalId, String schema, String version, String fileName, OutputStream os)
            throws FileNotExistsException, RepresentationNotExistsException {
        Representation rep = getRepresentation(globalId, schema, version);
        String md5 = null;
        for (File f : rep.getFiles()) {
            if (fileName.equals(f.getFileName())) {
                md5 = f.getMd5();
            }
        }
        if (md5 == null) {
            throw new FileNotExistsException();
        }
        try {
            contentDAO.getContent(generateKeyForFile(globalId, schema, version, fileName), -1, -1, os);
        } catch (IOException ex) {
            throw new SystemException(ex);
        }
        return md5;
    }


    /**
     * @inheritDoc
     */
    @Override
    public void deleteContent(String globalId, String schema, String version, String fileName)
            throws FileNotExistsException, CannotModifyPersistentRepresentationException {
        Representation representation = recordDAO.getRepresentation(globalId, schema, version);
        if (representation.isPersistent()) {
            throw new CannotModifyPersistentRepresentationException();
        }
        recordDAO.removeFileFromRepresentation(globalId, schema, version, fileName);
        contentDAO.deleteContent(generateKeyForFile(globalId, schema, version, fileName));
    }


    /**
     * @inheritDoc
     */
    @Override
    public Representation copyRepresentation(String globalId, String schema, String version)
            throws RepresentationNotExistsException {
        Date now = new Date();
        Representation srcRep = recordDAO.getRepresentation(globalId, schema, version);
        if (srcRep == null) {
            throw new RepresentationNotExistsException();
        }

        Representation copiedRep = recordDAO.createRepresentation(globalId, schema, srcRep.getDataProvider(), now);
        representationIndexer.insertRepresentation(copiedRep);
        for (File srcFile : srcRep.getFiles()) {
            File copiedFile = new File(srcFile);
            try {
                contentDAO.copyContent(generateKeyForFile(globalId, schema, version, srcFile.getFileName()),
                    generateKeyForFile(globalId, schema, copiedRep.getVersion(), copiedFile.getFileName()));
            } catch (FileNotExistsException ex) {
                log.warn("File {} was found in representation {}-{}-{} but no content of such file was found",
                    srcFile.getFileName(), globalId, schema, version);
            } catch (FileAlreadyExistsException ex) {
                log.warn("File already exists in newly created representation?", copiedFile.getFileName(), globalId,
                    schema, copiedRep.getVersion());
            }
            recordDAO.addOrReplaceFileInRepresentation(globalId, schema, copiedRep.getVersion(), copiedFile);
        }
        //get version after all modifications
        return recordDAO.getRepresentation(globalId, schema, copiedRep.getVersion());
    }


    /**
     * @inheritDoc
     */
    @Override
    public ResultSlice<Representation> search(RepresentationSearchParams searchParams, String thresholdParam, int limit) {
        int startFrom = 0;
        try {
            startFrom = Integer.parseInt(thresholdParam);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Wrong threshold param for searching");
        }
        List<Representation> foundRepresenations = solrDAO.search(searchParams, startFrom, limit + 1);
        String nextResultToken = null;
        if (foundRepresenations.size() == limit + 1) {
            nextResultToken = Integer.toString(startFrom + limit + 1);
            foundRepresenations.remove(limit);
        }
        return new ResultSlice<>(nextResultToken, foundRepresenations);
    }


    /**
     * @inheritDoc
     */
    @Override
    public File getFile(String globalId, String schema, String version, String fileName)
            throws RepresentationNotExistsException, FileNotExistsException {
        final Representation rep = getRepresentation(globalId, schema, version);
        for (File f : rep.getFiles()) {
            if (f.getFileName().equals(fileName)) {
                return f;
            }
        }

        throw new FileNotExistsException();
    }


    /**
     * Generates unique key for file content.
     * 
     * @param recordId
     * @param repName
     * @param version
     * @param fileName
     * @return
     */
    private String generateKeyForFile(String recordId, String repName, String version, String fileName) {
        return recordId + "|" + repName + "|" + version + "|" + fileName;
    }

}
