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

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.FilterTextField;
import com.rapidminer.gui.tools.FilterableListModel;
import com.rapidminer.gui.tools.FilterableListModel.FilterCondition;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.InputDialog;
import com.rapidminer.parameter.ParameterTypeAttributeOrderingRules;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.container.Pair;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;


/**
 * A dialog to generate rules for attribute ordering.
 * 
 * @author Nils Woehler
 */
public class AttributeOrderingDialog extends PropertyDialog {

	private static final long serialVersionUID = 5396725165122306231L;

	private final List<String> attributes;

	private final List<String> selectedRules;

	private final FilterTextField attributeSearchField;

	private final JTextField addRuleTextField;

	private final FilterableListModel<String> attributeListModel;

	private final DefaultListModel<String> selectedRulesListModel;

	private final JList<String> attributeList;

	private final JList<String> selectedRulesList;

	private FilterCondition currentTextFieldCondition;

	private final Map<String, FilterCondition> ruleToConditionMap = new HashMap<>();

	private final Action selectAttributesAction = new ResourceAction(true, "attribute_ordering.attributes_select") {

		private static final long serialVersionUID = -3046621278306353077L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			int[] indices = attributeList.getSelectedIndices();
			attributeList.setSelectedIndices(new int[] {});
			List<String> selectedItems = new LinkedList<>();
			for (int i = 0; i < indices.length; i++) {
				selectedItems.add(attributeListModel.getElementAt(indices[i]));

			}
			for (String item : selectedItems) {
				attributeListModel.removeElement(item);
				attributes.remove(item);

				selectedRulesListModel.addElement(item);
				selectedRules.add(item);

			}
			addFilterConditions(selectedItems);

		}
	};

	private final Action deselectAttributesAction = new ResourceAction(true, "attribute_ordering.attributes_deselect") {

		private static final long serialVersionUID = -3046621278306353077L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			int[] indices = selectedRulesList.getSelectedIndices();
			selectedRulesList.setSelectedIndices(new int[] {});
			List<String> selectedItems = new LinkedList<>();
			for (int i = 0; i < indices.length; i++) {
				selectedItems.add(selectedRulesListModel.getElementAt(indices[i]));

			}
			for (String item : selectedItems) {

				selectedRulesListModel.removeElement(item);
				selectedRules.remove(item);

				if (!attributes.contains(item)) {
					attributes.add(item);
					attributeListModel.addElement(item);
				}

			}
			removeFilterConditions(selectedItems);
		}
	};

	private final Action addOrderingRuleAction = new ResourceAction(true, "attribute_ordering.add") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			final String newRule = addRuleTextField.getText();

			// clear text field
			addRuleTextField.setText("");
			addRuleTextField.requestFocusInWindow();

			if (newRule != null && newRule.trim().length() != 0 && !(selectedRules.contains(newRule))) {
				// add rule to list
				selectedRules.add(newRule);
				selectedRulesListModel.addElement(newRule);

				attributes.remove(newRule);
				attributeListModel.removeElement(newRule);

				// apply filter to attributes if button is pressed
				addFilterCondition(newRule);

				// remove old condition
				currentTextFieldCondition = null;
			}

		}
	};

	private final Action moveRuleUpAction = new ResourceAction(true, "attribute_ordering.up") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {

			int[] selectedIndices = selectedRulesList.getSelectedIndices();
			if (selectedIndices.length != 0) {
				int firstIndex = selectedIndices[0];
				if (firstIndex > 0) {

					for (int i = 0; i < selectedIndices.length; i++) {

						// bubble sort rules list model
						int currentIndex = selectedIndices[i];
						String movedDown = selectedRulesListModel.get(currentIndex - 1);
						String movedUp = selectedRulesListModel.get(currentIndex);

						selectedRulesListModel.set(currentIndex, movedDown);
						selectedRulesListModel.set(currentIndex - 1, movedUp);

						// for arrays same
						selectedRules.set(currentIndex, movedDown);
						selectedRules.set(currentIndex - 1, movedUp);

						selectedIndices[i] = currentIndex - 1;
					}
					selectedRulesList.setSelectedIndices(selectedIndices);
				}
			}

		}
	};

	private final Action moveRuleDownAction = new ResourceAction(true, "attribute_ordering.down") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {

			int[] selectedIndices = selectedRulesList.getSelectedIndices();
			if (selectedIndices.length != 0) {
				int lastIndex = selectedIndices[selectedIndices.length - 1];
				if (lastIndex < selectedRulesListModel.size() - 1) {

					for (int i = selectedIndices.length - 1; i >= 0; i--) {

						// bubble sort rules list model
						int currentIndex = selectedIndices[i] + 1;
						String movedDown = selectedRulesListModel.get(currentIndex - 1);
						String movedUp = selectedRulesListModel.get(currentIndex);

						selectedRulesListModel.set(currentIndex, movedDown);
						selectedRulesListModel.set(currentIndex - 1, movedUp);

						// for arrays same
						selectedRules.set(currentIndex, movedDown);
						selectedRules.set(currentIndex - 1, movedUp);

						selectedIndices[i] = currentIndex;
					}
					selectedRulesList.setSelectedIndices(selectedIndices);
				}
			}

		}

	};

	private final Action editRuleAction = new ResourceAction(true, "attribute_ordering.rename") {

		private static final long serialVersionUID = 1L;

		@SuppressWarnings("deprecation")
		@Override
		public void loggedActionPerformed(ActionEvent e) {
			int[] selectedIndices = selectedRulesList.getSelectedIndices();
			if (selectedIndices.length != 0) {
				int currentIndex = selectedIndices[0];
				String oldRule = selectedRules.get(currentIndex);

				InputDialog inputDialog = new InputDialog("attribute_ordering", oldRule);
				inputDialog.setVisible(true);

				if (inputDialog.wasConfirmed()) {
					String newRule = inputDialog.getInputText();
					selectedRules.set(currentIndex, newRule);
					selectedRulesListModel.set(currentIndex, newRule);
					ruleToConditionMap.remove(oldRule);
					addFilterCondition(newRule);
				}
			}

		}
	};

	private final Action hideMatchedAction = new ResourceAction(true, "attribute_ordering.hide_unmatched") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			applyFilterConditions();
		}
	};

	private JToggleButton hideMatchedButton;

	public AttributeOrderingDialog(final ParameterTypeAttributeOrderingRules type, Collection<String> preselectedItems,
			boolean useRegExp) {
		super(type, "attribute_ordering");
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		attributes = type.getAttributeNamesAndTypes(false).stream().map(Pair::getFirst)
				.filter(name -> !preselectedItems.contains(name)).collect(Collectors.toList());
		attributes.sort(FilterableListModel.STRING_COMPARATOR);
		attributeListModel = new FilterableListModel<>(attributes, false);
		attributeListModel.setComparator(FilterableListModel.STRING_COMPARATOR);

		selectedRules = new ArrayList<>();
		selectedRulesListModel = new DefaultListModel<>();
		if (!preselectedItems.isEmpty()) {
			for (String item : preselectedItems) {
				if (item != null && item.trim().length() != 0) {
					selectedRules.add(item);
					selectedRulesListModel.addElement(item);
				}
			}
		}

		// --------------------- LEFT SIDE ---------------------------------------

		attributeSearchField = new FilterTextField();
		attributeSearchField.addFilterListener(attributeListModel);
		JButton itemSearchFieldClearButton = new JButton(new ResourceAction(true, "attribute_ordering.clear") {

			private static final long serialVersionUID = -3046621278306353077L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				attributeSearchField.clearFilter();
				attributeSearchField.requestFocusInWindow();
			}
		});
		JPanel itemSearchFieldPanel = new JPanel(new GridBagLayout());
		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;
		itemSearchFieldPanel.add(attributeSearchField, c);

		c.gridx = 1;
		c.weightx = 0;
		itemSearchFieldPanel.add(itemSearchFieldClearButton, c);

		hideMatchedButton = new JToggleButton(hideMatchedAction);
		hideMatchedButton.setSize(20, 20);
		hideMatchedButton.setPreferredSize(new Dimension(30, 25));
		hideMatchedButton.setEnabled(useRegExp);
		hideMatchedButton.setSelected(useRegExp);

		c.gridx = 2;
		c.weightx = 0;
		itemSearchFieldPanel.add(hideMatchedButton, c);

		attributeList = new JList<>(attributeListModel);
		attributeList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					selectAttributesAction.actionPerformed(null);
				}
			}
		});
		attributeList.setCellRenderer(new ListCellRenderer<String>() {

			DefaultListCellRenderer renderer = new DefaultListCellRenderer();

			@Override
			public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
					boolean isSelected, boolean cellHasFocus) {
				Component renderComp = renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (currentTextFieldCondition != null && currentTextFieldCondition.matches(value)) {
					renderComp.setForeground(Color.red);
				} else {
					renderComp.setForeground(Color.black);
				}

				return renderComp;
			}
		});
		JScrollPane attributeListPane = new ExtendedJScrollPane(attributeList);
		attributeListPane.setBorder(createBorder());
		JPanel attributeListPanel = new JPanel(new GridBagLayout());

		c.insets = new Insets(4, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;
		attributeListPanel.add(itemSearchFieldPanel, c);

		c.gridy = 1;
		c.weighty = 1;
		attributeListPanel.add(attributeListPane, c);
		attributeListPanel
				.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".attributes.border")));

		// ----------------------------- RIGHT SIDE ---------------------------------------

		JPanel selectedRulesListPanel = new JPanel(new GridBagLayout());

		JPanel addRulePanel = new JPanel(new GridBagLayout());
		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;

		addRuleTextField = new JTextField();
		addRuleTextField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				if (hideMatchedButton.isSelected()) {

					String text = addRuleTextField.getText();
					if (text != null && text.trim().length() != 0) {
						currentTextFieldCondition = createNewCondition(text);
					} else {
						currentTextFieldCondition = null;
					}
					attributeList.repaint();

				}
			}
		});
		addRulePanel.add(addRuleTextField, c);

		JButton addRuleButton = new JButton(addOrderingRuleAction);
		c.gridx = 1;
		c.weightx = 0;
		addRulePanel.add(addRuleButton, c);

		c.insets = new Insets(4, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		selectedRulesListPanel.add(addRulePanel, c);

		JPanel orderingListAndButtonContainer = new JPanel(new GridBagLayout());

		selectedRulesList = new JList<>(selectedRulesListModel);
		selectedRulesList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					deselectAttributesAction.actionPerformed(null);
				}
			}
		});
		JScrollPane selectedRulesListPane = new ExtendedJScrollPane(selectedRulesList);
		selectedRulesListPane.setBorder(createBorder());

		c.insets = new Insets(4, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		orderingListAndButtonContainer.add(selectedRulesListPane, c);

		// add right buttons
		JPanel rightButtonPanel = new JPanel(new GridLayout(3, 1));
		JButton moveUpButton = new JButton(moveRuleUpAction);
		JButton moveDownButton = new JButton(moveRuleDownAction);
		JButton renameButton = new JButton(editRuleAction);
		rightButtonPanel.add(moveUpButton, 0, 0);
		rightButtonPanel.add(moveDownButton, 1, 1);
		rightButtonPanel.add(renameButton, 2, 1);

		c.insets = new Insets(4, 4, 4, 4);
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 1;
		c.fill = GridBagConstraints.NONE;
		orderingListAndButtonContainer.add(rightButtonPanel, c);

		c.insets = new Insets(4, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;

		selectedRulesListPanel.add(orderingListAndButtonContainer, c);
		selectedRulesListPanel.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), getKey()
				+ ".selected_attributes.border")));

		// add available attributes panel
		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.5;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		panel.add(attributeListPanel, c);

		// add middle buttons

		JPanel midButtonPanel = new JPanel(new GridLayout(2, 1));
		JButton selectButton = new JButton(selectAttributesAction);
		JButton deselectButton = new JButton(deselectAttributesAction);
		midButtonPanel.add(deselectButton, 0, 0);
		midButtonPanel.add(selectButton, 1, 0);
		c.insets = new Insets(4, 4, 4, 4);
		c.gridx = 1;
		c.weightx = 0;
		c.weighty = 1;
		c.fill = GridBagConstraints.NONE;
		panel.add(midButtonPanel, c);

		// add selected rules panel

		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 0.5;
		c.fill = GridBagConstraints.BOTH;
		panel.add(selectedRulesListPanel, c);

		Dimension d = panel.getPreferredSize();
		d.setSize(d.getWidth() / 2, d.getHeight());
		attributeListPanel.setPreferredSize(d);
		selectedRulesListPanel.setPreferredSize(d);

		layoutDefault(panel, NORMAL, makeOkButton("attribute_ordering_dialog_order"), makeCancelButton());
		addRuleTextField.requestFocusInWindow();

		// finally add conditions
		addFilterConditions(selectedRules);
	}

	public Collection<String> getSelectedAttributeNames() {
		return selectedRules;
	}

	private FilterCondition createNewCondition(final String rule) {
		return new FilterCondition() {

			@Override
			public String toString() {
				return "Matching rule: '" + rule + "'";
			}

			@Override
			public boolean matches(Object o) {
				try {
					return o.toString().matches(rule);
				} catch (Exception e) {
					return false;
				}
			}

		};
	}

	private void addFilterConditions(List<String> rules) {
		for (String rule : rules) {
			ruleToConditionMap.put(rule, createNewCondition(rule));
		}
		applyFilterConditions();
	}

	private void addFilterCondition(final String rule) {
		ruleToConditionMap.put(rule, createNewCondition(rule));
		applyFilterConditions();
	}

	private void removeFilterConditions(List<String> rules) {
		for (String rule : rules) {
			ruleToConditionMap.remove(rule);
		}
		applyFilterConditions();
	}

	private void applyFilterConditions() {
		attributeListModel.removeAllConditions();
		if (hideMatchedButton.isSelected()) {
			attributeListModel.addConditions(ruleToConditionMap.values());
		}
	}
}
