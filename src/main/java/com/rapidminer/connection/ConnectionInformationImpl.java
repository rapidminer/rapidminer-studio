/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.connection;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;

import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.operator.Annotations;
import com.rapidminer.repository.Repository;


/**
 * Implementation of {@link ConnectionInformation}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public final class ConnectionInformationImpl implements ConnectionInformation {

	// the configuration data of a connection
	ConnectionConfiguration configuration;
	// statistics about the usage
	ConnectionStatistics statistics = new ConnectionStatisticsImpl();
	// Annotations for this object
	Annotations annotations = new Annotations();
	// files for the connection
	List<Path> libraryFiles = Collections.synchronizedList(new ArrayList<>());
	List<Path> otherFiles = Collections.synchronizedList(new ArrayList<>());
	// the repository of this connection
	Repository repository;

	@Override
	public ConnectionConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public ConnectionStatistics getStatistics() {
		return statistics;
	}

	@Override
	public List<Path> getLibraryFiles() {
		return new ArrayList<>(libraryFiles);
	}

	@Override
	public List<Path> getOtherFiles() {
		return new ArrayList<>(otherFiles);
	}

	@Override
	public Annotations getAnnotations() {
		return annotations;
	}

	@Override
	public Repository getRepository() {
		return repository;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ConnectionInformationImpl)) {
			return false;
		}
		ConnectionInformationImpl other = (ConnectionInformationImpl) obj;
		return Objects.equals(configuration, other.configuration) && CollectionUtils.isEqualCollection(libraryFiles, other.libraryFiles) && CollectionUtils.isEqualCollection(otherFiles, other.otherFiles) && Objects.equals(annotations, other.annotations);
	}

	@Override
	public int hashCode() {
		return Objects.hash(configuration, libraryFiles, otherFiles);
	}

	/**
	 * Add a lib file to this connection information
	 *
	 * @param name
	 * 		of the lib file
	 * @param inputStream
	 * 		source stream of the lib file
	 * @param md5Hash
	 * 		of the lib file, used for caching
	 */
	public void addLibFile(String name, InputStream inputStream, String md5Hash) throws IOException {
		libraryFiles.add(ConnectionInformationFileUtils.addFileInternally(name, inputStream, md5Hash));
	}

	/**
	 * Add other files to this connection information
	 *
	 * @param name
	 * 		of the file
	 * @param inputStream
	 * 		source stream of the file
	 * @param md5Hash
	 * 		of the file, used for caching
	 */
	public void addOtherFile(String name, InputStream inputStream, String md5Hash) throws IOException {
		otherFiles.add(ConnectionInformationFileUtils.addFileInternally(name, inputStream, md5Hash));
	}
}