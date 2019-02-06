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

import java.awt.Component;


/**
 * A simple printable component with no identifier.
 * 
 * @author Nils Woehler
 * 
 */
public class SimplePrintableComponent implements PrintableComponent {

	private final Component comp;
	private final String componentName;
	private String iconName;

	/**
	 * @param comp
	 *            the wrapped component.
	 * @param componentName
	 *            the component name. Must not be <code>null</code>.
	 * @param iconName
	 *            the name of an icon of size 32x132. Must not be <code>null</code>.
	 */
	public SimplePrintableComponent(Component comp, String componentName, String iconName) {
		this.comp = comp;
		this.componentName = componentName;
		this.iconName = iconName;
	}

	/**
	 * @param comp
	 *            the wrapped component.
	 * @param componentName
	 *            the component name. Must not be <code>null</code>.
	 */
	public SimplePrintableComponent(Component comp, String componentName) {
		this.comp = comp;
		this.componentName = componentName;
		this.iconName = "printer.png";
	}

	@Override
	public Component getExportComponent() {
		return comp;
	}

	@Override
	public String getExportName() {
		return componentName;
	}

	@Override
	public String getExportIconName() {
		return iconName;
	}

	@Override
	public String getIdentifier() {
		return null;
	}

	@Override
	public boolean isShowing() {
		return comp.isShowing();
	}

}
