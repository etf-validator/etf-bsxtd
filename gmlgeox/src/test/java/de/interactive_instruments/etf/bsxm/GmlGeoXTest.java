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
package de.interactive_instruments.etf.bsxm;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;

import org.basex.core.BaseXException;
import org.basex.core.Context;
import org.basex.core.cmd.CreateDB;
import org.basex.core.cmd.DropDB;
import org.basex.core.cmd.XQuery;
import org.basex.query.QueryException;
import org.basex.query.util.pkg.RepoManager;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.interactive_instruments.IFile;
import de.interactive_instruments.exceptions.ExcUtils;

/**
 * Run createTests in the IDE
 *
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class GmlGeoXTest {

    private Context ctx = new Context();

    private final static IFile ddtDirectory = new IFile("src/test/resources/ddt");
    private final static IFile outputDirectory = new IFile("build/tmp/ddt/results");
    private final static IFile tmpOutputDirectory = new IFile("build/tmp/ddt/tmp_outputs");
    private final static String TOPOX_INSTALL_PATH = "build/libs/etf-gmlgeox.xar";
    private final static String DB_NAME = "GMLGEOX-JUNIT-TEST-DB-000";
    private final static Logger logger = LoggerFactory.getLogger(GmlGeoXTest.class);

    @BeforeAll
    static void setUp() throws QueryException, IOException {
        final RepoManager repoManger = new RepoManager(new Context());
        try {
            repoManger.delete("ETF GmlGeoX");
        } catch (QueryException ign) {
            ExcUtils.suppress(ign);
        }
        repoManger.install(TOPOX_INSTALL_PATH);
        outputDirectory.ensureDir();
        tmpOutputDirectory.ensureDir();
        logger.info("Output directory is: {}", outputDirectory.getAbsolutePath());
        logger.info("Temporary directory is: {}", tmpOutputDirectory.getAbsolutePath());
    }

    @AfterEach
    @BeforeEach
    void dropDb() throws BaseXException {
        new DropDB(DB_NAME).execute(ctx);
    }

    private OutputStream outputStream(final IFile outputFile) {
        try {
            return new FileOutputStream(outputFile);
        } catch (FileNotFoundException e) {
            fail("Error writing to file", e);
            return null;
        }
    }

    @TestFactory
    Stream<DynamicTest> createTests() {

        logger.info("Generated tests are not displayed correctly. See issue https://github.com/gradle/gradle/issues/5975");

        return BsxDataDrivenTest.createDDT(ddtDirectory).stream()
                .map(t -> DynamicTest.dynamicTest(
                        "Data Test: " + t.getName(),
                        () -> {
                            final IFile testTmpOutputDirectory = tmpOutputDirectory.secureExpandPathDown(t.getName())
                                    .ensureDir();
                            new CreateDB(DB_NAME, t.getDataPath()).execute(ctx);
                            final XQuery xQuery = t.getQuery();
                            xQuery.bind("$tmpOutputDirectory", testTmpOutputDirectory.getAbsolutePath());
                            xQuery.bind("$dbname", DB_NAME);
                            final IFile outputFile = new IFile(outputDirectory, t.getName() + ".xml");
                            try {
                                xQuery.execute(ctx, outputStream(outputFile));
                            } catch (BaseXException e) {
                                logger.error("Executed Query : " + t.getPathToQueryFile());
                                fail("XQuery execution failed", e);
                            }
                            t.compare(outputFile);
                        }));
    }
}
