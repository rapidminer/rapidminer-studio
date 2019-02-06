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

import java.awt.Component;

import com.rapidminer.gui.viewer.collection.CollectionViewer;
import com.rapidminer.operator.GroupedModel;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.report.Reportable;


/**
 * A renderer for container models.
 *
 * @author Ingo Mierswa
 */
public class CollectionRenderer extends NonGraphicalRenderer {

	@Override
	public String getName() {
		return "Collection";
	}

	@Override
	@SuppressWarnings("unchecked")
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		if (renderable instanceof GroupedModel) {
			GroupedModel cm = (GroupedModel) renderable;
			return new CollectionViewer(cm, ioContainer);
		} else if (renderable instanceof IOObjectCollection) {
			return new CollectionViewer((IOObjectCollection<IOObject>) renderable, ioContainer);
		} else {
			throw new IllegalArgumentException(
					"Renderable must be GroupedModel or IOObjectCollection, but is " + renderable.getClass());
		}
	}

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer) {
		return new DefaultReadable(renderable.toString());
	}
}
