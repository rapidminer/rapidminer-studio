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
package com.rapidminer.gui.viewer;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.look.ui.TableHeaderUI;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.operator.visualization.dependencies.ANOVAMatrix;
import com.rapidminer.tools.Tools;


/**
 * This viewer class can be used to display the significant differences of numerical columns
 * depending on the groups defined by nominal columns. The result will be a type of
 * {@link ANOVAMatrix} which will be displayed here. The cells indicating probably significant
 * differences between the groups will be printed with a darker background color.
 *
 * @author Ingo Mierswa, Marco Boeck
 */
public class ANOVAMatrixViewer extends JPanel {

	private static final long serialVersionUID = 1L;

	public ANOVAMatrixViewer(ANOVAMatrix matrix) {
		super(new BorderLayout());

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(true);
		panel.setBackground(Colors.WHITE);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(42, 10, 20, 10);

		// table
		ANOVAMatrixViewerTable table = new ANOVAMatrixViewerTable(matrix);
		table.getTableHeader().putClientProperty(RapidLookTools.PROPERTY_TABLE_HEADER_BACKGROUND, Colors.WHITE);
		((TableHeaderUI) table.getTableHeader().getUI()).installDefaults();
		table.setRowHighlighting(true);
		table.setRowHeight(PropertyPanel.VALUE_CELL_EDITOR_HEIGHT);

		JScrollPane scrollPane = new ExtendedJScrollPane(table);
		scrollPane.setBorder(null);
		scrollPane.setBackground(Colors.WHITE);
		scrollPane.getViewport().setBackground(Colors.WHITE);
		panel.add(scrollPane, gbc);

		// info string
		JLabel infoText = new JLabel();
		infoText.setText("A colored background indicates that the probability for non-difference between the groups is less than "
				+ Tools.formatNumber(matrix.getSignificanceLevel()));
		gbc.gridy += 1;
		gbc.insets = new Insets(5, 10, 5, 10);
		gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(infoText, gbc);

		add(panel, BorderLayout.CENTER);
	}
}
