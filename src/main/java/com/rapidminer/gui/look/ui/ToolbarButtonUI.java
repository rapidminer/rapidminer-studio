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

import com.rapidminer.gui.look.ButtonListener;
import com.rapidminer.gui.look.RapidLookTools;
import com.vlsolutions.swing.toolbars.VLToolBar;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.plaf.basic.BasicButtonListener;
import javax.swing.plaf.basic.BasicButtonUI;


/**
 * The UI for toolbar buttons.
 * 
 * @author Ingo Mierswa
 */
public class ToolbarButtonUI extends BasicButtonUI {

	protected static boolean isToolbarButton(JComponent c) {
		return RapidLookTools.isToolbarButton(c);
	}

	@Override
	protected void installDefaults(AbstractButton b) {
		super.installDefaults(b);
	}

	@Override
	protected void uninstallDefaults(AbstractButton b) {
		super.uninstallDefaults(b);
	}

	@Override
	public void installUI(JComponent c) {
		super.installUI(c);
	}

	@Override
	public void uninstallUI(JComponent c) {
		super.uninstallUI(c);
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		super.paint(g, c);
	}

	public boolean isOpaque() {
		return false;
	}

	@Override
	public Dimension getPreferredSize(JComponent c) {
		if (c.getParent() instanceof JToolBar) {
			return super.getPreferredSize(c);
		}
		if (c.getParent() instanceof VLToolBar) {
			return super.getPreferredSize(c);
		}
		return new Dimension(22, 22);
	}

	@Override
	protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
		// do nothing
	}

	@Override
	protected void paintButtonPressed(Graphics g, AbstractButton b) {
		setTextShiftOffset();
	}

	@Override
	protected BasicButtonListener createButtonListener(AbstractButton b) {
		return new ButtonListener(b);
	}
}
