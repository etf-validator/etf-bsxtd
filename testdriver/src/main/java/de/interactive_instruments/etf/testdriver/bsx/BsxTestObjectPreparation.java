/**
 * Copyright 2017-2020 European Union, interactive instruments GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * This work was supported by the EU Interoperability Solutions for
 * European Public Administrations Programme (http://ec.europa.eu/isa)
 * through Action 1.17: A Reusable INSPIRE Reference Platform (ARE3NA).
 */
package de.interactive_instruments.etf.testdriver.bsx;

import static de.interactive_instruments.etf.testdriver.bsx.BsxConstants.PREPARE_CHUNK_XQ_FILE_NAME;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.basex.core.Context;
import org.basex.core.cmd.DropDB;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

import de.interactive_instruments.*;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectDto;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectTypeDto;
import de.interactive_instruments.etf.dal.dto.run.TestRunDto;
import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.etf.testdriver.TestResultCollector;
import de.interactive_instruments.etf.testdriver.bsx.partitioning.ChunkXqFile;
import de.interactive_instruments.etf.testdriver.bsx.partitioning.DatabasePartitioner;
import de.interactive_instruments.etf.testdriver.bsx.transformers.ForwardingTransformerFactory;
import de.interactive_instruments.etf.testdriver.bsx.transformers.Transformer;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.io.FileHashVisitor;
import de.interactive_instruments.io.MultiFileFilter;
import de.interactive_instruments.io.MultiThreadedFilteredFileVisitor;
import de.interactive_instruments.properties.ConfigPropertyHolder;
import de.interactive_instruments.validation.ParalellSchemaValidationManager;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class BsxTestObjectPreparation {

    private final TestTaskDto testTaskDto;
    private final WriteDao<TestObjectDto> testObjectDao;
    private final String dbName;
    private final Logger logger;
    private final FileHashVisitor fileHashVisitor;
    private final TestObjectTypeDto testObjectType;
    private final MultiFileFilter filter;

    final BsxUriResolver uriResolver;
    final boolean testObjectChanged;
    final TestObjectDto testObject;
    final IFile testDataDir;
    final IFile projectFile;
    final IFile projDir;
    final IFile prepareChunkXqFile;
    private String validationErrors;

    public BsxTestObjectPreparation(final TestTaskDto testTaskDto, final Context ctx, final String dbName,
            final WriteDao<TestObjectDto> testObjectDao, final Logger logger)
            throws IOException {
        this.testTaskDto = testTaskDto;
        this.testObject = testTaskDto.getTestObject();
        this.testDataDir = new IFile(
                testObject.getResourceCollection().iterator().next().getUri(), dbName);
        this.testObjectDao = testObjectDao;
        testDataDir.expectDirIsReadable();
        this.dbName = dbName;
        this.logger = logger;
        try {
            this.projectFile = new IFile(new File(testTaskDto.getExecutableTestSuite().getLocalPath(),
                    "../" + testTaskDto.getExecutableTestSuite().getReference()).getCanonicalFile());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        this.projDir = new IFile(projectFile.getParentFile());
        this.projDir.expectDirIsReadable();
        this.projectFile.expectIsReadable();

        uriResolver = new BsxUriResolver(ctx, projDir);

        // Init file filter
        final String regex = testTaskDto.getArguments().value("regex");

        testObjectType = testTaskDto.getTestObject()
                .getTestObjectTypes().iterator().next();
        if (regex != null && !regex.isEmpty()) {
            filter = testObjectType.filenameFilter().get().and(new RegexFileFilter(regex));
        } else {
            filter = testObjectType.filenameFilter().get();
        }
        // Init file hash visitor
        fileHashVisitor = new FileHashVisitor(filter);
        Files.walkFileTree(testDataDir.toPath(),
                EnumSet.of(FileVisitOption.FOLLOW_LINKS), 5, fileHashVisitor);

        if ("false".equals(testObject.properties().getPropertyOrDefault("indexed", "false"))) {
            this.testObjectChanged = true;
            logger.info("Creating new tests databases to speed up tests. "
                    + "Indexing " + fileHashVisitor.getFileCount() + " files with an total size of "
                    + FileUtils.byteCountToDisplayRoundedSize(fileHashVisitor.getSize(), 2));
            testObject.setVersionFromStr("0.0.1");
        } else if (!fileHashVisitor.getHash().equals(testObject.getItemHash())) {
            // Delete old databases
            logger.info("Recreating tests databases as the Test Object has changed. "
                    + "Indexing " + fileHashVisitor.getFileCount() + " files with an total size of "
                    + FileUtils.byteCountToDisplayRoundedSize(fileHashVisitor.getSize(), 2));
            for (int i = 0; i < 10000; i++) {

                boolean dropped = Boolean
                        .parseBoolean(new DropDB(DatabasePartitioner.databaseName(dbName, i)).execute(ctx));
                new DropDB(DatabasePartitioner.isolatedDatabaseName(dbName, i)).execute(ctx);
                if (dropped) {
                    logger.info("Database " + i + " dropped");
                } else {
                    break;
                }
            }
            this.testObjectChanged = true;
        } else {
            this.testObjectChanged = false;
        }

        prepareChunkXqFile = new IFile(this.projectFile.getParentFile())
                .secureExpandPathDown(PREPARE_CHUNK_XQ_FILE_NAME);
    }

    IFile resolveFile(final String relativeProjectFilePath) {
        return new IFile(new IFile(testTaskDto.getExecutableTestSuite().getLocalPath()).getParent())
                .secureExpandPathDown(relativeProjectFilePath);
    }

    void initDb(final ConfigPropertyHolder config,
            final TestResultCollector resultCollector)
            throws SAXException, IOException, InterruptedException, ObjectWithIdNotFoundException {
        if (testObjectChanged) {
            // Validate against schema if schema file is set
            // First of all get the schema file
            final IFile schemaFile;
            if (!SUtils.isNullOrEmpty(this.testTaskDto.getArguments().value("Schema_file"))) {
                schemaFile = resolveFile(this.testTaskDto.getArguments().value("Schema_file"));
            } else if (!SUtils.isNullOrEmpty(this.testTaskDto.getArguments().value("schema"))) {
                schemaFile = resolveFile(this.testTaskDto.getArguments().value("schema"));
            } else {
                // STD fallback: check for a schema.xsd named file
                final IFile stdSchemaFile = resolveFile("schema.xsd");
                if (stdSchemaFile.exists()) {
                    schemaFile = stdSchemaFile;
                } else {
                    schemaFile = null;
                }
            }

            final TestRunDto testRunDto = ((TestRunDto) testTaskDto.getParent());

            // Initialize the validator
            final Factory<MultiFileFilter> validationFilter;
            // TODO replace with EGAID
            if (testObjectType
                    .isInstanceOf(EidFactory.getDefault().createAndPreserveStr("810fce18-4bf5-4c6c-a972-6962bbe3b76b"))) {
                int maxErrors = 100;
                testTaskDto.getArguments().value("maximum_number_of_error_messages_per_test");
                final String errorLimitStr = testTaskDto.getArguments().value("maximum_number_of_error_messages_per_test");
                // default fallback
                if (!SUtils.isNullOrEmpty(errorLimitStr)) {
                    try {
                        maxErrors = Integer.valueOf(errorLimitStr);
                    } catch (final NumberFormatException ign) {}
                }
                if (schemaFile != null && schemaFile.exists()) {
                    schemaFile.expectIsReadable();
                    logger.info("Initializing parallel schema validation.");
                    validationFilter = new ParalellSchemaValidationManager(schemaFile, maxErrors,
                            new Locale(testRunDto.getDefaultLang()));
                } else {
                    validationFilter = new ParalellSchemaValidationManager(maxErrors, new Locale(testRunDto.getDefaultLang()));
                    logger.info(
                            "Skipping schema validation because no schema file has been set in the test suite. Data are only checked for well-formedness.");
                }
            } else {
                validationFilter = new Factory<MultiFileFilter>() {
                    @Override
                    public MultiFileFilter create() {
                        return pathname -> true;
                    }

                    @Override
                    public void release() {}
                };
            }

            // Initialize Database Partitioner
            final DatabasePartitioner databaseVisitor;
            final Transformer transformer = ForwardingTransformerFactory.getInstance().create(testObjectType,
                    resultCollector.getAttachmentDir());
            final ChunkXqFile chunkXqFile = ChunkXqFile.createOrNull(prepareChunkXqFile, this.uriResolver);
            databaseVisitor = new DatabasePartitioner(config, logger, this.dbName,
                    testDataDir.getAbsolutePath().length(), transformer, chunkXqFile);

            // Combine filters and visitors
            final MultiThreadedFilteredFileVisitor multiThreadedFileVisitor = new MultiThreadedFilteredFileVisitor(
                    filter, validationFilter, Collections.singleton(databaseVisitor));
            Files.walkFileTree(testDataDir.toPath(), EnumSet.of(FileVisitOption.FOLLOW_LINKS), 5, multiThreadedFileVisitor);
            multiThreadedFileVisitor.startWorkers();
            multiThreadedFileVisitor.awaitTermination();
            databaseVisitor.release();

            testObject.setItemHash(fileHashVisitor.getHash());
            testObject.setVersion(
                    new Version(testObject.getVersion().getMajorVersion() + 1, 0, testObject.getVersion().getBugfixVersion()));
            testObject.setLastUpdateDateNow();
            testObject.properties().setProperty("indexed", "true");
            testObject.properties().setProperty("dbCount",
                    String.valueOf(databaseVisitor.getDbCount()));

            final long fileCount = fileHashVisitor.getFileCount();
            testObject.properties().setProperty("files", String.valueOf(fileCount));
            final long size = fileHashVisitor.getSize();
            testObject.properties().setProperty("size", String.valueOf(fileHashVisitor.getSize()));
            testObject.properties().setProperty("sizeHR", FileUtils.byteCountToDisplayRoundedSize(size, 2));

            final long fileCountTransformed = databaseVisitor.getFileCount();
            if (fileCountTransformed != fileCount) {
                testObject.properties().setProperty("files.transformed", String.valueOf(fileCountTransformed));
            }
            final long sizeTransformed = databaseVisitor.getSize();
            if (sizeTransformed != size) {
                testObject.properties().setProperty("size.transformed", String.valueOf(sizeTransformed));
                testObject.properties().setProperty("sizeHR.transformed",
                        FileUtils.byteCountToDisplayRoundedSize(sizeTransformed, 2));
            }

            // Todo: use preparation task and update the DTO in the higher layer
            testObjectDao.replace(testObject);
            // FIXME
            for (final TestTaskDto taskDto : testRunDto.getTestTasks()) {
                if (taskDto.getTestObject().getId().equals(testObject.getId())) {
                    taskDto.setTestObject(testObject);
                }
            }

            if (validationFilter instanceof ParalellSchemaValidationManager) {
                final ParalellSchemaValidationManager schemaValidatorManager = (ParalellSchemaValidationManager) validationFilter;
                logger.info("Validation ended with {} error(s)", schemaValidatorManager.getErrorCount());
                final StringBuilder skippedFiles = new StringBuilder("");
                final int skippedFilesSize = schemaValidatorManager.getSkippedFiles().size();
                if (skippedFilesSize > 0) {
                    skippedFiles.append("Skipped " + skippedFilesSize +
                            " invalid file(s): " + SUtils.ENDL);
                    for (File f : schemaValidatorManager.getSkippedFiles()) {
                        skippedFiles.append(f.getName() + SUtils.ENDL);
                    }
                }
                ((TestRunDto) testTaskDto.getParent()).setExchangeProperty("validationErrors",
                        skippedFiles.toString() + schemaValidatorManager.getErrorMessages());
            }
        }
    }

    String getValidationErrors() {
        if (this.validationErrors == null) {
            this.validationErrors = ((TestRunDto) testTaskDto.getParent()).getExchangeProperty("validationErrors");
        }
        return validationErrors;
    }
}
