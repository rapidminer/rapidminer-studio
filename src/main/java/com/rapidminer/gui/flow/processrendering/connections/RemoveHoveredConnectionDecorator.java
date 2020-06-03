/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.gui.flow.processrendering.connections;

import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.operator.ports.OutputPort;


/**
 * Decorator that shows a trash can icon on a connection if the connection is hovered over and
 * deletes the connection if the icon is clicked. The icon is highlighted if the mouse is moved onto
 * it.
 *
 * @author Nils Woehler
 * @since 7.1.0
 */
public final class RemoveHoveredConnectionDecorator extends AbstractRemoveConnectionDecorator {

	public RemoveHoveredConnectionDecorator(ProcessRendererModel rendererModel) {
		super(rendererModel);
	}

	@Override
	protected OutputPort getSource() {
		return rendererModel.getHoveringConnectionSource();
	}

	@Override
	protected boolean isEligible(OutputPort source) {
		// check that connection is not hovered indirectly
		return source != rendererModel.getSelectedConnectionSource() && rendererModel.getHoveringPort() == null
				&& rendererModel.getHoveringOperator() == null;
	}
}
