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
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.gui.treenodes.DimensionConfigTreeNode;
import com.rapidminer.gui.new_plotter.gui.treenodes.PlotConfigurationTreeNode;
import com.rapidminer.gui.new_plotter.gui.treenodes.RangeAxisConfigTreeNode;
import com.rapidminer.gui.new_plotter.gui.treenodes.ValueSourceTreeNode;
import com.rapidminer.gui.new_plotter.listener.PlotConfigurationListener;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent.DimensionConfigChangeType;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent.PlotConfigurationChangeType;
import com.rapidminer.gui.new_plotter.listener.events.RangeAxisConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.RangeAxisConfigChangeEvent.RangeAxisConfigChangeType;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotConfigurationTreeModel extends DefaultTreeModel implements PlotConfigurationListener {

	private static final long serialVersionUID = 1L;
	private final PlotConfigurationTree plotConfigTree;
	private PlotConfiguration plotConfig;

	public static int NUMBER_OF_PERMANENT_DIMENSIONS = 4;

	/**
	 * @param root
	 */
	public PlotConfigurationTreeModel(DefaultMutableTreeNode root, PlotConfiguration plotConfig,
			PlotConfigurationTree plotConfigTree) {
		super(root);
		this.plotConfig = plotConfig;
		this.plotConfigTree = plotConfigTree;

		if (root != null) {
			fillNewPlotConfigNode(plotConfig);
		}

		if (plotConfig != null) {
			plotConfig.addPlotConfigurationListener(this);
		}

	}

	private void rangeAxisConfigAdded(int index, RangeAxisConfig rangeAxis) {
		RangeAxisConfigTreeNode newChild = new RangeAxisConfigTreeNode(rangeAxis);
		insertNodeInto(newChild, (MutableTreeNode) root, index + NUMBER_OF_PERMANENT_DIMENSIONS);

		List<ValueSource> rangeAxisValueSources = rangeAxis.getValueSources();
		if (rangeAxis.getValueSources().size() > 0) {
			int idx = 0;
			// add new value source child nodes
			for (ValueSource source : rangeAxisValueSources) {
				valueSourceAdded(idx, source, rangeAxis);
				++idx;
			}
		} else {

			// change selection path
			TreePath pathToNewChild = new TreePath(getPathToRoot(newChild));
			makeVisibleAndSelect(pathToNewChild);

		}
	}

	private void rangeAxisConfigRemoved(int index, RangeAxisConfig rangeAxis) {
		removeNodeFromParent((MutableTreeNode) root.getChildAt(index + NUMBER_OF_PERMANENT_DIMENSIONS));

		reload();
		plotConfigTree.expandAll();

		// Acquire new selection element
		int childCount = root.getChildCount();
		Object newSelection = null;

		if (childCount > NUMBER_OF_PERMANENT_DIMENSIONS) {
			newSelection = root.getChildAt(childCount - 1);
		} else {
			newSelection = root;
		}

		// change selection path
		TreePath path = new TreePath(getPathToRoot((TreeNode) newSelection));
		makeVisibleAndSelect(path);
	}

	private void dimensionConfigAdded(PlotDimension dimension, DimensionConfig dimensionConfig) {
		PlotConfigurationTreeNode root = (PlotConfigurationTreeNode) this.root;
		DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChild(dimension);
		child.setUserObject(dimensionConfig);

		// change selection path
		TreePath path = new TreePath(getPathToRoot(child));
		makeVisibleAndSelect(path);
	}

	private void dimensionConfigRemoved(PlotDimension dimension, DimensionConfig dimensionConfig) {
		PlotConfigurationTreeNode root = (PlotConfigurationTreeNode) this.root;
		DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChild(dimension);
		child.setUserObject(null);
	}

	private void rangeAxisMoved(int index, RangeAxisConfig rangeAxisConfig) {
		PlotConfigurationTreeNode rootNode = (PlotConfigurationTreeNode) root;
		RangeAxisConfigTreeNode childNode = (RangeAxisConfigTreeNode) rootNode.getChild(rangeAxisConfig);

		// remove from old parent
		removeNodeFromParent(childNode);

		// add at new index
		int newIndex = index + NUMBER_OF_PERMANENT_DIMENSIONS;
		insertNodeInto(childNode, rootNode, newIndex);

		// change selection path
		TreePath path = new TreePath(getPathToRoot(childNode));
		makeVisibleAndSelect(path);
	}

	private void valueSourcesCleared(RangeAxisConfig source) {
		RangeAxisConfigTreeNode rangeAxisNode = getRangeAxisTreeNode(source);
		if (rangeAxisNode != null) {
			int childCount = rangeAxisNode.getChildCount();
			for (int i = 0; i < childCount; ++i) {
				removeNodeFromParent((MutableTreeNode) rangeAxisNode.getChildAt(i));
			}

			// change selection path
			TreePath path = new TreePath(getPathToRoot(rangeAxisNode));
			makeVisibleAndSelect(path);
		} else {
			throw new RuntimeException("RangeAxisConfig source is not present in TreeModel. This should not happen.");
		}
	}

	private void valueSourceAdded(int index, ValueSource valueSource, RangeAxisConfig source) {
		RangeAxisConfigTreeNode rangeAxisNode = getRangeAxisTreeNode(source);

		if (rangeAxisNode != null) {
			TreeNode child = rangeAxisNode.getChild(valueSource);
			if (child != null) {
				return; // already added..
			}

			// create new value source tree node
			ValueSourceTreeNode newChild = new ValueSourceTreeNode(valueSource);

			// add new tree node
			insertNodeInto(newChild, rangeAxisNode, index);

			// change selection path
			TreePath path = new TreePath(getPathToRoot(newChild));
			makeVisibleAndSelect(path);
		} else {
			throw new RuntimeException("RangeAxisConfig source is not present in TreeModel. This should not happen.");
		}
	}

	private void valueSourceRemoved(ValueSource valueSource, RangeAxisConfig source) {
		RangeAxisConfigTreeNode rangeAxisNode = getRangeAxisTreeNode(source);

		if (rangeAxisNode != null) {
			TreeNode valueSourceNode = rangeAxisNode.getChild(valueSource);

			if (valueSourceNode == null) {
				throw new RuntimeException("ValueSource is not present in TreeModel. This should not happen.");
			}

			int oldIndex = rangeAxisNode.getIndex(valueSourceNode);

			removeNodeFromParent((MutableTreeNode) valueSourceNode);

			// Acquire new selection element
			int childCount = rangeAxisNode.getChildCount();
			Object newSelection = null;

			TreePath path = new TreePath(getPathToRoot(rangeAxisNode));
			if (oldIndex < childCount) {
				newSelection = rangeAxisNode.getChildAt(oldIndex);
				path = path.pathByAddingChild(newSelection);
			} else if (childCount > 0) {
				newSelection = rangeAxisNode.getChildAt(childCount - 1);
				path = path.pathByAddingChild(newSelection);
			}

			// change selection path
			makeVisibleAndSelect(path);

		} else {
			throw new RuntimeException("RangeAxisConfig source is not present in TreeModel. This should not happen.");
		}
	}

	private void makeVisibleAndSelect(TreePath path) {
		reload();
		plotConfigTree.expandAll();
		plotConfigTree.makeVisible(path);
		plotConfigTree.scrollPathToVisible(path);
		plotConfigTree.setSelectionPath(path);
	}

	private void valueSourceMoved(int index, ValueSource valueSource, RangeAxisConfig source) {
		PlotConfigurationTreeNode rootNode = (PlotConfigurationTreeNode) root;

		// get range axis node
		RangeAxisConfigTreeNode sourceNode = (RangeAxisConfigTreeNode) rootNode.getChild(source);

		// get value source node
		ValueSourceTreeNode childNode = (ValueSourceTreeNode) sourceNode.getChild(valueSource);

		// remove value source node from old parent
		removeNodeFromParent(childNode);

		// add value source at new position
		insertNodeInto(childNode, sourceNode, index);

		// change selection path
		TreePath path = new TreePath(getPathToRoot(childNode));
		makeVisibleAndSelect(path);
	}

	private void rangeAxisConfigChanged(RangeAxisConfigChangeEvent change) {

		PlotConfigurationTreeNode rootNode = (PlotConfigurationTreeNode) root;
		RangeAxisConfigTreeNode rangeAxisNode = (RangeAxisConfigTreeNode) rootNode.getChild(change.getSource());

		RangeAxisConfigChangeType type = change.getType();
		switch (type) {
			case CLEARED:
				valueSourcesCleared(change.getSource());
				break;
			case VALUE_SOURCE_ADDED:
				valueSourceAdded(change.getIndex(), change.getValueSource(), change.getSource());
				break;
			case VALUE_SOURCE_CHANGED:
				ValueSource source = change.getValueSourceChange().getSource();
				ValueSourceTreeNode valueSourceNode = (ValueSourceTreeNode) rangeAxisNode.getChild(source);
				nodeChanged(valueSourceNode);
				break;
			case VALUE_SOURCE_MOVED:
				valueSourceMoved(change.getIndex(), change.getValueSource(), change.getSource());
				break;
			case VALUE_SOURCE_REMOVED:
				valueSourceRemoved(change.getValueSource(), change.getSource());
				break;
			default:
				nodeChanged(rangeAxisNode);
		}

	}

	public void exchangePlotConfiguration(PlotConfiguration newPlotConfig) {
		if (plotConfig != null) {
			plotConfig.removePlotConfigurationListener(this);
		}
		plotConfig = newPlotConfig;

		setRoot(new PlotConfigurationTreeNode(newPlotConfig));

		fillNewPlotConfigNode(newPlotConfig);

		newPlotConfig.addPlotConfigurationListener(this);
	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {

		PlotConfigurationChangeType type = change.getType();
		RangeAxisConfigChangeEvent rangeAxisConfigChange = change.getRangeAxisConfigChange();
		switch (type) {
			case DIMENSION_CONFIG_ADDED:
				dimensionConfigAdded(change.getDimension(), change.getDimensionConfig());
				break;
			case DIMENSION_CONFIG_CHANGED:
				dimensionConfigChanged(change);
				break;
			case DIMENSION_CONFIG_REMOVED:
				dimensionConfigRemoved(change.getDimension(), change.getDimensionConfig());
				break;
			case RANGE_AXIS_CONFIG_ADDED:
				rangeAxisConfigAdded(change.getIndex(), change.getRangeAxisConfig());
				break;
			case RANGE_AXIS_CONFIG_CHANGED:
				rangeAxisConfigChanged(rangeAxisConfigChange);
				break;
			case RANGE_AXIS_CONFIG_MOVED:
				rangeAxisMoved(change.getIndex(), change.getRangeAxisConfig());
				break;
			case RANGE_AXIS_CONFIG_REMOVED:
				rangeAxisConfigRemoved(change.getIndex(), change.getRangeAxisConfig());
				break;
			case META_CHANGE:
				for (PlotConfigurationChangeEvent events : change.getPlotConfigChangeEvents()) {
					plotConfigurationChanged(events);
				}
			default:
		}

		return true;
	}

	private void dimensionConfigChanged(PlotConfigurationChangeEvent change) {
		PlotConfigurationTreeNode rootNode = (PlotConfigurationTreeNode) root;
		DimensionConfigChangeEvent dimensionChange = change.getDimensionChange();
		DimensionConfigTreeNode dimensionConfigNode = (DimensionConfigTreeNode) rootNode.getChild(dimensionChange
				.getDimension());

		nodeChanged(dimensionConfigNode);

		if (dimensionChange.getType() == DimensionConfigChangeType.COLUMN) {
			// change selection path
			TreePath path = new TreePath(getPathToRoot(dimensionConfigNode));
			makeVisibleAndSelect(path);
		}
	}

	private RangeAxisConfigTreeNode getRangeAxisTreeNode(RangeAxisConfig rangeAxis) {
		PlotConfigurationTreeNode root = (PlotConfigurationTreeNode) this.root;
		return (RangeAxisConfigTreeNode) root.getChild(rangeAxis);
	}

	private void fillNewPlotConfigNode(PlotConfiguration plotConfig) {

		if (plotConfig.getRangeAxisCount() == 0) {
			plotConfig.addRangeAxisConfig(new RangeAxisConfig(null, plotConfig));
		}

		// CAUTION: If you add more dimension configs you have to adapt
		// NUMBER_OF_PERMANENT_DIMENSIONS!!!

		// add domain dimension config
		insertNodeInto(new DimensionConfigTreeNode(PlotDimension.DOMAIN, plotConfig.getDomainConfigManager()),
				(MutableTreeNode) root, root.getChildCount());

		// add color dimension config
		insertNodeInto(new DimensionConfigTreeNode(PlotDimension.COLOR, plotConfig.getDimensionConfig(PlotDimension.COLOR)),
				(MutableTreeNode) root, root.getChildCount());

		// add shape dimension config
		insertNodeInto(new DimensionConfigTreeNode(PlotDimension.SHAPE, plotConfig.getDimensionConfig(PlotDimension.SHAPE)),
				(MutableTreeNode) root, root.getChildCount());

		// add size dimension config
		insertNodeInto(new DimensionConfigTreeNode(PlotDimension.SIZE, plotConfig.getDimensionConfig(PlotDimension.SIZE)),
				(MutableTreeNode) root, root.getChildCount());

		// CAUTION: If you add more dimension configs you have to adapt
		// NUMBER_OF_PERMANENT_DIMENSIONS!!!

		// at the end add existing range axis tree nodes
		List<RangeAxisConfig> rangeAxisConfigs = plotConfig.getRangeAxisConfigs();
		int idx = 0;
		for (RangeAxisConfig rangeAxis : rangeAxisConfigs) {
			rangeAxisConfigAdded(idx, rangeAxis);
			++idx;
		}
	}
}
