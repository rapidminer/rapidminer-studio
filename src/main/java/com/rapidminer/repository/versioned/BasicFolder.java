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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javax.swing.Action;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.external.alphanum.AlphanumComparator;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryConnectionsFolderImmutableException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryLocationBuilder;
import com.rapidminer.repository.RepositoryLocationType;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.RepositoryNotConnectionsFolderException;
import com.rapidminer.repository.RepositoryStoreOtherInConnectionsFolderException;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.versioning.repository.GeneralFile;
import com.rapidminer.versioning.repository.RepositoryFolder;
import com.rapidminer.versioning.repository.exceptions.RepositoryFileException;
import com.rapidminer.versioning.repository.exceptions.RepositoryFileMissingException;
import com.rapidminer.versioning.repository.exceptions.RepositoryFilenameAlreadyExistsException;
import com.rapidminer.versioning.repository.exceptions.RepositoryFolderDeletionDeniedException;
import com.rapidminer.versioning.repository.exceptions.RepositoryFolderException;
import com.rapidminer.versioning.repository.exceptions.RepositoryFolderMissingException;
import com.rapidminer.versioning.repository.exceptions.RepositoryFolderRootMovingException;
import com.rapidminer.versioning.repository.exceptions.RepositoryFolderRootRenamingException;
import com.rapidminer.versioning.repository.exceptions.RepositoryFoldernameAlreadyExistsException;
import com.rapidminer.versioning.repository.exceptions.RepositoryImmutableException;
import com.rapidminer.versioning.repository.exceptions.RepositoryNamingException;


/**
 * A folder for the filesystem repository and versioned repository.
 *
 * @author Andreas Timm
 * @since 9.7
 */
public class BasicFolder implements Folder {

	private static final String DOT = ".";

	// the folder from the filesystem repository, will always be there
	private final RepositoryFolder fsFolder;
	// this folders parent, may be null
	private BasicFolder parent;
	// the repository
	protected FilesystemRepositoryAdapter repositoryAdapter;

	// current folders, will be lazily initialized
	private List<Folder> folders;
	// folders created during a refresh, will be merged into the new, refreshed folders in #getSubfolders()
	private ConcurrentLinkedDeque<Folder> newfolders = new ConcurrentLinkedDeque<>();
	// during refreshs folders are going to change, oldfolders will show the old ones and are necessary for deletion events
	private List<Folder> oldfolders = Collections.emptyList();
	// names of previously contained files, required for deletion events
	private List<String> oldfiles;
	// lock for folders access
	private ReentrantLock foldersLock = new ReentrantLock();


	BasicFolder(RepositoryFolder newFolder, FilesystemRepositoryAdapter repositoryAdapter, BasicFolder parent) {
		fsFolder = Objects.requireNonNull(newFolder);
		this.parent = parent;
		this.repositoryAdapter = repositoryAdapter;
	}

	@Override
	public List<DataEntry> getDataEntries() {
		List<GeneralFile> files = new ArrayList<>(fsFolder.getFiles());
		AlphanumComparator alphanumComparator = new AlphanumComparator(AlphanumComparator.AlphanumCaseSensitivity.INSENSITIVE);
		files.sort((entry1, entry2) -> alphanumComparator.compare(entry1.getFullName().toLowerCase(Locale.ENGLISH),
				entry2.getFullName().toLowerCase(Locale.ENGLISH)));
		List<DataEntry> entries = FilesystemRepositoryAdapter.asLegacyEntries(files, this);
		if (oldfiles == null) {
			oldfiles = files.stream().map(f -> f.getFullName().toLowerCase(Locale.ENGLISH)).collect(Collectors.toList());
		}
		return entries;
	}


	@Override
	public List<Folder> getSubfolders() {
		List<Folder> tempFolders = folders;
		if (tempFolders != null && newfolders.isEmpty()) {
			return new ArrayList<>(tempFolders);
		}

		try {
			foldersLock.lock();
			if (folders != null && newfolders.isEmpty()) {
				return new ArrayList<>(folders);
			}
			List<Folder> myNewFolders;
			if (folders == null) {
				myNewFolders = new ArrayList<>(FilesystemRepositoryAdapter.asLegacyFolders(fsFolder.getFolders(), repositoryAdapter, this, oldfolders));
			} else {
				myNewFolders = new ArrayList<>(folders);
			}
			// just make sure nothing is glued to the stack
			while (!newfolders.isEmpty()) {
				Folder remFolder = null;
				Folder poll = newfolders.pollFirst();
				if (poll == null) {
					break;
				}
				for (Folder aFolder : myNewFolders) {
					if (aFolder.getName().toLowerCase(Locale.ENGLISH).equals(poll.getName().toLowerCase(Locale.ENGLISH))) {
						remFolder = aFolder;
						break;
					}
				}
				// remove the new instance, the instance from newfolders is already in use
				if (remFolder != null) {
					myNewFolders.remove(remFolder);
				}
				myNewFolders.add(poll);
			}
			folders = new ArrayList<>(myNewFolders);
			return myNewFolders;
		} finally {
			foldersLock.unlock();
		}
	}


	@Override
	public Folder createFolder(String name) throws RepositoryException {
		if (RepositoryTools.isInSpecialConnectionsFolder(this)) {
			throw new RepositoryStoreOtherInConnectionsFolderException(MESSAGE_CONNECTION_FOLDER);
		}
		if (containsFolder(name)) {
			throw new RepositoryException("Folder with name '" + name + "' already exists");
		}

		try {
			return createSubfolder(name);
		} catch (RepositoryFoldernameAlreadyExistsException | RepositoryFilenameAlreadyExistsException e) {
			throw new RepositoryException("Entry with name '" + name + "' already exists", e);
		} catch (RepositoryNamingException e) {
			throw new RepositoryException("Name '" + name + "' is not allowed", e);
		} catch (RepositoryImmutableException e) {
			throw new RepositoryException("Cannot write to this repository", e);
		} catch (RepositoryFolderException e) {
			throw new RepositoryException(e.getMessage(), e);
		}
	}

	@Override
	public IOObjectEntry createIOObjectEntry(String name, IOObject ioobject, Operator callingOperator, ProgressListener progressListener) throws RepositoryException {
		if (RepositoryTools.isInSpecialConnectionsFolder(this)) {
			throw new RepositoryStoreOtherInConnectionsFolderException(MESSAGE_CONNECTION_FOLDER);
		}
		if (containsData(name, IOObjectEntryTypeRegistry.getEntryClassForIOObjectClass(ValidationUtil.requireNonNull(ioobject, "ioobject").getClass()))) {
			throw new RepositoryException("Entry with name '" + name + "' already exists");
		}

		if (progressListener != null) {
			progressListener.setTotal(100);
			progressListener.setCompleted(10);
		}

		String suffix = IOObjectSuffixRegistry.getSuffix(ioobject);
		if (!name.endsWith(suffix)) {
			name += DOT + suffix;
		}
		GeneralFile<IOObject> file;
		try {
			file = fsFolder.createFile(name);
		} catch (RepositoryFileException | RepositoryNamingException e) {
			throw new RepositoryException(e);
		} catch (RepositoryImmutableException e) {
			throw new RepositoryException("Cannot write to this repository", e);
		}
		if (progressListener != null) {
			progressListener.setCompleted(20);
		}
		if (file instanceof IOObjectEntry) {
			try {
				file.setData(ioobject);
			} catch (RepositoryFileException e) {
				throw new RepositoryException(e);
			} catch (RepositoryImmutableException e) {
				throw new RepositoryException("Cannot write to this repository", e);
			}
			return (IOObjectEntry) file;
		}
		throw new RepositoryException("Could not create file for ioobject " + name);
	}

	@Override
	public ProcessEntry createProcessEntry(String name, String processXML) throws RepositoryException {
		if (RepositoryTools.isInSpecialConnectionsFolder(this)) {
			throw new RepositoryStoreOtherInConnectionsFolderException(MESSAGE_CONNECTION_FOLDER);
		}
		ValidationUtil.requireNonNull(processXML, "processXML");
		if (containsData(name, ProcessEntry.class)) {
			throw new RepositoryException("Entry with name '" + name + "' already exists");
		}

		GeneralFile<String> file;
		if (!name.endsWith(ProcessEntry.RMP_SUFFIX)) {
			name += ProcessEntry.RMP_SUFFIX;
		}
		try {
			file = fsFolder.createFile(name);
		} catch (RepositoryFileException | RepositoryImmutableException | RepositoryNamingException e) {
			throw new RepositoryException(e);
		}
		if (file instanceof BasicProcessEntry) {
			try {
				file.setData(processXML);
			} catch (RepositoryFileException | RepositoryImmutableException e) {
				throw new RepositoryException(e);
			}
			return (ProcessEntry) file;
		}
		throw new RepositoryException("Could not create file for process " + name);
	}

	@Override
	public ConnectionEntry createConnectionEntry(String name, ConnectionInformation connectionInformation) throws RepositoryException {
		if (!isSpecialConnectionsFolder()) {
			throw new RepositoryNotConnectionsFolderException(MESSAGE_CONNECTION_CREATION);
		}
		ValidationUtil.requireNonNull(connectionInformation, "connectionInformation");
		GeneralFile<ConnectionInformation> file;
		if (!name.endsWith(ConnectionEntry.CON_SUFFIX)) {
			name += ConnectionEntry.CON_SUFFIX;
		}
		try {
			file = fsFolder.createFile(name);
		} catch (RepositoryFileException | RepositoryImmutableException | RepositoryNamingException e) {
			throw new RepositoryException(e);
		}
		if (file instanceof ConnectionEntry) {
			try {
				file.setData(connectionInformation);
			} catch (RepositoryFileException | RepositoryImmutableException e) {
				throw new RepositoryException(e);
			}
			return (ConnectionEntry) file;
		}
		throw new RepositoryException("Could not create file for process " + name);
	}

	@Override
	public boolean isSupportingBinaryEntries() {
		return true;
	}

	@Override
	public BinaryEntry createBinaryEntry(String name) throws RepositoryException {
		if (RepositoryTools.isInSpecialConnectionsFolder(this)) {
			throw new RepositoryStoreOtherInConnectionsFolderException(MESSAGE_CONNECTION_FOLDER);
		}
		if (containsData(name, BinaryEntry.class)) {
			throw new RepositoryException("Entry with name '" + name + "' already exists");
		}

		GeneralFile<InputStream> file;
		try {
			file = fsFolder.createFile(name);
		} catch (RepositoryFileException | RepositoryImmutableException | RepositoryNamingException e) {
			throw new RepositoryException(e);
		}
		if (file instanceof BasicBinaryEntry) {
			return (BasicBinaryEntry) file;
		}
		throw new RepositoryException("Could not create file for binary entry " + name);
	}

	/**
	 * Cannot create blob entries in non-legacy repositories anymore.
	 *
	 * @param name irrelevant
	 * @return nothing as this will aways throw an exception
	 * @throws RepositoryException always thrown when this method is called
	 */
	@Override
	public BlobEntry createBlobEntry(String name) throws RepositoryException {
		throw new RepositoryException("Can only store blobs in old legacy repositories!");
	}

	@Override
	public void refresh() {
		// keep the order, current file status needs to be cached for upcoming events from the refresh
		filesChanged();
		fsFolder.refresh();
		foldersChanged();
		repositoryAdapter.fireRefreshed(this);
	}

	@Override
	public boolean containsFolder(String folderName) throws RepositoryException {
		try {
			fsFolder.getSubfolder(folderName);
			return true;
		} catch (RepositoryFolderMissingException e) {
			// returning false already
			return false;
		}
	}

	@Override
	public boolean containsData(String dataName, Class<? extends DataEntry> expectedDataType) throws RepositoryException {
		try {
			fsFolder.getFile(dataName);
			return true;
		} catch (RepositoryFileMissingException e) {
			List<String> suffixesToCheck;
			if (ProcessEntry.class.isAssignableFrom(expectedDataType)) {
				suffixesToCheck = Collections.singletonList(ProcessEntry.RMP_SUFFIX);
			} else if (ConnectionEntry.class.isAssignableFrom(expectedDataType)) {
				suffixesToCheck = Collections.singletonList(ConnectionEntry.CON_SUFFIX);
			} else if (BinaryEntry.class.isAssignableFrom(expectedDataType)) {
				// binary entry lookup has the suffix included
				suffixesToCheck = Collections.singletonList(null);
			} else if (IOObjectEntry.class.isAssignableFrom(expectedDataType)) {
				// n distinct IOObject classes can have n distinct suffixes, BUT as per this method's contract, we want to know any prefix hit
				suffixesToCheck = new ArrayList<>(IOObjectSuffixRegistry.getRegisteredSuffixes());
			} else {
				// completely unknown type: prevent if anything is named like it to be sure
				// should not happen, just for safety
				suffixesToCheck = new ArrayList<>(IOObjectSuffixRegistry.getRegisteredSuffixes());
				suffixesToCheck.add(null);
			}
			return suffixesToCheck.stream().anyMatch(suffix -> doesFileExist(dataName, suffix));
		}
	}

	private boolean doesFileExist(String dataName, String suffix) {
		try {
			String fullName = dataName;
			if (suffix != null) {
				if (!suffix.startsWith(DOT)) {
					fullName += DOT;
				}
				fullName += suffix;
			}
			fsFolder.getFile(fullName);
			return true;
		} catch (RepositoryFileMissingException ex) {
			// not found
		}
		return false;
	}

	@Override
	@Deprecated
	public synchronized boolean containsEntry(String name) throws RepositoryException {
		return containsFolder(name) || containsData(name, DataEntry.class);
	}

	@Override
	public boolean canRefreshChildFolder(String folderName) throws RepositoryException {
		if (repositoryAdapter.getRoot() != null) {
			String path = fsFolder.getPath();
			if (path.startsWith(String.valueOf(RepositoryLocation.SEPARATOR))) {
				path = path.substring(1);
			}
			File[] a = repositoryAdapter.getRoot().resolve(path).toFile()
					.listFiles((file) -> file.isDirectory() && file.getName().toLowerCase(Locale.ENGLISH).equals(folderName.toLowerCase(Locale.ENGLISH)));
			return a != null && a.length > 0;
		}
		return false;
	}

	@Override
	public boolean canRefreshChildData(String dataName) throws RepositoryException {
		if (repositoryAdapter.getRoot() != null) {
			String path = fsFolder.getPath();
			if (path.startsWith(String.valueOf(RepositoryLocation.SEPARATOR))) {
				path = path.substring(1);
			}
			File[] a = repositoryAdapter.getRoot().resolve(path).toFile()
					.listFiles((file) -> !file.isDirectory() && file.getName().toLowerCase(Locale.ENGLISH).equals(dataName.toLowerCase(Locale.ENGLISH)));
			return a != null && a.length > 0;
		}
		return false;
	}

	@Override
	@Deprecated
	public boolean canRefreshChild(String childName) throws RepositoryException {
		return canRefreshChildData(childName) || canRefreshChildFolder(childName);
	}

	@Override
	public boolean isSpecialConnectionsFolder() {
		// on Windows, you can have a "connections" folder or some other capitalization instead of "Connections"
		// therefore, we have to account for this by simply checking case-insensitive as we cannot just create a new "Connections" folder as we could on Unix
		boolean isOneLevelBelowRoot = fsFolder.getParent() != null && fsFolder.getParent().getParent() == null;
		// folder is one step below the repository, aka it
		return isOneLevelBelowRoot
				// and has the special name (case-insensitive)
				&& Folder.isConnectionsFolderName(getName(), false);
	}

	@Override
	public String getName() {
		return fsFolder.getName();
	}

	@Override
	public String getOwner() {
		return null;
	}

	@Override
	public String getDescription() {
		return "Folder '" + getName() + "'";
	}

	@Override
	public boolean isReadOnly() {
		return getRepositoryAdapter().isReadOnly();
	}

	@Override
	public boolean rename(String newName) throws RepositoryException {
		if (isSpecialConnectionsFolder()) {
			throw new RepositoryConnectionsFolderImmutableException(MESSAGE_CONNECTION_FOLDER_CHANGE);
		}
		if (getContainingFolder().containsFolder(newName)) {
			throw new RepositoryException("Folder with name '" + newName + "' already exists");
		}
		try {
			getRepositoryAdapter().getGeneralRepository().renameFolder(fsFolder, newName);
			return true;
		} catch (RepositoryFolderRootRenamingException | RepositoryFolderMissingException | RepositoryImmutableException | RepositoryNamingException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public boolean move(Folder newParent) throws RepositoryException {
		if (isSpecialConnectionsFolder()) {
			throw new RepositoryConnectionsFolderImmutableException(MESSAGE_CONNECTION_FOLDER_CHANGE);
		}
		if (RepositoryTools.isInSpecialConnectionsFolder(newParent)) {
			throw new RepositoryStoreOtherInConnectionsFolderException(MESSAGE_CONNECTION_FOLDER);
		}
		try {
			// the RepositoryManager will search in a NewFilesystemRepository and those return GeneralFolders
			RepositoryFolder parentFolder;
			if (newParent instanceof FilesystemRepositoryAdapter) {
				parentFolder = ((FilesystemRepositoryAdapter) newParent).getGeneralRepository().getRootFolder();
			} else if (newParent instanceof RepositoryFolder) {
				parentFolder = ((RepositoryFolder) newParent);
			} else {
				Folder locate = RepositoryManager.getInstance(null).locateFolder(getRepositoryAdapter(), newParent.getLocation().getPath(), false);
				if (locate instanceof BasicFolder) {
					parentFolder = ((BasicFolder) locate).fsFolder;
				} else if (locate instanceof RepositoryFolder) {
					parentFolder = (RepositoryFolder) locate;
				} else {
					throw new RepositoryException("Unexpected parent for move " + locate.getLocation());
				}
			}
			if (newParent instanceof FilesystemRepositoryAdapter) {
				parent = ((FilesystemRepositoryAdapter) newParent).getRootFolder();
			} else if (newParent instanceof BasicFolder) {
				parent = (BasicFolder) newParent;
			}

			getRepositoryAdapter().getGeneralRepository().move(fsFolder, parentFolder, true);
			return true;
		} catch (RepositoryImmutableException | RepositoryFolderRootMovingException | RepositoryNamingException | RepositoryFolderException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public boolean move(Folder newParent, String newName) throws RepositoryException {
		if (isSpecialConnectionsFolder()) {
			throw new RepositoryConnectionsFolderImmutableException(MESSAGE_CONNECTION_FOLDER_CHANGE);
		}
		if (RepositoryTools.isInSpecialConnectionsFolder(newParent)) {
			throw new RepositoryStoreOtherInConnectionsFolderException(MESSAGE_CONNECTION_FOLDER);
		}
		try {
			// the RepositoryManager will search in a NewFilesystemRepository and those return GeneralFolders
			RepositoryFolder parentFolder;
			if (newParent instanceof FilesystemRepositoryAdapter) {
				parentFolder = ((FilesystemRepositoryAdapter) newParent).getGeneralRepository().getRootFolder();
			} else if (newParent instanceof RepositoryFolder) {
				parentFolder = ((RepositoryFolder) newParent);
			} else {
				Folder locate = RepositoryManager.getInstance(null).locateFolder(getRepositoryAdapter(), newParent.getLocation().getPath(), false);
				if (locate instanceof BasicFolder) {
					parentFolder = ((BasicFolder) locate).fsFolder;
				} else if (locate instanceof RepositoryFolder) {
					parentFolder = (RepositoryFolder) locate;
				} else {
					throw new RepositoryException("Unexpected parent for move " + locate.getLocation());
				}
			}
			if (newParent instanceof FilesystemRepositoryAdapter) {
				parent = ((FilesystemRepositoryAdapter) newParent).getRootFolder();
			} else if (newParent instanceof BasicFolder) {
				parent = (BasicFolder) newParent;
			}

			getRepositoryAdapter().getGeneralRepository().move(fsFolder, parentFolder, newName, true);
			return true;
		} catch (RepositoryImmutableException | RepositoryFolderRootMovingException | RepositoryNamingException | RepositoryFolderException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public Folder getContainingFolder() {
		// if we are subfolder of root folder, we need to return repository here to adhere to legacy repository contract
		if (parent != null && parent.getContainingFolder() == null) {
			return getRepositoryAdapter();
		} else {
			return parent;
		}
	}

	@Override
	public boolean willBlock() {
		return false;
	}

	@Override
	public RepositoryLocation getLocation() {
		try {
			if (parent == null) {
				// I am root, the folder
				return new RepositoryLocationBuilder().withLocationType(RepositoryLocationType.FOLDER).buildFromPathComponents(getRepositoryAdapter().getName(), new String[0]);
			} else {
				return new RepositoryLocationBuilder().withLocationType(RepositoryLocationType.FOLDER).buildFromParentLocation(parent.getLocation(), getName());
			}
		} catch (MalformedRepositoryLocationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void delete() throws RepositoryException {
		if (isSpecialConnectionsFolder()) {
			throw new RepositoryConnectionsFolderImmutableException(MESSAGE_CONNECTION_FOLDER_CHANGE);
		}
		try {
			getRepositoryAdapter().getGeneralRepository().deleteFolder(fsFolder);
		} catch (RepositoryImmutableException | RepositoryFolderDeletionDeniedException e) {
			throw new RepositoryException("Could not delete " + fsFolder.getPath(), e);
		}
	}

	@Override
	public Collection<Action> getCustomActions() {
		return null;
	}

	@Override
	public String toString() {
		return getName();
	}

	/**
	 * The repository of basic folders is a {@link FilesystemRepositoryAdapter}, an adapter to the versioned
	 * repositories.
	 *
	 * @return the repositoryAdapter
	 */
	public FilesystemRepositoryAdapter getRepositoryAdapter() {
		return repositoryAdapter;
	}

	/**
	 * Get the backing filesystem repository folder
	 *
	 * @return a {@link RepositoryFolder} from the {@link BasicFolder#getRepositoryAdapter() Repository}
	 */
	public RepositoryFolder getFsFolder() {
		return fsFolder;
	}

	/**
	 * Get the subfolder with the specified name in a case-insensitive way, will block if a refresh is active in the
	 * background, so only use this if the latest information is necessary.
	 *
	 * @param folderName that should be found here, will be used case-insensitively. Must not be {@code null}
	 * @return the folder with that name
	 * @throws RepositoryFolderMissingException if no such folder exists or {@code null} is passed
	 */
	Folder getSubfolder(String folderName) throws RepositoryFolderMissingException {
		if (folderName == null) {
			throw new RepositoryFolderMissingException("null");
		}

		return getSubfolders().stream().filter(f -> f.getName().toLowerCase(Locale.ENGLISH).
				equals(folderName.toLowerCase(Locale.ENGLISH))).
				findFirst().orElseThrow(() -> new RepositoryFolderMissingException(folderName));
	}

	/**
	 * Retrieve an entry with the given name from this folder.
	 *
	 * @param fullName of the entry, with suffix
	 * @return the dataEntry with the name in this folder
	 * @throws RepositoryFileMissingException if no such file exists or {@code null} is passed
	 */
	DataEntry getFile(String fullName) throws RepositoryFileMissingException {
		if (fullName == null) {
			throw new RepositoryFileMissingException("null");
		}

		GeneralFile file = fsFolder.getFile(fullName);
		if (file instanceof DataEntry) {
			return (DataEntry) file;
		} else {
			throw new RepositoryFileMissingException(fullName);
		}
	}

	/**
	 * The names of old files that existed in this folder before a refresh happened
	 *
	 * @return list of names
	 */
	List<String> getOldfiles() {
		if (oldfiles == null) {
			filesChanged();
		}
		return oldfiles;
	}

	/**
	 * Notify when subfolders changed in the backend, remembers those as oldfolders for upcoming deletion events and as
	 * data in the meantime.
	 */
	void foldersChanged() {
		if (folders != null) {
			foldersLock.lock();
			if (folders != null) {
				oldfolders = folders;
				folders = null;
			}
			foldersLock.unlock();
		}
	}

	/**
	 * Notify when files changed in the backend.
	 */
	synchronized void filesChanged() {
		oldfiles = fsFolder.getFiles().stream()
				.map(f -> f.getFullName().toLowerCase(Locale.ENGLISH)).collect(Collectors.toList());
	}

	/**
	 * Create a subfolder, wait for completion and return it.
	 */
	Folder createSubfolder(String name) throws RepositoryFolderException, RepositoryImmutableException, RepositoryNamingException {
		RepositoryFolder subfolder = fsFolder.createSubfolder(name);
		if (foldersLock.tryLock()) {
			try {
				return getSubfolder(name);
			} catch (RepositoryFolderMissingException r) {
				// we just create it
				BasicFolder basicFolder = FilesystemRepositoryAdapter.asLegacyFolder(subfolder, repositoryAdapter, this);
				if (folders != null) {
					folders.add(basicFolder);
				} else {
					newfolders.addLast(basicFolder);
				}
				return basicFolder;
			} finally {
				foldersLock.unlock();
			}
		} else {
			// without the lock simply add to newfolders
			try {
				return getSubfolder(name);
			} catch (RepositoryFolderMissingException r) {
				// we just create it
				BasicFolder basicFolder = FilesystemRepositoryAdapter.asLegacyFolder(subfolder, repositoryAdapter, this);
				newfolders.addLast(basicFolder);
				return basicFolder;
			}
		}
	}
}
