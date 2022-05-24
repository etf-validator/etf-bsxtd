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
package de.interactive_instruments.etf.testdriver.bsx.partitioning;

import static de.interactive_instruments.etf.testdriver.bsx.BsxConstants.PREPARE_CHUNK_XQ_FILE_NAME;

import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;

import org.basex.core.BaseXException;
import org.basex.core.Context;
import org.basex.core.cmd.CreateDB;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.slf4j.Logger;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.testdriver.bsx.BsxConstants;
import de.interactive_instruments.etf.testdriver.bsx.BsxContextFactory;
import de.interactive_instruments.etf.testdriver.bsx.BsxUriResolver;
import de.interactive_instruments.exceptions.ExcUtils;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class ChunkXqFile {
    private final BsxUriResolver uriResolver;
    private final String chunkXqFileContent;

    public static ChunkXqFile createOrNull(final Context ctx, final IFile prepareChunkXqFile, final BsxUriResolver uriResolver)
            throws IOException {
        if (prepareChunkXqFile.exists()) {
            return new ChunkXqFile(ctx, prepareChunkXqFile, uriResolver);
        }
        return null;
    }

    private ChunkXqFile(final Context ctx, final IFile prepareChunkXqFile, final BsxUriResolver uriResolver)
            throws IOException {
        this.uriResolver = uriResolver;
        this.chunkXqFileContent = prepareChunkXqFile.readContent().toString();
        QueryProcessor initProc = null;
        try {
            // Initialize the QueryProcessor once. This is required for BaseX
            // to initialize the correct internal classloader, which otherwise
            // will not be able to load the ReusableRessources libraries on
            // partitioned test datasets (the problem occurs only on the
            // very first test run).
            initProc = new QueryProcessor(this.chunkXqFileContent, ctx);
            initProc.bind("$dbName", "");
            initProc.namespace("isolate", "https://modules.etf-validator.net/isolate-data/1")
                    .bind("isolate:dbName", "").namespace("isolate", "");
            initProc.uriResolver(uriResolver);
            initProc.compile();
        } catch (final OverlappingFileLockException | QueryException ignore) {
            // ignore error about invalid dbname, CL should be initialized now
            ExcUtils.suppress(ignore);
        } finally {
            BsxContextFactory.unloadModulesAndClose(initProc);
        }
    }

    void execute(final Logger logger, final Context ctx, final String dbName) {
        final String errorDbName = dbName + BsxConstants.ETF_ERROR_DB_SUFFIX;
        try {
            new CreateDB(errorDbName, "<notempty/>").execute(ctx);
            QueryProcessor updateProc = null;
            try {
                updateProc = new QueryProcessor(this.chunkXqFileContent, ctx);
                updateProc.jc().tracer = (message) -> {
                    logger.info(message);
                    return false;
                };
                updateProc.bind("$dbName", dbName);
                updateProc.namespace("isolate", "https://modules.etf-validator.net/isolate-data/1")
                        .bind("isolate:dbName", errorDbName).namespace("isolate", "");
                updateProc.uriResolver(uriResolver);
                updateProc.compile();
                updateProc.value();
                final int updates = updateProc.updates();
                if (updates > 0) {
                    logger.info("Isolated {} erroneous objects", updates / 2);
                }
            } finally {
                BsxContextFactory.unloadModulesAndClose(updateProc);
            }
        } catch (final OverlappingFileLockException | QueryException | BaseXException e) {
            logger.error("Error executing '{}' file for database '{}' : {}", PREPARE_CHUNK_XQ_FILE_NAME, dbName,
                    e.getMessage());
        }
    }
}
