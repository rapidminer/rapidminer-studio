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
package com.rapidminer.gui.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.processeditor.results.ResultDisplayTools;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.math.AnovaCalculator;
import com.rapidminer.tools.math.SignificanceCalculationException;
import com.rapidminer.tools.math.SignificanceTestResult;


/**
 * The ANOVA calculator dialog is a small tool which can be used to perform an ANalysis Of VAriances
 * in order to determine if a given set of mean values is probably actually different.
 *
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class AnovaCalculatorDialog extends ButtonDialog {

	private static final long serialVersionUID = 3023267244921354296L;

	private static class AnovaTableModel extends DefaultTableModel {

		private static final long serialVersionUID = -2904775003271582149L;

		public AnovaTableModel() {
			super(new String[] { "Mean", "Variance", "Number" }, 0);
		}

		@Override
		public Class<?> getColumnClass(int c) {
			if (c == 2) {
				return Integer.class;
			} else {
				return Double.class;
			}
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return true;
		}
	}

	private transient AnovaCalculator calculator = new AnovaCalculator();

	private final JTextField alphaField = new JTextField(8);
	{
		alphaField.setText("0.05");
	}

	private final AnovaTableModel tableModel;

	public AnovaCalculatorDialog() {
		super(ApplicationFrame.getApplicationFrame(), "anova_calculator", ModalityType.APPLICATION_MODAL, new Object[] {});

		JPanel panel = new JPanel(new BorderLayout());
		this.calculator = new AnovaCalculator();

		// data table
		this.tableModel = new AnovaTableModel();
		final JTable dataTable = new ExtendedJTable(tableModel, false);
		JScrollPane scrollPane = new ExtendedJScrollPane(dataTable);
		scrollPane.setBorder(createBorder());
		panel.add(scrollPane, BorderLayout.CENTER);

		JPanel significancePanel = new JPanel(new FlowLayout());
		significancePanel.add(new JLabel("Significance Level: "));
		significancePanel.add(alphaField);

		JButton addRowButton = new JButton(new ResourceAction("anova_calculator.add_row") {

			private static final long serialVersionUID = 207462851508903445L;

			@Override
			public void actionPerformed(ActionEvent e) {
				tableModel.addRow(new Object[] { Double.valueOf(0), Double.valueOf(0), Integer.valueOf(0) });
			}

		});

		JButton removeRowButton = new JButton(new ResourceAction("anova_calculator.remove_row") {

			private static final long serialVersionUID = -5306772619829043512L;

			@Override
			public void actionPerformed(ActionEvent e) {
				int[] rows = dataTable.getSelectedRows();
				for (int i = rows.length - 1; i >= 0; i--) {
					tableModel.removeRow(rows[i]);
				}
			}
		});

		JButton calculateButton = new JButton(new ResourceAction("anova_calculator.calculate") {

			private static final long serialVersionUID = -8155818315917061669L;

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					calculateANOVA();
				} catch (SignificanceCalculationException e1) {
					SwingTools.showSimpleErrorMessage("cannot_calc_anova", e1);
				}
			}
		});
		JButton clearButton = new JButton(new ResourceAction("anova_calculator.clear") {

			private static final long serialVersionUID = -3732977250759096948L;

			@Override
			public void actionPerformed(ActionEvent e) {
				while (tableModel.getRowCount() > 0) {
					tableModel.removeRow(0);
				}
			}
		});

		// button panel
		JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.add(significancePanel, BorderLayout.WEST);
		buttonPanel.add(makeButtonPanel(addRowButton, removeRowButton, clearButton, calculateButton, makeCloseButton()));

		layoutDefault(panel, buttonPanel, LARGE);
	}

	private void calculateANOVA() throws SignificanceCalculationException {
		double alpha = -1;
		String alphaString = alphaField.getText();
		try {
			alpha = Double.parseDouble(alphaString);
		} catch (NumberFormatException e) {
			SwingTools.showVerySimpleErrorMessage("sign_lvl_between_0_1");
		}

		if (alpha < 0 || alpha > 1) {
			SwingTools.showVerySimpleErrorMessage("sign_lvl_between_0_1");
		} else {
			this.calculator.clearGroups();
			this.calculator.setAlpha(alpha);
			for (int i = 0; i < tableModel.getRowCount(); i++) {
				double mean = ((Double) tableModel.getValueAt(i, 0)).doubleValue();
				double variance = ((Double) tableModel.getValueAt(i, 1)).doubleValue();
				int number = ((Integer) tableModel.getValueAt(i, 2)).intValue();
				calculator.addGroup(number, mean, variance);
			}
			if (tableModel.getRowCount() < 2) {
				SwingTools.showVerySimpleErrorMessage("two_rows_to_calc_anova_test");
				return;
			}

			SignificanceTestResult result = calculator.performSignificanceTest();
			SwingTools.showResultsDialog("anova",
					ResultDisplayTools.createVisualizationComponent(result, null, "ANOVA Result"));
		}
	}
}
