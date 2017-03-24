/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.parameter.ParameterTypeAttributes;
import com.rapidminer.tools.I18N;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


/**
 * A dialog to select a subset of attributes.
 * 
 * @author Tobias Malbrecht
 */
public class AttributesPropertyDialog extends PropertyDialog {

	private static final long serialVersionUID = 5396725165122306231L;

	private final ArrayList<String> items;

	private final ArrayList<String> selectedItems;

	private final FilterTextField itemSearchField;

	private final FilterTextField selectedItemSearchField;

	private final FilterableListModel<String> itemListModel;

	private final FilterableListModel<String> selectedItemListModel;

	private final JList<String> itemList;

	private final JList<String> selectedItemList;

	private final Action selectAttributesAction = new ResourceAction(true, "attributes_select") {

		private static final long serialVersionUID = -3046621278306353077L;

		@Override
		public void actionPerformed(ActionEvent e) {
			int[] indices = itemList.getSelectedIndices();
			itemList.setSelectedIndices(new int[] {});
			for (int i = indices.length - 1; i >= 0; i--) {
				String item = itemListModel.getElementAt(indices[i]).toString();
				selectedItemListModel.addElement(item);
				itemListModel.removeElementAt(indices[i]);
				selectedItems.add(item);
				items.remove(item);
			}
		}
	};

	private final Action deselectAttributesAction = new ResourceAction(true, "attributes_deselect") {

		private static final long serialVersionUID = -3046621278306353077L;

		@Override
		public void actionPerformed(ActionEvent e) {
			int[] indices = selectedItemList.getSelectedIndices();
			selectedItemList.setSelectedIndices(new int[] {});
			for (int i = indices.length - 1; i >= 0; i--) {
				String item = selectedItemListModel.getElementAt(indices[i]).toString();
				itemListModel.addElement(item);
				selectedItemListModel.removeElementAt(indices[i]);
				items.add(item);
				selectedItems.remove(item);
			}
		}
	};

	public AttributesPropertyDialog(final ParameterTypeAttributes type, Collection<String> preselectedItems) {
		super(type, "attributes");
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		items = new ArrayList<>();
		selectedItems = new ArrayList<>();

		itemListModel = new FilterableListModel<>();
		selectedItemListModel = new FilterableListModel<>();
		for (String item : type.getAttributeNames()) {
			if (!preselectedItems.contains(item)) {
				items.add(item);
				itemListModel.addElement(item);
			} else {
				selectedItems.add(item);
				selectedItemListModel.addElement(item);
			}
		}
		for (String item : preselectedItems) {
			if (!type.getAttributeNames().contains(item)) {
				selectedItems.add(item);
				selectedItemListModel.addElement(item);
			}
		}

		itemSearchField = new FilterTextField();
		itemSearchField.addFilterListener(itemListModel);
		JButton itemSearchFieldClearButton = new JButton(new ResourceAction(true, "attributes.clear") {

			private static final long serialVersionUID = -3046621278306353077L;

			@Override
			public void actionPerformed(ActionEvent e) {
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
		itemList.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					selectAttributesAction.actionPerformed(null);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}
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
			public void actionPerformed(ActionEvent e) {
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
			public void actionPerformed(ActionEvent e) {
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
		selectedItemList.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					deselectAttributesAction.actionPerformed(null);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {}
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
}
