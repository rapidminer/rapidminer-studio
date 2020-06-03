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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationSerializer;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.metadata.ConnectionInformationMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.versioning.repository.DataSummary;
import com.rapidminer.versioning.repository.FileTypeHandler;
import com.rapidminer.versioning.repository.GeneralFile;
import com.rapidminer.versioning.repository.GeneralFolder;
import com.rapidminer.versioning.repository.RepositoryFile;


/**
 * {@link FileTypeHandler} for {@link ConnectionInformation} entries in the repository. Handles both entry creation
 * ({@link #init(String, GeneralFolder)} as well as data summary extraction ({@link #createDataSummary(GeneralFile)}.
 *
 * @author Jan Czogalla
 * @since 9.7
 */
public enum ConnectionInformationFileTypeHandler implements FileTypeHandler<ConnectionInformation> {
	INSTANCE;

	public String getSuffix() {
		return ConnectionEntry.CON_SUFFIX;
	}

	@Override
	public RepositoryFile<ConnectionInformation> init(String filename, GeneralFolder parent) {
		return new BasicConnectionEntry(filename, FilesystemRepositoryAdapter.toBasicFolder(parent));
	}

	@Override
	public DataSummary createDataSummary(GeneralFile<ConnectionInformation> repositoryFile) {
		if (!(repositoryFile instanceof BasicConnectionEntry)) {
			return FaultyDataSummary.wrongFileType(repositoryFile);
		}
		// read config from zip file
		BasicConnectionEntry entry = (BasicConnectionEntry) repositoryFile;
		FilesystemRepositoryAdapter repositoryAdapter = entry.getRepositoryAdapter();
		Path path = repositoryAdapter.getRealPath(entry);
		try (ZipFile zipFile = new ZipFile(path.toFile())) {
			ZipEntry configEntry = zipFile.getEntry(ConnectionInformation.ENTRY_NAME_CONFIG);
			if (configEntry != null) {
				ConnectionConfiguration connectionConfiguration = ConnectionInformationSerializer.INSTANCE
						.loadConfiguration(zipFile.getInputStream(configEntry), repositoryAdapter.getEncryptionContext());
				if (connectionConfiguration != null) {
					return new ConnectionInformationMetaData(connectionConfiguration);
				}
				return FaultyDataSummary.additionalInfo(repositoryFile, "connection configuration was null");
			}
		} catch (IOException e) {
			// ignore
		}
		try {
			// fallback to full read
			IOObject ioObject = ((BasicConnectionEntry) repositoryFile).retrieveData(null);
			return MetaData.forIOObject(ioObject);
		} catch (RepositoryException e) {
			return FaultyDataSummary.withCause(repositoryFile, e);
		}
	}


}
