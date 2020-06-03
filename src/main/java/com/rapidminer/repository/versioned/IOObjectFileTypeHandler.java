/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository.versioned;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.storage.hdf5.Hdf5ExampleSetReader;
import com.rapidminer.storage.hdf5.HdfReaderException;
import com.rapidminer.tools.LogService;
import com.rapidminer.versioning.repository.DataSummary;
import com.rapidminer.versioning.repository.FileTypeHandler;
import com.rapidminer.versioning.repository.FileTypeHandlerRegistry;
import com.rapidminer.versioning.repository.GeneralFile;
import com.rapidminer.versioning.repository.GeneralFolder;
import com.rapidminer.versioning.repository.RepositoryFile;


/**
 * Sub interface for {@link FileTypeHandler} that is specific to IOObjects
 *
 * @param <T>
 * 		the type of IOObject that is handled by the handler
 * @author Jan Czogalla
 * @since 9.7
 */
public interface IOObjectFileTypeHandler<T extends IOObject, U extends IOObjectEntry> extends FileTypeHandler<T> {

	/**
	 * The suffix for {@link ExampleSet}s written in hdf5 format, see {@link com.rapidminer.storage.hdf5.ExampleSetHdf5Writer}
	 */
	String DATA_TABLE_FILE_ENDING = "rmhdf5table";
	String ERROR_READING_FILE = "com.rapidminer.repository.versioned.IOObjectFileTypeHandler.error_reading_file";

	/** @return the suffix associated with this handler */
	String getSuffix();

	/** @return the topmost class associated with this handler */
	Class<T> getIOOClass();

	/**
	 * @return the specific entry type class this handler would create in the repository
	 */
	Class<U> getEntryType();

	default void register() {
		FileTypeHandlerRegistry.register(getSuffix(), this);
		IOObjectSuffixRegistry.register(getIOOClass(), getSuffix());
		IOObjectEntryTypeRegistry.register(getIOOClass(), getEntryType());
	}

	/**
	 * General {@link FileTypeHandler} for {@link IOObject} entries in the repository. Handles both entry creation
	 * ({@link #init(String, GeneralFolder)} as well as data summary extraction ({@link #createDataSummary(GeneralFile)}.
	 *
	 * @author Jan Czogalla
	 * @since 9.7
	 */
	enum LegacyIOOHandler implements IOObjectFileTypeHandler<IOObject, BasicIOObjectEntry> {
		INSTANCE;

		private static final String IOO_SUFFIX = IOObjectEntry.IOO_SUFFIX.replaceFirst("^\\.", "");

		@Override
		public String getSuffix() {
			return IOO_SUFFIX;
		}

		@Override
		public Class<IOObject> getIOOClass() {
			return IOObject.class;
		}

		@Override
		public Class<BasicIOObjectEntry> getEntryType() {
			return BasicIOObjectEntry.class;
		}

		@Override
		public RepositoryFile<IOObject> init(String filename, GeneralFolder parent) {
			return new BasicIOObjectEntry(filename, FilesystemRepositoryAdapter.toBasicFolder(parent), IOObject.class);
		}

		@Override
		public DataSummary createDataSummary(GeneralFile<IOObject> repositoryFile) {
			if (!(repositoryFile instanceof BasicIOObjectEntry)) {
				return FaultyDataSummary.wrongFileType(repositoryFile);
			}
			IOObject ioObject;
			try {
				ioObject = ((BasicIOObjectEntry) repositoryFile).retrieveData(null);
			} catch (RepositoryException e) {
				LogService.log(LogService.getRoot(), Level.WARNING, e, ERROR_READING_FILE,
						((BasicIOObjectEntry) repositoryFile).getName());
				return FaultyDataSummary.withCause(repositoryFile, e);
			}
			MetaData md = MetaData.forIOObject(ioObject);
			MetaData.shrinkValues(md);
			return md;
		}
	}

	/**
	 * {@link FileTypeHandler} for {@link ExampleSet} entries in the repository with the new HDF5 format.
	 * Handles both entry creation ({@link #init(String, GeneralFolder)} as well as data summary extraction
	 * ({@link #createDataSummary(GeneralFile)}.
	 *
	 * @author Jan Czogalla
	 * @since 9.7
	 */
	enum HDF5TableHandler implements IOObjectFileTypeHandler<ExampleSet, BasicExampleSetEntry> {
		INSTANCE;

		@Override
		public String getSuffix() {
			return DATA_TABLE_FILE_ENDING;
		}

		@Override
		public Class<ExampleSet> getIOOClass() {
			return ExampleSet.class;
		}

		@Override
		public Class<BasicExampleSetEntry> getEntryType() {
			return BasicExampleSetEntry.class;
		}

		@Override
		public RepositoryFile<ExampleSet> init(String filename, GeneralFolder parent) {
			return new BasicExampleSetEntry(filename, FilesystemRepositoryAdapter.toBasicFolder(parent));
		}

		@Override
		public DataSummary createDataSummary(GeneralFile<ExampleSet> repositoryFile) {
			if (!(repositoryFile instanceof BasicExampleSetEntry)) {
				return FaultyDataSummary.wrongFileType(repositoryFile);
			}
			BasicExampleSetEntry entry = (BasicExampleSetEntry) repositoryFile;
			Path path = entry.getRepositoryAdapter().getRealPath(entry);
			try {
				// try to read MD directly from file
				ExampleSetMetaData emd = Hdf5ExampleSetReader.readMetaData(path);
				if (emd != null) {
					return emd;
				}
			} catch (HdfReaderException e) {
				LogService.log(LogService.getRoot(), Level.WARNING, e, ERROR_READING_FILE, entry.getName());
				return FaultyDataSummary.withCause(repositoryFile, e);
			} catch (IOException e) {
				// ignore and continue
			}
			try {
				// try to read full example set and create meta data from there
				IOObject ioObject = entry.retrieveData(null);
				MetaData md = MetaData.forIOObject(ioObject);
				MetaData.shrinkValues(md);
				return md;
			} catch (RepositoryException e) {
				LogService.log(LogService.getRoot(), Level.WARNING, e, ERROR_READING_FILE, entry.getName());
				return FaultyDataSummary.withCause(repositoryFile, e);
			}
		}
	}
}
