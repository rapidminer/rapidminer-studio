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
 * This interface should be implemented by {@link Component}s which should be exported if it focused
 * and an export as image action is called. If a subclass of {@link AbstractPrintComponentAction}
 * finds a {@link Component} that implements this interface, only the {@link Component} returned by
 * {@link #getComponent()} will be exported/printed.
 * 
 * @author Nils Woehler
 * 
 */
public interface PrintableComponent {

	/**
	 * @return the component that should be exported/printed. Can be the implementing component
	 *         itself or a subcomponent of the implementing component. Can also return
	 *         <code>null</code> if exporting/printing of component is currently not supported.
	 */
	public Component getExportComponent();

	/**
	 * @return the name that should be used when exporting. Must not be <code>null</code>.
	 */
	public String getExportName();

	/**
	 * @return the identifier that will be shown below the export name in the export dialog. Can be
	 *         be <code>null</code>.
	 */
	public String getIdentifier();

	/**
	 * @return the name of the 24x24 icon that is displayed in the print dialog. Must not be
	 *         <code>null</code>. An icon with the specified name must be present in the icons/24
	 *         folder.
	 */
	public String getExportIconName();

	/**
	 * @return <code>true</code> if the exported component currently is shown on screen.
	 */
	public boolean isShowing();

}
