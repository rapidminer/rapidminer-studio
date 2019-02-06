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
package com.rapidminer.gui.processeditor;

import com.rapidminer.Process;


/**
 * Extends the {@link ProcessEditor} interface and adds a method which is called when the user
 * changes the view on a process, e.g. when he enters/leaves a subprocess in the process design
 * panel.
 * 
 * @author Marco Boeck
 * 
 */
public interface ExtendedProcessEditor extends ProcessEditor {

	/**
	 * Notifies the listener that the view on a process was changed, e.g. when the user
	 * enters/leaves a subprocess in the process design view.
	 * 
	 * @param process
	 */
	public void processViewChanged(Process process);
}
