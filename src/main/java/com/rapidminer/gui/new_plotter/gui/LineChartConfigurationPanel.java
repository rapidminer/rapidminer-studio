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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.rapidminer.gui.new_plotter.configuration.LineFormat.LineStyle;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.FillStyle;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.ItemShape;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.gui.cellrenderer.EnumComboBoxCellRenderer;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.tools.I18N;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class LineChartConfigurationPanel extends AbstractTreeSelectionDependentPanel {

	private static final long serialVersionUID = 1L;

	private JLabel itemShapeLabel;
	private JComboBox<ItemShape> itemShapeComboBox;

	private JLabel itemColorLabel;
	private JButton itemColorButton;

	private JLabel opacityLabel;
	private JSlider opacitySlider;

	private JLabel lineStyleLabel;
	private JComboBox<LineStyle> lineStyleComboBox;

	// private JLabel lineColorLabel;
	// private JButton lineColorButton;

	private JLabel lineWidthLabel;
	private JSpinner lineWidthSpinner;

	private JLabel itemFillLabel;
	private JComboBox<SeriesFormat.FillStyle> itemFillComboBox;

	private JLabel itemSizeLabel;
	private JSpinner itemSizeSpinner;

	public LineChartConfigurationPanel(boolean smallIcons, JTree plotConfigurationTree, PlotInstance plotInstance) {
		super(plotConfigurationTree, plotInstance);

		createComponents(smallIcons);
		registerAsPlotConfigurationListener();
		initComponents();
	}

	private void createComponents(boolean smallIcons) {

		Dimension preferredSize = new Dimension(70, 24);
		{
			// creat item shape label
			itemShapeLabel = new ResourceLabel("plotter.configuration_dialog.itemshape");
			itemShapeLabel.setPreferredSize(preferredSize);

			// create item shape combobox
			itemShapeComboBox = new JComboBox<>(ItemShape.values());
			itemShapeLabel.setLabelFor(itemShapeComboBox);
			itemShapeComboBox.setPreferredSize(preferredSize);
			itemShapeComboBox.setRenderer(new EnumComboBoxCellRenderer<>("plotter.dotstyle"));
			itemShapeComboBox.setSelectedIndex(0);
			itemShapeComboBox.addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					return;

				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					if (getSelectedValueSource() != null) {
						getSelectedValueSource().getSeriesFormat().setItemShape(
								(ItemShape) itemShapeComboBox.getSelectedItem());
					}
				}

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
					return;

				}
			});

			// add item shape row
			addTwoComponentRow(this, itemShapeLabel, itemShapeComboBox);

		}

		// add line width
		{
			itemSizeLabel = new ResourceLabel("plotter.configuration_dialog.item_size");
			itemSizeLabel.setPreferredSize(preferredSize);

			itemSizeSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, null, .1));
			itemSizeLabel.setLabelFor(itemSizeSpinner);
			itemSizeSpinner.setPreferredSize(preferredSize);
			itemSizeSpinner.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					if (getSelectedValueSource() != null) {
						Double newItemSize = (Double) itemSizeSpinner.getValue();
						getSelectedValueSource().getSeriesFormat().setItemSize(newItemSize);
					}
				}

			});

			addTwoComponentRow(this, itemSizeLabel, itemSizeSpinner);
		}

		{
			// create line style label
			lineStyleLabel = new ResourceLabel("plotter.configuration_dialog.line_style");
			lineStyleLabel.setPreferredSize(preferredSize);

			// create line style combobox
			lineStyleComboBox = new JComboBox<>(LineStyle.values());
			lineStyleLabel.setLabelFor(lineStyleComboBox);
			lineStyleComboBox.setPreferredSize(preferredSize);
			lineStyleComboBox.setSelectedIndex(0);
			lineStyleComboBox.setRenderer(new EnumComboBoxCellRenderer<>("plotter.linestyle"));
			lineStyleComboBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (getSelectedValueSource() != null) {
						SeriesFormat format = getSelectedValueSource().getSeriesFormat();
						LineStyle selectedItem = (LineStyle) lineStyleComboBox.getSelectedItem();
						if (selectedItem != LineStyle.NONE && format.getAreaFillStyle() != FillStyle.SOLID) {
							format.setAreaFillStyle(FillStyle.SOLID);
						}
						format.setLineStyle(selectedItem);
					}
				}
			});

			// add line style row
			addTwoComponentRow(this, lineStyleLabel, lineStyleComboBox);

		}

		// Not supported at the moment
		// {
		// // create item color label
		// lineColorLabel = new ResourceLabel("plotter.configuration_dialog.line_color");
		//
		// // create item color button
		// lineColorButton = (new ResourceAction(smallIcons,
		// "plotter.configuration_dialog.choose_color") {
		//
		// private static final long serialVersionUID = 1L;
		//
		// @Override
		// public void loggedActionPerformed(ActionEvent e) {
		// createLineColorDialog();
		// }
		//
		// });
		//
		// // add line color row
		// addTwoComponentRow(this, lineColorLabel, lineColorButton);
		//
		// }

		// add line width
		{
			lineWidthLabel = new ResourceLabel("plotter.configuration_dialog.line_width");
			lineWidthLabel.setPreferredSize(preferredSize);

			lineWidthSpinner = new JSpinner(new SpinnerNumberModel(0.0f, 0.0f, null, 1f));
			lineWidthLabel.setLabelFor(lineWidthSpinner);
			lineWidthSpinner.setPreferredSize(preferredSize);
			lineWidthSpinner.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					if (getSelectedValueSource() != null) {
						float newLineWidth = (Float) lineWidthSpinner.getValue();
						getSelectedValueSource().getSeriesFormat().setLineWidth(newLineWidth);
					}
				}

			});

			addTwoComponentRow(this, lineWidthLabel, lineWidthSpinner);
		}

		// add item color
		{
			// add item color label
			itemColorLabel = new ResourceLabel("plotter.configuration_dialog.item_color");
			itemColorLabel.setPreferredSize(new java.awt.Dimension(70, 24));

			// create item color button
			itemColorButton = new JButton(new ResourceAction(smallIcons, "plotter.configuration_dialog.choose_item_color") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					createItemColorDialog();
				}

			});
			itemColorLabel.setLabelFor(itemColorButton);
			itemColorButton.setPreferredSize(preferredSize);

			addTwoComponentRow(this, itemColorLabel, itemColorButton);
		}

		{
			// create line style label
			itemFillLabel = new ResourceLabel("plotter.configuration_dialog.fill_style");
			itemFillLabel.setPreferredSize(preferredSize);

			itemFillComboBox = new JComboBox<>(SeriesFormat.FillStyle.values());
			itemFillLabel.setLabelFor(itemFillComboBox);
			itemFillComboBox.setPreferredSize(preferredSize);
			itemFillComboBox.setSelectedIndex(0);
			itemFillComboBox.setRenderer(new EnumComboBoxCellRenderer<>("plotter.fillstyle"));
			itemFillComboBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (getSelectedValueSource() != null) {
						getSelectedValueSource().getSeriesFormat()
								.setAreaFillStyle((FillStyle) itemFillComboBox.getSelectedItem());
					}
				}
			});

			// add line style row
			addTwoComponentRow(this, itemFillLabel, itemFillComboBox);

		}

		// add opacity slider
		{
			// create opacity label
			opacityLabel = new ResourceLabel("plotter.configuration_dialog.opacity");
			opacityLabel.setPreferredSize(preferredSize);

			// create opacity slider
			opacitySlider = new JSlider(0, 255, 125);
			opacityLabel.setLabelFor(opacitySlider);
			opacitySlider.setPreferredSize(preferredSize);
			opacitySlider.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					JSlider source = (JSlider) e.getSource();
					if (!source.getValueIsAdjusting()) {
						if (getSelectedValueSource() != null) {
							int newOpacity = opacitySlider.getValue();
							getSelectedValueSource().getSeriesFormat().setOpacity(newOpacity);
						}
					}
				}

			});

			// add opacity slider
			addTwoComponentRow(this, opacityLabel, opacitySlider);

		}

	}

	private void initComponents() {
		adaptGUI();
	}

	private void createItemColorDialog() {
		if (getSelectedValueSource() != null) {
			Color itemColor = getSelectedValueSource().getSeriesFormat().getItemColor();
			Color newItemColor = JColorChooser.showDialog(this,
					I18N.getGUILabel("plotter.configuration_dialog.choose_color.label"), itemColor);
			if (newItemColor != null) {
				getSelectedValueSource().getSeriesFormat().setItemColor(newItemColor);
			}
		}
	}

	// private void createLineColorDialog() {
	// if (getSelectedValueSource() != null) {
	// Color lineColor = getSelectedValueSource().getFormat().getLineColor();
	// Color newLineColor = JColorChooser.showDialog(this,
	// I18N.getGUILabel("plotter.configuration_dialog.choose_color.label"), lineColor);
	// if (newLineColor != null) {
	// getSelectedValueSource().getFormat().setLineColor(newLineColor);
	// }
	// }
	// }

	private void itemShapeChanged(ItemShape itemShape) {
		itemShapeComboBox.setSelectedItem(itemShape);
	}

	private void lineStyleChanged(LineStyle lineStyle) {
		if (lineStyle == lineStyleComboBox.getSelectedItem()) {
			return;
		}

		lineStyleComboBox.setSelectedItem(lineStyle);

		boolean enable = false;
		if (lineStyle != LineStyle.NONE) {
			enable = true;
		}
		// lineColorButton.setEnabled(enable);
		// lineColorLabel.setEnabled(enable);
		lineWidthLabel.setEnabled(enable);
		lineWidthSpinner.setEnabled(enable);
	}

	private void lineWidthChanged(Float lineWidth) {
		lineWidthSpinner.setValue(lineWidth);
	}

	private void itemFillChanged(FillStyle style) {
		itemFillComboBox.setSelectedItem(style);
	}

	private void opacityChanged(Integer integer) {
		opacitySlider.setValue(integer);
	}

	// private void lineColorChanged(Color lineColor) {
	// lineColorButton.setIcon(createColoredRectangleIcon(lineColor));
	// }

	private void itemSizeChanged(Double itemSize) {
		itemSizeSpinner.setValue(itemSize);
	}

	@Override
	protected void adaptGUI() {
		if (getSelectedValueSource() != null) {

			SeriesFormat format = getSelectedValueSource().getSeriesFormat();

			opacityChanged(format.getOpacity());

			itemShapeChanged(format.getItemShape());

			lineStyleChanged(format.getLineStyle());

			// lineColorChanged(format.getLineColor());

			lineWidthChanged(format.getLineWidth());

			itemFillChanged(format.getAreaFillStyle());

			itemSizeChanged(format.getItemSize());
		}

	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		adaptGUI();
		return true;
	}
}
