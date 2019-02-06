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
package com.rapidminer.gui.new_plotter.gui.treenodes;

import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;


/**
 * Never use the setUserObject method of this class! Instead use the exchangePlotConfiguration in
 * PlotConfigurationTreeModel class.
 *
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotConfigurationTreeNode extends DefaultMutableTreeNode {

	private static final long serialVersionUID = 1L;

	public PlotConfigurationTreeNode(PlotConfiguration plotConfig) {
		super(plotConfig);
	}

	public int getRangeAxisIndex(RangeAxisConfig rangeAxis) {
		for (Object child : children) {
			if (rangeAxis == ((DefaultMutableTreeNode) child).getUserObject()) {
				return children.indexOf(child);
			}
		}
		return -1;
	}

	public int getDimensionConfigIndex(PlotDimension dimension) {
		for (Object child : children) {
			if (child instanceof DimensionConfigTreeNode) {
				if (((DimensionConfigTreeNode) child).getDimension() == dimension) {
					return children.indexOf(child);
				}
			}

		}
		return -1;
	}

	public TreeNode getChild(RangeAxisConfig rangeAxis) {
		int rangeAxisIndex = getRangeAxisIndex(rangeAxis);
		if (rangeAxisIndex < 0) {
			return null;
		}
		return getChildAt(rangeAxisIndex);
	}

	public TreeNode getChild(PlotDimension dimension) {
		int dimensionConfigIndex = getDimensionConfigIndex(dimension);
		if (dimensionConfigIndex < 0) {
			return null;
		}
		return getChildAt(dimensionConfigIndex);
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public PlotConfiguration getUserObject() {
		return (PlotConfiguration) super.getUserObject();
	}
}
