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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.operator.ResultObject;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryLocationChooser;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.repository.gui.actions.EditConnectionAction;
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

	public static void open() {
		if (RapidMinerGUI.getMainFrame().close()) {
			String locationString = RepositoryLocationChooser.selectLocation(null, null, RapidMinerGUI.getMainFrame().getExtensionsMenu(), true,
					false);
			if (locationString != null) {
				try {
					RepositoryLocation location = new RepositoryLocation(locationString);
					Entry entry = location.locateEntry();
					if (entry instanceof ProcessEntry) {
						open(new RepositoryProcessLocation(location), true);
					} else if (entry instanceof ConnectionEntry) {
						showConnectionInformationDialog((ConnectionEntry) entry);
					} else if (entry instanceof IOObjectEntry) {
						showAsResult((IOObjectEntry) entry);
					} else {
						SwingTools.showVerySimpleErrorMessage("no_data_or_process");
					}
				} catch (MalformedRepositoryLocationException | RepositoryException e) {
					SwingTools.showSimpleErrorMessage("while_loading", e, locationString, e.getMessage());
				}
			}
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

	public static void open(String openLocation, boolean showInfo) {
		try {
			final RepositoryLocation location = new RepositoryLocation(openLocation);
			Entry entry = location.locateEntry();
			if (entry instanceof ProcessEntry) {
				open(new RepositoryProcessLocation(location), showInfo);
			} else if (entry instanceof ConnectionEntry) {
				showConnectionInformationDialog((ConnectionEntry) entry);
			} else if (entry instanceof IOObjectEntry) {
				showAsResult((IOObjectEntry) entry);
			} else {
				throw new RepositoryException("Cannot open entries of type " + entry.getType() + ".");
			}
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("while_loading", e, openLocation, e.getMessage());
		}
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
}
