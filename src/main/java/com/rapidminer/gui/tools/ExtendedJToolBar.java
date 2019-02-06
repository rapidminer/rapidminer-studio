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
package com.rapidminer.gui.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;


/**
 * This toolbar extension is not floatable and activate the hover effect.
 * 
 * @author Ingo Mierswa
 */
public class ExtendedJToolBar extends JToolBar {

	private static final long serialVersionUID = -9219638829666999431L;

	private boolean visibleSeparator;

	public ExtendedJToolBar() {
		this(false);
	}

	/**
	 * Creates a {@link JToolBar} with some custom settings.
	 * 
	 * @param visibleSeparator
	 *            determines whether a visible separator is added when calling
	 *            {@link #addSeparator()}.
	 */
	public ExtendedJToolBar(boolean visibleSeparator) {
		super();
		setFloatable(false);
		setRollover(true);
		setBorder(null);
		this.visibleSeparator = visibleSeparator;
	}

	@Override
	public void addSeparator() {
		if (visibleSeparator) {
			addVisibleSeparator();
		} else {
			super.addSeparator();
		}
	}

	/**
	 * Adds a visible {@link JSeparator} in the toolbar.
	 */
	private void addVisibleSeparator() {
		JSeparator separator = new JSeparator(SwingConstants.VERTICAL) {

			private static final long serialVersionUID = 1L;
			private static final int PADDING = 3;
			private static final int WIDTH = 10;

			@Override
			public Dimension getMinimumSize() {
				Dimension dim = super.getMinimumSize();
				dim.width = WIDTH;
				return dim;
			}

			@Override
			public Dimension getMaximumSize() {
				Dimension dim = super.getMaximumSize();
				dim.width = WIDTH;
				return dim;
			}

			@Override
			public Dimension getPreferredSize() {
				Dimension dim = super.getPreferredSize();
				dim.width = WIDTH;
				return dim;
			}

			@Override
			public void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				int width = getBounds().width;
				int height = getBounds().height;
				g2.setColor(Color.LIGHT_GRAY);
				g2.drawLine(width / 2, PADDING, width / 2, height - (PADDING + 1));
				g2.dispose();
			}
		};
		separator.setOpaque(true);
		add(separator);
	}
}
