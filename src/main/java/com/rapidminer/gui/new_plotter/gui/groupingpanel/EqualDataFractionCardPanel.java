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
import com.rapidminer.gui.new_plotter.configuration.EqualDataFractionGrouping;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping.GroupingType;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.tools.ResourceLabel;

import java.awt.GridBagConstraints;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * A configuration Panel for {@link EqualDataFractionGrouping}s.
 * 
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class EqualDataFractionCardPanel extends AbstractGroupingCardPanel {

	private static final long serialVersionUID = 1L;

	protected JSpinner binCountSpinner;

	public EqualDataFractionCardPanel(final PlotInstance plotInstance, final PlotDimension dimension) {
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
			final EqualDataFractionGrouping grouping = (EqualDataFractionGrouping) dimensionConfig.getGrouping();
			if (grouping != null) {
				final int oldBinCount = grouping.getBinCount();
				final int newBinCount = (Integer) binCountSpinner.getValue();
				if (oldBinCount != newBinCount) {
					grouping.setBinCount(newBinCount);
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
			if (grouping != null && grouping.getGroupingType() == GroupingType.EQUAL_DATA_FRACTION) {
				final EqualDataFractionGrouping equalDataFractionGrouping = (EqualDataFractionGrouping) grouping;
				binCountSpinner.setValue(equalDataFractionGrouping.getBinCount());
			}
		}
	}

}
