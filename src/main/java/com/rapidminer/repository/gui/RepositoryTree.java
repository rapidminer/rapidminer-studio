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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Point;
import java.awt.Rectangle;
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.gui.dnd.AbstractPatchedTransferHandler;
import com.rapidminer.gui.dnd.DragListener;
import com.rapidminer.gui.dnd.RepositoryLocationList;
import com.rapidminer.gui.dnd.TransferableOperator;
import com.rapidminer.gui.dnd.TransferableRepositoryEntry;
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
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryActionCondition;
import com.rapidminer.repository.RepositoryActionConditionAdditionallyNotConnections;
import com.rapidminer.repository.RepositoryActionConditionImplConfigRepository;
import com.rapidminer.repository.RepositoryActionConditionImplStandard;
import com.rapidminer.repository.RepositoryActionConditionRepositoryAndConnections;
import com.rapidminer.repository.RepositoryConnectionsFolderImmutableException;
import com.rapidminer.repository.RepositoryConnectionsNotSupportedException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.RepositoryNotConnectionsFolderException;
import com.rapidminer.repository.RepositorySortingMethod;
import com.rapidminer.repository.RepositorySortingMethodListener;
import com.rapidminer.repository.RepositoryStoreOtherInConnectionsFolderException;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.repository.gui.actions.AbstractRepositoryAction;
import com.rapidminer.repository.gui.actions.CheckProcessCompatibility;
import com.rapidminer.repository.gui.actions.ConfigureRepositoryAction;
import com.rapidminer.repository.gui.actions.CopyEntryRepositoryAction;
import com.rapidminer.repository.gui.actions.CopyLocationAction;
import com.rapidminer.repository.gui.actions.CreateConnectionAction;
import com.rapidminer.repository.gui.actions.CreateFolderAction;
import com.rapidminer.repository.gui.actions.CutEntryRepositoryAction;
import com.rapidminer.repository.gui.actions.DeleteRepositoryEntryAction;
import com.rapidminer.repository.gui.actions.EditConnectionAction;
import com.rapidminer.repository.gui.actions.OpenEntryAction;
import com.rapidminer.repository.gui.actions.OpenInFileBrowserAction;
import com.rapidminer.repository.gui.actions.PasteEntryRepositoryAction;
import com.rapidminer.repository.gui.actions.RefreshRepositoryEntryAction;
import com.rapidminer.repository.gui.actions.RenameRepositoryEntryAction;
import com.rapidminer.repository.gui.actions.ShowProcessInRepositoryAction;
import com.rapidminer.repository.gui.actions.SortByLastModifiedAction;
import com.rapidminer.repository.gui.actions.SortByNameAction;
import com.rapidminer.repository.gui.actions.StoreProcessAction;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.studio.io.gui.internal.DataImportWizardBuilder;
import com.rapidminer.studio.io.gui.internal.DataImportWizardUtils;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.PasswordInputCanceledException;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


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
					return copyOrMoveRepositoryEntries(droppedOnEntry, Collections.singletonList(location), ts);

				} else if (flavors.contains(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR)) {
					// Multiple repository entries

					Object transferData = ts.getTransferable()
							.getTransferData(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR);
					List<RepositoryLocation> locations;
					if (transferData instanceof RepositoryLocationList) {
						locations = ((RepositoryLocationList) transferData).getAll();
					} else if (transferData instanceof RepositoryLocation[]) {
						locations = Arrays.asList((RepositoryLocation[]) transferData);
					} else {
						// should not happen
						return false;
					}
					return copyOrMoveRepositoryEntries(droppedOnEntry, locations, ts);

				} else if (flavors.contains(DataFlavor.javaFileListFlavor)) {
					// Import file via wizard

					@SuppressWarnings("unchecked")
					List<File> files = (List<File>) ts.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
					File file = files.get(0);
					DataImportWizardBuilder builder = new DataImportWizardBuilder();
					builder.setCallback(DataImportWizardUtils.showInResultsCallback());
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
		private boolean copyOrMoveRepositoryEntries(Entry droppedOnEntry, final List<RepositoryLocation> locations,
				final TransferSupport ts) throws RepositoryException {

			final Folder droppedOnFolder;
			if (droppedOnEntry instanceof Folder) {
				droppedOnFolder = (Folder) droppedOnEntry;
			} else if (isMoveOperation(ts, false)) {
				// ignore move operations that don't target a folder
				return false;
			} else {
				// use parent folder for copy operations
				droppedOnFolder = droppedOnEntry.getContainingFolder();
			}

			// First check, if operation is allowed for all locations
			for (RepositoryLocation location : locations) {
				if (!copyOrMoveCheck(droppedOnFolder, location, ts)) {
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

					TreePath droppedOnPath = getModel().getPathTo(droppedOnFolder);

					// Initialize progress listener
					getProgressListener().setTotal(locations.size() * PROGRESS_LISTENER_SINGLE_STEP_SIZE);
					getProgressListener().setCompleted(progressListenerCompleted);

					final UserDecisions userDecisions = new UserDecisions();
					locationLoop: for (RepositoryLocation location : locations) {

						// Single entry check
						try {
							// Entry already exists, overwrite?
							final String effectiveNewName = location.locateEntry().getName();
							if (droppedOnFolder.containsEntry(effectiveNewName)) {

								// Do not confuse user with incorrect selected paths
								RepositoryTree.this.setSelectionPath(droppedOnPath);

								if (!userDecisions.repeatDecision) {

									final List<String> optionsToSelect = new ArrayList<>(3);
									optionsToSelect.add("existing_entry.insert");
									optionsToSelect.add("existing_entry.overwrite");
									optionsToSelect.add("existing_entry.skip");
									final List<String> optionsToCheck = new ArrayList<>(1);
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
						boolean done = executeCopyOrMoveOperation(location, droppedOnFolder, isRepositoryInLocations, isSingleEntryOperation,
								userDecisions.overwriteIfExists);
						if (!done) {
							break;
						}

						progressListenerCompleted += PROGRESS_LISTENER_SINGLE_STEP_SIZE;
					}

					// On multi-operations, select the target folder after finishing copy/move
					// operation
					if (droppedOnFolder != null && !isSingleEntryOperation) {
						SwingUtilities.invokeLater(() -> {
							TreePath droppedOnPath1 = getModel().getPathTo(droppedOnFolder);
							RepositoryTree.this.setSelectionPath(droppedOnPath1);
							RepositoryTree.this.scrollPathToVisible(droppedOnPath1);
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
				private boolean executeCopyOrMoveOperation(RepositoryLocation location, Folder target, boolean isRepositoryInLocations,
						boolean isSingleEntryOperation, boolean overwriteIfExists) {
					try {
						ProgressListener progressListener = new RescalingProgressListener(getProgressListener(), progressListenerCompleted,
								progressListenerCompleted + PROGRESS_LISTENER_SINGLE_STEP_SIZE);

						if (isMoveOperation(ts, isRepositoryInLocations)) {
							RepositoryManager.getInstance(null).move(location, target, null,
									overwriteIfExists, progressListener);

							// On drag and drop move operation with overwrite, two delete operations
							// are performed. This results in a selection of a wrong tree element.
							// For this case, select the element, which is the target of the drop
							// operation.
							if (overwriteIfExists && target != null) {
								SwingUtilities.invokeLater(() -> RepositoryTree.this.setSelectionPath(getModel().getPathTo(target)));
							}

						} else {
							RepositoryManager.getInstance(null).copy(location, target, null,
									overwriteIfExists, progressListener);
						}
					} catch (RepositoryConnectionsFolderImmutableException | RepositoryStoreOtherInConnectionsFolderException | RepositoryConnectionsNotSupportedException e) {
						// this happens when trying to modify/move/delete the connections folder or when trying to store other things inside a connection folder
						String errorKey;
						if (e instanceof RepositoryConnectionsFolderImmutableException) {
							errorKey = "error_modify_connections_folder";
						} else if (e instanceof RepositoryStoreOtherInConnectionsFolderException) {
							errorKey = "error_copy_other_to_connections_folder";
						} else {
							errorKey = "error_connections_not_supported";
						}
						final String locationName = location.getName() == null ? "" : location.getName();
						final AtomicInteger result = new AtomicInteger();

						if (isSingleEntryOperation) {
							SwingTools.invokeAndWait(() -> SwingTools.showVerySimpleErrorMessage(ProgressThreadDialog.getInstance(), errorKey, locationName));
						} else {
							SwingTools.invokeAndWait(() -> {
								final ConfirmDialog dialog = new ConfirmDialog(ProgressThreadDialog.getInstance(), errorKey,
										ConfirmDialog.OK_CANCEL_OPTION, false, locationName);
								dialog.setVisible(true);
								result.set(dialog.getReturnOption());
							});
							if (result.get() == ConfirmDialog.CANCEL_OPTION) {
								// user cancels whole copy operation. No need to log, as this is not an unexpected error state
								return false;
							}
						}
					} catch (RepositoryNotConnectionsFolderException e) {
						// this happens when trying to move a connection into a non-connection special folder AND that repository knows about this
						// we offer to automatically move them to the appropriate connections folder for that repository instead
						final int dialogMode = isSingleEntryOperation ? ConfirmDialog.YES_NO_OPTION : ConfirmDialog.YES_NO_CANCEL_OPTION;
						final String locationName = location.getName() == null ? "" : location.getName();
						final AtomicInteger result = new AtomicInteger();
						Folder repoConnectionsFolder = null;
						try {
							repoConnectionsFolder = RepositoryTools.getConnectionFolder(target.getLocation().getRepository());
						} catch (RepositoryException e1) {
							LogService.getRoot().log(Level.WARNING,
									"com.rapidminer.repository.RepositoryTree.error_resolving_connections_folder", e1);
						}

						if (repoConnectionsFolder != null) {
							String connLocRepo = repoConnectionsFolder.getLocation().getRepositoryName();
							SwingTools.invokeAndWait(() -> {
								final ConfirmDialog dialog = new ConfirmDialog(ProgressThreadDialog.getInstance(), "error_copy_to_non_connections_folder",
										dialogMode, false, e.getMessage(), locationName, connLocRepo);
								dialog.setVisible(true);
								result.set(dialog.getReturnOption());
							});

							// user wants to automatically target the connection to the connections folder, then do it
							// otherwise, we do nothing as logging is pointless for a non-error scenario
							if (result.get() == ConfirmDialog.YES_OPTION) {
								return executeCopyOrMoveOperation(location, repoConnectionsFolder, isRepositoryInLocations, isSingleEntryOperation,
										overwriteIfExists);
							}
						} else {
							// if we for whatever reason cannot resolve the repository connections folder, we go to the regular error handling
							return handleRepoException(e, location, target, isRepositoryInLocations, isSingleEntryOperation, overwriteIfExists);
						}
					} catch (RepositoryException e) {
						return handleRepoException(e, location, target, isRepositoryInLocations, isSingleEntryOperation, overwriteIfExists);
					}
					return true;
				}

				/**
				 * Handles a repository exception during copy/move.
				 * @return {@code true} if user pressed retry and it worked OR if he pressed no-retry; {@code false} if user pressed cancel
				 */
				private boolean handleRepoException(RepositoryException e, RepositoryLocation location, Folder target, boolean isRepositoryInLocations, boolean isSingleEntryOperation, boolean overwriteIfExists) {
					if (e.getCause() != null && e.getCause() instanceof PasswordInputCanceledException) {
						// no extra dialog if login dialog was canceled
						return false;
					}
					// Do not show "cancel" option, if is is an single entry operation
					final int dialogMode = isSingleEntryOperation ? ConfirmDialog.YES_NO_OPTION : ConfirmDialog.YES_NO_CANCEL_OPTION;
					final String locationName = location.getName() == null ? "" : location.getName();
					final AtomicInteger result = new AtomicInteger();

					SwingTools.invokeAndWait(() -> {
						final ConfirmDialog dialog;
						if (e.getMessage() != null && !e.getMessage().isEmpty()) {
							dialog = new ConfirmDialog(ProgressThreadDialog.getInstance(), "error_in_copy_entry_with_cause",
									dialogMode, false, locationName, e.getMessage());
						} else {
							dialog = new ConfirmDialog(ProgressThreadDialog.getInstance(), "error_in_copy_entry",
									dialogMode, false, locationName);
						}
						dialog.setVisible(true);
						result.set(dialog.getReturnOption());
					});

					if (result.get() == ConfirmDialog.YES_OPTION) {
						return executeCopyOrMoveOperation(location, target, isRepositoryInLocations, isSingleEntryOperation,
								overwriteIfExists);

					} else if (result.get() == ConfirmDialog.CANCEL_OPTION) {
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.repository.RepositoryTree.error_during_copying", e);
						return false;

					} else {
						// user pressed "no-retry", so log and return true
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.repository.RepositoryTree.error_during_copying", e);
						return true;
					}
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
				RepositoryLocation targetLocation = droppedOnEntry.getLocation();
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
				if (locationEntry instanceof Repository) {
					SwingTools.showVerySimpleErrorMessage("repository_copy_repository");
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
			final List<Entry> selectedEntries = getSelectedEntries();
			if (selectedEntries.isEmpty()) {
				// Nothing selected
				return null;
			}

			List<RepositoryLocation> locationList = getSelectedEntries().stream().map(Entry::getLocation).collect(Collectors.toList());
			RepositoryLocation[] repositoryLocations = RepositoryLocation.removeIntersectedLocations(locationList).toArray(new RepositoryLocation[0]);
			TransferableRepositoryEntry transferable = new TransferableRepositoryEntry(repositoryLocations);
			transferable.setUsageStatsLogger(
					() -> ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_REPOSITORY_TREE, "inserted", null));
			return transferable;
		}

		@Override
		public Icon getVisualRepresentation(Transferable t) {
			return null;
		}

		/**
		 * Checks, if the current operation is a MOVE operation. If not, it is a COPY operation.
		 *
		 * @param ts
		 * 		Provides info about the current operation
		 * @param isRepositoryInLocations
		 * 		This is true, if the sources to copy contain a repository. Repositories can
		 * 		not be moved. This results in a copy operation.
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

		private Class<? extends AbstractRepositoryAction<?>> actionClass;

		private RepositoryActionCondition condition;

		private boolean hasSeparatorBefore;

		private boolean hasSeparatorAfter;

		public RepositoryActionEntry(Class<? extends AbstractRepositoryAction<?>> actionClass,
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

		public Class<? extends AbstractRepositoryAction<?>> getRepositoryActionClass() {
			return actionClass;
		}
	}

	private final Dialog owner;

	public final AbstractRepositoryAction<Entry> RENAME_ACTION = new RenameRepositoryEntryAction(this);

	public final AbstractRepositoryAction<Entry> DELETE_ACTION = new DeleteRepositoryEntryAction(this);

	public final AbstractRepositoryAction<DataEntry> OPEN_ACTION = new OpenEntryAction(this);

	public final AbstractRepositoryAction<Entry> REFRESH_ACTION = new RefreshRepositoryEntryAction(this);

	public final AbstractRepositoryAction<Folder> CREATE_FOLDER_ACTION = new CreateFolderAction(this);

	public final ResourceActionAdapter SHOW_PROCESS_IN_REPOSITORY_ACTION = new ShowProcessInRepositoryAction(this);

	private List<AbstractRepositoryAction<?>> listToEnable = new ArrayList<>();

	private EventListenerList listenerList = new EventListenerList();

	// must be after the treeModel, since they require it to be initialized
	final ToggleAction SORT_BY_NAME_ACTION = new SortByNameAction(this);
	final ToggleAction SORT_BY_LAST_MODIFIED_DATE_ACTION = new SortByLastModifiedAction(this);

	private static final long serialVersionUID = -6613576606220873341L;

	private static final List<RepositoryActionEntry> REPOSITORY_ACTIONS = new ArrayList<>();

	/** List of actions available for multiple entries selected in menu */
	private static final List<RepositoryActionEntry> REPOSITORY_MULTIPLE_ENTRIES_ACTIONS = new ArrayList<>();

	/** Configuration: List of actions available for multiple entries selected in menu */
	private static final List<String> multipleMenuEntriesActionClasses = new ArrayList<>(
			Arrays.asList(CutEntryRepositoryAction.class.getName(), CopyEntryRepositoryAction.class.getName(),
					DeleteRepositoryEntryAction.class.getName(), RefreshRepositoryEntryAction.class.getName()));

	private final int TREE_ROW_HEIGHT = 24;

	static {
		addRepositoryAction(ConfigureRepositoryAction.class, new RepositoryActionConditionImplConfigRepository(), false, true);
		addRepositoryAction(OpenEntryAction.class,
				new RepositoryActionConditionImplStandard(new Class<?>[]{DataEntry.class}), false, false);
		addRepositoryAction(StoreProcessAction.class,
				new RepositoryActionConditionAdditionallyNotConnections(new Class<?>[]{ProcessEntry.class, Folder.class},
						true, true), false, false);
		addRepositoryAction(RenameRepositoryEntryAction.class,
				new RepositoryActionConditionAdditionallyNotConnections(new Class<?>[]{Entry.class}, false), false, false);
		addRepositoryAction(EditConnectionAction.class, new RepositoryActionConditionImplStandard(new Class<?>[]{ConnectionEntry.class}, new Class<?>[]{LocalRepository.class, RemoteRepository.class}), false,
				false);
		addRepositoryAction(CreateConnectionAction.class, new RepositoryActionConditionRepositoryAndConnections(new Class<?>[]{Folder.class}), false,
				false);
		addRepositoryAction(CreateFolderAction.class,
				new RepositoryActionConditionAdditionallyNotConnections(new Class<?>[]{Folder.class}, true, true), false, false);
		addRepositoryAction(CutEntryRepositoryAction.class,
				new RepositoryActionConditionAdditionallyNotConnections(new Class<?>[0], false), true, false);
		addRepositoryAction(CopyEntryRepositoryAction.class,
				new RepositoryActionConditionAdditionallyNotConnections(new Class<?>[0], false), false, false);
		addRepositoryAction(PasteEntryRepositoryAction.class,
				new RepositoryActionConditionImplStandard(new Class<?>[0]), false, false);
		addRepositoryAction(CopyLocationAction.class,
				new RepositoryActionConditionImplStandard(new Class<?>[0]), false, false);
		addRepositoryAction(DeleteRepositoryEntryAction.class,
				new RepositoryActionConditionAdditionallyNotConnections(new Class<?>[]{Entry.class}, true), false, false);
		addRepositoryAction(RefreshRepositoryEntryAction.class,
				new RepositoryActionConditionImplStandard(new Class<?>[]{Entry.class}), true, false);
		addRepositoryAction(OpenInFileBrowserAction.class, new RepositoryActionConditionImplStandard(
				new Class<?>[]{Entry.class}, new Class<?>[]{LocalRepository.class}), false, false);
		addRepositoryAction(CheckProcessCompatibility.class,
				new RepositoryActionConditionImplStandard(new Class<?>[]{ProcessEntry.class}, new Class<?>[]{RemoteRepository.class}), true, true);
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

	public RepositoryTree(Dialog owner, boolean onlyFolders, boolean onlyWritableRepositories, boolean installDraghandler,
			final Color backgroundColor) {
		this(owner, onlyFolders, onlyWritableRepositories, installDraghandler, backgroundColor, null);
	}

	/**
	 * Create a new RepositoryTree, configure it directly in the constructor
	 *
	 * @param owner
	 * 		the dialog that is the owner of this tree, used when creating subdialogs
	 * @param onlyFolders
	 * 		if true only show repositories and folders, no entries
	 * @param onlyWritableRepositories
	 * 		if true only show writable repositories and their content
	 * @param installDraghandler
	 * 		when true, the {@link RepositoryTreeTransferhandler} is installed and the user is
	 * 		able to drag/drop data.
	 * @param backgroundColor
	 * 		if {@code null} the default background color will be used, otherwise the provided
	 * 		background color will be used
	 * @param predicate
	 * 		if supplied only those repositories, folders and entries will be shown that match the predicate
	 */
	public RepositoryTree(Dialog owner, boolean onlyFolders, boolean onlyWritableRepositories, boolean installDraghandler,
						  final Color backgroundColor, Predicate<Entry> predicate) {
		super(new RepositoryTreeModel(RepositoryManager.getInstance(null), onlyFolders, onlyWritableRepositories, predicate));
		this.owner = owner;
		getModel().setParentTree(this);
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
				}

				@Override
				public Color getBackgroundNonSelectionColor() {
					return backgroundColor;
				}
			});
		} else {
			setCellRenderer(new RepositoryTreeCellRenderer());
		}
		getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

		addTreeSelectionListener(e -> {
			if (e.getSource() instanceof JTree) {
				JTree jtree = (JTree) e.getSource();
				jtree.repaint();
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
						if (path != null && path.getLastPathComponent() instanceof Entry) {
							fireLocationSelected((Entry) path.getLastPathComponent());
						}
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

		getSelectionModel().addTreeSelectionListener(e -> enableActions());

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
				return (o instanceof Entry) ? ToolTipProviderHelper.getTip((Entry) o) : null;
			}

			@Override
			public Object getIdUnder(Point point) {
				TreePath path = getPathForLocation((int) point.getX(), (int) point.getY());
				return path == null ? null : path.getLastPathComponent();
			}

			@Override
			public Component getCustomComponent(Object o) {
				return (o instanceof Entry) ? ToolTipProviderHelper.getCustomComponent((Entry) o) : null;
			}
		}, this, TooltipLocation.RIGHT);

		if (backgroundColor != null) {
			setBackground(backgroundColor);
		}
	}

	@Override
	public RepositoryTreeModel getModel() {
		return (RepositoryTreeModel) super.getModel();
	}

	@Override
	public void setModel(TreeModel treeModel) {
		if (treeModel instanceof RepositoryTreeModel) {
			super.setModel(treeModel);
		}
	}

	public void enableActions() {
		listToEnable.forEach(AbstractRepositoryAction::enable);
	}

	public void addRepositorySelectionListener(RepositorySelectionListener listener) {
		listenerList.add(RepositorySelectionListener.class, listener);
	}

	public void removeRepositorySelectionListener(RepositorySelectionListener listener) {
		listenerList.remove(RepositorySelectionListener.class, listener);
	}

	/**
	 * Adds a {@link RepositorySortingMethodListener}
	 *
	 * @since 7.4
	 */
	public void addRepostorySortingMethodListener(RepositorySortingMethodListener l) {
		listenerList.add(RepositorySortingMethodListener.class, l);
	}

	/**
	 * Removes a {@link RepositorySortingMethodListener}
	 *
	 * @since 7.4
	 */
	public void removeRepostorySortingMethodListener(RepositorySortingMethodListener l) {
		listenerList.remove(RepositorySortingMethodListener.class, l);
	}

	private void fireLocationSelected(Entry entry) {
		RepositorySelectionEvent event = new RepositorySelectionEvent(entry);
		for (RepositorySelectionListener l : listenerList.getListeners(RepositorySelectionListener.class)) {
			l.repositoryLocationSelected(event);
		}
	}

	/**
	 * Similar as {@link #scrollPathToVisible(TreePath)}, but centers it instead. Used for explicit highlighting.
	 *
	 * @param path
	 * 		the path to center
	 * 	@since 8.1
	 */
	private void scrollPathToVisibleCenter(TreePath path) {
		if (path == null) {
			return;
		}

		// set y and height in a way that the path always appears in the center
		Rectangle bounds = getPathBounds(path);
		if (bounds == null) {
			return;
		}
		Rectangle visibleRect = getVisibleRect();
		// try to stay as far left as possible. If very deep folder structure, let Swing decide where to start
		if ((bounds.x + 50) < visibleRect.x + visibleRect.width) {
			bounds.x = 0;
		}
		bounds.y = bounds.y - visibleRect.height / 2;
		bounds.height = visibleRect.height;
		scrollRectToVisible(bounds);
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
			if (relativeTo != null) {
				RepositoryManager.getInstance(null).unhide(relativeTo.getRepositoryName());
			}
			TreePath pathTo = getModel().getPathTo(entry);
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
		scrollPathToVisibleCenter(getSelectionPath());
	}

	private void showPopup(MouseEvent e) {

		List<Entry> entries = getSelectedEntries();
		JPopupMenu menu = new JPopupMenu();

		if (entries.isEmpty()) {
			return;
		} else if (entries.size() == 1) {
			// Add actions
			List<Action> actionList = createContextMenuActions(this, entries);
			// Go through ordered list of actions and add them
			for (Action action : actionList) {
				if (action == null) {
					menu.addSeparator();
				} else {
					menu.add(action);
				}
			}

			// Append custom actions if there are any
			Collection<Action> customActions = entries.get(0).getCustomActions();
			if (customActions != null && !customActions.isEmpty()) {
				menu.addSeparator();
				for (Action customAction : customActions) {
					menu.add(customAction);
				}
			}

		} else {
			// Multiple items selected

			// Get all selected entries
			List<Entry> selectedEntries = getSelectedEntries();

			// Get possible actions of selected entries
			List<RepositoryActionEntry> evaluatedActions = REPOSITORY_MULTIPLE_ENTRIES_ACTIONS.stream()
					.filter(actionEntry -> actionEntry.getRepositoryActionCondition().evaluateCondition(selectedEntries))
			        .collect(Collectors.toList());

			// Add actions
			boolean lastWasSeparator = true;
			for (RepositoryActionEntry actionEntry : evaluatedActions) {

				try {
					Constructor<? extends AbstractRepositoryAction<?>> constructor = actionEntry.getRepositoryActionClass()
							.getConstructor(RepositoryTree.class);
					AbstractRepositoryAction<?> createdAction = constructor.newInstance(this);
					createdAction.enable();

					if (actionEntry.hasSeparatorBefore && !lastWasSeparator) {
						menu.addSeparator();
					}
					menu.add(createdAction);
					if (actionEntry.hasSeparatorAfter) {
						menu.addSeparator();
					}
					lastWasSeparator = actionEntry.hasSeparatorAfter;
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
		List<Entry> selectedEntries = new ArrayList<>();
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
		List<AbstractRepositoryAction<?>> listOfAbstractRepositoryActions = new ArrayList<>();
		for (Action action : createContextMenuActions(this, new ArrayList<>())) {
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
	 */
	public static void addRepositoryAction(Class<? extends AbstractRepositoryAction<?>> actionClass,
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
	 */
	public static void addRepositoryAction(Class<? extends AbstractRepositoryAction<?>> actionClass,
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
			// searching for pos to insert after
			int insertPos = 1 + REPOSITORY_ACTIONS.stream()
					.filter(e -> insertAfterThisAction.equals(e.getRepositoryActionClass()))
					.findFirst().map(REPOSITORY_ACTIONS::indexOf)
					.orElse(REPOSITORY_ACTIONS.size() - 1);
			REPOSITORY_ACTIONS.add(insertPos, newEntry);
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
	public static void removeRepositoryAction(Class<? extends AbstractRepositoryAction<?>> actionClass) {
		REPOSITORY_ACTIONS.removeIf(repositoryActionEntry -> repositoryActionEntry.getRepositoryActionClass().equals(actionClass));
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
		List<Action> listOfActions = new ArrayList<>();
		boolean lastWasSeparator = true;

		for (RepositoryActionEntry actionEntry : REPOSITORY_ACTIONS) {
			try {
				if (actionEntry.getRepositoryActionCondition().evaluateCondition(entryList)) {
					if (!lastWasSeparator && actionEntry.hasSeperatorBefore()) {
						// add null element which means a separator will be added in the menu
						listOfActions.add(null);
					}
					Constructor<? extends AbstractRepositoryAction<?>> constructor = actionEntry.getRepositoryActionClass()
							.getConstructor(RepositoryTree.class);
					AbstractRepositoryAction<?> createdAction = constructor.newInstance(repositoryTree);
					createdAction.enable();
					listOfActions.add(createdAction);
					if (actionEntry.hasSeperatorAfter()) {
						listOfActions.add(null);
					}
					lastWasSeparator = actionEntry.hasSeperatorAfter();
				}
			} catch (Exception e) {
				LogService.getRoot().log(Level.SEVERE,
						"com.rapidminer.repository.gui.RepositoryTree.creating_repository_action_error",
						actionEntry.getRepositoryActionClass());
			}
		}
		return listOfActions;
	}

	/**
	 * Sets the {@link RepositorySortingMethod} with which the {@link RepositoryTreeModel} is sorted
	 *
	 * @param method
	 *            The {@link RepositorySortingMethod}
	 * @since 7.4
	 */
	public void setSortingMethod(RepositorySortingMethod method) {
		// Remember expansion state before setting new RepositorySortingMethod
		Enumeration<TreePath> expandedDescendants = getExpandedDescendants(new TreePath(getModel().getRoot()));
		expandedDescendants = expandedDescendants == null ? Collections.emptyEnumeration() : expandedDescendants;

		getModel().setSortingMethod(method);

		while (expandedDescendants.hasMoreElements()) {
			setExpandedState(expandedDescendants.nextElement(), true);
		}
		for (RepositorySortingMethodListener l : listenerList.getListeners(RepositorySortingMethodListener.class)) {
			l.changedRepositorySortingMethod(method);
		}
	}

	/**
	 * Gets the {@link RepositorySortingMethod} with which this {@link RepositoryTreeModel} is
	 * sorted
	 *
	 * @since 7.4
	 */
	public RepositorySortingMethod getSortingMethod() {
		return getModel().getSortingMethod();
	}

}
