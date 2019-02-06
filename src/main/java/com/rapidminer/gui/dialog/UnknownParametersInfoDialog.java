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
package com.rapidminer.gui.dialog;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.operator.UnknownParameterInformation;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;


/**
 * This dialog is shown after loading a process in cases where some of the parameters were unknown.
 * This can for example happen if parameters are removed / renamed and the user should notice this.
 * 
 * @author Ingo Mierswa
 */
public class UnknownParametersInfoDialog extends JDialog {

	private static final long serialVersionUID = 1724548085738058812L;

	private static class UnknownParametersTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 8106632496564917801L;

		private static final String[] COLUMN_NAMES = { "Operator Type", "Operator Name", "Parameter Name", "Parameter Value" };

		private List<UnknownParameterInformation> unknownParameters;

		public UnknownParametersTableModel(List<UnknownParameterInformation> unknownParameters) {
			this.unknownParameters = unknownParameters;
		}

		@Override
		public Class<?> getColumnClass(int c) {
			return String.class;
		}

		@Override
		public String getColumnName(int c) {
			return COLUMN_NAMES[c];
		}

		@Override
		public int getColumnCount() {
			return COLUMN_NAMES.length;
		}

		@Override
		public int getRowCount() {
			return this.unknownParameters.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			UnknownParameterInformation unknownParameter = unknownParameters.get(rowIndex);
			switch (columnIndex) {
				case 0:
					return unknownParameter.getOperatorClass();
				case 1:
					return unknownParameter.getOperatorName();
				case 2:
					return unknownParameter.getParameterName();
				case 3:
					return unknownParameter.getParameterValue();
				default:
					return "?";
			}
		}

	}

	public UnknownParametersInfoDialog(Frame owner, List<UnknownParameterInformation> unknownParameters) {
		super(owner, "Unknown Parameters", true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		setLayout(new BorderLayout());

		setModal(true);

		// text
		JTextArea text = new JTextArea();
		text.setLineWrap(true);
		text.setWrapStyleWord(true);
		text.setBackground(getBackground());
		text.setEditable(false);
		String textString = "The following table shows all parameters which are not (no longer) valid. This can happen for several reasons. First, a mistake in the parameter name can cause this error. Second, a parameter was removed and is now no longer supported. Third, a parameter was replaced by a (set of) other parameter(s). Please ensure that the process still performs the desired task by checking the parameter settings manually.";
		text.setText(textString);
		text.setBorder(BorderFactory.createEmptyBorder(11, 11, 11, 11));
		add(text, BorderLayout.NORTH);

		// table
		ExtendedJTable table = new ExtendedJTable(new UnknownParametersTableModel(unknownParameters), true, true, true);
		ExtendedJScrollPane pane = new ExtendedJScrollPane(table);
		pane.setBorder(BorderFactory.createEmptyBorder(11, 11, 11, 11));
		add(pane, BorderLayout.CENTER);

		// ok button
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton okButton = new JButton("Ok");
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ok();
			}
		});
		buttonPanel.add(okButton);
		add(buttonPanel, BorderLayout.SOUTH);

		setSize(640, 480);
		setLocationRelativeTo(owner);
	}

	public void ok() {
		dispose();
	}
}
