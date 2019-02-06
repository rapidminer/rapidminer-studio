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

import com.rapidminer.gui.actions.export.ComponentPrinter;
import com.rapidminer.gui.actions.export.SimplePrintableComponent;
import com.rapidminer.gui.tools.PrintingTools;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.print.PrinterException;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa, Nils Woehler
 */
public class PrintAction extends ResourceAction {

	private static final long serialVersionUID = -9086092676881347047L;

	private final Component component;
	private final String componentName;
	private boolean canceled = false;

	public PrintAction(Component component, String componentName) {
		super("print", componentName);
		this.component = component;
		this.componentName = componentName;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		canceled = false;
		PrintingTools.getPrinterJob().setPrintable(
				new ComponentPrinter(new SimplePrintableComponent(component, componentName, "printer.png")));
		if (PrintingTools.getPrinterJob().printDialog()) {
			try {
				PrintingTools.getPrinterJob().print();
			} catch (PrinterException pe) {
				SwingTools.showSimpleErrorMessage("printer_error", pe);
			}
		} else {
			canceled = true;
		}
	}

	public boolean wasCanceled() {
		return canceled;
	}
}
