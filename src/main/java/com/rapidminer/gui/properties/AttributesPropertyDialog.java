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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.rapidminer.gui.tools.AttributeGuiTools;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.FilterTextField;
import com.rapidminer.gui.tools.FilterableListModel;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.parameter.ParameterTypeAttributes;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.container.Pair;


/**
 * A dialog to select a subset of attributes.
 * 
 * @author Tobias Malbrecht
 */
public class AttributesPropertyDialog extends PropertyDialog {

	private static final long serialVersionUID = 5396725165122306231L;

	private final List<String> items;

	private final List<String> selectedItems;

	private final Map<String, Integer> valueTypeMap;

	private final FilterTextField itemSearchField;

	private final FilterTextField selectedItemSearchField;

	private final FilterableListModel<String> itemListModel;

	private final FilterableListModel<String> selectedItemListModel;

	private final JList<String> itemList;

	private final JList<String> selectedItemList;

	private final Action selectAttributesAction = new ResourceAction(true, "attributes_select") {

		private static final long serialVersionUID = -3046621278306353077L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			List<String> selectedValues = itemList.getSelectedValuesList();
			itemList.setSelectedIndices(new int[] {});
			for (String item : selectedValues) {
				selectedItemListModel.addElement(item);
				itemListModel.removeElement(item);
				selectedItems.add(item);
				items.remove(item);
			}
		}
	};

	private final Action deselectAttributesAction = new ResourceAction(true, "attributes_deselect") {

		private static final long serialVersionUID = -3046621278306353077L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			int[] indices = selectedItemList.getSelectedIndices();
			selectedItemList.setSelectedIndices(new int[] {});
			for (int i = indices.length - 1; i >= 0; i--) {
				String item = selectedItemListModel.getElementAt(indices[i]);
				itemListModel.addElement(item);
				selectedItemListModel.removeElementAt(indices[i]);
				items.add(item);
				selectedItems.remove(item);
			}
		}
	};

	public AttributesPropertyDialog(final ParameterTypeAttributes type, Collection<String> preselectedItems) {
		this(type, preselectedItems, true);
	}

	/**
	 * Creates a dialog instance.
	 *
	 * @param type
	 * 		the parameter type attribute instance
	 * @param preselectedItems
	 * 		the preselected attribute names
	 * @param sortAttributes
	 * 		if {@code true}, attributes will be sorted alpha-numerically; if {@code false} attributes will not be sorted at
	 * 		all. This is only relevant for the right (selected attributes) side, the left side (available attributes) will
	 * 		always be sorted because it's hugely inconvenient if it isn't
	 * @since 9.2.0
	 */
	public AttributesPropertyDialog(final ParameterTypeAttributes type, Collection<String> preselectedItems, boolean sortAttributes) {
		super(type, "attributes");
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		List<Pair<String, Integer>> attributeNamesAndTypes = type.getAttributeNamesAndTypes(false);
		items = attributeNamesAndTypes.stream().map(Pair::getFirst).filter(att -> !preselectedItems.contains(att)).collect(Collectors.toList());
		valueTypeMap = attributeNamesAndTypes.stream().collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
		selectedItems = new ArrayList<>(preselectedItems);
		if (sortAttributes) {
			items.sort(FilterableListModel.STRING_COMPARATOR);
			selectedItems.sort(FilterableListModel.STRING_COMPARATOR);
		}

		itemListModel = new FilterableListModel<>(items, false);
		selectedItemListModel = new FilterableListModel<>(selectedItems, false);
		if (sortAttributes) {
			itemListModel.setComparator(FilterableListModel.STRING_COMPARATOR);
			selectedItemListModel.setComparator(FilterableListModel.STRING_COMPARATOR);
		}

		itemSearchField = new FilterTextField();
		itemSearchField.addFilterListener(itemListModel);
		JButton itemSearchFieldClearButton = new JButton(new ResourceAction(true, "attributes.clear") {

			private static final long serialVersionUID = -3046621278306353077L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				itemSearchField.clearFilter();
				itemSearchField.requestFocusInWindow();
			}
		});
		JPanel itemSearchFieldPanel = new JPanel(new GridBagLayout());
		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;
		itemSearchFieldPanel.add(itemSearchField, c);

		c.gridx = 1;
		c.weightx = 0;
		itemSearchFieldPanel.add(itemSearchFieldClearButton, c);

		itemList = new JList<>(itemListModel);
		itemList.setCellRenderer(createAttributeTypeListRenderer());
		itemList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					selectAttributesAction.actionPerformed(null);
				}
			}
		});
		JScrollPane itemListPane = new ExtendedJScrollPane(itemList);
		itemListPane.setBorder(createBorder());
		JPanel itemListPanel = new JPanel(new GridBagLayout());

		c.insets = new Insets(4, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;
		itemListPanel.add(itemSearchFieldPanel, c);

		c.gridy = 1;
		c.weighty = 1;
		itemListPanel.add(itemListPane, c);
		itemListPanel.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".attributes.border")));

		selectedItemSearchField = new FilterTextField();
		selectedItemSearchField.addFilterListener(selectedItemListModel);
		JButton selectedItemSearchFieldClearButton = new JButton(new ResourceAction(true, "attributes.clear") {

			private static final long serialVersionUID = -3046621278306353032L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				selectedItemSearchField.clearFilter();
				selectedItemSearchField.requestFocusInWindow();
			}
		});
		JPanel selectedItemSearchFieldPanel = new JPanel(new GridBagLayout());
		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;
		selectedItemSearchFieldPanel.add(selectedItemSearchField, c);

		JButton addValueButton = new JButton(new ResourceAction(true, "attributes.add") {

			private static final long serialVersionUID = 41667438431831572L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				String item = selectedItemSearchField.getText();
				selectedItemSearchField.clearFilter();
				selectedItemSearchField.requestFocusInWindow();
				if (selectedItems.contains(item)) {
					return;
				}
				if (items.contains(item)) {
					selectedItemListModel.addElement(item);
					itemListModel.removeElement(item);
					selectedItems.add(item);
					items.remove(item);
					return;
				}
				selectedItems.add(item);
				selectedItemListModel.addElement(item);
			}
		});

		c.gridx = 1;
		c.weightx = 0;
		selectedItemSearchFieldPanel.add(addValueButton, c);

		c.gridx = 2;
		c.weightx = 0;
		selectedItemSearchFieldPanel.add(selectedItemSearchFieldClearButton, c);

		selectedItemList = new JList<>(selectedItemListModel);
		selectedItemList.setCellRenderer(createAttributeTypeListRenderer());
		selectedItemList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					deselectAttributesAction.actionPerformed(null);
				}
			}
		});
		JScrollPane selectedItemListPane = new ExtendedJScrollPane(selectedItemList);
		selectedItemListPane.setBorder(createBorder());
		JPanel selectedItemListPanel = new JPanel(new GridBagLayout());

		c.insets = new Insets(4, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.BOTH;
		selectedItemListPanel.add(selectedItemSearchFieldPanel, c);

		c.gridy = 1;
		c.weighty = 1;
		selectedItemListPanel.add(selectedItemListPane, c);
		selectedItemListPanel.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), getKey()
				+ ".selected_attributes.border")));

		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.5;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		panel.add(itemListPanel, c);

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

		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 0.5;
		c.fill = GridBagConstraints.BOTH;
		panel.add(selectedItemListPanel, c);
		Dimension d = panel.getPreferredSize();
		d.setSize(d.getWidth() / 2, d.getHeight());
		itemListPanel.setPreferredSize(d);
		selectedItemListPanel.setPreferredSize(d);

		layoutDefault(panel, NORMAL, makeOkButton("attributes_property_dialog_apply"), makeCancelButton());
	}

	public Collection<String> getSelectedAttributeNames() {
		return selectedItems;
	}

	/**
	 * Create a list cell renderer for lists that show attributes and their value types.
	 *
	 * @return the renderer, never {@code null}
	 */
	private DefaultListCellRenderer createAttributeTypeListRenderer() {
		return new DefaultListCellRenderer() {

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				String stringValue = (String) value;
				Integer type = valueTypeMap.get(stringValue);
				if (type != null) {
					Icon icon;
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.NUMERICAL)) {
						icon = AttributeGuiTools.NUMERICAL_COLUMN_ICON;
					} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.NOMINAL)) {
						icon = AttributeGuiTools.NOMINAL_COLUMN_ICON;
					} else {
						icon = AttributeGuiTools.DATE_COLUMN_ICON;
					}
					label.setIcon(icon);
				}
				return label;
			}
		};
	}
}
