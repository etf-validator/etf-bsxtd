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

import static de.interactive_instruments.etf.testdriver.bsx.BsxConstants.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.basex.core.BaseXException;
import org.basex.core.Context;
import org.basex.core.cmd.Close;
import org.basex.core.cmd.DropDB;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.util.Performance;

import de.interactive_instruments.IFile;
import de.interactive_instruments.SUtils;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.Arguments;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectDto;
import de.interactive_instruments.etf.dal.dto.run.TestRunDto;
import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.etf.model.ParameterSet;
import de.interactive_instruments.etf.model.Parameterizable;
import de.interactive_instruments.etf.testdriver.AbstractTestTask;
import de.interactive_instruments.etf.testdriver.AbstractTestTaskProgress;
import de.interactive_instruments.etf.testdriver.TestResultCollector;
import de.interactive_instruments.etf.testdriver.bsx.partitioning.DatabasePartitioner;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.properties.ConfigPropertyHolder;

/**
 * BaseX test run task for executing XQuery on a BaseX database.
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
class BasexTestTask extends AbstractTestTask {

    private final String dbName;
    // TODO remove
    private final WriteDao<TestObjectDto> testObjectDao;
    private final Context ctx;
    private final ConfigPropertyHolder config;
    // private final IFile dsDir;
    private QueryProcessor proc;
    private TestResultCollector resultCollector;

    static class BasexTaskProgress extends AbstractTestTaskProgress {
        void doInit(final long maxSteps) {
            initMaxSteps(maxSteps);
        }

        void doAdvance() {
            advance();
        }
    }

    /**
     * Default constructor.
     *
     */
    public BasexTestTask(final TestTaskDto testTaskDto, final WriteDao<TestObjectDto> testObjectDao,
            final ConfigPropertyHolder config) {
        super(testTaskDto, new BasexTaskProgress(), BasexTestTask.class.getClassLoader());
        this.testObjectDao = testObjectDao;
        this.config = config;

        this.dbName = BsxConstants.ETF_TESTDB_PREFIX + testTaskDto.getTestObject().getId().toString();
        this.ctx = new Context();
    }

    @Override
    protected void doRun() throws Exception {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        // Add missing default arguments
        final ParameterSet parameters = this.testTaskDto.getExecutableTestSuite().getParameters();
        final Arguments arguments = this.testTaskDto.getArguments();
        for (final Parameterizable.Parameter parameter : parameters.getParameters()) {
            if (!arguments.containsName(parameter.getName()) && !SUtils.isNullOrEmpty(parameter.getDefaultValue())) {
                arguments.setValue(parameter.getName(), parameter.getDefaultValue());
            }
        }

        advance();

        final BsxTestObjectPreparation preparation = new BsxTestObjectPreparation(testTaskDto, ctx, dbName, testObjectDao,
                getLogger());
        checkUserParameters(preparation);
        preparation.initDb(config, resultCollector);

        advance();
        advance();
        checkCancelStatus();

        // Load the test project as XQuery
        proc = new QueryProcessor(preparation.projectFile.readContent().toString(), ctx);
        if ("true".equals(config.getPropertyOrDefault(LOG_MEMORY, "false"))) {
            proc.jc().tracer = (message) -> {
                getLogger().info("Memory: " + Performance.getMemory());
                getLogger().info(message);
                return false;
            };
        } else {
            proc.jc().tracer = (message) -> {
                getLogger().info(message);
                return false;
            };
        }

        // Bind script variables
        // Workaround: Wrap File around URI for a clean path or basex will
        // throw an exception
        final File tmpResultFile = new File(resultCollector.getTempDir(), "TestTaskResult-" + this.getId() + ".xml");
        proc.bind("$outputFile", tmpResultFile.getAbsolutePath());
        proc.bind("$testTaskResultId", testTaskDto.getTestTaskResult().getId().getId());
        proc.bind("$attachmentDir", resultCollector.getAttachmentDir().getAbsolutePath());
        proc.bind("$projDir", preparation.projDir.getAbsolutePath());
        proc.bind("$dbBaseName", this.dbName);
        proc.bind("$tmpDir", this.resultCollector.getTempDir().getAbsolutePath());
        proc.bind("$dbDir", preparation.testDataDir.getAbsolutePath());
        proc.bind("$etsFile", testTaskDto.getExecutableTestSuite().getLocalPath());
        final int dbCount = preparation.testObject.properties().getPropertyAsInt("dbCount");
        proc.bind("$dbCount", dbCount);
        proc.bind("$reportLabel", ((TestRunDto) testTaskDto.getParent()).getLabel());
        proc.bind("$reportStartTimestamp", getProgress().getStartTimestamp().getTime());

        final EID testTaskResultId = EidFactory.getDefault().createRandomId();
        proc.bind("$testObjectId", "EID" + testTaskResultId);
        proc.bind("$testTaskResultId", "EID" + testTaskDto.getTestTaskResult().getId().getId());

        proc.bind("$testObjectId", "EID" + this.testTaskDto.getTestObject().getId().getId());
        proc.bind("$testTaskId", "EID" + this.testTaskDto.getId().getId());
        proc.bind("$testTaskResultId", "EID" + this.testTaskDto.getTestTaskResult().getId().getId());
        proc.bind("$testRunId", "EID" + this.testTaskDto.getParent().getId().getId());
        proc.bind("$executableTestSuiteId", "EID" + this.testTaskDto.getExecutableTestSuite().getId().getId());
        proc.bind("$translationTemplateBundleId",
                "EID" + this.testTaskDto.getExecutableTestSuite().getTranslationTemplateBundle().getId().getId());

        // Add errors about not well-formed or invalid XML
        proc.bind("validationErrors", preparation.getValidationErrors());

        if (preparation.prepareChunkXqFile != null && preparation.prepareChunkXqFile.exists()) {
            final StringBuilder dbNamesBuilder = new StringBuilder(DatabasePartitioner.isolatedDatabaseName(dbName, 0));
            for (int i = 1; i < dbCount; i++) {
                dbNamesBuilder.append(',');
                dbNamesBuilder.append(DatabasePartitioner.isolatedDatabaseName(dbName, i));
            }
            proc.namespace("isolate", "https://modules.etf-validator.net/isolate-data/1")
                    .bind("$isolate:dbNames", dbNamesBuilder.toString()).namespace("isolate", "");
        }

        setUserParameters(preparation);
        proc.uriResolver(preparation.uriResolver);
        proc.compile();

        getLogger().info("Compiling test script");
        advance();
        checkCancelStatus();

        getLogger().info("Starting XQuery tests");

        proc.value();

        final FileInputStream fileStream = new FileInputStream(tmpResultFile);
        getPersistor().streamResult(fileStream);

        if (testTaskDto.getParent() != null && testTaskDto.getTestObject() != null &&
                "true".equals(testTaskDto.getTestObject().properties().getPropertyOrDefault("temporary", "false"))) {
            final TestRunDto testRun = (TestRunDto) testTaskDto.getParent();
            if (testRun.getTestTasks().get(testRun.getTestTasks().size() - 1) == testTaskDto) {
                clean();
            }
        }

    }

    private void advance() {
        ((BasexTaskProgress) progress).doAdvance();
    }

    @Override
    protected void doInit() throws InitializationException {
        if (testTaskDto.getExecutableTestSuite().getLocalPath() == null) {
            throw new InitializationException("Required property 'localPath' must be set!");
        }

        this.resultCollector = this.getPersistor().getResultCollector();

        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        ((BasexTaskProgress) progress).doInit(testTaskDto.getExecutableTestSuite().getLowestLevelItemSize());
    }

    /**
     * Check the user parameters by executing the Project check file
     *
     * @throws IOException
     *             I/O error reading check file
     * @throws QueryException
     *             error executing check file
     */
    private void checkUserParameters(final BsxTestObjectPreparation preparation) throws IOException, QueryException {
        // Check parameters by executing the xquery script
        final String projectFile = preparation.projectFile.getName();
        final int li = projectFile.lastIndexOf("-");
        if (li > 0) {
            final String checkParamXqFileName = projectFile.substring(0, li) + PROJECT_CHECK_FILE_SUFFIX;
            final IFile checkParamXqFileNew = preparation.projDir.secureExpandPathDown(checkParamXqFileName);
            final IFile checkParamXqFile;
            if (!checkParamXqFileNew.exists()) {
                // Support old check parameter files names with -bsxpc.xq
                final String checkParamXqFileNameDeprecated = projectFile.substring(0, li)
                        + PROJECT_CHECK_FILE_SUFFIX_DEPRECATED;
                checkParamXqFile = preparation.projDir.secureExpandPathDown(checkParamXqFileNameDeprecated);
                if (checkParamXqFile.exists()) {
                    getLogger().warn("This information is intended for the ETS developer: "
                            + "deprecated parameter check file name is used. Please rename the file '{}' to '{}'. ",
                            checkParamXqFileNameDeprecated, checkParamXqFileName);
                }
            } else {
                checkParamXqFile = checkParamXqFileNew;
            }

            if (checkParamXqFile.exists()) {
                proc = new QueryProcessor(checkParamXqFile.readContent().toString(), ctx);
                try {
                    setUserParameters(preparation);
                    proc.uriResolver(preparation.uriResolver);
                    proc.compile();
                    proc.value();
                } catch (QueryException e) {
                    getLogger().info("Invalid user parameters. Error message: " + e.getMessage());
                    throw e;
                }
                getLogger().info("User parameters accepted");
                proc.close();
            }

        }
    }

    /**
     * Bind user parameters
     *
     * @throws QueryException
     *             database error
     */
    private void setUserParameters(final BsxTestObjectPreparation preparation) throws QueryException {
        // Bind additional user specified properties
        for (Map.Entry<String, String> property : this.testTaskDto.getArguments().values().entrySet()) {
            final Parameterizable.Parameter p = this.testTaskDto.getExecutableTestSuite().getParameters()
                    .getParameter(property.getKey());

            // do not set empty parameters unless they are required or the key is empty
            if (!SUtils.isNullOrEmpty(property.getKey()) && (!SUtils.isNullOrEmpty(property.getValue())
                    || (p != null && p.isRequired()))) {
                final String key = property.getKey().replaceAll("\\s+", "_");

                final String value;
                // set a absolute paths for file parameters
                if (p != null && "file-resource".equals(p.getType())) {
                    IFile file = preparation.resolveFile(property.getValue());
                    if (!file.exists()) {
                        throw new IllegalArgumentException(
                                "The file resource '" + property.getValue() + "' could not be found.");
                    }
                    value = file.getAbsolutePath();
                } else {
                    value = property.getValue();
                }

                // replace whitespaces with underscores
                proc.bind(key, value);
            }
        }
    }

    @Override
    public void doRelease() {
        try {
            if (proc != null) {
                proc.close();
            }
            new Close().execute(ctx);
            ctx.close();
        } catch (BaseXException e) {
            ExcUtils.suppress(e);
        }
        // set to null or this will cause
        // a memory leak in multiple test runs
        this.proc = null;
    }

    @Override
    protected void doCancel() {
        clean();
        if (proc != null) {
            proc.stop();
            this.proc = null;
        }
    }

    private void clean() {
        try {
            if (proc != null) {
                proc.close();
                this.proc = null;
            }
            for (final String dbName : ctx
                    .listDBs(ETF_TESTDB_PREFIX + this.testTaskDto.getTestObject().getId().getId() + "-*")) {
                try {
                    new DropDB(dbName).execute(ctx);
                } catch (final Exception e) {
                    ExcUtils.suppress(e);
                }
            }
        } catch (Exception e) {
            ExcUtils.suppress(e);
        }
    }
}
