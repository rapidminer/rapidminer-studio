/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.DateEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;


/**
 * @author Simon Fischer
 */
public class SimpleFolder extends SimpleEntry implements Folder, DateEntry {

	private List<DataEntry> data;
	private List<Folder> folders;

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();

	SimpleFolder(String name, SimpleFolder parent, LocalRepository repository) {
		super(name, parent, repository);
	}

	protected void mkdir() throws RepositoryException {
		File file = getFile();
		if (!file.exists()) {
			if (!file.mkdirs()) {
				throw new RepositoryException("Cannot create repository folder at '" + file + "'.");
			}
		}
	}

	@Override
	protected void handleRename(String newName) throws RepositoryException {
		renameFile(getFile(), newName);
	}

	@Override
	protected void handleMove(Folder newParent, String newName) throws RepositoryException {
		moveFile(getFile(), ((SimpleFolder) newParent).getFile(), newName, "");
	}

	protected File getFile() {
		return new File(((SimpleFolder) getContainingFolder()).getFile(), getName());
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
		data = new ArrayList<DataEntry>();
		folders = new ArrayList<Folder>();
		File fileFolder = getFile();
		if (fileFolder != null && fileFolder.exists()) {
			File[] listFiles = fileFolder.listFiles();
			for (File file : listFiles) {
				if (file.isHidden()) {
					continue;
				}
				if (file.isDirectory()) {
					folders.add(new SimpleFolder(file.getName(), this, getRepository()));
				} else if (file.getName().endsWith(".ioo")) {
					data.add(new SimpleIOObjectEntry(file.getName().substring(0, file.getName().length() - 4), this,
							getRepository()));
				} else if (file.getName().endsWith(".rmp")) {
					data.add(new SimpleProcessEntry(file.getName().substring(0, file.getName().length() - 4), this,
							getRepository()));

				} else if (file.getName().endsWith(".blob")) {
					data.add(new SimpleBlobEntry(file.getName().substring(0, file.getName().length() - 5), this,
							getRepository()));
				}
				Collections.sort(data, RepositoryTools.SIMPLE_NAME_COMPARATOR);
				Collections.sort(folders, RepositoryTools.SIMPLE_NAME_COMPARATOR);
			}
		}
	}

	@Override
	public IOObjectEntry createIOObjectEntry(String name, IOObject ioobject, Operator callingOperator, ProgressListener l)
			throws RepositoryException {
		// check for possible invalid name
		if (!RepositoryLocation.isNameValid(name)) {
			throw new RepositoryException(
					I18N.getMessage(I18N.getErrorBundle(), "repository.illegal_entry_name", name, getLocation()));
		}

		IOObjectEntry entry = new SimpleIOObjectEntry(name, this, getRepository());

		acquireWriteLock();
		try {
			ensureLoaded();
			data.add(entry);
		} finally {
			releaseWriteLock();
		}

		if (ioobject != null) {
			entry.storeData(ioobject, null, l);
		}
		getRepository().fireEntryAdded(entry, this);
		return entry;
	}

	@Override
	public Folder createFolder(String name) throws RepositoryException {
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
				// folder with the same name (no matter if they have different capitalization) must
				// not
				// be created
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
	public String getType() {
		return Folder.TYPE_NAME;
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
		// check for possible invalid name
		if (!RepositoryLocation.isNameValid(name)) {
			throw new RepositoryException(
					I18N.getMessage(I18N.getErrorBundle(), "repository.illegal_entry_name", name, getLocation()));
		}

		SimpleProcessEntry entry = null;
		acquireWriteLock();
		try {
			ensureLoaded();
			entry = new SimpleProcessEntry(name, this, getRepository());
			data.add(entry);
			try {
				entry.storeXML(processXML);
			} catch (RepositoryException e) {
				data.remove(entry);
				throw e;
			}
		} finally {
			releaseWriteLock();
		}
		if (entry != null) {
			getRepository().fireEntryAdded(entry, this);
		}
		return entry;
	}

	@Override
	public BlobEntry createBlobEntry(String name) throws RepositoryException {
		// check for possible invalid name
		if (!RepositoryLocation.isNameValid(name)) {
			throw new RepositoryException(
					I18N.getMessage(I18N.getErrorBundle(), "repository.illegal_entry_name", name, getLocation()));
		}

		BlobEntry entry = null;
		acquireWriteLock();
		try {
			ensureLoaded();
			entry = new SimpleBlobEntry(name, this, getRepository());
			data.add(entry);
		} finally {
			releaseWriteLock();
		}
		if (entry != null) {
			getRepository().fireEntryAdded(entry, this);
		}
		return entry;
	}

	@Override
	public boolean canRefreshChild(String childName) throws RepositoryException {
		// check existence of properties file
		return new File(getFile(), childName + SimpleEntry.PROPERTIES_SUFFIX).exists();
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
