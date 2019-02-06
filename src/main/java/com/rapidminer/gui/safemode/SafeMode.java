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
package com.rapidminer.gui.safemode;

import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;


/**
 * Safe mode for RapidMiner. When {@link #launchStarts()} is called, a lock file is created. This
 * lock file is deleted in {@link #launchComplete()}. If RapidMiner starts, and the lock file still
 * exists when {@link #launchStarts()} is called, This class will ask the user whether they want to
 * go into safe mode, which disables loading of all plugins.
 * 
 * @author Simon Fischer
 * 
 */
public class SafeMode {

	private File lockFile;
	private boolean safeMode = false;

	public SafeMode() {
		lockFile = FileSystemService.getUserConfigFile("safeMode.lock");
	}

	/** Call at the beginning of the startup phase. */
	public void launchStarts() {
		if (lockFile.exists()) {
			// Lock file not deleted? Crashed during last startup
			LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.safemode.SafeMode.lock_left_behind", lockFile);
			safeMode = askForSafeMode();
			if (safeMode) {
				LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.safemode.SafeMode.entering_safe_mode");
			}
		}
		// Create file on startup
		try {
			lockFile.createNewFile();
		} catch (IOException e) {
			LogService.getRoot().log(
					Level.INFO,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.safemode.SafeMode.cannot_create_lock", lockFile), e);
		}
	}

	public void launchComplete() {
		if (lockFile.exists()) {
			// Delete lock on end of startup
			if (!lockFile.delete()) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.safemode.SafeMode.failed_to_delete_log_file",
						lockFile);
			}
		}
	}

	public boolean isSafeMode() {
		return safeMode;
	}

	private boolean askForSafeMode() {
		int result = SafeModeDialog.showSafeModeDialog("safemode.enter_safe_mode", ConfirmDialog.YES_NO_OPTION);
		return result == ConfirmDialog.YES_OPTION;
	}

}
