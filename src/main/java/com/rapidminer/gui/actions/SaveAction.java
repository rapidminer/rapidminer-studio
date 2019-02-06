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
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.DecisionRememberingConfirmDialog;


/**
 * Start the corresponding action.
 *
 * @author Ingo Mierswa, Marco Boeck
 */
public class SaveAction extends ResourceAction {

	private static final long serialVersionUID = -2226200404990114956L;

	/** key of the progress thread to save */
	public static final String SAVE_PROGRESS_KEY = "save_action";

	public SaveAction() {
		super("save");

		setCondition(EDIT_IN_PROGRESS, DONT_CARE);
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		saveAsync(RapidMinerGUI.getMainFrame().getProcess());
	}

	/**
	 * Saves the specified process to its {@link ProcessLocation}. If it has none, will open the
	 * SaveAs dialog. <br/>
	 * <strong>Note:</strong> This call executes in the calling thread. In other words, this would
	 * block the GUI if called from the EDT!
	 *
	 * @param process
	 *            the {@link Process} to save
	 * @return true on success, false on failure
	 */
	public static boolean save(final Process process) {
		return save(process, false);
	}

	/**
	 * Saves the specified process to its {@link ProcessLocation}. If it has none, will open the
	 * SaveAs dialog. <br/>
	 * <strong>Note:</strong> This call executes in the calling thread. In other words, this would
	 * block the GUI if called from the EDT!
	 *
	 * @param process
	 * 		the {@link Process} to save
	 * @param refreshProcessMetaData
	 * 		if {@code true}, the meta data of the process will be recalculated after successful save
	 * @return true on success, false on failure
	 * @since 8.2
	 */
	public static boolean save(final Process process, final boolean refreshProcessMetaData) {

		if (process.hasSaveDestination()) {
			if (confirmOverwriteWithNewVersion(process)) {
				// user wants to save
				// disable save action
				RapidMinerGUI.getMainFrame().SAVE_ACTION.setEnabled(false);
				boolean successful = true;
				try {
					process.save();
					// check if process has really been saved or user has pressed
					// cancel in saveAs dialog
					if (process.hasSaveDestination()) {
						RapidMinerGUI.useProcessFile(process);
						RapidMinerGUI.getMainFrame().processHasBeenSaved();

						// after successful save, if desired, force recalculation of meta data
						if (refreshProcessMetaData) {
							process.getRootOperator().transformMetaData();
						}
					}
				} catch (IOException e) {
					successful = false;
					SwingTools
							.showSimpleErrorMessage("cannot_save_process", e, process.getProcessLocation(), e.getMessage());
					// something went wrong, enable save action again
					RapidMinerGUI.getMainFrame().SAVE_ACTION.setEnabled(true);
				}
				return successful;
			} else {
				return false;
			}
		} else {
			// SaveAsAction.saveAs cannot be null since async=false
			return SaveAsAction.saveAs(process, false);
		}
	}

	/**
	 * Saves the specified process to its {@link ProcessLocation}. If it has none, will open the
	 * SaveAs dialog. <br/>
	 * <strong>Note:</strong> This call executes in a {@link ProgressThread} with the key
	 * {@link #SAVE_PROGRESS_KEY}. In other words, this method can return immediately!
	 *
	 * @param process
	 *            the {@link Process} to save
	 */
	public static void saveAsync(final Process process) {
		saveAsync(process, false);
	}

	/**
	 * Saves the specified process to its {@link ProcessLocation}. If it has none, will open the
	 * SaveAs dialog. <br/>
	 * <strong>Note:</strong> This call executes in a {@link ProgressThread} with the key
	 * {@link #SAVE_PROGRESS_KEY}. In other words, this method can return immediately!
	 *
	 * @param process
	 * 		the {@link Process} to save
	 * @param refreshProcessMetaData
	 * 		if {@code true}, the meta data of the process will be recalculated after successful save
	 * @since 8.2
	 */
	public static void saveAsync(final Process process, final boolean refreshProcessMetaData) {
		if (process.hasSaveDestination()) {
			if (confirmOverwriteWithNewVersion(process)) {
				// user wants to save
				// disable save action
				RapidMinerGUI.getMainFrame().SAVE_ACTION.setEnabled(false);
				// save in progressThread to execute asynchronously
				ProgressThread saveThread = new ProgressThread(SAVE_PROGRESS_KEY) {

					@Override
					public void run() {
						try {
							process.save();

							try {
								SwingUtilities.invokeAndWait(() -> {

									// check if process has really been saved or user has pressed cancel in saveAs dialog
									if (process.hasSaveDestination()) {
										RapidMinerGUI.useProcessFile(process);
										RapidMinerGUI.getMainFrame().processHasBeenSaved();
									}
								});
							} catch (InvocationTargetException | InterruptedException e) {
								// can be ignored here. To prevent a silent exception (due to a bug
								// in the called methods), print the stacktrace
								e.printStackTrace();
							}

							// after successful save, if desired, force recalculation of meta data
							if (refreshProcessMetaData && process.hasSaveDestination()) {
								process.getRootOperator().transformMetaData();
							}
						} catch (IOException ex) {
							SwingTools.showSimpleErrorMessage("cannot_save_process", ex, process.getProcessLocation(),
									ex.getMessage());
							// something went wrong, enable save action again
							RapidMinerGUI.getMainFrame().SAVE_ACTION.setEnabled(true);
						}
					}
				};
				// just in case always depent on ourself so we cannot execute multiple saves at the
				// same time
				saveThread.addDependency(SAVE_PROGRESS_KEY);
				saveThread.setIndeterminate(true);
				saveThread.setCancelable(false);
				saveThread.start();
			} else {
				return;
			}
		} else {
			SaveAsAction.saveAs(process);
			return;
		}
	}

	private static boolean confirmOverwriteWithNewVersion(Process process) {
		return !process.isProcessConverted()
				|| DecisionRememberingConfirmDialog.confirmAction("save_over_with_new_version",
						"rapidminer.gui.saveover_new_version");
	}
}
