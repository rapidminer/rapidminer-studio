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
package com.rapidminer.gui.look.ui;

import com.rapidminer.gui.look.RapidLookTools;

import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolBarUI;


/**
 * The UI for tool bars.
 * 
 * @author Ingo Mierswa
 */
public class ToolBarUI extends BasicToolBarUI {

	protected class RapidLookDockingListener extends DockingListener {

		private boolean pressedToolbarHandler = false;

		public RapidLookDockingListener(JToolBar t) {
			super(t);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			super.mousePressed(e);
			if (!this.toolBar.isEnabled()) {
				return;
			}
			this.pressedToolbarHandler = false;
			Rectangle bumpRect = new Rectangle();

			if (this.toolBar.getOrientation() == SwingConstants.HORIZONTAL) {
				int x = RapidLookTools.isLeftToRight(this.toolBar) ? 0 : this.toolBar.getSize().width - 14;
				bumpRect.setBounds(x, 0, 14, this.toolBar.getSize().height);
			} else { // vertical
				bumpRect.setBounds(0, 0, this.toolBar.getSize().width, 14);
			}
			if (bumpRect.contains(e.getPoint())) {
				this.pressedToolbarHandler = true;
				Point dragOffset = e.getPoint();
				if (!RapidLookTools.isLeftToRight(this.toolBar)) {
					dragOffset.x -= (this.toolBar.getSize().width - this.toolBar.getPreferredSize().width);
				}
				setDragOffset(dragOffset);
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (this.pressedToolbarHandler) {
				super.mouseDragged(e);
			}
		}
	}

	public static ComponentUI createUI(JComponent c) {
		return new ToolBarUI();
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();
	}

	@Override
	protected Border createRolloverBorder() {
		return new EmptyBorder(3, 3, 3, 3);
	}

	@Override
	protected Border createNonRolloverBorder() {
		return new EmptyBorder(3, 3, 3, 3);
	}

	public Border createNonRolloverToggleBorder() {
		return new EmptyBorder(3, 3, 3, 3);
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		if (c.getParent() instanceof JFileChooser) {
			return;
		} else {
			super.paint(g, c);
		}
	}

	@Override
	public boolean isRolloverBorders() {
		return true;
	}

	@Override
	protected MouseInputListener createDockingListener() {
		return new RapidLookDockingListener(this.toolBar);
	}

	protected void setDragOffset(Point p) {
		if (!GraphicsEnvironment.isHeadless()) {
			if (this.dragWindow == null) {
				this.dragWindow = createDragWindow(this.toolBar);
			}
			this.dragWindow.setOffset(p);
		}
	}
}
