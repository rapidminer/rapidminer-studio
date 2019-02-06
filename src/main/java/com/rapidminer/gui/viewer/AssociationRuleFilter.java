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
package com.rapidminer.gui.viewer;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.ExtendedJList;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedListModel;
import com.rapidminer.operator.learner.associations.AssociationRule;
import com.rapidminer.operator.learner.associations.AssociationRuleGenerator;
import com.rapidminer.operator.learner.associations.AssociationRules;
import com.rapidminer.operator.learner.associations.Item;


/**
 * This is a gui component which can be used to define filter conditions for association rules.
 *
 * @author Ingo Mierswa
 */
public class AssociationRuleFilter extends JPanel {

	private static final long serialVersionUID = 5619543957729778883L;

	private static final int MAX_VALUE = 10000;

	private JComboBox<String> criterionSelectorBox = new JComboBox<>(AssociationRuleGenerator.CRITERIA);

	private JSlider criterionMinSlider = new JSlider(SwingConstants.HORIZONTAL, 0, MAX_VALUE, MAX_VALUE / 10);

	private double[] minValues;

	private double[] maxValues;

	private JList<Item> conclusionList = null;

	private JComboBox<String> conjunctionBox = new JComboBox<>(AssociationRuleFilterListener.CONJUNCTION_NAMES);

	private Item[] itemArray;

	private List<AssociationRuleFilterListener> listeners = new LinkedList<AssociationRuleFilterListener>();

	private AssociationRules rules;

	public AssociationRuleFilter(AssociationRules rules) {
		this.rules = rules;
		this.itemArray = rules.getAllConclusionItems();

		setOpaque(true);
		setBackground(Colors.WHITE);

		// init min and max values
		this.minValues = new double[AssociationRuleGenerator.CRITERIA.length];
		this.maxValues = new double[AssociationRuleGenerator.CRITERIA.length];
		for (int i = 0; i < minValues.length; i++) {
			minValues[i] = Double.POSITIVE_INFINITY;
			maxValues[i] = Double.NEGATIVE_INFINITY;
		}
		for (AssociationRule rule : rules) {
			if (!Double.isInfinite(rule.getConfidence())) {
				this.minValues[AssociationRuleGenerator.CONFIDENCE] = Math.min(
						this.minValues[AssociationRuleGenerator.CONFIDENCE], rule.getConfidence());
				this.maxValues[AssociationRuleGenerator.CONFIDENCE] = Math.max(
						this.maxValues[AssociationRuleGenerator.CONFIDENCE], rule.getConfidence());
			}

			if (!Double.isInfinite(rule.getConviction())) {
				this.minValues[AssociationRuleGenerator.CONVICTION] = Math.min(
						this.minValues[AssociationRuleGenerator.CONVICTION], rule.getConviction());
				this.maxValues[AssociationRuleGenerator.CONVICTION] = Math.max(
						this.maxValues[AssociationRuleGenerator.CONVICTION], rule.getConviction());
			}

			if (!Double.isInfinite(rule.getGain())) {
				this.minValues[AssociationRuleGenerator.GAIN] = Math.min(this.minValues[AssociationRuleGenerator.GAIN],
						rule.getGain());
				this.maxValues[AssociationRuleGenerator.GAIN] = Math.max(this.maxValues[AssociationRuleGenerator.GAIN],
						rule.getGain());
			}

			if (!Double.isInfinite(rule.getLaplace())) {
				this.minValues[AssociationRuleGenerator.LAPLACE] = Math.min(
						this.minValues[AssociationRuleGenerator.LAPLACE], rule.getLaplace());
				this.maxValues[AssociationRuleGenerator.LAPLACE] = Math.max(
						this.maxValues[AssociationRuleGenerator.LAPLACE], rule.getLaplace());
			}

			if (!Double.isInfinite(rule.getLift())) {
				this.minValues[AssociationRuleGenerator.LIFT] = Math.min(this.minValues[AssociationRuleGenerator.LIFT],
						rule.getLift());
				this.maxValues[AssociationRuleGenerator.LIFT] = Math.max(this.maxValues[AssociationRuleGenerator.LIFT],
						rule.getLift());
			}

			if (!Double.isInfinite(rule.getPs())) {
				this.minValues[AssociationRuleGenerator.PS] = Math.min(this.minValues[AssociationRuleGenerator.PS],
						rule.getPs());
				this.maxValues[AssociationRuleGenerator.PS] = Math.max(this.maxValues[AssociationRuleGenerator.PS],
						rule.getPs());
			}
		}

		// layout
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(4, 4, 4, 4);

		// conjunction mode
		conjunctionBox.setPreferredSize(new Dimension(200, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
		conjunctionBox.putClientProperty(RapidLookTools.PROPERTY_INPUT_BACKGROUND_DARK, true);
		conjunctionBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				adjustFilter();
			}
		});

		JLabel label = new JLabel("Show rules matching");
		layout.setConstraints(label, c);
		add(label);

		layout.setConstraints(conjunctionBox, c);
		add(conjunctionBox);

		// conclusion list
		ExtendedListModel<Item> model = new ExtendedListModel<>();
		for (Item item : itemArray) {
			model.addElement(item, "The item '" + item.toString() + "'.");
		}
		this.conclusionList = new ExtendedJList<>(model, 200);
		this.conclusionList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		conclusionList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					adjustFilter();
				}
			}
		});

		ExtendedJScrollPane listPane = new ExtendedJScrollPane(conclusionList);
		listPane.setBorder(null);
		listPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		c.weighty = 1;
		c.weightx = 0;
		layout.setConstraints(listPane, c);
		add(listPane);

		c.weighty = 0;
		c.weightx = 1;
		label = new JLabel("Min. Criterion:");
		layout.setConstraints(label, c);
		add(label);

		criterionSelectorBox.setPreferredSize(new Dimension(criterionSelectorBox.getPreferredSize().width,
				PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
		criterionSelectorBox.putClientProperty(RapidLookTools.PROPERTY_INPUT_BACKGROUND_DARK, true);
		criterionSelectorBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				adjustFilter();
			}
		});
		layout.setConstraints(criterionSelectorBox, c);
		add(criterionSelectorBox);

		label = new JLabel("Min. Criterion Value:");
		layout.setConstraints(label, c);
		add(label);

		criterionMinSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if (!criterionMinSlider.getValueIsAdjusting()) {
					adjustFilter();
				}
			}
		});
		layout.setConstraints(criterionMinSlider, c);
		add(criterionMinSlider);
	}

	/**
	 * Triggers the filtering.
	 */
	public void triggerFilter() {
		adjustFilter();
	}

	private void adjustFilter() {
		int conjunctionMode = conjunctionBox.getSelectedIndex();
		Item[] searchFilter = null;
		int[] selectedIndices = conclusionList.getSelectedIndices();
		if (selectedIndices.length > 0 && selectedIndices.length <= itemArray.length) {
			searchFilter = new Item[selectedIndices.length];
			int counter = 0;
			for (int s : selectedIndices) {
				searchFilter[counter++] = itemArray[s];
			}
		}

		double minRatio = criterionMinSlider.getValue() / (double) MAX_VALUE;
		fireFilteringEvent(searchFilter, conjunctionMode, minRatio);
	}

	public void addAssociationRuleFilterListener(AssociationRuleFilterListener listener) {
		this.listeners.add(listener);
	}

	public void removeAssociationRuleFilterListener(AssociationRuleFilterListener listener) {
		this.listeners.remove(listener);
	}

	private void fireFilteringEvent(Item[] searchFilter, int conjunctionMode, double minRatio) {
		boolean[] filter = getFilter(rules, searchFilter, conjunctionMode, minRatio);
		for (AssociationRuleFilterListener listener : listeners) {
			listener.setFilter(filter);
		}
	}

	private boolean[] getFilter(AssociationRules rules, Item[] filter, int conjunctionMode, double minRatio) {
		boolean[] mapping = new boolean[rules.getNumberOfRules()];
		int counter = 0;
		for (AssociationRule rule : rules) {
			if (getCriterionValue(rule) >= getCriterionMinValue(minRatio)) {
				if (checkForItem(filter, rule, conjunctionMode)) {
					mapping[counter] = true;
				} else {
					mapping[counter] = false;
				}
			} else {
				mapping[counter] = false;
			}
			counter++;
		}

		return mapping;
	}

	private double getCriterionMinValue(double minRatio) {
		int criterionSelection = criterionSelectorBox.getSelectedIndex();
		return minValues[criterionSelection] + (maxValues[criterionSelection] - minValues[criterionSelection]) * minRatio;
	}

	private double getCriterionValue(AssociationRule rule) {
		int criterionSelection = criterionSelectorBox.getSelectedIndex();
		switch (criterionSelection) {
			case AssociationRuleGenerator.LIFT:
				return rule.getLift();
			case AssociationRuleGenerator.CONVICTION:
				return rule.getConviction();
			case AssociationRuleGenerator.PS:
				return rule.getPs();
			case AssociationRuleGenerator.GAIN:
				return rule.getGain();
			case AssociationRuleGenerator.LAPLACE:
				return rule.getLaplace();
			case AssociationRuleGenerator.CONFIDENCE:
			default:
				return rule.getConfidence();
		}
	}

	private boolean checkForItem(Item[] filter, AssociationRule rule, int conjunctionMode) {
		if (filter == null) {
			return true;
		}
		switch (conjunctionMode) {
			case AssociationRuleFilterListener.CONJUNCTION_ANY:
				List<Item> filterList = Arrays.asList(filter);
				Iterator<Item> c = rule.getConclusionItems();
				while (c.hasNext()) {
					if (filterList.contains(c.next())) {
						return true;
					}
				}
				return false;
			case AssociationRuleFilterListener.CONJUNCTION_ALL:
				for (Item item : filter) {
					c = rule.getConclusionItems();
					boolean found = false;
					while (c.hasNext()) {
						if (c.next().equals(item)) {
							found = true;
							break;
						}
					}
					if (!found) {
						return false;
					}
				}
				return true;
			default:
				throw new RuntimeException("Illegal filter type index: " + conjunctionMode);
		}
	}
}
