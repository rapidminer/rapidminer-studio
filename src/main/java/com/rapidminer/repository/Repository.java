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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.operator.IOObject;
import com.rapidminer.repository.gui.RepositoryConfigurationPanel;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * @author Simon Fischer
 */
public interface Repository extends Folder {

	void addRepositoryListener(RepositoryListener l);

	void removeRepositoryListener(RepositoryListener l);

	/**
	 * This will return the {@link Folder} with the given name or {@code null} if no subfolder with the given name can be
	 * found.
	 *
	 * @param folderName the name of the folder, must not be {@code null} or empty
	 * @return the folder or {@code null} if it does not exist
	 * @throws RepositoryException if something goes wrong
	 * @since 9.7
	 */
	default Folder locateFolder(String folderName) throws RepositoryException {
		return RepositoryManager.getInstance(null).locateFolder(this, folderName, false);
	}

	/**
	 * This will return the {@link DataEntry} with the given name and data entry (sub-)type, or {@code null} if no data
	 * entry with the given name can be found.
	 * <p>
	 * Note: If multiple data entries of different types but with the same name (prefix) exist, will return the first
	 * one to be found if the expected type is not specified! This is a situation that usually cannot occur, except for
	 * versioned projects introduced in 9.7. There, a user can create e.g. a data table "test.rmhdf5table", and another
	 * user a process "test.rmp". That would not be a conflict for Git, but causes our repository (which only shows the
	 * prefixes for known types) to have 2 entries which could be found when locating "test". If however the type {@link
	 * ProcessEntry} is given, then it will find the process called "test", even if the example set "test" would be
	 * found first normally.
	 * </p>
	 *
	 * @param dataName                      the name of the data, must not be {@code null} or empty
	 * @param expectedDataType              the expected specific {@link DataEntry} (sub-)type. Can be one of {@link
	 *                                      ProcessEntry}, {@link IOObjectEntry}, {@link ConnectionEntry}, and either
	 *                                      {@link BinaryEntry} (if {@link Repository#isSupportingBinaryEntries()} is
	 *                                      {@code true}) or {@link BlobEntry} (for legacy repositories that do not
	 *                                      support the new binary entry concept). Must not be {@code null}!
	 *                                      <br>
	 *                                      Note: {@link IOObjectEntry IOObjectEntries} can have subtypes, meaning in
	 *                                      file-based repositories it could even happen that multiple IOObjects sit
	 *                                      next to each other, all having the same prefix but with distinct suffixes.
	 *                                      (test.ioo, test.rmhdf5table, ...) For the purpose of this method, this
	 *                                      scenario is not considered here. Even if you specify a specific subtype of
	 *                                      {@link IOObjectEntry} as the expected data type, it will return the first
	 *                                      {@link IOObjectEntry} it finds with the given name (aka prefix in this
	 *                                      example). Because for historical reasons, {@link RepositoryLocation} only
	 *                                      consists of a string which only includes the prefix of such entries, it
	 *                                      would be impossible to later determine which specific subtype of an IOObject
	 *                                      was requested. Therefore, the creation of such scenarios is prohibited for
	 *                                      the user. The only scenario in which this case can happen, is if this state
	 *                                      is achieved from the outside (think Git pull on versioned repositories).
	 * @param failIfDuplicateIOObjectExists if {@code true} and the expected data type is of {@link IOObjectEntry}, it
	 *                                      will check that the repository folder contains only a single {@link
	 *                                      IOObjectEntry} with the requested name (prefix). Otherwise it will throw a
	 *                                      {@link RepositoryIOObjectEntryDuplicateFoundException}. See {@link
	 *                                      RepositoryLocation#locateData()} for more information.
	 * @return the folder or {@code null} if it does not exist
	 * @throws RepositoryException if something goes wrong
	 * @since 9.7
	 */
	default <T extends DataEntry> T locateData(String dataName, Class<T> expectedDataType, boolean failIfDuplicateIOObjectExists) throws RepositoryException {
		return RepositoryManager.getInstance(null).locateData(this, dataName, expectedDataType, false, failIfDuplicateIOObjectExists);
	}

	/**
	 * This will return the entry if existing or null if it can't be found.
	 *
	 * @deprecated since 9.7, because it cannot distinguish between folders and files. Use {@link
	 * #locateFolder(String)} and {@link #locateData(String, Class, boolean)} instead
	 */
	@Deprecated
	default Entry locate(String path) throws RepositoryException {
		Entry entry = locateData(path, DataEntry.class, true);
		if (entry != null) {
			return entry;
		}
		return locateFolder(path);
	}

	/** Returns some user readable information about the state of this repository. */
	String getState();

	/** Returns the icon name for the repository. */
	String getIconName();

	/** Returns a piece of XML to store the repository in a configuration file. */
	Element createXML(Document doc);

	boolean shouldSave();

	/**
	 * Called after the repository is added.
	 */
	void postInstall();

	/**
	 * Called directly before the repository is removed.
	 */
	void preRemove();

	/** Returns true if the repository is configurable. */
	boolean isConfigurable();

	/**
	 * Returns whether this repository can successfully handle the {@link com.rapidminer.connection.ConnectionInformation
	 * ConnectionInformation} objects introduced with RapidMiner Studio 9.3.
	 *
	 * @return {@code true} if this repository supports connections; {@code false} otherwise. By default, returns {@code
	 * false}
	 * @see Folder#createConnectionEntry(String, ConnectionInformation)
	 * @since 9.3.0
	 */
	default boolean supportsConnections() {
		return false;
	}

	/**
	 * Returns whether this is a transient repository which:
	 * <ul>
	 *     <li>is NOT saved to the repositories.xml file</li>
	 *     <li>does NOT appear in the Global Search</li>
	 *     <li>does NOT allow saving of a process opened from it (it will always claim that the process is unchanged)</li>
	 * </ul>
	 *
	 * @return {@code true} if it should be treated as a transient repository; {@code false} otherwise (default)
	 * @since 9.7
	 */
	default boolean isTransient() {
		return false;
	}

	/**
	 * Gets the encryption context key that is used by the repository for encrypting files in it. See {@link
	 * com.rapidminer.tools.encryption.EncryptionProvider}. Defaults to {@link EncryptionProvider#DEFAULT_CONTEXT}.
	 *
	 * @return the encryption context key or {@code null}, in which case no encryption will be used (and e.g. passwords
	 * would be stored as-is, i.e. unencrypted)
	 * @since 9.7
	 */
	default String getEncryptionContext() {
		return EncryptionProvider.DEFAULT_CONTEXT;
	}

	/** Creates a configuration panel. */
	RepositoryConfigurationPanel makeConfigurationPanel();


	/**
	 * Allows to update or remove the entry if it is not compatible with the given {@link IOObject}. By default returns
	 * the given entry.
	 *
	 * @param ioobject
	 * 		the ioobject with which the entry should be compatible with
	 * @param entry
	 * 		the entry to check for compatibility
	 * @return an entry compatible with the ioobject or {@code null}
	 * @throws RepositoryException
	 * 		if updating or removing the entry fails
	 * @since 9.7
	 */
	default DataEntry updateIncompatibleEntry(IOObject ioobject, DataEntry entry) throws RepositoryException {
		return entry;
	}

	/**
	 * Gets the {@link IOObjectEntry} (sub-)type that would be used to represent the given {@link IOObject} class in the
	 * repository. By default, it returns {@link IOObjectEntry}, though some repositories may be able to differentiate
	 * between subtypes (like example set, model, etc). This is only used when loading data (i.e. the different
	 * IOObjects with the same name but different subtype has already occurred), <strong>not</strong> for storing.
	 * Storing is limited to one IOObject per name (prefix).
	 * <br>
	 * This became necessary with version 9.7, where external versioning could introduce different IOObject subtypes in
	 * the same folder (think "test.ioo" and "test.rmhdf5table". This is an undesirable situation, because it cannot be
	 * used in a process effectively as it is unknown what IOObject is actually desired further down the road, e.g.
	 * after a Retrieve operator. However, to let the user rectify the situation, data loading has to support this
	 * scenario, which is why this method exists.
	 *
	 * @param ioObjectClass the class of an {@link IOObject}, must not be {@code null}
	 * @return the class of the {@link IOObjectEntry} (sub-)type that would be used
	 * @since 9.7
	 */
	default Class<? extends IOObjectEntry> getIOObjectEntrySubtype(Class<? extends IOObject> ioObjectClass) {
		return IOObjectEntry.class;
	}
}
