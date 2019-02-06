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

import com.rapidminer.gui.look.InternalFrameTitlePane;
import com.rapidminer.gui.look.borders.Borders;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicInternalFrameUI;


/**
 * The UI for internal frames.
 * 
 * @author Ingo Mierswa
 */
public class InternalFrameUI extends BasicInternalFrameUI {

	public static ComponentUI createUI(JComponent c) {
		return new InternalFrameUI((JInternalFrame) c);
	}

	public InternalFrameUI(JInternalFrame b) {
		super(b);
	}

	@Override
	public void installDefaults() {
		super.installDefaults();
		this.frame.setBorder(Borders.getInternalFrameBorder());
	}

	@Override
	public void installUI(JComponent c) {
		super.installUI(c);
		c.setOpaque(false);
	}

	@Override
	public void uninstallDefaults() {
		this.frame.setBorder(null);
		super.uninstallDefaults();
	}

	@Override
	protected LayoutManager createLayoutManager() {
		return new BasicInternalFrameUI.InternalFrameLayout() {

			@Override
			public void layoutContainer(Container c) {
				Insets i = InternalFrameUI.this.frame.getInsets();
				int cx = i.left;
				int cy = 0;
				int cw = InternalFrameUI.this.frame.getWidth() - i.left - i.right;
				int ch = InternalFrameUI.this.frame.getHeight() - i.bottom;

				if (getNorthPane() != null) {
					Dimension size = getNorthPane().getPreferredSize();
					// Ignore insets when placing the title pane
					getNorthPane().setBounds(0, 0, InternalFrameUI.this.frame.getWidth(), size.height);
					cy += size.height;
					ch -= size.height;
				}

				if (getSouthPane() != null) {
					Dimension size = getSouthPane().getPreferredSize();
					getSouthPane().setBounds(cx, InternalFrameUI.this.frame.getHeight() - i.bottom - size.height, cw,
							size.height);
					ch -= size.height;
				}

				if (getWestPane() != null) {
					Dimension size = getWestPane().getPreferredSize();
					getWestPane().setBounds(cx, cy, size.width, ch);
					cw -= size.width;
					cx += size.width;
				}

				if (getEastPane() != null) {
					Dimension size = getEastPane().getPreferredSize();
					getEastPane().setBounds(cw - size.width, cy, size.width, ch);
					cw -= size.width;
				}

				if (InternalFrameUI.this.frame.getRootPane() != null) {
					InternalFrameUI.this.frame.getRootPane().setBounds(cx, cy, cw, ch);
				}
			}
		};
	}

	@Override
	protected JComponent createNorthPane(JInternalFrame w) {
		this.titlePane = new InternalFrameTitlePane(w);
		return this.titlePane;
	}
}
