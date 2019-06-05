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
import java.nio.file.Path;
import java.util.List;

import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.operator.Annotations;
import com.rapidminer.repository.Repository;


/**
 * Interface for connection information. Instances contain everything that is needed to connect, according to the
 * {@link ConnectionHandler} for the {@link ConnectionConfiguration ConnectionConfiguration's} type.
 * <p>
 * The {@link ConnectionConfiguration} is mandatory, a default {@link ConnectionStatistics} object is present and can be updated.
 * If necessary, the instance contains also a list of library and/or other files.
 * <p>
 * Everything is nicely zipped up in a single file. New connection information objects can be created using the {@link ConnectionInformationBuilder}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public interface ConnectionInformation {

	/** Internal name of the {@link ConnectionConfiguration} */
	String ENTRY_NAME_CONFIG = "Config";
	/** Internal name of the {@link ConnectionStatistics} */
	String ENTRY_NAME_STATS = "Stats";
	/** Internal name of the {@link Annotations} */
	String ENTRY_NAME_ANNOTATIONS = "Annotations";
	/** Internal name of the library file dir */
	String DIRECTORY_NAME_LIB = "Lib";
	/** Internal name of the general file dir */
	String DIRECTORY_NAME_FILES = "Files";

	/** Gets the connection configuration */
	ConnectionConfiguration getConfiguration();

	/** Gets the connection statistics if present */
	ConnectionStatistics getStatistics();

	/** Gets a (possibly empty) list of library files */
	List<Path> getLibraryFiles();

	/** Gets a (possibly empty) list of general files */
	List<Path> getOtherFiles();

	/** Gets the Annotations from this ConnectionInformation */
	Annotations getAnnotations();

	/** Returns the {@link Repository} this connection belongs to. Might be {@code null} */
	Repository getRepository();

	/** Create a carbon copy of this connection */
	default ConnectionInformation copy() {
		try {
			return new ConnectionInformationBuilder(this).build();
		} catch (IOException e) {
			// should not happen; see ConnectionConfigurationBuilder(ConnectionConfiguration)
			return null;
		}
	}
}
