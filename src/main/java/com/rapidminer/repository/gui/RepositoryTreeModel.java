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
package com.rapidminer.repository.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.DisconnectedWhileLoadingRepositoryException;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryListener;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.RepositorySortingMethod;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * Model representing {@link Entry}s as a tree.
 *
 * @author Simon Fischer , Denis Schernov, Marcel Seifert
 *
 */
public class RepositoryTreeModel implements TreeModel {

	// Placeholder which is shown during loading
	private static final String PENDING_FOLDER_NAME = "Pending...";

	// keep all entries for the default view
	private static final Predicate<Entry> KEEP_ALL = element -> true;

	// keeps only writable entries
	private static final Predicate<Entry> ONLY_WRITABLE = element -> !element.isReadOnly();

	// Since we want the repository entries to be sorted alphanumeric we have to manipulate the
	// methods getChild and getChildIndex. If we wouldn't cache the sorted entries we would have to
	// sort the entries every time one of these methods is called.
	// We're using a HashMap to cache the location of folders as keys while storing their subfolders
	// and entries as their values in a sorted list.
	private final Map<RepositoryLocation, List<Entry>> sortedRepositoryEntriesHashMap = new ConcurrentHashMap<>();

	private final RepositoryManager root;

	private final EventListenerList listeners = new EventListenerList();

	// filter function to hide some elements from this TreeModel instance
	private final Predicate<Entry> checkElements;

	private final boolean onlyFolders;

	private final boolean onlyWriteableRepositories;

	private final ConcurrentMap<Folder, Boolean> pendingFolders = new ConcurrentHashMap<>();

	private final Set<Folder> brokenFolders = Collections.newSetFromMap(new ConcurrentHashMap<>());

	// should be final, but can't be set in the constructor
	private JTree parentTree = null;

	private RepositorySortingMethod sortingMethod = RepositorySortingMethod.NAME_ASC;

	private final RepositoryListener repositoryListener = new RepositoryListener() {

		/**
		 * Checks if this entry is irrelevant for this tree model
		 *
		 * @param entry the entry
		 * @return {@code true} if the entry doesn't appear in this model
		 */
		private boolean isIrrelevantForTreeModel(Entry entry) {
			if (onlyFolders && !(entry instanceof Folder)) {
				return true;
			}
			if (!checkElements.test(entry)) {
				return true;
			}
			try {
				if (onlyWriteableRepositories && entry.getLocation().getRepository().isReadOnly()) {
					return true;
				}
			} catch (RepositoryException e) {
				return true;
			}
			return false;
		}

		private TreeModelEvent makeChangeEvent(Entry entry) {
			TreePath path = getPathTo(entry.getContainingFolder());
			int index;
			if (entry instanceof Repository) {
				index = getRepositories(RepositoryManager.getInstance(null)).indexOf(entry);
			} else {
				index = getIndexOfChild(entry.getContainingFolder(), entry);
			}
			int[] childIndices = index == -1 ? new int[]{} : new int[]{index};
			Object[] children = index == -1 ? new Object[]{} : new Object[]{index};
			return new TreeModelEvent(RepositoryTreeModel.this, path, childIndices, children);
		}

		@Override
		public void entryAdded(final Entry newEntry, final Folder parent) {
			if (isIrrelevantForTreeModel(newEntry)) {
				return;
			}
			// If there is already a sorted list of the entries of the parentfolder, the key and the
			// list will be deleted so it will be recached with the new entry in the
			// updateCachedRepositoryEntries method.
			sortedRepositoryEntriesHashMap.remove(parent.getLocation());
			// fire event
			final TreeModelEvent e = makeChangeEvent(newEntry);
			SwingTools.invokeAndWait(() -> {
				for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
					l.treeNodesInserted(e);
				}
			});
		}

		@Override
		public void entryRemoved(final Entry removedEntry, final Folder parent, final int index) {
			if (isIrrelevantForTreeModel(removedEntry)) {
				return;
			}
			// If there is already a sorted list of the entries of the parentfolder, the key and the
			// list will be deleted so it will be recached without the deleted entry in the
			// updateCachedRepositoryEntries method.
			sortedRepositoryEntriesHashMap.remove(parent.getLocation());

			// Save path of parent
			final RepositoryTreeUtil treeUtil = new RepositoryTreeUtil();
			TreePath parentPath = getPathTo(parent);
			treeUtil.saveSelectionPath(parentPath);

			// Fire event
			final TreeModelEvent p = makeChangeEvent(removedEntry);
			final TreeModelEvent e = new TreeModelEvent(RepositoryTreeModel.this, parentPath, new int[]{index},
					new Object[]{removedEntry});

			SwingTools.invokeAndWait(() -> {
				for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
					l.treeNodesRemoved(e);
					l.treeStructureChanged(p);
				}
				// Restore selected path / expansion state of parent
				if (parentTree != null) {
					treeUtil.restoreSelectionPaths(parentTree);
				}
			});
		}

		@Override
		public void entryChanged(final Entry entry) {
			if (isIrrelevantForTreeModel(entry)) {
				return;
			}
			// If there is already a sorted list of the entries of the parent folder, the key and the
			// list will be deleted so it will be reached with the changed entry in the
			// updateCachedRepositoryEntries method.
			Folder parent = entry.getContainingFolder();
			if (parent != null) {
				sortedRepositoryEntriesHashMap.remove(parent.getLocation());
			}

			// fire event
			SwingTools.invokeAndWait(() -> updateEntry(entry, true));
		}

		@Override
		public void folderRefreshed(final Folder folder) {
			if (isIrrelevantForTreeModel(folder)) {
				return;
			}
			SwingTools.invokeAndWait(() -> updateEntry(folder, false));
			sortedRepositoryEntriesHashMap.clear();
		}

		@Override
		public void repositoryDisconnected(RemoteRepository repository) {
			// clean up
			brokenFolders.remove(repository);
			pendingFolders.remove(repository);
		}

		/**
		 * Update tree if it was refreshed or an entry was changed.
		 *
		 * @param entry the changed entry
		 * @param changed whether the entry was changed or just refreshed
		 * @since 8.1.2
		 */
		private void updateEntry(Entry entry, boolean changed) {
			RepositoryTreeUtil treeUtil = new RepositoryTreeUtil();
			TreeModelEvent e = makeChangeEvent(entry);
			if (parentTree != null) {
				treeUtil.saveExpansionState(parentTree);
				if (!changed) {
					//Fix for UI glitches if children of a refreshed folder are selected during a refresh
					treeUtil.retainRootSelections(parentTree);
				}
			}
			for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
				if (changed) {
					l.treeNodesChanged(e);
				}
				l.treeStructureChanged(e);
			}
			if (parentTree != null) {
				treeUtil.restoreExpansionState(parentTree);
			}
		}
	};

	public RepositoryTreeModel(final RepositoryManager root) {
		this(root, false, false);
	}

	public RepositoryTreeModel(final RepositoryManager root, final boolean onlyFolders,
							   final boolean onlyWritableRepositories) {
		this(root, onlyFolders, onlyWritableRepositories, KEEP_ALL);
	}

	/**
	 * Construct a new instance of the tree model to show repositories
	 *
	 * @param root
	 * 		the {@link RepositoryManager} to retrieve repositories from
	 * @param onlyFolders
	 * 		if true only show repositories and folders
	 * @param onlyWritableRepositories
	 * 		if true only show repositories and their content with write access
	 * @param predicate
	 * 		if supplied this {@link Predicate} will be used to filter all the shown contents
	 */
	public RepositoryTreeModel(final RepositoryManager root, final boolean onlyFolders,
							   final boolean onlyWritableRepositories, Predicate<Entry> predicate) {
		this.root = root;
		this.onlyFolders = onlyFolders;
		this.onlyWriteableRepositories = onlyWritableRepositories;
		this.checkElements = predicate == null ? KEEP_ALL : predicate;

		for (Repository repository : root.getRepositories()) {
			repository.addRepositoryListener(repositoryListener);
		}
		root.addObserver((o, a) -> {
			for (Repository repository : root.getRepositories()) {
				repository.removeRepositoryListener(repositoryListener);
				repository.addRepositoryListener(repositoryListener);
			}
			final TreeModelEvent e = new TreeModelEvent(this, new TreePath(root));

			SwingTools.invokeAndWait(() -> {
				for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
					l.treeStructureChanged(e);
				}
			});
		}, true);
	}

	/**
	 * Gets a tree path based on an entry.
	 * <p>Warning: this only works for a completely unfiltered tree</p>
	 *
	 * @param entry
	 * 		The entry for which a path should be determined
	 * @param repositoryManager
	 * 		The manager is used as the root of a tree path
	 * @return A tree path.
	 */
	public static TreePath getPathTo(Entry entry, RepositoryManager repositoryManager) {
		if (entry == null) {
			return new TreePath(repositoryManager);
		} else if (entry.getContainingFolder() == null) {
			return new TreePath(repositoryManager).pathByAddingChild(entry);
		} else {
			return getPathTo(entry.getContainingFolder(), repositoryManager).pathByAddingChild(entry);
		}
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		listeners.add(TreeModelListener.class, l);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		listeners.remove(TreeModelListener.class, l);
	}

	/**
	 * Set's the tree which uses this model Must be set directly after the JTree creation, can only be set once.
	 *
	 * @param tree
	 * 		the parent tree
	 */
	public void setParentTree(JTree tree) {
		if (parentTree == null) {
			parentTree = tree;
		}
	}

	/**
	 * Returns the Object which is contained by the parent and has the wanted index while the child will be determinate
	 * by a sorted list of the children of the parent. If there is no data known an empty string will be returned and if
	 * the folder is blocking the data for now PENDING_FOLDER_NAME will be returned.
	 *
	 * @param parent
	 * 		The parent {@link Repository}, {@link Folder}, {@link Entry} etc. of the child.
	 * @param index
	 * 		Refers to the index of the child you want to get.
	 * @return The child with the parent at the index.
	 */
	@Override
	public Object getChild(Object parent, int index) {
		if (parent instanceof RepositoryManager) {
			return getEntryOrNull(getRepositories((RepositoryManager) parent), index);
		} else if (parent instanceof Folder && checkElements.test((Entry) parent)) {
			Folder folder = (Folder) parent;
			if (folder.willBlock()) {
				unblock(folder);
				return index == 0 ? PENDING_FOLDER_NAME : null;
			} else {
				try {
					List<Entry> sortedEntries = sortedRepositoryEntriesHashMap.get(folder.getLocation());
					if (sortedEntries == null || sortedEntries.size() <= index) {
						sortedEntries = updateCachedRepositoryEntries(folder);
					}
					return getEntryOrNull(sortedEntries, index);
				} catch (RepositoryException e) {
					LogService.getRoot().log(Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.repository.gui.RepositoryTreeModel.getting_children_of_folder_error",
									folder.getName(), e),
							e);
					return null;
				}
			}
		} else {
			return null;
		}
	}

	@Override
	public int getChildCount(Object parent) {
		if (parent instanceof RepositoryManager) {
			return getRepositories((RepositoryManager) parent).size();
		} else if (parent instanceof Folder) {
			Folder folder = (Folder) parent;
			if (folder.willBlock()) {
				unblock(folder);
				// folder is broken and has no children
				if (brokenFolders.contains(folder)) {
					return 0;
				}

				return 1; // "Pending...."
			} else {
				try {
					Stream<? extends Entry> stream = folder.getSubfolders().stream();
					if (!onlyFolders) {
						stream = Stream.concat(stream, folder.getDataEntries().stream());
					}
					return (int) stream.filter(checkElements).count();
				} catch (RepositoryException e) {
					LogService.getRoot().log(Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.repository.gui.RepositoryTreeModel.getting_children_count_of_folder_error",
									folder.getName(), e),
							e);
					return 0;
				}
			}
		} else {
			return 0;
		}
	}

	/**
	 * This method will return the index of the child which is contained by parent.
	 *
	 * @param parent
	 * 		Parent object (directory) of the child
	 * @param child
	 * 		Child object which index will be returned
	 * @return The index of the child which is in the parent directory.
	 */
	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if (parent instanceof RepositoryManager) {
			return getRepositories((RepositoryManager) parent).indexOf(child);
		} else if (parent instanceof Folder) {
			// don't return -1 for index of pending "folder" (for blocking folder requests)
			if (PENDING_FOLDER_NAME.equals(child)) {
				return 0;
			}
			Folder folder = (Folder) parent;
			try {
				if (child instanceof Entry) {
					List<Entry> sortedEntries = sortedRepositoryEntriesHashMap.get(folder.getLocation());
					if (sortedEntries == null || !sortedEntries.contains(child)) {
						sortedEntries = updateCachedRepositoryEntries(folder);
					}
					return sortedEntries.indexOf(child);
				}
			} catch (RepositoryException e) {
				LogService.getRoot().log(Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.repository.gui.RepositoryTreeModel.getting_child_index_of_folder_error",
								folder.getName(), e),
						e);
			}
		}
		return -1;
	}

	@Override
	public Object getRoot() {
		return root;
	}

	@Override
	public boolean isLeaf(Object node) {
		return !(node instanceof Folder) && !(node instanceof RepositoryManager);
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		try {
			((Entry) path.getLastPathComponent()).rename(newValue.toString());
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("error_rename", e, e.toString());
		}
	}

	/**
	 * Changes from {@link com.rapidminer.repository.RepositoryFilter} may need to trigger an update here
	 *
	 * @since 9.3
	 */
	public void notifyTreeStructureChanged() {
		TreeModelEvent e = new TreeModelEvent(RepositoryTreeModel.this, getPathTo(null), new int[]{0},
				new Object[]{null});
		for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
			l.treeStructureChanged(e);
		}
	}

	/**
	 * Returns the tree path to the entry, or the first parent, which is visible in the tree
	 *
	 * @param entry
	 * 		the entry
	 * @return the tree path to the entry or it's first visible parent
	 */
	TreePath getPathTo(Entry entry) {
		// get parent if only folder are allowed
		if (onlyFolders && entry != null && !(entry instanceof Folder)) {
			entry = entry.getContainingFolder();
		}
		TreePath path = new TreePath(root);
		// quick exit for null / zero children
		if (entry == null) {
			return path;
		}
		Deque<Entry> entries = new ArrayDeque<>();
		for (Entry parent = entry; parent != null; parent = parent.getContainingFolder()) {
			entries.addFirst(parent);
		}
		// Check if writable repositories are allowed
		if (onlyWriteableRepositories && entries.getFirst().isReadOnly()) {
			return path;
		}
		// build path
		for (Entry child : entries) {
			if (!checkElements.test(child)) {
				return path;
			}
			path = path.pathByAddingChild(child);
		}
		return path;
	}

	/**
	 * Sets the {@link RepositorySortingMethod} with which this {@link RepositoryTreeModel} is sorted
	 *
	 * @param method
	 * 		The {@link RepositorySortingMethod}
	 * @since 7.4
	 */
	void setSortingMethod(RepositorySortingMethod method) {
		sortingMethod = method;
		sortedRepositoryEntriesHashMap.clear();

		// Save expansion state and notify listeners to prevent GUI misbehavior
		final RepositoryTreeUtil treeUtil = new RepositoryTreeUtil();
		if (parentTree != null) {
			treeUtil.saveExpansionState(parentTree);
		}

		notifyTreeStructureChanged();

		SwingUtilities.invokeLater(() -> treeUtil.restoreSelectionPaths(parentTree));
	}

	/**
	 * Gets the {@link RepositorySortingMethod} with which this {@link RepositoryTreeModel} is sorted
	 *
	 * @since 7.4
	 */
	RepositorySortingMethod getSortingMethod() {
		return sortingMethod;
	}

	/**
	 * Returns the list entry, or {@code null}
	 *
	 * @param list
	 * 		the list
	 * @param index
	 * 		the index in the list
	 * @return the list entry if available, {@code null} otherwise
	 */
	private static Object getEntryOrNull(List list, int index) {
		if (index < 0 || index >= list.size()) {
			return null;
		}
		return list.get(index);
	}

	/**
	 * Get the filtered and predicate checked repositories from a {@link RepositoryManager}
	 */
	private List<Repository> getRepositories(RepositoryManager parent) {
		Predicate<Entry> filter = onlyWriteableRepositories ? ONLY_WRITABLE.and(checkElements) : checkElements;
		return parent.getFilteredRepositories().stream().filter(filter).collect(Collectors.toList());
	}

	/**
	 * This method will update the cached HashMap depending on the key which is the parameter parent more specifically
	 * its path.
	 *
	 * @param parent
	 * 		determinate which part of the cached HashMap has to be updated
	 * @throws RepositoryException
	 * 		if either sub folders or entries cannot be retrieved
	 */
	private List<Entry> updateCachedRepositoryEntries(Folder parent) throws RepositoryException {
		// sort and filter
		List<Entry> sortedFolders = parent.getSubfolders().stream().filter(checkElements).sorted(sortingMethod).collect(Collectors.toCollection(ArrayList::new));

		if (!onlyFolders) {
			// sort and filter and add to list
			sortedFolders.addAll(parent.getDataEntries().stream().filter(checkElements).sorted(sortingMethod).collect(Collectors.toList()));
		}
		sortedRepositoryEntriesHashMap.put(parent.getLocation(), sortedFolders);
		return sortedFolders;
	}

	/**
	 * Asynchronously fetches data from the folder so it will no longer block and then notifies listeners on the EDT.
	 */
	private void unblock(final Folder folder) {
		if (pendingFolders.putIfAbsent(folder, Boolean.TRUE) != null) {
			return;
		}

		new Thread("wait-for-" + folder.getName()) {

			@Override
			public void run() {

				final List<Entry> children = new ArrayList<>();
				final AtomicBoolean folderBroken = new AtomicBoolean(false);
				boolean disconnected = false;
				try {
					children.addAll(updateCachedRepositoryEntries(folder)); // this may take some time
				} catch (DisconnectedWhileLoadingRepositoryException e) {
					// ignore actual exception and mark loading as interrupted to prevent new authentication
					LogService.getRoot().info(I18N.getMessage(LogService.getRoot().getResourceBundle(), "com.rapidminer.repository.gui.RepositoryTreeModel.disconnect_while_loading_info", folder.getName()));
					disconnected = true;
				} catch (Exception e) {
					// this occurs for example if the remote repository is unreachable
					folderBroken.set(true);
					brokenFolders.add(folder);
					SwingTools.showSimpleErrorMessage("error_fetching_folder_contents_from_repository", e);
				} finally {
					if (!disconnected) {
						SwingUtilities.invokeLater(() -> {
							try {
								TreeModelEvent removeEvent = new TreeModelEvent(RepositoryTreeModel.this, getPathTo(folder),
										new int[]{0}, new Object[]{PENDING_FOLDER_NAME});

								for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
									try {
										l.treeNodesRemoved(removeEvent);
									} catch (Exception e) {
										// the pending entry is most times already gone
									}
								}

								int[] index = new int[children.size()];
								Arrays.setAll(index, i -> i);
								Object[] childArray = children.toArray();
								if (childArray.length > 0) {
									TreeModelEvent insertEvent = new TreeModelEvent(RepositoryTreeModel.this,
											getPathTo(folder), index, childArray);

									for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
										l.treeNodesInserted(insertEvent);
									}
								}
								for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
									l.treeStructureChanged(new TreeModelEvent(RepositoryTreeModel.this, getPathTo(folder)));
								}
							} finally {
								if (!folderBroken.get()) {
									pendingFolders.remove(folder);
									brokenFolders.remove(folder);
								}
							}
						});
					}
				}
			}
		}.start();

	}
}
