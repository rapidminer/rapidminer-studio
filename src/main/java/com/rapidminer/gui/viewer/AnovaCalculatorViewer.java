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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.tools.Tools;


/**
 *
 * @author Sebastian Land, Marco Boeck
 */
public class AnovaCalculatorViewer extends JPanel {

	private static final long serialVersionUID = 1L;

	public AnovaCalculatorViewer(String name, double sumSquaresBetween, int degreesOfFreedom1, double meanSquaresBetween,
			double fValue, double prob, double sumSquaresResiduals, int degreesOfFreedom2, double meanSquaresResiduals,
			double alpha) {

		this.setLayout(new BorderLayout());

		String[] row1 = new String[] { "Between", Tools.formatNumber(sumSquaresBetween), String.valueOf(degreesOfFreedom1),
				Tools.formatNumber(meanSquaresBetween), Tools.formatNumber(fValue), Tools.formatNumber(prob) };
		String[] row2 = new String[] { "Residuals", Tools.formatNumber(sumSquaresResiduals),
				String.valueOf(degreesOfFreedom2), Tools.formatNumber(meanSquaresResiduals), "", "" };
		String[] row3 = new String[] { "Total", Tools.formatNumber(sumSquaresBetween + sumSquaresResiduals),
				String.valueOf(degreesOfFreedom1 + degreesOfFreedom2), "", "", "" };
		String[] header = new String[] { "Source", "Square Sums", "DF", "Mean Squares", "F", "Prob" };
		TableModel model = new DefaultTableModel(new String[][] { row1, row2, row3 }, header);

		ExtendedJTable table = new ExtendedJTable(model, true);
		table.setRowHeight(PropertyPanel.VALUE_CELL_EDITOR_HEIGHT);
		table.setRowHighlighting(true);

		String label2Text = null;
		if (prob < alpha) {
			label2Text = "Difference between actual mean values is probably significant, since " + Tools.formatNumber(prob)
					+ " < alpha = " + Tools.formatNumber(alpha);
		} else {
			label2Text = "Difference between actual mean values is probably not significant, since "
					+ Tools.formatNumber(prob) + " > alpha = " + Tools.formatNumber(alpha);
		}
		JLabel label1 = new JLabel("Probability for random values with the same result: " + Tools.formatNumber(prob));
		JLabel label2 = new JLabel(label2Text);

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

		JScrollPane scrollPane = new ExtendedJScrollPane(table);
		scrollPane.setBorder(null);
		scrollPane.setBackground(Colors.WHITE);
		scrollPane.getViewport().setBackground(Colors.WHITE);
		panel.add(scrollPane, gbc);

		gbc.gridy += 1;
		gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 10, 5, 10);
		panel.add(label1, gbc);

		gbc.gridy += 1;
		panel.add(label2, gbc);

		this.add(panel, BorderLayout.CENTER);
	}
}
