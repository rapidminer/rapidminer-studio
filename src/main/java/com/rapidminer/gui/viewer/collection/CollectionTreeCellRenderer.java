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
package com.rapidminer.gui.viewer.collection;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.ResultObject;
import com.rapidminer.operator.learner.meta.MetaModel;


/**
 *
 * @author Sebastian Land
 */
public class CollectionTreeCellRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1L;

	private final Icon ICON_FOLDER_OPEN = SwingTools.createIcon("16/folder_open.png");
	private final Icon ICON_FOLDER_CLOSED = SwingTools.createIcon("16/folder.png");

	private final Map<IOObject, String> childNames = new HashMap<>();

	public CollectionTreeCellRenderer(IOObject collection) {
		if (collection instanceof MetaModel) {
			MetaModel mm = (MetaModel) collection;
			for (int i = 0; i < mm.getModels().size(); i++) {
				childNames.put(mm.getModels().get(i), mm.getModelNames().get(i));
			}
		}
		setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf,
			int row, boolean hasFocus) {
		JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		CollectionTreeElement treeElement = (CollectionTreeElement) value;
		IOObject ioobject = treeElement.getIOObject();
		if (ioobject instanceof ResultObject) {
			ResultObject ro = (ResultObject) ioobject;
			String name = childNames.get(ro);
			if (name == null) {
				name = ro.getName();
			}
			label.setText("<html>" + name + "</html>");
			if (ro instanceof IOObjectCollection) {
				label.setIcon(expanded ? ICON_FOLDER_OPEN : ICON_FOLDER_CLOSED);
			} else {
				Icon resultIcon = ro.getResultIcon();
				label.setIcon(resultIcon);
			}
		} else if (ioobject != null) {
			label.setText(ioobject.getClass().getSimpleName());
		}
		return label;
	}
}
