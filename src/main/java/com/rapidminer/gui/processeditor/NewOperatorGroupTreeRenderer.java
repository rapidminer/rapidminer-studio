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
package com.rapidminer.gui.processeditor;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.GroupTree;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.OperatorService;


/**
 * The renderer for the group tree (displays a small group icon).
 *
 * @author Ingo Mierswa
 */
public class NewOperatorGroupTreeRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1L;

	/** Greyscale version of the blacklisted icon */
	private static final Icon BLACKLISTED_ICON = SwingTools.createIcon("16/" + I18N.getGUILabel("operator.blacklisted.icon"), true);

	/** border so the operators have a little bit more breathing space */
	private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(4, 0, 4, 0);

	/**
	 * Blacklisted operator name color
	 * <p>
	 * The {@link NewOperatorEditor Operators Panel} has a grey background,
	 * instead of a white one like the {@link com.rapidminer.gui.flow.ProcessPanel Process Panel}, therefore darker.
	 */
	private static final Color BLACKLISTED_OPERATOR_NAME_COLOR = new Color(150, 150, 150).darker();

	public NewOperatorGroupTreeRenderer() {
		setLeafIcon(getDefaultClosedIcon());
	}

	@Override
	public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean isSelected,
			final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {

		if (value instanceof GroupTree) {
			GroupTree groupTree = (GroupTree) value;
			setToolTipText("This group contains all operators of the group '" + groupTree.getName() + "'.");
			JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, groupTree.toString(), isSelected, expanded,
					leaf, row, hasFocus);
			label.setBorder(EMPTY_BORDER);
			return label;
		} else {
			OperatorDescription op = (OperatorDescription) value;
			setToolTipText(null);
			String labelText = op.getName();

			JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, labelText, isSelected, expanded, leaf, row,
					hasFocus);
			label.setBorder(EMPTY_BORDER);

			label.setIcon(op.getSmallIcon());

			if (OperatorService.isOperatorBlacklisted(op.getKey())) {
				label.setIcon(BLACKLISTED_ICON);
				label.setForeground(BLACKLISTED_OPERATOR_NAME_COLOR);
			}

			return label;
		}
	}

}
