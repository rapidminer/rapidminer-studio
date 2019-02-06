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
package com.rapidminer.gui.autosave;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import com.rapidminer.FileProcessLocation;
import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.RapidMiner;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.processeditor.ExtendedProcessEditor;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;


/**
 * This class handles the automatic saving (and potential recovery after a Studio crash) of the
 * current process.
 *
 * @author Venkatesh Umaashankar, Marco Boeck
 *
 */
public class AutoSave {

	private static final String LOCATION_TYPE_REPOSITORY = "repository_object";
	private static final String LOCATION_TYPE_FILE = "file";
	private static final String LOCATION_TYPE_NONE = "none";

	private static final String PROPERTY_PROCESS_PATH = "autosave.process.path";
	private static final String PROPERTY_PROCESS_TYPE = "autosave.process.type";

	private Properties autoSaveProperties;
	private Path autoSavedProcessPropertiesPath;
	private Path autoSavedProcessPath;
	private UpdateQueue autoSaveQueue;
	private boolean autoSaveEnabled;
	private boolean isRecoveryProcessPresent;

	/**
	 * Initializes auto save functionality for current Studio session and also checks if an auto
	 * save exists.
	 */
	public void init() {
		// already initialized
		if (autoSaveEnabled) {
			return;
		}

		String rapidMinerDir = FileSystemService.getUserRapidMinerDir().getAbsolutePath();
		Path autosaveDir = null;
		try {
			autosaveDir = Paths.get(rapidMinerDir, "autosave");
			if (!Files.exists(autosaveDir)) {
				Files.createDirectory(autosaveDir);
			}

			autoSavedProcessPropertiesPath = autosaveDir.resolve("autosaved_process.properties");
			autoSavedProcessPath = autosaveDir.resolve("autosaved_process.xml");
			autoSaveQueue = new UpdateQueue("autosave-queue");
			autoSaveQueue.start();

			// all good, enable auto save
			this.autoSaveEnabled = true;
		} catch (IOException e1) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.autosave.AutoSave.dir_creation_failed");
			this.autoSaveEnabled = false;
			// we can't recover if we fail here, therefore return
			return;
		}

		autoSaveProperties = new Properties();
		// if properties file exists we have an auto saved process
		if (Files.exists(autoSavedProcessPropertiesPath)) {
			try (InputStream inputStream = new FileInputStream(autoSavedProcessPropertiesPath.toFile());
					Reader autoSavePropertiesReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
				if (Files.exists(autoSavedProcessPropertiesPath)) {
					autoSaveProperties.load(autoSavePropertiesReader);

					if (this.autoSaveEnabled) {
						// ask if user wants to recover his previous process
						isRecoveryProcessPresent = true;

					}
				}
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.autosave.AutoSave.access_failed", e);
				this.autoSaveEnabled = false;
				// no need to add listeners here, we cannot save anyway
				return;
			}
		}

		// add listener which auto saves the process on every change
		RapidMinerGUI.getMainFrame().addExtendedProcessEditor(new ExtendedProcessEditor() {

			@Override
			public void setSelection(List<Operator> selection) {
				// do nothing
			}

			@Override
			public void processUpdated(Process process) {
				saveProcess(process);
			}

			@Override
			public void processChanged(Process process) {
				saveProcess(process);
			}

			@Override
			public void processViewChanged(Process process) {
				// do nothing
			}
		});

		// called when RapidMiner has shutdown gracefully, in that case we can delete the autosave
		RapidMiner.addShutdownHook(new Runnable() {

			@Override
			public void run() {
				AutoSave.this.onShutdown();

			}
		});

	}

	/**
	 * Returns whether there is an autosaved process, which can be used for recovery. Should be
	 * called after {@link #init()}.
	 *
	 * @return {@code true} if there is an autosaved process for recovery
	 */
	public boolean isRecoveryProcessPresent() {
		return isRecoveryProcessPresent;
	}

	/**
	 * Returns the path of the autosaved process if the process has a path. Should be called after
	 * {@link #init()}.
	 *
	 * @return the path of the autosaved process or {@code null} if no path was associated to the
	 *         process
	 */
	public String getAutosavedPath() {
		String processPath = autoSaveProperties.getProperty(PROPERTY_PROCESS_PATH);
		return LOCATION_TYPE_NONE.equals(processPath) ? null : processPath;
	}

	/**
	 * Recovers the autosaved process, if present.
	 */
	public void recoverAutosavedProcess() {
		if (!isRecoveryProcessPresent()) {
			return;
		}
		String processType = autoSaveProperties.getProperty(PROPERTY_PROCESS_TYPE);
		String processPath = autoSaveProperties.getProperty(PROPERTY_PROCESS_PATH);

		ProcessLocation autoSaveProcessLocation = new FileProcessLocation(autoSavedProcessPath.toFile());
		ProcessLocation actualProcessLocation = null;
		if (processType.equals(LOCATION_TYPE_REPOSITORY)) {
			try {
				actualProcessLocation = new RepositoryProcessLocation(new RepositoryLocation(processPath));
			} catch (MalformedRepositoryLocationException e) {
				// in that case location just stays null
			}
		} else if (processType.equals(LOCATION_TYPE_FILE)) {
			actualProcessLocation = new FileProcessLocation(Paths.get(processPath).toFile());
		}

		// try restoring the process
		Process process = null;
		try {
			process = autoSaveProcessLocation.load(null);
		} catch (IOException | XMLException e) {
			// failed to recover process but can continue to auto save new ones
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.autosave.AutoSave.load_process_failed", e);
		}

		// if process successfully restored, open it in Studio
		if (process != null) {
				process.setProcessLocation(actualProcessLocation);
			if (actualProcessLocation != null) {
				RapidMinerGUI.getMainFrame().setOpenedProcess(process);
			} else {
				RapidMinerGUI.getMainFrame().setProcess(process, true);
			}
			process.updateNotify();

			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					RapidMinerGUI.getMainFrame().SAVE_ACTION.setEnabled(true);
				}
			});
		}
	}

	/**
	 * Save the given process as an auto save.
	 *
	 * @param process
	 *            the process to save
	 */
	private void saveProcess(final Process process) {
		// no need to do anything if auto save is disabled
		if (!autoSaveEnabled) {
			return;
		}

		// store saving in update queue to avoid multiple saves in a row
		this.autoSaveQueue.execute(new Runnable() {

			@Override
			public void run() {

				ProcessLocation processLocation = process.getProcessLocation();
				if (processLocation != null) {
					if (processLocation instanceof FileProcessLocation) {
						autoSaveProperties.put(PROPERTY_PROCESS_PATH,
								((FileProcessLocation) processLocation).getFile().getAbsolutePath());
						autoSaveProperties.put(PROPERTY_PROCESS_TYPE, LOCATION_TYPE_FILE);
					} else if (processLocation instanceof RepositoryProcessLocation) {
						autoSaveProperties.put(PROPERTY_PROCESS_PATH,
								((RepositoryProcessLocation) processLocation).getRepositoryLocation().getAbsoluteLocation());
						autoSaveProperties.put(PROPERTY_PROCESS_TYPE, LOCATION_TYPE_REPOSITORY);
					}
				} else {
					// process is not saved yet
					autoSaveProperties.put(PROPERTY_PROCESS_PATH, LOCATION_TYPE_NONE);
					autoSaveProperties.put(PROPERTY_PROCESS_TYPE, LOCATION_TYPE_NONE);
				}
				String processXML = process.getRootOperator().getXML(false);

				try (OutputStreamWriter infoWriter = new OutputStreamWriter(
						new FileOutputStream(autoSavedProcessPropertiesPath.toFile()), StandardCharsets.UTF_8);
						OutputStreamWriter processWriter = new OutputStreamWriter(
								new FileOutputStream(autoSavedProcessPath.toFile()), StandardCharsets.UTF_8)) {
					autoSaveProperties.store(infoWriter, null);

					processWriter.write(processXML);
					processWriter.flush();

					// process has been overwritten, we do not longer provide the recovery process
					isRecoveryProcessPresent = false;
				} catch (IOException e) {
					LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.autosave.AutoSave.dir_creation_failed", e);
					AutoSave.this.autoSaveEnabled = false;
				}

			}
		});
	}

	/**
	 * Indicate a graceful shutdown where deletes the auto save because it is not needed.
	 */
	private void onShutdown() {
		if (autoSaveEnabled) {
			try {
				Files.deleteIfExists(autoSavedProcessPropertiesPath);
				Files.deleteIfExists(autoSavedProcessPath);
			} catch (IOException e) {
				LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.autosave.AutoSave.deletion_failed", e);
			}
		}
	}

}
