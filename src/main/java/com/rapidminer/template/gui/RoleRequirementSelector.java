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
package com.rapidminer.template.gui;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.template.RoleAssignment;
import com.rapidminer.template.RoleRequirement;
import com.rapidminer.template.TemplateController;
import com.rapidminer.template.TemplateState;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.SortedSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;


/**
 * Used to specify an attribute for a {@link RoleRequirement}. Shows the
 * {@link RoleRequirement#getDescription()} and a small plot visualizing the distribution of the
 * currently selected attribute.
 * 
 * @author Simon Fischer
 * 
 */
public class RoleRequirementSelector extends JPanel {

	private static final long serialVersionUID = 1L;

	private TemplateController controller;

	private RoleRequirement requirement;

	// UI Components

	/** Label for displaying the help text. */
	private JLabel helpLabel = new JLabel("");

	/** Combo box to select attribute from. */
	private JComboBox<String> attributeCombo = new JComboBox<>();

	/** Combo box to select positive value in case of binomial label. */
	private JComboBox<String> positiveClassCombo = new JComboBox<>();

	/**
	 * Caches positive classes for {@link RoleAssignment}s so we restore the original state after
	 * switching.
	 */
	private Map<String, String> positiveClassesCache = new HashMap<>();

	private ResourceLabel columnLabel = new ResourceLabel("template.column_label", SwingConstants.RIGHT) {

		private static final long serialVersionUID = 1L;

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(110, (int) super.getPreferredSize().getHeight());
		};
	};
	private ResourceLabel positiveClassLabel = new ResourceLabel("template.positive_class_label", SwingConstants.RIGHT) {

		private static final long serialVersionUID = 1L;

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(110, (int) super.getPreferredSize().getHeight());
		};

	};

	private ChartPanel chartPanel;

	private boolean areValuesAdjusting = false;
	private RoundTitledBorder border = new RoundTitledBorder(3, "");

	public RoleRequirementSelector(final TemplateController controller) {
		super(new BorderLayout());
		setBorder(border);
		this.controller = controller;

		setBackground(Color.WHITE);
		updateRequirement();
		controller.getModel().addObserver(new Observer() {

			@Override
			public void update(Observable o, Object arg) {
				if (TemplateState.OBSERVER_EVENT_TEMPLATE.equals(arg)) {
					updateRequirement();
					updateComponents();
				} else if (TemplateState.OBSERVER_EVENT_ROLES.equals(arg)) {
					assignSelectionToCombo();
					updateComponents();
					attributeCombo.repaint();
				} else if (TemplateState.OBSERVER_EVENT_INPUT.equals(arg)) {
					updateAttributes();
				}
			}
		});

		AutoCompleteDecorator.decorate(attributeCombo);
		AutoCompleteDecorator.decorate(positiveClassCombo);

		attributeCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (areValuesAdjusting) {
					return;
				}
				assignRoles();
			}
		});
		positiveClassCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (areValuesAdjusting) {
					return;
				}
				assignRoles();
			}
		});

		helpLabel.setHorizontalAlignment(SwingConstants.CENTER);
		add(helpLabel, BorderLayout.PAGE_START);

		chartPanel = new ChartPanel(null, 250, 250, 100, 100, 360, 360, true, false, false, false, false, false);
		chartPanel.setMinimumDrawWidth(0);
		chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
		chartPanel.setMinimumDrawHeight(0);
		chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
		attributeCombo.setPreferredSize(new Dimension(100, 30));
		attributeCombo.setMaximumSize(new Dimension(150, 30));
		add(chartPanel, BorderLayout.CENTER);

		JComponent comboPanel = new JPanel();
		comboPanel.setLayout(new BoxLayout(comboPanel, BoxLayout.PAGE_AXIS));
		JComponent comboPanelAtt = new JPanel();
		comboPanelAtt.setBackground(Color.WHITE);
		comboPanelAtt.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		JComponent comboPanelClass = new JPanel();
		comboPanelClass.setBackground(Color.WHITE);
		comboPanelClass.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		positiveClassCombo.setPreferredSize(new Dimension(100, 30));
		positiveClassCombo.setMaximumSize(new Dimension(150, 30));
		positiveClassLabel.setLabelFor(positiveClassCombo);
		positiveClassLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
		positiveClassLabel.setToolTipText(positiveClassLabel.getText());
		positiveClassLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		columnLabel.setLabelFor(attributeCombo);
		columnLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
		columnLabel.setToolTipText(columnLabel.getText());
		columnLabel.setHorizontalAlignment(SwingConstants.RIGHT);

		comboPanelAtt.add(columnLabel);
		comboPanelAtt.add(attributeCombo);
		comboPanelClass.add(positiveClassLabel);
		comboPanelClass.add(positiveClassCombo);
		comboPanel.add(comboPanelAtt);
		comboPanel.add(comboPanelClass);

		add(comboPanel, BorderLayout.SOUTH);

		updateComponents();
	}

	private void updateRequirement() {
		if (controller.getModel().getTemplate() != null) {
			RoleRequirement newRoleRequirement = controller.getModel().getTemplate().getRoleRequirement(0);
			if (newRoleRequirement != requirement) {
				requirement = newRoleRequirement;
				positiveClassesCache.clear();
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(requirement.getValueType(), Ontology.NOMINAL)) {
					positiveClassCombo.setVisible(true);
					positiveClassLabel.setVisible(true);
				} else {
					positiveClassCombo.setVisible(false);
					positiveClassLabel.setVisible(false);
				}
			}
		}
	}

	private void updateAttributes() {
		ExampleSet input = controller.getModel().getInputData();
		if (input == null) {
			attributeCombo.removeAllItems();
			attributeCombo.setSelectedItem(null);
		} else {
			attributeCombo.removeAllItems();
			attributeCombo.setSelectedItem(null);
			SortedSet<String> sortedNames = controller.getCompatibleAttributes(requirement);
			for (String name : sortedNames) {
				attributeCombo.addItem(name);
			}
			if (attributeCombo.getItemCount() > 0) {
				attributeCombo.setSelectedIndex(0);
			} else {
				attributeCombo.setSelectedIndex(-1);
			}
			assignSelectionToCombo();

			boolean enabled = attributeCombo.getItemCount() > 0;
			attributeCombo.setEnabled(enabled);
			positiveClassCombo.setEnabled(enabled);
			positiveClassLabel.setEnabled(enabled);
		}
	}

	private JFreeChart makeChart(ExampleSet exampleSet, Attribute att) {
		DefaultPieDataset pieDataset = new DefaultPieDataset();
		if (att.isNominal()) {
			for (String val : att.getMapping().getValues()) {
				pieDataset.setValue(val, exampleSet.getStatistics(att, Statistics.COUNT, val));
			}
		}
		JFreeChart chart = ChartFactory.createPieChart(null, pieDataset, true, false, false);
		chart.setBackgroundPaint(Color.WHITE);
		chart.getLegend().setFrame(BlockBorder.NONE);
		chart.setBackgroundImageAlpha(0.0f);
		chart.setBorderVisible(false);
		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setLabelGenerator(null);
		plot.setShadowPaint(null);
		plot.setOutlineVisible(false);
		plot.setBackgroundPaint(new Color(255, 255, 255, 0));
		plot.setBackgroundImageAlpha(0.0f);
		plot.setCircular(true);
		return chart;
	}

	private void assignSelectionToCombo() {
		try {
			areValuesAdjusting = true;
			if ((requirement == null) || (controller.getModel().getInputData() == null)) {
				attributeCombo.setSelectedItem(null);
				positiveClassCombo.setSelectedItem(null);
				return;
			}
			TemplateState state = controller.getModel();
			RoleAssignment assignment = state.getRoleAssignment(requirement.getRoleName());
			if (assignment == null) {
				attributeCombo.setSelectedItem(null);
				return;
			}
			// Remember choice for assignRoles
			positiveClassesCache.put(assignment.getAttributeName(), assignment.getPositiveClass());
			Attribute att = state.getInputData().getAttributes().get(assignment.getAttributeName());
			if (att != null) {
				attributeCombo.setSelectedItem(att.getName());
				positiveClassCombo.setSelectedItem(null);
				positiveClassCombo.removeAllItems();
				if (att.isNominal()) {
					for (String value : att.getMapping().getValues()) {
						positiveClassCombo.addItem(value);
					}
				}
				positiveClassCombo.setSelectedItem(assignment.getPositiveClass());
			} else {
				attributeCombo.setSelectedItem(null);
				positiveClassCombo.setSelectedItem(null);
			}
			if (att != null && attributeCombo.getSelectedItem() != null
					&& attributeCombo.getSelectedItem().equals(att.getName())) {
				chartPanel.setChart(makeChart(state.getInputData(), att));
				chartPanel.setVisible(true);
			} else {
				chartPanel.setChart(null);
				chartPanel.setVisible(false);
			}
		} finally {
			areValuesAdjusting = false;
		}

	}

	private void updateComponents() {
		if (requirement != null) {
			border.setTitle(I18N.getMessage(I18N.getGUIBundle(), "gui.label.template.select_role_title.label",
					requirement.getHumanName()));
			if (attributeCombo.getItemCount() > 0) {
				helpLabel.setText("<html><div style=\"width: 280 px\">" + requirement.getDescription() + "</div></html>");
			} else {
				helpLabel.setText("<html><div style=\"width: 280 px\"><strong><font color=\"#FF0000\">"
						+ I18N.getMessage(I18N.getGUIBundle(), "gui.label.template.no_compatible_attribute.label",
								Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(requirement.getValueType()))
						+ "</font></strong></div></html>");
			}
		} else {
			helpLabel.setText("");
			border.setTitle("");
		}
	}

	private void assignRoles() {
		TemplateState state = controller.getModel();
		String attributeName = (String) attributeCombo.getSelectedItem();
		Attribute selectedAtt = null;
		if (attributeName != null) {
			selectedAtt = state.getInputData().getAttributes().get(attributeName);
			if (selectedAtt != null) {
				chartPanel.setChart(makeChart(state.getInputData(), selectedAtt));
			}
		}
		String positiveClass = (String) positiveClassCombo.getSelectedItem();
		if (positiveClass != null && selectedAtt != null) {
			if (!selectedAtt.getMapping().getValues().contains(positiveClass)) {
				positiveClass = null;
			}
		}
		if ((positiveClass == null) && (selectedAtt != null)) {
			String previousPositiveClass = positiveClassesCache.get(attributeName);
			if (previousPositiveClass != null) {
				controller.assignRole(requirement, attributeName, previousPositiveClass);
			} else {
				controller.assignRole(requirement, controller.makeRoleAssignment(requirement, selectedAtt));
			}
		} else {
			controller.assignRole(requirement, attributeName, positiveClass);
		}
	}
}
