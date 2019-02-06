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
package com.rapidminer.gui.renderer;

import com.rapidminer.operator.IOContainer;
import com.rapidminer.report.Reportable;

import java.awt.Component;


/**
 * This is the abstract renderer superclass for all renderers which should simply use a Java
 * component. Basically, this class only exists to allow for dirty hacks and should not be used in
 * general.
 * 
 * @author Ingo Mierswa
 * @deprecated deprecated with 9.2.0, will be removed in a future release!
 */
@Deprecated
public class DefaultComponentRenderer extends AbstractRenderer {

	private String name;

	private Component component;

	public DefaultComponentRenderer(String name, Component component) {
		this.name = name;
		this.component = component;
	}

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		return new DefaultComponentRenderable(component);
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		return component;
	}
}
