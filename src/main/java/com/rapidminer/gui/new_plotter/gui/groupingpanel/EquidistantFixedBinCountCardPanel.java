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
package com.rapidminer.gui.new_plotter.gui.groupingpanel;

import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.EquidistantFixedBinCountBinning;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping.GroupingType;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.utility.DataStructureUtils;
import com.rapidminer.gui.tools.ResourceLabel;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * A configuration Panel for {@link EquidistantFixedBinCountBinning}s.
 * 
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class EquidistantFixedBinCountCardPanel extends AbstractGroupingCardPanel {

	private static final long serialVersionUID = 1L;

	private JCheckBox autoRangeCheckBox;

	private JLabel upperBoundLabel;
	private JSpinner upperBoundSpinner;

	private JLabel lowerBoundLabel;
	private JSpinner lowerBoundSpinner;

	private JSpinner binCountSpinner;

	private boolean changingAutoRange;

	public EquidistantFixedBinCountCardPanel(final PlotInstance plotInstance, final PlotDimension dimension) {
		super(plotInstance, dimension);
		adaptGUI();
	}

	@Override
	protected void createComponents() {
		super.createComponents();
		{
			final JLabel binCountLabel = new ResourceLabel("plotter.configuration_dialog.bin_count");

			// create input text field
			binCountSpinner = new JSpinner(new SpinnerNumberModel(5, 1, null, 1));
			binCountLabel.setLabelFor(binCountSpinner);
			binCountSpinner.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(final ChangeEvent e) {
					binCountChanged();
				}
			});

			addTwoComponentRow(this, binCountLabel, binCountSpinner);

		}

		// add auto range selection
		{
			final JLabel autoRangeLabel = new ResourceLabel("plotter.configuration_dialog.auto_range");

			autoRangeCheckBox = new JCheckBox();
			autoRangeLabel.setLabelFor(autoRangeCheckBox);
			autoRangeCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {
					autoRangeChanged();
				}
			});

			addTwoComponentRow(this, autoRangeLabel, autoRangeCheckBox);
		}

		// add upper bound spinner
		{
			upperBoundLabel = new ResourceLabel("plotter.configuration_dialog.upper_bound");

			upperBoundSpinner = new JSpinner(new SpinnerNumberModel(10.1, null, null, 0.1));
			upperBoundLabel.setLabelFor(upperBoundSpinner);
			upperBoundSpinner.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(final ChangeEvent e) {
					upperBoundChanged();
				}
			});

			addTwoComponentRow(this, upperBoundLabel, upperBoundSpinner);
		}

		// add lower bound spinner
		{

			lowerBoundLabel = new ResourceLabel("plotter.configuration_dialog.lower_bound");

			lowerBoundSpinner = new JSpinner(new SpinnerNumberModel(0.1, null, null, 0.1));
			lowerBoundLabel.setLabelFor(lowerBoundSpinner);
			lowerBoundSpinner.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(final ChangeEvent e) {
					lowerBoundChanged();
				}
			});

			addTwoComponentRow(this, lowerBoundLabel, lowerBoundSpinner);
		}

		final JPanel spacerPanel = new JPanel();
		final GridBagConstraints itemConstraint = new GridBagConstraints();
		itemConstraint.fill = GridBagConstraints.BOTH;
		itemConstraint.weightx = 1;
		itemConstraint.weighty = 1;
		itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
		this.add(spacerPanel, itemConstraint);
	}

	private void binCountChanged() {
		final DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(getDimension());
		if (dimensionConfig != null) {
			final EquidistantFixedBinCountBinning grouping = (EquidistantFixedBinCountBinning) dimensionConfig.getGrouping();
			if (grouping != null) {
				final int oldBinCount = grouping.getBinCount();
				final int newBinCount = (Integer) binCountSpinner.getValue();
				if (oldBinCount != newBinCount) {
					grouping.setBinCount(newBinCount);
				}
			}
		}
	}

	protected void upperBoundChanged() {
		final DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(getDimension());
		if (dimensionConfig != null) {
			final EquidistantFixedBinCountBinning grouping = (EquidistantFixedBinCountBinning) dimensionConfig.getGrouping();
			if (grouping != null) {
				final double oldUpperBound = grouping.getMaxValue();
				final double newUpperBound = (Double) upperBoundSpinner.getValue();
				if (oldUpperBound != newUpperBound) {
					final double currentLowerBound = (Double) lowerBoundSpinner.getValue();
					if (DataStructureUtils.almostEqual(currentLowerBound, newUpperBound, 1E-6)
							|| newUpperBound <= currentLowerBound) {
						upperBoundSpinner.setValue(oldUpperBound);
						return;
					}
					if (!changingAutoRange) {
						grouping.setMaxValue(newUpperBound);
					}
				}
			}
		}
	}

	protected void lowerBoundChanged() {
		final DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(getDimension());
		if (dimensionConfig != null) {
			final EquidistantFixedBinCountBinning grouping = (EquidistantFixedBinCountBinning) dimensionConfig.getGrouping();
			if (grouping != null) {
				final double oldLowerBound = grouping.getMinValue();
				final double newLowerBound = (Double) lowerBoundSpinner.getValue();
				if (oldLowerBound != newLowerBound) {
					final double currentUpperBound = (Double) upperBoundSpinner.getValue();
					if (DataStructureUtils.almostEqual(currentUpperBound, newLowerBound, 1E-6)
							|| currentUpperBound <= newLowerBound) {
						lowerBoundSpinner.setValue(oldLowerBound);
						return;
					}
					if (!changingAutoRange) {
						grouping.setMinValue(newLowerBound);
					}
				}

			}
		}
	}

	private void autoRangeChanged() {
		final DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(getDimension());
		if (dimensionConfig != null) {
			final EquidistantFixedBinCountBinning grouping = (EquidistantFixedBinCountBinning) dimensionConfig.getGrouping();
			if (grouping != null) {
				final boolean selected = autoRangeCheckBox.isSelected();
				final boolean autoRanging = grouping.isAutoRanging();
				if (selected != autoRanging) {
					changingAutoRange = true;
					grouping.setAutoRange(selected);
				}
			}
		}

	}

	@Override
	protected void adaptGUI() {
		super.adaptGUI();

		final DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(getDimension());
		if (dimensionConfig != null) {
			final ValueGrouping grouping = dimensionConfig.getGrouping();
			if (grouping != null && grouping.getGroupingType() == GroupingType.EQUIDISTANT_FIXED_BIN_COUNT) {
				final EquidistantFixedBinCountBinning equidistantFixedBinCountBinning = (EquidistantFixedBinCountBinning) grouping;
				binCountSpinner.setValue(equidistantFixedBinCountBinning.getBinCount());

				upperBoundSpinner.setValue(equidistantFixedBinCountBinning.getMaxValue());
				lowerBoundSpinner.setValue(equidistantFixedBinCountBinning.getMinValue());

				final boolean autoRanging = equidistantFixedBinCountBinning.isAutoRanging();
				autoRangeCheckBox.setSelected(autoRanging);
				lowerBoundLabel.setEnabled(!autoRanging);
				lowerBoundSpinner.setEnabled(!autoRanging);
				upperBoundLabel.setEnabled(!autoRanging);
				upperBoundSpinner.setEnabled(!autoRanging);
				if (changingAutoRange) {
					changingAutoRange = false;
				}
			}
		}
	}
}
