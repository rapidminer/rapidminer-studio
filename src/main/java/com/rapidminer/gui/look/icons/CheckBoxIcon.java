/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.look.icons;

import com.rapidminer.gui.look.painters.CashedPainter;

import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.plaf.UIResource;


/**
 * The check box icon.
 * 
 * @author Ingo Mierswa
 */
public class CheckBoxIcon implements Icon, UIResource, Serializable {

	private static final long serialVersionUID = -5782212003890049479L;

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		g.translate(x, y);
		CashedPainter.drawCheckBoxIcon(c, g);
		g.translate(-x, -y);
	}

	@Override
	public int getIconWidth() {
		return 16;
	}

	@Override
	public int getIconHeight() {
		return 16;
	}
}
