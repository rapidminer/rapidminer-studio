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
package com.rapidminer.gui.look.borders;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;
import javax.swing.plaf.UIResource;


/**
 * The UIResource for dummy (empty) borders.
 * 
 * @author Ingo Mierswa
 */
public class DummyBorder extends AbstractBorder implements UIResource {

	private static final long serialVersionUID = 2364995114235662736L;

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(2, 2, 3, 3);
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		insets.left = 2;
		insets.top = 2;
		insets.right = 3;
		insets.bottom = 3;
		return insets;
	}
}
