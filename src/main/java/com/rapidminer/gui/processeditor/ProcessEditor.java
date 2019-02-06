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
import com.rapidminer.operator.Operator;

import java.util.EventListener;
import java.util.List;


/**
 * <p>
 * <i>Should no longer be used, see {@link ExtendedProcessEditor} instead.</i>
 * </p>
 * Interface for a GUI component that can display and/or edit an process. (e.g. the tree, the
 * xml-text...) Thus, several views on the process can be added to a tabbed pane. The methods of
 * this interface are mainly used to perform checks and to notify that the process has changed.
 * 
 * @author Ingo Mierswa
 * @see {@link ExtendedProcessEditor}
 */
public interface ProcessEditor extends EventListener {

	/** Notifies the component that the entire process has changed. */
	public void processChanged(Process process);

	/** Sets the currently selected operator. */
	public void setSelection(List<Operator> selection);

	/** Notifies the component that process was updated in some way. */
	public void processUpdated(Process process);
}
