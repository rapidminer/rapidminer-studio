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
package com.rapidminer.repository.local;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.DateEntry;
import com.rapidminer.repository.EntryCreator;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryConnectionsFolderImmutableException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryNotConnectionsFolderException;
import com.rapidminer.repository.RepositoryStoreOtherInConnectionsFolderException;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.tools.ConsumerWithThrowable;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;


/**
 * @author Simon Fischer, Jan Czogalla
 */
public class SimpleFolder extends SimpleEntry implements Folder, DateEntry {

	/**
	 * A map of {@link EntryCreator}, one for each {@link SimpleDataEntry}.
	 * @since 9.3
	 */
	private static final Map<String, EntryCreator<String, ? extends SimpleDataEntry, SimpleFolder, LocalRepository>> CREATOR_MAP;
	static {
		Map<String, EntryCreator<String, ? extends SimpleDataEntry, SimpleFolder, LocalRepository>> entryCreators = new HashMap<>();
		entryCreators.put(IOObjectEntry.IOO_SUFFIX, SimpleIOObjectEntry::new);
		entryCreators.put(ProcessEntry.RMP_SUFFIX, SimpleProcessEntry::new);
		entryCreators.put(BlobEntry.BLOB_SUFFIX, SimpleBlobEntry::new);
		// ignore connections outside connection folder
		entryCreators.put(ConnectionEntry.CON_SUFFIX, (s, f, r) -> f.isSpecialConnectionsFolder() ? new SimpleConnectionEntry(s, f, r) : null);
		CREATOR_MAP = Collections.unmodifiableMap(entryCreators);
	}

	private List<DataEntry> data;
	private List<Folder> folders;

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();

	public SimpleFolder(String name, SimpleFolder parent, LocalRepository repository) {
		super(name, parent, repository);
	}

	protected void mkdir() throws RepositoryException {
		File file = getFile();
		if (!file.exists() && !file.mkdirs()) {
			throw new RepositoryException("Cannot create repository folder at '" + file + "'.");
		}
	}

	@Override
	protected void handleRename(String newName) throws RepositoryException {
		if (isSpecialConnectionsFolder()) {
			throw new RepositoryConnectionsFolderImmutableException(MESSAGE_CONNECTION_FOLDER_CHANGE);
		}
		renameFile(getFile(), newName);
	}

	@Override
	protected void handleMove(Folder newParent, String newName) throws RepositoryException {
		if (isSpecialConnectionsFolder()) {
			throw new RepositoryConnectionsFolderImmutableException(MESSAGE_CONNECTION_FOLDER_CHANGE);
		} else if (newParent.isSpecialConnectionsFolder()) {
			throw new RepositoryStoreOtherInConnectionsFolderException(MESSAGE_CONNECTION_FOLDER);
		}
		moveFile(getFile(), ((SimpleFolder) newParent).getFile(), newName, "");
	}

	protected File getFile() {
		return getFile("");
	}

	@Override
	public List<DataEntry> getDataEntries() throws RepositoryException {
		acquireReadLock();
		try {
			if (isLoaded()) {
				return Collections.unmodifiableList(new ArrayList<>(data));
			}
		} finally {
			releaseReadLock();
		}
		acquireWriteLock();
		try {
			ensureLoaded();
			return Collections.unmodifiableList(new ArrayList<>(data));
		} finally {
			releaseWriteLock();
		}
	}

	@Override
	public List<Folder> getSubfolders() throws RepositoryException {
		acquireReadLock();
		try {
			if (isLoaded()) {
				return Collections.unmodifiableList(new ArrayList<>(folders));
			}
		} finally {
			releaseReadLock();
		}
		acquireWriteLock();
		try {
			ensureLoaded();
			return Collections.unmodifiableList(new ArrayList<>(folders));
		} finally {
			releaseWriteLock();
		}
	}

	private boolean isLoaded() {
		return data != null && folders != null;
	}

	/**
	 * Makes sure the corresponding content is loaded. This method will perform write operations,
	 * you need to acquire the write lock before calling it.
	 */
	private void ensureLoaded() throws RepositoryException {
		if (isLoaded()) {
			return;
		}
		data = new ArrayList<>();
		folders = new ArrayList<>();
		File fileFolder = getFile();
		if (fileFolder == null || !fileFolder.exists()) {
			return;
		}
		File[] listFiles = fileFolder.listFiles();
		if (listFiles == null) {
			throw new RepositoryException("Could not read folder contents of " + fileFolder);
		}
		if (listFiles.length == 0) {
			// no files found, nothing left to do
			return;
		}
		for (File file : listFiles) {
			if (file.isHidden()) {
				continue;
			}
			if (file.isDirectory()) {
				folders.add(new SimpleFolder(file.getName(), this, getRepository()));
			} else {
				String name = file.getName();
				int dotPos = name.lastIndexOf('.');
				if (dotPos >= 0) {
					String suffix = name.substring(dotPos);
					name = name.substring(0, dotPos);
					SimpleDataEntry entry = CREATOR_MAP.getOrDefault(suffix, EntryCreator.nullCreator()).create(name, this, getRepository());
					if (entry != null) {
						data.add(entry);
					}
				}
			}
		}
		data.sort(RepositoryTools.SIMPLE_NAME_COMPARATOR);
		folders.sort(RepositoryTools.SIMPLE_NAME_COMPARATOR);
	}

	@Override
	public IOObjectEntry createIOObjectEntry(String name, IOObject ioobject, Operator callingOperator, ProgressListener l)
			throws RepositoryException {
		if (ioobject instanceof ConnectionInformationContainerIOObject) {
			return createConnectionEntry(name, ((ConnectionInformationContainerIOObject) ioobject).getConnectionInformation());
		} else if (RepositoryTools.isInSpecialConnectionsFolder(this)) {
			throw new RepositoryStoreOtherInConnectionsFolderException(MESSAGE_CONNECTION_FOLDER);
		}
		ConsumerWithThrowable<SimpleIOObjectEntry, RepositoryException> storeAction;
		if (ioobject == null) {
			storeAction = x -> {};
		} else {
			storeAction = entry -> entry.storeData(ioobject, null, l);
		}
		return createEntry(name, SimpleIOObjectEntry::new, storeAction);
	}

	@Override
	public Folder createFolder(String name) throws RepositoryException {
		if (RepositoryTools.isInSpecialConnectionsFolder(this)) {
			throw new RepositoryStoreOtherInConnectionsFolderException(MESSAGE_CONNECTION_FOLDER);
		}
		// check for possible invalid name
		if (!RepositoryLocation.isNameValid(name)) {
			throw new RepositoryException(
					I18N.getMessage(I18N.getErrorBundle(), "repository.illegal_entry_name", name, getLocation()));
		}

		SimpleFolder newFolder = new SimpleFolder(name, this, getRepository());
		acquireWriteLock();
		try {
			ensureLoaded();
			for (Folder folder : folders) {
				// folder with the same name (no matter if they have different capitalization)
				// must
				// not
				// be
				// created
				if (folder.getName().toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH))) {
					throw new RepositoryException(
							I18N.getMessage(I18N.getErrorBundle(), "repository.repository_folder_already_exists", name));
				}
			}
			for (DataEntry entry : data) {
				if (entry.getName().equals(name)) {
					throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(),
							"repository.repository_entry_with_same_name_already_exists", name));
				}
			}
			newFolder.mkdir();
			folders.add(newFolder);
		} finally {
			releaseWriteLock();
		}
		getRepository().fireEntryAdded(newFolder, this);
		return newFolder;
	}

	@Override
	public String getDescription() {
		return "Folder '" + getName() + "'";
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public void refresh() throws RepositoryException {
		acquireWriteLock();
		try {
			data = null;
			folders = null;
		} finally {
			releaseWriteLock();
		}
		getRepository().fireRefreshed(this);
	}

	@Override
	public boolean containsEntry(String name) throws RepositoryException {
		acquireReadLock();
		try {
			if (isLoaded()) {
				return containsEntryNotThreadSafe(name);
			}
		} finally {
			releaseReadLock();
		}
		acquireWriteLock();
		try {
			ensureLoaded();
			return containsEntryNotThreadSafe(name);
		} finally {
			releaseWriteLock();
		}
	}

	private boolean containsEntryNotThreadSafe(String name) {
		for (Folder folder : folders) {
			if (folder.getName().equals(name)) {
				return true;
			}
		}
		for (DataEntry entry : data) {
			if (entry.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void delete() throws RepositoryException {
		if (isSpecialConnectionsFolder()) {
			throw new RepositoryConnectionsFolderImmutableException(MESSAGE_CONNECTION_FOLDER_CHANGE);
		}
		if (!Tools.delete(getFile())) {
			throw new RepositoryException("Cannot delete directory");
		} else {
			super.delete();
		}
	}

	void removeChild(SimpleEntry child) throws RepositoryException {
		int index;
		acquireWriteLock();
		try {
			ensureLoaded();
			if (child instanceof SimpleFolder) {
				index = folders.indexOf(child);
				folders.remove(child);
			} else {
				index = data.indexOf(child) + folders.size();
				data.remove(child);
			}
		} finally {
			releaseWriteLock();
		}
		getRepository().fireEntryRemoved(child, this, index);
	}

	void addChild(SimpleEntry child) throws RepositoryException {
		acquireWriteLock();
		try {
			ensureLoaded();
			if (child instanceof SimpleFolder) {
				folders.add((Folder) child);
			} else {
				data.add((DataEntry) child);
			}
		} finally {
			releaseWriteLock();
		}
		getRepository().fireEntryAdded(child, this);
	}

	@Override
	public ProcessEntry createProcessEntry(String name, String processXML) throws RepositoryException {
		if (RepositoryTools.isInSpecialConnectionsFolder(this)) {
			throw new RepositoryStoreOtherInConnectionsFolderException(MESSAGE_CONNECTION_FOLDER);
		}
		return createEntry(name, SimpleProcessEntry::new, entry -> entry.storeXML(processXML));
	}

	@Override
	public ConnectionEntry createConnectionEntry(String name, ConnectionInformation connectionInformation) throws RepositoryException {
		if (!isSpecialConnectionsFolder()) {
			throw new RepositoryNotConnectionsFolderException(MESSAGE_CONNECTION_CREATION);
		}
		return createEntry(name, SimpleConnectionEntry::new, entry -> entry.storeConnectionInformation(connectionInformation));
	}

	@Override
	public BlobEntry createBlobEntry(String name) throws RepositoryException {
		if (RepositoryTools.isInSpecialConnectionsFolder(this)) {
			throw new RepositoryStoreOtherInConnectionsFolderException(MESSAGE_CONNECTION_FOLDER);
		}
		return createEntry(name, SimpleBlobEntry::new, x -> {});
	}

	/**
	 * Creates a new {@link SimpleDataEntry} with the given name, using the specified creator and executing the store action
	 * after the entry was created and added. This method streamlines all four creation methods into one.
	 *
	 * @since 9.3
	 */
	private <T extends SimpleDataEntry> T createEntry(String name, EntryCreator<String, T, SimpleFolder, LocalRepository> creator,
													  ConsumerWithThrowable<T, RepositoryException> storeAction) throws RepositoryException {
		// check for possible invalid name
		if (!RepositoryLocation.isNameValid(name)) {
			throw new RepositoryException(
					I18N.getMessage(I18N.getErrorBundle(), "repository.illegal_entry_name", name, getLocation()));
		}
		T entry;
		acquireWriteLock();
		try {
			ensureLoaded();
			entry = creator.create(name, this, getRepository());
			data.add(entry);
			try {
				storeAction.acceptWithException(entry);
			} catch (RepositoryException e) {
				data.remove(entry);
				throw e;
			}
		} finally {
			releaseWriteLock();
		}
		getRepository().fireEntryAdded(entry, this);
		return entry;
	}

	@Override
	public boolean canRefreshChild(String childName) throws RepositoryException {
		// check existence of properties file
		childName += PROPERTIES_SUFFIX;
		File propFile = new File(getFile(), childName);
		if (!propFile.exists()) {
			return false;
		}
		try {
			// Relevant for Windows only; since file system is case insensitive, we have to be sure
			// that the referenced path is correct in regards to cases; the canonical path on Windows
			// will return the proper cased path (if it exists)
			// see https://stackoverflow.com/a/7896461, especially second comment
			if (!propFile.getCanonicalPath().endsWith(childName)) {
				return false;
			}
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	/**
	 * Checks whether this folder is the special "Connections" folder in which only connections can be stored. Opposed
	 * to the overwritten default method, this check is case insensitive to not break existing connections folders with
	 * different capitalization.
	 *
	 * @return {@code true} if this folder is called 'Connections' in any capitalization; {@code false} otherwise
	 */
	@Override
	public boolean isSpecialConnectionsFolder() {
		// on Windows, you can have a "connections" folder or some other capitalization instead of "Connections"
		// therefore, we have to account for this by simply checking case-insensitive as we cannot just create a new "Connections" folder as we could on Unix
		Folder containingFolder = getContainingFolder();
		// folder is one step below the repository
		return containingFolder instanceof Repository
				// and has the special name (case-insensitive)
				&& Folder.isConnectionsFolderName(getName(), false);
	}

	private void acquireReadLock() throws RepositoryException {
		try {
			readLock.lock();
		} catch (RuntimeException e) {
			throw new RepositoryException("Could not get read lock", e);
		}
	}

	private void releaseReadLock() throws RepositoryException {
		try {
			readLock.unlock();
		} catch (RuntimeException e) {
			throw new RepositoryException("Could not release read lock", e);
		}
	}

	private void acquireWriteLock() throws RepositoryException {
		try {
			writeLock.lock();
		} catch (RuntimeException e) {
			throw new RepositoryException("Could not get write lock", e);
		}
	}

	private void releaseWriteLock() throws RepositoryException {
		try {
			writeLock.unlock();
		} catch (RuntimeException e) {
			throw new RepositoryException("Could not release write lock", e);
		}
	}

	/**
	 * @since 7.4
	 */
	@Override
	public long getDate() {
		return getFile().lastModified();
	}
}
