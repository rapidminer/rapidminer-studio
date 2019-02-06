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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import com.rapidminer.gui.new_plotter.templates.style.ColorRGB;
import com.rapidminer.gui.new_plotter.templates.style.ColorScheme;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ColorSchemeComboBoxRenderer extends ComboSeparatorsRenderer<Object> {

	private final static int WIDTH = 130;
	private final static int HEIGHT = 20;
	private JLabel nameLabel;
	private JPanel[] colorPanels;
	private JPanel colorSchemeComponent;
	private JPanel colorPanel;

	public ColorSchemeComboBoxRenderer() {
		super(new DefaultListCellRenderer());

		colorSchemeComponent = new JPanel(new GridBagLayout());

		int nameLabelWidth = 100;

		// add name
		{

			nameLabel = new JLabel("");

			nameLabel.setPreferredSize(new Dimension(nameLabelWidth, HEIGHT));

			GridBagConstraints itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 0.0;
			itemConstraint.weighty = 0.0;
			itemConstraint.insets = new Insets(2, 1, 2, 1);

			colorSchemeComponent.add(nameLabel, itemConstraint);
		}

		// add colors panels
		{
			int width = WIDTH - nameLabelWidth;
			colorPanel = new JPanel(new FlowLayout());
			colorPanel.setPreferredSize(new Dimension(width, HEIGHT));

			int size = 5;
			colorPanels = new JPanel[size];

			int fraction = width / size;
			Map<Integer, Integer> fractionMap = new HashMap<Integer, Integer>();
			for (int j = 0; j < size; ++j) {
				fractionMap.put(j, fraction);
			}
			int fractionSum = size * fraction;
			int index = 0;
			while (fractionSum < width) {
				fractionMap.put(index, fractionMap.get(index));
				fractionSum++;
				index++;
			}

			for (int i = 0; i < size; ++i) {
				JPanel newColorPanel = new JPanel();
				newColorPanel.setPreferredSize(new Dimension(fractionMap.get(i), HEIGHT));
				colorPanel.add(newColorPanel);
				colorPanels[i] = newColorPanel;
			}

			GridBagConstraints itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 1;
			itemConstraint.weighty = 1;
			itemConstraint.insets = new Insets(2, 1, 2, 1);
			itemConstraint.fill = GridBagConstraints.BOTH;

			colorSchemeComponent.add(colorPanel, itemConstraint);
		}

	}

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {

		Component renderComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		renderComponent.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		if (value instanceof ColorScheme && index != -1) {
			adaptColorSchemePreviewComponent((ColorScheme) value, renderComponent, index);
			return colorSchemeComponent;
		}
		return renderComponent;
	}

	private void adaptColorSchemePreviewComponent(ColorScheme colorScheme, Component comp, int index) {

		nameLabel.setText(colorScheme.getName());

		List<ColorRGB> colors = colorScheme.getColors();
		int colorCount = colors.size();
		Color background = comp.getBackground();
		for (int i = 0; i < colorPanels.length; ++i) {

			if (i < colorCount) {
				Color cColor = ColorRGB.convertToColor(colors.get(i));
				colorPanels[i].setBackground(cColor);
			} else {
				colorPanels[i].setBackground(background);
			}

		}

		colorSchemeComponent.setBackground(background);
		colorPanel.setBackground(background);

	}

	@Override
	protected boolean addSeparatorAfter(JList<?> list, Object value, int index) {
		if (!(value instanceof ColorScheme)) {
			return true;
		}
		return false;
	}

}
