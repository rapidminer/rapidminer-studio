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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ColorListCellRenderer implements ListCellRenderer<Color> {

	// private final Border focusBorder = BorderFactory.createLineBorder(Color.gray, 3);
	// private final Border noFocusBorder = BorderFactory.createEmptyBorder(3, 3, 3, 3);

	private final JPanel container;
	private final JPanel colorComponent;
	private JPanel darkerColorComponent;
	// private JPanel darkestColorComponent;
	private JPanel colorContainer;

	private DefaultListCellRenderer delegate = new DefaultListCellRenderer();

	public ColorListCellRenderer() {
		container = new JPanel(new GridBagLayout());

		GridBagConstraints itemConstraint = new GridBagConstraints();
		itemConstraint.insets = new Insets(1, 1, 1, 1);

		{
			colorContainer = new JPanel(new GridBagLayout());

			{
				colorComponent = new JPanel();
				colorComponent.setPreferredSize(new Dimension(50, 20));
				colorContainer.add(colorComponent, itemConstraint);
			}

			{
				darkerColorComponent = new JPanel();
				darkerColorComponent.setPreferredSize(new Dimension(50, 20));
				colorContainer.add(darkerColorComponent, itemConstraint);
			}

			// {
			//
			// darkestColorComponent = new JPanel();
			// darkestColorComponent.setPreferredSize(new Dimension(50, 20));
			// itemConstraint.ipadx = 5;
			// colorContainer.add(darkestColorComponent, itemConstraint);
			// }

			// {
			// JPanel spacer = new JPanel();
			// spacer.setPreferredSize(new Dimension(5,20));
			// colorContainer.add(spacer,itemConstraint);
			// }

			container.add(colorContainer, itemConstraint);
		}

	}

	@Override
	public Component getListCellRendererComponent(JList<? extends Color> list, Color value, int index, boolean isSelected,
			boolean cellHasFocus) {

		Color background = delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
				.getBackground();

		colorComponent.setBackground(value);
		darkerColorComponent.setBackground(value.darker());
		// darkestColorComponent.setBackground(color.darker().darker());
		// if (isSelected) {
		// colorContainer.setBorder(focusBorder);
		// } else {
		// colorContainer.setBorder(noFocusBorder);
		// }
		colorContainer.setBackground(background);
		container.setBackground(background);

		return container;
	}

}
