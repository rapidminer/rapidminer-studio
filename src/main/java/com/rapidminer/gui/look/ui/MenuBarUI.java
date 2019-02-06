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

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuBarUI;

import com.rapidminer.gui.look.painters.CachedPainter;


/**
 * The UI for menu bars.
 *
 * @author Ingo Mierswa
 */
public class MenuBarUI extends BasicMenuBarUI {

	public static ComponentUI createUI(JComponent c) {
		return new MenuBarUI();
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();
	}

	@Override
	public void update(Graphics g, JComponent c) {
		if (c.isOpaque()) {
			Color oldColor = g.getColor();
			CachedPainter.drawMenuBarBackground(c, g, 0, 0, c.getWidth(), c.getHeight());
			g.setColor(oldColor);
		}
	}
}
