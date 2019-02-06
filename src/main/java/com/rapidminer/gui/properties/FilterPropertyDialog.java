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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.properties.tablepanel.TablePanel;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.preprocessing.filter.ExampleFilter;
import com.rapidminer.parameter.ParameterTypeFilter;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.tools.I18N;


/**
 * This dialog displays the custom filters for the {@link ExampleFilter} operator.
 *
 * @author Marco Boeck
 *
 */
public class FilterPropertyDialog extends PropertyDialog {

	private static final long serialVersionUID = 2417533731869697659L;

	/** the operator for this dialog */
	private final Operator operator;

	/** the model of the TablePanel for this dialog */
	private FilterTableModel model;

	private JRadioButton radioButtonAND;
	private JRadioButton radioButtonOR;
	private JCheckBox checkBoxMetadata;

	/**
	 * Creates a new {@link FilterPropertyDialog}.
	 *
	 * @param type
	 * @param key
	 */
	public FilterPropertyDialog(final Operator operator, final ParameterTypeFilter type, final String key) {
		super(type, key);
		if (operator == null) {
			throw new IllegalArgumentException("operator must not be null!");
		}
		this.operator = operator;

		initGUI(type);
		initOperatorSettings();
	}

	/**
	 * Initializes the GUI.
	 */
	private void initGUI(final ParameterTypeFilter type) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		model = new FilterTableModel(type.getInputPort());
		final TablePanel innerPanel = new TablePanel(model, true, false);
		innerPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

		JButton okButton = new JButton(new ResourceAction("ok") {

			private static final long serialVersionUID = 2265489760585034488L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {
				okAction();
			}
		});
		// deliberately not the normal ButtonDialog OK button -> it's very irritating to press Enter
		// to confirm your value
		// and have your dialog vanish. This behavior is ok for powerusers who love their keyboard
		// to death,
		// and know everything inside out, but the vast majority are NOT powerusers
		// and for them the default behavior is just annoying
		okButton.setMnemonic(I18N.getMessage(I18N.getGUIBundle(), "gui.action.settings_ok.mne").charAt(0));
		JButton cancelButton = makeCancelButton();
		JButton addRowButton = new JButton(new ResourceAction("list.add_row") {

			private static final long serialVersionUID = 5289974084350157673L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {
				innerPanel.getModel().appendRow();
			}
		});

		// add radio buttons to chose between AND and OR for the filters
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridBagLayout());

		ButtonGroup logicRadioButtonGroup = new ButtonGroup();
		radioButtonAND = new JRadioButton(new ResourceActionAdapter(true, "filter_property_dialog.radio_and") {

			private static final long serialVersionUID = -7003906588940462016L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {} // only used as a flag

		});
		radioButtonAND.setSelected(true);
		radioButtonOR = new JRadioButton(new ResourceActionAdapter(true, "filter_property_dialog.radio_or") {

			private static final long serialVersionUID = -7003906588940462016L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {} // only used as a flag

		});

		checkBoxMetadata = new JCheckBox(new ResourceActionAdapter(true, "filter_property_dialog.check_metadata") {

			private static final long serialVersionUID = -7003906588940462016L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {

				if (checkBoxMetadata.isSelected()) {
					model.setCheckMetaDataForComparators(true);
				} else {
					model.setCheckMetaDataForComparators(false);
				}
			}
		});
		checkBoxMetadata.setSelected(true);

		logicRadioButtonGroup.add(radioButtonAND);
		logicRadioButtonGroup.add(radioButtonOR);

		GridBagConstraints gbc = new GridBagConstraints();
		JLabel labelLogic = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.filter_property_dialog.logic.title"));
		labelLogic.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.filter_property_dialog.logic.tip"));
		gbc.gridx = 1;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 5, 5, 5);
		buttonPanel.add(labelLogic, gbc);

		gbc.gridx = 2;
		buttonPanel.add(radioButtonAND, gbc);

		gbc.gridx = 3;
		buttonPanel.add(radioButtonOR, gbc);

		gbc.gridx = 4;
		gbc.insets = new Insets(5, 25, 5, 5);
		buttonPanel.add(checkBoxMetadata, gbc);

		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.weightx = 1.0;
		gbc.gridx = 5;
		buttonPanel.add(Box.createHorizontalBox(), gbc);

		gbc.weightx = 0.0;
		gbc.gridx = 6;
		buttonPanel.add(addRowButton, gbc);

		gbc.gridx = 7;
		buttonPanel.add(okButton, gbc);

		gbc.gridx = 8;
		buttonPanel.add(cancelButton, gbc);

		panel.add(innerPanel, BorderLayout.CENTER);
		setLocationRelativeTo(ApplicationFrame.getApplicationFrame());
		setDefaultSize(ButtonDialog.LARGE);
		layoutDefault(panel, buttonPanel);
	}

	/**
	 * Initializes this dialog with the existing filters stored for the operator.
	 */
	private void initOperatorSettings() {
		// add filters to model
		List<String[]> tupelList = new LinkedList<>();
		// this is necessary as operator.getParameterList() replaces '%{test}' by 'test'
		String rawParameterString = operator.getParameters().getParameterAsSpecified(ExampleFilter.PARAMETER_FILTERS_LIST);
		List<String[]> operatorFilterList = rawParameterString != null ? ParameterTypeList
				.transformString2List(rawParameterString) : Collections.<String[]> emptyList();
				for (String[] entry : operatorFilterList) {
					tupelList.add(ParameterTypeTupel.transformString2Tupel(entry[1]));
				}
				model.setRowTupels(tupelList);
				// set logic operation in dialog
				if (operator.getParameterAsBoolean(ExampleFilter.PARAMETER_FILTERS_LOGIC_AND)) {
					radioButtonAND.setSelected(true);
				} else {
					radioButtonOR.setSelected(true);
				}
				// set selection for checking meta data
				if (!operator.getParameterAsBoolean(ExampleFilter.PARAMETER_FILTERS_CHECK_METADATA)) {
					checkBoxMetadata.setSelected(false);
					model.setCheckMetaDataForComparators(false);
				}
	}

	/**
	 * Stores the settings from the model to the operator via its parameters.
	 */
	private void okAction() {
		// add filters to hidden filter list parameter
		List<String[]> filterList = new LinkedList<>();
		for (String[] tupel : model.getRowTupels()) {
			filterList.add(new String[] { ExampleFilter.PARAMETER_FILTERS_ENTRY_KEY,
					ParameterTypeTupel.transformTupel2String(tupel) });
		}
		operator.setListParameter(ExampleFilter.PARAMETER_FILTERS_LIST, filterList);
		// set logic operation
		operator.setParameter(ExampleFilter.PARAMETER_FILTERS_LOGIC_AND, Boolean.toString(radioButtonAND.isSelected()));
		// add selection about checking meta data to hidden filter parameter
		operator.setParameter(ExampleFilter.PARAMETER_FILTERS_CHECK_METADATA,
				Boolean.toString(checkBoxMetadata.isSelected()));

		dispose();
	}

	@Override
	public void setVisible(final boolean b) {
		if (model.getRowCount() == 0) {
			model.appendRow();
		}
		super.setVisible(b);
	}
}
