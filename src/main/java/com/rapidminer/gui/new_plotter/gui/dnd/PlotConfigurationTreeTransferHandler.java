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
package com.rapidminer.gui.new_plotter.gui.dnd;

import java.awt.Component;
import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.rapidminer.gui.dnd.AbstractPatchedTransferHandler;
import com.rapidminer.gui.new_plotter.ChartConfigurationException;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.configuration.DefaultDimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.DomainConfigManager;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping.GroupingType;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping.ValueGroupingFactory;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.configuration.ValueSource.SeriesUsageType;
import com.rapidminer.gui.new_plotter.gui.PlotConfigurationTree;
import com.rapidminer.gui.new_plotter.gui.PlotConfigurationTreeModel;
import com.rapidminer.gui.new_plotter.gui.treenodes.DimensionConfigTreeNode;
import com.rapidminer.gui.new_plotter.gui.treenodes.PlotConfigurationTreeNode;
import com.rapidminer.gui.new_plotter.gui.treenodes.RangeAxisConfigTreeNode;
import com.rapidminer.gui.new_plotter.gui.treenodes.ValueSourceTreeNode;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.math.function.aggregation.AbstractAggregationFunction.AggregationFunctionType;


/**
 * The {@link TransferHandler} for the {@link PlotConfigurationTree}. It handles dropping
 * {@link DataTableColumn}s on the tree and exporting {@link ValueSource}s and
 * {@link RangeAxisConfig}s.
 *
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotConfigurationTreeTransferHandler extends AbstractPatchedTransferHandler {

	private static final long serialVersionUID = 1L;

	private static final String[] OPTIONS = {
			I18N.getMessage(I18N.getGUIBundle(),
					"gui.label.input.plotter.drop_below_last_range_axis.option.add_value_source.label"),
			I18N.getMessage(I18N.getGUIBundle(),
					"gui.label.input.plotter.drop_below_last_range_axis.option.create_new_axis.label"),
			I18N.getMessage(I18N.getGUIBundle(), "gui.label.plotter.configuration_dialog.cancel_menu_item.label"),
			I18N.getMessage(I18N.getGUIBundle(),
					"gui.label.input.plotter.drop_below_last_range_axis.option.move_value_source_to_axis_end.label") };

	private static final String[] VALUE_SOURCE_OPTIONS = {
			I18N.getMessage(I18N.getGUIBundle(),
					"gui.label.input.plotter.drop_on_value_source.option.exchange_main_column.label"),
			I18N.getMessage(I18N.getGUIBundle(), "gui.label.input.plotter.drop_on_value_source.option.utility1.label"),
			I18N.getMessage(I18N.getGUIBundle(), "gui.label.input.plotter.drop_on_value_source.option.utility2.label"), };

	private final JTree parent;

	public PlotConfigurationTreeTransferHandler(JTree parent) {
		this.parent = parent;
	}

	/*
	 * ***************** EXPORT **********************
	 */

	@Override
	public int getSourceActions(JComponent c) {
		return MOVE;
	}

	@Override
	public Icon getVisualRepresentation(Transferable t) {
		// ImageIcon iicon = null;
		// String i18nKey = null;
		// if (t.isDataFlavorSupported(RangeAxisConfigTreeNode.RANGE_AXIS_CONFIG_FLAVOR)) {
		// i18nKey = "plotter.configuration_dialog.range_axis";
		// } else if (t.isDataFlavorSupported(ValueSourceTreeNode.VALUE_SOURCE_FLAVOR)) {
		// i18nKey = "plotter.configuration_dialog.value_source";
		// }
		// if (i18nKey != null) {
		// // get icon
		// String icon = I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.label." + i18nKey +
		// ".icon");
		// if (icon != null) {
		// iicon = SwingTools.createIcon("16/" + icon);
		// }
		// }
		// return iicon;
		return null;
	}

	@Override
	public Transferable createTransferable(JComponent c) {
		JTree tree = (JTree) c;
		TreePath selectionPath = tree.getSelectionPath();
		Object lastPathComponent = selectionPath.getLastPathComponent();

		Transferable t = null;
		if (lastPathComponent instanceof ValueSourceTreeNode) {
			t = (ValueSourceTreeNode) lastPathComponent;
		} else if (lastPathComponent instanceof RangeAxisConfigTreeNode) {
			t = (RangeAxisConfigTreeNode) lastPathComponent;
		}
		return t;
	}

	/*
	 * ****************** IMPORT ******************
	 */

	@Override
	public boolean canImport(TransferSupport support) {

		// only support drops
		if (!support.isDrop()) {
			updateDropDeniedTooltip((JComponent) support.getComponent(), null);
			return false;
		}

		if (!doesSupportFlavor(support)) {
			updateDropDeniedTooltip((JComponent) support.getComponent(), null);
			return false;
		}

		// fetch the drop location
		JTree.DropLocation dropLocation = (javax.swing.JTree.DropLocation) support.getDropLocation();

		// get path, if path is null importing is not possible
		TreePath path = dropLocation.getPath();
		if (path == null) {
			updateDropDeniedTooltip((JComponent) support.getComponent(), null);
			return false;
		}

		Object lastPathComponent = path.getLastPathComponent();

		// make drop location and transferable dependent decision
		int childIndex = dropLocation.getChildIndex();
		Transferable transferable = support.getTransferable();

		// check if drop on tree component is possible
		String actionDescription = isDropOnTreeComponentPossible(support.getComponent(), (TreeNode) lastPathComponent,
				transferable, childIndex);
		if (actionDescription == null) {
			return false;
		}

		// check if the source actions (a bitwise-OR of supported actions)
		// contains the COPY action
		boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;
		if (copySupported) {
			support.setDropAction(COPY);
			updateDropDeniedTooltip((JComponent) support.getComponent(), actionDescription);
			return true;
		}
		boolean moveSupported = (MOVE & support.getSourceDropActions()) == MOVE;
		if (moveSupported) {
			support.setDropAction(MOVE);
			updateDropDeniedTooltip((JComponent) support.getComponent(), actionDescription);
			return true;
		}

		updateDropDeniedTooltip((JComponent) support.getComponent(), null);
		// reject the transfer if copy or move is not supported
		return false;

	}

	public boolean doesSupportFlavor(TransferSupport support) {

		// check if transferable is DataTableColumn, ValueSourceTreeNode or
		// RangeAxisConfigTreeNode
		if (!(support.isDataFlavorSupported(ValueSourceTreeNode.VALUE_SOURCE_FLAVOR)
				|| support.isDataFlavorSupported(RangeAxisConfigTreeNode.RANGE_AXIS_CONFIG_FLAVOR)
				|| support.isDataFlavorSupported(DataTableColumnCollection.DATATABLE_COLUMN_COLLECTION_FLAVOR))) {
			return false;
		}

		return true;
	}

	@Override
	public boolean importData(TransferSupport support) {

		try {

			// check if transferable can be imported
			if (!canImport(support)) {
				return false;
			}

			// fetch the drop location
			JTree.DropLocation dropLocation = (javax.swing.JTree.DropLocation) support.getDropLocation();
			TreePath path = dropLocation.getPath();
			int childIndex = dropLocation.getChildIndex();
			support.setShowDropLocation(true);

			if (support.isDataFlavorSupported(RangeAxisConfigTreeNode.RANGE_AXIS_CONFIG_FLAVOR)) {
				rangeAxisConfigDrop(support, path, childIndex);
			} else if (support.isDataFlavorSupported(ValueSourceTreeNode.VALUE_SOURCE_FLAVOR)) {
				ValueSourceTreeNode valueSourceTreeNode = (ValueSourceTreeNode) support.getTransferable()
						.getTransferData(ValueSourceTreeNode.VALUE_SOURCE_FLAVOR);
				valueSourceDrop(valueSourceTreeNode, path, childIndex, false);
			} else if (support.isDataFlavorSupported(DataTableColumnCollection.DATATABLE_COLUMN_COLLECTION_FLAVOR)) {
				dataTableColumnDrop(support, path, childIndex);
			} else {
				return false;
			}

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Returns <code>null</code> if drop is not possible. Returns string that describes action that
	 * would happen on drop otherwise.
	 */
	public String isDropOnTreeComponentPossible(Component container, TreeNode treeNode, Transferable transferable,
			int childIndex) {
		try {
			if (transferable.isDataFlavorSupported(ValueSourceTreeNode.VALUE_SOURCE_FLAVOR)) {
				return canImportValueSource(container, treeNode, transferable, childIndex);
			} else if (transferable.isDataFlavorSupported(RangeAxisConfigTreeNode.RANGE_AXIS_CONFIG_FLAVOR)) {
				return canImportRangeAxis(container, treeNode, childIndex, container);
			} else if (transferable.isDataFlavorSupported(DataTableColumnCollection.DATATABLE_COLUMN_COLLECTION_FLAVOR)) {
				return canImportDataTableColumnCollection(container, treeNode, transferable, childIndex);
			}
			return null;
		} catch (IOException e) {
			// Should not happen, has been checked before
			e.printStackTrace();
			return null;
		} catch (UnsupportedFlavorException e) {
			// Should not happen, has been checked before
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @param container
	 * @param treeNode
	 * @param transferable
	 * @param childIndex
	 * @return
	 */
	private String canImportDataTableColumnCollection(Component container, TreeNode treeNode, Transferable transferable,
			int childIndex) {
		String importAction = I18N.getGUILabel("plotter.drag_and_drop.column_drop.create_or_add");

		DataTableColumnCollection collection;
		try {
			collection = (DataTableColumnCollection) transferable
					.getTransferData(DataTableColumnCollection.DATATABLE_COLUMN_COLLECTION_FLAVOR);
		} catch (UnsupportedFlavorException e) {
			return null;
		} catch (IOException e) {
			return null;
		}

		int size = collection.getDataTableColumns().size();
		if (size == 0) {
			updateToolTip(container, I18N.getGUILabel("plotter.drag_and_drop.column_drop.error"));
			return null;
		}

		if (treeNode instanceof RangeAxisConfigTreeNode) {
			if (childIndex == -1 || childIndex < treeNode.getChildCount()) {
				return I18N.getGUILabel("plotter.drag_and_drop.column_drop.add_to_axis");
			}
			return importAction;
		} else if (treeNode instanceof PlotConfigurationTreeNode) {
			if (childIndex > -1 && childIndex < PlotConfigurationTreeModel.NUMBER_OF_PERMANENT_DIMENSIONS) {
				updateToolTip(container, I18N.getGUILabel("plotter.drag_and_drop.column_drop.cant_drop_between_dims"));
				return null;
			}
			if (childIndex == PlotConfigurationTreeModel.NUMBER_OF_PERMANENT_DIMENSIONS) {
				return I18N.getGUILabel("plotter.drag_and_drop.column_drop.create_after_dims");
			}
			if (childIndex == -1) {
				return I18N.getGUILabel("plotter.drag_and_drop.column_drop.create_at_end");
			}
			return importAction;
		} else if (size == 1) {
			if (treeNode instanceof DimensionConfigTreeNode) {
				importAction = I18N.getGUILabel("plotter.drag_and_drop.column_drop.set_dim_column");
			} else if (treeNode instanceof ValueSourceTreeNode) {
				// check if value source allwos adding value type
				importAction = I18N.getGUILabel("plotter.drag_and_drop.column_drop.drop_on_data_config");
			}
			return importAction;
		} else {
			updateToolTip(container, I18N.getGUILabel("plotter.drag_and_drop.column_drop.drop_collection_not_possible"));
			return null;
		}

	}

	/**
	 * @param container
	 * @param comp
	 * @param childIndex
	 * @param container2
	 */
	private String canImportRangeAxis(Component container, TreeNode treeNode, int childIndex, Component container2) {
		String importAction = I18N.getGUILabel("plotter.drag_and_drop.axis_drop.move_axis");

		// RangeAxisConfigTreeNode can only be dropped on PlotConfigTreeNodes
		if (!(treeNode instanceof PlotConfigurationTreeNode)) {
			updateToolTip(container, I18N.getGUILabel("plotter.drag_and_drop.axis_drop.cant_drop_axis_config"));
			return null;
		}
		// can only be dropped below static dimension configs
		if (childIndex > -1 && childIndex < PlotConfigurationTreeModel.NUMBER_OF_PERMANENT_DIMENSIONS) {
			updateToolTip(container, I18N.getGUILabel("plotter.drag_and_drop.axis_drop.only_below_dims"));
			return null;
		}

		if (childIndex == -1) {
			return I18N.getGUILabel("plotter.drag_and_drop.axis_drop.move_to_end");
		}

		return importAction;
	}

	/**
	 * @param comp
	 * @param transferable
	 * @param childIndex
	 * @throws UnsupportedFlavorException
	 * @throws IOException
	 */
	private String canImportValueSource(Component comp, TreeNode treeNode, Transferable transferable, int childIndex)
			throws UnsupportedFlavorException, IOException {
		String importAction = null;

		// ValueSourceTreeNodes can only be dropped on RangeAxisConfigTreeNodes and
		// PlotConfigTreeNodes
		if (treeNode instanceof RangeAxisConfigTreeNode) {
			if (childIndex == -1) {
				importAction = I18N.getGUILabel("plotter.drag_and_drop.source_drop.move_to_axis_end");
			} else {
				importAction = I18N.getGUILabel("plotter.drag_and_drop.source_drop.move_or_change_position");
			}
		} else if (treeNode instanceof PlotConfigurationTreeNode) {
			// can only be dropped below static dimension configs
			if (childIndex > -1 && childIndex < PlotConfigurationTreeModel.NUMBER_OF_PERMANENT_DIMENSIONS) {
				updateToolTip(comp, I18N.getGUILabel("plotter.drag_and_drop.source_drop.only_below_dims"));
				return null;
			}
			if (childIndex == -1) {
				importAction = I18N.getGUILabel("plotter.drag_and_drop.column_drop.create_at_end");
			} else {
				importAction = I18N.getGUILabel("plotter.drag_and_drop.source_drop.create_new_or_add");
			}

		} else {
			updateToolTip(comp, I18N.getGUILabel("plotter.drag_and_drop.source_drop.cant_drop"));
			return null;
		}

		return importAction;
	}

	private void updateToolTip(Component comp, String reason) {
		if (comp instanceof JComponent) {
			JComponent jComp = (JComponent) comp;
			updateDropDeniedTooltip(jComp, reason);
		}
	}

	/**
	 * Is called if a {@link ValueSourceTreeNode} is dropped on the {@link PlotConfigurationTree}.
	 */
	private void valueSourceDrop(final ValueSourceTreeNode valueSourceTreeNode, TreePath path, final int childIndex,
			boolean importDataTableColumn) throws UnsupportedFlavorException, IOException, ChartConfigurationException {

		// fetch dropped value source and parent
		final ValueSource valueSource = valueSourceTreeNode.getUserObject();

		Object lastPathComponent = path.getLastPathComponent();
		if (lastPathComponent instanceof RangeAxisConfigTreeNode) {
			valueSourceDropOnRangeAxisConfig(valueSourceTreeNode, childIndex, valueSource, lastPathComponent);
		} else {
			valueSourceDropOnPlotConfiguration(valueSourceTreeNode, childIndex, valueSource, lastPathComponent,
					importDataTableColumn);
		}

	}

	private void valueSourceDropOnPlotConfiguration(final ValueSourceTreeNode valueSourceTreeNode, final int childIndex,
			final ValueSource valueSource, Object lastPathComponent, final boolean importedDataTableColumn) {

		// get plot configuration
		final PlotConfigurationTreeNode plotConfigurationTreeNode = (PlotConfigurationTreeNode) lastPathComponent;
		final PlotConfiguration plotConfiguration = plotConfigurationTreeNode.getUserObject();

		final int index = childIndex - PlotConfigurationTreeModel.NUMBER_OF_PERMANENT_DIMENSIONS;
		if (index >= 0) {
			valueSourceDropOnPlotConfigBelowDimensionConfigs(valueSourceTreeNode, childIndex, valueSource,
					plotConfigurationTreeNode, plotConfiguration, index, importedDataTableColumn);
		} else {
			// value source is dropped on PlotConfigurationTreeNode

			// fetch old parent
			RangeAxisConfigTreeNode parent = (RangeAxisConfigTreeNode) valueSourceTreeNode.getParent();

			boolean process = plotConfiguration.isProcessingEvents(); // save processing state
			plotConfiguration.setProcessEvents(false);

			if (parent != null) {
				// ValueSource is moved to new RangeAxisConfig and has a parent

				// Remove from old RangeAxis
				RangeAxisConfig oldRangeAxis = parent.getUserObject();
				oldRangeAxis.removeValueSource(valueSource);

			}

			// create new RangeAxis
			RangeAxisConfig newRangeAxis = new RangeAxisConfig(null, plotConfiguration);

			SeriesFormat newSeriesFormat = null;
			if (importedDataTableColumn) {
				newSeriesFormat = plotConfiguration.getAutomaticSeriesFormatForNextValueSource(newRangeAxis);
			}

			// user drops on plot configuration tree node
			newRangeAxis.addValueSource(valueSource, newSeriesFormat);
			plotConfiguration.addRangeAxisConfig(newRangeAxis);

			plotConfiguration.setProcessEvents(process);
		}
	}

	private void valueSourceDropOnPlotConfigBelowDimensionConfigs(final ValueSourceTreeNode valueSourceTreeNode,
			final int childIndex, final ValueSource valueSource, final PlotConfigurationTreeNode plotConfigurationTreeNode,
			final PlotConfiguration plotConfiguration, final int index, final boolean importedDataTableColumn) {

		if (index > 0) {
			valueSourceDropOnPlotConfigBelowLastRangeAxis(valueSourceTreeNode, childIndex, valueSource,
					plotConfigurationTreeNode, plotConfiguration, index, importedDataTableColumn);
		} else {

			// fetch old parent
			RangeAxisConfigTreeNode parent = (RangeAxisConfigTreeNode) valueSourceTreeNode.getParent();

			boolean process = plotConfiguration.isProcessingEvents(); // save processing state
			plotConfiguration.setProcessEvents(false);

			if (parent != null) {
				// ValueSource is moved to new RangeAxisConfig and has a parent

				// Remove from old RangeAxis
				RangeAxisConfig oldRangeAxis = parent.getUserObject();
				oldRangeAxis.removeValueSource(valueSource);

			}

			RangeAxisConfig newRangeAxis = new RangeAxisConfig(null, plotConfiguration);

			// fetch new series format of data table column is imported
			SeriesFormat newSeriesFormat = null;
			if (importedDataTableColumn) {
				newSeriesFormat = plotConfiguration.getAutomaticSeriesFormatForNextValueSource(newRangeAxis);
			}

			// add value source
			newRangeAxis.addValueSource(valueSource, newSeriesFormat);

			// add range axis to plot configuration
			plotConfiguration.addRangeAxisConfig(index, newRangeAxis);

			plotConfiguration.setProcessEvents(process);
		}
	}

	private void valueSourceDropOnPlotConfigBelowLastRangeAxis(final ValueSourceTreeNode valueSourceTreeNode,
			final int childIndex, final ValueSource valueSource, final PlotConfigurationTreeNode plotConfigurationTreeNode,
			final PlotConfiguration plotConfiguration, final int index, final boolean importedDataTableColumn) {
		// ask user for advice

		// create popup menu
		final JPopupMenu rangeAxisPopupMenu = new JPopupMenu();

		// get last range axis config
		final RangeAxisConfig rangeAxis = (RangeAxisConfig) ((DefaultMutableTreeNode) plotConfigurationTreeNode
				.getChildAt(childIndex - 1)).getUserObject();

		String menuItemText = OPTIONS[0];
		if (!importedDataTableColumn) {
			menuItemText = OPTIONS[3];
		}
		JMenuItem menuItem = new JMenuItem(menuItemText);
		menuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// user wants to add value source to range axis config

				// fetch old parent
				RangeAxisConfigTreeNode parent = (RangeAxisConfigTreeNode) valueSourceTreeNode.getParent();

				boolean process = plotConfiguration.isProcessingEvents();
				plotConfiguration.setProcessEvents(false);

				if (parent != null) {
					// ValueSource is moved to new RangeAxisConfig and has a parent

					// Remove from old RangeAxis
					RangeAxisConfig oldRangeAxis = parent.getUserObject();
					oldRangeAxis.removeValueSource(valueSource);

				}

				// fetch new series format of data table column is imported
				SeriesFormat newSeriesFormat = null;
				if (importedDataTableColumn) {
					newSeriesFormat = plotConfiguration.getAutomaticSeriesFormatForNextValueSource(rangeAxis);
				}

				// add value source
				rangeAxis.addValueSource(valueSource, newSeriesFormat);

				plotConfiguration.setProcessEvents(process);

			}

		});
		rangeAxisPopupMenu.add(menuItem);

		menuItem = new JMenuItem(OPTIONS[1]);
		menuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// user wants to create a new range axis config

				// fetch old parent
				RangeAxisConfigTreeNode parent = (RangeAxisConfigTreeNode) valueSourceTreeNode.getParent();

				boolean process = plotConfiguration.isProcessingEvents(); // save processing state
				plotConfiguration.setProcessEvents(false);

				if (parent != null) {
					// ValueSource is moved to new RangeAxisConfig and has a parent

					// Remove from old RangeAxis
					RangeAxisConfig oldRangeAxis = parent.getUserObject();
					oldRangeAxis.removeValueSource(valueSource);

				}

				RangeAxisConfig newRangeAxis = new RangeAxisConfig(null, plotConfiguration);

				// add range axis to plot configuration
				plotConfiguration.addRangeAxisConfig(index, newRangeAxis);

				// fetch new series format of data table column is imported
				SeriesFormat newSeriesFormat = null;
				if (importedDataTableColumn) {
					newSeriesFormat = plotConfiguration.getAutomaticSeriesFormatForNextValueSource(newRangeAxis);
				}

				// add value source
				newRangeAxis.addValueSource(valueSource, newSeriesFormat);

				plotConfiguration.setProcessEvents(process); // restore old processing state

			}

		});
		rangeAxisPopupMenu.add(menuItem);

		rangeAxisPopupMenu.addSeparator();

		menuItem = new JMenuItem(OPTIONS[2]);
		Font font = menuItem.getFont();
		menuItem.setFont(FontTools.getFont(font.getFamily(), Font.ITALIC, font.getSize()));
		rangeAxisPopupMenu.add(menuItem);

		// get mouse position
		PointerInfo mouseInfo = MouseInfo.getPointerInfo();
		Point point = mouseInfo.getLocation();

		SwingUtilities.convertPointFromScreen(point, parent);

		// show popup menu
		rangeAxisPopupMenu.show(parent, (int) point.getX(), (int) point.getY());
	}

	private void valueSourceDropOnRangeAxisConfig(final ValueSourceTreeNode valueSourceTreeNode, final int childIndex,
			final ValueSource valueSource, Object lastPathComponent) {

		// fetch last path range axis tree node and containing range axis
		RangeAxisConfigTreeNode rangeAxisTreeNode = (RangeAxisConfigTreeNode) lastPathComponent;
		RangeAxisConfig rangeAxis = rangeAxisTreeNode.getUserObject();

		PlotConfiguration plotConfig = (PlotConfiguration) ((DefaultMutableTreeNode) rangeAxisTreeNode.getParent())
				.getUserObject();

		// fetch old parent
		RangeAxisConfigTreeNode parent = (RangeAxisConfigTreeNode) valueSourceTreeNode.getParent();
		if (parent != lastPathComponent) {
			// ValueSource is moved to new RangeAxisConfig

			// Remove from old RangeAxis
			RangeAxisConfig oldRangeAxis = parent.getUserObject();

			boolean process = plotConfig.isProcessingEvents();
			plotConfig.setProcessEvents(false);

			oldRangeAxis.removeValueSource(valueSource);

			// Add to new RangeAxis
			if (childIndex < 0) {
				rangeAxis.addValueSource(valueSource, null);
			} else {
				rangeAxis.addValueSource(childIndex, valueSource, null);
			}

			plotConfig.setProcessEvents(process);

		} else {
			// ValueSource is moved within old parent RangeAxisConfig
			if (childIndex < 0) {
				rangeAxis.changeIndex(rangeAxis.getSize() - 1, valueSource);
			} else {
				rangeAxis.changeIndex(childIndex, valueSource);
			}
		}
	}

	private void rangeAxisConfigDrop(TransferSupport support, TreePath path, int childIndex)
			throws UnsupportedFlavorException, IOException {

		// fetch dropped value source and parent
		RangeAxisConfigTreeNode rangeAxisConfigTreeNode = (RangeAxisConfigTreeNode) support.getTransferable()
				.getTransferData(RangeAxisConfigTreeNode.RANGE_AXIS_CONFIG_FLAVOR);
		RangeAxisConfig rangeAxis = rangeAxisConfigTreeNode.getUserObject();
		PlotConfigurationTreeNode root = (PlotConfigurationTreeNode) rangeAxisConfigTreeNode.getParent();
		PlotConfiguration plotConfig = root.getUserObject();

		int maxIndex = plotConfig.getRangeAxisCount() - 1;
		int newIndex = childIndex - PlotConfigurationTreeModel.NUMBER_OF_PERMANENT_DIMENSIONS;
		if (childIndex < 0 || newIndex > maxIndex) {
			plotConfig.changeIndex(maxIndex, rangeAxis);
		} else {
			plotConfig.changeIndex(newIndex, rangeAxis);
		}

	}

	private void dataTableColumnDrop(TransferSupport support, TreePath path, final int childIndex)
			throws ChartConfigurationException, UnsupportedFlavorException, IOException {

		// fetch data table column
		Transferable transferable = support.getTransferable();

		List<DataTableColumn> dataTableColumnList = new LinkedList<DataTableColumn>();
		DataTableColumnCollection dataTableColumCollection = (DataTableColumnCollection) transferable
				.getTransferData(DataTableColumnCollection.DATATABLE_COLUMN_COLLECTION_FLAVOR);
		dataTableColumnList.addAll(dataTableColumCollection.getDataTableColumns());

		Object lastPathComponent = path.getLastPathComponent();

		// check dropping target
		if (lastPathComponent instanceof PlotConfigurationTreeNode) {

			// get plot configuration
			final PlotConfigurationTreeNode plotConfigurationTreeNode = (PlotConfigurationTreeNode) lastPathComponent;
			final PlotConfiguration plotConfiguration = plotConfigurationTreeNode.getUserObject();

			List<RangeAxisConfig> rangeAxisConfigs = plotConfiguration.getRangeAxisConfigs();
			int size = rangeAxisConfigs.size();
			boolean grouping = false;
			if (size > 0) {
				grouping = plotConfiguration.isGroupingRequiredForNewValueSource(rangeAxisConfigs.get(size - 1));
			}

			List<ValueSource> valueSources = new LinkedList<ValueSource>();
			for (DataTableColumn dtc : dataTableColumnList) {
				ValueSource newValueSource = new ValueSource(plotConfiguration, dtc, AggregationFunctionType.average,
						grouping);
				valueSources.add(newValueSource);
			}

			if (dataTableColumnList.size() == 1) {
				valueSourceDrop(new ValueSourceTreeNode(valueSources.get(0)), path, childIndex, true);
			} else {
				multipleDataTableColumnsDropOnPlotConfigTreeNode(valueSources, path, childIndex);
			}

		} else if (lastPathComponent instanceof RangeAxisConfigTreeNode) {

			// get tree node
			RangeAxisConfigTreeNode rangeAxisConfigTreeNode = (RangeAxisConfigTreeNode) lastPathComponent;

			// get plot configuration
			PlotConfiguration plotConfiguration = ((PlotConfigurationTreeNode) ((RangeAxisConfigTreeNode) lastPathComponent)
					.getParent()).getUserObject();

			// get range axis config
			RangeAxisConfig rangeAxis = rangeAxisConfigTreeNode.getUserObject();

			// determine grouping properties of new value source
			boolean grouped = plotConfiguration.isGroupingRequiredForNewValueSource(rangeAxis);
			ValueSource referenceValueSource = null;
			if (!rangeAxis.getValueSources().isEmpty()) {
				referenceValueSource = rangeAxis.getValueSources().get(rangeAxis.getValueSources().size() - 1);
			}
			AggregationFunctionType newAggregationFunctionType = null;
			if (referenceValueSource != null) {
				newAggregationFunctionType = referenceValueSource.getAggregationFunctionType(SeriesUsageType.MAIN_SERIES);
			} else if (referenceValueSource == null && grouped) {
				newAggregationFunctionType = AggregationFunctionType.count;
			}

			boolean process = plotConfiguration.isProcessingEvents();  // save processing state
			plotConfiguration.setProcessEvents(false);

			// create new value source
			if (childIndex >= 0) {
				for (int i = dataTableColumnList.size() - 1; i >= 0; --i) {
					DataTableColumn dtc = dataTableColumnList.get(i);
					ValueSource newValueSource = new ValueSource(plotConfiguration, dtc, newAggregationFunctionType,
							grouped);
					rangeAxis.addValueSource(childIndex, newValueSource,
							plotConfiguration.getAutomaticSeriesFormatForNextValueSource(rangeAxis));
				}
			} else {
				for (DataTableColumn dtc : dataTableColumnList) {
					ValueSource newValueSource = new ValueSource(plotConfiguration, dtc, newAggregationFunctionType,
							grouped);
					rangeAxis.addValueSource(newValueSource,
							plotConfiguration.getAutomaticSeriesFormatForNextValueSource(rangeAxis));
				}
			}

			plotConfiguration.setProcessEvents(process);  // restore old processing state

			// FROM HERE ON DROPPING ONLY ONE DATATABLECOLUMN IS POSSIBLE THUS GET(0) IS ALLOWED IN
			// THIS CASE
		} else if (lastPathComponent instanceof DimensionConfigTreeNode) {
			DataTableColumn dataTableColumn = dataTableColumnList.get(0);
			DimensionConfigTreeNode dimensionConfigTreeNode = (DimensionConfigTreeNode) lastPathComponent;
			PlotDimension dimension = dimensionConfigTreeNode.getDimension();

			JTree dropTarget = (JTree) support.getComponent();
			PlotConfigurationTreeNode root = (PlotConfigurationTreeNode) dropTarget.getModel().getRoot();
			PlotConfiguration plotConfig = root.getUserObject();

			boolean process = plotConfig.isProcessingEvents();

			plotConfig.setProcessEvents(false); // set processing false;

			if (dimension == PlotDimension.DOMAIN) {

				// get domain config manager
				DomainConfigManager domainConfigMngr = (DomainConfigManager) dimensionConfigTreeNode.getUserObject();

				// change domain column
				domainConfigMngr.setDataTableColumn(dataTableColumn);
			} else {
				DimensionConfig dimensionConfig = dimensionConfigTreeNode.getUserObject();

				// if there is a dimension config, change data table column
				ValueType valueType = dataTableColumn.getValueType();
				if (dimensionConfig != null) {

					if (dimensionConfig.isGrouping()) {
						if (valueType != dimensionConfig.getDataTableColumn().getValueType()) {
							switch (valueType) {
								case NOMINAL:
									dimensionConfig
											.setGrouping(ValueGroupingFactory.getValueGrouping(GroupingType.DISTINCT_VALUES,
													dataTableColumn, true, dimensionConfig.getDateFormat()));
									break;
								case DATE_TIME:
								case NUMERICAL:
									dimensionConfig.setGrouping(
											ValueGroupingFactory.getValueGrouping(GroupingType.EQUIDISTANT_FIXED_BIN_COUNT,
													dataTableColumn, true, dimensionConfig.getDateFormat()));
									break;
								default:
									break;
							}

						}
					}

					dimensionConfig.setDataTableColumn(dataTableColumn);
				} else {

					// else create a new dimension config
					DefaultDimensionConfig newDimensionConfig = new DefaultDimensionConfig(plotConfig, dataTableColumn,
							dimension);

					boolean grouped = plotConfig.getDomainConfigManager().isGrouping();

					if (grouped) {
						switch (valueType) {
							case NOMINAL:
								newDimensionConfig
										.setGrouping(ValueGroupingFactory.getValueGrouping(GroupingType.DISTINCT_VALUES,
												dataTableColumn, true, newDimensionConfig.getDateFormat()));
								break;
							case DATE_TIME:
							case NUMERICAL:
								newDimensionConfig.setGrouping(
										ValueGroupingFactory.getValueGrouping(GroupingType.EQUIDISTANT_FIXED_BIN_COUNT,
												dataTableColumn, true, newDimensionConfig.getDateFormat()));
								break;
							default:
								break;
						}
					}

					plotConfig.setDimensionConfig(dimension, newDimensionConfig);
				}
			}

			plotConfig.setProcessEvents(process); // restore processing state

		} else if (lastPathComponent instanceof ValueSourceTreeNode) {
			final DataTableColumn dataTableColumn = dataTableColumnList.get(0);
			ValueSourceTreeNode valueSourceNode = (ValueSourceTreeNode) lastPathComponent;
			final ValueSource valueSource = valueSourceNode.getUserObject();

			ActionListener valueSourceExchangeAction = new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// user wants to add exchange value source data table column

					if (valueSource instanceof ValueSource) {
						ValueSource source = valueSource;
						try {
							source.setDataTableColumn(SeriesUsageType.MAIN_SERIES, dataTableColumn);
						} catch (ChartConfigurationException e1) {
							e1.printStackTrace();
							SwingTools.showVerySimpleErrorMessage("plotting.general_error");
						}
					}
				}
			};

			ActionListener valueSourceUtility1Action = new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// user wants to add exchange value source data table column

					if (valueSource instanceof ValueSource) {
						ValueSource source = valueSource;
						try {
							source.setDataTableColumn(SeriesUsageType.INDICATOR_1, dataTableColumn);
						} catch (ChartConfigurationException e1) {
							e1.printStackTrace();
							SwingTools.showVerySimpleErrorMessage("plotting.general_error");
						}
					}
				}
			};

			ActionListener valueSourceUtility2Action = new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// user wants to add exchange value source data table column

					if (valueSource instanceof ValueSource) {
						ValueSource source = valueSource;
						try {
							source.setDataTableColumn(SeriesUsageType.INDICATOR_2, dataTableColumn);
						} catch (ChartConfigurationException e1) {
							e1.printStackTrace();
							SwingTools.showVerySimpleErrorMessage("plotting.general_error");
						}
					}
				}
			};

			// create popup menu
			JPopupMenu valueSourcePopupMenu = new JPopupMenu();

			JMenuItem menuItem = new JMenuItem(VALUE_SOURCE_OPTIONS[0]);
			menuItem.addActionListener(valueSourceExchangeAction);
			valueSourcePopupMenu.add(menuItem);

			menuItem = new JMenuItem(VALUE_SOURCE_OPTIONS[1]);
			menuItem.addActionListener(valueSourceUtility1Action);
			valueSourcePopupMenu.add(menuItem);

			menuItem = new JMenuItem(VALUE_SOURCE_OPTIONS[2]);
			menuItem.addActionListener(valueSourceUtility2Action);
			valueSourcePopupMenu.add(menuItem);

			valueSourcePopupMenu.addSeparator();
			menuItem = new JMenuItem(OPTIONS[2]);
			Font font = menuItem.getFont();
			menuItem.setFont(FontTools.getFont(font.getFamily(), Font.ITALIC, font.getSize()));
			valueSourcePopupMenu.add(menuItem);

			// get mouse position
			PointerInfo mouseInfo = MouseInfo.getPointerInfo();
			Point point = mouseInfo.getLocation();

			SwingUtilities.convertPointFromScreen(point, parent);

			// show popup menu
			valueSourcePopupMenu.show(parent, (int) point.getX(), (int) point.getY());
		}
	}

	private void multipleDataTableColumnsDropOnPlotConfigTreeNode(final List<ValueSource> valueSources, TreePath path,
			int childIndex) {

		Object lastPathComponent = path.getLastPathComponent();

		// get plot configuration
		final PlotConfigurationTreeNode plotConfigurationTreeNode = (PlotConfigurationTreeNode) lastPathComponent;
		final PlotConfiguration plotConfiguration = plotConfigurationTreeNode.getUserObject();

		final int index = childIndex - PlotConfigurationTreeModel.NUMBER_OF_PERMANENT_DIMENSIONS;
		if (index > 0) {

			// ask user for advice

			// create popup menu
			final JPopupMenu rangeAxisPopupMenu = new JPopupMenu();

			// get last range axis config
			final RangeAxisConfig rangeAxis = (RangeAxisConfig) ((DefaultMutableTreeNode) plotConfigurationTreeNode
					.getChildAt(childIndex - 1)).getUserObject();

			JMenuItem menuItem = new JMenuItem(OPTIONS[0]);
			menuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// user wants to add value sources to range axis config

					// save old processing state
					boolean tmp_processing = plotConfiguration.isProcessingEvents();
					plotConfiguration.setProcessEvents(false);

					for (ValueSource valueSource : valueSources) {
						rangeAxis.addValueSource(valueSource,
								plotConfiguration.getAutomaticSeriesFormatForNextValueSource(rangeAxis));
					}

					// restore old processing state
					plotConfiguration.setProcessEvents(tmp_processing);
				}

			});
			rangeAxisPopupMenu.add(menuItem);

			menuItem = new JMenuItem(OPTIONS[1]);
			menuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// user wants to create a new range axis config

					// create new RangeAxis
					RangeAxisConfig newRangeAxis = new RangeAxisConfig(null, plotConfiguration);

					// save old processing state
					boolean process = plotConfiguration.isProcessingEvents();
					plotConfiguration.setProcessEvents(false);

					// add range axis to plot config
					plotConfiguration.addRangeAxisConfig(newRangeAxis);

					// add value sources
					for (ValueSource valueSource : valueSources) {
						newRangeAxis.addValueSource(valueSource,
								plotConfiguration.getAutomaticSeriesFormatForNextValueSource(rangeAxis));
					}

					plotConfiguration.setProcessEvents(process); // restore old processing state

				}

			});
			rangeAxisPopupMenu.add(menuItem);

			rangeAxisPopupMenu.addSeparator();

			menuItem = new JMenuItem(OPTIONS[2]);
			Font font = menuItem.getFont();
			menuItem.setFont(FontTools.getFont(font.getFamily(), Font.ITALIC, font.getSize()));
			rangeAxisPopupMenu.add(menuItem);

			// get mouse position
			PointerInfo mouseInfo = MouseInfo.getPointerInfo();
			Point point = mouseInfo.getLocation();

			SwingUtilities.convertPointFromScreen(point, parent);

			// show popup menu
			rangeAxisPopupMenu.show(parent, (int) point.getX(), (int) point.getY());

		} else {
			// value source is dropped on PlotConfigurationTreeNode

			// create new RangeAxis
			RangeAxisConfig newRangeAxis = new RangeAxisConfig(null, plotConfiguration);

			boolean tmp_processing = plotConfiguration.isProcessingEvents();  // save all processing
																			  // status
			plotConfiguration.setProcessEvents(false);

			plotConfiguration.addRangeAxisConfig(newRangeAxis);

			for (ValueSource valueSource : valueSources) {
				newRangeAxis.addValueSource(valueSource,
						plotConfiguration.getAutomaticSeriesFormatForNextValueSource(newRangeAxis));
			}

			plotConfiguration.setProcessEvents(tmp_processing); // revert old processing status

		}
	}
}
