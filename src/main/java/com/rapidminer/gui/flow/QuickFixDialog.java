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
package com.rapidminer.gui.flow;

import java.awt.Component;
import java.util.Collection;

import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.operator.ports.quickfix.QuickFix;


/**
 *
 * @author Simon Fischer
 */
public class QuickFixDialog extends ButtonDialog {

	private static final long serialVersionUID = -6465984401606083317L;

	private final JComboBox<QuickFix> comboBox = new JComboBox<>();
	{
		comboBox.setRenderer(new DefaultListCellRenderer() {

			private static final long serialVersionUID = -1011284904143401245L;

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof QuickFix) {
					label.setIcon((Icon) ((QuickFix) value).getAction().getValue(Action.SMALL_ICON));
				}
				return label;
			}
		});
	}

	public QuickFixDialog(Collection<? extends QuickFix> fixes) {
		super(ApplicationFrame.getApplicationFrame(), "quick_fix_dialog", ModalityType.APPLICATION_MODAL, new Object[] {});

		for (final QuickFix fix : fixes) {
			comboBox.addItem(fix);
		}
		comboBox.setSelectedIndex(0);
		layoutDefault(comboBox, makeOkButton("apply_quick_fix"), makeCancelButton());
	}

	@Override
	protected void ok() {
		QuickFix selected = (QuickFix) comboBox.getSelectedItem();
		if (selected != null) {
			selected.apply();
		}
		super.ok();
	}
}
