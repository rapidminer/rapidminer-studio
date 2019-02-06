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
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import com.rapidminer.operator.OperatorDescription;


/**
 * A renderer for operator list cells that displays the operator's icon and name.
 *
 * @author Helge Homburg, Ingo Mierswa
 */
public class OperatorListCellRenderer extends DefaultListCellRenderer {

	private static final long serialVersionUID = -4236587258844548010L;

	private boolean coloredBackground;

	public OperatorListCellRenderer(boolean coloredBackground) {
		this.coloredBackground = coloredBackground;
	}

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		OperatorDescription operatorDescription = (OperatorDescription) value;
		Component component = super.getListCellRendererComponent(list, operatorDescription.getName(), index, isSelected,
				cellHasFocus);
		JLabel label = (JLabel) component;
		try {
			label.setIcon(operatorDescription.getSmallIcon());
		} catch (Exception e) {
			// error --> no icon
		}
		if (coloredBackground && !isSelected && index % 2 != 0) {
			label.setBackground(SwingTools.LIGHTEST_BLUE);
		}

		String descriptionString = operatorDescription.getLongDescriptionHTML();
		if (descriptionString == null) {
			descriptionString = operatorDescription.getShortDescription();
		}
		StringBuffer toolTipText = new StringBuffer("<b>Description:</b> " + descriptionString);
		label.setToolTipText(SwingTools.transformToolTipText(toolTipText.toString(), false, false));

		if (operatorDescription.isDeprecated()) {
			label.setForeground(Color.LIGHT_GRAY);
		}
		label.setBorder(null);
		return label;
	}
}
