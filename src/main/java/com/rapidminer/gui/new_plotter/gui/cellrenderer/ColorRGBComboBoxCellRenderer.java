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
package com.rapidminer.gui.new_plotter.gui.cellrenderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ColorRGBComboBoxCellRenderer<E> implements ListCellRenderer<E> {

	private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

	private final Border focusBorder = BorderFactory.createLineBorder(Color.gray, 2);
	private final Border noFocusBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);

	private Map<Color, Icon> colorMap = new HashMap<Color, Icon>();

	@Override
	public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected,
			boolean cellHasFocus) {
		JLabel listCellRendererComponent = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index,
				isSelected, cellHasFocus);

		listCellRendererComponent.setPreferredSize(new Dimension(50, 20));

		if (!(value instanceof Color)) {
			return listCellRendererComponent;
		}

		listCellRendererComponent.setText("");

		Color color = (Color) value;

		Icon iicon = colorMap.get(color);
		if (iicon == null) {
			colorMap.put(color, createColoredRectangleIcon(color));
			iicon = colorMap.get(color);
		}

		listCellRendererComponent.setIcon(iicon);
		listCellRendererComponent.setBackground(color);

		if (isSelected && index != -1) {
			listCellRendererComponent.setBorder(focusBorder);
		} else {
			listCellRendererComponent.setBorder(noFocusBorder);
		}

		return listCellRendererComponent;
	}

	protected Icon createColoredRectangleIcon(Color color) {
		// create buffered image for colored icon
		BufferedImage bufferedImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = bufferedImage.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, // Anti-alias!
				RenderingHints.VALUE_ANTIALIAS_ON);

		if (color != null) {
			// fill image with item color
			Color newColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
			g2.setColor(newColor);
		} else {
			g2.setColor(Color.gray);
		}
		g2.fillRect(0, 0, 10, 10);

		return new ImageIcon(bufferedImage);
	}

}
