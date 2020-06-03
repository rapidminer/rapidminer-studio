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

import static com.rapidminer.repository.versioned.FilesystemRepositoryUtils.normalizeSuffix;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.event.EventListenerList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryEntryWrongTypeException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryListener;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryLocationBuilder;
import com.rapidminer.repository.RepositoryLocationType;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.repository.gui.RepositoryConfigurationPanel;
import com.rapidminer.repository.versioned.IOObjectFileTypeHandler.HDF5TableHandler;
import com.rapidminer.repository.versioned.IOObjectFileTypeHandler.LegacyIOOHandler;
import com.rapidminer.repository.versioned.datasummary.ContentMapperStorage;
import com.rapidminer.repository.versioned.gui.FilesystemRepositoryConfigurationPanel;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;
import com.rapidminer.versioning.repository.FileSystemRepository;
import com.rapidminer.versioning.repository.FileTypeHandler;
import com.rapidminer.versioning.repository.FileTypeHandlerRegistry;
import com.rapidminer.versioning.repository.GeneralFile;
import com.rapidminer.versioning.repository.GeneralFolder;
import com.rapidminer.versioning.repository.GeneralRepository;
import com.rapidminer.versioning.repository.RepositoryChangeListener;
import com.rapidminer.versioning.repository.RepositoryFolder;
import com.rapidminer.versioning.repository.RepositoryUser;
import com.rapidminer.versioning.repository.exceptions.RepositoryFolderException;
import com.rapidminer.versioning.repository.exceptions.RepositoryFolderMissingException;
import com.rapidminer.versioning.repository.exceptions.RepositoryImmutableException;
import com.rapidminer.versioning.repository.exceptions.RepositoryNamingException;


/**
 * Filesystem based repository as an adapter to use the versioned filesystem based repository. This repository supports
 * binary files and all the usual RapidMiner files.
 *
 * @author Andreas Timm
 * @since 9.7
 */
public class FilesystemRepositoryAdapter implements Repository, NewFilesystemRepository {

	private static final Set<String> STANDARD_SUFFIXES = new HashSet<>();

	static {
		// Initialize file handling for the new versioned repository for our known types (e.g. .rmp, .ioo, ...)
		FileTypeHandler<InputStream> binaryFileTypeHandler = (filename, parent) -> new BasicBinaryEntry(filename, toBasicFolder(parent));
		FileTypeHandlerRegistry.setDefaultHandler(binaryFileTypeHandler);

		FileTypeHandlerRegistry.register(ProcessEntry.RMP_SUFFIX,
				(filename, parent) -> new BasicProcessEntry(filename, toBasicFolder(parent)));
		STANDARD_SUFFIXES.add(normalizeSuffix(ProcessEntry.RMP_SUFFIX));
		LegacyIOOHandler.INSTANCE.register();
		FileTypeHandlerRegistry.register(ConnectionInformationFileTypeHandler.INSTANCE.getSuffix(),
				ConnectionInformationFileTypeHandler.INSTANCE);
		STANDARD_SUFFIXES.add(normalizeSuffix(ConnectionInformationFileTypeHandler.INSTANCE.getSuffix()));
		HDF5TableHandler.INSTANCE.register();

		// we do not find legacy .blob files when looking for entries from open file from blob, need to handle them differently for compatibility
		FileTypeHandlerRegistry.register(BlobEntry.BLOB_SUFFIX, (filename, parent) -> new BasicBinaryEntry(filename, toBasicFolder(parent)) {
			@Override
			public String getName() {
				return getPrefix();
			}

			@Override
			public boolean rename(String newName) throws RepositoryException {
				return super.rename(newName + BlobEntry.BLOB_SUFFIX);
			}

			@Override
			public RepositoryLocation getLocation() {
				RepositoryLocation repLoc = BasicDataEntry.getRepositoryLocationWithoutSuffix(getPath(), getSuffix(),
						getRepositoryAdapter().getName());
				repLoc.setExpectedDataEntryType(BlobEntry.class);
				return repLoc;
			}
		});
		STANDARD_SUFFIXES.add(normalizeSuffix(BlobEntry.BLOB_SUFFIX));
	}

	private final EventListenerList listeners = new EventListenerList();
	/** The repository which this is an adapter for */
	private FileSystemRepository repository;
	/** flag for {@link Repository#isTransient()} */
	private boolean isTransient;
	/** The root folder of the new repository */
	protected BasicFolder rootFolder;
	protected final String encryptionContext;


	/**
	 * Initialize a filesystem repository. Internal method, do not use! Requires internal security permissions.
	 *
	 * @param alias             of the repository
	 * @param path              on the filesystem to the repository root
	 * @param editable          if this repository can be edited
	 * @param encryptionContext the encryption context that will be used to potentially encrypt values (see {@link
	 *                          com.rapidminer.tools.encryption.EncryptionProvider})
	 * @throws FileNotFoundException if the path does not exist
	 */
	public FilesystemRepositoryAdapter(String alias, String path, boolean editable, String encryptionContext)
			throws FileNotFoundException {
		this(alias, path, editable, false, encryptionContext);
	}

	/**
	 * Initialize a filesystem repository. Internal method, do not use! Requires internal security permissions.
	 *
	 * @param alias             of the repository
	 * @param path              on the filesystem to the repository root
	 * @param editable          if this repository can be edited
	 * @param isTransient       see {@link Repository#isTransient()}
	 * @param encryptionContext the encryption context that will be used to potentially encrypt values (see {@link
	 *                          com.rapidminer.tools.encryption.EncryptionProvider})
	 * @throws FileNotFoundException if the path does not exist
	 */
	public FilesystemRepositoryAdapter(String alias, String path, boolean editable, boolean isTransient, String encryptionContext)
			throws FileNotFoundException {
		this(encryptionContext);
		this.isTransient = isTransient;
		setRepository(new FileSystemRepository(alias, Paths.get(path), editable, null));
	}

	/**
	 * Main constructor for all FilesystemRepositoryAdapter constructions
	 */
	protected FilesystemRepositoryAdapter(String encryptionContext) {
		Tools.requireInternalPermission();
		this.encryptionContext = encryptionContext;
	}

	private FilesystemRepositoryAdapter() {
		throw new UnsupportedOperationException("Cannot instantiate without parameters");
	}

	@Override
	public void addRepositoryListener(RepositoryListener l) {
		listeners.add(RepositoryListener.class, l);
	}

	@Override
	public void removeRepositoryListener(RepositoryListener l) {
		listeners.remove(RepositoryListener.class, l);
	}

	@Override
	public String getState() {
		return I18N.getGUIMessage("gui.repository.fs_new.label");
	}

	@Override
	public String getIconName() {
		return I18N.getGUIMessage("gui.repository.fs_new.icon");
	}

	/**
	 * Delegate to the root folder
	 */
	@Override
	public void refresh() {
		rootFolder.refresh();
		ensureConnectionsFolder();
	}

	@Override
	public boolean containsFolder(String folderName) throws RepositoryException {
		return rootFolder.containsFolder(folderName);
	}

	@Override
	public boolean containsData(String dataName, Class<? extends DataEntry> expectedDataType) throws RepositoryException {
		return rootFolder.containsData(dataName, expectedDataType);
	}

	@Override
	@Deprecated
	public boolean containsEntry(String name) throws RepositoryException {
		return rootFolder.containsEntry(name);
	}

	@Override
	public Folder createFolder(String name) throws RepositoryException {
		return rootFolder.createFolder(name);
	}

	@Override
	public IOObjectEntry createIOObjectEntry(String name, IOObject ioobject, Operator callingOperator, ProgressListener progressListener) throws RepositoryException {
		return rootFolder.createIOObjectEntry(name, ioobject, callingOperator, progressListener);
	}

	@Override
	public ProcessEntry createProcessEntry(String name, String processXML) throws RepositoryException {
		return rootFolder.createProcessEntry(name, processXML);
	}

	@Override
	public ConnectionEntry createConnectionEntry(String name, ConnectionInformation connectionInformation) throws RepositoryException {
		return rootFolder.createConnectionEntry(name, connectionInformation);
	}

	@Override
	public BinaryEntry createBinaryEntry(String name) throws RepositoryException {
		return rootFolder.createBinaryEntry(name);
	}

	@Override
	public boolean isSupportingBinaryEntries() {
		return rootFolder.isSupportingBinaryEntries();
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
		return rootFolder.createBlobEntry(name);
	}

	@Override
	public Element createXML(Document doc) {
		return FilesystemRepositoryFactory.toXml(this, doc);
	}

	@Override
	public boolean shouldSave() {
		return true;
	}

	@Override
	public void postInstall() {
		// not needed
	}

	@Override
	public void preRemove() {
		// not needed
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return always {@code true}
	 */
	@Override
	public boolean isConfigurable() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return always {@code true}
	 */
	@Override
	public boolean supportsConnections() {
		return true;
	}

	@Override
	public String getEncryptionContext() {
		return encryptionContext;
	}

	@Override
	public boolean isTransient() {
		return isTransient;
	}

	@Override
	public RepositoryConfigurationPanel makeConfigurationPanel() {
		return new FilesystemRepositoryConfigurationPanel(false);
	}

	/**
	 * If the entry cannot hold the ioobject, the entry is deleted and {@code null} is returned so that the repository
	 * manager creates a new fitting entry.
	 */
	@Override
	public DataEntry updateIncompatibleEntry(IOObject ioobject, DataEntry entry) throws RepositoryException {
		// connections are not registered in the IOObjectSuffixRegistry, but if entry is ConnEntry all is fine
		if (ioobject instanceof ConnectionInformationContainerIOObject && entry instanceof ConnectionEntry) {
			return entry;
		}

		if (entry instanceof IOObjectEntry && entry instanceof GeneralFile
				&& !((GeneralFile) entry).getSuffix().equals(IOObjectSuffixRegistry.getSuffix(ioobject))) {
			try {
				entry.delete();
				return null;
			} catch (RepositoryException e) {
				if (e.getCause() instanceof IOException) {
					throw new RepositoryException(e.getCause());
				} else {
					throw new RepositoryEntryWrongTypeException(I18N.getErrorMessage("repository.error.incompatible_entry_type", entry.getLocation(), entry.getType()));
				}
			}
		}
		return entry;
	}

	@Override
	public boolean isSpecialConnectionsFolder() {
		// this is root
		return false;
	}

	@Override
	public List<DataEntry> getDataEntries() {
		return rootFolder.getDataEntries();
	}

	@Override
	public List<Folder> getSubfolders() {
		return rootFolder.getSubfolders();
	}

	@Override
	public boolean canRefreshChildFolder(String folderName) throws RepositoryException {
		return rootFolder.canRefreshChildFolder(folderName);
	}

	@Override
	public boolean canRefreshChildData(String dataName) throws RepositoryException {
		return rootFolder.canRefreshChildData(dataName);
	}

	@Override
	@Deprecated
	public boolean canRefreshChild(String childName) throws RepositoryException {
		return rootFolder.canRefreshChild(childName);
	}

	@Override
	public String getName() {
		return repository.getName();
	}

	@Override
	public String getOwner() {
		return null;
	}

	@Override
	public String getDescription() {
		return I18N.getGUIMessage("gui.repository.fs_new.tip", getRoot().toString());
	}

	@Override
	public boolean isReadOnly() {
		return !repository.isEditable();
	}

	@Override
	public boolean rename(String newName) {
		String formerName = getName();
		repository.setName(newName);
		fireEntryMoved(this, null, formerName);
		return true;
	}

	@Override
	public boolean move(Folder newParent) throws RepositoryException {
		return false;
	}

	@Override
	public boolean move(Folder newParent, String newName) throws RepositoryException {
		return false;
	}

	@Override
	public Class<? extends IOObjectEntry> getIOObjectEntrySubtype(Class<? extends IOObject> ioObjectClass) {
		return IOObjectEntryTypeRegistry.getEntryClassForIOObjectClass(ioObjectClass);
	}

	@Override
	public Folder getContainingFolder() {
		return null;
	}

	@Override
	public boolean willBlock() {
		return false;
	}

	@Override
	public RepositoryLocation getLocation() {
		try {
			return new RepositoryLocationBuilder().withLocationType(RepositoryLocationType.FOLDER).buildFromPathComponents(getName(), new String[0]);
		} catch (MalformedRepositoryLocationException e) {
			// cannot happen
			LogService.getRoot().log(Level.SEVERE, "Unexpected malformed repository location: " + e.getMessage(), e);
		}
		return null;
	}

	@Override
	public void delete() {
		RepositoryManager.getInstance(null).removeRepository(this);
	}

	@Override
	public Collection<Action> getCustomActions() {
		return new ArrayList<>(0);
	}

	@Override
	public String toString() {
		return getName();
	}

	/**
	 * The root folder of this repository. Unlike other repositories, this is not the root folder at the same time as
	 * being the repository.
	 *
	 * @return the root folder of this repository
	 */
	public BasicFolder getRootFolder() {
		return rootFolder;
	}

	/**
	 * Path to the base directory of this repository
	 *
	 * @return a Path on the filesystem
	 */
	public Path getRoot() {
		return repository.getBaseDir();
	}

	/**
	 * Access the real {@link Path} on the filesystem of a {@link BasicEntry}.
	 *
	 * @param dataEntry that needs to be accessed completely
	 * @return File of the BasicDataEntry
	 */
	public Path getRealPath(BasicEntry<?> dataEntry) {
		return repository.getFilePath(dataEntry);
	}

	/**
	 * The general repository is hidden behind this adapter. The general repository is a {@link FileSystemRepository}
	 * that can contain a {@link com.rapidminer.versioning.repository.VersioningAdapter}
	 *
	 * @return the repo, never {@code null}
	 */
	protected GeneralRepository<FileInputStream, FileOutputStream, RepositoryFolder> getGeneralRepository() {
		return repository;
	}

	/**
	 * Set this filesystem repository for this adapter, add a listener to it to forward changes to the Studio
	 * repository.
	 *
	 * Adds this repository to the {@link RepositoryManager} and ensures the connections folder.
	 *
	 * @param fileSystemRepository to be this adapters repository backend
	 */
	protected void setRepository(FileSystemRepository fileSystemRepository) {
		repository = fileSystemRepository;

		RepositoryChangeListener<RepositoryFolder> repoChangeListener = new RepositoryChangeListener<RepositoryFolder>() {
			@Override
			public void fileAdded(GeneralFile file) {
				if (file instanceof Entry) {
					Entry entry = (Entry) file;
					fireEntryAdded(entry, entry.getContainingFolder());
				}
			}

			@Override
			public void fileChanged(GeneralFile file) {
				if (file instanceof Entry) {
					Entry entry = (Entry) file;
					fireEntryChanged(entry);
				}
			}

			@Override
			public void fileMoved(GeneralFile file, RepositoryFolder formerParent, String formerName) {
				fileDeleted(formerName, formerParent);

				if (file instanceof Entry) {
					Entry entry = (Entry) file;
					fireEntryAdded(entry, entry.getContainingFolder());
				} else {
					BasicFolder folder = toBasicFolder(file.getParent());
					if (folder != null) {
						fireRefreshed(folder);
					}
				}
			}

			@Override
			public void fileRenamed(GeneralFile file, String formerName) {
				if (file instanceof Entry) {
					Entry entry = (Entry) file;
					// known repository objects in Studio (e.g. IOObject, Processes, etc) are not accessed with their suffix
					// we need to remove the suffix for known entries, otherwise there will be internal inconsistencies
					if (getInternalSuffixes().contains(RepositoryTools.getSuffixFromFilename(formerName))) {
						formerName = RepositoryTools.removeSuffix(formerName);
					}
					fireEntryMoved(entry, entry.getContainingFolder(), formerName);
				}
			}

			@Override
			public void fileDeleted(String filename, RepositoryFolder formerParent) {
				BasicFolder formerParentBasicFolder = toBasicFolder(formerParent);
				if (formerParentBasicFolder == null) {
					return;
				}

				String fakeName = filename;
				// known repository objects in Studio (e.g. IOObject, Processes, etc) are not accessed with their suffix
				// for proper faking here, we need to remove the suffix for known entries
				// otherwise e.g. the Global Search will not properly delete the given entry
				if (getInternalSuffixes().contains(RepositoryTools.getSuffixFromFilename(filename))) {
					fakeName = RepositoryTools.removeSuffix(filename);
				}
				BasicEntry fakeEntry = new BasicBinaryEntry(fakeName, formerParentBasicFolder);
				int index = formerParentBasicFolder.getOldfiles().indexOf(filename.toLowerCase(Locale.ENGLISH));
				if (index > -1) {
					fireEntryRemoved(fakeEntry, formerParentBasicFolder, index + formerParentBasicFolder.getSubfolders().size());
					formerParentBasicFolder.filesChanged();
				} else {
					// the old entry is not available anymore, notify that the folder content changed here
					fireRefreshed(formerParentBasicFolder);
				}
			}

			@Override
			public void folderAdded(GeneralFolder folder) {
				BasicFolder basicParentFolder = toBasicFolder(folder.getParent());
				BasicFolder basicFolder = null;
				if (basicParentFolder != null) {
					basicParentFolder.foldersChanged();
					try {
						basicFolder = (BasicFolder) basicParentFolder.getSubfolder(folder.getName());
					} catch (RepositoryFolderMissingException e) {
						// noop
					}
				}
				if (basicFolder == null) {
					// may be null if the versioned repository backend added it but the RM repository did not yet pick it up
					basicFolder = toBasicFolder(folder);
				}
				if (basicFolder != null) {
					fireEntryAdded(basicFolder, basicFolder.getContainingFolder());
				}
			}

			@Override
			public void folderMoved(RepositoryFolder folder, RepositoryFolder formerParent) {
				internalFolderDeleted(folder, toBasicFolder(formerParent));
				folderAdded(folder);
			}

			@Override
			public void folderMoved(RepositoryFolder folder, RepositoryFolder formerParent, String formerName) {
				internalFolderDeleted(new RepositoryFolder(formerName, getGeneralRepository(), formerParent) {
				}, toBasicFolder(formerParent));
				folderAdded(folder);
			}

			@Override
			public void folderRenamed(RepositoryFolder folder, String formerName) {
				try {
					Folder locatedFolder = locateFolder(folder.getPath());
					BasicFolder locatedParent = toBasicFolder(folder.getParent());
					if (locatedParent != null) {
						if (locatedFolder != null) {
							fireEntryMoved(locatedFolder, locatedParent, formerName);
						} else {
							fireRefreshed(locatedParent);
						}
					}
				} catch (RepositoryException e) {
					// noop
				}
			}

			@Override
			public void folderDeleted(RepositoryFolder folder, RepositoryFolder formerParent) {
				internalFolderDeleted(folder, toBasicFolder(formerParent));
			}

			/**
			 * Update data structures to reflect the deletion of a folder that was contained in the former parent but may
			 * now reside in a different location, thus only the name of the folder will be used to identify the
			 * obsolete data and get rid of it. Sends refresh or removal information to listeners.
			 */
			private void internalFolderDeleted(RepositoryFolder folder, BasicFolder formerParent) {
				if (formerParent == null || folder == null) {
					return;
				}

				// the old folder does not exist anymore, creating a virtual temp folder to contain the repository location
				BasicFolder deletedFolder = new BasicFolder(folder, FilesystemRepositoryAdapter.this, formerParent);
				String folderName = deletedFolder.getName().toLowerCase(Locale.ENGLISH);
				Optional<Integer> optionalIndex = formerParent.getSubfolders().stream()
						.filter(fold -> fold.getName().toLowerCase(Locale.ENGLISH).equals(folderName)).
								map(fold -> formerParent.getSubfolders().indexOf(fold)).findFirst();

				formerParent.foldersChanged();

				if (optionalIndex.isPresent()) {
					fireEntryRemoved(deletedFolder, formerParent, optionalIndex.get());
				} else {
					fireRefreshed(formerParent);
				}
			}
		};
		repository.setDataSummaryStorage(new ContentMapperStorage(this));

		rootFolder = asLegacyFolder(repository.getRootFolder(), this, null);

		RepositoryManager.getInstance(null).addRepository(this);

		repository.addChangeListener(repoChangeListener);

		ensureConnectionsFolder();
	}

	protected void fireEntryChanged(final Entry entry) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			try {
				l.entryChanged(entry);
			} catch (Exception e) {
				LogService.getRoot().log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	protected void fireEntryMoved(final Entry newEntry, Folder formerParent, String formerName) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			try {
				l.entryMoved(newEntry, formerParent, formerName);
			} catch (Exception e) {
				LogService.getRoot().log(Level.SEVERE, e.getMessage(), e);
			}
		}
		if (formerParent instanceof BasicFolder) {
			((BasicFolder) formerParent).filesChanged();
		} else if (formerParent instanceof FilesystemRepositoryAdapter) {
			((FilesystemRepositoryAdapter) formerParent).getRootFolder().filesChanged();
		}
	}

	protected void fireEntryAdded(final Entry newEntry, final Folder parent) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			try {
				l.entryAdded(newEntry, parent);
			} catch (Exception e) {
				LogService.getRoot().log(Level.SEVERE, e.getMessage(), e);
			}
		}
		if (parent instanceof BasicFolder) {
			((BasicFolder) parent).filesChanged();
		} else if (parent instanceof FilesystemRepositoryAdapter) {
			((FilesystemRepositoryAdapter) parent).getRootFolder().filesChanged();
		}
	}

	protected void fireRefreshed(final Folder folder) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			try {
				l.folderRefreshed(folder);
			} catch (Exception e) {
				LogService.getRoot().log(Level.SEVERE, e.getMessage());
			}
		}
	}

	protected void fireEntryRemoved(final Entry removedEntry, final Folder parent, final int index) {
		for (RepositoryListener l : listeners.getListeners(RepositoryListener.class)) {
			try {
				l.entryRemoved(removedEntry, parent, index);
			} catch (Exception e) {
				LogService.getRoot().log(Level.SEVERE, e.getMessage());
			}
		}
	}

	/**
	 * Returns the repository user accessing this repo.
	 *
	 * @return the user, can be {@code null}
	 */
	protected RepositoryUser getRepositoryUser() {
		return repository.getRepositoryUser();
	}

	/**
	 * Checks if a Connections folder already exists for this repository and if not, creates it.
	 */
	protected void ensureConnectionsFolder() {
		boolean folderExists;
		try {
			Folder subfolder = rootFolder.getSubfolder(Folder.CONNECTION_FOLDER_NAME);
			folderExists = subfolder != null;
		} catch (RepositoryFolderMissingException e) {
			folderExists = false;
		}
		boolean readOnly = isReadOnly();
		if (!folderExists) {
			try {
				if (readOnly) {
					getGeneralRepository().setEditable(true);
				}
				rootFolder.createSubfolder(Folder.CONNECTION_FOLDER_NAME);
			} catch (RepositoryFolderException | RepositoryImmutableException | RepositoryNamingException ex) {
				LogService.getRoot().log(Level.SEVERE, ex, () -> I18N.getErrorMessage("repository.create_connections_failed", getName()));
			} finally {
				if (readOnly) {
					getGeneralRepository().setEditable(false);
				}
			}
		}
	}

	/**
	 * Returns the suffixes for internal RapidMiner types, e.g. {@link IOObject}s, {@link Process}s, {@link
	 * ConnectionEntry}s and {@link BlobEntry}s.
	 *
	 * @return the suffixes that are not be displayed in the repository
	 */
	public static Collection<String> getInternalSuffixes() {
		Set<String> suffixes = new HashSet<>(STANDARD_SUFFIXES);
		suffixes.addAll(IOObjectSuffixRegistry.getRegisteredSuffixes());
		return suffixes;
	}

	/**
	 * Find the corresponding BasicFolder from Studio for the GeneralFolder in VersionedRepository
	 *
	 * @param folder a folder that is known in the versioned repository
	 * @return the corresponding folder in the repository adapter
	 */
	static BasicFolder toBasicFolder(GeneralFolder folder) {
		if (folder == null || folder.getRepository() == null) {
			return null;
		}
		String repositoryName = folder.getRepository().getName();
		String path = folder.getPath();
		try {
			RepositoryManager repoManager = RepositoryManager.getInstance(null);
			Folder locate = repoManager.locateFolder(repoManager.getRepository(repositoryName), path, false);
			if (locate instanceof BasicFolder) {
				return (BasicFolder) locate;
			} else if (locate instanceof FilesystemRepositoryAdapter) {
				return ((FilesystemRepositoryAdapter) locate).getRootFolder();
			}
		} catch (RepositoryException e) {
			// show error message after this catch block
		}
		LogService.getRoot().log(Level.INFO, () -> "could not find folder //" + repositoryName + path);
		return null;
	}

	/**
	 * Transform the list of new entries to legacy repository entries.
	 *
	 * @param entries to be transformed, either they are already {@link GeneralFile} instances created by the {@link
	 *                FileTypeHandler} or they become {@link BasicBinaryEntry}
	 * @param folder  containing the entries
	 * @return List of {@link DataEntry}
	 */
	protected static List<DataEntry> asLegacyEntries(Collection<GeneralFile> entries, BasicFolder folder) {
		List<DataEntry> result = new ArrayList<>(entries.size());
		for (GeneralFile entry : entries) {
			if (entry instanceof BasicEntry) {
				result.add((DataEntry) entry);
			} else {
				result.add(new BasicBinaryEntry(entry, folder));
			}
		}
		return result;
	}

	/**
	 * Transforms new {@link RepositoryFolder} instances to legacy {@link Folder} instances
	 */
	protected static List<Folder> asLegacyFolders(Collection<RepositoryFolder> folders, FilesystemRepositoryAdapter repository, BasicFolder parent, List<Folder> knownFolders) {
		final List<Folder> theKnownFolders;
		if (knownFolders == null) {
			theKnownFolders = Collections.emptyList();
		} else {
			theKnownFolders = Collections.unmodifiableList(knownFolders);
		}
		return folders.stream().map((RepositoryFolder folder) ->
				theKnownFolders.stream()
						.filter(chkFolder -> chkFolder.getName().equalsIgnoreCase(folder.getName()))
						.findFirst()
						.orElse(asLegacyFolder(folder, repository, parent)))
				.collect(Collectors.toList());
	}

	/**
	 * Transforms new {@link RepositoryFolder} instances to legacy {@link Folder} instances
	 */
	protected static BasicFolder asLegacyFolder(RepositoryFolder folder, FilesystemRepositoryAdapter repository, BasicFolder parent) {
		return new BasicFolder(folder, repository, parent);
	}
}
