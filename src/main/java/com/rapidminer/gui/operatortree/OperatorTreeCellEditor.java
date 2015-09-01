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
package com.rapidminer.gui.operatortree;

import com.rapidminer.operator.Operator;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;


/**
 * Editor for tree cells that displays a text field to change the operator name. This is used in the
 * operator tree.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class OperatorTreeCellEditor extends DefaultTreeCellEditor {

	public OperatorTreeCellEditor(JTree tree) {
		super(tree, new DefaultTreeCellRenderer());
	}

	@Override
	public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
			boolean leaf, int row) {
		if (value instanceof Operator) {
			Operator op = (Operator) value;
			value = op.getName();
			ImageIcon icon = op.getOperatorDescription().getSmallIcon();
			ImageIcon usedIcon = (icon == null) ? new ImageIcon(new java.awt.image.BufferedImage(16, 16,
					java.awt.image.BufferedImage.TYPE_INT_ARGB)) : icon;
			editingIcon = usedIcon;
			renderer.setClosedIcon(usedIcon);
			renderer.setOpenIcon(usedIcon);
			renderer.setLeafIcon(usedIcon);
		}
		return super.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row);
	}
}
