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
package com.rapidminer.gui.tools.components;

import com.rapidminer.gui.actions.export.PrintableComponent;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.LogService;

import java.util.UUID;
import java.util.logging.Level;

import javax.swing.Icon;


/**
 * 
 * A {@link Card} that contains a printable component.
 * 
 * @author Nils Woehler
 * 
 */
public class PrintableComponentCard implements Card {

	private final PrintableComponent component;
	private final UUID uuid = UUID.randomUUID();
	private Icon icon;

	public PrintableComponentCard(PrintableComponent component) {
		this.component = component;
		this.icon = SwingTools.createIcon("24/" + component.getExportIconName());
		if (icon == null) {
			this.icon = SwingTools.createIcon("24/printer.png");
			LogService.getRoot().log(Level.INFO,
					"Could not find icon for printable component card with name " + component.getExportIconName());
		}
	}

	@Override
	public String getKey() {
		return uuid.toString();
	}

	@Override
	public String getTitle() {
		return component.getExportName();
	}

	@Override
	public String getTip() {
		return component.getIdentifier();
	}

	@Override
	public Icon getIcon() {
		return icon;
	}

	/**
	 * @return the {@link PrintableComponent} this card was created for.
	 */
	public PrintableComponent getPrintableComponent() {
		return component;
	}

	@Override
	public String getFooter() {
		return component.getIdentifier();
	}

}
