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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.SwingUtilities;

import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.operator.ResultObject;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryLocationBuilder;
import com.rapidminer.repository.RepositoryLocationType;
import com.rapidminer.repository.gui.OpenBinaryEntryActionRegistry;
import com.rapidminer.repository.gui.OpenBinaryEntryCallback;
import com.rapidminer.repository.gui.RepositoryLocationChooser;
import com.rapidminer.repository.gui.actions.EditConnectionAction;
import com.rapidminer.repository.gui.actions.OpenInOperatingSystemAction;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;


/**
 * Start the corresponding action.
 *
 * @author Ingo Mierswa
 */
public class OpenAction extends ResourceAction {

	private static final long serialVersionUID = -323403851840397447L;

	public OpenAction() {
		super("open");

		setCondition(EDIT_IN_PROGRESS, DONT_CARE);
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		open();
	}

	/**
	 * Loads the data held by the given entry (in the background) and opens it as a result.
	 */
	public static void showAsResult(final IOObjectEntry data) {
		if (data == null) {
			throw new IllegalArgumentException("data entry must not be null");
		}
		final ProgressThread downloadProgressThread = new ProgressThread("download_from_repository") {

			@Override
			public void run() {
				try {
					ResultObject result = (ResultObject) data.retrieveData(this.getProgressListener());
					if (isCancelled()) {
						return;
					}
					result.setSource(data.getLocation().toString());
					RapidMinerGUI.getMainFrame().getResultDisplay().showResult(result);
				} catch (Exception e1) {
					SwingTools.showSimpleErrorMessage("cannot_fetch_data_from_repository", e1);
				}
			}
		};
		downloadProgressThread.start();
	}

	/**
	 * Asks the user via repository location chooser dialog what to open.
	 *
	 * @deprecated since 9.7, because it cannot distinguish between different {@link DataEntry} types with the same
	 * prefix. Use {@link #open(DataEntry, boolean)} instead!
	 */
	@Deprecated
	public static void open() {
		if (RapidMinerGUI.getMainFrame().close()) {
			String locationString = RepositoryLocationChooser.selectLocation(null, null, RapidMinerGUI.getMainFrame().getExtensionsMenu(), true,
					false);
			if (locationString != null) {
				try {
					RepositoryLocation location = new RepositoryLocationBuilder().withLocationType(RepositoryLocationType.DATA_ENTRY).buildFromAbsoluteLocation(locationString);
					DataEntry entry = location.locateData();
					open(entry, true);
				} catch (MalformedRepositoryLocationException | RepositoryException e) {
					SwingTools.showSimpleErrorMessage("while_loading", e, locationString, e.getMessage());
				}
			}
		}
	}

	/**
	 * @deprecated since 9.7, because it cannot distinguish between different {@link DataEntry} types with the same
	 * prefix. Use {@link #open(DataEntry, boolean)} instead!
	 */
	@Deprecated
	public static void open(String openLocation, boolean showInfo) {
		try {
			final RepositoryLocation location = new RepositoryLocationBuilder().withLocationType(RepositoryLocationType.DATA_ENTRY).buildFromAbsoluteLocation(openLocation);
			DataEntry entry = location.locateData();
			open(entry, false);
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("while_loading", e, openLocation, e.getMessage());
		}
	}

	/**
	 * Tries to open the given entry. If it fails, displays an error message.
	 *
	 * @param entry                   the entry, if {@code null} an error message appears
	 * @param askBeforeOpeningProcess if {@code true}, a confirmation dialog pops up when the entry is a process entry
	 *                                to confirm with the user he wants the current process to be replaced. This is to
	 *                                avoid potential data loss due to overwriting the currently edited process which
	 *                                may be dirty
	 * @since 9.7
	 */
	public static void open(DataEntry entry, boolean askBeforeOpeningProcess) {
		try {
			if (entry instanceof ProcessEntry) {
				if (!askBeforeOpeningProcess || RapidMinerGUI.getMainFrame().close()) {
					open(new RepositoryProcessLocation(entry.getLocation()), false);
				}
			} else if (entry instanceof ConnectionEntry) {
				showConnectionInformationDialog((ConnectionEntry) entry);
			} else if (entry instanceof IOObjectEntry) {
				showAsResult((IOObjectEntry) entry);
			}  else if (entry instanceof BinaryEntry) {
				openBinaryEntryViaRegisteredActionOrInOperatingSystem((BinaryEntry) entry);
			} else if (entry == null) {
				SwingTools.showVerySimpleErrorMessage("data_is_missing");
			} else {
				SwingTools.showVerySimpleErrorMessage("unknown_type");
			}
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("while_loading", e, entry.getLocation(), e.getMessage());
		}
	}

	/**
	 * This method will open the process specified by the process location. If showInfo is true, the
	 * description of the process will be shown depending on the fact if this feature is enabled or
	 * disabled in the settings. So if you don't want to silently load a process, this should be
	 * true.
	 */
	public static void open(final ProcessLocation processLocation, final boolean showInfo) {
		// ask for confirmation before stopping the currently running process and opening another
		// one!
		if (RapidMinerGUI.getMainFrame().getProcessState() == Process.PROCESS_STATE_RUNNING
				|| RapidMinerGUI.getMainFrame().getProcessState() == Process.PROCESS_STATE_PAUSED) {
			if (SwingTools.showConfirmDialog("close_running_process",
					ConfirmDialog.YES_NO_OPTION) != ConfirmDialog.YES_OPTION) {
				return;
			}
		}
		RapidMinerGUI.getMainFrame().stopProcess();
		ProgressThread openProgressThread = new ProgressThread("open_file") {

			@Override
			public void run() {
				getProgressListener().setTotal(100);
				getProgressListener().setCompleted(10);
				try {
					final Process process = processLocation.load(getProgressListener());
					if (isCancelled()) {
						return;
					}
					process.setProcessLocation(processLocation);
					if (isCancelled()) {
						return;
					}
					SwingUtilities.invokeLater(() -> RapidMinerGUI.getMainFrame().setOpenedProcess(process));
				} catch (XMLException ex) {
					try {
						RapidMinerGUI.getMainFrame()
								.handleBrokenProxessXML(processLocation, processLocation.getRawXML(), ex);
					} catch (IOException e) {
						SwingTools.showSimpleErrorMessage("while_loading", e, processLocation, e.getMessage());
						return;
					}
				} catch (Exception e) {
					SwingTools.showSimpleErrorMessage("while_loading", e, processLocation, e.getMessage());
					return;
				} finally {
					getProgressListener().complete();
				}
			}
		};
		openProgressThread.start();
	}

	/**
	 * ConnectionManagement Frontend : show a dialog
	 *
	 * @param connectionEntry
	 * 		the entry to be shown
	 * @since 9.3
	 */
	public static void showConnectionInformationDialog(ConnectionEntry connectionEntry) {
		EditConnectionAction.editConnection(connectionEntry, false);
	}

	/**
	 * Tries to open the given binary entry via the {@link OpenBinaryEntryActionRegistry}. If nothing is registered for
	 * the suffix of that entry, will fall back to the {@link OpenInOperatingSystemAction}. This may or may not do
	 * anything, depends on OS and various other circumstances. Does so in an async fashion via a {@link
	 * ProgressThread}.
	 *
	 * @param entry the binary entry, must not be {@code null}
	 * @since 9.7
	 */
	public static void openBinaryEntryViaRegisteredActionOrInOperatingSystem(BinaryEntry entry) {
		if (entry == null) {
			throw new IllegalArgumentException("entry must not be null!");
		}

		// try the registry
		OpenBinaryEntryCallback callback = OpenBinaryEntryActionRegistry.getInstance().getCallback(entry.getSuffix());
		if (callback != null) {
			new ProgressThread("open_binary") {

				@Override
				public void run() {
					try {
						callback.openEntry(entry);
					} catch (Throwable t) {
						SwingTools.showSimpleErrorMessage("cannot_open_with_registry", t);
						LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.actions.OpenAction.open_binary_registry_error", t);
					}
				}
			}.start();
			return;
		}

		// nothing registered, fall back to opening via the OS
		OpenInOperatingSystemAction.openInOperatingSystem(entry);
	}
}
