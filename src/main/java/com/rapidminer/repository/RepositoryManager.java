/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.RepositoryGuiTools;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.internal.db.DBRepository;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.repository.resource.ResourceRepository;
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

	private static final Logger LOGGER = LogService.getRoot();

	private static RepositoryManager instance;
	private static final Object INSTANCE_LOCK = new Object();
	private static Repository sampleRepository;
	private static final Map<RepositoryAccessor, RepositoryManager> CACHED_MANAGERS = new HashMap<>();
	private static final List<RepositoryFactory> FACTORIES = new LinkedList<>();
	private static final int DEFAULT_TOTAL_PROGRESS = 100000;

	private static RepositoryProvider provider = new FileRepositoryProvider();

	private final List<Repository> repositories = new LinkedList<>();

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
	public static enum RepositoryType {

		/**
		 * The order of repository types is very important as it is used in the
		 * {@link RepositoryManager#repositoryComparator}
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
			} else if (repo instanceof RemoteRepository) {
				return REMOTE;
			} else {
				return OTHER;
			}
		}
	};

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
			sampleRepository = new ResourceRepository(SAMPLE_REPOSITORY_NAME, "samples");
		}
		repositories.add(sampleRepository);
		sortRepositories();

		// only load local repositories, custom repositories will be loaded after initialization
		load(LocalRepository.class);
	}

	public static void init() {
		synchronized (INSTANCE_LOCK) {
			instance = new RepositoryManager();
			instance.postInstall();
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
		if (customRepoClasses.size() > 0) {
			instance.load(customRepoClasses.toArray(new Class[customRepoClasses.size()]));
		}
	}

	/**
	 * Replaces the used {@link RepositoryProvider}. The {@link DefaultRepositoryProvider} will be
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
		}
	}

	public static void registerFactory(RepositoryFactory factory) {
		synchronized (INSTANCE_LOCK) {
			FACTORIES.add(factory);
		}
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
			// we cannot call post install during init(). The reason is that
			// post install may access RepositoryManager.getInstance() which will be null and hence
			// trigger further recursive, endless calls to init()
			repository.postInstall();
			save();
		}
		sortRepositories();
		fireUpdate(repository);
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
		fireUpdate(null);
	}

	public List<Repository> getRepositories() {
		return Collections.unmodifiableList(repositories);
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
	 * @see #load()
	 */
	public void save() {
		provider.save(getRepositories());
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
				parentFolder.createIOObjectEntry(childName, ioobject, callingOperator, progressListener);
				return ioobject;
			} else {
				throw new RepositoryException("Entry '" + location + "' does not exist.");
			}
		} else if (entry instanceof IOObjectEntry) {
			((IOObjectEntry) entry).storeData(ioobject, callingOperator, null);
			return ioobject;
		} else {
			throw new RepositoryException("Entry '" + location + "' is not a data entry, but " + entry.getType());
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
				throw new RepositoryException("Entry '" + location + "' does not exist.");
			}
		} else if (entry instanceof BlobEntry) {
			return (BlobEntry) entry;
		} else {
			throw new RepositoryException("Entry '" + location + "' is not a blob entry, but a " + entry.getType());
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
			destination.createIOObjectEntry(newName, original, null, null);
			if (listener != null) {
				listener.setCompleted(maxProgress);
			}
		} else if (entry instanceof BlobEntry) {
			BlobEntry blob = (BlobEntry) entry;
			BlobEntry target = destination.createBlobEntry(newName);
			try {
				InputStream in = blob.openInputStream();
				String mimeType = blob.getMimeType();
				OutputStream out = target.openOutputStream(mimeType);
				Tools.copyStreamSynchronously(in, out, false);
				out.close();
				if (listener != null) {
					listener.setCompleted(maxProgress);
				}
			} catch (IOException e) {
				destination.refresh();
				throw new RepositoryException(e);
			}
		} else if (entry instanceof Folder) {
			String sourceAbsolutePath = entry.getLocation().getAbsoluteLocation();
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
			allChildren.addAll(((Folder) entry).getSubfolders());
			allChildren.addAll(((Folder) entry).getDataEntries());
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
		if (path.equals("")) {
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
					if (folder.canRefreshChild(splitted[index])) {
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
						if (folder.canRefreshChild(splitted[index])) {
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
	 * Generates new entry name for copies
	 *
	 * @param destination
	 *            The folder, to which the entry has to be copied
	 * @param newName
	 *            The name of the entry, which has to be copied
	 * @return A new, not used entry name
	 * @throws RepositoryException
	 */
	private String getNewNameForExistingEntry(Folder destination, String newName) throws RepositoryException {
		String originalName = newName;
		newName = "Copy of " + newName;
		int i = 2;
		while (destination.containsEntry(newName)) {
			newName = "Copy " + i++ + " of " + originalName;
		}
		return newName;
	}

	/**
	 * sorts the repositories by type and name
	 */
	private void sortRepositories() {
		Collections.sort(repositories, RepositoryTools.REPOSITORY_COMPARATOR);
	}
}
