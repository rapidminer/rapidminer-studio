/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.dnd.AbstractPatchedTransferHandler;
import com.rapidminer.gui.dnd.DragListener;
import com.rapidminer.gui.dnd.RepositoryLocationList;
import com.rapidminer.gui.dnd.TransferableOperator;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ProgressThreadDialog;
import com.rapidminer.gui.tools.RepositoryGuiTools;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.SwingTools.ResultRunnable;
import com.rapidminer.gui.tools.components.ToolTipWindow;
import com.rapidminer.gui.tools.components.ToolTipWindow.TipProvider;
import com.rapidminer.gui.tools.components.ToolTipWindow.TooltipLocation;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.gui.tools.dialogs.SelectionDialog;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryActionCondition;
import com.rapidminer.repository.RepositoryActionConditionImplConfigRepository;
import com.rapidminer.repository.RepositoryActionConditionImplStandard;
import com.rapidminer.repository.RepositoryActionConditionImplStandardNoRepository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.gui.actions.AbstractRepositoryAction;
import com.rapidminer.repository.gui.actions.ConfigureRepositoryAction;
import com.rapidminer.repository.gui.actions.CopyEntryRepositoryAction;
import com.rapidminer.repository.gui.actions.CopyLocationAction;
import com.rapidminer.repository.gui.actions.CreateFolderAction;
import com.rapidminer.repository.gui.actions.CutEntryRepositoryAction;
import com.rapidminer.repository.gui.actions.DeleteRepositoryEntryAction;
import com.rapidminer.repository.gui.actions.OpenEntryAction;
import com.rapidminer.repository.gui.actions.OpenInFileBrowserAction;
import com.rapidminer.repository.gui.actions.PasteEntryRepositoryAction;
import com.rapidminer.repository.gui.actions.RefreshRepositoryEntryAction;
import com.rapidminer.repository.gui.actions.RenameRepositoryEntryAction;
import com.rapidminer.repository.gui.actions.ShowProcessInRepositoryAction;
import com.rapidminer.repository.gui.actions.StoreProcessAction;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.studio.io.gui.internal.DataImportWizardBuilder;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.PasswordInputCanceledException;
import com.rapidminer.tools.ProgressListener;


/**
 * A tree displaying repository contents.
 * <p>
 * To add new actions to the popup menu, call
 * {@link #addRepositoryAction(Class, RepositoryActionCondition, Class, boolean, boolean)} or
 * {@link #addRepositoryAction(Class, RepositoryActionCondition, boolean, boolean)}. Be sure to
 * follow its instructions carefully.
 *
 * @author Simon Fischer, Tobias Malbrecht, Marco Boeck, Adrian Wilke
 */
public class RepositoryTree extends JTree {

	/**
	 * @author Nils Woehler, Adrian Wilke
	 *
	 */
	private final class RepositoryTreeTransferhandler extends AbstractPatchedTransferHandler {

		private static final long serialVersionUID = 1L;

		// Remember whether the last cut/copy action was a MOVE
		// A move will result in the entry being deleted upon drop / paste
		// Unfortunately there is no easy way to know this from the TransferSupport
		// passed to importData(). It is not even kno wn in createTransferable(), so we
		// cannot even attach it to the Transferable
		// This implementation implies that we can only transfer from one repository tree
		// to the same instance since this state is not passed to other instances.
		// REASON THIS IS HERE:
		// ctrl+x followed by ctrl+v would copy the entry instead of moving it due to
		// the way the transfer system is implemented. See RM-10 for further details.
		int latestAction = 0;

		public RepositoryTreeTransferhandler() {
			addDragListener(new DragListener() {

				@Override
				public void dragStarted(Transferable t) {
					// reset latestAction because a new drag makes the last action irrelevant
					latestAction = 0;
				}

				@Override
				public void dragEnded() {}
			});
		}

		@Override
		public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
			List<DataFlavor> flavors = Arrays.asList(transferFlavors);
			boolean contains = flavors.contains(DataFlavor.javaFileListFlavor);
			contains |= flavors.contains(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR);
			contains |= flavors.contains(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR);
			return contains;
		}

		@Override
		public boolean importData(final TransferSupport ts) {

			// Determine where to insert
			final Entry droppedOnEntry;
			if (ts.isDrop()) {
				Point dropPoint = ts.getDropLocation().getDropPoint();
				TreePath path = getPathForLocation((int) dropPoint.getX(), (int) dropPoint.getY());
				if (path == null) {
					return false;
				}
				droppedOnEntry = (Entry) path.getLastPathComponent();
			} else {
				droppedOnEntry = getSelectedEntry();
			}
			if (droppedOnEntry == null) {
				return false;
			}

			// Execute operation chosen by flavor
			try {
				List<DataFlavor> flavors = Arrays.asList(ts.getDataFlavors());

				if (flavors.contains(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR)) {
					// Single repository entry

					final RepositoryLocation location = (RepositoryLocation) ts.getTransferable()
							.getTransferData(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR);
					List<RepositoryLocation> singleLocationList = new LinkedList<>();
					singleLocationList.add(location);
					return copyOrMoveRepositoryEntries(droppedOnEntry, singleLocationList, ts);

				} else if (flavors.contains(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR)) {
					// Multiple repository entries

					RepositoryLocationList locationList = (RepositoryLocationList) ts.getTransferable()
							.getTransferData(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR);
					List<RepositoryLocation> locations = locationList.getAll();
					return copyOrMoveRepositoryEntries(droppedOnEntry, locations, ts);

				} else if (flavors.contains(DataFlavor.javaFileListFlavor)) {
					// Import file via wizard

					@SuppressWarnings("unchecked")
					List<File> files = (List<File>) ts.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
					File file = files.get(0);
					DataImportWizardBuilder builder = new DataImportWizardBuilder();
					builder.forFile(file.toPath()).build(owner).getDialog().setVisible(true);
					return true;
				} else {
					// Flavor not supported
					return false;
				}
			} catch (UnsupportedFlavorException e) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.repository.RepositoryTree.accepting_flavor_error", e), e);
				return false;
			} catch (IOException | RepositoryException | RuntimeException e) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.repository.RepositoryTree.error_during_drop", e), e);
				return false;
			}
		}

		/**
		 * Copies / moves multiple repository locations.
		 *
		 * Additionally, at first it is checked, if the operations are allowed.
		 */
		private boolean copyOrMoveRepositoryEntries(final Entry droppedOnEntry, final List<RepositoryLocation> locations,
				final TransferSupport ts) throws RepositoryException {

			// First check, if operation is allowed for all locations
			for (RepositoryLocation location : locations) {
				if (!copyOrMoveCheck(droppedOnEntry, location, ts)) {
					return false;
				}
			}

			// Execute operations in new thread
			new ProgressThread("copy_repository_entry", true) {

				final class UserDecisions {

					boolean repeatDecision = false;
					boolean overwriteIfExists = false;
					int lastDecision = 0;
				}

				private static final int INSERT = 1;
				private static final int OVERWRITE = 2;
				private static final int SKIP = 3;

				/** Total progress of progress listener bar */
				private int progressListenerCompleted = 0;

				/** Step size for single entry operation */
				private final int PROGRESS_LISTENER_SINGLE_STEP_SIZE = 100;

				/**
				 * Iteratively perform copy / move
				 */
				@Override
				public void run() {

					// Check, if repository is in location.
					// Repositories can not be moved.
					// This results in a copy operation.
					boolean isRepositoryInLocations = false;
					for (RepositoryLocation location : locations) {
						try {
							if (location.locateEntry() instanceof Repository) {
								isRepositoryInLocations = true;
							}
						} catch (RepositoryException e) {
							SwingTools.showSimpleErrorMessage("error_in_copy_repository_entry", e, location.toString(),
									e.getMessage());
							return;
						}
					}

					boolean isSingleEntryOperation = true;
					if (locations.size() > 1) {
						isSingleEntryOperation = false;
					}

					TreePath droppedOnPath = RepositoryTreeModel.getPathTo(droppedOnEntry,
							RepositoryManager.getInstance(null));

					// Initialize progress listener
					getProgressListener().setTotal(locations.size() * PROGRESS_LISTENER_SINGLE_STEP_SIZE);
					getProgressListener().setCompleted(progressListenerCompleted);

					final UserDecisions userDecisions = new UserDecisions();
					locationLoop: for (RepositoryLocation location : locations) {

						// Single entry check
						try {
							// Entry already exists, overwrite?
							final String effectiveNewName = location.locateEntry().getName();
							if (((Folder) droppedOnEntry).containsEntry(effectiveNewName)) {

								// Do not confuse user with incorrect selected paths
								RepositoryTree.this.setSelectionPath(droppedOnPath);

								if (!userDecisions.repeatDecision) {

									final List<String> optionsToSelect = new LinkedList<>();
									optionsToSelect.add("existing_entry.insert");
									optionsToSelect.add("existing_entry.overwrite");
									optionsToSelect.add("existing_entry.skip");
									final List<String> optionsToCheck = new LinkedList<>();
									if (!isSingleEntryOperation) {
										optionsToCheck.add("existing_entry.repeat");
									}

									int result = SwingTools.invokeAndWaitWithResult(new ResultRunnable<Integer>() {

										@Override
										public Integer run() {
											SelectionDialog selectionDialog = new SelectionDialog(
													ProgressThreadDialog.getInstance(), "existing_entry",
													SelectionDialog.OK_CANCEL_OPTION, new String[] { effectiveNewName },
													optionsToSelect, optionsToCheck).showDialog();

											if (selectionDialog.isOptionSelected("existing_entry.insert")) {
												userDecisions.lastDecision = INSERT;
											} else if (selectionDialog.isOptionSelected("existing_entry.overwrite")) {
												userDecisions.lastDecision = OVERWRITE;
											} else {
												userDecisions.lastDecision = SKIP;
											}
											if (selectionDialog.isOptionChecked("existing_entry.repeat")) {
												userDecisions.repeatDecision = true;
											}
											return selectionDialog.getResult();
										}
									});

									if (result != SelectionDialog.OK_OPTION) {
										return;
									}
								}

								switch (userDecisions.lastDecision) {
									case INSERT:
										userDecisions.overwriteIfExists = false;
										break;
									case OVERWRITE:
										userDecisions.overwriteIfExists = true;
										break;
									case SKIP:
									default:
										continue locationLoop;
								}
							}
						} catch (RepositoryException e) {
							SwingTools.showSimpleErrorMessage("error_in_copy_repository_entry", e, location.toString(),
									e.getMessage());
							continue;
						}

						// Do copy or move operation
						// Extracted to own method for lock handling and retry calls
						boolean done = executeCopyOrMoveOperation(location, isRepositoryInLocations, isSingleEntryOperation,
								userDecisions.overwriteIfExists);
						if (!done) {
							break;
						}

						progressListenerCompleted += PROGRESS_LISTENER_SINGLE_STEP_SIZE;
					}

					// On multi-operations, select the target folder after finishing copy/move
					// operation
					if (droppedOnEntry != null && !isSingleEntryOperation) {
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								TreePath droppedOnPath = RepositoryTreeModel.getPathTo(droppedOnEntry,
										RepositoryManager.getInstance(null));
								RepositoryTree.this.setSelectionPath(droppedOnPath);
								RepositoryTree.this.scrollPathToVisible(droppedOnPath);
							}
						});
					}

					getProgressListener().complete();
				}

				/**
				 * Runs the copy or move operation using {@link RepositoryManager}
				 *
				 * @return <code>false</code> if the user chose to cancel the operation;
				 *         <code>true</code> otherwise
				 */
				private boolean executeCopyOrMoveOperation(RepositoryLocation location, boolean isRepositoryInLocations,
						boolean isSingleEntryOperation, boolean overwriteIfExists) {
					try {
						ProgressListener progressListener = null;
						progressListener = new RescalingProgressListener(getProgressListener(), progressListenerCompleted,
								progressListenerCompleted + PROGRESS_LISTENER_SINGLE_STEP_SIZE);

						if (isMoveOperation(ts, isRepositoryInLocations)) {
							RepositoryManager.getInstance(null).move(location, (Folder) droppedOnEntry, null,
									overwriteIfExists, progressListener);

							// On drag and drop move operation with overwrite, two delete operations
							// are performed. This results in a selection of a wrong tree element.
							// For this case, select the element, which is the target of the drop
							// operation.
							if (overwriteIfExists && droppedOnEntry != null) {
								SwingUtilities.invokeLater(new Runnable() {

									@Override
									public void run() {
										RepositoryTree.this.setSelectionPath(RepositoryTreeModel.getPathTo(droppedOnEntry,
												RepositoryManager.getInstance(null)));
									}
								});

							}

						} else {
							RepositoryManager.getInstance(null).copy(location, (Folder) droppedOnEntry, null,
									overwriteIfExists, progressListener);
						}
					} catch (RepositoryException e) {
						if (e.getCause() != null && e.getCause() instanceof PasswordInputCanceledException) {
							// no extra dialog if login dialog was canceled
							return false;
						}
						// Do not show "cancel" option, if is is an single entry operation
						int dialogMode = ConfirmDialog.YES_NO_CANCEL_OPTION;
						if (isSingleEntryOperation) {
							dialogMode = ConfirmDialog.YES_NO_OPTION;
						}

						String locationName = location.getName();
						if (locationName == null) {
							locationName = "";
						}

						ConfirmDialog dialog = null;
						if (e.getMessage() != null && !e.getMessage().isEmpty()) {
							dialog = new ConfirmDialog(ProgressThreadDialog.getInstance(), "error_in_copy_entry_with_cause",
									dialogMode, false, locationName, e.getMessage());
						} else {
							dialog = new ConfirmDialog(ProgressThreadDialog.getInstance(), "error_in_copy_entry", dialogMode,
									false, locationName);
						}
						dialog.setVisible(true);
						int retry = dialog.getReturnOption();

						if (retry == ConfirmDialog.YES_OPTION) {
							return executeCopyOrMoveOperation(location, isRepositoryInLocations, isSingleEntryOperation,
									overwriteIfExists);

						} else if (retry == ConfirmDialog.CANCEL_OPTION) {
							LogService.getRoot().log(Level.WARNING,
									"com.rapidminer.repository.RepositoryTree.error_during_copying", e);
							return false;

						} else {
							LogService.getRoot().log(Level.WARNING,
									"com.rapidminer.repository.RepositoryTree.error_during_copying", e);
							// No retry and no cancel, just go on
						}
					}
					return true;
				}
			}.start();

			// No failures in initial check
			return true;
		}

		/**
		 * Checks, if desired copy or move operation is allowed.
		 *
		 * If not, false is returned and an additional error message is shown.
		 *
		 * @throws RepositoryException
		 *             If repository can not be found or entry can not be located.
		 * @throws IllegalArgumentException
		 *             If an argument is null
		 */
		private boolean copyOrMoveCheck(final Entry droppedOnEntry, final RepositoryLocation location,
				final TransferSupport ts) throws RepositoryException {

			if (droppedOnEntry == null) {
				throw new IllegalArgumentException("Entry must not be null.");
			} else if (location == null) {
				throw new IllegalArgumentException("RepositoryLocation must not be null.");
			} else if (ts == null) {
				throw new IllegalArgumentException("TransferSupport must not be null.");

			} else if (!(droppedOnEntry instanceof Folder)) {
				// Copy / move only to folder
				return false;

			} else {
				// Check for unknown parameters
				RepositoryLocation targetLocation = ((Folder) droppedOnEntry).getLocation();
				if (targetLocation == null) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.repository.RepositoryTree.parameter_missing.target_location");
					return false;
				}
				String targetAbsolutePath = targetLocation.getAbsoluteLocation();
				if (targetAbsolutePath == null || targetAbsolutePath.isEmpty()) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.repository.RepositoryTree.parameter_missing.target_path");
					return false;
				}
				String sourceAbsolutePath = location.getAbsoluteLocation();
				if (sourceAbsolutePath == null || sourceAbsolutePath.isEmpty()) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.repository.RepositoryTree.parameter_missing.source_path");
					return false;
				}
				Entry locationEntry = location.locateEntry();
				if (locationEntry == null) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.repository.RepositoryTree.parameter_missing.repository_location");
					return false;
				}
				String effectiveNewName = locationEntry.getName();
				if (effectiveNewName == null || effectiveNewName.isEmpty()) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.repository.RepositoryTree.parameter_missing.name");
					return false;
				}

				if (isMoveOperation(ts, false)) {
					// Check for MOVE

					// Make sure same folder moves are forbidden
					if (sourceAbsolutePath.equals(targetAbsolutePath)) {
						SwingTools.showVerySimpleErrorMessage("repository_move_same_folder");
						return false;
					}

					// Make sure moving parent folder into subfolder is forbidden
					if (RepositoryGuiTools.isSuccessor(sourceAbsolutePath, targetAbsolutePath)) {
						SwingTools.showVerySimpleErrorMessage("repository_move_into_subfolder");
						return false;
					}

					// Entry should be moved into its own parent folder, invalid
					if (!(location.locateEntry() instanceof Repository)) {
						String sourceParentLocation = location.locateEntry().getContainingFolder().getLocation()
								.getAbsoluteLocation();
						if (sourceParentLocation.equals(targetAbsolutePath)) {
							SwingTools.showVerySimpleErrorMessage("repository_move_same_folder");
							return false;
						}
					}

				} else {
					// Check for COPY

					// Make sure same folder moves are forbidden
					if (sourceAbsolutePath.equals(targetAbsolutePath)) {
						SwingTools.showVerySimpleErrorMessage("repository_copy_same_folder");
						return false;
					}

					// Make sure moving parent folder into subfolder is forbidden
					if (RepositoryGuiTools.isSuccessor(sourceAbsolutePath, targetAbsolutePath)) {
						SwingTools.showVerySimpleErrorMessage("repository_copy_into_subfolder");
						return false;
					}
				}
				return true;
			}
		}

		@Override
		protected void exportDone(JComponent c, Transferable data, int action) {
			if (action == MOVE) {
				latestAction = MOVE;
			} else {
				latestAction = 0;
			}
		}

		@Override
		public int getSourceActions(JComponent c) {
			return COPY_OR_MOVE;
		}

		@Override
		protected Transferable createTransferable(JComponent c) {

			final TreePath[] treePaths = getSelectionPaths();

			if (treePaths.length == 0) {
				// Nothing selected

				return null;

			} else if (treePaths.length == 1) {
				// Exactly one item selected

				Entry e = (Entry) treePaths[0].getLastPathComponent();
				final RepositoryLocation location = e.getLocation();
				return new Transferable() {

					@Override
					public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
						if (flavor.equals(DataFlavor.stringFlavor)) {
							return location.getAbsoluteLocation();
						} else if (TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR.equals(flavor)) {
							return location;
						} else {
							throw new UnsupportedFlavorException(flavor);
						}
					}

					@Override
					public DataFlavor[] getTransferDataFlavors() {
						return new DataFlavor[] { TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR,
								DataFlavor.stringFlavor };
					}

					@Override
					public boolean isDataFlavorSupported(DataFlavor flavor) {
						return TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR.equals(flavor)
								|| DataFlavor.stringFlavor.equals(flavor);
					}
				};

			} else {
				// Multiple entries selected

				final RepositoryLocationList locationList = new RepositoryLocationList();
				for (TreePath treePath : treePaths) {
					locationList.add(((Entry) treePath.getLastPathComponent()).getLocation());
				}
				locationList.removeIntersectedLocations();

				return new Transferable() {

					final RepositoryLocationList locations = locationList;

					@Override
					public boolean isDataFlavorSupported(DataFlavor flavor) {
						return TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR.equals(flavor)
								|| DataFlavor.stringFlavor.equals(flavor);
					}

					@Override
					public DataFlavor[] getTransferDataFlavors() {
						return new DataFlavor[] { TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR,
								DataFlavor.stringFlavor };
					}

					@Override
					public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
						if (TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR.equals(flavor)) {
							return locations;
						} else if (DataFlavor.stringFlavor.equals(flavor)) {
							return locations.toString();
						} else {
							throw new UnsupportedFlavorException(flavor);
						}
					}
				};
			}
		}

		@Override
		public Icon getVisualRepresentation(Transferable t) {
			return null;
		}

		/**
		 * Checks, if the current operation is a MOVE operation. If not, it is a COPY operation.
		 *
		 * @param ts
		 *            Provides info about the current operation
		 * @param isRepositoryInLocations
		 *            This is true, if the sources to copy contain a repository. Repositories can
		 *            not be moved. This results in a copy operation.
		 * @return true, if the operation is a move operation
		 * @return false, if the operation is not a move operation (e.g. copy)
		 */
		private boolean isMoveOperation(TransferSupport ts, boolean isRepositoryInLocations) {
			return (latestAction == MOVE || ts.isDrop() && ts.getDropAction() == MOVE) && !isRepositoryInLocations;
		}
	}

	/**
	 * Holds the RepositoryAction entries.
	 */
	private static class RepositoryActionEntry {

		private Class<? extends AbstractRepositoryAction> actionClass;

		private RepositoryActionCondition condition;

		private boolean hasSeparatorBefore;

		private boolean hasSeparatorAfter;

		public RepositoryActionEntry(Class<? extends AbstractRepositoryAction> actionClass,
				RepositoryActionCondition condition, boolean hasSeparatorBefore, boolean hasSeparatorAfter) {
			this.actionClass = actionClass;
			this.condition = condition;
			this.hasSeparatorAfter = hasSeparatorAfter;
			this.hasSeparatorBefore = hasSeparatorBefore;
		}

		public boolean hasSeperatorBefore() {
			return hasSeparatorBefore;
		}

		public boolean hasSeperatorAfter() {
			return hasSeparatorAfter;
		}

		public RepositoryActionCondition getRepositoryActionCondition() {
			return condition;
		}

		public Class<? extends AbstractRepositoryAction> getRepositoryActionClass() {
			return actionClass;
		}
	}

	public final AbstractRepositoryAction<Entry> RENAME_ACTION = new RenameRepositoryEntryAction(this);

	public final AbstractRepositoryAction<Entry> DELETE_ACTION = new DeleteRepositoryEntryAction(this);

	public final AbstractRepositoryAction<DataEntry> OPEN_ACTION = new OpenEntryAction(this);

	public final AbstractRepositoryAction<Entry> REFRESH_ACTION = new RefreshRepositoryEntryAction(this);

	public final AbstractRepositoryAction<Folder> CREATE_FOLDER_ACTION = new CreateFolderAction(this);

	public final ResourceActionAdapter SHOW_PROCESS_IN_REPOSITORY_ACTION = new ShowProcessInRepositoryAction(this);

	private final Dialog owner;

	private List<AbstractRepositoryAction> listToEnable = new LinkedList<>();

	private EventListenerList listenerList = new EventListenerList();

	private static final long serialVersionUID = -6613576606220873341L;

	private static final List<RepositoryActionEntry> REPOSITORY_ACTIONS = new LinkedList<>();

	/** List of actions available for multiple entries selected in menu */
	private static final LinkedList<RepositoryActionEntry> REPOSITORY_MULTIPLE_ENTRIES_ACTIONS = new LinkedList<>();

	/** Configuration: List of actions available for multiple entries selected in menu */
	private static final List<String> multipleMenuEntriesActionClasses = new LinkedList<>(
			Arrays.asList(CutEntryRepositoryAction.class.getName(), CopyEntryRepositoryAction.class.getName(),
					DeleteRepositoryEntryAction.class.getName(), RefreshRepositoryEntryAction.class.getName()));

	private final int TREE_ROW_HEIGHT = 24;

	static {
		addRepositoryAction(ConfigureRepositoryAction.class, new RepositoryActionConditionImplConfigRepository(), false,
				true);
		addRepositoryAction(OpenEntryAction.class,
				new RepositoryActionConditionImplStandard(new Class<?>[] { DataEntry.class }, new Class<?>[] {}), false,
				false);
		addRepositoryAction(StoreProcessAction.class, new RepositoryActionConditionImplStandard(
				new Class<?>[] { ProcessEntry.class, Folder.class }, new Class<?>[] {}), false, false);
		addRepositoryAction(RenameRepositoryEntryAction.class,
				new RepositoryActionConditionImplStandardNoRepository(new Class<?>[] { Entry.class }, new Class<?>[] {}),
				false, false);
		addRepositoryAction(CreateFolderAction.class,
				new RepositoryActionConditionImplStandard(new Class<?>[] { Folder.class }, new Class<?>[] {}), false, false);
		addRepositoryAction(CutEntryRepositoryAction.class,
				new RepositoryActionConditionImplStandardNoRepository(new Class<?>[] {}, new Class<?>[] {}), true, false);
		addRepositoryAction(CopyEntryRepositoryAction.class,
				new RepositoryActionConditionImplStandard(new Class<?>[] {}, new Class<?>[] {}), false, false);
		addRepositoryAction(PasteEntryRepositoryAction.class,
				new RepositoryActionConditionImplStandard(new Class<?>[] {}, new Class<?>[] {}), false, false);
		addRepositoryAction(CopyLocationAction.class,
				new RepositoryActionConditionImplStandard(new Class<?>[] {}, new Class<?>[] {}), false, false);
		addRepositoryAction(DeleteRepositoryEntryAction.class,
				new RepositoryActionConditionImplStandard(new Class<?>[] { Entry.class }, new Class<?>[] {}), false, false);
		addRepositoryAction(RefreshRepositoryEntryAction.class,
				new RepositoryActionConditionImplStandard(new Class<?>[] { Entry.class }, new Class<?>[] {}), true, false);
		addRepositoryAction(OpenInFileBrowserAction.class, new RepositoryActionConditionImplStandard(
				new Class<?>[] { Entry.class }, new Class<?>[] { LocalRepository.class }), false, false);
	}

	public RepositoryTree() {
		this(null);
	}

	public RepositoryTree(Dialog owner) {
		this(owner, false);
	}

	public RepositoryTree(Dialog owner, boolean onlyFolders) {
		this(owner, onlyFolders, false, true);
	}

	public RepositoryTree(Dialog owner, boolean onlyFolders, boolean onlyWritableRepositories) {
		this(owner, onlyFolders, onlyWritableRepositories, true);
	}

	public RepositoryTree(Dialog owner, boolean onlyFolders, boolean onlyWritableRepositories, boolean installDraghandler) {
		this(owner, onlyFolders, onlyWritableRepositories, installDraghandler, null);
	}

	/**
	 * @param installDraghandler
	 *            when true, the {@link RepositoryTreeTransferhandler} is installed and the user is
	 *            able to drag/drop data.
	 * @param backgroundColor
	 *            if {@code null} the default background color will be used, otherwise the provided
	 *            background color will be used
	 */
	public RepositoryTree(Dialog owner, boolean onlyFolders, boolean onlyWritableRepositories, boolean installDraghandler,
			final Color backgroundColor) {
		super(new RepositoryTreeModel(RepositoryManager.getInstance(null), onlyFolders, onlyWritableRepositories));
		this.owner = owner;
		((RepositoryTreeModel) getModel()).setParentTree(this);

		// these actions are a) needed for the action map or b) needed by other classes for toolbars
		// etc
		listToEnable.add(DELETE_ACTION);
		listToEnable.add(RENAME_ACTION);
		listToEnable.add(REFRESH_ACTION);
		listToEnable.add(OPEN_ACTION);
		listToEnable.add(CREATE_FOLDER_ACTION);

		RENAME_ACTION.addToActionMap(this, WHEN_FOCUSED);
		DELETE_ACTION.addToActionMap(this, "delete", WHEN_FOCUSED);
		REFRESH_ACTION.addToActionMap(this, WHEN_FOCUSED);

		setLargeModel(true);
		setRowHeight(Math.max(TREE_ROW_HEIGHT, getRowHeight()));
		setRootVisible(false);
		setShowsRootHandles(true);
		if (backgroundColor != null) {
			// in case of a custom background color we need to overwrite the getBackground() method
			// as the constructor of DefaultTreeCellRenderer uses the method to query the background
			// image. Thus we cannot provide the color within the RepositoryTreeCellRenderer
			// constructor.
			setCellRenderer(new RepositoryTreeCellRenderer() {

				private static final long serialVersionUID = 1L;

				@Override
				public Color getBackground() {
					return backgroundColor;
				};

				@Override
				public Color getBackgroundNonSelectionColor() {
					return backgroundColor;
				};
			});
		} else {
			setCellRenderer(new RepositoryTreeCellRenderer());
		}
		getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

		addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if (e.getSource() instanceof JTree) {
					JTree jtree = (JTree) e.getSource();
					jtree.repaint();
				}
			}
		});

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				mouseClickPressReleasePopup(e);

				// Doubleclick
				if (getSelectionCount() == 1) {
					if (e.getClickCount() == 2) {
						TreePath path = getSelectionPath();
						if (path == null) {
							return;
						}
						fireLocationSelected((Entry) path.getLastPathComponent());
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				mouseClickPressReleasePopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				mouseClickPressReleasePopup(e);
			}

			private void mouseClickPressReleasePopup(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {

					int mouseRow = getRowForLocation(e.getX(), e.getY());

					// Mouse is not over row -> Remove current selection
					if (mouseRow == -1) {
						setSelectionInterval(mouseRow, mouseRow);
					}

					// No multiple row selection -> Update selected element
					if (getSelectionCount() <= 1) {
						setSelectionInterval(mouseRow, mouseRow);
					}

					// Multiple rows selected -> Update if mouse over other element
					if (getSelectionCount() > 1) {
						boolean inMultiRow = false;
						for (int selectedRow : getSelectionRows()) {
							if (selectedRow == mouseRow) {
								inMultiRow = true;
							}
						}
						if (!inMultiRow) {
							setSelectionInterval(mouseRow, mouseRow);
						}
					}

					// Finally show popup
					if (e.isPopupTrigger()) {
						showPopup(e);
					}
				}
			}
		});

		addKeyListener(new KeyListener() {

			// status variable to fix bug 987
			private int lastPressedKey;

			@Override
			public void keyTyped(KeyEvent e) {}

			/**
			 * Opens entries on enter pressed; collapses/expands folders
			 */
			@Override
			public void keyReleased(KeyEvent e) {
				if (lastPressedKey != e.getKeyCode()) {
					e.consume();
					return;
				}
				lastPressedKey = 0;

				if (e.getModifiers() == 0) {
					switch (e.getKeyCode()) {
						case KeyEvent.VK_ENTER:
						case KeyEvent.VK_SPACE:
							TreePath path = getSelectionPath();
							if (path == null) {
								return;
							}
							Entry entry = (Entry) path.getLastPathComponent();
							if (entry instanceof Folder) {
								if (isExpanded(path)) {
									collapsePath(path);
								} else {
									expandPath(path);
								}
							} else {
								fireLocationSelected((Entry) path.getLastPathComponent());
							}
							e.consume();
							break;
					}
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
				lastPressedKey = e.getKeyCode();
			}
		});

		if (installDraghandler) {
			setDragEnabled(true);
			setTransferHandler(new RepositoryTreeTransferhandler());
		}

		getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				enableActions();
			}
		});

		addTreeExpansionListener(new TreeExpansionListener() {

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				// select the last expanded/collapsed path
				selectionModel.setSelectionPath(event.getPath());
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				// select the last expanded/collapsed path
				treeExpanded(event);
			}
		});

		enableActions();

		new ToolTipWindow(owner, new TipProvider() {

			@Override
			public String getTip(Object o) {
				if (o instanceof Entry) {
					return ToolTipProviderHelper.getTip((Entry) o);
				} else {
					return null;
				}
			}

			@Override
			public Object getIdUnder(Point point) {
				TreePath path = getPathForLocation((int) point.getX(), (int) point.getY());
				if (path != null) {
					return path.getLastPathComponent();
				} else {
					return null;
				}
			}

			@Override
			public Component getCustomComponent(Object o) {
				if (o instanceof Entry) {
					return ToolTipProviderHelper.getCustomComponent((Entry) o);
				} else {
					return null;
				}
			}
		}, this, TooltipLocation.RIGHT);

		if (backgroundColor != null) {
			setBackground(backgroundColor);
		}
	}

	public void enableActions() {
		for (AbstractRepositoryAction action : listToEnable) {
			action.enable();
		}
	}

	public void addRepositorySelectionListener(RepositorySelectionListener listener) {
		listenerList.add(RepositorySelectionListener.class, listener);
	}

	public void removeRepositorySelectionListener(RepositorySelectionListener listener) {
		listenerList.remove(RepositorySelectionListener.class, listener);
	}

	private void fireLocationSelected(Entry entry) {
		RepositorySelectionEvent event = null;
		for (RepositorySelectionListener l : listenerList.getListeners(RepositorySelectionListener.class)) {
			if (event == null) {
				event = new RepositorySelectionEvent(entry);
			}
			l.repositoryLocationSelected(event);
		}
	}

	/**
	 * Selects as much as possible of the selected path to the given location. Returns true if the
	 * given location references a folder.
	 */
	boolean expandIfExists(RepositoryLocation relativeTo, String location) {
		RepositoryLocation loc;
		boolean full = true;
		if (location != null) {
			try {
				if (relativeTo != null) {
					loc = new RepositoryLocation(relativeTo, location);
				} else {
					loc = new RepositoryLocation(location + "/");
				}
			} catch (Exception e) {
				// do nothing
				return false;
			}
		} else {
			loc = relativeTo;
		}
		if (loc == null) {
			return false;
		}
		Entry entry = null;
		while (true) {
			try {
				entry = loc.locateEntry();
				if (entry != null) {
					break;
				}
			} catch (RepositoryException e) {
				return false;
			}
			loc = loc.parent();
			if (loc == null) {
				return false;
			}
			full = false;
		}
		if (entry != null) {
			RepositoryTreeModel model = (RepositoryTreeModel) getModel();
			TreePath pathTo = model.getPathTo(entry);
			expandPath(pathTo);
			setSelectionPath(pathTo);
			if (entry instanceof Folder) {
				return full;
			}
		}
		return false;
		// loc = loc.parent();
	}

	/**
	 * Expands the tree to select the given entry if it exists.
	 */
	public void expandAndSelectIfExists(RepositoryLocation location) {
		if (location.parent() != null) {
			expandIfExists(location.parent(), location.getName());
		} else {
			expandIfExists(location, null);
		}
		scrollPathToVisible(getSelectionPath());
	}

	private void showPopup(MouseEvent e) {

		TreePath[] paths = getSelectionPaths();
		JPopupMenu menu = new JPopupMenu();

		if (paths == null || paths.length < 1) {
			return;
		} else if (paths.length == 1) {
			// Exactly one item selected
			Object component = paths[0].getLastPathComponent();

			// Add actions
			List<Entry> entryList = new ArrayList<>(1);
			if (component instanceof Entry) {
				entryList.add((Entry) component);
			}
			List<Action> actionList = createContextMenuActions(this, entryList);
			// Go through ordered list of actions and add them
			for (Action action : actionList) {
				if (action == null) {
					menu.addSeparator();
				} else {
					menu.add(action);
				}
			}

			// Append custom actions if there are any
			if (component instanceof Entry) {
				Collection<Action> customActions = ((Entry) component).getCustomActions();
				if (customActions != null && !customActions.isEmpty()) {
					menu.addSeparator();
					for (Action customAction : customActions) {
						menu.add(customAction);
					}
				}
			}

		} else {
			// Multiple items selected

			// Get all selected entries
			List<Entry> selectedEntries = new LinkedList<>();
			for (TreePath path : paths) {
				Object component = path.getLastPathComponent();
				if (component instanceof Entry) {
					selectedEntries.add((Entry) component);
				}
			}

			// Get possible actions of selected entries
			LinkedList<RepositoryActionEntry> evaluatedActions = (LinkedList<RepositoryActionEntry>) REPOSITORY_MULTIPLE_ENTRIES_ACTIONS
					.clone();
			for (int i = 0; i < REPOSITORY_MULTIPLE_ENTRIES_ACTIONS.size(); i++) {
				RepositoryActionEntry actionEntry = REPOSITORY_MULTIPLE_ENTRIES_ACTIONS.get(i);
				if (!actionEntry.getRepositoryActionCondition().evaluateCondition(selectedEntries)) {
					evaluatedActions.remove(actionEntry);
				}
			}

			// Add actions
			boolean lastWasSeparator = true;
			for (RepositoryActionEntry actionEntry : evaluatedActions) {

				try {
					Constructor constructor = actionEntry.getRepositoryActionClass()
							.getConstructor(new Class[] { RepositoryTree.class });
					AbstractRepositoryAction createdAction = (AbstractRepositoryAction) constructor.newInstance(this);
					createdAction.enable();

					if (actionEntry.hasSeparatorBefore && !lastWasSeparator) {
						menu.addSeparator();
					}
					menu.add(createdAction);
					if (actionEntry.hasSeparatorAfter) {
						menu.addSeparator();
						lastWasSeparator = true;
					} else {
						lastWasSeparator = false;
					}

				} catch (Exception ex) {
					LogService.getRoot().log(Level.SEVERE,
							"com.rapidminer.repository.gui.RepositoryTree.creating_repository_action_error",
							actionEntry.getRepositoryActionClass());
				}
			}
		}

		menu.show(this, e.getX(), e.getY());
	}

	/** Opens the process held by the given entry (in the background) and opens it. */
	public static void openProcess(final ProcessEntry processEntry) {
		RepositoryProcessLocation processLocation = new RepositoryProcessLocation(processEntry.getLocation());
		if (RapidMinerGUI.getMainFrame().close()) {
			OpenAction.open(processLocation, true);
		}
		/*
		 * PRE FIX OF BUG 308: When opening process with double click all changes are discarded
		 * ProgressThread openProgressThread = new ProgressThread("open_process") {
		 *
		 * @Override public void run() { try { RepositoryProcessLocation processLocation = new
		 * RepositoryProcessLocation(processEntry.getLocation()); String xml =
		 * processEntry.retrieveXML(); try { final Process process = new Process(xml);
		 * process.setProcessLocation(processLocation); SwingUtilities.invokeLater(new Runnable() {
		 * public void run() { RapidMinerGUI.getMainFrame().setOpenedProcess(process, true,
		 * processEntry.getLocation().toString()); } }); } catch (Exception e) {
		 * RapidMinerGUI.getMainFrame().handleBrokenProxessXML(processLocation, xml, e); } } catch
		 * (Exception e1) { SwingTools.showSimpleErrorMessage("cannot_fetch_data_from_repository",
		 * e1); }
		 *
		 * } }; openProgressThread.start();
		 */
	}

	public Entry getSelectedEntry() {
		TreePath path = getSelectionPath();
		if (path == null) {
			return null;
		}
		Object selected = path.getLastPathComponent();
		if (selected instanceof Entry) {
			return (Entry) selected;
		} else {
			return null;
		}
	}

	/**
	 * Gets list of selected entries of this tree
	 */
	public List<Entry> getSelectedEntries() {
		List<Entry> selectedEntries = new LinkedList<>();
		TreePath[] paths = getSelectionPaths();
		if (paths != null) {
			for (TreePath treePath : paths) {
				Object selected = treePath.getLastPathComponent();
				if (selected instanceof Entry) {
					selectedEntries.add((Entry) selected);
				}
			}
		}
		return selectedEntries;
	}

	public Collection<AbstractRepositoryAction<?>> getAllActions() {
		List<AbstractRepositoryAction<?>> listOfAbstractRepositoryActions = new LinkedList<>();
		for (Action action : createContextMenuActions(this, new LinkedList<Entry>())) {
			if (action instanceof AbstractRepositoryAction<?>) {
				listOfAbstractRepositoryActions.add((AbstractRepositoryAction<?>) action);
			}
		}
		return listOfAbstractRepositoryActions;
	}

	/**
	 * Appends the given {@link AbstractRepositoryAction} extending class to the popup menu actions.
	 * <p>
	 * The class <b>MUST</b> have one public constructor taking only a RepositoryTree. </br>
	 * Example: public MyNewRepoAction(RepositoryTree tree) { ... } </br>
	 * Otherwise creating the action via reflection will fail.
	 *
	 * @param actionClass
	 *            the class extending {@link AbstractRepositoryAction}
	 * @param condition
	 *            the {@link RepositoryActionCondition} which determines on which selected entries
	 *            the action will be visible.
	 * @param hasSeparatorBefore
	 *            if true, a separator will be added before the action
	 * @param hasSeparatorAfter
	 *            if true, a separator will be added after the action
	 * @return true if the action was successfully added; false otherwise
	 */
	public static void addRepositoryAction(Class<? extends AbstractRepositoryAction> actionClass,
			RepositoryActionCondition condition, boolean hasSeparatorBefore, boolean hasSeparatorAfter) {
		addRepositoryAction(actionClass, condition, null, hasSeparatorBefore, hasSeparatorAfter);
	}

	/**
	 * Adds the given {@link AbstractRepositoryAction} extending class to the popup menu actions at
	 * the given index.
	 * <p>
	 * The class <b>MUST</b> have one public constructor taking only a RepositoryTree. </br>
	 * Example: public MyNewRepoAction(RepositoryTree tree) { ... } </br>
	 * Otherwise creating the action via reflection will fail.
	 *
	 * @param actionClass
	 *            the class extending {@link AbstractRepositoryAction}
	 * @param condition
	 *            the {@link RepositoryActionCondition} which determines on which selected entries
	 *            the action will be visible.
	 * @param insertAfterThisAction
	 *            the class of the action after which the new action should be inserted. Set to
	 *            {@code null} to append the action at the end.
	 * @param hasSeparatorBefore
	 *            if true, a separator will be added before the action
	 * @param hasSeparatorAfter
	 *            if true, a separator will be added after the action
	 * @return true if the action was successfully added; false otherwise
	 */
	public static void addRepositoryAction(Class<? extends AbstractRepositoryAction> actionClass,
			RepositoryActionCondition condition, Class<? extends Action> insertAfterThisAction, boolean hasSeparatorBefore,
			boolean hasSeparatorAfter) {
		if (actionClass == null || condition == null) {
			throw new IllegalArgumentException("actionClass and condition must not be null!");
		}

		RepositoryActionEntry newEntry = new RepositoryActionEntry(actionClass, condition, hasSeparatorBefore,
				hasSeparatorAfter);
		if (insertAfterThisAction == null) {
			REPOSITORY_ACTIONS.add(newEntry);
		} else {
			// searching for class to insert after
			boolean inserted = false;
			int i = 0;
			for (RepositoryActionEntry entry : REPOSITORY_ACTIONS) {
				Class<? extends Action> existingAction = entry.getRepositoryActionClass();
				if (existingAction.equals(insertAfterThisAction)) {
					REPOSITORY_ACTIONS.add(i + 1, newEntry);
					inserted = true;
					break;
				}
				i++;
			}

			// if reference couldn't be found: just add as last
			if (!inserted) {
				REPOSITORY_ACTIONS.add(newEntry);
			}
		}

		// add action instances for multiple entires
		if (multipleMenuEntriesActionClasses.contains(actionClass.getName())) {
			REPOSITORY_MULTIPLE_ENTRIES_ACTIONS.add(newEntry);
		}
	}

	/**
	 * Removes the given action from the popup menu actions.
	 *
	 * @param actionClass
	 *            the class of the {@link AbstractRepositoryAction} to remove
	 */
	public static void removeRepositoryAction(Class<? extends AbstractRepositoryAction> actionClass) {
		Iterator<RepositoryActionEntry> iterator = REPOSITORY_ACTIONS.iterator();

		while (iterator.hasNext()) {
			if (iterator.next().getRepositoryActionClass().equals(actionClass)) {
				iterator.remove();
			}
		}
	}

	/**
	 * This method returns a list of actions shown in the context menu if the given
	 * {@link RepositoryActionCondition} is true. Contains {@code null} elements for each separator.
	 * This method is called by each {@link RepositoryTree} instance during construction time and
	 * creates instances via reflection of all registered acionts. See
	 * {@link #addRepositoryAction(Class, RepositoryActionCondition, Class, boolean, boolean)} to
	 * add actions.
	 */
	private static List<Action> createContextMenuActions(RepositoryTree repositoryTree, List<Entry> entryList) {
		List<Action> listOfActions = new LinkedList<>();
		boolean lastWasSeparator = true;

		for (RepositoryActionEntry actionEntry : REPOSITORY_ACTIONS) {
			try {
				if (actionEntry.getRepositoryActionCondition().evaluateCondition(entryList)) {
					if (!lastWasSeparator && actionEntry.hasSeperatorBefore()) {
						// add null element which means a separator will be added in the menu
						listOfActions.add(null);
					}
					Constructor constructor = actionEntry.getRepositoryActionClass()
							.getConstructor(new Class[] { RepositoryTree.class });
					AbstractRepositoryAction createdAction = (AbstractRepositoryAction) constructor
							.newInstance(repositoryTree);
					createdAction.enable();
					listOfActions.add(createdAction);
					if (actionEntry.hasSeperatorAfter()) {
						listOfActions.add(null);
						lastWasSeparator = true;
					} else {
						lastWasSeparator = false;
					}
				}
			} catch (Exception e) {
				LogService.getRoot().log(Level.SEVERE,
						"com.rapidminer.repository.gui.RepositoryTree.creating_repository_action_error",
						actionEntry.getRepositoryActionClass());
			}
		}
		return listOfActions;
	}

}
