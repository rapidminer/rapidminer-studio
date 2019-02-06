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
package com.rapidminer.gui.attributeeditor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.rapidminer.RapidMiner;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;


/**
 * Can be used to control the data ranges displayed by other components. Counting starts at 1.
 *
 * @author Ingo Mierswa
 */
public class DataControl extends JPanel {

	private static final long serialVersionUID = 6441468388055505143L;

	/** The label for the maximum row counter. */
	private JLabel rowCounter;

	/** The label for the maximum column counter. */
	private JLabel columnCounter;

	/** The first row which should be shown. Start counting from 1! */
	private int firstRow;

	/** The first row which should be shown (including). */
	private int lastRow;

	/** The first attribute which should be shown (counting starts at 1). */
	private int firstColumn;

	/** The first attribute which should be shown (counting starts at 1). */
	private int lastColumn;

	/** The textfield for the row start. */
	private JTextField fromRowField;

	/** The textfield for the row end. */
	private JTextField toRowField;

	/** The textfield for the column start. */
	private JTextField fromColumnField;

	/** The textfield for the column end. */
	private JTextField toColumnField;

	/** The textfield for the fraction digits. */
	private JTextField fractionDigitsField;

	/** The maximum number of rows. */
	private int maxRows;

	/** The maximum number of columns. */
	private int maxColumns;

	/** The name for the rows. */
	private String rowName;

	/** The name for the columns. */
	private String columnName;

	/** Number of fraction digits. */
	private int fractionDigits = 3;

	/** The data view change listeners. */
	private List<DataControlListener> listeners = new LinkedList<DataControlListener>();

	/** Creates a new data control object including the fraction digits field. */
	public DataControl(int _maxRows, int _maxColumns, String _rowName, String _columnName) {
		this(_maxRows, _maxColumns, _rowName, _columnName, true);
	}

	/** Creates a new data control object. */
	public DataControl(int _maxRows, int _maxColumns, String _rowName, String _columnName, final boolean fractionD) {
		this.maxRows = _maxRows;
		this.maxColumns = _maxColumns;
		this.rowName = _rowName;
		this.columnName = _columnName;
		this.firstRow = 1;
		this.lastRow = Math.min(10, maxRows);
		this.firstColumn = 1;
		this.lastColumn = Math.min(10, maxColumns);

		// data control
		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(2, 2, 2, 2);
		setLayout(gridBag);

		// example range
		rowCounter = new JLabel("Number of " + rowName + "s: " + maxRows);
		gridBag.setConstraints(rowCounter, c);
		add(rowCounter);

		columnCounter = new JLabel("Number of " + columnName + "s: " + maxColumns);
		gridBag.setConstraints(columnCounter, c);
		add(columnCounter);

		JLabel label = new JLabel(rowName + " range:");
		gridBag.setConstraints(label, c);
		add(label);

		c.gridwidth = GridBagConstraints.RELATIVE;
		label = new JLabel("from:");
		gridBag.setConstraints(label, c);
		add(label);

		fromRowField = new JTextField(firstRow + "");
		fromRowField.setToolTipText("The first " + rowName + " which is shown.");
		c.weightx = 1.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridBag.setConstraints(fromRowField, c);
		add(fromRowField);

		c.gridwidth = GridBagConstraints.RELATIVE;
		c.weightx = 0.0;
		label = new JLabel("to:");
		gridBag.setConstraints(label, c);
		add(label);

		toRowField = new JTextField(lastRow + "");
		toRowField.setToolTipText("The last " + rowName + " which is shown.");
		c.weightx = 1.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridBag.setConstraints(toRowField, c);
		add(toRowField);

		JLabel fillLabel = new JLabel("");
		gridBag.setConstraints(fillLabel, c);
		add(fillLabel);

		// attribute range
		label = new JLabel(columnName + " range:");
		gridBag.setConstraints(label, c);
		add(label);

		c.gridwidth = GridBagConstraints.RELATIVE;
		label = new JLabel("from:");
		gridBag.setConstraints(label, c);
		add(label);

		fromColumnField = new JTextField(firstColumn + "");
		fromColumnField.setToolTipText("The first " + columnName + " which is shown.");
		c.weightx = 1.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridBag.setConstraints(fromColumnField, c);
		add(fromColumnField);

		c.gridwidth = GridBagConstraints.RELATIVE;
		c.weightx = 0.0;
		label = new JLabel("to:");
		gridBag.setConstraints(label, c);
		add(label);

		toColumnField = new JTextField(lastColumn + "");
		toColumnField.setToolTipText("The last " + columnName + " which is shown.");
		c.weightx = 1.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		gridBag.setConstraints(toColumnField, c);
		add(toColumnField);

		// fraction digits
		if (fractionD) {
			label = new JLabel("Fraction digits:");
			c.weightx = 0.0;
			gridBag.setConstraints(label, c);
			add(label);

			c.weightx = 1.0;
			fractionDigits = 3;
			try {
				String numberDigitsString = ParameterService
						.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_NUMBERS);
				if (numberDigitsString != null) {
					fractionDigits = Integer.parseInt(numberDigitsString);
				}
			} catch (NumberFormatException e) {
				// LogService.getGlobal().log("Bad integer format in property 'rapidminer.gui.fractiondigits.numbers', using default number of fraction digits (3).",
				// LogService.WARNING);
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.attributeeditor.DataControl.bad_integer_format_in_property");
			}
			fractionDigitsField = new JTextField(fractionDigits + "");
			fractionDigitsField.setToolTipText("The number of fraction digits which is used for numerical values.");
			gridBag.setConstraints(fractionDigitsField, c);
			add(fractionDigitsField);
		}

		// update button
		c.weightx = 1.0;
		JButton updateButton = new JButton("Update");
		updateButton.setToolTipText("Update the view.");
		updateButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// generic range checks
				try {
					int newFirstRow = Integer.parseInt(fromRowField.getText());
					if (newFirstRow >= 1 && newFirstRow < maxRows) {
						firstRow = newFirstRow;
					}
				} catch (NumberFormatException ex) {
				} finally {
					fromRowField.setText(firstRow + "");
				}

				try {
					int newLastRow = Integer.parseInt(toRowField.getText());
					if (newLastRow >= 1 && newLastRow <= maxRows) {
						lastRow = newLastRow;
					}
				} catch (NumberFormatException ex) {
				} finally {
					toRowField.setText(lastRow + "");
				}

				try {
					int newFirstColumn = Integer.parseInt(fromColumnField.getText());
					if (newFirstColumn >= 1 && newFirstColumn < maxColumns) {
						firstColumn = newFirstColumn;
					}
				} catch (NumberFormatException ex) {
				} finally {
					fromColumnField.setText(firstColumn + "");
				}

				try {
					int newLastColumn = Integer.parseInt(toColumnField.getText());
					if (newLastColumn >= 1 && newLastColumn <= maxColumns) {
						lastColumn = newLastColumn;
					}
				} catch (NumberFormatException ex) {
				} finally {
					toColumnField.setText(lastColumn + "");
				}

				if (fractionD) {
					try {
						int newFractionDigits = Integer.parseInt(fractionDigitsField.getText());
						if (newFractionDigits >= 0) {
							fractionDigits = newFractionDigits;
						}
					} catch (NumberFormatException ex) {
					} finally {
						fractionDigitsField.setText(fractionDigits + "");
					}
				}

				// sanity checks
				if (firstRow > lastRow) {
					firstRow = lastRow;
					fromRowField.setText(firstRow + "");
				}
				if (firstColumn > lastColumn) {
					firstColumn = lastColumn;
					fromColumnField.setText(firstColumn + "");
				}
				update();
			}
		});
		gridBag.setConstraints(updateButton, c);
		add(updateButton);

		c.weighty = 1.0;
		fillLabel = new JLabel("");
		gridBag.setConstraints(fillLabel, c);
		add(fillLabel);
	}

	public void setFirstRow(int i) {
		firstRow = i;
		fromRowField.setText(firstRow + "");
	}

	public void setLastRow(int i) {
		lastRow = i;
		toRowField.setText(lastRow + "");
	}

	public void setFirstColumn(int i) {
		firstColumn = i;
		fromColumnField.setText(firstColumn + "");
	}

	public void setLastColumn(int i) {
		lastColumn = i;
		toColumnField.setText(lastColumn + "");
	}

	public int getMaxRows() {
		return maxRows;
	}

	public int getMaxColumns() {
		return maxColumns;
	}

	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
		rowCounter.setText("Number of " + rowName + "s: " + maxRows);
	}

	public void setMaxColumns(int maxColumns) {
		this.maxColumns = maxColumns;
		columnCounter.setText("Number of " + columnName + "s: " + maxColumns);
	}

	public void addViewChangeListener(DataControlListener listener) {
		listeners.add(listener);
	}

	public void removeViewChangeListener(DataControlListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Performs a last range check for changes due to setting the maximum via a method call.
	 */
	public void update() {
		if (firstRow > maxRows) {
			firstRow = maxRows;
			fromRowField.setText(firstRow + "");
		}
		if (lastRow > maxRows) {
			lastRow = maxRows;
			toRowField.setText(lastRow + "");
		}
		if (firstColumn > maxColumns) {
			firstColumn = maxColumns;
			fromColumnField.setText(firstColumn + "");
		}
		if (lastColumn > maxColumns) {
			lastColumn = maxColumns;
			toColumnField.setText(lastColumn + "");
		}

		for (DataControlListener l : listeners) {
			l.update(firstRow, lastRow, firstColumn, lastColumn, fractionDigits);
		}
	}
}
