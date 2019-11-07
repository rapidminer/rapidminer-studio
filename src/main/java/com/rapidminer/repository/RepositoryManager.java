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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.EventListenerList;
import javax.swing.tree.TreeModel;

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
import com.rapidminer.security.PluginSandboxPolicy;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;
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
		RESOURCES, DB, LOCAL, REMOTE, OTHER;

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
			new RepositoryGlobalSearch();
			// initialize the content store mapper to enable it to update data for repo locations on rename
			PersistentContentMapperStore.INSTANCE.init();
			instance.postInstall();
			RapidMiner.registerCleanupHook(RepositoryManager::cleanup);
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
		boolean noLocalRepository = true;
		// check if we have at least one repository that is not pre-defined
		for (Repository repository : repositories) {
			if (repository instanceof LocalRepository) {
				noLocalRepository = false;
				break;
			}
		}
		if (noLocalRepository) {
			try {
				LocalRepository defaultRepo = new LocalRepository("Local Repository");
				RepositoryManager.getInstance(null).addRepository(defaultRepo);
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
		Entry entry = location.locateEntry();
		boolean isConnectionEntry = entry instanceof ConnectionEntry;
		if (entry != null && isConnectionEntry != ioobject instanceof ConnectionInformationContainerIOObject) {
			String expectedType = isConnectionEntry ? "connection" : "data";
			throw new RepositoryEntryWrongTypeException(location, expectedType, entry.getType());
		}
		if (entry == null) {
			RepositoryLocation parentLocation = location.parent();
			if (parentLocation != null) {
				String childName = location.getName();

				Entry parentEntry = parentLocation.locateEntry();
				Folder parentFolder;
				if (parentEntry != null) {
					if (parentEntry instanceof Folder) {
						parentFolder = (Folder) parentEntry;
					} else {
						throw new RepositoryException(
								"Parent '" + parentLocation + "' of '" + location + "' is not a folder.");
					}
				} else {
					parentFolder = parentLocation.createFoldersRecursively();
				}
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

	/** Gets the referenced blob entry. Creates a new one if it does not exist. */
	public BlobEntry getOrCreateBlob(RepositoryLocation location) throws RepositoryException {
		Entry entry = location.locateEntry();
		if (entry == null) {
			RepositoryLocation parentLocation = location.parent();
			if (parentLocation != null) {
				String childName = location.getName();
				Entry parentEntry = parentLocation.locateEntry();
				Folder parentFolder;
				if (parentEntry != null) {
					if (parentEntry instanceof Folder) {
						parentFolder = (Folder) parentEntry;
					} else {
						throw new RepositoryException(
								"Parent '" + parentLocation + "' of '" + location + "' is not a folder.");
					}
				} else {
					parentFolder = parentLocation.createFoldersRecursively();
				}
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

	/** Copies an entry to a given destination folder. */
	public void copy(RepositoryLocation source, Folder destination, ProgressListener listener) throws RepositoryException {
		copy(source, destination, null, listener);
	}

	/**
	 * Copies an entry to a given destination folder with the name newName. If newName is null the
	 * old name will be kept. If an entry named newName exists, newName will be changed and not
	 * overwritten.
	 */
	public void copy(RepositoryLocation source, Folder destination, String newName, ProgressListener listener)
			throws RepositoryException {
		copy(source, destination, newName, false, listener);
	}

	/**
	 * Copies an entry to a given destination folder with the name newName. If newName is null the
	 * old name will be kept.
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
		Entry entry = source.locateEntry();
		if (entry == null) {
			throw new RepositoryException("No such entry: " + source);
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

		if (destination.containsEntry(newName)) {
			if (overwriteIfExists) {
				if (destination.equals(entry.getContainingFolder())) {
					// Do not overwrite file, if source and target folders are the same
					return;
				} else {
					List<DataEntry> entries = destination.getDataEntries();
					for (int i = 0; i < entries.size(); i++) {
						DataEntry entrytoDelete = entries.get(i);
						if (entrytoDelete.getName().equals(newName)) {
							entrytoDelete.delete();
							break;
						}
					}
					List<Folder> folders = destination.getSubfolders();
					for (int i = 0; i < folders.size(); i++) {
						Folder foldertoDelete = folders.get(i);
						if (foldertoDelete.getName().equals(newName)) {
							foldertoDelete.delete();
							break;
						}
					}
				}
			} else {
				newName = getNewNameForExistingEntry(destination, newName);
			}
		}

		if (entry instanceof ProcessEntry) {
			ProcessEntry pe = (ProcessEntry) entry;
			String xml = pe.retrieveXML();
			if (listener != null) {
				listener.setCompleted((minProgress + maxProgress) / 2);
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
		} else if (entry instanceof Folder) {
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
		} else {
			throw new RepositoryException("Cannot copy entry of type " + entry.getType());
		}
	}

	/** Moves an entry to a given destination folder. */
	public void move(RepositoryLocation source, Folder destination, ProgressListener listener) throws RepositoryException {
		move(source, destination, null, listener);
	}

	/** Moves an entry to a given destination folder with the name newName. */
	public void move(RepositoryLocation source, Folder destination, String newName, ProgressListener listener)
			throws RepositoryException {
		// Default: Overwrite existing entry
		move(source, destination, newName, true, listener);
	}

	/** Moves an entry to a given destination folder with the name newName. */
	public void move(RepositoryLocation source, Folder destination, String newName, boolean overwriteIfExists,
					 ProgressListener listener) throws RepositoryException {
		Entry entry = source.locateEntry();
		if (entry == null) {
			throw new RepositoryException("No such entry: " + source);
		} else {
			String sourceAbsolutePath = source.getAbsoluteLocation();
			String destinationAbsolutePath;
			if (!(entry instanceof Folder)) {
				destinationAbsolutePath = destination.getLocation().getAbsoluteLocation() + RepositoryLocation.SEPARATOR
						+ source.getName();
			} else {
				destinationAbsolutePath = destination.getLocation().getAbsoluteLocation();
			}
			// make sure same folder moves are forbidden
			if (sourceAbsolutePath.equals(destinationAbsolutePath)) {
				throw new RepositoryException(
						I18N.getMessage(I18N.getErrorBundle(), "repository.repository_move_same_folder"));
			}
			// make sure moving parent folder into subfolder is forbidden
			if (RepositoryGuiTools.isSuccessor(sourceAbsolutePath, destinationAbsolutePath)) {
				throw new RepositoryException(
						I18N.getMessage(I18N.getErrorBundle(), "repository.repository_move_into_subfolder"));
			}
			if (destination.getLocation().getRepository() != source.getRepository()) {
				copy(source, destination, newName, listener);
				entry.delete();
			} else {
				String effectiveNewName = newName != null ? newName : entry.getName();
				Entry toDeleteEntry = null;
				if (destination.containsEntry(effectiveNewName)) {
					if (overwriteIfExists) {
						for (DataEntry dataEntry : destination.getDataEntries()) {
							if (dataEntry.getName().equals(effectiveNewName)) {
								toDeleteEntry = dataEntry;
							}
						}
						for (Folder folderEntry : destination.getSubfolders()) {
							if (folderEntry.getName().equals(effectiveNewName)) {
								toDeleteEntry = folderEntry;
							}
						}
						if (toDeleteEntry != null) {
							toDeleteEntry.delete();
						}
					} else {
						newName = getNewNameForExistingEntry(destination, effectiveNewName);
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
	}

	/**
	 * Looks up the entry with the given path in the given repository. This method will return null
	 * when it finds a folder that blocks (has not yet loaded all its data) AND failIfBlocks is
	 * true.
	 *
	 * This method can be used as a first approach to locate an entry and fall back to a more
	 * expensive solution when this fails.
	 *
	 */
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
			repositoryFilters.stream().filter(filter -> filter.filter(singleRepoList).isEmpty()).forEach( filter -> {
				try {
					filter.reset();
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "com.rapidminer.repository.RepositoryManager.filter_failure", e);
				}
			});
		}
	}


	/**
	 * Generates new entry name for copies
	 *
	 * @param destination
	 *            The folder, to which the entry has to be copied
	 * @param originalName
	 *            The name of the entry, which has to be copied
	 * @return A new, not used entry name
	 * @throws RepositoryException
	 */
	private String getNewNameForExistingEntry(Folder destination, String originalName) throws RepositoryException {
		String newName;
		int i = 2;
		do {
			newName = originalName + " - " + i++;
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
}
