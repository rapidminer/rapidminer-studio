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
package com.rapidminer.gui.properties.tablepanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.rapidminer.gui.properties.tablepanel.cells.implementations.CellTypeCheckBoxImpl;
import com.rapidminer.gui.properties.tablepanel.cells.implementations.CellTypeComboBoxImpl;
import com.rapidminer.gui.properties.tablepanel.cells.implementations.CellTypeDateImpl;
import com.rapidminer.gui.properties.tablepanel.cells.implementations.CellTypeRegexImpl;
import com.rapidminer.gui.properties.tablepanel.cells.implementations.CellTypeTextFieldDefaultImpl;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellType;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeCheckBox;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeComboBox;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeDate;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeDateTime;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeRegex;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldDefault;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldInteger;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldNumerical;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldTime;
import com.rapidminer.gui.properties.tablepanel.model.TablePanelModel;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.tools.container.Pair;


/**
 * This component can display a {@link TablePanelModel} in a table-like structure without actually
 * using a table. It also supports content assist (if available) for textfields, a date picker for
 * date fields and a regular expression dialog for regex fields. <br/>
 * It displays contents of the model via GUI components depending on the
 * {@link TablePanelModel#getColumnClass(int, int)}.
 * <p/>
 * GUI mapping:
 * <ul>
 * <li>CellTypeComboBox.class - {@link CellTypeComboBoxImpl} with auto complete if it is enabled</li>
 * <li>CellTypeTextFieldDefault.class - {@link CellTypeTextFieldDefaultImpl} with possible content
 * assist</li>
 * <li>CellTypeTextFieldNumerical.class - {@link CellTypeTextFieldDefaultImpl} which only accepts
 * numbers with possible content assist</li>
 * <li>CellTypeTextFieldInteger.class - {@link CellTypeTextFieldDefaultImpl} which only accepts
 * integers with possible content assist</li>
 * <li>CellTypeTextFieldDate.class - {@link CellTypeDateImpl} with date picker</li>
 * <li>CellTypeTextFieldTime.class - {@link CellTypeTextFieldDefaultImpl} with possible content
 * assist</li>
 * <li>CellTypeTextFieldDateTime.class - {@link CellTypeDateImpl} with date picker</li>
 * <li>CellTypeTextFieldExpression.class - {@link CellTypeRegexImpl} with regular expression assist
 * dialog</li>
 * <li>CellTypeCheckBox.class - {@link CellTypeCheckBoxImpl}</li>
 * <li>fallback - {@link JLabel}</li>
 * </ul>
 * These elements might be disabled or enabled depending on
 * {@link TablePanelModel#isCellEditable(int, int)}.
 * <p/>
 * If content assist is available, will display {@link JCheckBox}es for multiple available values
 * and {@link JRadioButton}s when only one value is supported.
 * <p/>
 * For more references regarding the support for content assist see
 * {@link TablePanelModel#isContentAssistPossibleForCell(int, int)},<br/>
 * {@link TablePanelModel#getPossibleValuesForCellOrNull(int, int)} and<br/>
 * {@link TablePanelModel#canCellHaveMultipleValues(int, int)}.
 *
 * @author Marco Boeck
 *
 */
public class TablePanel extends JPanel {

	/**
	 * Defines how additional space is distributed when using fixed width mode.
	 *
	 *
	 * @author Marco Boeck
	 *
	 */
	public static enum FillerMode {
		/**
		 * additional space is not filled in any way, components will be centered in the middle of
		 * their respective location
		 */
		NONE,

		/** additional space is filled between the last component per row and the delete row button */
		IN_BETWEEN,

		/** additional space is filled after the delete row button */
		REMAINDER;
	}

	private static final long serialVersionUID = 7783828436200806566L;

	/** the backing model */
	private TablePanelModel model;

	/** the listener for the table model */
	private TableModelListener listener;

	/** flag indicating if this component should take care of the scrollpane around it */
	private boolean useScrollPane;

	/** if <code>true</code>, hides the content assist button when not ca is available */
	private boolean hideUnavailableContentAssist;

	/** the size constraints for each column */
	private Dimension[] constraints;

	/** the mode how additional space should be filled */
	private FillerMode fillerMode = FillerMode.IN_BETWEEN;

	/** holds the inner panel */
	private ExtendedJScrollPane scrollPane;

	/** holds all dynamically created components */
	private JPanel innerPanel;

	/** global GridBagConstraints for the dynamically created components */
	private GridBagConstraints gbc;

	/** stores all component (aka cells) to allow for easy replacement */
	private Map<Pair<Integer, Integer>, Component> mapOfComponents;

	/**
	 * Creates a new {@link TablePanel} instance.
	 *
	 * @param model
	 * @param useScrollPane
	 *            if set to <code>true</code>, will add a scrollpane around the GUI.
	 * @param hideUnavailableContentAssist
	 *            if <code>true</code>, the content assist button will be hidden if no content
	 *            assist is available for the given field
	 */
	public TablePanel(final TablePanelModel model, boolean useScrollPane, boolean hideUnavailableContentAssist) {
		this.mapOfComponents = new HashMap<>();
		this.useScrollPane = useScrollPane;
		this.hideUnavailableContentAssist = hideUnavailableContentAssist;
		this.listener = new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e) {
				// table structure changed, re-create it
				if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
					createGUI();
				} else {
					updateComponent(e.getFirstRow(), e.getColumn());
				}
			}
		};

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				initGUI();
				setModel(model);
			}

		});
	}

	/**
	 * Inits the GUI.
	 */
	private void initGUI() {
		scrollPane = new ExtendedJScrollPane();
		scrollPane.setBorder(null);
		innerPanel = new JPanel();
	}

	/**
	 * Creates the GUI via the table model data.
	 */
	private void createGUI() {
		mapOfComponents.clear();
		setLayout(new BorderLayout());
		removeAll();
		innerPanel.removeAll();
		innerPanel.setLayout(new GridBagLayout());
		if (useScrollPane) {
			add(scrollPane, BorderLayout.CENTER);
		} else {
			add(innerPanel);
		}

		gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.anchor = GridBagConstraints.PAGE_START;

		// iterate over table-like structure and display it
		for (int rowIndex = 0; rowIndex < model.getRowCount(); rowIndex++) {
			if (isConstraintsUsed() && fillerMode == FillerMode.NONE) {
				gbc.fill = GridBagConstraints.VERTICAL;
			} else {
				gbc.fill = GridBagConstraints.BOTH;
			}
			for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++) {
				if (isConstraintsUsed()) {
					gbc.weightx = 0.0;
				} else {
					if (Collection.class.isAssignableFrom(model.getColumnClass(columnIndex))) {
						gbc.weightx = 0.1;
					} else {
						gbc.weightx = 1.0 / model.getColumnCount();
					}
				}
				gbc.gridx = columnIndex;
				gbc.gridy = rowIndex;

				Component component = createComponentForCell(rowIndex, columnIndex);
				// add dimension constraint (if applicable)
				if (isConstraintsUsed()) {
					component.setMinimumSize(constraints[columnIndex]);
					component.setMaximumSize(constraints[columnIndex]);
					component.setPreferredSize(constraints[columnIndex]);
				}

				mapOfComponents.put(new Pair<>(rowIndex, columnIndex), component);
				innerPanel.add(component, gbc);
			}
			// if filler mode is IN_BETWEEN, add filler component here
			if (isConstraintsUsed() && fillerMode == FillerMode.IN_BETWEEN) {
				gbc.weightx = 1.0;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.gridx += 1;
				innerPanel.add(new JLabel(), gbc);
			}

			// add "remove row" button
			gbc.weightx = 0.0;
			gbc.fill = GridBagConstraints.NONE;
			gbc.gridx = model.getColumnCount();
			gbc.anchor = GridBagConstraints.EAST;

			final int row = rowIndex;
			JButton removeRowButton = new JButton(new ResourceAction(true, "list.remove_entry") {

				private static final long serialVersionUID = 5289974084350157673L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					model.removeRow(row);
				}
			});
			removeRowButton.setText(null);
			removeRowButton.setPreferredSize(new Dimension(44, 33));
			removeRowButton.setContentAreaFilled(false);
			removeRowButton.setBorderPainted(false);
			innerPanel.add(removeRowButton, gbc);

			// if filler mode is REMAINDER, add filler component here
			if (isConstraintsUsed() && fillerMode == FillerMode.REMAINDER) {
				gbc.weightx = 1.0;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.gridx += 1;
				innerPanel.add(new JLabel(), gbc);
			}
		}

		// filler component so the others are placed neatly at the top
		gbc.gridy++;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.VERTICAL;
		innerPanel.add(Box.createVerticalBox(), gbc);

		if (useScrollPane) {
			scrollPane.setViewportView(innerPanel);
		}

		validate();
		repaint();
	}

	/**
	 * Set the backing model.
	 *
	 * @param model
	 */
	public void setModel(TablePanelModel model) {
		// unregister ourself if we had a model before
		if (this.model != null) {
			this.model.removeTableModelListener(listener);
		}

		this.model = model;
		this.model.addTableModelListener(listener);

		// model changed, re-create GUI
		createGUI();
	}

	/**
	 * Returns the {@link TablePanelModel} instance of this {@link TablePanel}.
	 *
	 * @return
	 */
	public TablePanelModel getModel() {
		return this.model;
	}

	/**
	 * Defines how additional space is distributed in each row if dimension constraints have been
	 * set via {@link #setDimensionConstraints(Dimension[])}. See {@link FillerMode} for a
	 * description of each mode.
	 *
	 * @param fillerMode
	 */
	public void setFillerMode(FillerMode fillerMode) {
		this.fillerMode = fillerMode;
	}

	/**
	 * Returns the currently used {@link FillerMode}.
	 */
	public FillerMode getFillerMode() {
		return fillerMode;
	}

	/**
	 * Sets the {@link Dimension} constraints used by this panel. Each column uses exactly the
	 * specified dimension, no more and no less.The constraints array has to consist of n entries
	 * where n is the number of columns in the {@link TablePanelModel}. If the model is changed
	 * after this method has been called and has more or less columns than the constraints specify,
	 * the constraints are ignored! Set to <code>null</code> to remove the constraints. <br/>
	 * See {@link #setFillerMode(FillerMode)} for options to distribute additional space
	 *
	 * @param constraints
	 *            the {@link Dimension} array consisting of n entries, where n is the number of
	 *            columns of the {@link TablePanelModel}.
	 * @throws IllegalArgumentException
	 *             if constraints.length != getModel().getColumnCount()
	 */
	public void setDimensionConstraints(Dimension[] constraints) throws IllegalArgumentException {
		if (constraints == null) {
			this.constraints = null;
			return;
		}
		if (getModel() != null && constraints.length != getModel().getColumnCount()) {
			throw new IllegalArgumentException("constraints length must match the TablePanelModel column count!");
		}
		for (Dimension dim : constraints) {
			if (dim == null) {
				throw new IllegalArgumentException("constraints element must not be null!");
			}
		}

		this.constraints = constraints;
	}

	/**
	 * Creates the appropriate GUI component for the specified cell of the {@link TablePanelModel}.
	 *
	 * @param rowIndex
	 * @param columnIndex
	 */
	private Component createComponentForCell(final int rowIndex, final int columnIndex) {
		final Class<? extends CellType> cellClass = model.getColumnClass(rowIndex, columnIndex);
		// create appropriate GUI element for class

		// Collections are shown via JComboBox
		if (CellTypeComboBox.class.isAssignableFrom(cellClass)) {
			return new CellTypeComboBoxImpl(model, rowIndex, columnIndex);
		}
		// Strings are shown via JTextField with Content Assist
		if (CellTypeTextFieldDefault.class.isAssignableFrom(cellClass)) {
			return createPanelForStrings(rowIndex, columnIndex, cellClass);
		}
		// Numbers are shown via JTextField with Content Assist
		if (CellTypeTextFieldNumerical.class.isAssignableFrom(cellClass)) {
			return createPanelForDoubles(rowIndex, columnIndex, cellClass);
		}
		// Times are shown via JTextField with Content Assist
		if (CellTypeTextFieldTime.class.isAssignableFrom(cellClass)) {
			return createPanelForStrings(rowIndex, columnIndex, cellClass);
		}
		// Dates are shown via JTextField with Date Picker
		if (CellTypeDate.class.isAssignableFrom(cellClass)) {
			return new CellTypeDateImpl(model, rowIndex, columnIndex, cellClass);
		}
		// DateTimes are shown via JTextField with Date Picker
		if (CellTypeDateTime.class.isAssignableFrom(cellClass)) {
			return new CellTypeDateImpl(model, rowIndex, columnIndex, cellClass);
		}
		// Regular Expressions are shown via JTextField with Regular Exrepssions Dialog
		if (CellTypeRegex.class.isAssignableFrom(cellClass)) {
			return new CellTypeRegexImpl(model, rowIndex, columnIndex, cellClass);
		}
		// Booleans are shown via JCheckBox
		if (CellTypeCheckBox.class.isAssignableFrom(cellClass)) {
			return new CellTypeCheckBoxImpl(model, rowIndex, columnIndex);
		}

		// add more GUI elements here if needed

		// default fallback component is a JLabel
		return createLabel(rowIndex, columnIndex);
	}

	/**
	 * Creates a {@link JLabel} for the specified cell. Does not validate the model, so make sure
	 * this call works!
	 *
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	private JLabel createLabel(final int rowIndex, final int columnIndex) {
		JLabel defaultLabel = new JLabel(String.valueOf(model.getValueAt(rowIndex, columnIndex)));
		defaultLabel.setToolTipText(model.getHelptextAt(rowIndex, columnIndex));
		return defaultLabel;
	}

	/**
	 * Creates a {@link JFormattedTextField} for the specified cell and adds it to a {@link JPanel}
	 * which is returned. Only allows double values as input! Does not validate the model, so make
	 * sure this call works!
	 *
	 * @param rowIndex
	 * @param columnIndex
	 * @param cellClass
	 * @return
	 */
	private JPanel createPanelForDoubles(final int rowIndex, final int columnIndex, final Class<? extends CellType> cellClass) {
		// add a number formatter
		NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
		if (CellTypeTextFieldInteger.class.isAssignableFrom(cellClass)) {
			format.setMinimumFractionDigits(0);
			format.setMaximumFractionDigits(0);
		} else {
			format.setMinimumFractionDigits(1);
		}

		return new CellTypeTextFieldDefaultImpl(model, rowIndex, columnIndex, cellClass, null, hideUnavailableContentAssist);
	}

	/**
	 * Creates a {@link JFormattedTextField} for the specified cell and adds it to a {@link JPanel}
	 * which is returned. Adds content assist of applicable via
	 * {@link TablePanelModel#getPossibleValuesForColumnOrNull(int, int)}. Does not validate the
	 * model, so make sure this call works!
	 *
	 * @param rowIndex
	 * @param columnIndex
	 * @param cellClass
	 * @return
	 */
	private JPanel createPanelForStrings(final int rowIndex, final int columnIndex, final Class<? extends CellType> cellClass) {
		return new CellTypeTextFieldDefaultImpl(model, rowIndex, columnIndex, cellClass, null, hideUnavailableContentAssist);
	}

	/**
	 * Updates the component in the specified cell.
	 *
	 * @param rowIndex
	 * @param columnIndex
	 */
	private void updateComponent(int rowIndex, int columnIndex) {
		Pair<Integer, Integer> key = new Pair<>(rowIndex, columnIndex);

		// remove old component
		Component oldComponent = mapOfComponents.get(key);
		if (oldComponent != null) {
			innerPanel.remove(oldComponent);
		}

		// add updated component to panel instead
		Component updatedComponent = createComponentForCell(rowIndex, columnIndex);

		// add dimension constraint (if applicable)
		if (isConstraintsUsed()) {
			updatedComponent.setMinimumSize(constraints[columnIndex]);
			updatedComponent.setMaximumSize(constraints[columnIndex]);
			updatedComponent.setPreferredSize(constraints[columnIndex]);
		}

		if (isConstraintsUsed()) {
			gbc.weightx = 0.0;
		} else {
			if (Collection.class.isAssignableFrom(model.getColumnClass(columnIndex))) {
				gbc.weightx = 0.1;
			} else {
				gbc.weightx = 1.0 / model.getColumnCount();
			}
		}
		gbc.weighty = 0.0;
		if (isConstraintsUsed() && fillerMode == FillerMode.NONE) {
			gbc.fill = GridBagConstraints.VERTICAL;
		} else {
			gbc.fill = GridBagConstraints.BOTH;
		}
		gbc.gridx = columnIndex;
		gbc.gridy = rowIndex;
		innerPanel.add(updatedComponent, gbc);
		innerPanel.revalidate();
		innerPanel.repaint();

		mapOfComponents.put(key, updatedComponent);
	}

	/**
	 * Returns <code>true</code> if constraints are to be used; <code>false</code> otherwise.
	 */
	private boolean isConstraintsUsed() {
		return constraints != null && constraints.length == getModel().getColumnCount();
	}

}
