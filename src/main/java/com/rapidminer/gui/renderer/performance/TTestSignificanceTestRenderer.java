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
package com.rapidminer.gui.renderer.performance;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.gui.tools.CellColorProvider;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.validation.significance.TTestSignificanceTestOperator.TTestSignificanceTestResult;
import com.rapidminer.report.Reportable;
import com.rapidminer.tools.Tools;


/**
 *
 * @author Sebastian Land, Marco Boeck
 */
public class TTestSignificanceTestRenderer extends AbstractRenderer {

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		// TODO: can do anything else than text output?
		return (com.rapidminer.report.Readable) renderable;
	}

	@Override
	public String getName() {
		return "T-Test Significance";
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		final TTestSignificanceTestResult result = (TTestSignificanceTestResult) renderable;
		PerformanceVector[] allVectors = result.getAllVectors();
		String[][] data = new String[allVectors.length + 1][allVectors.length + 1];
		String[] row1 = new String[allVectors.length + 1];
		row1[0] = "";
		for (int i = 0; i < allVectors.length; i++) {
			row1[i + 1] = Tools.formatNumber(allVectors[i].getMainCriterion().getAverage()) + " +/- "
					+ Tools.formatNumber(Math.sqrt(allVectors[i].getMainCriterion().getVariance()));

			String rowI[] = new String[allVectors.length + 1];
			data[i + 1] = rowI;
		}
		data[0] = row1;

		for (int i = 0; i < allVectors.length; i++) {
			String rowI[] = data[i + 1];
			rowI[0] = Tools.formatNumber(allVectors[i].getMainCriterion().getAverage()) + " +/- "
					+ Tools.formatNumber(Math.sqrt(allVectors[i].getMainCriterion().getVariance()));
			for (int j = 0; j < allVectors.length; j++) {
				if (!Double.isNaN(result.getProbMatrix()[i][j])) {
					double prob = result.getProbMatrix()[i][j];
					rowI[j + 1] = Tools.formatNumber(prob);
				} else {
					rowI[j + 1] = "";
				}
			}
		}

		String[] header = new String[allVectors.length + 1];
		TableModel model = new DefaultTableModel(data, header);

		final ExtendedJTable table = new ExtendedJTable(model, false);
		table.setRowHeight(PropertyPanel.VALUE_CELL_EDITOR_HEIGHT);
		table.setRowHighlighting(true);
		table.setCellColorProvider(new CellColorProvider() {

			@Override
			public Color getCellColor(int row, int col) {
				int actualCol = table.convertColumnIndexToModel(col);
				if (actualCol == 0 || row == 0) {
					return Colors.WHITE;
				} else {
					double prob = result.getProbMatrix()[row - 1][actualCol - 1];
					if (!Double.isNaN(prob)) {
						if (prob < result.getAlpha()) {
							return SwingTools.LIGHTEST_YELLOW;
						} else {
							return Colors.WHITE;
						}
					} else {
						return Colors.WHITE;
					}
				}
			}
		});

		JLabel label1 = new JLabel("Probabilities for random values with the same result.");
		JLabel label2 = new JLabel("Values with a colored background are smaller than alpha="
				+ Tools.formatNumber(result.getAlpha())
				+ " which indicates a probably significant difference between the actual mean values.");

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
		gbc.insets = new Insets(5, 10, 5, 10);
		gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(label1, gbc);

		gbc.gridy += 1;
		panel.add(label2, gbc);

		JPanel outerPanel = new JPanel(new BorderLayout());
		outerPanel.add(panel, BorderLayout.CENTER);
		return outerPanel;
	}
}
