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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;


/**
 * A cell renderer for datatable column lists. Each datatable column is representeted by an icon and
 * a label. Date columns are colored green, nominal columns red and numerical columns are colored
 * blue.
 *
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class DataTableColumnListCellRenderer implements ListCellRenderer<DataTableColumn> {

	private final Color BORDER_SELECTION_COLOR = UIManager.getColor("Tree.selectionBorderColor").darker();
	private JPanel container;
	private Border noFocusBorder, focusBorder;
	private JLabel tableColumnLabel;

	public DataTableColumnListCellRenderer() {

		container = new JPanel(new GridBagLayout());

		tableColumnLabel = new JLabel();

		focusBorder = BorderFactory.createLineBorder(BORDER_SELECTION_COLOR);
		noFocusBorder = BorderFactory.createLineBorder(Color.WHITE);

		GridBagConstraints itemConstraint = new GridBagConstraints();
		itemConstraint.insets = new Insets(2, 1, 2, 1);
		itemConstraint.anchor = GridBagConstraints.WEST;
		itemConstraint.weightx = 1;
		itemConstraint.fill = GridBagConstraints.HORIZONTAL;

		container.add(tableColumnLabel, itemConstraint);
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends DataTableColumn> list, DataTableColumn value, int index,
			boolean isSelected, boolean cellHasFocus) {

		String text;
		ValueType valueType = null;
		if (value == null) {
			text = "##ERROR## Empty";
		} else {
			text = value.toString();
			valueType = value.getValueType();
		}

		tableColumnLabel.setText(text);

		String i18nKey;

		if (valueType == ValueType.DATE_TIME) {
			tableColumnLabel.setForeground(TreeNodeColors.getDateColor());
			i18nKey = "plotter.configuration_dialog.table_column_date_time";
		} else if (valueType == ValueType.NOMINAL) {
			tableColumnLabel.setForeground(TreeNodeColors.getNominalColor());
			i18nKey = "plotter.configuration_dialog.table_column_nominal";
		} else {
			tableColumnLabel.setForeground(TreeNodeColors.getNumericalColor());
			i18nKey = "plotter.configuration_dialog.table_column_numerical";
		}

		// set label icon
		String icon = I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.label." + i18nKey + ".icon");
		if (icon != null) {
			ImageIcon iicon = SwingTools.createIcon("16/" + icon, true);
			tableColumnLabel.setIcon(iicon);
		}

		container.setBackground(list.getBackground());
		if (isSelected || cellHasFocus) {
			container.setBackground(list.getSelectionBackground());
			container.setForeground(list.getSelectionForeground());
		} else {
			container.setForeground(list.getForeground());
			container.setBackground(list.getBackground());
		}

		if (cellHasFocus) {
			container.setBorder(focusBorder);
		} else {
			container.setBorder(noFocusBorder);
		}

		return container;
	}

}
