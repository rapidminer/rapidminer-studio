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
package com.rapidminer.repository.search;

import org.apache.lucene.document.Document;

import com.rapidminer.RapidMiner;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryLocationBuilder;
import com.rapidminer.repository.RepositoryLocationType;
import com.rapidminer.search.GlobalSearchIndexer;
import com.rapidminer.search.GlobalSearchManager;
import com.rapidminer.search.GlobalSearchRegistry;
import com.rapidminer.search.GlobalSearchUtilities;
import com.rapidminer.search.GlobalSearchable;


/**
 * Responsible for implementing Global Search capabilities for repositories. See {@link GlobalSearchRegistry}
 * for more information.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public class RepositoryGlobalSearch implements GlobalSearchable {

	public static final String CATEGORY_ID = "repository";
	public static final String FIELD_CONNECTION_TYPE = "connection_type";
	/** @since 9.7 */
	public static final String ID_SEPARATOR = "|";

	/** property controlling whether full repository indexing is enabled */
	public static final String PROPERTY_FULL_REPOSITORY_INDEXING = "rapidminer.search.repository.enable_full_indexing";


	private final RepositoryGlobalSearchManager manager;


	public RepositoryGlobalSearch() {
		RapidMiner.registerParameter(new ParameterTypeBoolean(RepositoryGlobalSearch.PROPERTY_FULL_REPOSITORY_INDEXING, "", false));
		manager = new RepositoryGlobalSearchManager();

		if (GlobalSearchIndexer.INSTANCE.isInitialized()) {
			GlobalSearchRegistry.INSTANCE.registerSearchCategory(this);
		}
	}

	@Override
	public GlobalSearchManager getSearchManager() {
		return manager;
	}

	/**
	 * Maps an {@link com.rapidminer.repository.Entry#getType()} to the {@link DataEntry} (sub-)type.
	 *
	 * @param type the type string, must not be {@code null}
	 * @return the data entry class, or {@code null} if it is a folder
	 * @since 9.7
	 */
	public static Class<? extends DataEntry> getDataEntryTypeForTypeString(String type) {
		switch (type) {
			case ProcessEntry.TYPE_NAME:
				return ProcessEntry.class;
			case IOObjectEntry.TYPE_NAME:
				return IOObjectEntry.class;
			case ConnectionEntry.TYPE_NAME:
				return ConnectionEntry.class;
			case BinaryEntry.TYPE_NAME:
				return BinaryEntry.class;
			case BlobEntry.TYPE_NAME:
				return BlobEntry.class;
			case Folder.TYPE_NAME:
				return null;
			default:
				return DataEntry.class;
		}
	}

	/**
	 * Creates the unique ID that is used as the {@link GlobalSearchUtilities#FIELD_UNIQUE_ID} for repository entry
	 * search documents. Consists of the absolute repository path, as well as the {@link
	 * com.rapidminer.repository.Entry#getType()}.
	 *
	 * @param absoluteRepoPath the path, must not be {@code null}
	 * @param entryType        the type, must not be {@code null}
	 * @return the id string, never {@code null}
	 * @since 9.7
	 */
	public static String createUniqueIdForRepoItem(String absoluteRepoPath, String entryType) {
		return absoluteRepoPath + ID_SEPARATOR + entryType;
	}

	/**
	 * Creates the {@link RepositoryLocation} with all relevant parameters (location type, expected data type) set.
	 *
	 * @param document the document, must not be {@code null}
	 * @return the location, never {@code null}
	 * @throws MalformedRepositoryLocationException if creation of the location goes wrong, should not happen
	 * @since 9.7
	 */
	public static RepositoryLocation getRepositoryLocationForDocument(Document document) throws MalformedRepositoryLocationException {
		String[] idSplit = document.get(GlobalSearchUtilities.FIELD_UNIQUE_ID).split("\\" + ID_SEPARATOR);
		String loc = idSplit[0];
		String type = idSplit[1];
		Class<? extends DataEntry> dataEntryType = RepositoryGlobalSearch.getDataEntryTypeForTypeString(type);
		if (dataEntryType == null) {
			// folder
			return new RepositoryLocationBuilder().withFailIfDuplicateIOObjectExists(false).withLocationType(RepositoryLocationType.FOLDER).buildFromAbsoluteLocation(loc);
		} else {
			return new RepositoryLocationBuilder().withFailIfDuplicateIOObjectExists(false).withExpectedDataEntryType(dataEntryType).buildFromAbsoluteLocation(loc);
		}
	}
}
