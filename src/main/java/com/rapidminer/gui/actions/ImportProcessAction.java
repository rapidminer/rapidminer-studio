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

import com.rapidminer.FileProcessLocation;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;

import java.awt.event.ActionEvent;
import java.io.File;


/**
 * Opens a process from a file.
 * 
 * @author Simon Fischer
 * 
 */
public class ImportProcessAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	public ImportProcessAction() {
		super("import_process");
		setCondition(EDIT_IN_PROGRESS, DISALLOWED);
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		File file = SwingTools.chooseFile(RapidMinerGUI.getMainFrame(), "import_process", null, true, false, new String[] {
				RapidMiner.PROCESS_FILE_EXTENSION, "xml" }, new String[] { "Process File", "Process File" });
		if (file == null) {
			return;
		}
		open(file);
	}

	public static void open(final File file) {
		OpenAction.open(new FileProcessLocation(file), true);
	}
}
