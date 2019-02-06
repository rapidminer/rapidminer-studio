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
 * Decorator that shows a trash can icon on a connection if the connection is selected and deletes
 * the connection if the icon is clicked. The icon is highlighted if the mouse is moved onto it.
 *
 * @author Nils Woehler
 * @since 7.1.0
 */
public final class RemoveSelectedConnectionDecorator implements ProcessDrawDecorator, ProcessEventDecorator {

	private final ProcessRendererModel rendererModel;

	private Shape shape = null;
	boolean enableTrashSymbol = false;
	boolean renderTrashSymbol = false;
	OutputPort from = null;

	public RemoveSelectedConnectionDecorator(ProcessRendererModel rendererModel) {
		this.rendererModel = rendererModel;
	}

	@Override
	public void draw(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel rendererModel) {
		draw(process, g2, false);
	}

	@Override
	public void print(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel rendererModel) {
		draw(process, g2, true);
	}

	private void draw(final ExecutionUnit process, final Graphics2D g2, final boolean printing) {
		OutputPort source = rendererModel.getSelectedConnectionSource();
		if (source == null || !process.getAllOutputPorts().contains(source)) {
			// source undefined or not part of the current subprocess
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
		if (type == MouseEventType.MOUSE_MOVED) {
			if (shape != null && shape.contains(rendererModel.getMousePositionRelativeToProcess())) {
				enableTrashSymbol = true;
				rendererModel.fireMiscChanged();
			} else if (enableTrashSymbol && shape != null
					&& !shape.contains(rendererModel.getMousePositionRelativeToProcess())) {
				enableTrashSymbol = false;
				shape = null;
				rendererModel.fireMiscChanged();
			}
		} else if (type == MouseEventType.MOUSE_PRESSED && SwingUtilities.isLeftMouseButton(e)) {
			if (shape != null && shape.contains(rendererModel.getMousePositionRelativeToProcess())) {
				from.disconnect();
				enableTrashSymbol = false;
				shape = null;
				// set from port back to normal size
				if (rendererModel.getSelectedConnectionSource() != null
						&& rendererModel.getSelectedConnectionSource().equals(from)) {
					rendererModel.setSelectedConnectionSource(null);
				}
				if (rendererModel.getHoveringConnectionSource() != null
						&& rendererModel.getHoveringConnectionSource().equals(from)) {
					rendererModel.setHoveringConnectionSource(null);
				}
				rendererModel.fireMiscChanged();
				e.consume();
			}
		}

	}

	@Override
	public void processKeyEvent(ExecutionUnit process, KeyEventType type, KeyEvent e) {
		// noop
	}

}
