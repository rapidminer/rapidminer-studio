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
package com.rapidminer.repository.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryListener;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.RepositorySortingMethod;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;


/**
 * Model representing {@link Entry}s as a tree.
 *
 * @author Simon Fischer , Denis Schernov, Marcel Seifert
 *
 */
public class RepositoryTreeModel implements TreeModel {

	// Since we want the repository entries to be sorted alphanumeric we have to manipulate the
	// methods getChild and getChildIndex. If we wouldn't cache the sorted entries we would have to
	// sort the entries every time one of these methods is called.
	// We're using a HashMap to cache the location of folders as keys while storing their subfolders
	// and entries as their values in a sorted list.
	private final HashMap<RepositoryLocation, List<Entry>> sortedRepositoryEntriesHashMap = new HashMap<>();

	private static final String PENDING_FOLDER_NAME = "Pending...";

	private final RepositoryManager root;

	private final EventListenerList listeners = new EventListenerList();

	private JTree parentTree = null;

	private final RepositoryListener repositoryListener = new RepositoryListener() {

		private TreeModelEvent makeChangeEvent(Entry entry) {
			TreePath path = getPathTo(entry.getContainingFolder());
			int index;
			if (entry instanceof Repository) {
				index = RepositoryManager.getInstance(null).getRepositories().indexOf(entry);
			} else {
				index = getIndexOfChild(entry.getContainingFolder(), entry);
			}
			return new TreeModelEvent(RepositoryTreeModel.this, path, new int[] { index }, new Object[] { entry });
		}

		@Override
		public void entryAdded(final Entry newEntry, final Folder parent) {

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
			final TreeModelEvent e = new TreeModelEvent(RepositoryTreeModel.this, parentPath, new int[] { index },
					new Object[] { removedEntry });

			SwingTools.invokeAndWait(() -> {
				for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
					l.treeNodesRemoved(e);
					l.treeStructureChanged(p);
				}
			});

			// Restore selected path / expansion state of parent
			SwingUtilities.invokeLater(() -> treeUtil.restoreSelectionPaths(parentTree));
		}

		@Override
		public void entryChanged(final Entry entry) {

			// If there is already a sorted list of the entries of the parentfolder, the key and the
			// list will be deleted so it will be recached with the changed entry in the
			// updateCachedRepositoryEntries method.
			Folder parent = entry.getContainingFolder();
			if (parent != null) {
				sortedRepositoryEntriesHashMap.remove(parent.getLocation());
			}

			// fire event
			final TreeModelEvent p = entry != null ? makeChangeEvent(entry) : null;
			final TreeModelEvent e = makeChangeEvent(entry);

			final RepositoryTreeUtil treeUtil = new RepositoryTreeUtil();
			if (parentTree != null) {
				treeUtil.saveExpansionState(parentTree);
			}

			SwingTools.invokeAndWait(() -> {
				for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
					l.treeNodesChanged(e);
					if (p != null) {
						l.treeStructureChanged(p);
					}
				}
			});
			if (parentTree != null) {
				SwingUtilities.invokeLater(() -> treeUtil.restoreExpansionState(parentTree));
			}
		}

		@Override
		public void folderRefreshed(final Folder folder) {
			final RepositoryTreeUtil treeUtil = new RepositoryTreeUtil();
			final TreeModelEvent e = makeChangeEvent(folder);
			SwingTools.invokeAndWait(() -> {
				if (parentTree != null) {
					treeUtil.saveExpansionState(parentTree);
				}
				for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
					l.treeStructureChanged(e);
				}
				treeUtil.locateExpandedEntries();
			});
			if (parentTree != null) {
				SwingUtilities.invokeLater(() -> treeUtil.restoreExpansionState(parentTree));
			}
			sortedRepositoryEntriesHashMap.clear();
		}
	};

	private boolean onlyFolders = false;

	private boolean onlyWriteableRepositories = false;

	RepositorySortingMethod sortingMethod = RepositorySortingMethod.NAME_ASC;

	public RepositoryTreeModel(final RepositoryManager root) {
		this(root, false, false);
	}

	public RepositoryTreeModel(final RepositoryManager root, final boolean onlyFolders,
			final boolean onlyWritableRepositories) {

		this.root = root;
		this.onlyFolders = onlyFolders;
		this.onlyWriteableRepositories = onlyWritableRepositories;
		for (Repository repository : root.getRepositories()) {
			repository.addRepositoryListener(repositoryListener);
		}
		root.addObserver(new Observer<Repository>() {

			@Override
			public void update(Observable<Repository> observable, Repository arg) {
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
			}
		}, true);
	}

	TreePath getPathTo(Entry entry) {
		return RepositoryTreeModel.getPathTo(entry, root);
	}

	/**
	 * Gets a tree path based on an entry.
	 *
	 * @param entry
	 *            The entry for which a path should be determined
	 * @param repositoryManager
	 *            The manager is used as the root of a tree path
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

	public void setParentTree(JTree tree) {
		parentTree = tree;
	}

	/**
	 * Returns the Object which is contained by the parent and has the wanted index while the child
	 * will be determinate by a sorted list of the children of the parent. If there is no data known
	 * an empty string will be returned and if the folder is blocking the data for now
	 * PENDING_FOLDER_NAME will be returned.
	 *
	 * @param parent
	 *            The parent {@link Repository}, {@link Folder}, {@link Entry} etc. of the child.
	 * @param index
	 *            Refers to the index of the child you want to get.
	 * @return The child with the parent at the index.
	 */
	@Override
	public Object getChild(Object parent, int index) {
		if (parent instanceof RepositoryManager) {
			if (onlyWriteableRepositories) {
				return getWritableRepositories((RepositoryManager) parent).get(index);
			}
			return ((RepositoryManager) parent).getRepositories().get(index);
		} else if (parent instanceof Folder) {
			Folder folder = (Folder) parent;
			if (folder.willBlock()) {
				unblock(folder);
				return PENDING_FOLDER_NAME;
			} else {
				try {
					List<Entry> sortedEntries = sortedRepositoryEntriesHashMap.get(folder.getLocation());
					if (sortedEntries == null) {
						sortedEntries = updateCachedRepositoryEntries(folder);
					}
					int numFolders = folder.getSubfolders().size();
					if (index < numFolders) {
						if (sortedEntries.size() <= index) {
							sortedEntries = updateCachedRepositoryEntries(folder);
						}
						return sortedEntries.get(index);
					} else if (onlyFolders) {
						return null;
					} else {
						if (folder.getDataEntries().size() > index - numFolders) {
							if (sortedEntries.size() <= index) {
								sortedEntries = updateCachedRepositoryEntries(folder);
							}
							return sortedEntries.get(index);
						} else {
							// In this case and at this state, no data entry is known.
							// Returning null would cause a NPE.
							// This solution prevents from an IndexOutOfBoundsException inside EDT.
							return "";
						}
					}
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

	private final Set<Folder> pendingFolders = new HashSet<>();
	private final Set<Folder> brokenFolders = new HashSet<>();

	/**
	 * Asynchronously fetches data from the folder so it will no longer block and then notifies
	 * listeners on the EDT.
	 */
	private void unblock(final Folder folder) {
		if (pendingFolders.contains(folder)) {
			return;
		}
		pendingFolders.add(folder);

		new Thread("wait-for-" + folder.getName()) {

			@Override
			public void run() {

				final List<Entry> children = new ArrayList<>();
				final AtomicBoolean folderBroken = new AtomicBoolean(false);
				try {
					List<Folder> subfolders = folder.getSubfolders();
					children.addAll(subfolders); // this may take some time
					children.addAll(folder.getDataEntries()); // this may take some time
				} catch (Exception e) {
					// this occurs for example if the remote repository is unreachable
					folderBroken.set(true);
					brokenFolders.add(folder);
					SwingTools.showSimpleErrorMessage("error_fetching_folder_contents_from_repository", e);
				} finally {
					SwingUtilities.invokeLater(() -> {
							try {
								TreeModelEvent removeEvent = new TreeModelEvent(RepositoryTreeModel.this, getPathTo(folder),
										new int[] { 0 }, new Object[] { PENDING_FOLDER_NAME });

								for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
									l.treeNodesRemoved(removeEvent);
								}

								int[] index = new int[children.size()];
								for (int i = 0; i < index.length; i++) {
									index[i] = i;
								}
								Object[] childArray = children.toArray();
								if (childArray.length > 0) {
									TreeModelEvent insertEvent = new TreeModelEvent(RepositoryTreeModel.this,
											getPathTo(folder), index, childArray);

									for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
										l.treeNodesInserted(insertEvent);
									}
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
		}.start();

	}

	@Override
	public int getChildCount(Object parent) {
		if (parent instanceof RepositoryManager) {
			if (onlyWriteableRepositories) {
				return getWritableRepositories((RepositoryManager) parent).size();
			}
			return ((RepositoryManager) parent).getRepositories().size();
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
					if (onlyFolders) {
						return folder.getSubfolders().size();
					} else {
						return folder.getSubfolders().size() + folder.getDataEntries().size();
					}
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
	 *            Parent object (directory) of the child
	 * @param child
	 *            Child object which index will be returned
	 * @return The index of the child which is in the parent directory.
	 */
	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if (parent instanceof RepositoryManager) {
			if (onlyWriteableRepositories) {
				return getWritableRepositories((RepositoryManager) parent).indexOf(child);
			}
			return ((RepositoryManager) parent).getRepositories().indexOf(child);
		} else if (parent instanceof Folder) {
			// don't return -1 for index of pending "folder" (for blocking folder requests)
			if (PENDING_FOLDER_NAME.equals(child)) {
				return 0;
			}
			Folder folder = (Folder) parent;
			try {
				List<Entry> sortedEntries = sortedRepositoryEntriesHashMap.get(folder.getLocation());
				if (sortedEntries == null) {
					sortedEntries = updateCachedRepositoryEntries(folder);
				}
				if (child instanceof Entry) {
					if (folder.getSubfolders().contains(child) || folder.getDataEntries().contains(child)) {
						if (!sortedEntries.contains(child)) {
							sortedEntries = updateCachedRepositoryEntries(folder);
						}
						return sortedEntries.indexOf(child);
					}
				} else {
					return -1;
				}
			} catch (RepositoryException e) {
				LogService.getRoot().log(Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.repository.gui.RepositoryTreeModel.getting_child_index_of_folder_error",
								folder.getName(), e),
						e);
				return -1;
			}
		} else {
			return -1;
		}
		return 0;
	}

	/**
	 * This method will update the cached HashMap depending on the key which is the parameter parent
	 * more specifically its path.
	 *
	 * @param parent
	 *            determinate which part of the cached HashMap has to be updated
	 * @throws RepositoryException
	 */
	private List<Entry> updateCachedRepositoryEntries(Folder parent) throws RepositoryException {
		List<Entry> sortedFolders = new ArrayList<>(parent.getSubfolders());
		List<Entry> sortedEntries = new ArrayList<>(parent.getDataEntries());

		// sort entries depending on sorting method
		Collections.sort(sortedEntries, sortingMethod);
		Collections.sort(sortedFolders, sortingMethod);

		List<Entry> sortedFoldersFirst = sortedFolders;
		sortedFoldersFirst.addAll(sortedEntries);
		sortedRepositoryEntriesHashMap.put(parent.getLocation(), sortedFoldersFirst);
		return sortedFoldersFirst;
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

	private List<Repository> getWritableRepositories(RepositoryManager manager) {
		List<Repository> repositories = manager.getRepositories();
		List<Repository> writeableRepositories = new ArrayList<>();
		for (Repository repository : repositories) {
			if (!repository.isReadOnly()) {
				writeableRepositories.add(repository);
			}
		}
		return writeableRepositories;
	}

	/**
	 * Sets the {@link RepositorySortingMethod} with which this {@link RepositoryTreeModel} is
	 * sorted
	 *
	 * @param method
	 *            The {@link RepositorySortingMethod}
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

		TreeModelEvent e = new TreeModelEvent(RepositoryTreeModel.this, getPathTo(null), new int[] { 0 },
				new Object[] { null });
		for (TreeModelListener l : listeners.getListeners(TreeModelListener.class)) {
			l.treeStructureChanged(e);
		}

		SwingUtilities.invokeLater(() -> treeUtil.restoreSelectionPaths(parentTree));
	}

	/**
	 * Gets the {@link RepositorySortingMethod} with which this {@link RepositoryTreeModel} is
	 * sorted
	 *
	 * @since 7.4
	 */
	RepositorySortingMethod getSortingMethod() {
		return sortingMethod;
	}

}
