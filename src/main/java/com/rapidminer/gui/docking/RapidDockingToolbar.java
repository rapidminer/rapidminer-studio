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
package com.rapidminer.gui.docking;

import com.rapidminer.gui.look.ui.ButtonUI;
import com.rapidminer.tools.I18N;
import com.vlsolutions.swing.toolbars.VLToolBar;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;


/**
 * 
 * @author Simon Fischer
 */
public class RapidDockingToolbar extends VLToolBar {

	private static final long serialVersionUID = 8849041223899314188L;

	public RapidDockingToolbar(String i18nKey) {
		super();
		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		setDraggedBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.GRAY));
		setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.toolbar." + i18nKey + ".tip"));
	}

	public void add(Action action) {
		JButton button = new JButton(action);
		if (button.getIcon() != null) {
			button.setText(null);
		}
		add(button);
	}

	@Override
	public void installButtonUI(AbstractButton button) {
		button.setUI(new ButtonUI());
		button.setBorder(null);
		button.setMargin(new Insets(0, 0, 0, 0));
		if ((button.getText() == null || "".equals(button.getText())) && button.getIcon() != null) {
			button.setPreferredSize(new Dimension((int) (button.getIcon().getIconWidth() * 1.45d), (int) (button.getIcon()
					.getIconHeight() * 1.45d)));
		}
	}
}
