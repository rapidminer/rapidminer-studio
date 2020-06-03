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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.event.EventListenerList;
import javax.swing.tree.TreeModel;

import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.RepositoryGuiTools;
import com.rapidminer.io.process.ProcessOriginProcessXMLFilter.ProcessOriginState;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.gui.RepositoryTreeModel;
import com.rapidminer.repository.internal.db.DBRepository;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.repository.resource.ResourceFolder;
import com.rapidminer.repository.resource.ResourceRepository;
import com.rapidminer.repository.search.RepositoryGlobalSearch;
import com.rapidminer.repository.versioned.FilesystemRepositoryAdapter;
import com.rapidminer.repository.versioned.FilesystemRepositoryFactory;
import com.rapidminer.repository.versioned.NewFilesystemRepository;
import com.rapidminer.repository.versioned.NewVersionedRepository;
import com.rapidminer.security.PluginSandboxPolicy;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.encryption.EncryptionProvider;
import com.rapidminer.tools.plugin.Plugin;


/**
 * Keeps static references to registered repositories and provides helper methods.
 *
 * Observers will be notified when repositories are added (with the repository passed as an argument
 * to the {@link Observer#update(com.rapidminer.tools.Observable, Object)} method and when they are
 * removed, in which case null is passed.
 *
 * @author Simon Fischer, Adrian Wilke
 *
 */
public class RepositoryManager extends AbstractObservable<Repository> {

	public static final String SAMPLE_REPOSITORY_NAME = "Samples";

	/** @since 9.0 */
	static final List<String> SPECIAL_RESOURCE_REPOSITORY_NAMES = new ArrayList<>();

	static {
		SPECIAL_RESOURCE_REPOSITORY_NAMES.add(SAMPLE_REPOSITORY_NAME);
	}

	/** @since 9.0.0 */
	private static final Map<String, ProcessOriginState> REPOSITORY_ORIGINS = new HashMap<>();

	private static final Logger LOGGER = LogService.getRoot();
	/** @since 9.0 */
	private static final String LOG_SAMPLES_PREFIX = "com.rapidminer.sample_repository.";
	/** @since 9.0 */
	private static final String LOG_IGNORE_FOLDER_PREFIX = LOG_SAMPLES_PREFIX + "ignore_folder.";
	/** @since 9.0 */
	private static final String LOG_REGISTER_ERROR_PREFIX = LOG_SAMPLES_PREFIX + "register_error.";

	private static RepositoryManager instance;
	private static final Object INSTANCE_LOCK = new Object();
	private static Repository sampleRepository;
	private static final Map<RepositoryAccessor, RepositoryManager> CACHED_MANAGERS = new HashMap<>();
	private static final List<RepositoryFactory> FACTORIES = new LinkedList<>();
	private static final int DEFAULT_TOTAL_PROGRESS = 100_000;
	/** @since 9.0 */
	private static final Map<String, String> extensionFolders = new HashMap<>();

	private static RepositoryProvider provider = new FileRepositoryProvider();

	// use copy-on-write to prevent modification exceptions on startup
	private final List<Repository> repositories = new CopyOnWriteArrayList<>();

	private final EventListenerList listeners = new EventListenerList();

	/** @since 9.3 */
	private final List<RepositoryFilter> repositoryFilters = new CopyOnWriteArrayList<>();

	/**
	 * listener which reacts on repository changes like renaming and sorts the list of repositories
	 */
	private final RepositoryListener repositoryListener = new RepositoryListener() {

		@Override
		public void folderRefreshed(Folder folder) {}

		@Override
		public void entryRemoved(Entry removedEntry, Folder parent, int oldIndex) {}

		@Override
		public void entryChanged(Entry entry) {
			if (entry instanceof Repository) {
				sortRepositories();
			}
		}

		@Override
		public void entryAdded(Entry newEntry, Folder parent) {}
	};

	/**
	 * Ordered types of {@link Repository}s
	 *
	 * @author Sabrina Kirstein
	 *
	 */
	public enum RepositoryType {

		/**
		 * The order of repository types is very important as it is used in the
		 * {@link RepositoryTools#REPOSITORY_COMPARATOR}
		 */
		RESOURCES, FILESYSTEM_NEW, VERSIONED_NEW, LOCAL, REMOTE, OTHER, DB;

		/**
		 * Returns the RepositoryType for a given repository to enable the ordering of repositories
		 *
		 * @param repo
		 *            given repository
		 * @return the related {@link RepositoryType}
		 */
		public static RepositoryType getRepositoryType(Repository repo) {
			if (repo instanceof ResourceRepository) {
				return RESOURCES;
			} else if (repo instanceof DBRepository) {
				return DB;
			} else if (repo instanceof NewVersionedRepository) {
				return VERSIONED_NEW;
			} else if (repo instanceof NewFilesystemRepository) {
				return FILESYSTEM_NEW;
			} else if (repo instanceof LocalRepository) {
				return LOCAL;
			} else if (repo instanceof ConnectionRepository && SPECIAL_RESOURCE_REPOSITORY_NAMES.contains(repo.getName())) {
				return RESOURCES;
			} else if (repo instanceof RemoteRepository) {
				return REMOTE;
			} else {
				return OTHER;
			}
		}
	}

	public static RepositoryManager getInstance(RepositoryAccessor repositoryAccessor) {
		synchronized (INSTANCE_LOCK) {
			if (instance == null) {
				init();
			}
			if (repositoryAccessor != null) {
				RepositoryManager manager = CACHED_MANAGERS.get(repositoryAccessor);
				if (manager == null) {
					manager = new RepositoryManager(instance);
					for (RepositoryFactory factory : FACTORIES) {
						for (Repository repos : factory.createRepositoriesFor(repositoryAccessor)) {
							manager.repositories.add(repos);
							repos.addRepositoryListener(instance.repositoryListener);
						}
						manager.sortRepositories();
					}
					CACHED_MANAGERS.put(repositoryAccessor, manager);
				}
				return manager;
			}
		}
		return instance;
	}

	private RepositoryManager(RepositoryManager cloned) {
		this.repositories.addAll(cloned.repositories);
		for (Repository repo : cloned.repositories) {
			repo.addRepositoryListener(repositoryListener);
		}
		sortRepositories();
	}

	private RepositoryManager() {
		if (sampleRepository == null) {
			sampleRepository = new ResourceRepository(SAMPLE_REPOSITORY_NAME, "samples") {

				@Override
				protected void ensureLoaded(List<Folder> folders, List<DataEntry> data) throws RepositoryException {
					super.ensureLoaded(folders, data);
					extensionFolders.forEach((n, e) -> {
						if (folders.stream().anyMatch(f -> f.getName().equals(n))) {
							LOGGER.log(Level.INFO, LOG_IGNORE_FOLDER_PREFIX + "already_present", n);
							return;
						}
						ResourceFolder folder = new ResourceFolder(this, n, "/" + n, this);
						try {
							if (folder.getDataEntries().isEmpty() && folder.getSubfolders().isEmpty()) {
								LOGGER.log(Level.INFO, LOG_IGNORE_FOLDER_PREFIX + "no_content", n);
								return;
							}
						} catch (RepositoryException re) {
							LOGGER.log(Level.INFO, LOG_IGNORE_FOLDER_PREFIX + "error", new Object[]{n, re.getMessage()});
							return;
						}
						folders.add(folder);
					});
				}
			};
		}
		repositories.add(sampleRepository);
		fireRepositoryWasAdded(sampleRepository);
		sortRepositories();


		// only load local repositories, custom repositories will be loaded after initialization
		load(LocalRepository.class);
	}

	@SuppressWarnings("unchecked")
	public static void init() {
		synchronized (INSTANCE_LOCK) {
			instance = new RepositoryManager();
			// initialize Repository Global Search
			if (!RapidMiner.getExecutionMode().isHeadless()) {
				new RepositoryGlobalSearch();
			}
			// initialize the content store mapper to enable it to update data for repo locations on rename
			PersistentContentMapperStore.INSTANCE.init();
			instance.postInstall();
			RapidMiner.registerCleanupHook(RepositoryManager::cleanup);
			if (!CustomRepositoryRegistry.INSTANCE.getClasses().contains(FilesystemRepositoryAdapter.class)) {
				try {
					CustomRepositoryRegistry.INSTANCE.register(new FilesystemRepositoryFactory());
				} catch (RepositoryException e) {
					LogService.getRoot().log(Level.SEVERE, "Not possible to register Filesystem Repository Factory", e);
				}
			}
		}
	}

	/**
	 * Initializes custom repositories registered by extensions. Will be called by
	 * {@link RapidMiner#init()} after {@link Plugin#initAll()} and {@link RepositoryManager#init()}
	 * .
	 */
	@SuppressWarnings("unchecked")
	public static void initCustomRepositories() {
		Set<Class<? extends Repository>> customRepoClasses = CustomRepositoryRegistry.INSTANCE.getClasses();

		// only call the load method if custom repositories have been found. Otherwise all
		// repositories would be loaded twice when providing an empty list.
		if (!customRepoClasses.isEmpty()) {
			instance.load(customRepoClasses.toArray(new Class[0]));
		}
	}

	/**
	 * Refreshes the sample repository. Will be called by
	 * {@link RapidMiner#init()} after {@link Plugin#initAll()} and {@link RepositoryManager#init()}
	 */
	public static void refreshSampleRepository() {
		try {
			sampleRepository.refresh();
		} catch (Exception e) {
			LOGGER.log(Level.INFO, "com.rapidminer.repository.RepositoryManager.sample_repository_refresh_failed", e);
		}
	}

	/**
	 * Replaces the used {@link RepositoryProvider}. The {@link FileRepositoryProvider} will be
	 * used as default.
	 *
	 * @param repositoryProvider
	 *            the new {@link RepositoryProvider}
	 */
	public static void setProvider(RepositoryProvider repositoryProvider) {
		provider = repositoryProvider;
	}

	private void postInstall() {
		for (Repository repository : getRepositories()) {
			repository.postInstall();

			// the original firings were during init phase of the RepositoryManager, nobody could have listened yet. Fire again
			fireRepositoryWasAdded(repository);
		}
	}

	public static void registerFactory(RepositoryFactory factory) {
		synchronized (INSTANCE_LOCK) {
			FACTORIES.add(factory);
		}
	}

	/**
	 * Adds the given listener to be notified of repository additions and removals.
	 *
	 * @param l
	 * 		the listener, must not be {@code null}
	 * @since 8.1
	 */
	public void addRepositoryManagerListener(RepositoryManagerListener l) {
		if (l == null) {
			throw new IllegalArgumentException("l must not be null!");
		}

		listeners.add(RepositoryManagerListener.class, l);
	}

	/**
	 * Removes the given listener.
	 *
	 * @param l
	 * 		the listener, must not be {@code null}
	 * @since 8.1
	 */
	public void removeRepositoryManagerListener(RepositoryManagerListener l) {
		if (l == null) {
			throw new IllegalArgumentException("l must not be null!");
		}

		listeners.remove(RepositoryManagerListener.class, l);
	}

	/**
	 * Registers a repository.
	 *
	 * @see #removeRepository(Repository)
	 */
	public void addRepository(Repository repository) {
		if (repositories.contains(repository)) {
			return;
		}
		LOGGER.config("Adding repository " + repository.getName());
		repositories.add(repository);
		repository.addRepositoryListener(repositoryListener);

		if (instance != null) {
			save();
		}
		sortRepositories();

		// observer is kept for legacy reasons
		fireUpdate(repository);
		// since 8.1, this is the new way to detect changes to the repository manager
		fireRepositoryWasAdded(repository);

		if (instance != null) {
			// we cannot call post install during init(). The reason is that
			// post install may access RepositoryManager.getInstance() which will be null and hence
			// trigger further recursive, endless calls to init()
			repository.postInstall();
			save();
		}
	}

	/**
	 * Add a repository as a special resource repository. It will be sorted to the end.
	 *
	 * <strong>Note:</strong> only signed extensions can call this method outside the core!
	 *
	 * @param repository
	 * 		the repository to add
	 * @see #addSpecialRepository(Repository, String, String)
	 * @since 9.0.0
	 */
	public void addSpecialRepository(Repository repository) {
		addSpecialRepository(repository, null, null);
	}

	/**
	 * Add a repository as a special resource repository. The ordering is determined by the {@code before} or {@code after}
	 * parameters. Only one should not be {@code null}. If both are {@code null} or not {@code null}, or if the referenced name can not be found, the new repository
	 * will simply be sorted to the end.
	 * <p>
	 * <strong>Note:</strong> only signed extensions can call this method outside the core!
	 *
	 * @param repository
	 * 		the repository to add
	 * @param before
	 * 		the name of the repository the new repository should be inserted in front of; can be {@code null}
	 * @param after
	 * 		the name of the repository the new repository should be inserted after; can be {@code null}
	 * @since 9.0.0
	 */
	public void addSpecialRepository(Repository repository, String before, String after) {
		try {
			// only signed extensions are allowed to add special repositories
			if (System.getSecurityManager() != null) {
				AccessController.checkPermission(new RuntimePermission(PluginSandboxPolicy.RAPIDMINER_INTERNAL_PERMISSION));
			}
		} catch (AccessControlException e) {
			return;
		}
		int insertionIndex = -1;
		if (before == null && after != null) {
			insertionIndex = SPECIAL_RESOURCE_REPOSITORY_NAMES.indexOf(after);
			// sort to end (-1) or after the actual position
			if (insertionIndex != -1) {
				insertionIndex++;
			}
		} else if (after == null && before != null) {
			// insert at that specific index; sorted to the end automatically if reference point not found
			insertionIndex = SPECIAL_RESOURCE_REPOSITORY_NAMES.indexOf(before);
		}
		if (insertionIndex == -1) {
			SPECIAL_RESOURCE_REPOSITORY_NAMES.add(repository.getName());
		} else {
			SPECIAL_RESOURCE_REPOSITORY_NAMES.add(insertionIndex, repository.getName());
		}
		addRepository(repository);
	}

	/**
	 * Add an {@link ProcessOriginState origin} to the given repository. It has to be a special repository added with
	 * {@link #addSpecialRepository(Repository, String, String)}.
	 *
	 * @param repository
	 * 		the special repository to add an origin to
	 * @param origin
	 * 		the origin state of the repository
	 * @since 9.0.0
	 */
	public void setSpecialRepositoryOrigin(Repository repository, ProcessOriginState origin) {
		try {
			// only signed extensions are allowed to add origins to special repositories
			if (System.getSecurityManager() != null) {
				AccessController.checkPermission(new RuntimePermission(PluginSandboxPolicy.RAPIDMINER_INTERNAL_PERMISSION));
			}
		} catch (AccessControlException e) {
			return;
		}
		// only special repos can have an origin, and null origins are not allowed
		String name = repository.getName();
		if (origin == null || !SPECIAL_RESOURCE_REPOSITORY_NAMES.contains(name)) {
			return;
		}
		REPOSITORY_ORIGINS.put(name, origin);
	}

	/**
	 * Returns the origin of the given repository if it was registered.
	 *
	 * @param repository
	 * 		the repository whose origin should be looked up
	 * @return the origin state of the repository or {@code null}
	 * @see #setSpecialRepositoryOrigin(Repository, ProcessOriginState)
	 * @since 9.0.0
	 */
	public ProcessOriginState getSpecialRepositoryOrigin(Repository repository) {
		return REPOSITORY_ORIGINS.get(repository.getName());
	}

	/**
	 * Removes a registered repository.
	 *
	 * @see #addRepository(Repository)
	 */
	public void removeRepository(Repository repository) {
		repository.preRemove();
		repository.removeRepositoryListener(repositoryListener);
		repositories.remove(repository);

		if (instance != null) {
			save();
		}

		// observer is kept for legacy reasons
		fireUpdate(null);
		// since 8.1, this is the new way to detect changes to the repository manager
		fireRepositoryWasRemoved(repository);
	}

	public List<Repository> getRepositories() {
		return Collections.unmodifiableList(repositories);
	}

	/**
	 * Returns the filtered, visible list of {@link Repository Repositories}
	 *
	 * @return a list of repositories where all filters were applied
	 */
	public List<Repository> getFilteredRepositories() {
		List<Repository> result = new ArrayList<>(repositories);
		// transient repos are never visible
		result.removeIf(Repository::isTransient);
		for (RepositoryFilter repositoryFilter : repositoryFilters) {
			try {
				result = repositoryFilter.filter(result);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "com.rapidminer.repository.RepositoryManager.filter_failure", e);
			}
		}
		return result;
	}

	/**
	 * Gets a registered ({@link #addRepository(Repository)} repository by
	 * {@link Repository#getName()}
	 */
	public Repository getRepository(String name) throws RepositoryException {
		for (Repository repos : repositories) {
			if (repos.getName().equals(name)) {
				return repos;
			}
		}
		throw new RepositoryException("Requested repository " + name + " does not exist.");
	}

	/** Gets a list of all registered repositories inheriting from {@link RemoteRepository}. */
	public List<RemoteRepository> getRemoteRepositories() {
		List<RemoteRepository> result = new LinkedList<>();
		for (Repository repos : getRepositories()) {
			if (repos instanceof RemoteRepository) {
				result.add((RemoteRepository) repos);
			}
		}
		return result;
	}

	/**
	 * Uses the specified {@link RepositoryProvider} to load the configuration. The default provider
	 * will load the settings from a XML file. Use {@link #setProvider(RepositoryProvider)} to
	 * replace this behavior.
	 *
	 * @param repoClasses
	 *            the implementations of {@link Repository} that should be loaded. If none are
	 *            provided all loaded classes are added.
	 * @since 6.5.0
	 * @see #save()
	 */
	@SafeVarargs
	public final void load(Class<? extends Repository>... repoClasses) {
		for (Repository repository : provider.load()) {
			// if no classes have been provided
			if (repoClasses.length == 0) {
				// add all found repositories
				addRepository(repository);
			} else {
				// otherwise add repositories of provided classes only
				for (Class<? extends Repository> repoClass : repoClasses) {
					if (repoClass.isAssignableFrom(repository.getClass())) {
						addRepository(repository);
						break;
					}
				}
			}

		}
	}

	public void createRepositoryIfNoneIsDefined() {
		String localRepositoryAlias = "Local Repository";
		boolean noLocalRepository = true;
		// check if we have at least one repository that is not pre-defined
		for (Repository repository : repositories) {
			if (repository instanceof NewFilesystemRepository || localRepositoryAlias.equals(repository.getName())) {
				noLocalRepository = false;
				break;
			}
		}
		if (noLocalRepository) {
			try {
				File dir = FileSystemService.getUserConfigFile("repositories");
				if (!dir.exists()) {
					dir.mkdir();
				}

				Path repositoryPath = dir.toPath().resolve(localRepositoryAlias);
				if(!repositoryPath.toFile().exists()) {
					repositoryPath.toFile().mkdir();
				}
				Repository defaultRepo = FilesystemRepositoryFactory.createRepository(localRepositoryAlias, repositoryPath, EncryptionProvider.DEFAULT_CONTEXT);
				defaultRepo.createFolder("data");
				defaultRepo.createFolder("processes");
			} catch (RepositoryException e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.repository.RepositoryManager.failed_to_create_default", e);
			}
		}
	}

	/**
	 * Uses the specified {@link RepositoryProvider} to save the configuration. The default provider
	 * will stores the settings to a XML file. Use {@link #setProvider(RepositoryProvider)} to
	 * replace this behavior.
	 *
	 * @see #load
	 */
	public void save() {
		provider.save(getRepositories());
		repositoryFilters.forEach(RepositoryFilter::save);
	}

	/** Stores an IOObject at the given location. Creates entries if they don't exist. */
	public IOObject store(IOObject ioobject, RepositoryLocation location, Operator callingOperator)
			throws RepositoryException {
		return store(ioobject, location, callingOperator, null);
	}

	/** Stores an IOObject at the given location. Creates entries if they don't exist. */
	public IOObject store(IOObject ioobject, RepositoryLocation location, Operator callingOperator,
						  ProgressListener progressListener) throws RepositoryException {
		ValidationUtil.requireNonNull(ioobject, "ioobject");
		DataEntry entry = location.locateData();
		Repository repository = location.getRepository();
		if (repository != null) {
			entry = repository.updateIncompatibleEntry(ioobject, entry);
		}
		boolean isConnectionEntry = entry instanceof ConnectionEntry;
		if (entry != null && isConnectionEntry != ioobject instanceof ConnectionInformationContainerIOObject) {
			String expectedType = isConnectionEntry ? "connection" : "data";
			throw new RepositoryEntryWrongTypeException(location, expectedType, entry.getType());
		}
		if (entry == null) {
			RepositoryLocation parentLocation = location.parent();
			if (parentLocation != null) {
				Folder parentFolder = getOrCreateParentFolder(location, parentLocation);
				String childName = location.getName();
				if (ioobject instanceof ConnectionInformationContainerIOObject) {
					parentFolder.createConnectionEntry(childName, ((ConnectionInformationContainerIOObject) ioobject).getConnectionInformation());
				} else {
					parentFolder.createIOObjectEntry(childName, ioobject, callingOperator, progressListener);
				}
				return ioobject;
			} else {
				throw new RepositoryEntryNotFoundException(location);
			}
		} else if (isConnectionEntry) {
			((ConnectionEntry) entry).storeConnectionInformation(((ConnectionInformationContainerIOObject) ioobject).getConnectionInformation());
			return ioobject;
		} else if (entry instanceof IOObjectEntry) {
			((IOObjectEntry) entry).storeData(ioobject, callingOperator, progressListener);
			return ioobject;
		} else {
			throw new RepositoryEntryWrongTypeException(location, IOObjectEntry.TYPE_NAME, entry.getType());
		}
	}

	/**
	 * Gets the referenced {@link BinaryEntry}. Creates a new one if it does not exist.
	 *
	 * @param location the location, must not be {@code null}
	 * @return the entry, never {@code null}
	 * @throws RepositoryException if the location does not support creating binary entries or something goes wrong
	 * @since 9.7
	 */
	public BinaryEntry getOrCreateBinaryEntry(RepositoryLocation location) throws RepositoryException {
		DataEntry entry = location.locateData();
		if (entry == null) {
			RepositoryLocation parentLocation = location.parent();
			if (parentLocation != null) {
				Folder parentFolder = getOrCreateParentFolder(location, parentLocation);
				String childName = location.getName();
				return parentFolder.createBinaryEntry(childName);
			} else {
				throw new RepositoryEntryNotFoundException(location);
			}
		} else if (entry instanceof BinaryEntry) {
			return (BinaryEntry) entry;
		} else {
			throw new RepositoryEntryWrongTypeException(location, null, entry.getType());
		}
	}

	/**
	 * Gets the referenced blob entry. Creates a new one if it does not exist.
	 *
	 * @deprecated since 9.7 blob entry creation is only supported for legacy repositories (Local and AI Hub)!
	 * Use {@link #getOrCreateBinaryEntry(RepositoryLocation)} for new repositories
	 */
	@Deprecated
	public BlobEntry getOrCreateBlob(RepositoryLocation location) throws RepositoryException {
		DataEntry entry = location.locateData();
		if (entry == null) {
			RepositoryLocation parentLocation = location.parent();
			if (parentLocation != null) {
				Folder parentFolder = getOrCreateParentFolder(location, parentLocation);
				String childName = location.getName();
				return parentFolder.createBlobEntry(childName);
			} else {
				throw new RepositoryEntryNotFoundException(location);
			}
		} else if (entry instanceof BlobEntry) {
			return (BlobEntry) entry;
		} else {
			throw new RepositoryEntryWrongTypeException(location, BlobEntry.TYPE_NAME, entry.getType());
		}
	}

	/** Saves the configuration file. */
	public static void shutdown() {
		if (instance != null) {
			instance.save();
		}
	}

	/**
	 * Copies an entry to a given destination folder.
	 *
	 * @param source the source location. Make sure it has a proper {@link RepositoryLocation#getLocationType()} set!
	 * @throws RepositoryException if something goes wrong
	 */
	public void copy(RepositoryLocation source, Folder destination, ProgressListener listener) throws RepositoryException {
		copy(source, destination, null, listener);
	}

	/**
	 * Copies an entry to a given destination folder with the name newName. If newName is null the old name will be
	 * kept. If an entry named newName exists, newName will be changed and not overwritten.
	 *
	 * @param source the source location. Make sure it has a proper {@link RepositoryLocation#getLocationType()} set!
	 * @throws RepositoryException if something goes wrong
	 */
	public void copy(RepositoryLocation source, Folder destination, String newName, ProgressListener listener)
			throws RepositoryException {
		copy(source, destination, newName, false, listener);
	}

	/**
	 * Copies an entry to a given destination folder with the name newName. If newName is null the old name will be
	 * kept.
	 *
	 * @param source the source location. Make sure it has a proper {@link RepositoryLocation#getLocationType()} set!
	 * @throws RepositoryException if something goes wrong
	 */
	public void copy(RepositoryLocation source, Folder destination, String newName, boolean overwriteIfExists,
					 ProgressListener listener) throws RepositoryException {
		if (listener != null) {
			listener.setTotal(DEFAULT_TOTAL_PROGRESS);
			listener.setCompleted(0);
		}
		try {
			copy(source, destination, newName, overwriteIfExists, listener, 0, DEFAULT_TOTAL_PROGRESS);
		} finally {
			if (listener != null) {
				listener.complete();
			}
		}
	}

	private void copy(RepositoryLocation source, Folder destination, String newName, boolean overwriteIfExists,
					  ProgressListener listener, int minProgress, int maxProgress) throws RepositoryException {
		boolean isFolder;
		Entry entry;
		switch (source.getLocationType()) {
			case DATA_ENTRY:
				entry = source.locateData();
				isFolder = false;
				break;
			case FOLDER:
				entry = source.locateFolder();
				isFolder = true;
				break;
			case UNKNOWN:
			default:
				isFolder = false;
				entry = source.locateData();
				if (entry == null) {
					entry = source.locateFolder();
					isFolder = true;
				}
				break;
		}

		if (entry == null) {
			throw new RepositoryException("No such " + (isFolder ? "folder" : "entry") + ": " + source);
		}

		copy(entry, destination, newName, overwriteIfExists, listener, minProgress, maxProgress);
	}

	private void copy(Entry entry, Folder destination, String newName, boolean overwriteIfExists, ProgressListener listener,
					  int minProgress, int maxProgress) throws RepositoryException {
		if (listener != null) {
			listener.setMessage(entry.getName());
		}

		if (newName == null) {
			newName = entry.getName();
		}

		boolean isFolder = entry instanceof Folder;

		// when copying files between repos, their entry type may change (from generic IOObject to a more specific subtype)
		Class<? extends DataEntry> targetClass = !isFolder ? ((DataEntry) entry).getClass() : null;
		boolean destinationContainsEntry = isFolder ? destination.containsFolder(newName) : destination.containsData(newName, targetClass);
		if (destinationContainsEntry) {
			if (overwriteIfExists) {
				if (destination.equals(entry.getContainingFolder())) {
					// Do not overwrite file, if source and target folders are the same
					return;
				} else {
					if (isFolder) {
						List<Folder> folders = destination.getSubfolders();
						for (Folder folderToDelete : folders) {
							if (folderToDelete.getName().equals(newName)) {
								folderToDelete.delete();
								break;
							}
						}
					} else {
						List<DataEntry> dataEntries = destination.getDataEntries();
						for (DataEntry dataToDelete : dataEntries) {
							if (dataToDelete.getName().equals(newName)) {
								dataToDelete.delete();
								break;
							}
						}
					}
				}
			} else {
				if (isFolder) {
					newName = getNewNameForExistingFolder(destination, newName);
				} else {
					boolean keepSuffix = entry instanceof BinaryEntry || (entry instanceof BlobEntry && destination.isSupportingBinaryEntries());
					newName = getNewNameForExistingData(destination, newName, keepSuffix);
				}
			}
		}

		if (isFolder) {
			Folder folder = (Folder) entry;
			Folder connectionFolder = RepositoryTools.getConnectionFolder(folder.getLocation().getRepository());
			if (connectionFolder != null && connectionFolder.getLocation().getAbsoluteLocation().equals(folder.getLocation().getAbsoluteLocation())) {
				throw new RepositoryConnectionsFolderImmutableException(Folder.MESSAGE_CONNECTION_FOLDER_CHANGE);
			}
			String sourceAbsolutePath = folder.getLocation().getAbsoluteLocation();
			String destinationAbsolutePath = destination.getLocation().getAbsoluteLocation();
			// make sure same folder moves are forbidden
			if (sourceAbsolutePath.equals(destinationAbsolutePath)) {
				throw new RepositoryException(
						I18N.getMessage(I18N.getErrorBundle(), "repository.repository_copy_same_folder"));
			}
			// make sure moving parent folder into subfolder is forbidden
			if (destinationAbsolutePath.contains(sourceAbsolutePath)) {
				throw new RepositoryException(
						I18N.getMessage(I18N.getErrorBundle(), "repository.repository_copy_into_subfolder"));
			}
			Folder destinationFolder = destination.createFolder(newName);
			List<Entry> allChildren = new LinkedList<>();
			allChildren.addAll(folder.getSubfolders());
			allChildren.addAll(folder.getDataEntries());
			final int count = allChildren.size();
			int progressStart = minProgress;
			int progressDiff = maxProgress - minProgress;
			int i = 0;
			for (Entry child : allChildren) {
				copy(child, destinationFolder, null, false, listener, progressStart + i * progressDiff / count,
						progressStart + (i + 1) * progressDiff / count);
				i++;
			}
		} else if (entry instanceof ProcessEntry) {
			ProcessEntry pe = (ProcessEntry) entry;
			String xml = pe.retrieveXML();
			if (listener != null) {
				listener.setCompleted((minProgress + maxProgress) / 2);
			}

			// if we copy between repositories with different encryption contexts, we need to create the process, then store it again
			// if we did not do this here, we could end up with a process XML encrypted with the local key in a versioned repository, unable to be read by colleagues
			// this means that copy will not work if extensions used are not available - dummy operators must cause a failure, otherwise the process is stored in a broken state
			String sourceEncryptionContext = entry.getLocation().getRepository().getEncryptionContext();
			String targetEncryptionContext = destination.getLocation().getRepository().getEncryptionContext();
			if (!Objects.equals(sourceEncryptionContext, targetEncryptionContext)) {
				try {
					Process p = new Process(xml, sourceEncryptionContext);
					xml = p.getRootOperator().getXML(false, targetEncryptionContext);
					Process validatingProcess = new Process(xml, targetEncryptionContext);
					try {
						Process.checkIfSavable(validatingProcess);
					} catch (Exception e) {
						throw new RepositoryException("Cannot copy process: " + e.getMessage());
					}
				} catch (IOException | XMLException e) {
					throw new RepositoryException("Cannot copy process because it could not be created from " + newName, e);
				}
			}

			destination.createProcessEntry(newName, xml);
			if (listener != null) {
				listener.setCompleted(maxProgress);
			}
		} else if (entry instanceof IOObjectEntry) {
			IOObjectEntry iooe = (IOObjectEntry) entry;
			IOObject original = iooe.retrieveData(null);
			if (listener != null) {
				listener.setCompleted((minProgress + maxProgress) / 2);
			}
			if (original instanceof ConnectionInformationContainerIOObject) {
				destination.createConnectionEntry(newName, ((ConnectionInformationContainerIOObject) original).getConnectionInformation());
			} else {
				destination.createIOObjectEntry(newName, original, null, null);
			}
			if (listener != null) {
				listener.setCompleted(maxProgress);
			}
		} else if (entry instanceof BlobEntry) {
			BlobEntry blob = (BlobEntry) entry;
			if (destination.isSupportingBinaryEntries()) {
				// support copying from legacy repo blob format to new repo binary entry (it's a bytestream anyway)
				// obviously, the new entry may not have a file suffix, that is for the user to fix via rename
				BinaryEntry targetEntry = destination.createBinaryEntry(newName);
				try (InputStream in = blob.openInputStream(); OutputStream out = targetEntry.openOutputStream()) {
					Tools.copyStreamSynchronously(in, out, false);
					if (listener != null) {
						listener.setCompleted(maxProgress);
					}
				} catch (IOException e) {
					destination.refresh();
					throw new RepositoryException(e);
				}
			} else {
				BlobEntry target = destination.createBlobEntry(newName);
				String mimeType = blob.getMimeType();
				try (InputStream in = blob.openInputStream(); OutputStream out = target.openOutputStream(mimeType)) {
					Tools.copyStreamSynchronously(in, out, false);
					if (listener != null) {
						listener.setCompleted(maxProgress);
					}
				} catch (IOException e) {
					destination.refresh();
					throw new RepositoryException(e);
				}
			}
		} else if (entry instanceof BinaryEntry && destination.getLocation().getRepository().isSupportingBinaryEntries()) {
			BinaryEntry binEntry = (BinaryEntry) entry;
			BinaryEntry targetEntry = destination.createBinaryEntry(newName);
			try (InputStream in = binEntry.openInputStream(); OutputStream out = targetEntry.openOutputStream()) {
				Tools.copyStreamSynchronously(in, out, false);
				if (listener != null) {
					listener.setCompleted(maxProgress);
				}
			} catch (IOException e) {
				destination.refresh();
				throw new RepositoryException(e);
			}
		} else {
			throw new RepositoryException("Cannot copy entry of type " + entry.getType());
		}
	}

	/**
	 * Moves an entry to a given destination folder.
	 *
	 * @param source the source location. Make sure it has a proper {@link RepositoryLocation#getLocationType()} set!
	 * @throws RepositoryException if something goes wrong
	 */
	public void move(RepositoryLocation source, Folder destination, ProgressListener listener) throws RepositoryException {
		move(source, destination, null, listener);
	}

	/**
	 * Moves an entry to a given destination folder with the name newName.
	 *
	 * @param source the source location. Make sure it has a proper {@link RepositoryLocation#getLocationType()} set!
	 * @throws RepositoryException if something goes wrong
	 */
	public void move(RepositoryLocation source, Folder destination, String newName, ProgressListener listener) throws RepositoryException {
		// Default: Overwrite existing entry
		move(source, destination, newName, true, listener);
	}

	/**
	 * Moves an entry to a given destination folder with the name newName.
	 *
	 * @param source the source location. Make sure it has a proper {@link RepositoryLocation#getLocationType()} set!
	 * @throws RepositoryException if something goes wrong
	 */
	public void move(RepositoryLocation source, Folder destination, String newName, boolean overwriteIfExists,
					 ProgressListener listener) throws RepositoryException {
		Boolean isFolder;
		Entry entry;
		switch (source.getLocationType()) {
			case DATA_ENTRY:
				entry = source.locateData();
				isFolder = false;
				break;
			case FOLDER:
				entry = source.locateFolder();
				isFolder = true;
				break;
			case UNKNOWN:
			default:
				isFolder = null;
				entry = source.locateData();
				if (entry == null) {
					entry = source.locateFolder();
				}
				break;
		}

		if (entry == null) {
			if (isFolder == null) {
				throw new RepositoryException("No such entry or folder: " + source);
			} else {
				throw new RepositoryException("No such " + (isFolder ? "folder" : "entry") + ": " + source);
			}
		} else {
			isFolder = entry instanceof Folder;
		}

		String sourceAbsolutePath = source.getAbsoluteLocation();
		String destinationAbsolutePath;
		RepositoryLocation destLocation = destination.getLocation();
		if (!(entry instanceof Folder)) {
			destinationAbsolutePath = destLocation.getAbsoluteLocation() + RepositoryLocation.SEPARATOR
					+ newName;
		} else {
			destinationAbsolutePath = destLocation.getAbsoluteLocation();
		}
		// make sure same folder moves are forbidden
		if (sourceAbsolutePath.equals(destinationAbsolutePath)) {
			throw new RepositoryException(
					I18N.getMessage(I18N.getErrorBundle(), "repository.repository_move_same_folder"));
		}
		// make sure moving parent folder into subfolder is forbidden
		if (source.getLocationType() == destLocation.getLocationType() && RepositoryGuiTools.isSuccessor(sourceAbsolutePath, destinationAbsolutePath)) {
			throw new RepositoryException(
					I18N.getMessage(I18N.getErrorBundle(), "repository.repository_move_into_subfolder"));
		}
		if (destLocation.getRepository() != source.getRepository()) {
			copy(source, destination, newName, overwriteIfExists, listener);
			entry.delete();
		} else {
			String effectiveNewName = newName != null ? newName : entry.getName();
			Entry toDeleteEntry = null;
			boolean destinationContainsEntry = isFolder ? destination.containsFolder(effectiveNewName) :
					destination.containsData(effectiveNewName, ((DataEntry) entry).getClass());
			if (destinationContainsEntry) {
				if (overwriteIfExists) {
					if (isFolder) {
						for (Folder folderEntry : destination.getSubfolders()) {
							if (folderEntry.getName().equals(effectiveNewName)) {
								toDeleteEntry = folderEntry;
							}
						}
					} else {
						for (DataEntry dataEntry : destination.getDataEntries()) {
							if (dataEntry.getName().equals(effectiveNewName)) {
								toDeleteEntry = dataEntry;
							}
						}
					}

					if (toDeleteEntry != null) {
						toDeleteEntry.delete();
					}
				} else {
					if (isFolder) {
						newName = getNewNameForExistingFolder(destination, effectiveNewName);
					} else {
						boolean keepSuffix = entry instanceof BinaryEntry || (entry instanceof BlobEntry && destination.isSupportingBinaryEntries());
						newName = getNewNameForExistingData(destination, effectiveNewName, keepSuffix);
					}
				}
			}
			if (listener != null) {
				listener.setTotal(100);
				listener.setCompleted(10);
			}
			if (newName == null) {
				entry.move(destination);
			} else {
				entry.move(destination, newName);
			}
			if (listener != null) {
				listener.setCompleted(100);
				listener.complete();
			}
		}
	}

	/**
	 * Looks up the {@link Folder} with the given path in the given repository. This method will return {@code null}
	 * when it finds a folder that blocks (has not yet loaded all its data) AND failIfBlocks is {@code true}.
	 *
	 * @param repository   the repository to locate the folder in
	 * @param path         the relative path within the repository to the folder
	 * @param failIfBlocks if {@code true}, will return {@code null} when the folder will block and does not know about
	 *                     the subfolder
	 * @return the folder if it exists, or {@code null} if it cannot be found or the failIfBlocks case is encountered
	 * @throws RepositoryException if something goes wrong
	 * @since 9.7
	 */
	public Folder locateFolder(Repository repository, String path, boolean failIfBlocks) throws RepositoryException {
		if (path.startsWith("" + RepositoryLocation.SEPARATOR)) {
			path = path.substring(1);
		}
		if (path.trim().isEmpty()) {
			return repository;
		}
		String[] pathArray = path.split("" + RepositoryLocation.SEPARATOR);
		Folder folder = repository;
		int index = 0;
		while (true) {
			if (failIfBlocks && folder.willBlock()) {
				// special case mentioned in JD above, directly return null here
				return null;
			}

			boolean isLastPathElement = index == pathArray.length - 1;
			int retryCount = 0;
			retryLoop: while (retryCount <= 1) {
				for (Folder subfolder : folder.getSubfolders()) {
					if (subfolder.getName().equals(pathArray[index])) {
						if (isLastPathElement) {
							// last element in path, we found the folder, return it
							return subfolder;
						} else {
							// not last element, but found folder on our way down the path, can proceed to next path element
							folder = subfolder;
							break retryLoop;
						}
					}
				}

				// folder not found -> refresh and try again
				if (retryCount == 0 && folder.canRefreshChildFolder(pathArray[index])) {
					folder.refresh();
				} else {
					// no point in retrying, abort completely and return null
					// we have not found the element (either mid-path or last path), we can abort as we are not going to find it
					return null;
				}
				retryCount++;
			}

			index++;
		}
	}

	/**
	 * Looks up the {@link DataEntry} with the given path in the given repository. This method will return {@code null}
	 * when it finds a folder that blocks (has not yet loaded all its data) AND failIfBlocks is {@code true}.
	 * <p>
	 * Note: If multiple data entries of different types but with the same name (prefix) exist, will return the first
	 * one to be found if the expected type is not specified! This is a situation that usually cannot occur, except for
	 * versioned projects introduced in 9.7. There, a user can create e.g. a data table "test.rmhdf5table", and another
	 * user a model "test.ioo". That would not be a conflict for Git, but causes our repository (which only shows the
	 * prefixes for known types) to have 2 entries which could be found when locating "test".
	 * </p>
	 *
	 * @param repository                    the repository to locate the data in
	 * @param path                          the relative path within the repository to the data
	 * @param expectedDataType              the expected specific {@link DataEntry} (sub-)type. At the same repository
	 *                                      location, for example a "test.rmhdf5table" (example set) and "test.rmp"
	 *                                      (process) might live, and if the expected data entry subtype is not
	 *                                      specified, this method will return the first one it finds. Must not be
	 *                                      {@code null}! Also see {@link RepositoryLocation#locateData()}.
	 * @param failIfBlocks                  if {@code true}, will return {@code null} when the folder will block and
	 *                                      does not know about the data
	 * @param failIfDuplicateIOObjectExists if {@code true} and the expected data type is of {@link IOObjectEntry}, it
	 *                                      will check that the repository folder contains only a single {@link
	 *                                      IOObjectEntry} with the requested name (prefix). Otherwise it will throw a
	 *                                      {@link RepositoryIOObjectEntryDuplicateFoundException}. See {@link
	 *                                      RepositoryLocation#locateData()} for more information.
	 * @return the data if it exists, or {@code null} if it cannot be found or the failIfBlocks case is encountered
	 * @throws RepositoryIOObjectEntryDuplicateFoundException if the expectedDataType is of type {@link IOObjectEntry}
	 *                                                        AND more than one exists with the same name (prefix) AND
	 *                                                        failIfDuplicateIOObjectExists is {@code true}
	 * @throws RepositoryException                            if something else goes wrong
	 * @since 9.7
	 */
	public <T extends DataEntry> T locateData(Repository repository, String path, Class<T> expectedDataType, boolean failIfBlocks, boolean failIfDuplicateIOObjectExists) throws RepositoryException {
		if (path.startsWith("" + RepositoryLocation.SEPARATOR)) {
			path = path.substring(1);
		}
		if (path.trim().isEmpty()) {
			return null;
		}
		ValidationUtil.requireNonNull(expectedDataType, "expectedDataType");
		String[] pathArray = path.split("" + RepositoryLocation.SEPARATOR);
		if (Arrays.stream(pathArray).anyMatch(p -> p == null || p.isEmpty())) {
			throw new RepositoryException("Path did contain empty elements, probably two or more '" + RepositoryLocation.SEPARATOR + "' were part of the path");
		}
		String parentFolderPath = Arrays.stream(pathArray).limit(pathArray.length - 1).collect(Collectors.joining(String.valueOf(RepositoryLocation.SEPARATOR)));
		Folder parentFolder = locateFolder(repository, parentFolderPath, failIfBlocks);
		if (parentFolder == null) {
			return null;
		}

		String dataName = pathArray[pathArray.length - 1];
		T candidate = findData(repository, parentFolder, dataName, expectedDataType, failIfDuplicateIOObjectExists, path);
		if (candidate != null) {
			return candidate;
		}

		// not found yet
		if (failIfBlocks && parentFolder.willBlock()) {
			// special case mentioned in JD above, directly return null here
			return null;
		}
		if (parentFolder.canRefreshChildData(dataName)) {
			// can refresh, do it!
			parentFolder.refresh();
		}
		// try again

		// either we found something or not, return now
		return findData(repository, parentFolder, dataName, expectedDataType, failIfDuplicateIOObjectExists, path);
	}

	/**
	 * Tries to find the specified data.
	 *
	 * @param repository                    the repository to locate the data in
	 * @param parentFolder                  the folder to look for the data in
	 * @param dataName                      the name of the data entry
	 * @param expectedDataType              the expected specific {@link DataEntry} (sub-)type
	 * @param failIfDuplicateIOObjectExists if {@code true} and the expected data type is of {@link IOObjectEntry}, it
	 *                                      will check that the repository folder contains only a single {@link
	 *                                      IOObjectEntry} with the requested name (prefix). Otherwise it will throw a
	 *                                      {@link RepositoryIOObjectEntryDuplicateFoundException}. See {@link
	 *                                      RepositoryLocation#locateData()} for more information.
	 * @param repoPath                      the relative path within the repository for exception details
	 * @return the data or {@code null} if it could not be found
	 * @throws RepositoryException if something goes wrong
	 */
	private <T extends DataEntry> T findData(Repository repository, Folder parentFolder, String dataName, Class<T> expectedDataType, boolean failIfDuplicateIOObjectExists, String repoPath) throws RepositoryException {
		boolean isIOObjectEntry = expectedDataType != ConnectionEntry.class && IOObjectEntry.class.isAssignableFrom(expectedDataType);
		T candidate = null;
		for (DataEntry data : parentFolder.getDataEntries()) {
			if (data.getName().equals(dataName)) {
				// we have a name hit, now see if that is good enough:

				// not an IOObject:
				if (!isIOObjectEntry) {
					if (expectedDataType.isAssignableFrom(data.getClass())) {
						// only one version of non IOObjects, so we can just return the first hit
						return expectedDataType.cast(data);
					} else {
						//only one version of non IOObjects, no hit, try next data entry
						continue;
					}
				}

				// it is an IOObject:
				// we have to catch the case that since version 9.7, it is possible to end up in a situation where
				// multiple IOObjects exist right next to each other that have the same prefix, but different suffixes (test.ioo, test.rmhdf5table, ...)
				// this is NOT a good scenario because it is impossible to figure out the desired specific type in some cases (think through ports of operators)
				// therefore, we throw immediately whenever this becomes the case and a duplicate is found, to alert the user so he can fix it
				if (expectedDataType.isAssignableFrom(data.getClass())) {
					// we have a hit, behavior now depends if we need to fail in case of duplicates:

					if (candidate != null) {
						// we already found one (candidate would not be set if fail on duplicate was false), now another hit -> throw duplicate exception
						throw new RepositoryIOObjectEntryDuplicateFoundException(RepositoryLocation.REPOSITORY_PREFIX + repository.getName() + RepositoryLocation.SEPARATOR + repoPath);
					}
					candidate = expectedDataType.cast(data);
					if (!failIfDuplicateIOObjectExists) {
						// don't care about potential duplicates, so we can just return the first hit now
						return expectedDataType.cast(data);
					}
				}
			}
		}

		// we either have exactly one candidate now (aka no duplicates), or we have no hit at all. Both fine, return now
		return candidate;
	}

	/**
	 * Looks up the entry with the given path in the given repository. This method will return null when it finds a
	 * folder that blocks (has not yet loaded all its data) AND failIfBlocks is true.
	 * <p>
	 * This method can be used as a first approach to locate an entry and fall back to a more expensive solution when
	 * this fails.
	 *
	 * @deprecated since 9.7, because it cannot distinguish between folders and files. Use {@link
	 * #locateFolder(Repository, String, boolean)} or {@link #locateData(Repository, String, Class, boolean, boolean)} instead!
	 */
	@Deprecated
	public Entry locate(Repository repository, String path, boolean failIfBlocks) throws RepositoryException {
		if (path.startsWith("" + RepositoryLocation.SEPARATOR)) {
			path = path.substring(1);
		}
		if (path.trim().isEmpty()) {
			return repository;
		}
		String[] splitted = path.split("" + RepositoryLocation.SEPARATOR);
		Folder folder = repository;
		int index = 0;
		while (true) {
			if (failIfBlocks && folder.willBlock()) {
				return null;
			}
			if (index == splitted.length - 1) {
				int retryCount = 0;
				while (retryCount <= 1) {
					List<Entry> all = new LinkedList<>();
					all.addAll(folder.getSubfolders());
					all.addAll(folder.getDataEntries());
					for (Entry child : all) {
						if (child.getName().equals(splitted[index])) {
							return child;
						}
					}
					// missed entry -> refresh and try again
					if (retryCount == 0 && folder.canRefreshChild(splitted[index])) {
						folder.refresh();
					} else {
						break;
					}
					retryCount++;
				}

				return null;
			} else {
				int retryCount = 0;
				boolean found = false;
				while (retryCount <= 1) {
					for (Folder subfolder : folder.getSubfolders()) {
						if (subfolder.getName().equals(splitted[index])) {
							folder = subfolder;
							found = true;
							break;
						}
					}
					if (found) {
						// found in 1st round
						break;
					} else {
						// missed entry -> refresh and try again
						if (retryCount == 0 && folder.canRefreshChild(splitted[index])) {
							folder.refresh();
						} else {
							break;
						}
						retryCount++;
					}

				}
				if (!found) {
					return null;
				}
			}
			index++;
		}
	}

	/** Returns the repository containing the RapidMiner sample processes. */
	public Repository getSampleRepository() {
		return sampleRepository;
	}

	/**
	 * Registers the given folder name for the specified extension ID in the samples folder.
	 * The folder name has to be a directory in the extensions {@code com.rapidminer.resources.samples} resource package
	 * and must not contain any "/" or "\". Also it must abide by the general {@link ResourceFolder} rules, i.e there must
	 * be a {@code CONTENTS} file listing the valid contents. The registered folder name is ignored if it is empty or could
	 * not be properly read.
	 * <p>
	 * <strong>Note:</strong> Make sure that {@link #refreshSampleRepository()} is called after registering a new sample folder.
	 * This is not necessary for extensions that add folders during initialization.
	 *
	 * @param extensionID
	 * 		the ID of the registering extension
	 * @param folderName
	 * 		the folder name to be registered
	 * @see #unregisterExtensionSamples(String, String)
	 * @since 9.0
	 */
	public static void registerExtensionSamples(String extensionID, String folderName) {
		if (folderName.contains("/") || folderName.contains("\\")) {
			LOGGER.log(Level.WARNING, LOG_REGISTER_ERROR_PREFIX + "file_separator", folderName);
			return;
		}
		if (extensionFolders.containsKey(folderName)) {
			LOGGER.log(Level.WARNING, LOG_REGISTER_ERROR_PREFIX + "already_registered", folderName);
			return;
		}
		extensionFolders.put(folderName, extensionID);
		LOGGER.log(Level.INFO, LOG_SAMPLES_PREFIX + "register.success", folderName);
	}

	/**
	 * Unregisters the given folder name from the samples folder. This will not succeed if the calling extension is not
	 * the same as the registering extension.
	 * <p>
	 * <strong>Note:</strong> Make sure that {@link #refreshSampleRepository()} is called after unregistering a sample folder.
	 *
	 * @param extensionID
	 * 		the ID of the registering extension
	 * @param folderName
	 * 		the folder name to be unregistered
	 * @see #registerExtensionSamples(String, String)
	 * @since 9.0
	 */
	public static void unregisterExtensionSamples(String extensionID, String folderName) {
		String registeredExtension = extensionFolders.get(folderName);
		if (registeredExtension == null) {
			return;
		}
		if (!registeredExtension.equals(extensionID)) {
			LOGGER.log(Level.WARNING, LOG_SAMPLES_PREFIX + "unregister_error.other_extension", folderName);
			return;
		}
		extensionFolders.remove(folderName);
		LOGGER.log(Level.INFO, LOG_SAMPLES_PREFIX + "unregister.success", folderName);
	}

	/**
	 * Visitor pattern for repositories. Callbacks to the visitor will be made only for matching
	 * types. (Recursion happens also if the type is not a Folder.
	 *
	 * @throws RepositoryException
	 */
	public <T extends Entry> void walk(Entry start, RepositoryVisitor<T> visitor, Class<T> visitedType)
			throws RepositoryException {
		boolean continueChildren = true;
		if (visitedType.isInstance(start)) {
			continueChildren &= visitor.visit(visitedType.cast(start));
		}
		if (continueChildren && start instanceof Folder) {
			Folder folder = (Folder) start;
			for (Entry child : folder.getDataEntries()) {
				walk(child, visitor, visitedType);
			}
			for (Folder childFolder : folder.getSubfolders()) {
				walk(childFolder, visitor, visitedType);
			}
		}
	}

	/**
	 * Add a filter to hide parts of the {@link Repository}
	 *
	 * @param filter
	 * 		a custom filter
	 * @since 9.3
	 */
	public void registerRepositoryFilter(RepositoryFilter filter) {
		if (!RapidMiner.getExecutionMode().isHeadless() && filter != null) {
			repositoryFilters.add(filter);
			filter.notificationCallback(() -> {
				TreeModel treeModel = RapidMinerGUI.getMainFrame().getRepositoryBrowser().getRepositoryTree().getModel();
				if (treeModel instanceof RepositoryTreeModel) {
					((RepositoryTreeModel) treeModel).notifyTreeStructureChanged();
				}
			});
		}
	}

	/**
	 * Remove a previously registered filter
	 *
	 * @param filter
	 * 		to be removed
	 * @since 9.3
	 */
	public void unregisterRepositoryFilter(RepositoryFilter filter) {
		repositoryFilters.remove(filter);
	}

	/**
	 * Due to the filters the results for an action that wants to highlight a repository entry may be hidden.
	 * The {@link com.rapidminer.repository.gui.RepositoryTree} will ask this manager to unhide a repository name.
	 *
	 * @param repositoryName
	 * 		the name of the repository to show
	 * @since 9.3
	 */
	public void unhide(String repositoryName) {
		Repository repository;
		try {
			repository = getRepository(repositoryName);
		} catch (RepositoryException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.RepositoryManager.repository_does_not_exist", e);
			return;
		}
		if (repository != null && !getFilteredRepositories().contains(repository)) {
			List<Repository> singleRepoList = Collections.singletonList(repository);
			// reset filters that hide the requested repository
			repositoryFilters.stream().filter(filter -> filter.filter(singleRepoList).isEmpty()).forEach(filter -> {
				try {
					filter.reset();
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "com.rapidminer.repository.RepositoryManager.filter_failure", e);
				}
			});
		}
	}

	/**
	 * Generates new folder name for copies
	 *
	 * @param destination  The folder, to which the folder has to be copied
	 * @param originalName The name of the folder, which has to be copied
	 * @return A new, not used folder name
	 * @throws RepositoryException if something goes wrong
	 */
	public String getNewNameForExistingFolder(Folder destination, String originalName) throws RepositoryException {
		String oldName;
		oldName = originalName;
		String newName;
		int i = 2;
		do {
			newName = oldName + " - " + i++;
		} while (destination.containsFolder(newName));
		return newName;
	}

	/**
	 * Generates new entry name for copies.
	 * <p>Note: This method will treat all duplicate prefixes as collision, meaning if you have a process called "test"
	 * in the folder, passing "test" will result in the "test - 2" new name, regardless of any suffix.</p>
	 *
	 * @param destination  The folder, to which the entry has to be copied
	 * @param originalName The name of the entry, which has to be copied
	 * @param keepSuffix   if {@code true}, a suffix (defined as last dot followed by characters) is kept
	 * @return A new, not used entry name
	 * @throws RepositoryException if something goes wrong
	 */
	public String getNewNameForExistingData(Folder destination, String originalName, boolean keepSuffix) throws RepositoryException {
		String suffix;
		String oldName;
		if (keepSuffix && originalName.contains(".") && !originalName.endsWith(".")) {
			int beginIndex = originalName.lastIndexOf('.');
			suffix = originalName.substring(beginIndex);
			oldName = originalName.substring(0, beginIndex);
		} else {
			suffix = "";
			oldName = originalName;
		}
		String newName;
		int i = 2;
		do {
			newName = oldName + " - " + i++ + suffix;
		} while (destination.containsData(newName, DataEntry.class));
		return newName;
	}

	/**
	 * Generates new entry name for copies
	 *
	 * @param destination  The folder, to which the entry has to be copied
	 * @param originalName The name of the entry, which has to be copied
	 * @param keepSuffix   if {@code true}, a suffix (defined as last dot followed by characters) is kept
	 * @return A new, not used entry name
	 * @throws RepositoryException if something goes wrong
	 * @deprecated since 9.7, use {@link #getNewNameForExistingData(Folder, String, boolean)} and {@link
	 * #getNewNameForExistingData(Folder, String, boolean)} instead
	 */
	@Deprecated
	public String getNewNameForExistingEntry(Folder destination, String originalName, boolean keepSuffix) throws RepositoryException {
		String suffix;
		String oldName;
		if (keepSuffix && originalName.contains(".") && !originalName.endsWith(".")) {
			int beginIndex = originalName.lastIndexOf('.');
			suffix = originalName.substring(beginIndex);
			oldName = originalName.substring(0, beginIndex);
		} else {
			suffix = "";
			oldName = originalName;
		}
		String newName;
		int i = 2;
		do {
			newName = oldName + " - " + i++ + suffix;
		} while (destination.containsEntry(newName));
		return newName;
	}

	/**
	 * sorts the repositories by type and name
	 */
	private void sortRepositories() {
		Collections.sort(repositories, RepositoryTools.REPOSITORY_COMPARATOR);
	}


	/**
	 * Notifies all {@link RepositoryManagerListener}s that a repository was added to this manager.
	 *
	 * @param repository
	 * 		the added repository
	 */
	private void fireRepositoryWasAdded(Repository repository) {
		for (RepositoryManagerListener l : listeners.getListeners(RepositoryManagerListener.class)) {
			l.repositoryWasAdded(repository);
		}
	}

	/**
	 * Notifies all {@link RepositoryManagerListener}s that a repository will be removed from this manager.
	 *
	 * @param repository
	 * 		the removed repository
	 */
	private void fireRepositoryWasRemoved(Repository repository) {
		for (RepositoryManagerListener l : listeners.getListeners(RepositoryManagerListener.class)) {
			l.repositoryWasRemoved(repository);
		}
	}

	/**
	 * Runs the cleanup for the repositories, i.e. disconnects remote repositories (including SAML authentication)
	 * and clears the {@link #CACHED_MANAGERS cached managers}.
	 *
	 * @since 9.5
	 */
	private static void cleanup() {
		synchronized (INSTANCE_LOCK) {
			if (instance == null) {
				return;
			}
			for (Repository repo : instance.getRepositories()) {
				if (repo instanceof RemoteRepository) {
					((RemoteRepository) repo).cleanup();
				}
			}
			CACHED_MANAGERS.clear();
		}
	}

	/**
	 * Gets or creates the parent folder location of the specified entry.
	 *
	 * @param location       the actual target location of the entry
	 * @param parentLocation the parent location of the entry
	 * @return the parent folder of the entry
	 * @throws RepositoryException if something goes wrong
	 * @since 9.7
	 */
	private Folder getOrCreateParentFolder(RepositoryLocation location, RepositoryLocation parentLocation) throws RepositoryException {
		Folder parentEntry = parentLocation.locateFolder();
		Folder parentFolder;
		if (parentEntry != null) {
			parentFolder = parentEntry;
		} else {
			parentFolder = parentLocation.createFoldersRecursively();
		}

		return parentFolder;
	}
}
