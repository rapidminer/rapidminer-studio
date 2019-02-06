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
package com.rapidminer.gui.new_plotter.gui.cellrenderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.Transferable;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.RapidMinerGUI.DragHighlightMode;
import com.rapidminer.gui.dnd.DragListener;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.VisualizationType;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.gui.dnd.DataTableColumnListTransferHandler;
import com.rapidminer.gui.new_plotter.gui.treenodes.DimensionConfigTreeNode;
import com.rapidminer.gui.new_plotter.gui.treenodes.PlotConfigurationTreeNode;
import com.rapidminer.gui.new_plotter.gui.treenodes.RangeAxisConfigTreeNode;
import com.rapidminer.gui.new_plotter.gui.treenodes.ValueSourceTreeNode;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;


/**
 * A renderer for plot configuration tree cells that displays the configuration's icon and name.
 *
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotConfigurationTreeCellRenderer extends DefaultTreeCellRenderer implements DragListener {

	private final Font TREE_FONT = UIManager.getFont("Tree.font");

	private final Color SELECTED_COLOR = UIManager.getColor("Tree.selectionBackground");

	private final Color NOT_SELECTED_COLOR = Colors.WHITE;

	private final Color TEXT_SELECTED_COLOR = UIManager.getColor("Tree.selectionForeground");

	private final Color TEXT_NON_SELECTED_COLOR = UIManager.getColor("Tree.textForeground");

	private final Color BORDER_SELECTION_COLOR = UIManager.getColor("Tree.selectionBorderColor").darker();

	private Icon ERROR_ICON;
	private Icon WARNING_ICON;

	private Border focusBorder, nonFocusBorder;

	/**
	 * The panel which will be used for the actual rendering of {@link DimensionConfigTreeNode}s and
	 * {@link RangeAxisConfigTreeNode}s.
	 */
	private class DimensionAndRangeAxisTreeCellPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private JLabel nameLabel;
		private JLabel attributeLabel;
		private JLabel errorIconLabel;

		private final int CELL_WIDTH = 355;
		private final int CELL_HEIGTH = 18;

		public DimensionAndRangeAxisTreeCellPanel() {
			this.setLayout(new BorderLayout());
			this.setPreferredSize(new Dimension(CELL_WIDTH, CELL_HEIGTH));

			nameLabel = new JLabel("");
			nameLabel.setFont(TREE_FONT);
			nameLabel.setPreferredSize(new Dimension(140, CELL_HEIGTH));

			this.add(nameLabel, BorderLayout.WEST);

			attributeLabel = new JLabel("");
			attributeLabel.setFont(TREE_FONT);
			attributeLabel.setHorizontalAlignment(SwingConstants.LEFT);
			attributeLabel.setPreferredSize(new Dimension(155, CELL_HEIGTH));

			this.add(attributeLabel, BorderLayout.CENTER);

			errorIconLabel = new JLabel("");
			this.add(errorIconLabel, BorderLayout.EAST);
		}

		public void updateTreeCell(JTree tree, RangeAxisConfigTreeNode value, boolean selected, boolean expanded,
				boolean leaf, int row, boolean hasFocus, boolean dragging) {
			adaptRangeAxisCell(tree, value, selected, expanded, leaf, row, hasFocus, dragging);
		}

		public void updateTreeCell(JTree tree, DimensionConfigTreeNode value, boolean selected, boolean expanded,
				boolean leaf, int row, boolean hasFocus, boolean dragging) {
			adaptDimensionCell(tree, value, selected, expanded, leaf, row, hasFocus, dragging);
		}

		private void adaptContainerStyle(JTree tree, Object node, boolean selected, boolean expanded, boolean leaf, int row,
				boolean hasFocus, boolean dragging, boolean setForeground) {
			SwingTools.setEnabledRecursive(this, tree.isEnabled());

			Color fg = null;
			Color bg = null;

			boolean highlightingEnabled = RapidMinerGUI.getDragHighlighteMode().equals(DragHighlightMode.FULL);

			JTree.DropLocation dropLocation = tree.getDropLocation();
			if (dropLocation != null && dropLocation.getChildIndex() == -1
					&& tree.getRowForPath(dropLocation.getPath()) == row) {
				fg = TEXT_SELECTED_COLOR;
				bg = SELECTED_COLOR;
			} else if (selected && !dragging) {
				fg = TEXT_SELECTED_COLOR;
				bg = SELECTED_COLOR;
			} else {
				fg = TEXT_NON_SELECTED_COLOR;
				if (highlightingEnabled && dragging) {
					bg = ProcessDrawer.INNER_DRAG_COLOR;
				} else {
					bg = NOT_SELECTED_COLOR;
				}
			}

			if (setForeground) {
				nameLabel.setForeground(fg);
				attributeLabel.setForeground(fg);
			}
			this.setBackground(bg);

			if (hasFocus) {
				this.setBorder(focusBorder);
			} else {
				if (highlightingEnabled && dragging) {
					this.setBorder(draggingNotFocusedBorder);
				} else {
					this.setBorder(nonFocusBorder);
				}
			}
		}

		private void adaptRangeAxisCell(JTree tree, RangeAxisConfigTreeNode node, boolean selected, boolean expanded,
				boolean leaf, int row, boolean hasFocus, boolean dragging) {

			String i18nKey = "plotter.configuration_dialog.plot_dimension.range_axis";

			// set label font
			Font fontValue = TREE_FONT;
			if (fontValue == null) {
				fontValue = nameLabel.getFont();
			}
			fontValue = FontTools.getFont(fontValue.getFamily(), Font.PLAIN, fontValue.getSize());
			nameLabel.setFont(fontValue);

			// set label icon
			String icon = I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.label." + i18nKey + ".icon");
			if (icon != null) {
				ImageIcon iicon = SwingTools.createIcon("16/" + icon);
				nameLabel.setIcon(iicon);
			}

			// set label text
			RangeAxisConfig rangeAxis = node.getUserObject();
			ValueType type = rangeAxis.getValueType();

			Color foregroundColor = getValueTypeColor(type);
			String valueText = "";
			switch (type) {
				case DATE_TIME:
					valueText = I18N.getGUILabel("plotter.configuration_dialog.plot_dimension.date_range_axis.label") + " ";
					break;
				case INVALID:
					valueText = I18N.getGUILabel("plotter.configuration_dialog.plot_dimension.invalid_range_axis.label")
							+ " ";
					break;
				case NOMINAL:
					valueText = I18N.getGUILabel("plotter.configuration_dialog.plot_dimension.nominal_range_axis.label")
							+ " ";
					break;
				case NUMERICAL:
					valueText = I18N.getGUILabel("plotter.configuration_dialog.plot_dimension.numerical_range_axis.label")
							+ " ";
					break;
				case UNKNOWN:
					valueText = I18N.getGUILabel("plotter.configuration_dialog.plot_dimension.empty_range_axis.label") + " ";
					break;
			}

			Icon errorLabelIcon = null;

			if (foregroundColor != null && foregroundColor.equals(TreeNodeColors.getWarningColor())) {
				errorLabelIcon = WARNING_ICON;
			}

			if (!rangeAxis.getWarnings().isEmpty()) {
				foregroundColor = TreeNodeColors.getWarningColor();
				errorLabelIcon = WARNING_ICON;
			}

			if (!rangeAxis.getErrors().isEmpty()) {
				foregroundColor = TreeNodeColors.getInvalidColor();
				valueText = I18N.getGUILabel("plotter.configuration_dialog.plot_dimension.invalid_range_axis.label") + " ";
				errorLabelIcon = ERROR_ICON;
			}

			errorIconLabel.setIcon(errorLabelIcon);

			valueText += I18N.getMessage(I18N.getGUIBundle(), "gui.label." + i18nKey + ".label") + ":";
			nameLabel.setText(valueText);

			// set attribute label text
			String label = rangeAxis.getLabel();
			if (label == null) {
				label = I18N.getGUILabel("plotter.unnamed_value_label");
			}
			attributeLabel.setText(label);

			// show attribute label
			attributeLabel.setVisible(true);

			boolean foregroundSet = foregroundColor != null;
			if (foregroundSet) {
				nameLabel.setForeground(foregroundColor);
				attributeLabel.setForeground(foregroundColor);
			}

			adaptContainerStyle(tree, node, selected, expanded, leaf, row, hasFocus, dragging, !foregroundSet);
		}

		private void adaptDimensionCell(JTree tree, DimensionConfigTreeNode node, boolean selected, boolean expanded,
				boolean leaf, int row, boolean hasFocus, boolean dragging) {

			String i18nKey = null;
			switch (node.getDimension()) {
				case COLOR:
					i18nKey = "plotter.configuration_dialog.plot_dimension.color";
					break;
				case DOMAIN:
					i18nKey = "plotter.configuration_dialog.plot_dimension.domain";
					break;
				case SHAPE:
					i18nKey = "plotter.configuration_dialog.plot_dimension.shape";
					break;
				case SIZE:
					i18nKey = "plotter.configuration_dialog.plot_dimension.size";
					break;
				default:
			}

			// set label font
			Font fontValue = TREE_FONT;
			if (fontValue == null) {
				fontValue = nameLabel.getFont();
			}
			fontValue = FontTools.getFont(fontValue.getFamily(), Font.PLAIN, fontValue.getSize());
			nameLabel.setFont(fontValue);

			// set label icon
			String icon = I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.label." + i18nKey + ".icon");
			if (icon != null) {
				ImageIcon iicon = SwingTools.createIcon("16/" + icon);
				nameLabel.setIcon(iicon);
			}

			// get name label text
			PlotDimension dimension = node.getDimension();
			String valueText = dimension.getName() + ": ";

			// set name label
			nameLabel.setText(valueText);
			DimensionConfig dimensionConfig = node.getUserObject();

			// set attribute text and icons
			String emptyLabel = I18N.getMessage(I18N.getGUIBundle(),
					"gui.label.plotter.configuration_dialog.empty_dimension.label");
			if (dimensionConfig != null) {
				valueText = dimensionConfig.getLabel();
				DataTableColumn dataTableColumn = dimensionConfig.getDataTableColumn();
				String name = dataTableColumn.getName();
				if (valueText == null) {
					if (dimensionConfig.getDimension() == PlotDimension.DOMAIN && (name == null || name.isEmpty())) {
						valueText = emptyLabel;
					} else {
						valueText = I18N.getGUILabel("plotter.unnamed_value_label");
						if (!dimensionConfig.isAutoNaming()) {
							valueText += " [";
							valueText += name;
							valueText += "]";
						}

					}
				} else {
					if (!dimensionConfig.isAutoNaming()) {
						valueText += " [";
						valueText += name;
						valueText += "]";
					}
				}
			} else {
				valueText = emptyLabel;
			}

			attributeLabel.setIcon(null);
			attributeLabel.setText(valueText);

			// show attribute label
			attributeLabel.setVisible(true);

			Color foregroundColor = null;

			Icon errorLabelIcon = null;

			if (dimensionConfig != null) {
				ValueType valueType = dimensionConfig.getValueType();

				if (!(valueType == ValueType.UNKNOWN && dimensionConfig.getDimension() == PlotDimension.DOMAIN)) {
					foregroundColor = getValueTypeColor(valueType);
				}

				PlotConfiguration plotConfig = (PlotConfiguration) ((DefaultMutableTreeNode) tree.getModel().getRoot())
						.getUserObject();

				if (!dimensionConfig.getWarnings().isEmpty() || !plotConfig.isDimensionUsed(dimension)) {
					foregroundColor = TreeNodeColors.getWarningColor();
					errorLabelIcon = WARNING_ICON;
				}

				if (!dimensionConfig.getErrors().isEmpty()
						|| foregroundColor != null && foregroundColor.equals(TreeNodeColors.getInvalidColor())) {
					foregroundColor = TreeNodeColors.getInvalidColor();
					errorLabelIcon = ERROR_ICON;
				}
			}

			errorIconLabel.setIcon(errorLabelIcon);

			boolean foregroundSet = foregroundColor != null;
			if (foregroundSet) {
				nameLabel.setForeground(foregroundColor);
				attributeLabel.setForeground(foregroundColor);
			}

			adaptContainerStyle(tree, node, selected, expanded, leaf, row, hasFocus, dragging, !foregroundSet);
		}

		/**
		 * This is a workaround to fix a Swing bug with causes the drag cursor to flicker. See
		 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6700748 TODO: Occasionally check
		 * whether this bug was resolved by Sun.
		 */
		@Override
		public boolean isVisible() {
			return false;
		}

		/**
		 * Overrides <code>JComponent.getPreferredSize</code> to return slightly wider preferred
		 * size value.
		 */
		@Override
		public Dimension getPreferredSize() {
			Dimension retDimension = super.getPreferredSize();

			if (retDimension != null) {
				retDimension = new Dimension(retDimension.width + 3, retDimension.height + 3);
			}
			return retDimension;
		}
	}

	/**
	 * The panel which will be used for the actual rendering of {@link PlotConfigurationTreeNode}s
	 * and {@link ValueSourceTreeNode}s.
	 */
	private class GlobalAndValueSourceTreeCellPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private JLabel nameLabel;

		private JLabel errorIconLabel;

		public GlobalAndValueSourceTreeCellPanel() {
			this.setLayout(new BorderLayout());

			nameLabel = new JLabel("");
			nameLabel.setFont(TREE_FONT);
			nameLabel.setHorizontalAlignment(SwingConstants.LEFT);
			// nameLabel.setPreferredSize(new Dimension(CELL_WIDTH, CELL_HEIGTH));

			this.add(nameLabel, BorderLayout.CENTER);

			errorIconLabel = new JLabel("");
			this.add(errorIconLabel, BorderLayout.EAST);

		}

		public void updateTreeCell(JTree tree, PlotConfigurationTreeNode value, boolean selected, boolean expanded,
				boolean leaf, int row, boolean hasFocus, boolean dragging) {
			adaptPlotConfigurationCell(tree, value, selected, expanded, leaf, row, hasFocus);
			adaptContainerStyle(tree, value, selected, expanded, leaf, row, hasFocus, dragging, true);
		}

		public void updateTreeCell(JTree tree, ValueSourceTreeNode value, boolean selected, boolean expanded, boolean leaf,
				int row, boolean hasFocus, boolean dragging) {
			adaptValueSource(tree, value, selected, expanded, leaf, row, hasFocus, dragging);
		}

		private void adaptContainerStyle(JTree tree, Object node, boolean selected, boolean expanded, boolean leaf, int row,
				boolean hasFocus, boolean dragging, boolean setForeGround) {
			SwingTools.setEnabledRecursive(this, tree.isEnabled());

			Color fg = null;
			Color bg = null;

			boolean highlightingEnabled = RapidMinerGUI.getDragHighlighteMode().equals(DragHighlightMode.FULL);

			JTree.DropLocation dropLocation = tree.getDropLocation();
			if (dropLocation != null && dropLocation.getChildIndex() == -1
					&& tree.getRowForPath(dropLocation.getPath()) == row) {
				fg = TEXT_SELECTED_COLOR;
				bg = SELECTED_COLOR;
			} else if (selected && !dragging) {
				fg = TEXT_SELECTED_COLOR;
				bg = SELECTED_COLOR;
			} else {
				fg = TEXT_NON_SELECTED_COLOR;
				if (highlightingEnabled && dragging) {
					bg = ProcessDrawer.INNER_DRAG_COLOR;
				} else {
					bg = NOT_SELECTED_COLOR;
				}
			}

			if (setForeGround) {
				nameLabel.setForeground(fg);
			}
			this.setBackground(bg);

			if (hasFocus) {
				this.setBorder(focusBorder);
			} else {
				if (highlightingEnabled && dragging) {
					this.setBorder(draggingNotFocusedBorder);
				} else {
					this.setBorder(nonFocusBorder);
				}
			}

		}

		private void adaptPlotConfigurationCell(JTree tree, PlotConfigurationTreeNode node, boolean selected,
				boolean expanded, boolean leaf, int row, boolean hasFocus) {

			String i18nKey = "plotter.configuration_dialog.global_configuration";

			// set label font
			Font fontValue = TREE_FONT;
			if (fontValue == null) {
				fontValue = nameLabel.getFont();
			}

			// set label icon
			String icon = I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.label." + i18nKey + ".icon");
			if (icon != null) {
				ImageIcon iicon = SwingTools.createIcon("16/" + icon);
				nameLabel.setIcon(iicon);
			}

			// set label text
			nameLabel.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.label." + i18nKey + ".label"));

			errorIconLabel.setIcon(null);
		}

		private void adaptValueSource(JTree tree, ValueSourceTreeNode node, boolean selected, boolean expanded, boolean leaf,
				int row, boolean hasFocus, boolean dragging) {

			// set label font
			Font fontValue = TREE_FONT;
			if (fontValue == null) {
				fontValue = nameLabel.getFont();
			}
			fontValue = FontTools.getFont(fontValue.getFamily(), Font.PLAIN, fontValue.getSize());
			nameLabel.setFont(fontValue);

			ValueSource valueSource = node.getUserObject();
			String i18nKey = null;
			if (valueSource.getSeriesFormat().getSeriesType() == VisualizationType.LINES_AND_SHAPES) {
				i18nKey = "plotter.configuration_dialog.line_value_source";
			} else if (valueSource.getSeriesFormat().getSeriesType() == VisualizationType.AREA) {
				i18nKey = "plotter.configuration_dialog.area_value_source";
			} else {
				i18nKey = "plotter.configuration_dialog.bar_value_source";

			}
			// set label icon
			String icon = I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.label." + i18nKey + ".icon");
			if (icon != null) {
				ImageIcon iicon = SwingTools.createIcon("16/" + icon);
				nameLabel.setIcon(iicon);
			}

			// set label text
			String valueText = valueSource.getLabel();
			if (valueText == null) {
				valueText = I18N.getGUILabel("plotter.unnamed_value_label");
			}

			if (!valueSource.isAutoNaming()) {
				valueText += " [";
				valueText += valueSource.getAutoLabel();
				valueText += "]";
			}

			valueText = I18N.getGUILabel("plotter.series_label.label") + valueText;

			nameLabel.setText(valueText);

			Icon errorLabelIcon = null;

			Color foregroundColor = getValueTypeColor(valueSource.getValueType());

			if (foregroundColor != null && foregroundColor.equals(TreeNodeColors.getWarningColor())) {
				errorLabelIcon = WARNING_ICON;
			}

			if (!valueSource.getErrors().isEmpty()) {
				foregroundColor = TreeNodeColors.getInvalidColor();
				errorLabelIcon = ERROR_ICON;
			}

			errorIconLabel.setIcon(errorLabelIcon);

			boolean foregroundSet = foregroundColor != null;
			if (foregroundSet) {
				nameLabel.setForeground(foregroundColor);
			}

			adaptContainerStyle(tree, node, selected, expanded, leaf, row, hasFocus, dragging, !foregroundSet);
		}

		/**
		 * This is a workaround to fix a Swing bug with causes the drag cursor to flicker. See
		 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6700748 TODO: Occasionally check
		 * whether this bug was resolved by Sun.
		 */
		@Override
		public boolean isVisible() {
			return false;
		}

		/**
		 * Overrides <code>JComponent.getPreferredSize</code> to return slightly wider preferred
		 * size value.
		 */
		@Override
		public Dimension getPreferredSize() {
			Dimension retDimension = super.getPreferredSize();

			if (retDimension != null) {
				retDimension = new Dimension(retDimension.width + 3, retDimension.height + 3);
			}
			return retDimension;
		}
	}

	private static final long serialVersionUID = 1L;

	private final DimensionAndRangeAxisTreeCellPanel dimensionAndRangeAxisRenderPanel;
	private final GlobalAndValueSourceTreeCellPanel globalAndValueSourceRenderPanel;

	private boolean dragging;

	private Border draggingNotFocusedBorder;

	public PlotConfigurationTreeCellRenderer(DataTableColumnListTransferHandler aTH) {
		aTH.addDragListener(this);

		ERROR_ICON = SwingTools.createIcon(
				"16/" + I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.label.plotter.configuratiom_dialog.error_icon"));
		WARNING_ICON = SwingTools.createIcon(
				"16/" + I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.label.plotter.configuratiom_dialog.warning_icon"));

		focusBorder = BorderFactory.createLineBorder(BORDER_SELECTION_COLOR);
		nonFocusBorder = BorderFactory.createLineBorder(Color.white);
		draggingNotFocusedBorder = BorderFactory.createLineBorder(ProcessDrawer.INNER_DRAG_COLOR);

		dimensionAndRangeAxisRenderPanel = new DimensionAndRangeAxisTreeCellPanel();
		globalAndValueSourceRenderPanel = new GlobalAndValueSourceTreeCellPanel();
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf,
			int row, boolean hasFocus) {
		if (value instanceof PlotConfigurationTreeNode) {
			globalAndValueSourceRenderPanel.updateTreeCell(tree, (PlotConfigurationTreeNode) value, selected, expanded, leaf,
					row, hasFocus, dragging);
			return globalAndValueSourceRenderPanel;
		} else if (value instanceof ValueSourceTreeNode) {
			globalAndValueSourceRenderPanel.updateTreeCell(tree, (ValueSourceTreeNode) value, selected, expanded, leaf, row,
					hasFocus, dragging);
			return globalAndValueSourceRenderPanel;
		} else if (value instanceof RangeAxisConfigTreeNode) {
			dimensionAndRangeAxisRenderPanel.updateTreeCell(tree, (RangeAxisConfigTreeNode) value, selected, expanded, leaf,
					row, hasFocus, dragging);
			return dimensionAndRangeAxisRenderPanel;
		} else if (value instanceof DimensionConfigTreeNode) {
			dimensionAndRangeAxisRenderPanel.updateTreeCell(tree, (DimensionConfigTreeNode) value, selected, expanded, leaf,
					row, hasFocus, dragging);
			return dimensionAndRangeAxisRenderPanel;
		} else {
			return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		}

	}

	protected Color getValueTypeColor(ValueType type) {
		switch (type) {
			case DATE_TIME:
				return TreeNodeColors.getDateColor();
			case INVALID:
				return TreeNodeColors.getInvalidColor();
			case NOMINAL:
				return TreeNodeColors.getNominalColor();
			case NUMERICAL:
				return TreeNodeColors.getNumericalColor();
			default:
				return TreeNodeColors.getWarningColor();
		}
	}

	@Override
	public void dragStarted(Transferable t) {
		dragging = true;
	}

	@Override
	public void dragEnded() {
		dragging = false;
	}
}
