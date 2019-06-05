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
package com.rapidminer.repository.gui.actions;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.gui.RepositoryTree;


/**
 * This action opens the selected entry.
 * 
 * @author Tobias Malbrecht, Sebastian Land
 */
public class OpenEntryAction extends AbstractRepositoryAction<DataEntry> {

	private static final long serialVersionUID = 1L;

	public OpenEntryAction(RepositoryTree tree) {
		super(tree, DataEntry.class, false, "open_repository_entry");
	}

	@Override
	public void actionPerformed(DataEntry data) {
		if (data instanceof ConnectionEntry){
			com.rapidminer.gui.actions.OpenAction.showConnectionInformationDialog((ConnectionEntry) data);
		} else if (data instanceof IOObjectEntry) {
			com.rapidminer.gui.actions.OpenAction.showAsResult((IOObjectEntry) data);
		} else if (data instanceof ProcessEntry) {
			RepositoryTree.openProcess((ProcessEntry) data);
		} else {
			SwingTools.showVerySimpleErrorMessage("no_data_or_process");
		}
	}

}
