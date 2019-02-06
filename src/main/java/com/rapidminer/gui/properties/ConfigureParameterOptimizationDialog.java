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
package com.rapidminer.gui.properties;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.Process;
import com.rapidminer.gui.tools.ExtendedJList;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedListModel;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.meta.ParameterConfigurator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeNumber;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.ParameterTypeValue;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.value.ParameterValueGrid;
import com.rapidminer.parameter.value.ParameterValueList;
import com.rapidminer.parameter.value.ParameterValueRange;
import com.rapidminer.parameter.value.ParameterValues;
import com.rapidminer.tools.Tools;


/**
 * A Dialog which lets the user select and configure parameter values and ranges for optimization
 * purposes.
 *
 * @author Tobias Malbrecht, Ingo Mierswa
 */
public class ConfigureParameterOptimizationDialog extends PropertyDialog {

	private static final long serialVersionUID = 187660784321413390L;

	private static final String ADD_ICON_NAME = "add.png";
	private static final String LEFT_ICON_NAME = "nav_left.png";
	private static final String RIGHT_ICON_NAME = "nav_right.png";
	private static final String UP_ICON_NAME = "nav_up.png";
	private static final String DOWN_ICON_NAME = "nav_down.png";

	private static Icon ADD_ICON;
	private static Icon LEFT_ICON;
	private static Icon RIGHT_ICON;
	private static Icon UP_ICON;
	private static Icon DOWN_ICON;

	static {
		ADD_ICON = SwingTools.createIcon("16/" + ADD_ICON_NAME);
		LEFT_ICON = SwingTools.createIcon("16/" + LEFT_ICON_NAME);
		RIGHT_ICON = SwingTools.createIcon("16/" + RIGHT_ICON_NAME);
		UP_ICON = SwingTools.createIcon("16/" + UP_ICON_NAME);
		DOWN_ICON = SwingTools.createIcon("16/" + DOWN_ICON_NAME);
	}

	private boolean ok = false;

	private final int mode;

	private ExtendedListModel<Operator> operatorListModel;

	private ExtendedListModel<String> parametersListModel;

	private ExtendedListModel<String> selectedParametersListModel;

	private ExtendedJList<Operator> operatorList;

	private ExtendedJList<String> parametersList;

	private ExtendedJList<String> selectedParametersList;

	private JLabel minValueJLabel;

	private JLabel maxValueJLabel;

	private JLabel stepsValueJLabel;

	private JLabel gridScaleValueJLabel;

	private JTextField minValueTextField;

	private JTextField maxValueTextField;

	private JTextField stepsValueTextField;

	private JComboBox<String> gridScaleValueComboBox;

	private JList<String> categoriesList;

	private JList<String> selectedCategoriesList;

	private DefaultListModel<String> categoriesListModel;

	private DefaultListModel<String> selectedCategoriesListModel;

	private JTextField createValueTextField;

	private JButton createValueButton;

	private JButton addValueButton;

	private JButton removeValueButton;

	private JButton upListButton;

	private JButton downListButton;

	private JRadioButton choseGridRadioButton;

	private JRadioButton choseListRadioButton;

	private JLabel infoLabel;

	private final ConfigurationListener listener;

	private final Process process;

	private final LinkedHashMap<String, ParameterValues> parameterValuesMap;

	public ConfigureParameterOptimizationDialog(ParameterType type, ConfigurationListener listener) {
		super(type, "parameter_optimization");
		this.listener = listener;
		process = listener.getProcess();
		parameterValuesMap = new LinkedHashMap<>();

		layoutDefault(createMainPanel(), createButtonPanel(), LARGE);

		ParameterConfigurator parameterConfigurator = (ParameterConfigurator) listener;
		Operator op = (Operator) listener;
		this.mode = parameterConfigurator.getParameterValueMode();
		List<ParameterValues> readParameterValues = null;
		try {
			List<String[]> parameterValueList = ParameterTypeList
					.transformString2List(op.getParameters().getParameter(ParameterConfigurator.PARAMETER_PARAMETERS));
			readParameterValues = parameterConfigurator.parseParameterValues(parameterValueList);
		} catch (Exception e) {
			op.logWarning(e.getMessage());
		}
		if (readParameterValues != null) {
			for (ParameterValues parameterValue : readParameterValues) {
				addParameter(parameterValue);
			}
		}

		updateInfoLabel();
	}

	private JPanel createMainPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		JPanel selectionPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		// initialize selection lists
		operatorListModel = new ExtendedListModel<>();
		for (Operator op : ((OperatorChain) listener).getAllInnerOperators()) {
			operatorListModel.addElement(op, null);
		}

		operatorList = new ExtendedJList<>(operatorListModel);
		operatorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		operatorList.setLayoutOrientation(JList.VERTICAL);

		parametersListModel = new ExtendedListModel<>();
		parametersList = new ExtendedJList<>(parametersListModel);
		parametersList.setLayoutOrientation(JList.VERTICAL);

		operatorList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				int index = operatorList.getSelectedIndex();
				if (index != -1) {
					Operator op = operatorList.getModel().getElementAt(index);
					updateParameterListModel(op);
				}
			}
		});

		selectedParametersListModel = new ExtendedListModel<>();
		selectedParametersList = new ExtendedJList<>(selectedParametersListModel);
		selectedParametersList.setLayoutOrientation(JList.VERTICAL);
		selectedParametersList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {

					// only update if an element is selected
					if (selectedParametersList.getSelectedIndex() != -1) {
						int previousIndex = selectedParametersList.getSelectedIndex() == e.getFirstIndex() ? e.getLastIndex()
								: e.getFirstIndex();

						if (previousIndex != selectedParametersList.getSelectedIndex() && previousIndex >= 0
								&& previousIndex < selectedParametersListModel.getSize()) {
							updateNumericalParameterValues(previousIndex);
						}
					}
					showParameterValues(selectedParametersList.getSelectedValue());
				}
			}
		});

		JPanel parameterSelectionButtonsPanel = new JPanel(new BorderLayout());

		JButton addParameterButton = null;
		if (RIGHT_ICON != null) {
			addParameterButton = new JButton(RIGHT_ICON);
		} else {
			addParameterButton = new JButton(">");
		}
		addParameterButton.setToolTipText("Select parameters.");
		addParameterButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				addSelectedParameters();

				// select the last element which was appended to the selected parameters
				int indexLastElement = selectedParametersListModel.getSize() - 1;
				selectedParametersList.setSelectedIndex(indexLastElement);
				selectedParametersList.ensureIndexIsVisible(indexLastElement);
			}
		});

		JButton removeParameterButton = null;
		if (LEFT_ICON != null) {
			removeParameterButton = new JButton(LEFT_ICON);
		} else {
			removeParameterButton = new JButton("<");
		}
		removeParameterButton.setToolTipText("Deselect parameters.");
		removeParameterButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				removeSelectedParameters();
			}
		});
		parameterSelectionButtonsPanel.add(addParameterButton, BorderLayout.NORTH);
		parameterSelectionButtonsPanel.add(removeParameterButton, BorderLayout.SOUTH);

		JScrollPane operatorListScrollPane = new ExtendedJScrollPane(operatorList) {

			private static final long serialVersionUID = 1103467036573935368L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension((int) (getWidth() * 0.25d), super.getPreferredSize().height);
			}
		};
		JScrollPane parametersListScrollPane = new ExtendedJScrollPane(parametersList) {

			private static final long serialVersionUID = 8474453689364798720L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension((int) (getWidth() * 0.25d), super.getPreferredSize().height);
			}
		};
		JScrollPane selectedParametersListScrollPane = new ExtendedJScrollPane(selectedParametersList) {

			private static final long serialVersionUID = -7089596032717082128L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension((int) (getWidth() * 0.25d), super.getPreferredSize().height);
			}
		};

		c.insets = new Insets(0, 0, GAP, GAP);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		operatorListScrollPane.setBorder(createTitledBorder("Operators"));
		selectionPanel.add(operatorListScrollPane, c);

		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		parametersListScrollPane.setBorder(createTitledBorder("Parameters"));
		selectionPanel.add(parametersListScrollPane, c);

		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 1;
		c.fill = GridBagConstraints.NONE;
		selectionPanel.add(parameterSelectionButtonsPanel, c);

		c.insets = new Insets(0, 0, GAP, 0);
		c.gridx = 3;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		selectedParametersListScrollPane.setBorder(createTitledBorder("Selected Parameters"));
		selectionPanel.add(selectedParametersListScrollPane, c);

		panel.add(selectionPanel, BorderLayout.NORTH);

		JPanel gridPanel = new JPanel(new GridLayout(2, 4));
		((GridLayout) gridPanel.getLayout()).setHgap(5);
		((GridLayout) gridPanel.getLayout()).setVgap(0);
		gridPanel.setBorder(createTitledBorder("Grid/Range"));

		minValueJLabel = new JLabel("Min");
		minValueJLabel.setEnabled(false);
		gridPanel.add(minValueJLabel);

		maxValueJLabel = new JLabel("Max");
		maxValueJLabel.setEnabled(false);
		gridPanel.add(maxValueJLabel);

		stepsValueJLabel = new JLabel("Steps");
		stepsValueJLabel.setEnabled(false);
		gridPanel.add(stepsValueJLabel);

		gridScaleValueJLabel = new JLabel("Scale");
		gridScaleValueJLabel.setEnabled(false);
		gridPanel.add(gridScaleValueJLabel);

		minValueTextField = new JFormattedTextField();
		minValueTextField.setText(Double.valueOf(0).toString());
		minValueTextField.setToolTipText("Minimum value of grid or range.");
		minValueTextField.setEnabled(false);
		minValueTextField.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {}

			@Override
			public void focusLost(FocusEvent e) {
				// prevent multiple updates, call will be triggered by selectedParameterList
				if (e.getOppositeComponent() == null || !e.getOppositeComponent().equals(selectedParametersList)) {
					updateSelectedNumericalParameterValues();
				}
			}
		});
		minValueTextField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (updateSelectedNumericalParameterValues()) {
					minValueTextField.transferFocus();
				}
			}
		});
		gridPanel.add(minValueTextField);

		maxValueTextField = new JFormattedTextField();
		maxValueTextField.setText(Double.valueOf(0).toString());
		maxValueTextField.setToolTipText("Maximum value of grid or range.");
		maxValueTextField.setEnabled(false);
		maxValueTextField.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {}

			@Override
			public void focusLost(FocusEvent e) {
				// prevent multiple updates, call will be triggered by selectedParameterList
				if (e.getOppositeComponent() == null || !e.getOppositeComponent().equals(selectedParametersList)) {
					updateSelectedNumericalParameterValues();
				}
			}
		});
		maxValueTextField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (updateSelectedNumericalParameterValues()) {
					maxValueTextField.transferFocus();
				}
			}
		});
		gridPanel.add(maxValueTextField);

		stepsValueTextField = new JFormattedTextField();
		stepsValueTextField.setText(Integer.valueOf(0).toString());
		stepsValueTextField.setToolTipText("Number of steps in grid.");
		stepsValueTextField.setEnabled(false);
		stepsValueTextField.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {}

			@Override
			public void focusLost(FocusEvent e) {
				// prevent multiple updates, call will be triggered by selectedParameterList
				if (e.getOppositeComponent() == null || !e.getOppositeComponent().equals(selectedParametersList)) {
					updateSelectedNumericalParameterValues();
				}
			}
		});
		stepsValueTextField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (updateSelectedNumericalParameterValues()) {
					stepsValueTextField.transferFocus();
				}
			}
		});
		gridPanel.add(stepsValueTextField);

		if (mode == ParameterConfigurator.VALUE_MODE_DISCRETE) {
			gridScaleValueComboBox = new JComboBox<String>(ParameterValueGrid.SCALES);
		} else {
			gridScaleValueComboBox = new JComboBox<String>();
		}
		gridScaleValueComboBox.setToolTipText("Grid scheme.");
		gridScaleValueComboBox.setEnabled(false);
		gridScaleValueComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				updateSelectedNumericalParameterValues();
			}
		});
		gridPanel.add(gridScaleValueComboBox);

		JPanel listPanel = new JPanel(new GridBagLayout());
		listPanel.setBorder(createTitledBorder("Value List"));

		categoriesListModel = new DefaultListModel<>();
		selectedCategoriesListModel = new DefaultListModel<>();

		createValueTextField = new JTextField();
		createValueTextField.setToolTipText("Type in a new value here.");
		createValueTextField.setEnabled(false);
		createValueTextField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				createListValue();
			}
		});
		c.insets = new Insets(GAP, 0, GAP, GAP);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.475;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;
		listPanel.add(createValueTextField, c);

		categoriesList = new JList<String>(categoriesListModel);
		categoriesList.setToolTipText("Available (or predefined) values.");
		categoriesList.setEnabled(false);
		c.insets = new Insets(0, 0, 0, GAP);
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0.475;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		JScrollPane categoriesListPane = new ExtendedJScrollPane(categoriesList) {

			private static final long serialVersionUID = 6534315233269693255L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension((int) (getWidth() * 0.5d), super.getPreferredSize().height);
			}
		};
		categoriesListPane.setBorder(createBorder());
		listPanel.add(categoriesListPane, c);

		if (ADD_ICON != null) {
			createValueButton = new JButton(ADD_ICON);
		} else {
			createValueButton = new JButton("+");
		}

		createValueButton.setToolTipText("Add a new value.");
		createValueButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				createListValue();
			}
		});
		createValueButton.setEnabled(false);
		c.insets = new Insets(GAP, 0, GAP, GAP);
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0.0;
		c.weighty = 0;
		c.fill = GridBagConstraints.NONE;
		listPanel.add(createValueButton, c);

		JPanel valueSelectionButtonsPanel = new JPanel(new BorderLayout());
		if (RIGHT_ICON != null) {
			addValueButton = new JButton(RIGHT_ICON);
		} else {
			addValueButton = new JButton(">");
		}
		addValueButton.setToolTipText("Select value from list of available values.");
		addValueButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedParameter = selectedParametersListModel.get(selectedParametersList.getLeadSelectionIndex());
				for (String selected : categoriesList.getSelectedValuesList()) {
					categoriesListModel.removeElement(selected);
					selectedCategoriesListModel.addElement(selected);
					ParameterValues parameterValue = parameterValuesMap.get(selectedParameter);
					if (parameterValue instanceof ParameterValueList) {
						((ParameterValueList) parameterValue).add(selected);
					}
				}
				updateInfoLabel();
			}
		});
		addValueButton.setEnabled(false);
		if (LEFT_ICON != null) {
			removeValueButton = new JButton(LEFT_ICON);
		} else {
			removeValueButton = new JButton("<");
		}
		removeValueButton.setToolTipText("Remove value from selection.");
		removeValueButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedParameter = selectedParametersListModel.get(selectedParametersList.getLeadSelectionIndex());
				for (String selected : selectedCategoriesList.getSelectedValuesList()) {
					selectedCategoriesListModel.removeElement(selected);
					ParameterValues parameterValue = parameterValuesMap.get(selectedParameter);
					if (parameterValue instanceof ParameterValueList) {
						if (((ParameterValueList) parameterValue).contains(selected)) {
							categoriesListModel.addElement(selected);
							((ParameterValueList) parameterValue).remove(selected);
						}
					}
				}
				updateInfoLabel();
			}
		});
		removeValueButton.setEnabled(false);
		valueSelectionButtonsPanel.add(addValueButton, BorderLayout.CENTER);
		valueSelectionButtonsPanel.add(removeValueButton, BorderLayout.SOUTH);
		c.insets = new Insets(0, 0, 0, GAP);
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 0.0;
		c.gridheight = 2;
		c.fill = GridBagConstraints.NONE;
		listPanel.add(valueSelectionButtonsPanel, c);

		selectedCategoriesList = new JList<>(selectedCategoriesListModel);
		selectedCategoriesList.setToolTipText("Selected values.");
		selectedCategoriesList.setEnabled(false);
		c.insets = new Insets(GAP, 0, 0, GAP);
		c.gridx = 2;
		c.gridy = 0;
		c.gridheight = 2;
		c.weightx = 0.475;
		c.fill = GridBagConstraints.BOTH;
		JScrollPane selectedCategoriesListPane = new ExtendedJScrollPane(selectedCategoriesList) {

			private static final long serialVersionUID = 4901808754076811895L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension((int) (getWidth() * 0.5d), super.getPreferredSize().height);
			}
		};
		selectedCategoriesListPane.setBorder(createBorder());
		listPanel.add(selectedCategoriesListPane, c);

		// ===

		JPanel valueMovingButtonsPanel = new JPanel(new BorderLayout());
		if (UP_ICON != null) {
			upListButton = new JButton(UP_ICON);
		} else {
			upListButton = new JButton("Up");
		}
		upListButton.setToolTipText("Move the selected value up in the list.");
		upListButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedParameter = selectedParametersListModel.get(selectedParametersList.getLeadSelectionIndex());
				int[] selectedIndices = selectedCategoriesList.getSelectedIndices();
				if (selectedIndices.length == 1) {
					ParameterValues parameterValue = parameterValuesMap.get(selectedParameter);
					if (parameterValue instanceof ParameterValueList) {
						String selectedValue = selectedCategoriesList.getSelectedValue();
						if (((ParameterValueList) parameterValue).contains(selectedValue)) {
							int selectedIndex = selectedIndices[0];
							if (selectedIndex >= 1) {
								parameterValue.move(selectedIndex, -1);
								selectedCategoriesListModel.remove(selectedIndex);
								selectedCategoriesListModel.add(selectedIndex - 1, selectedValue);
								selectedCategoriesList.setSelectedIndex(selectedIndex - 1);
							}
						}
					}
				}
			}
		});
		upListButton.setEnabled(false);

		if (DOWN_ICON != null) {
			downListButton = new JButton(DOWN_ICON);
		} else {
			downListButton = new JButton("Down");
		}
		downListButton.setToolTipText("Move the selected value down in the list.");
		downListButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedParameter = selectedParametersListModel.get(selectedParametersList.getLeadSelectionIndex());
				int[] selectedIndices = selectedCategoriesList.getSelectedIndices();
				if (selectedIndices.length == 1) {
					ParameterValues parameterValue = parameterValuesMap.get(selectedParameter);
					if (parameterValue instanceof ParameterValueList) {
						String selectedValue = selectedCategoriesList.getSelectedValue();
						if (((ParameterValueList) parameterValue).contains(selectedValue)) {
							int selectedIndex = selectedIndices[0];
							if (selectedIndex < selectedCategoriesListModel.size() - 1) {
								parameterValue.move(selectedIndex, 1);
								selectedCategoriesListModel.remove(selectedIndex);
								selectedCategoriesListModel.add(selectedIndex + 1, selectedValue);
								selectedCategoriesList.setSelectedIndex(selectedIndex + 1);

							}
						}
					}
				}
			}
		});
		downListButton.setEnabled(false);

		valueMovingButtonsPanel.add(upListButton, BorderLayout.CENTER);
		valueMovingButtonsPanel.add(downListButton, BorderLayout.SOUTH);
		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 3;
		c.gridy = 1;
		c.weightx = 0.0;
		c.gridheight = 2;
		c.fill = GridBagConstraints.NONE;
		listPanel.add(valueMovingButtonsPanel, c);

		// ===

		listPanel.setEnabled(false);

		JPanel valuePanel = new JPanel(new BorderLayout());
		valuePanel.add(gridPanel, BorderLayout.NORTH);
		valuePanel.add(listPanel, BorderLayout.CENTER);

		panel.add(valuePanel, BorderLayout.CENTER);
		return panel;
	}

	private JPanel createButtonPanel() {
		JPanel radioButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		choseGridRadioButton = new JRadioButton("Grid", true);
		choseGridRadioButton.setToolTipText("Use a regular grid for numerical parameters.");
		choseGridRadioButton.setEnabled(false);
		choseGridRadioButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (choseGridRadioButton.isSelected()) {
					switchToGrid();
				}
				choseGridRadioButton.setSelected(true);
				choseListRadioButton.setSelected(false);
			}
		});
		radioButtonPanel.add(choseGridRadioButton);
		choseListRadioButton = new JRadioButton("List", false);
		choseListRadioButton.setToolTipText("Use a list of single values for numerical parameters.");
		choseListRadioButton.setEnabled(false);
		choseListRadioButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (choseListRadioButton.isSelected()) {
					switchToList();
				}
				choseListRadioButton.setSelected(true);
				choseGridRadioButton.setSelected(false);
			}
		});
		radioButtonPanel.add(choseListRadioButton);

		JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		infoLabel = new JLabel();
		infoPanel.add(infoLabel);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okButton = new JButton(new ResourceAction("ok") {

			private static final long serialVersionUID = -5102786702723664410L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				ok();
			}
		});
		buttonPanel.add(okButton);
		JButton cancelButton = makeCancelButton("cancel");
		buttonPanel.add(cancelButton);
		getRootPane().setDefaultButton(okButton);

		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(radioButtonPanel, BorderLayout.WEST);
		bottomPanel.add(infoPanel, BorderLayout.CENTER);
		bottomPanel.add(buttonPanel, BorderLayout.EAST);
		return bottomPanel;
	}

	private void updateInfoLabel() {
		int size = parameterValuesMap.size();
		int combinations = 1;
		try {
			if (mode == ParameterConfigurator.VALUE_MODE_DISCRETE) {
				for (ParameterValues parameterValues : parameterValuesMap.values()) {
					int values = parameterValues.getNumberOfValues();
					combinations *= values == 0 ? 1 : values;
				}
				infoLabel.setText(
						size + " parameter" + (size > 1 ? "s" : "") + " / " + combinations + " combinations selected");
			} else {
				infoLabel.setText(size + " parameter" + (size > 1 ? "s" : "") + " selected");
			}
		} catch (NumberFormatException e) {
			infoLabel.setText(size + " parameter" + (size > 1 ? "s" : "") + " selected");
		}
	}

	private void createListValue() {
		String selectedParameter = selectedParametersListModel.get(selectedParametersList.getLeadSelectionIndex());
		String createdValue = createValueTextField.getText();
		if (createdValue.equals("")) {
			return;
		} else if (selectedCategoriesListModel.contains(createdValue)) {
			return;
		} else {
			selectedCategoriesListModel.addElement(createdValue);
			ParameterValues parameterValue = parameterValuesMap.get(selectedParameter);
			if (parameterValue instanceof ParameterValueList) {
				((ParameterValueList) parameterValue).add(createdValue);
			}
			if (categoriesListModel.contains(createdValue)) {
				categoriesListModel.removeElement(createdValue);
			}
		}
		createValueTextField.setText("");
		updateInfoLabel();
	}

	private void switchToGrid() {
		String key = selectedParametersList.getSelectedValue();
		if (key != null) {
			ParameterValues oldParameterValues = parameterValuesMap.get(key);
			if (oldParameterValues instanceof ParameterValueList) {
				ParameterValues newParameterValues = createNumericalParameterValues(oldParameterValues.getOperator(),
						oldParameterValues.getParameterType());
				parameterValuesMap.put(key, newParameterValues);
				fillComponents(newParameterValues);
			}
		}
		minValueJLabel.setEnabled(true);
		maxValueJLabel.setEnabled(true);
		stepsValueJLabel.setEnabled(true);
		gridScaleValueJLabel.setEnabled(true);
		minValueTextField.setEnabled(true);
		maxValueTextField.setEnabled(true);
		stepsValueTextField.setEnabled(true);
		gridScaleValueComboBox.setEnabled(true);
		categoriesList.setEnabled(false);
		selectedCategoriesList.setEnabled(false);
		addValueButton.setEnabled(false);
		removeValueButton.setEnabled(false);
		createValueButton.setEnabled(false);
		createValueTextField.setEnabled(false);
		upListButton.setEnabled(false);
		downListButton.setEnabled(false);
		updateInfoLabel();
	}

	private void switchToList() {
		String key = selectedParametersList.getSelectedValue();
		if (key != null) {
			ParameterValues oldParameterValues = parameterValuesMap.get(key);
			if (oldParameterValues instanceof ParameterValueGrid) {
				ParameterValues newParameterValues = new ParameterValueList(oldParameterValues.getOperator(),
						oldParameterValues.getParameterType());
				parameterValuesMap.put(key, newParameterValues);
				fillComponents(newParameterValues);
			}
		}
		minValueJLabel.setEnabled(false);
		maxValueJLabel.setEnabled(false);
		stepsValueJLabel.setEnabled(false);
		gridScaleValueJLabel.setEnabled(false);
		minValueTextField.setEnabled(false);
		maxValueTextField.setEnabled(false);
		stepsValueTextField.setEnabled(false);
		gridScaleValueComboBox.setEnabled(false);
		categoriesList.setEnabled(true);
		selectedCategoriesList.setEnabled(true);
		addValueButton.setEnabled(true);
		removeValueButton.setEnabled(true);
		createValueButton.setEnabled(true);
		createValueTextField.setEnabled(true);
		upListButton.setEnabled(true);
		downListButton.setEnabled(true);
		updateInfoLabel();
	}

	private void updateParameterListModel(Operator operator) {
		parametersListModel.removeAllElements();
		Collection<ParameterType> parameters = operator.getParameters().getParameterTypes();
		for (ParameterType parameter : parameters) {
			// do not show parameters that are not numerical in continuous mode
			if (mode == ParameterConfigurator.VALUE_MODE_CONTINUOUS) {
				if (!operator.getParameterType(parameter.getKey()).isNumerical()) {
					continue;
				}
			}

			if (!parameterValuesMap.containsKey(operator.getName() + "." + parameter.getKey())) {
				parametersListModel.addElement(parameter.getKey(), parameter.getDescription());
				if (parameter.isNumerical() || parameter instanceof ParameterTypeCategory
						|| parameter instanceof ParameterTypeStringCategory || parameter instanceof ParameterTypeString
						|| parameter instanceof ParameterTypeBoolean || parameter instanceof ParameterTypeFile) {
				} else {
					parametersListModel.setEnabled(parameter.getKey(), false);
				}
			}
		}
	}

	private void addSelectedParameters() {
		Operator operator = operatorList.getSelectedValue();
		for (String parameterKey : parametersList.getSelectedValuesList()) {
			ParameterType type = operator.getParameterType(parameterKey);

			ParameterValues parameterValue = null;
			if (type.isNumerical()) {
				parameterValue = createNumericalParameterValues(operator, type);
			} else {
				if (type instanceof ParameterTypeCategory || type instanceof ParameterTypeStringCategory
						|| type instanceof ParameterTypeString || type instanceof ParameterTypeBoolean
						|| type instanceof ParameterTypeFile) {
					parameterValue = new ParameterValueList(operator, type, getDefaultListParameterValues(type));
				}
			}
			if (parameterValue != null) {
				addParameter(parameterValue);
			}
		}
		updateInfoLabel();
	}

	private void addParameter(ParameterValues parameterValue) {
		String key = parameterValue.getKey();
		parameterValuesMap.put(key, parameterValue);
		selectedParametersListModel.addElement(key, parameterValue.getParameterType().getDescription());
		parametersListModel.removeElement(parameterValue.getParameterType().getKey());
	}

	private void removeSelectedParameters() {
		for (String selected : selectedParametersList.getSelectedValuesList()) {
			String operatorName = ParameterTypeTupel.transformString2Tupel(selected)[0];
			// String operatorName = ((String)selectedParameters[i]).substring(0,
			// ((String)selectedParameters[i]).indexOf("."));
			selectedParametersListModel.removeElement(selected);
			parameterValuesMap.remove(selected);
			int index = operatorList.getSelectedIndex();
			if (index != -1) {
				Operator op = operatorList.getModel().getElementAt(index);
				if (op == process.getOperator(operatorName)) {
					updateParameterListModel(op);
				}
			}
		}
		updateInfoLabel();
	}

	private void enableComponents(ParameterValues parameterValue) {
		minValueJLabel.setEnabled(false);
		maxValueJLabel.setEnabled(false);
		stepsValueJLabel.setEnabled(false);
		gridScaleValueJLabel.setEnabled(false);
		minValueTextField.setEnabled(false);
		maxValueTextField.setEnabled(false);
		stepsValueTextField.setEnabled(false);
		gridScaleValueComboBox.setEnabled(false);

		categoriesList.setEnabled(false);
		selectedCategoriesList.setEnabled(false);
		addValueButton.setEnabled(false);
		removeValueButton.setEnabled(false);
		createValueButton.setEnabled(false);
		createValueTextField.setEnabled(false);
		upListButton.setEnabled(false);
		downListButton.setEnabled(false);

		choseGridRadioButton.setEnabled(false);
		choseListRadioButton.setEnabled(false);

		if (parameterValue != null) {
			ParameterType type = parameterValue.getParameterType();
			if (type instanceof ParameterTypeBoolean || type instanceof ParameterTypeCategory) {
				categoriesList.setEnabled(true);
				selectedCategoriesList.setEnabled(true);
				addValueButton.setEnabled(true);
				removeValueButton.setEnabled(true);
				upListButton.setEnabled(true);
				downListButton.setEnabled(true);
			} else if (type instanceof ParameterTypeNumber || type instanceof ParameterTypeString) {
				if (!(parameterValue instanceof ParameterValueRange)) {
					choseGridRadioButton.setEnabled(true);
					choseListRadioButton.setEnabled(true);
				}
				if (parameterValue instanceof ParameterValueList) {
					categoriesList.setEnabled(true);
					selectedCategoriesList.setEnabled(true);
					addValueButton.setEnabled(true);
					removeValueButton.setEnabled(true);
					createValueTextField.setEnabled(true);
					createValueButton.setEnabled(true);
					choseGridRadioButton.setSelected(false);
					choseListRadioButton.setSelected(true);
					upListButton.setEnabled(true);
					downListButton.setEnabled(true);
				} else {
					minValueJLabel.setEnabled(true);
					maxValueJLabel.setEnabled(true);
					minValueTextField.setEnabled(true);
					maxValueTextField.setEnabled(true);
					if (parameterValue instanceof ParameterValueGrid) {
						stepsValueJLabel.setEnabled(true);
						gridScaleValueJLabel.setEnabled(true);
						stepsValueTextField.setEnabled(true);
						gridScaleValueComboBox.setEnabled(true);
						choseGridRadioButton.setSelected(true);
						choseListRadioButton.setSelected(false);
					}
				}
			} else if (type instanceof ParameterTypeStringCategory || type instanceof ParameterTypeValue
					|| type instanceof ParameterTypeFile) {
				categoriesList.setEnabled(true);
				selectedCategoriesList.setEnabled(true);
				createValueButton.setEnabled(true);
				createValueTextField.setEnabled(true);
				addValueButton.setEnabled(true);
				removeValueButton.setEnabled(true);
				upListButton.setEnabled(true);
				downListButton.setEnabled(true);
			}
		}
	}

	private void showGridValues(ParameterValueGrid parameterValueGrid) {
		selectedCategoriesListModel.removeAllElements();
		try {
			double[] gridValues = parameterValueGrid.getValues();
			for (int i = 0; i < gridValues.length; i++) {
				selectedCategoriesListModel.addElement(Tools.formatIntegerIfPossible(gridValues[i]));
			}
		} catch (NumberFormatException e) {
			// TODO show message in list?!
		}
	}

	private void fillComponents(ParameterValues parameterValue) {
		categoriesListModel.removeAllElements();
		selectedCategoriesListModel.removeAllElements();
		if (parameterValue instanceof ParameterValueRange) {
			ParameterValueRange parameterValueRange = (ParameterValueRange) parameterValue;
			minValueTextField.setText(parameterValueRange.getMin());
			maxValueTextField.setText(parameterValueRange.getMax());
		} else if (parameterValue instanceof ParameterValueGrid) {
			ParameterValueGrid parameterValueGrid = (ParameterValueGrid) parameterValue;
			minValueTextField.setText(parameterValueGrid.getMin());
			maxValueTextField.setText(parameterValueGrid.getMax());
			stepsValueTextField.setText(parameterValueGrid.getSteps());
			gridScaleValueComboBox.setSelectedIndex(parameterValueGrid.getScale());
			showGridValues(parameterValueGrid);
		} else if (parameterValue instanceof ParameterValueList) {
			ParameterValueList parameterValueList = (ParameterValueList) parameterValue;
			ParameterType type = parameterValueList.getParameterType();
			for (String value : parameterValueList) {
				selectedCategoriesListModel.addElement(value);
			}
			String[] categories = getDefaultListParameterValues(type);
			if (categories != null) {
				for (int i = 0; i < categories.length; i++) {
					if (!parameterValueList.contains(categories[i])) {
						categoriesListModel.addElement(categories[i]);
					}
				}
			}
		}
	}

	private void showParameterValues(String key) {
		if (key == null) {
			enableComponents(null);
			return;
		}
		ParameterValues parameterValues = parameterValuesMap.get(key);
		fillComponents(parameterValues);
		enableComponents(parameterValues);
	}

	/*
	 * private void finishTextFieldEdit(JFormattedTextField textField) { try {
	 * textField.commitEdit(); } catch (Exception e) { System.err.println(e.getMessage()); return; }
	 * updateSelectedNumericalParameterValues(); }
	 */

	private boolean updateSelectedNumericalParameterValues() {
		return updateNumericalParameterValues(selectedParametersList.getSelectedIndex());
	}

	private boolean updateNumericalParameterValues(int index) {
		if (index == -1) {
			enableComponents(null);
			return false;
		}
		String key = selectedParametersListModel.get(index);
		ParameterValues parameterValues = parameterValuesMap.get(key);
		if (parameterValues != null) {
			if (parameterValues instanceof ParameterValueGrid) {
				ParameterValueGrid parameterValueGrid = (ParameterValueGrid) parameterValues;
				String value = minValueTextField.getText();
				if (ParameterValues.isValidNumericalParameter(value)) {
					parameterValueGrid.setMin(value);
				} else {
					minValueTextField.setText(parameterValueGrid.getMin());
					SwingTools.showVerySimpleErrorMessage(this, "parameter_grid", "minimum value", value);
					return false;
				}
				value = maxValueTextField.getText();
				if (ParameterValues.isValidNumericalParameter(value)) {
					parameterValueGrid.setMax(value);
				} else {
					maxValueTextField.setText(parameterValueGrid.getMax());
					SwingTools.showVerySimpleErrorMessage(this, "parameter_grid", "maximum value", value);
					return false;
				}
				value = stepsValueTextField.getText();
				if (ParameterValues.isValidNumericalParameter(value)) {
					parameterValueGrid.setSteps(value);
				} else {
					stepsValueTextField.setText(parameterValueGrid.getSteps());
					SwingTools.showVerySimpleErrorMessage(this, "parameter_grid", "number of steps", value);
					return false;
				}
				parameterValueGrid.setScale(gridScaleValueComboBox.getSelectedIndex());
				showGridValues(parameterValueGrid);
			}
			if (parameterValues instanceof ParameterValueRange) {
				ParameterValueRange parameterValueRange = (ParameterValueRange) parameterValues;
				String value = minValueTextField.getText();
				if (ParameterValues.isValidNumericalParameter(value)) {
					parameterValueRange.setMin(value);
				} else {
					minValueTextField.setText(parameterValueRange.getMin());
					SwingTools.showVerySimpleErrorMessage(this, "parameter_range", "minimum value", value);
					return false;
				}
				value = maxValueTextField.getText();
				if (ParameterValues.isValidNumericalParameter(value)) {
					parameterValueRange.setMax(value);
				} else {
					maxValueTextField.setText(parameterValueRange.getMax());
					SwingTools.showVerySimpleErrorMessage(this, "parameter_range", "maximum value", value);
					return false;
				}
			}
		}
		updateInfoLabel();
		return true;
	}

	private ParameterValues createNumericalParameterValues(Operator operator, ParameterType type) {
		double min;
		double max;
		if (type instanceof ParameterTypeNumber) {
			min = ((ParameterTypeNumber) type).getMinValue();
			max = ((ParameterTypeNumber) type).getMaxValue();
			if (min == Integer.MIN_VALUE) {
				min = 0;
			}
			if (max == Integer.MAX_VALUE) {
				max = 100;
			}
		} else {
			String value = minValueTextField.getText();
			if (ParameterValues.isValidNumericalParameter(value)) {
				min = Double.parseDouble(value);
			} else {
				min = 0;
			}

			value = maxValueTextField.getText();
			if (ParameterValues.isValidNumericalParameter(value)) {
				max = Double.parseDouble(value);
			} else {
				max = 100;
			}
		}

		if (mode == ParameterConfigurator.VALUE_MODE_DISCRETE) {
			return new ParameterValueGrid(operator, type, Double.toString(min), Double.toString(max));
		} else {
			return new ParameterValueRange(operator, type, Double.toString(min), Double.toString(max));
		}
	}

	private String[] getDefaultListParameterValues(ParameterType type) {
		if (type instanceof ParameterTypeCategory) {
			return ((ParameterTypeCategory) type).getValues();
		} else if (type instanceof ParameterTypeStringCategory) {
			return ((ParameterTypeStringCategory) type).getValues();
		} else if (type instanceof ParameterTypeBoolean) {
			return new String[] { "true", "false" };
		} else {
			return new String[] {};
		}
	}

	@Override
	protected void ok() {
		updateSelectedNumericalParameterValues();
		ok = true;
		List<String[]> parameterList = new LinkedList<>();
		for (String key : parameterValuesMap.keySet()) {
			String value = parameterValuesMap.get(key).getValuesString();
			parameterList.add(new String[] { key, value });
		}
		Parameters parameters = listener.getParameters();
		parameters.setParameter(ParameterConfigurator.PARAMETER_PARAMETERS,
				ParameterTypeList.transformList2String(parameterList));
		listener.setParameters(parameters);
		dispose();
	}

	@Override
	protected void cancel() {
		ok = false;
		dispose();
	}

	@Override
	public boolean isOk() {
		return ok;
	}
}
