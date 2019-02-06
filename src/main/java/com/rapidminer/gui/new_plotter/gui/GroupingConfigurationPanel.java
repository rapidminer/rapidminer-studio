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
package com.rapidminer.gui.new_plotter.gui;

import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping.GroupingType;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.gui.groupingpanel.DistinctValueGroupingCardPanel;
import com.rapidminer.gui.new_plotter.gui.groupingpanel.EqualDataFractionCardPanel;
import com.rapidminer.gui.new_plotter.gui.groupingpanel.EquidistantFixedBinCountCardPanel;
import com.rapidminer.gui.new_plotter.gui.treenodes.DimensionConfigTreeNode;
import com.rapidminer.gui.new_plotter.listener.PlotInstanceChangedListener;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;

import java.awt.CardLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;


/**
 * @author Nils Woehler, Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class GroupingConfigurationPanel extends AbstractConfigurationPanel implements TreeSelectionListener {

	private static final long serialVersionUID = 1L;

	private Map<GroupingType, JPanel> groupingTypeToCardMap = new HashMap<GroupingType, JPanel>();

	private final PlotDimension dimension;

	public GroupingConfigurationPanel(JTree plotConfigurationTree, PlotInstance plotIntance, PlotDimension dimension) {
		super(plotIntance);
		this.setLayout(new CardLayout());

		this.dimension = dimension;
		createComponents(dimension);
		registerAsPlotConfigurationListener();
		plotConfigurationTree.addTreeSelectionListener(this);

		adaptGUI();

	}

	private void createComponents(PlotDimension dimension) {

		// add panel with panels for every grouping configuration

		for (GroupingType groupingType : GroupingType.values()) {
			JPanel groupingCardPanel = null;
			switch (groupingType) {
				case EQUAL_DATA_FRACTION:
					groupingCardPanel = new EqualDataFractionCardPanel(getCurrentPlotInstance(), dimension);
					addPlotInstanceChangeListener((PlotInstanceChangedListener) groupingCardPanel);
					break;
				case EQUIDISTANT_FIXED_BIN_COUNT:
					groupingCardPanel = new EquidistantFixedBinCountCardPanel(getCurrentPlotInstance(), dimension);
					addPlotInstanceChangeListener((PlotInstanceChangedListener) groupingCardPanel);
					break;
				case NONE:
					groupingCardPanel = new JPanel();
					break;
				case DISTINCT_VALUES:
					groupingCardPanel = new DistinctValueGroupingCardPanel(getCurrentPlotInstance(), dimension);
					addPlotInstanceChangeListener((PlotInstanceChangedListener) groupingCardPanel);
					break;
				default:
					throw new RuntimeException("Unknown grouping type " + groupingType);
			}
			this.add(groupingCardPanel, groupingType.toString());
			groupingTypeToCardMap.put(groupingType, groupingCardPanel);
		}

		CardLayout cl = (CardLayout) this.getLayout();
		cl.show(this, ((GroupingType.NONE).toString()));

	}

	@Override
	protected void adaptGUI() {
		DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(dimension);
		if (dimensionConfig != null) {
			CardLayout cl = (CardLayout) this.getLayout();
			GroupingType groupingType = null;

			ValueGrouping grouping = dimensionConfig.getGrouping();
			if (grouping != null) {
				groupingType = grouping.getGroupingType();
			} else {
				groupingType = GroupingType.NONE;
			}
			cl.show(this, groupingType.toString());
		}
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		TreePath newLeadSelectionPath = e.getNewLeadSelectionPath();
		if (newLeadSelectionPath != null) {
			if (newLeadSelectionPath.getLastPathComponent() instanceof DimensionConfigTreeNode) {
				adaptGUI();
			}
		}
	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		adaptGUI();
		return true;
	}
}
