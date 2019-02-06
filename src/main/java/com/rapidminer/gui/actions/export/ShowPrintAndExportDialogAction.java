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
package com.rapidminer.gui.actions.export;

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.gui.tools.PrintingTools;
import com.rapidminer.gui.tools.ResourceAction;


/**
 * Export action for {@link PrintableComponent}s. Executing this action shows the
 * {@link PrintAndExportDialog}.
 * 
 * @author Nils Woehler
 * 
 */
public class ShowPrintAndExportDialogAction extends ResourceAction {

	private static final long serialVersionUID = 1L;
	private transient PrintableComponent comp;

	/**
	 * Creates a export action for the single component provided.
	 * 
	 * @param comp
	 *            the component that should be printed.
	 * @param smallIcon
	 *            whether the action should have a small icon or not
	 */
	public ShowPrintAndExportDialogAction(PrintableComponent comp, boolean smallIcon) {
		this(smallIcon);
		this.comp = comp;
	}

	/**
	 * Creates an action for the current shown perspective. All found {@link PrintableComponent}
	 * will be listed and can be printed.
	 * 
	 * @param smallIcon
	 *            whether the action should have a small icon or not
	 */
	public ShowPrintAndExportDialogAction(boolean smallIcon) {
		super(smallIcon, "export_and_print");
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		List<PrintableComponent> components = new LinkedList<>();
		if (comp != null) {
			components.add(comp);
		} else {
			components = PrintingTools.findExportComponents();
		}
		PrintAndExportDialog dialog = new PrintAndExportDialog(components);
		dialog.setVisible(true);
	}

}
