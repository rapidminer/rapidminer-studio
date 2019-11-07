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
package com.rapidminer.repository;

import java.util.List;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ProgressListener;


/**
 * An entry containing sub-entries.
 *
 * @author Simon Fischer
 */
public interface Folder extends Entry {

	/**
	 * The name of the special connections folder.
	 * <p>To get the connections folder for a repository, use {@link RepositoryTools#getConnectionFolder(Repository)}.
	 * </p>
	 */
	String CONNECTION_FOLDER_NAME = "Connections";

	String TYPE_NAME = "folder";

	/**
	 * Error message for creating non-connection entries in this folder.
	 */
	String MESSAGE_CONNECTION_FOLDER = I18N.getErrorMessage("repository.create_connection_folder");

	/**
	 * Error message for creating connection entries outside connection folders.
	 */
	String MESSAGE_CONNECTION_CREATION = I18N.getErrorMessage("repository.connection_create_outside");

	/**
	 * Error message for moving, deleting or renaming connection folders.
	 */
	String MESSAGE_CONNECTION_FOLDER_CHANGE = I18N.getErrorMessage("repository.connection_folder_change");

	/**
	 * Error message for trying to create another connections folder.
	 */
	String MESSAGE_CONNECTION_FOLDER_DUPLICATE = I18N.getErrorMessage("repository.connection_folder_duplicate");

	/**
	 * Error message when trying to create/copy a connection entry to a folder implementation which does not know about connection entries.
	 */
	String MESSAGE_CONNECTION_FOLDER_CONNECTIONS_UNKNOWN = I18N.getErrorMessage("repository.connection_folder_unknown");

	/**
	 * Error message when trying to create/copy a connection entry to a folder implementation which does not know about connection entries.
	 */
	String MESSAGE_CONNECTION_FOLDER_ERROR = I18N.getErrorMessage("repository.connection_folder_error");

	@Override
	default String getType() {
		return TYPE_NAME;
	}

	/**
	 * Checks whether this folder is the special "Connections" folder in which only connections can be stored. By
	 * default, the check is case sensitive! Override if it needs to be case-insensitive for special repositories!
	 *
	 * @return {@code true} if this folder is called 'Connections' in exactly this capitalization; {@code false}
	 * otherwise
	 */
	default boolean isSpecialConnectionsFolder() {
		Folder containingFolder = getContainingFolder();
		// folder is one step below the repository
		return containingFolder instanceof Repository
				// and repo supports connections
				&& ((Repository) containingFolder).supportsConnections()
				// and has the special name
				&& isConnectionsFolderName(getName(), true);
	}

	List<DataEntry> getDataEntries() throws RepositoryException;

	List<Folder> getSubfolders() throws RepositoryException;

	void refresh() throws RepositoryException;

	boolean containsEntry(String name) throws RepositoryException;

	Folder createFolder(String name) throws RepositoryException;

	IOObjectEntry createIOObjectEntry(String name, IOObject ioobject, Operator callingOperator,
									  ProgressListener progressListener) throws RepositoryException;

	ProcessEntry createProcessEntry(String name, String processXML) throws RepositoryException;

	/**
	 * Create a {@link ConnectionEntry} with the given name and store the {@link ConnectionInformation}.
	 *
	 * @param name
	 * 		of the connection entry
	 * @param connectionInformation
	 * 		the information to be stored in this {@link Entry}
	 * @return the created {@link Entry}
	 * @throws RepositoryException
	 * 		in case storing was not successful
	 * @see Repository#supportsConnections()
	 * @since 9.3.0
	 */
	default ConnectionEntry createConnectionEntry(String name, ConnectionInformation connectionInformation) throws RepositoryException {
		throw new RepositoryConnectionsNotSupportedException(MESSAGE_CONNECTION_FOLDER_CONNECTIONS_UNKNOWN);
	}

	BlobEntry createBlobEntry(String name) throws RepositoryException;

	/**
	 * Returns true iff a child with the given name exists and a {@link #refresh()} would find this
	 * entry (or it is already loaded).
	 *
	 * @throws RepositoryException
	 */
	boolean canRefreshChild(String childName) throws RepositoryException;

	/**
	 * Checks if the name is a special "Connections" folder name, i.e. it is the same as {@value
	 * #CONNECTION_FOLDER_NAME}. Case sensitivity is a parameter.
	 *
	 * @param name
	 * 		the name to check
	 * @param caseSensitive
	 * 		if {@code true}, the name must be exactly equal; otherwise the case is ignored
	 * @return whether the name is the special connections folder name
	 */
	static boolean isConnectionsFolderName(String name, boolean caseSensitive) {
		return caseSensitive ? CONNECTION_FOLDER_NAME.equals(name) : CONNECTION_FOLDER_NAME.equalsIgnoreCase(name);
	}

}
