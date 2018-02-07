/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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
package com.rapidminer.gui.processeditor.results;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.ResultObject;
import com.vlsolutions.swing.docking.Dockable;


/**
 * Superclass of alternative result displays.
 * 
 * @author Simon Fischer
 * 
 */
public interface ResultDisplay extends Dockable, ProcessEditor {

	public static final String RESULT_DOCK_KEY = "result";

	/** Initializer called after the main frame is set up. */
	public void init(MainFrame mainFrame);

	public void showResult(ResultObject result);

	public void showData(final IOContainer resultContainer, final String message);

	public void addDataTable(DataTable dataTable);

	public void clearAll();

}
