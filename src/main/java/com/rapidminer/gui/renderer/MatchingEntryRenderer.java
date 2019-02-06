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
package com.rapidminer.gui.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;


/**
 * A renderer for {@link MatchingEntry}. Each entry stores a match score showing how well this entry matches other data.
 *
 * @author Andreas Timm, Marco Böck
 * @since 9.1.0
 */
public class MatchingEntryRenderer extends JPanel implements ListCellRenderer<MatchingEntry> {

	/** The label to show the matching value */
	private JLabel matchLabel;

	/** The label that contains the text MatchingEntry name */
	private JLabel textLabel;


	/**
	 * Creates a new renderer.
	 */
	public MatchingEntryRenderer() {
		textLabel = new JLabel();
		matchLabel = new JLabel() {

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(36, super.getPreferredSize().height);
			}

			@Override
			public Dimension getMinimumSize() {
				return getPreferredSize();
			}

			@Override
			public Dimension getMaximumSize() {
				return getPreferredSize();
			}
		};

		setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 3));
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(0, 0, 0, 0);

		c.weightx = 0;
		c.gridwidth = GridBagConstraints.RELATIVE;
		add(matchLabel, c);

		c.weightx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		add(textLabel, c);
	}

	/**
	 * Returns the preferred size.
	 *
	 * @return the preferred size
	 */
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(150, super.getPreferredSize().height + 12);
	}

	/**
	 * Returns the actual renderer component.
	 *
	 * @param list
	 * 		the list
	 * @param value
	 * 		the value
	 * @param index
	 * 		the index
	 * @param isSelected
	 * 		if selected
	 * @param cellHasFocus
	 * 		if has focus
	 * @return the renderer
	 */
	@Override
	public Component getListCellRendererComponent(JList<? extends MatchingEntry> list, MatchingEntry value, int index, boolean isSelected, boolean cellHasFocus) {
		setComponentOrientation(list.getComponentOrientation());

		setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());

		matchLabel.setText(null);
		if (value != null ) {
			double match = value.getMatch();
			String matchText = getMatchtext(match);
			String entryName = value.getEntryName();
			matchLabel.setVisible(!matchText.isEmpty() && !entryName.isEmpty());
			matchLabel.setText(matchText);
			textLabel.setText(entryName.isEmpty() ? " " : entryName);
			setToolTipText(entryName.isEmpty() ? null : entryName);

			Color foreGroundColor = match < 0.1 ? Color.GRAY : Color.BLACK;
			matchLabel.setForeground(foreGroundColor);
			textLabel.setForeground(foreGroundColor);
		}

		matchLabel.setFont(list.getFont());
		textLabel.setFont(list.getFont());

		return this;
	}

	/**
	 * Get the formatted matching text. Can be green.
	 *
	 * @param match
	 * 		the match value to show
	 * @return an HTML String or an empty String
	 */
	public static String getMatchtext(double match) {
		if (Double.isNaN(match)) {
			return "";
		}
		return "<html><b><span color=\"" +
				(match >= 0.8 ? "green" : match < 0.1 ? "light-gray" : "black") +
				"\">" +
				NumberFormat.getPercentInstance().format(match) +
				"</span></b></html>";
	}
}