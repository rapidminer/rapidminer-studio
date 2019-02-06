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

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.management.RuntimeErrorException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler.TransferSupport;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.export.PrintableComponent;
import com.rapidminer.gui.dnd.DragListener;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.new_plotter.MasterOfDesaster;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.engine.jfreechart.JFreeChartPlotEngine;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.LinkAndBrushChartPanel;
import com.rapidminer.gui.new_plotter.gui.cellrenderer.DataTableColumnListCellRenderer;
import com.rapidminer.gui.new_plotter.gui.cellrenderer.EnumComboBoxCellRenderer;
import com.rapidminer.gui.new_plotter.gui.dnd.DataTableColumnListTransferHandler;
import com.rapidminer.gui.new_plotter.gui.dnd.PlotConfigurationTreeTransferHandler;
import com.rapidminer.gui.new_plotter.gui.treenodes.DimensionConfigTreeNode;
import com.rapidminer.gui.new_plotter.gui.treenodes.PlotConfigurationTreeNode;
import com.rapidminer.gui.new_plotter.gui.treenodes.RangeAxisConfigTreeNode;
import com.rapidminer.gui.new_plotter.gui.treenodes.ValueSourceTreeNode;
import com.rapidminer.gui.new_plotter.listener.MasterOfDesasterListener;
import com.rapidminer.gui.new_plotter.listener.PlotConfigurationProcessingListener;
import com.rapidminer.gui.new_plotter.listener.PlotInstanceChangedListener;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.utility.DataTransformation;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ChartConfigurationPanel extends AbstractConfigurationPanel
		implements MasterOfDesasterListener, DragListener, PlotConfigurationProcessingListener, PrintableComponent {

	private static final Insets STANDARD_INSETS = new Insets(2, 2, 2, 2);

	private static final long serialVersionUID = 1L;

	private static final int LEFT_SIDE_WIDTH = 100;

	private static final Dimension MAX_LIST_AND_TREE_DIMENSION = new Dimension(LEFT_SIDE_WIDTH, 300);
	private static final Dimension MIN_LIST_AND_TREE_DIMENSION = new Dimension(LEFT_SIDE_WIDTH, 100);
	private static final Dimension PREFFERED_LIST_AND_TREE_DIMENSION = new Dimension(LEFT_SIDE_WIDTH, 170);

	private static final int PREFERRED_CHART_HEIGTH = 500;
	private static final int PERFERRED_CHART_WIDTH = 600;

	private static final int MAX_CHART_HEIGTH = 650;
	private static final int MAX_CHART_WIDTH = 780;

	private static final int MIN_CHART_HEIGTH = 200;
	private static final int MIN_CHART_WIDTH = 200;

	private static final Dimension PREFERRED_CHART_SIZE = new Dimension(PERFERRED_CHART_WIDTH, PREFERRED_CHART_HEIGTH);
	private static final Dimension MAX_CHART_SIZE = new Dimension(MAX_CHART_WIDTH, MAX_CHART_HEIGTH);
	private static final Dimension MIN_CHART_SIZE = new Dimension(MIN_CHART_WIDTH, MIN_CHART_HEIGTH);

	private static final String EMPTY = "Empty";
	private static final String GLOBAL_CONFIG = "GlobalConfig";
	private static final String RANGE_AXIS_CONFIG = "RangeAxisConfig";
	private static final String VALUE_SOURCE_CONFIG = "ValueSourceConfig";
	private static final String COLOR_DIMENSION_CONFIG = "ColorDimensionConfig";
	private static final String SIZE_DIMENSION_CONFIG = "SizeDimensionConfig";
	private static final String SHAPE_DIMENSION_CONFIG = "ShapeDimensionConfig";
	private static final String DOMAIN_DIMENSION = "DomainDimension";

	private DataTable dataTable;

	private final JFreeChartPlotEngine plotEngine;
	private final LinkAndBrushChartPanel chartPanel;

	private JList<DataTableColumn> attributeList = new JList<>();

	private ExtendedHTMLJEditorPane statusTextArea;

	private PlotConfigurationTree plotConfigurationTree;

	private JPanel configurationContainerPanel;

	private final boolean smallIcons;

	private boolean allowCollapse = true;

	private DataTableColumnListTransferHandler attributeListTransferHandler;

	private JPopupMenu rangeAxisPopupMenu = new JPopupMenu("Remove Axis");

	private JPopupMenu valueSourcePopupMenu;

	private JPopupMenu dimensionConfigPopupMenu;

	private static final Color DROP_BORDER_COLOR = ProcessDrawer.BORDER_DRAG_COLOR;

	private static final Border ONGOING_DROP_BORDER = BorderFactory.createLineBorder(DROP_BORDER_COLOR, 2);

	private static final Border DROP_ENDED_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);

	private JScrollPane plotConfigurationTreeScrollPane;

	private PlotConfigurationTreeTransferHandler plotConfigurationTreeTransferHandler;

	private JPanel leftSideConfigPanel;
	private JPanel leftSideShowHiddenPanel;

	private JScrollPane chartPanelScrollPane;

	private JScrollPane statusTextAreaScrollPane;

	private MasterOfDesaster masterOfDesaster;

	// private DimensionDialog dialog;

	private JButton resetButton;

	private JMenuItem removeRangeAxisMenuItem;

	private JMenuItem removeAttributeFromDimensionMenuItem;

	private transient DataTable cachedDePivotedDataTable;

	private JComboBox<DatasetTransformationType> datasetTransformationSelectionComboBox;

	private JButton configureDataSetTransformationButton;

	private PlotConfiguration dePivotedPlotConfig;

	private String exampleSetSource = null;

	public ChartConfigurationPanel(boolean smallIcons, PlotInstance plotInstance, DataTable dataTable,
			PlotConfiguration dePivotedPlotConfig, String exampleSetSource) {
		this(smallIcons, plotInstance, dataTable, dePivotedPlotConfig);
		this.exampleSetSource = exampleSetSource;
	}

	public ChartConfigurationPanel(boolean smallIcons, PlotInstance plotInstance, DataTable dataTable) {
		super(plotInstance);

		this.smallIcons = smallIcons;
		this.dataTable = dataTable;
		this.cachedDePivotedDataTable = null;

		this.plotEngine = new JFreeChartPlotEngine(plotInstance, true);

		chartPanel = plotEngine.getChartPanel();

		plotEngine.startInitializing();

		masterOfDesaster = plotInstance.getMasterOfDesaster();
		createComponents();
		registerAsPlotConfigurationListener();

		plotConfigurationTree.setSelectionPath(plotConfigurationTree.getPathForRow(0));

		masterOfDesaster.addListener(this);

		attributeListTransferHandler.addDragListener(this);
		plotConfigurationTreeTransferHandler.addDragListener(this);

		attributeList.requestFocusInWindow();
		plotConfigurationTree.expandAll();

		plotEngine.endInitializing();
	}

	public ChartConfigurationPanel(boolean smallIcons, PlotInstance plotInstance, DataTable dataTable,
			PlotConfiguration dePivotedPlotConfig) {
		this(smallIcons, plotInstance, dataTable);
		this.dePivotedPlotConfig = dePivotedPlotConfig;
	}

	/**
	 * Returns the de-pivoted data table. If grouping attributes is not <code>null</code> a now data
	 * table is created in any case. If grouping attributes is <code>null</code> the cached
	 * datatable, if available, is returned.
	 *
	 * @param selectedNominalToNumericAttributesList2
	 *
	 * @return <code>null</code> if creating the dePivoted datatable fails, a dePivoted datatable
	 *         otherwise.
	 */
	private DataTable getDePivotedDataTable(Collection<String> excludedNumericalAttributeList,
			Collection<String> selectedNominalToNumericAttributesList) {
		if (cachedDePivotedDataTable == null || selectedNominalToNumericAttributesList != null
				|| excludedNumericalAttributeList != null) {
			Vector<DataTableColumn> dataTableColumns = assembleDataTableColumnList(dataTable);
			List<String> selectedNumericAttributes = new ArrayList<>();
			for (DataTableColumn column : dataTableColumns) {
				if (column.isNumerical() && !excludedNumericalAttributeList.contains(column.getName())) {
					selectedNumericAttributes.add(column.getName());
				}
			}

			ExampleSet metaSet = DataTransformation.createDePivotizedExampleSet(
					DataTableExampleSetAdapter.createExampleSetFromDataTable(dataTable), selectedNumericAttributes,
					selectedNominalToNumericAttributesList);

			// in case of error or no attributes specified
			if (metaSet == null) {
				return null;
			}

			cachedDePivotedDataTable = new DataTableExampleSetAdapter(metaSet, null);
		}
		return cachedDePivotedDataTable;
	}

	@Override
	public void print(Graphics pg) {

		JPanel printPanel = new JPanel() {

			private static final long serialVersionUID = 7315234075649335574L;

			@Override
			public void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				plotEngine.getChartPanel().print(g2);
			}
		};
		Insets insets = plotEngine.getChartPanel().getInsets();
		int w = plotEngine.getChartPanel().getWidth() - insets.left - insets.right;
		int h = plotEngine.getChartPanel().getHeight() - insets.top - insets.bottom;
		printPanel.setSize(new Dimension(w, h));

		printPanel.print(pg);
	}

	private void createPopups() {

		// creat popup menus
		{

			rangeAxisPopupMenu = new JPopupMenu();
			removeRangeAxisMenuItem = new JMenuItem(
					I18N.getGUILabel("plotter.configuration_dialog.remove_axis_menu_item.label"));
			removeRangeAxisMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					removeRangeAxisAction();
				}

			});
			rangeAxisPopupMenu.add(removeRangeAxisMenuItem);

			valueSourcePopupMenu = new JPopupMenu();
			JMenuItem menuItem = new JMenuItem(
					I18N.getGUILabel("plotter.configuration_dialog.remove_source_menu_item.label"));
			menuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					removeValueSourceAction();
				}

			});
			valueSourcePopupMenu.add(menuItem);

			dimensionConfigPopupMenu = new JPopupMenu();
			removeAttributeFromDimensionMenuItem = new JMenuItem(
					I18N.getGUILabel("plotter.configuration_dialog.remove_attribute_menu_item.label"));
			removeAttributeFromDimensionMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					removeAttributeFromDimensionAction();
				}

			});
			dimensionConfigPopupMenu.add(removeAttributeFromDimensionMenuItem);

		}
	}

	/**
	 *
	 * Returns a new PlotInstance with a dePivoted data table of the original data table. If the
	 * transformation could not be achieved, <code>null</code> is returned. If no plot config for
	 * the new PlotInstance is provided a PlotConfiguration with an invalid domain dimension will be
	 * created.
	 *
	 * @param nominalToNumericalAttributeList
	 */
	private PlotInstance getNewDePivotedPlotInstance(PlotConfiguration newPlotConfig,
			Collection<String> excludedNumericalAttributeList, Collection<String> selectedNominalToNumericAttributesList) {
		PlotInstance plotInstance;
		DataTable transformed = getDePivotedDataTable(excludedNumericalAttributeList,
				selectedNominalToNumericAttributesList);

		if (transformed == null) {
			return null;
		}

		if (newPlotConfig == null) {
			newPlotConfig = new PlotConfiguration(new DataTableColumn("-Empty-", ValueType.INVALID));
		}

		plotInstance = new PlotInstance(newPlotConfig, transformed);
		return plotInstance;
	}

	private void createComponents() {

		// set gridbag layout
		this.setLayout(new GridBagLayout());

		createPopups();

		GridBagConstraints itemConstraint = new GridBagConstraints();

		leftSideConfigPanel = createLeftSidePanel();

		itemConstraint = new GridBagConstraints();
		itemConstraint.fill = GridBagConstraints.BOTH;
		itemConstraint.weightx = 0;
		itemConstraint.weighty = 1;
		itemConstraint.insets = new Insets(5, 10, 2, 2);

		this.add(leftSideConfigPanel, itemConstraint);

		leftSideShowHiddenPanel = new JPanel(new GridBagLayout());
		leftSideShowHiddenPanel.setBackground(Color.red);

		JButton showConfigButton = new JButton(
				new ResourceAction(smallIcons, "plotter.configuration_dialog.show_configuration") {

					private static final long serialVersionUID = 1L;

					@Override
					public void loggedActionPerformed(ActionEvent e) {
						leftSideShowHiddenPanel.setVisible(false);
						leftSideConfigPanel.setVisible(true);
						statusTextArea.setVisible(true);
						statusTextAreaScrollPane.setVisible(true);
						chartPanel.chartChanged(null);
					}
				});

		itemConstraint = new GridBagConstraints();
		itemConstraint.insets = STANDARD_INSETS;
		itemConstraint.anchor = GridBagConstraints.NORTH;
		itemConstraint.weightx = 0;
		itemConstraint.weighty = 0;
		itemConstraint.fill = GridBagConstraints.NONE;
		itemConstraint.gridwidth = GridBagConstraints.REMAINDER;

		leftSideShowHiddenPanel.add(showConfigButton, itemConstraint);

		itemConstraint = new GridBagConstraints();
		itemConstraint.anchor = GridBagConstraints.SOUTH;
		itemConstraint.weightx = 1;
		itemConstraint.weighty = 1;
		itemConstraint.fill = GridBagConstraints.BOTH;
		itemConstraint.gridwidth = GridBagConstraints.REMAINDER;

		leftSideShowHiddenPanel.add(new JPanel(), itemConstraint);

		itemConstraint = new GridBagConstraints();
		itemConstraint.fill = GridBagConstraints.BOTH;
		itemConstraint.insets = STANDARD_INSETS;
		itemConstraint.weightx = 0;
		itemConstraint.weighty = 1;

		leftSideShowHiddenPanel.setVisible(false);

		this.add(leftSideShowHiddenPanel, itemConstraint);

		JPanel rightSidePanel = createRightSidePanel();

		itemConstraint = new GridBagConstraints();
		itemConstraint.gridwidth = GridBagConstraints.REMAINDER; // end row
		itemConstraint.insets = STANDARD_INSETS;
		itemConstraint.weightx = 1;
		itemConstraint.weighty = 1;
		itemConstraint.fill = GridBagConstraints.BOTH;

		this.add(rightSidePanel, itemConstraint);

	}

	private JPanel createRightSidePanel() {
		GridBagConstraints itemConstraint;
		// create panel for right side
		JPanel rightSidePanel = new JPanel(new GridBagLayout());

		// add chart panel to box
		{
			chartPanelScrollPane = new JScrollPane(chartPanel);
			chartPanelScrollPane.setPreferredSize(PREFERRED_CHART_SIZE);
			chartPanelScrollPane.setMaximumSize(MAX_CHART_SIZE);
			chartPanelScrollPane.setMinimumSize(MIN_CHART_SIZE);
			chartPanelScrollPane.setBorder(null);
			chartPanelScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			chartPanelScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

			itemConstraint = new GridBagConstraints();
			itemConstraint.fill = GridBagConstraints.BOTH;
			itemConstraint.weightx = 1;
			itemConstraint.weighty = 0.5;
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
			itemConstraint.insets = STANDARD_INSETS;

			rightSidePanel.add(chartPanelScrollPane, itemConstraint);
		}

		// add status text field
		{

			statusTextArea = new ExtendedHTMLJEditorPane("", masterOfDesaster.toHtmlString());
			statusTextArea.installDefaultStylesheet();
			statusTextArea.setEditable(false);
			statusTextArea.setFocusable(false);
			statusTextArea.setDoubleBuffered(true);

			statusTextAreaScrollPane = new JScrollPane(statusTextArea);
			statusTextAreaScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			statusTextAreaScrollPane.setPreferredSize(new Dimension(PERFERRED_CHART_WIDTH, 100));
			statusTextAreaScrollPane.setMinimumSize(new Dimension(MIN_CHART_WIDTH, 70));
			statusTextAreaScrollPane.setMaximumSize(new Dimension(MAX_CHART_WIDTH, 120));
			statusTextAreaScrollPane.setWheelScrollingEnabled(true);

			itemConstraint = new GridBagConstraints();
			itemConstraint.fill = GridBagConstraints.BOTH;
			itemConstraint.weightx = 1;
			itemConstraint.weighty = 0.1;
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
			itemConstraint.insets = STANDARD_INSETS;

			rightSidePanel.add(statusTextAreaScrollPane, itemConstraint);

		}

		return rightSidePanel;
	}

	/**
	 * @return
	 */
	private JPanel createLeftSidePanel() {
		GridBagConstraints itemConstraint = new GridBagConstraints();
		Insets standardInsets = STANDARD_INSETS;

		JPanel leftSidePanel = new JPanel(new GridBagLayout());

		// add attribute selection combobox
		{

			JPanel datasetTranformationContainerPanel = new JPanel(new GridBagLayout());

			{
				ResourceLabel datasetTransformationLabel = new ResourceLabel("plotter.datasetTransformation");
				datasetTransformationLabel.setFont(datasetTransformationLabel.getFont().deriveFont(Font.BOLD));

				itemConstraint = new GridBagConstraints();
				itemConstraint.gridx = 0;
				itemConstraint.weightx = 0.0;
				itemConstraint.gridwidth = 1;
				itemConstraint.anchor = GridBagConstraints.WEST;
				itemConstraint.insets = new Insets(0, 0, 0, 5);

				datasetTranformationContainerPanel.add(datasetTransformationLabel, itemConstraint);

				datasetTransformationSelectionComboBox = new JComboBox<>(DatasetTransformationType.values());
				itemConstraint = new GridBagConstraints();
				itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
				itemConstraint.fill = GridBagConstraints.HORIZONTAL;
				itemConstraint.insets = standardInsets;

				datasetTransformationSelectionComboBox.addPopupMenuListener(new PopupMenuListener() {

					@Override
					public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
						return;
					}

					@Override
					public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
						if (datasetTransformationSelectionComboBox.getSelectedItem() != getCurrentTranformationType()) {

							DatasetTransformationType type = (DatasetTransformationType) datasetTransformationSelectionComboBox
									.getSelectedItem();
							if (!changeDatatableTransformationType(type, false)) {
								datasetTransformationSelectionComboBox.setSelectedItem(getCurrentTranformationType());
							}
							configureDataSetTransformationButton
									.setEnabled((DatasetTransformationType) datasetTransformationSelectionComboBox
											.getSelectedItem() != DatasetTransformationType.ORIGINAL);
						}
					}

					@Override
					public void popupMenuCanceled(PopupMenuEvent e) {
						return;
					}
				});
				datasetTransformationSelectionComboBox
						.setRenderer(new EnumComboBoxCellRenderer<>("plotter.DatasetTransformationType"));

				itemConstraint = new GridBagConstraints();
				itemConstraint.gridx = 1;
				itemConstraint.weightx = 1.0;
				itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
				itemConstraint.fill = GridBagConstraints.HORIZONTAL;

				datasetTranformationContainerPanel.add(datasetTransformationSelectionComboBox, itemConstraint);

				configureDataSetTransformationButton = new JButton(
						new ResourceAction(smallIcons, "plotter.configure_dataset_transformation") {

							private static final long serialVersionUID = 1L;

							@Override
							public void loggedActionPerformed(ActionEvent e) {
								DatasetTransformationType type = (DatasetTransformationType) datasetTransformationSelectionComboBox
										.getSelectedItem();
								changeDatatableTransformationType(type, true);
							}
						});

				itemConstraint = new GridBagConstraints();
				itemConstraint.gridx = 2;
				itemConstraint.weightx = 0.0;
				itemConstraint.gridwidth = GridBagConstraints.REMAINDER; // end row
				itemConstraint.fill = GridBagConstraints.NONE;

				configureDataSetTransformationButton.setEnabled(false);

				datasetTranformationContainerPanel.add(configureDataSetTransformationButton, itemConstraint);

			}

			itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 1.0;
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER; // end row
			itemConstraint.fill = GridBagConstraints.HORIZONTAL;

			leftSidePanel.add(datasetTranformationContainerPanel, itemConstraint);
		}

		JLabel attributeListLabel = new ResourceLabel("plotter.configuration_dialog.attribute_list");
		{
			Font oldFont = attributeListLabel.getFont();
			JPanel labelPanel = new JPanel();
			labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.LINE_AXIS));

			attributeListLabel
					.setFont(FontTools.getFont(oldFont.getFamily(), Font.BOLD, oldFont.getSize()));
			// add attribute list label
			labelPanel.add(attributeListLabel);
			labelPanel.add(new JLabel(" "));

			// add attribute drag from here label
			JLabel dragLabel = new ResourceLabel("plotter.configuration_dialog.drag_from_here");

			labelPanel.add(dragLabel);

			labelPanel.add(Box.createHorizontalGlue());

			itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 1.0;
			itemConstraint.weighty = 0;
			itemConstraint.fill = GridBagConstraints.HORIZONTAL;
			itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
			itemConstraint.insets = STANDARD_INSETS;

			leftSidePanel.add(labelPanel, itemConstraint);
		}

		// add panel with reset and hide buttons
		{
			JPanel buttonPanel = new JPanel(new GridBagLayout());

			// add reset configuration button
			{
				resetButton = new JButton(
						new ResourceAction(smallIcons, "plotter.configuration_dialog.reset_configuration") {

							private static final long serialVersionUID = 1L;

							@Override
							public void loggedActionPerformed(ActionEvent e) {
								if (ConfirmDialog.showConfirmDialogWithOptionalCheckbox(
										ApplicationFrame.getApplicationFrame(),
										"plotter.configuration_dialog.reset_configuration", ConfirmDialog.YES_NO_OPTION,
										null, ConfirmDialog.NO_OPTION, false) == ConfirmDialog.YES_OPTION) {
									getPlotConfiguration().resetToDefaults();
								}
							}
						});

				itemConstraint = new GridBagConstraints();
				itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
				itemConstraint.insets = STANDARD_INSETS;

				buttonPanel.add(resetButton, itemConstraint);

			}

			// add hide configuration button
			{
				JButton hideButton = new JButton(
						new ResourceAction(smallIcons, "plotter.configuration_dialog.hide_configuration") {

							private static final long serialVersionUID = 1L;

							@Override
							public void loggedActionPerformed(ActionEvent e) {
								boolean visible = !leftSideConfigPanel.isVisible();
								leftSideConfigPanel.setVisible(visible);
								statusTextArea.setVisible(visible);
								statusTextAreaScrollPane.setVisible(visible);
								leftSideShowHiddenPanel.setVisible(!visible);
								chartPanel.chartChanged(null);
							}
						});

				itemConstraint = new GridBagConstraints();
				itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
				itemConstraint.insets = STANDARD_INSETS;

				buttonPanel.add(hideButton, itemConstraint);

			}

			// actually add panel
			{
				itemConstraint = new GridBagConstraints();
				itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
				itemConstraint.insets = STANDARD_INSETS;

				leftSidePanel.add(buttonPanel, itemConstraint);
			}

		}

		// add attribute list
		{
			// assemble attributeVector
			Vector<DataTableColumn> attributeVector = assembleDataTableColumnList(dataTable);

			// add attribute list to panel
			attributeList = new JList<>(attributeVector);
			attributeListLabel.setLabelFor(attributeList);
			attributeList.setDragEnabled(true);
			attributeList.setCellRenderer(new DataTableColumnListCellRenderer());
			attributeListTransferHandler = new DataTableColumnListTransferHandler();
			attributeList.setTransferHandler(attributeListTransferHandler);
			attributeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

			JScrollPane attributeListScrollPane = new JScrollPane(attributeList);
			attributeListScrollPane.setPreferredSize(PREFFERED_LIST_AND_TREE_DIMENSION);
			attributeListScrollPane.setMaximumSize(MAX_LIST_AND_TREE_DIMENSION);
			attributeListScrollPane.setMinimumSize(MIN_LIST_AND_TREE_DIMENSION);
			attributeListScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			attributeListScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

			itemConstraint = new GridBagConstraints();
			itemConstraint.fill = GridBagConstraints.BOTH;
			itemConstraint.weightx = 1;
			itemConstraint.weighty = 0.5;
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
			itemConstraint.insets = standardInsets;

			leftSidePanel.add(attributeListScrollPane, itemConstraint);
		}

		// add configarution tree label
		JLabel plotConfigurationLabel = new ResourceLabel("plotter.configuration_dialog.chart_configuration");
		{
			Font oldFont = plotConfigurationLabel.getFont();
			JPanel labelPanel = new JPanel();
			labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.LINE_AXIS));

			plotConfigurationLabel
					.setFont(FontTools.getFont(oldFont.getFamily(), Font.BOLD, oldFont.getSize()));
			// add attribute list label
			labelPanel.add(plotConfigurationLabel);

			labelPanel.add(new JLabel(" "));

			// add attribute drag from here label
			JLabel dropLabel = new ResourceLabel("plotter.configuration_dialog.drop_here");

			labelPanel.add(dropLabel);

			labelPanel.add(Box.createHorizontalGlue());

			itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 1.0;
			itemConstraint.weighty = 0;
			itemConstraint.fill = GridBagConstraints.HORIZONTAL;
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
			itemConstraint.insets = STANDARD_INSETS;

			leftSidePanel.add(labelPanel, itemConstraint);
		}

		// add plot configuration tree
		{

			// create tree
			plotConfigurationTree = new PlotConfigurationTree(getPlotConfiguration(), dataTable,
					attributeListTransferHandler);
			plotConfigurationTree.addTreeSelectionListener(new TreeSelectionListener() {

				@Override
				public void valueChanged(TreeSelectionEvent e) {
					TreePath newLeadSelectionPath = e.getNewLeadSelectionPath();
					if (newLeadSelectionPath == null) {
						CardLayout cl = (CardLayout) configurationContainerPanel.getLayout();
						cl.show(configurationContainerPanel, EMPTY);
						return;
					}
					Object lastPathComponent = newLeadSelectionPath.getLastPathComponent();
					if (lastPathComponent instanceof PlotConfigurationTreeNode) {
						CardLayout cl = (CardLayout) configurationContainerPanel.getLayout();
						cl.show(configurationContainerPanel, GLOBAL_CONFIG);
					} else if (lastPathComponent instanceof RangeAxisConfigTreeNode) {
						CardLayout cl = (CardLayout) configurationContainerPanel.getLayout();
						cl.show(configurationContainerPanel, RANGE_AXIS_CONFIG);
					} else if (lastPathComponent instanceof ValueSourceTreeNode) {
						CardLayout cl = (CardLayout) configurationContainerPanel.getLayout();
						cl.show(configurationContainerPanel, VALUE_SOURCE_CONFIG);
					} else if (lastPathComponent instanceof DimensionConfigTreeNode) {
						CardLayout cl = (CardLayout) configurationContainerPanel.getLayout();
						switch (((DimensionConfigTreeNode) lastPathComponent).getDimension()) {
							case COLOR:
								cl.show(configurationContainerPanel, COLOR_DIMENSION_CONFIG);
								break;
							case SHAPE:
								cl.show(configurationContainerPanel, SHAPE_DIMENSION_CONFIG);
								break;
							case DOMAIN:
								cl.show(configurationContainerPanel, DOMAIN_DIMENSION);
								break;
							case SIZE:
								cl.show(configurationContainerPanel, SIZE_DIMENSION_CONFIG);
								break;
							default:
								throw new RuntimeException("Unsupported Dimension Config.. this should not happen");

						}

					}
					// else {
					// throw new RuntimeException("Unknown TreeNode type. This should not happen");
					// }
				}
			});

			MouseAdapter ma = new MouseAdapter() {

				private void myPopupEvent(MouseEvent e) {
					int x = e.getX();
					int y = e.getY();
					JTree tree = (JTree) e.getSource();
					TreePath path = tree.getPathForLocation(x, y);
					if (path == null) {
						return;
					}

					tree.setSelectionPath(path);

					TreeNode node = (TreeNode) path.getLastPathComponent();

					if (node instanceof RangeAxisConfigTreeNode) {
						PlotConfiguration plotConfig = (PlotConfiguration) ((DefaultMutableTreeNode) tree.getModel()
								.getRoot()).getUserObject();
						removeRangeAxisMenuItem.setEnabled(plotConfig.getRangeAxisCount() > 1);
						rangeAxisPopupMenu.show(tree, x, y);
					} else if (node instanceof ValueSourceTreeNode) {
						valueSourcePopupMenu.show(tree, x, y);
					} else if (node instanceof DimensionConfigTreeNode) {
						// only enable remove attribute from dimension action when there is actually
						// one there
						DimensionConfig config = ((DimensionConfigTreeNode) node).getUserObject();
						if (config != null) {
							if (config.getDimension() == PlotDimension.DOMAIN && getPlotConfiguration()
									.getDomainConfigManager().getDataTableColumn().getValueType() == ValueType.INVALID) {
								removeAttributeFromDimensionMenuItem.setEnabled(false);
							} else {
								removeAttributeFromDimensionMenuItem.setEnabled(true);
							}
						} else {
							removeAttributeFromDimensionMenuItem.setEnabled(false);
						}
						dimensionConfigPopupMenu.show(tree, x, y);
					}
				}

				@Override
				public void mousePressed(MouseEvent e) {
					if (e.isPopupTrigger()) {
						myPopupEvent(e);
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					if (e.isPopupTrigger()) {
						myPopupEvent(e);
					}
				}
			};

			plotConfigurationTree.addMouseListener(ma);
			plotConfigurationTree.addKeyListener(new KeyListener() {

				@Override
				public void keyTyped(KeyEvent e) {
					return; // Nothing to be done
				}

				@Override
				public void keyReleased(KeyEvent e) {
					return; // Nothing to be done
				}

				@Override
				public void keyPressed(KeyEvent e) {
					int key = e.getKeyCode();
					if (key == KeyEvent.VK_DELETE) {
						JTree tree = (JTree) e.getSource();
						TreePath selectionPath = plotConfigurationTree.getSelectionPath();
						if (selectionPath != null) {

							Object node = selectionPath.getLastPathComponent();
							if (node instanceof RangeAxisConfigTreeNode) {
								PlotConfiguration plotConfig = (PlotConfiguration) ((DefaultMutableTreeNode) tree.getModel()
										.getRoot()).getUserObject();
								if (plotConfig.getRangeAxisCount() > 1) {
									removeRangeAxisAction();
								}
							} else if (node instanceof ValueSourceTreeNode) {
								removeValueSourceAction();
							} else if (node instanceof DimensionConfigTreeNode) {
								removeAttributeFromDimensionAction();
							}
						}
						e.consume();
					}
				}
			});

			plotConfigurationTree.addTreeWillExpandListener(new TreeWillExpandListener() {

				@Override
				public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
					return; // Nothing to be done
				}

				@Override
				public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
					// this stops the tree from collapsing
					if (!allowCollapse) {
						throw new ExpandVetoException(event);
					}
				}
			});

			// create Transferhandler
			plotConfigurationTreeTransferHandler = new PlotConfigurationTreeTransferHandler(plotConfigurationTree);
			plotConfigurationTree.setTransferHandler(plotConfigurationTreeTransferHandler);

			plotConfigurationTreeScrollPane = new JScrollPane(plotConfigurationTree);
			plotConfigurationLabel.setLabelFor(plotConfigurationTree);

			plotConfigurationTreeScrollPane.setPreferredSize(PREFFERED_LIST_AND_TREE_DIMENSION);
			plotConfigurationTreeScrollPane.setMaximumSize(MAX_LIST_AND_TREE_DIMENSION);
			plotConfigurationTreeScrollPane.setMinimumSize(MIN_LIST_AND_TREE_DIMENSION);
			plotConfigurationTreeScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			plotConfigurationTreeScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			plotConfigurationTreeScrollPane.setBorder(DROP_ENDED_BORDER);

			itemConstraint = new GridBagConstraints();
			itemConstraint.fill = GridBagConstraints.BOTH;
			itemConstraint.weightx = 1;
			itemConstraint.weighty = 1;
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
			itemConstraint.insets = standardInsets;

			leftSidePanel.add(plotConfigurationTreeScrollPane, itemConstraint);
		}

		// add container for configuration
		{

			configurationContainerPanel = new JPanel(new CardLayout());

			// add global config panel
			{
				JPanel globalConfigPanel = new GlobalConfigurationPanel(getCurrentPlotInstance());
				addPlotInstanceChangeListener((PlotInstanceChangedListener) globalConfigPanel);
				configurationContainerPanel.add(globalConfigPanel, GLOBAL_CONFIG);

			}

			// add range axis config panel
			{
				JPanel rangeAxisPanel = new RangeAxisConfigPanel(plotConfigurationTree, getCurrentPlotInstance());
				addPlotInstanceChangeListener((PlotInstanceChangedListener) rangeAxisPanel);
				configurationContainerPanel.add(rangeAxisPanel, RANGE_AXIS_CONFIG);

			}

			// add plot axes config panel
			{
				JPanel plotAxesConfigPanel = new ValueSourceConfigurationPanel(smallIcons, plotConfigurationTree,
						attributeListTransferHandler, getCurrentPlotInstance());
				addPlotInstanceChangeListener((PlotInstanceChangedListener) plotAxesConfigPanel);
				configurationContainerPanel.add(plotAxesConfigPanel, VALUE_SOURCE_CONFIG);
			}

			// add domain dimension config panel
			{
				JPanel domainDimensionPanel = new DimensionConfigPanel(PlotDimension.DOMAIN, plotConfigurationTree,
						getCurrentPlotInstance());
				addPlotInstanceChangeListener((PlotInstanceChangedListener) domainDimensionPanel);
				configurationContainerPanel.add(domainDimensionPanel, DOMAIN_DIMENSION);
			}

			// add empty panel
			{
				configurationContainerPanel.add(new JPanel(), EMPTY);
			}

			// add color dimension config panel
			{
				JPanel dimensionConfigPanel = new DimensionConfigPanel(PlotDimension.COLOR, plotConfigurationTree,
						getCurrentPlotInstance());
				addPlotInstanceChangeListener((PlotInstanceChangedListener) dimensionConfigPanel);
				configurationContainerPanel.add(dimensionConfigPanel, COLOR_DIMENSION_CONFIG);
			}

			// add shape dimension config panel
			{
				JPanel dimensionConfigPanel = new DimensionConfigPanel(PlotDimension.SHAPE, plotConfigurationTree,
						getCurrentPlotInstance());
				addPlotInstanceChangeListener((PlotInstanceChangedListener) dimensionConfigPanel);
				configurationContainerPanel.add(dimensionConfigPanel, SHAPE_DIMENSION_CONFIG);
			}

			// add size dimension config panel
			{
				JPanel dimensionConfigPanel = new DimensionConfigPanel(PlotDimension.SIZE, plotConfigurationTree,
						getCurrentPlotInstance());
				addPlotInstanceChangeListener((PlotInstanceChangedListener) dimensionConfigPanel);
				configurationContainerPanel.add(dimensionConfigPanel, SIZE_DIMENSION_CONFIG);
			}

			CardLayout cl = (CardLayout) configurationContainerPanel.getLayout();
			cl.show(configurationContainerPanel, EMPTY);

			itemConstraint = new GridBagConstraints();
			itemConstraint.fill = GridBagConstraints.BOTH;
			itemConstraint.insets = STANDARD_INSETS;
			itemConstraint.weightx = 1;
			itemConstraint.weighty = 0;
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER;

			leftSidePanel.add(configurationContainerPanel, itemConstraint);

		}

		return leftSidePanel;

	}

	private void removeRangeAxisAction() {
		RangeAxisConfigTreeNode node = (RangeAxisConfigTreeNode) plotConfigurationTree.getSelectionPath()
				.getLastPathComponent();

		RangeAxisConfig axisConfig = node.getUserObject();
		getPlotConfiguration().removeRangeAxisConfig(axisConfig);
	}

	private void removeValueSourceAction() {
		ValueSourceTreeNode node = (ValueSourceTreeNode) plotConfigurationTree.getSelectionPath().getLastPathComponent();
		ValueSource source = node.getUserObject();
		RangeAxisConfigTreeNode parent = (RangeAxisConfigTreeNode) node.getParent();
		if (parent == null) {
			new RuntimeErrorException(null, "ValueSource Node has no parent!!").printStackTrace();
		} else {
			RangeAxisConfig rangeAxis = parent.getUserObject();
			rangeAxis.removeValueSource(source);
		}
	}

	private void removeAttributeFromDimensionAction() {
		DimensionConfigTreeNode node = (DimensionConfigTreeNode) plotConfigurationTree.getSelectionPath()
				.getLastPathComponent();
		DimensionConfig config = node.getUserObject();
		if (config != null) {
			if (config.getDimension() != PlotDimension.DOMAIN) {
				getPlotConfiguration().setDimensionConfig(config.getDimension(), null);
			} else {
				getPlotConfiguration().getDomainConfigManager()
						.setDataTableColumn(new DataTableColumn(null, ValueType.INVALID));
			}
		}
	}

	/**
	 * Inspects the {@link DataTable} and assembles a model for the attribute selection list.
	 */
	private Vector<DataTableColumn> assembleDataTableColumnList(DataTable dataTable) {
		Vector<DataTableColumn> dataTableColumnVector = new Vector<DataTableColumn>();
		int numberOfColumns = dataTable.getNumberOfColumns();
		for (int i = 0; i < numberOfColumns; ++i) {
			dataTableColumnVector.add(new DataTableColumn(dataTable, i));
		}
		return dataTableColumnVector;
	}

	private boolean changeDatatableTransformationType(DatasetTransformationType type, boolean reconfigure) {
		PlotInstance plotInstance = getPlotInstance(type);
		if (plotInstance == null || reconfigure) {
			if (type == DatasetTransformationType.DE_PIVOTED) {

				List<DataTableColumn> columns = new LinkedList<DataTableColumn>();

				// fetch nominal columns
				Vector<DataTableColumn> dataTableColumns = assembleDataTableColumnList(dataTable);
				for (DataTableColumn column : dataTableColumns) {
					if ((column.isNominal() || column.isNumerical()) && !column.getName().equals("id")
							&& !column.getName().equals("value") && !column.getName().equals("attribute")) {
						columns.add(column);
					}
				}
				Collection<String> selectedAttributeExclusionList = null;

				// show grouping attributes configuration dialog if nominal attributes have been
				// found
				AttributeSelectionDialog dialog = new AttributeSelectionDialog(columns);
				dialog.setVisible(true);
				if (dialog.wasConfirmed()) {
					selectedAttributeExclusionList = dialog.getSelectedAttributeNames();
				} else {
					return false;
				}

				Collection<String> nominalToNumericalAttributeList = new LinkedList<String>();
				Collection<String> excludedNumericalAttributesList = new LinkedList<String>();
				if (selectedAttributeExclusionList != null) {
					for (DataTableColumn column : dataTableColumns) {
						String name = column.getName();
						if (selectedAttributeExclusionList.contains(name)) {
							if (column.isNumerical()) {
								excludedNumericalAttributesList.add(name);
							}
						} else {
							if (column.isNominal()) {
								nominalToNumericalAttributeList.add(name);
							}
						}
					}
				}

				// if a plotInstance already exists and the data should be reconfigured, save
				// current PlotConfig
				PlotConfiguration currentPlotConfiguration = null;
				if (plotInstance != null) {
					currentPlotConfiguration = plotInstance.getMasterPlotConfiguration();
				} else {
					currentPlotConfiguration = dePivotedPlotConfig;
				}

				plotInstance = getNewDePivotedPlotInstance(currentPlotConfiguration, excludedNumericalAttributesList,
						nominalToNumericalAttributeList);

				if (plotInstance == null) {
					SwingTools.showVerySimpleErrorMessage("plotter.no_datatable_for_transformation_type", type);
					return false;
				}
			} else {
				SwingTools.showVerySimpleErrorMessage("plotter.no_datatable_for_transformation_type", type);
				return false;
			}
		}

		plotInstanceChanged(null, plotInstance, type);
		return true;
	}

	@Override
	public void masterOfDesasterChanged(MasterOfDesaster masterOfDesaster) {
		final String htmlString = masterOfDesaster.toHtmlString();

		if (statusTextArea != null) {
			// String oldHtmlString = statusTextArea.getText();
			// if (!(htmlString.contains("ok.png") && oldHtmlString.contains("ok.png"))) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {

					statusTextArea.setText(htmlString);
					statusTextArea.setCaretPosition(0);
					statusTextAreaScrollPane.scrollRectToVisible(new Rectangle(1, 1, 1, 1));
				}
			});
			// }
		}
	}

	@Override
	public void dragStarted(Transferable t) {
		TransferSupport support = new TransferSupport(this, t);
		boolean doesSupportFlavor = ((PlotConfigurationTreeTransferHandler) plotConfigurationTree.getTransferHandler())
				.doesSupportFlavor(support);

		if (doesSupportFlavor) {
			if (SwingUtilities.isEventDispatchThread()) {
				switch (RapidMinerGUI.getDragHighlighteMode()) {
					case FULL:
						plotConfigurationTree.setBackground(ProcessDrawer.INNER_DRAG_COLOR);
					case BORDER:
						plotConfigurationTreeScrollPane.setBorder(ONGOING_DROP_BORDER);
						break;
					default:
						break;

				}
			} else {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						switch (RapidMinerGUI.getDragHighlighteMode()) {
							case FULL:
								plotConfigurationTree.setBackground(ProcessDrawer.INNER_DRAG_COLOR);
							case BORDER:
								plotConfigurationTreeScrollPane.setBorder(ONGOING_DROP_BORDER);
								break;
							default:
								break;

						}
					}
				});
			}
		}
	}

	@Override
	public void dragEnded() {
		if (SwingUtilities.isEventDispatchThread()) {
			plotConfigurationTreeScrollPane.setBorder(DROP_ENDED_BORDER);
			plotConfigurationTree.setBackground(Color.WHITE);
		} else {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					plotConfigurationTreeScrollPane.setBorder(DROP_ENDED_BORDER);
					plotConfigurationTree.setBackground(Color.WHITE);
				}
			});
		}

	}

	@Override
	protected void adaptGUI() {
		plotConfigurationTree.treeDidChange();
	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		if (plotConfigurationTree == null) {
			return true;
		}
		switch (change.getType()) {
			case DIMENSION_CONFIG_ADDED:
				adaptGUI();
				break;
			case DIMENSION_CONFIG_CHANGED:
				adaptGUI();
				break;
			case DIMENSION_CONFIG_REMOVED:
				adaptGUI();
				break;
			case RANGE_AXIS_CONFIG_ADDED:
				adaptGUI();
				break;
			case RANGE_AXIS_CONFIG_CHANGED:
				adaptGUI();
				break;
			case RANGE_AXIS_CONFIG_REMOVED:
				adaptGUI();
				break;
			case META_CHANGE:
				adaptGUI();
				break;
			default:
		}
		return true;
	}

	@Override
	public void startProcessing() {
		resetButton.setEnabled(false);
	}

	@Override
	public void endProcessing() {
		resetButton.setEnabled(true);
	}

	@Override
	public void plotInstanceChanged(PlotInstance oldPlotInstance, PlotInstance newPlotInstance,
			DatasetTransformationType newType) {

		// remove old listeners
		unregisterAsPlotConfigurationListener();
		masterOfDesaster.removeListener(this);

		// change to new PlotInstance
		setPlotInstance(newPlotInstance, newType);

		// register as listener
		masterOfDesaster = getCurrentPlotInstance().getMasterOfDesaster();
		masterOfDesaster.addListener(this);
		registerAsPlotConfigurationListener();

		// get new attribute list
		Vector<DataTableColumn> columnList = new Vector<DataTableColumn>();
		switch (newType) {
			case DE_PIVOTED:
				columnList = assembleDataTableColumnList(getDePivotedDataTable(null, null));
				break;
			case ORIGINAL:
			default:
				columnList = assembleDataTableColumnList(dataTable);
		}

		// adapt attribute list to selection
		DefaultListModel<DataTableColumn> attributeListModel = new DefaultListModel<>();
		for (DataTableColumn column : columnList) {
			attributeListModel.addElement(column);
		}
		attributeList.setModel(attributeListModel);

		// adapt config tree to selection
		PlotConfigurationTreeModel newModel = (PlotConfigurationTreeModel) plotConfigurationTree.getModel();
		newModel.exchangePlotConfiguration(getPlotConfiguration());
		plotConfigurationTree.expandAll();

		// tell plotEngine that PlotInstance has changed
		plotEngine.setPlotInstance(newPlotInstance);
	}

	public JFreeChartPlotEngine getPlotEngine() {
		return plotEngine;
	}

	@Override
	public Component getExportComponent() {
		// create new LinkAndBrushChartPanel with double buffering set to false to get vector
		// graphic export
		// The real chart has to use double buffering for a) performance and b) zoom rectangle
		// drawing
		LinkAndBrushChartPanel newLaBPanel = new LinkAndBrushChartPanel(getPlotEngine().getChartPanel().getChart(),
				getPlotEngine().getChartPanel().getWidth(), getPlotEngine().getChartPanel().getHeight(),
				getPlotEngine().getChartPanel().getMinimumDrawWidth(),
				getPlotEngine().getChartPanel().getMinimumDrawHeight(), false, false);
		newLaBPanel.setSize(getPlotEngine().getChartPanel().getSize());
		newLaBPanel.setOverlayList(getPlotEngine().getChartPanel().getOverlayList());
		return newLaBPanel;
	}

	@Override
	public String getExportIconName() {
		return I18N.getGUIMessage("gui.cards.result_view.advanced_charts.icon");
	}

	@Override
	public String getExportName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.cards.result_view.advanced_charts.title");
	}

	@Override
	public String getIdentifier() {
		return exampleSetSource;
	}
}
