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
package com.rapidminer.gui.tools.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;


/**
 * Transparent panel, which can be used as glass pane for a {@link JLayer}, i.e. to show a loading
 * panel.
 *
 * The panel shows an icon, a text below and a transparent background.
 * {@link #paintComponents(Graphics)} is overwritten to make a glass pane transparent.
 *
 * @author Sabrina Kirstein
 *
 */
public class TransparentGlassPanePanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private JLabel label;

	/**
	 * Creates a transparent panel with a loading icon and a text.
	 *
	 * @param icon
	 *            loading icon that is displayed
	 * @param text
	 *            which is shown below the icon
	 * @param backgroundColor
	 *            origin color of the background
	 * @param transparency
	 *            degree of transparency (0.0f = fully transparent, 1.0f = opaque)
	 */
	public TransparentGlassPanePanel(ImageIcon icon, String text, Color backgroundColor, float transparency) {

		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = 0;
		gbc.gridx = 0;

		JLabel iconLabel = new JLabel(icon);

		add(iconLabel, gbc);

		gbc.gridy += 1;
		label = new JLabel(text);
		add(label, gbc);

		if (transparency > 1) {
			transparency = 1;
		} else if (transparency < 0) {
			transparency = 0;
		}
		Color transparentColor = new Color(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue(),
				(int) (transparency * 255));
		setBackground(transparentColor);
		if (transparency < 1) {
			setOpaque(false);
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		// necessary for glass panes to see a transparent background
		// first paint the background
		setOpaque(true);
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		super.paintComponent(g);
		// then show the transparency
		setOpaque(false);
	}

	/**
	 * Sets the text of the transparent glass pane
	 *
	 * @param text
	 */
	public void setText(String text) {
		label.setText(text);
	}
}
