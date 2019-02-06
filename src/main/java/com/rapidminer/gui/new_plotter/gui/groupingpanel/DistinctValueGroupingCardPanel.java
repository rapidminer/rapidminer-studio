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

import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.DistinctValueGrouping;
import com.rapidminer.gui.new_plotter.data.PlotInstance;

import java.awt.GridBagConstraints;

import javax.swing.JPanel;


/**
 * A configuration panel for {@link DistinctValueGrouping}s.
 * 
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class DistinctValueGroupingCardPanel extends AbstractGroupingCardPanel {

	private static final long serialVersionUID = 1L;

	public DistinctValueGroupingCardPanel(PlotInstance plotInstance, PlotDimension dimension) {
		super(plotInstance, dimension);
	}

	@Override
	protected void createComponents() {
		super.createComponents();

		final JPanel spacerPanel = new JPanel();
		final GridBagConstraints itemConstraint = new GridBagConstraints();
		itemConstraint.fill = GridBagConstraints.BOTH;
		itemConstraint.weightx = 1;
		itemConstraint.weighty = 1;
		itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
		this.add(spacerPanel, itemConstraint);
	}
}
