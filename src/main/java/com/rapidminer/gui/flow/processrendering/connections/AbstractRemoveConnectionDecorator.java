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

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawDecorator;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessEventDecorator;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.ports.OutputPort;


/**
 * Abstract decorator that shows a trash can icon on a connection if the connection is eligible and deletes
 * the connection if the icon is clicked. The icon is highlighted if the mouse is moved onto it.
 *
 * @author Nils Wöhler, Jan Czogalla
 * @since 9.7
 */
public abstract class AbstractRemoveConnectionDecorator  implements ProcessDrawDecorator, ProcessEventDecorator {

	protected final ProcessRendererModel rendererModel;

	private boolean enableTrashSymbol = false;
	private Shape shape = null;
	private OutputPort from = null;

	public AbstractRemoveConnectionDecorator(ProcessRendererModel rendererModel) {
		this.rendererModel = rendererModel;
	}

	@Override
	public void draw(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel rendererModel) {
		draw(process, g2);
	}

	@Override
	public void print(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel rendererModel) {
		draw(process, g2);
	}

	/**
	 * @return the {@link OutputPort} of the connection to be decorated
	 * @since 9.7
	 */
	protected abstract OutputPort getSource();

	/**
	 * @param source the port to test
	 * @return {@code true} if the given port together with this decorator are eligible to display decoration
	 * @since 9.7
	 */
	protected boolean isEligible(OutputPort source) {
		return true;
	}

	/**
	 * Draw the decoration if conditions are fulfilled.
	 *
	 * @param process the enclosing execution unit
	 * @param g2 the graphics
	 * @since 9.7
	 */
	private void draw(final ExecutionUnit process, final Graphics2D g2) {
		OutputPort source = getSource();
		if (source == null || !process.getAllOutputPorts().contains(source)) {
			// source undefined or not part of the current subprocess
			return;
		}
		if (!isEligible(source)) {
			return;
		}
		if (rendererModel.getConnectingPortSource() != null) {
			// we are in the process of creating a new connection
			return;
		}

		from = source;
		shape = ConnectionDrawUtils.renderConnectionRemovalIcon(source, enableTrashSymbol, g2, rendererModel);
	}

	@Override
	public void processMouseEvent(ExecutionUnit process, MouseEventType type, MouseEvent e) {
		boolean isContained = shape != null && shape.contains(rendererModel.getMousePositionRelativeToProcess());
		if (type == MouseEventType.MOUSE_MOVED) {
			if (shape != null && enableTrashSymbol != isContained) {
				enableTrashSymbol = isContained;
				if (!isContained) {
					shape = null;
				}
				rendererModel.fireMiscChanged();
			}
			return;
		}
		if (type != MouseEventType.MOUSE_PRESSED || !SwingUtilities.isLeftMouseButton(e)
				|| !isContained || !from.isConnected()) {
			return;
		}
		from.disconnect();
		enableTrashSymbol = false;
		shape = null;
		// set from port back to normal size
		if (from.equals(rendererModel.getSelectedConnectionSource())) {
			rendererModel.setSelectedConnectionSource(null);
		}
		if (from.equals(rendererModel.getHoveringConnectionSource())) {
			rendererModel.setHoveringConnectionSource(null);
		}
		rendererModel.fireMiscChanged();
		e.consume();
	}

	@Override
	public void processKeyEvent(ExecutionUnit process, KeyEventType type, KeyEvent e) {
		// noop
	}
}
