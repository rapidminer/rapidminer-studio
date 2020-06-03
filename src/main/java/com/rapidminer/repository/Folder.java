/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

	/**
	 * Whether this folder contains a subfolder with the given name.
	 *
	 * @param folderName the name of the folder, must not be {@code null} or empty
	 * @return {@code true} if it contains a subfolder with the given name; {@code false} otherwise
	 * @throws RepositoryException if something goes wrong
	 * @since 9.7
	 */
	boolean containsFolder(String folderName) throws RepositoryException;

	/**
	 * Whether this folder contains a data entry with the given name and data entry (sub-)type.
	 *
	 * @param dataName         the name of the data, must not be {@code null} or empty
	 * @param expectedDataType the expected specific {@link DataEntry} (sub-)type. Can be one of {@link ProcessEntry},
	 *                         {@link IOObjectEntry}, {@link ConnectionEntry}, and either {@link BinaryEntry} (if {@link
	 *                         Repository#isSupportingBinaryEntries()} is {@code true}) or {@link BlobEntry} (for legacy
	 *                         repositories that do not support the new binary entry concept). Must not be {@code null}!
	 *                         <br>
	 *                         Note: {@link IOObjectEntry IOObjectEntries} can have subtypes, meaning in file-based
	 *                         repositories it could even happen that multiple IOObjects sit next to each other, all
	 *                         having the same prefix but with distinct suffixes. (test.ioo, test.rmhdf5table, ...) For
	 *                         the purpose of this method, this scenario is not considered here. Even if you specify a
	 *                         specific subtype of {@link IOObjectEntry} as the expected data type, it will return
	 *                         {@code true} if at least one {@link IOObjectEntry} exists with the given name (aka prefix
	 *                         in this example). Because for historical reasons, {@link RepositoryLocation} only
	 *                         consists of a string which only includes the prefix of such entries, it would be
	 *                         impossible to later determine which specific subtype of an IOObject was requested.
	 *                         Therefore, the creation of such scenarios is prohibited for the user. The only scenario
	 *                         in which this case can happen, is if this state is achieved from the outside (think Git
	 *                         pull on versioned repositories). See {@link RepositoryLocation#locateData()} for more
	 *                         information.
	 * @return {@code true} if it contains a data entry with the given name; {@code false} otherwise
	 * @throws RepositoryException if something goes wrong
	 * @since 9.7
	 */
	boolean containsData(String dataName, Class<? extends DataEntry> expectedDataType) throws RepositoryException;

	/**
	 * @deprecated since 9.7, because it cannot distinguish between folders and files. Use {@link
	 * #containsFolder(String)} or {@link #containsData(String, Class)} instead!
	 */
	@Deprecated
	boolean containsEntry(String name) throws RepositoryException;

	Folder createFolder(String name) throws RepositoryException;

	IOObjectEntry createIOObjectEntry(String name, IOObject ioobject, Operator callingOperator,
									  ProgressListener progressListener) throws RepositoryException;

	/**
	 * <strong>Important:</strong> Make sure the XML is already encrypted for this repository (see {@link
	 * com.rapidminer.tools.encryption.EncryptionProvider} and {@link Operator#getXML(boolean, String)}), otherwise
	 * loading the process later again will cause a decryption failure!
	 */
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

	/**
	 * @deprecated since 9.7, we only use {@link BinaryEntry BinaryEntries} from now on that are 100% identical to
	 * the original content
	 */
	@Deprecated
	BlobEntry createBlobEntry(String name) throws RepositoryException;

	/**
	 * Creates a binary entry with the full name (including the suffix).
	 *
	 * @param name the full name of the binary file, including the suffix
	 * @return the entry, never {@code null}
	 * @throws RepositoryException if that repository cannot store binary entries, or something else goes wrong
	 * @since 9.7
	 */
	default BinaryEntry createBinaryEntry(String name) throws RepositoryException {
		throw new RepositoryException("Cannot store binary entries in this repository!");
	}

	/**
	 * Whether this folder supports {@link BinaryEntry BinaryEntries} or not. By default, returns {@code false}.
	 *
	 * @return {@code true} if it does; {@code false} otherwise.
	 * @since 9.7
	 */
	default boolean isSupportingBinaryEntries() {
		return false;
	}

	/**
	 * Returns true iff a child folder with the given name exists and a {@link #refresh()} would find this folder (or it
	 * is already loaded).
	 *
	 * @param folderName the name of the folder that is being requested
	 * @return {@code true} if there would be a child folder after a refresh with the given name; {@code false} if not
	 * or unknown
	 * @throws RepositoryException if something goes wrong
	 * @since 9.7
	 */
	boolean canRefreshChildFolder(String folderName) throws RepositoryException;

	/**
	 * Returns true iff a child data entry with the given name exists and a {@link #refresh()} would find this entry (or
	 * it is already loaded). This ignores the fact that multiple different data entry subtypes could exist at that
	 * location. One hit is enough.
	 *
	 * @param dataName the name of the data entry that is being requested
	 * @return {@code true} if there would be a child data entry after a refresh with the given name; {@code false} if
	 * not or unknown
	 * @throws RepositoryException if something goes wrong
	 * @since 9.7
	 */
	boolean canRefreshChildData(String dataName) throws RepositoryException;

	/**
	 * Returns true iff a child with the given name exists and a {@link #refresh()} would find this entry (or it is
	 * already loaded). This can either be a folder or a data entry.
	 *
	 * @return {@code true} if there would be a child after a refresh with the given name (either folder or file);
	 * {@code false} if not or unknown
	 * @throws RepositoryException if something goes wrong
	 * @deprecated since 9.7, use {@link #canRefreshChildFolder(String)} and {@link #canRefreshChildData(String)}
	 * instead!
	 */
	@Deprecated
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
