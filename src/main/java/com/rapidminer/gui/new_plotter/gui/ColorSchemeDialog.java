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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jfree.chart.ChartPanel;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.new_plotter.ChartConfigurationException;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.DefaultDimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.EqualDataFractionGrouping;
import com.rapidminer.gui.new_plotter.configuration.LegendConfiguration.LegendPosition;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.StackingMode;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.VisualizationType;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.engine.jfreechart.JFreeChartPlotEngine;
import com.rapidminer.gui.new_plotter.gui.cellrenderer.ColorListCellRenderer;
import com.rapidminer.gui.new_plotter.gui.cellrenderer.ColorRGBComboBoxCellRenderer;
import com.rapidminer.gui.new_plotter.gui.cellrenderer.ColorSchemeComboBoxRenderer;
import com.rapidminer.gui.new_plotter.listener.PlotConfigurationListener;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent.PlotConfigurationChangeType;
import com.rapidminer.gui.new_plotter.templates.style.ColorRGB;
import com.rapidminer.gui.new_plotter.templates.style.ColorScheme;
import com.rapidminer.gui.new_plotter.utility.ContinuousColorProvider;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.math.function.aggregation.AbstractAggregationFunction.AggregationFunctionType;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ColorSchemeDialog extends ButtonDialog implements PlotConfigurationListener {

	private class GradientPreview extends JComponent {

		private static final long serialVersionUID = 1L;
		private LinearGradientPaint gradientPaint;

		public static final int WIDTH = 50;
		private static final int HEIGHT = 30;

		public GradientPreview(LinearGradientPaint paint) {
			this.gradientPaint = paint;
			this.setSize(new Dimension(WIDTH, HEIGHT));
			this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		}

		@Override
		public void paint(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;
			if (gradientPaint == null) {
				g2d.setPaint(Color.gray);
			} else {
				g2d.setPaint(gradientPaint);
			}
			g2d.fillRect(0, 0, getWidth(), getHeight());
		}

		public void setGradientPaint(LinearGradientPaint gradientPaint) {
			this.gradientPaint = gradientPaint;
		}
	}

	private static final long serialVersionUID = 1L;

	private JList<Color> colorList;
	private JPopupMenu popupMenu;
	private JButton addCategoryButton;
	private JButton removeSchemeButton;
	private JButton removeCategoryColorButton;
	private JScrollPane colorListScrollPane;
	private JPanel categoryAndGradientConfigPanel;
	private JComboBox<Object> colorSchemeComboBox;

	private JMenuItem removeMenuItem;
	private JMenuItem changeColorMenuItem;

	private JButton revertButton;
	private JButton saveButton;

	private DefaultListModel<Color> nominalColorListModel;
	private DefaultComboBoxModel<Color> gradientStartColorComboBoxModel;
	private DefaultComboBoxModel<Color> gradientEndColorComboBoxModel;
	private DefaultComboBoxModel<Object> colorSchemeComboBoxModel;

	private JComboBox<Color> gradientEndColorComboBox;
	private JComboBox<Color> gradientStartColorComboBox;

	private GradientPreview preview;

	private Dimension preferredGradientComboBoxSize = new Dimension(42, 20);

	private String initialActiveColorSchemeName;
	private Map<String, ColorScheme> initialColorSchemes;

	private String currentActiveColorSchemeName;
	private Map<String, ColorScheme> currentColorSchemes;

	private PlotConfiguration gradientPlotConfig;
	private PlotConfiguration nominalPlotConfig;

	private boolean adaptingModels;

	private JButton renameSchemeButton;

	private ChartPanel gradientPreviewPanel;

	private JFreeChartPlotEngine nominalPlotter;

	private JFreeChartPlotEngine gradientPlotter;

	private ChartPanel nominalPreviewPanel;

	private PlotConfiguration plotConfig;

	private boolean initializing = false;

	private JMenuItem moveUpColorMenuItem;

	private JMenuItem moveDownColorMenuItem;

	/**
	 * @param key
	 * @param arguments
	 */
	public ColorSchemeDialog(Component actionComp, String key, PlotConfiguration plotConfig, Object... arguments) {
		super(actionComp != null ? SwingUtilities.getWindowAncestor(actionComp) : null, key, ModalityType.APPLICATION_MODAL,
				arguments);
		this.plotConfig = plotConfig;

		initializing = true;

		nominalColorListModel = new DefaultListModel<Color>();
		gradientStartColorComboBoxModel = new DefaultComboBoxModel<Color>();
		gradientEndColorComboBoxModel = new DefaultComboBoxModel<Color>();
		colorSchemeComboBoxModel = new DefaultComboBoxModel<>();

		this.setResizable(false);

		createPreviewPlotBackend(new JPanel().getBackground(), plotConfig.getActiveColorScheme().getColors().size());

		createComponents();
		save(plotConfig.getColorSchemes(), plotConfig.getActiveColorScheme().getName());

		initializing = false;

		adaptPreviewPlots();

		setLocationRelativeTo(actionComp);

	}

	/**
	 *
	 */
	private void createComponents() {

		// creat popup menus
		{

			popupMenu = new JPopupMenu();
			removeMenuItem = new JMenuItem(
					I18N.getGUILabel("plotter.configuration_dialog.color_scheme_dialog.remove_color_menu_item.label"));
			removeMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					removeSelectedColorAction();
				}

			});
			popupMenu.add(removeMenuItem);

			changeColorMenuItem = new JMenuItem(
					I18N.getGUILabel("plotter.configuration_dialog.color_scheme_dialog.change_color_menu_item.label"));
			changeColorMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					replaceSelectedColorAction();
				}

			});
			popupMenu.add(changeColorMenuItem);

			popupMenu.addSeparator();

			moveUpColorMenuItem = new JMenuItem(
					I18N.getGUILabel("plotter.configuration_dialog.color_scheme_dialog.move_up_menu_item.label"));
			moveUpColorMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					moveSelectedColorUpAction();

				}

			});
			popupMenu.add(moveUpColorMenuItem);

			moveDownColorMenuItem = new JMenuItem(
					I18N.getGUILabel("plotter.configuration_dialog.color_scheme_dialog.move_down_menu_item.label"));
			moveDownColorMenuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					moveSelectedColorDownAction();
				}

			});
			popupMenu.add(moveDownColorMenuItem);

		}

		JPanel containerPanel = new JPanel(new GridBagLayout());
		containerPanel.setPreferredSize(new Dimension(520, 450));

		// create containing panel
		{

			{
				JPanel configurePanel = new JPanel(new GridBagLayout());
				configurePanel.setPreferredSize(new Dimension(220, 400));
				configurePanel.setBorder(BorderFactory.createTitledBorder(I18N
						.getGUILabel("plotter.configuration_dialog.color_scheme_dialog.scheme_configuration_border.label")));

				// add scheme list panel
				{
					JPanel schemeComboBoxPanel = createSchemeComboBoxPanel();

					// add category choosing panel
					GridBagConstraints itemConstraint = new GridBagConstraints();
					itemConstraint.fill = GridBagConstraints.BOTH;
					itemConstraint.weightx = 1;
					itemConstraint.weighty = 1;
					itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
					itemConstraint.insets = new Insets(2, 2, 2, 5);

					configurePanel.add(schemeComboBoxPanel, itemConstraint);
				}

				{
					categoryAndGradientConfigPanel = new JPanel(new GridBagLayout());

					// add categories panel
					{
						JPanel categoryConfigurationPanel = createColorCategoriesPanel();

						// add category choosing panel
						GridBagConstraints itemConstraint = new GridBagConstraints();
						itemConstraint.fill = GridBagConstraints.BOTH;
						itemConstraint.weightx = 1;
						itemConstraint.weighty = 1;
						itemConstraint.insets = new Insets(2, 2, 2, 5);
						itemConstraint.gridwidth = GridBagConstraints.REMAINDER;

						categoryAndGradientConfigPanel.add(categoryConfigurationPanel, itemConstraint);

					}

					// add gradient chooser panel
					{
						JPanel gradientConfigPanel = createGradientConfigurationPanel();

						GridBagConstraints itemConstraint = new GridBagConstraints();
						itemConstraint.fill = GridBagConstraints.BOTH;
						itemConstraint.weightx = 1;
						itemConstraint.weighty = 1;
						itemConstraint.insets = new Insets(2, 2, 2, 5);
						itemConstraint.gridwidth = GridBagConstraints.REMAINDER;

						categoryAndGradientConfigPanel.add(gradientConfigPanel, itemConstraint);
					}

					GridBagConstraints itemConstraint = new GridBagConstraints();
					itemConstraint.fill = GridBagConstraints.BOTH;
					itemConstraint.weightx = 1;
					itemConstraint.weighty = 1;
					itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
					itemConstraint.insets = new Insets(2, 2, 2, 5);

					configurePanel.add(categoryAndGradientConfigPanel, itemConstraint);

				}

				GridBagConstraints itemConstraint = new GridBagConstraints();
				itemConstraint.fill = GridBagConstraints.BOTH;
				itemConstraint.weightx = 1;
				itemConstraint.weighty = 1;
				itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
				containerPanel.add(configurePanel, itemConstraint);
			}

			createPlotPreviewPanel(containerPanel);

		}

		// create buttons
		Collection<AbstractButton> buttons = new LinkedList<AbstractButton>();
		buttons.add(makeOkButton());

		Action saveAction = new ResourceAction("plotter.configuration_dialog.color_scheme_dialog.save_button") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				save(currentColorSchemes, currentActiveColorSchemeName);

				// set new scheme
				plotConfig.setColorSchemes(currentColorSchemes, currentActiveColorSchemeName);
			}
		};
		saveButton = new JButton(saveAction);
		buttons.add(saveButton);
		saveButton.setEnabled(false);

		Action revertAction = new ResourceAction("plotter.configuration_dialog.color_scheme_dialog.revert_button") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				revert();
			}
		};
		revertButton = new JButton(revertAction);
		revertButton.setEnabled(false);
		buttons.add(revertButton);
		buttons.add(makeCancelButton("plotter.configuration_dialog.color_scheme_dialog.cancel_button"));

		layoutDefault(containerPanel, buttons);
	}

	/**
	 * @param containerPanel
	 */
	private void createPlotPreviewPanel(JPanel containerPanel) {

		JPanel previewPanel = new JPanel(new GridBagLayout());
		previewPanel.setPreferredSize(new Dimension(300, 300));

		GridBagConstraints itemConstraint = new GridBagConstraints();
		itemConstraint.fill = GridBagConstraints.BOTH;
		itemConstraint.weightx = 1;
		itemConstraint.weighty = 1;
		itemConstraint.gridwidth = GridBagConstraints.REMAINDER;

		// nominal value color preview
		{
			nominalPreviewPanel = nominalPlotter.getChartPanel();
			nominalPreviewPanel.setPreferredSize(new Dimension(150, 150));
			previewPanel.add(nominalPreviewPanel, itemConstraint);
		}

		// add gradient preview
		{

			gradientPreviewPanel = gradientPlotter.getChartPanel();
			gradientPreviewPanel.setPreferredSize(new Dimension(150, 150));

			previewPanel.add(gradientPreviewPanel, itemConstraint);

		}

		itemConstraint = new GridBagConstraints();
		itemConstraint.fill = GridBagConstraints.BOTH;
		itemConstraint.weightx = 1;
		itemConstraint.weighty = 1;
		itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
		containerPanel.add(previewPanel, itemConstraint);
	}

	/**
	 * @return
	 */
	private JPanel createGradientConfigurationPanel() {

		// create gradient config panel
		JPanel gradientConfigPanel = new JPanel(new GridBagLayout());
		gradientConfigPanel.setPreferredSize(new Dimension(100, 50));

		GridBagConstraints itemConstraint;

		// add gradient label
		{
			JLabel gradientLabel = new ResourceLabel("plotter.configuration_dialog.color_scheme_dialog.gradient_preview");

			itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 1;
			itemConstraint.weighty = 1;
			itemConstraint.fill = GridBagConstraints.BOTH;
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
			itemConstraint.insets = new Insets(2, 2, 2, 2);

			gradientConfigPanel.add(gradientLabel, itemConstraint);
		}

		// add combobox panel
		{
			JPanel comboBoxPanel = new JPanel(new GridBagLayout());

			// gradient start panel
			{

				JPanel startGradientPanel = createStartGradientPanel();

				itemConstraint = new GridBagConstraints();
				itemConstraint.weightx = 1.0;
				itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
				itemConstraint.anchor = GridBagConstraints.WEST;
				itemConstraint.fill = GridBagConstraints.HORIZONTAL;

				comboBoxPanel.add(startGradientPanel, itemConstraint);

			}

			// gradient end panel
			{

				JPanel endGradientPanel = createEndGradientPanel();

				itemConstraint = new GridBagConstraints();
				itemConstraint.weightx = 1.0;
				itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
				itemConstraint.anchor = GridBagConstraints.EAST;
				itemConstraint.fill = GridBagConstraints.HORIZONTAL;

				comboBoxPanel.add(endGradientPanel, itemConstraint);

			}

			itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 1;
			itemConstraint.weighty = 0.0;
			itemConstraint.fill = GridBagConstraints.HORIZONTAL;
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
			itemConstraint.insets = new Insets(2, 2, 2, 2);

			gradientConfigPanel.add(comboBoxPanel, itemConstraint);
		}

		// add 0.0 label
		{
			JLabel zeroLabel = new JLabel("0");

			itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 0;
			itemConstraint.weighty = 0;
			itemConstraint.fill = GridBagConstraints.NONE;
			itemConstraint.insets = new Insets(2, 2, 2, 2);

			gradientConfigPanel.add(zeroLabel, itemConstraint);
		}

		// add gradient preview
		{
			preview = new GradientPreview(null);

			itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 1;
			itemConstraint.weighty = 1;
			itemConstraint.fill = GridBagConstraints.BOTH;
			itemConstraint.insets = new Insets(2, 2, 2, 2);

			gradientConfigPanel.add(preview, itemConstraint);

		}

		// add 1.0 label
		{
			JLabel zeroLabel = new JLabel("1");

			itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 0;
			itemConstraint.weighty = 0;
			itemConstraint.fill = GridBagConstraints.NONE;
			itemConstraint.insets = new Insets(2, 2, 2, 2);
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER;

			gradientConfigPanel.add(zeroLabel, itemConstraint);
		}

		return gradientConfigPanel;
	}

	/**
	 * @return
	 */
	private JPanel createEndGradientPanel() {
		GridBagConstraints itemConstraint;
		JPanel endGradientPanel = new JPanel(new GridBagLayout());

		// add gradient end label
		JLabel gradientEndLabel = new ResourceLabel("plotter.configuration_dialog.color_scheme_dialog.gradient_end");
		{
			itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 1.0;
			itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
			itemConstraint.insets = new Insets(0, 2, 0, 0);

			endGradientPanel.add(gradientEndLabel, itemConstraint);
		}

		// add gradient end color combo box
		{
			gradientEndColorComboBox = new JComboBox<Color>(gradientEndColorComboBoxModel);
			gradientEndLabel.setLabelFor(gradientEndColorComboBox);
			gradientEndColorComboBox.setPreferredSize(preferredGradientComboBoxSize);
			gradientEndColorComboBox.setRenderer(new ColorRGBComboBoxCellRenderer<>());
			gradientEndColorComboBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					// set gradient end color to current active color scheme
					Color color = (Color) gradientEndColorComboBox.getSelectedItem();
					if (color != null && !adaptingModels) {

						// enable apply and revert button
						saveButton.setEnabled(true);
						revertButton.setEnabled(true);

						getCurrentActiveColorScheme().setGradientEndColor(ColorRGB.convertColorToColorRGB(color));
					}
					adaptGradientPlot();
					calculateGradientPreview();
				}

			});

			itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 1.0;
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER; // end row
			itemConstraint.insets = new Insets(0, 2, 0, 0);

			endGradientPanel.add(gradientEndColorComboBox, itemConstraint);
		}
		return endGradientPanel;
	}

	/**
	 * @return
	 */
	private JPanel createStartGradientPanel() {
		GridBagConstraints itemConstraint;
		JPanel startGradientPanel = new JPanel(new GridBagLayout());

		// add gradient start label
		JLabel gradientStartLabel = new ResourceLabel("plotter.configuration_dialog.color_scheme_dialog.gradient_start");
		{
			itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 1;
			itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
			itemConstraint.insets = new Insets(0, 5, 5, 5);

			startGradientPanel.add(gradientStartLabel, itemConstraint);
		}

		// add gradient start color combobox
		{
			gradientStartColorComboBox = new JComboBox<Color>(gradientStartColorComboBoxModel);
			gradientStartLabel.setLabelFor(gradientStartColorComboBox);
			gradientStartColorComboBox.setPreferredSize(preferredGradientComboBoxSize);
			gradientStartColorComboBox.setRenderer(new ColorRGBComboBoxCellRenderer<>());
			gradientStartColorComboBox.setEditable(false);
			gradientStartColorComboBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					// set gradient start color at current active color scheme
					Color color = (Color) gradientStartColorComboBox.getSelectedItem();
					if (color != null && !adaptingModels) {

						// enable apply and revert button
						saveButton.setEnabled(true);
						revertButton.setEnabled(true);

						getCurrentActiveColorScheme().setGradientStartColor(ColorRGB.convertColorToColorRGB(color));
					}
					adaptGradientPlot();
					calculateGradientPreview();
				}

			});

			itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 1.0;
			itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
			// itemConstraint.fill = GridBagConstraints.HORIZONTAL;
			// itemConstraint.insets = new Insets(0, 5, 5, 5);

			startGradientPanel.add(gradientStartColorComboBox, itemConstraint);
		}
		return startGradientPanel;
	}

	/**
	 * @return
	 */
	private JPanel createColorCategoriesPanel() {
		JPanel categoryColorsConfigurationPanel = new JPanel(new GridBagLayout());
		categoryColorsConfigurationPanel.setPreferredSize(new Dimension(180, 200));

		GridBagConstraints itemConstraint = new GridBagConstraints();

		JLabel categoryColorsLabel = new ResourceLabel("plotter.configuration_dialog.color_scheme_dialog.category_colors");
		{

			itemConstraint.fill = GridBagConstraints.HORIZONTAL;
			itemConstraint.anchor = GridBagConstraints.WEST;
			itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
			itemConstraint.insets = new Insets(0, 5, 5, 5);
			itemConstraint.weightx = 1.0;

			categoryColorsConfigurationPanel.add(categoryColorsLabel, itemConstraint);
		}

		// add button panel
		{
			JPanel buttonPanel = new JPanel(new GridBagLayout());

			// remove scheme button
			{
				removeCategoryColorButton = new JButton(new ResourceAction(true,
						"plotter.configuration_dialog.color_scheme_dialog.remove_category_color_button") {

					private static final long serialVersionUID = 1L;

					@Override
					public void loggedActionPerformed(ActionEvent e) {
						removeSelectedColorAction();
					}

				});

				itemConstraint = new GridBagConstraints();
				itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
				itemConstraint.fill = GridBagConstraints.NONE;

				buttonPanel.add(removeCategoryColorButton, itemConstraint);
			}

			{
				addCategoryButton = new JButton(new ResourceAction(true,
						"plotter.configuration_dialog.color_scheme_dialog.add_category_color_button") {

					private static final long serialVersionUID = 1L;

					@Override
					public void loggedActionPerformed(ActionEvent e) {
						Color oldColor = Color.white;
						Color newSchemeColor = createColorDialog(oldColor);
						if (newSchemeColor != null && !newSchemeColor.equals(oldColor)) {
							addColorAction(newSchemeColor);
						}
					}

				});

				itemConstraint = new GridBagConstraints();
				itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
				itemConstraint.fill = GridBagConstraints.NONE;

				buttonPanel.add(addCategoryButton, itemConstraint);

			}

			itemConstraint = new GridBagConstraints();
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
			itemConstraint.fill = GridBagConstraints.NONE;
			itemConstraint.anchor = GridBagConstraints.EAST;
			itemConstraint.insets = new Insets(0, 5, 5, 5);

			categoryColorsConfigurationPanel.add(buttonPanel, itemConstraint);
		}

		{

			JPanel categoryListPanel = new JPanel(new GridBagLayout());

			// add list of categorie colors
			{

				colorList = new JList<Color>(nominalColorListModel);
				categoryColorsLabel.setLabelFor(colorList);
				colorList.setCellRenderer(new ColorListCellRenderer());
				colorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

				MouseAdapter ma = new MouseAdapter() {

					private void myPopupEvent(MouseEvent e) {
						int x = e.getX();
						int y = e.getY();
						JList<?> list = (JList<?>) e.getSource();
						list.setSelectedIndex(list.locationToIndex(e.getPoint()));
						Color selectedColor = (Color) list.getSelectedValue();
						if (selectedColor == null) {
							return;
						}

						removeMenuItem.setEnabled(nominalColorListModel.getSize() > 2);

						popupMenu.show(list, x, y);
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

				colorList.addMouseListener(ma);
				colorList.addKeyListener(new KeyListener() {

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
							if (nominalColorListModel.getSize() > 2) {
								removeSelectedColorAction();
							}
						}
						if (key == KeyEvent.VK_F2) {
							replaceSelectedColorAction();
						}
						if (key == KeyEvent.VK_UP && SwingTools.isControlOrMetaDown(e)) {
							moveSelectedColorUpAction();
						}
						if (key == KeyEvent.VK_DOWN && SwingTools.isControlOrMetaDown(e)) {
							moveSelectedColorDownAction();
						}
					}
				});

				colorListScrollPane = new JScrollPane(colorList);
				colorListScrollPane.setPreferredSize(new Dimension(170, 200));
				colorListScrollPane.setMaximumSize(new Dimension(170, 200));
				colorListScrollPane.setMinimumSize(new Dimension(170, 180));
				colorListScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
				colorListScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

				itemConstraint = new GridBagConstraints();
				itemConstraint.fill = GridBagConstraints.BOTH;
				itemConstraint.weightx = 0.0;
				itemConstraint.weighty = 0.5;
				itemConstraint.gridwidth = GridBagConstraints.RELATIVE;

				categoryListPanel.add(colorListScrollPane, itemConstraint);
			}

			// add up/down button panel
			{

				JPanel upAndDownButtonPanel = new JPanel(new GridBagLayout());

				// add up button
				{
					JButton upButton = new JButton(new ResourceAction(true, "plotter.configuration_dialog.move_color_up") {

						private static final long serialVersionUID = 1L;

						@Override
						public void loggedActionPerformed(ActionEvent e) {
							moveSelectedColorUpAction();
						}
					});

					itemConstraint = new GridBagConstraints();
					itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
					itemConstraint.weightx = 0;
					itemConstraint.weighty = 0;
					itemConstraint.fill = GridBagConstraints.NONE;
					itemConstraint.insets = new Insets(0, 2, 0, 12);

					upAndDownButtonPanel.add(upButton, itemConstraint);
				}

				// add down button
				{
					JButton downButton = new JButton(
							new ResourceAction(true, "plotter.configuration_dialog.move_color_down") {

								private static final long serialVersionUID = 1L;

								@Override
								public void loggedActionPerformed(ActionEvent e) {
									moveSelectedColorDownAction();
								}
							});

					itemConstraint = new GridBagConstraints();
					itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
					itemConstraint.weightx = 0;
					itemConstraint.weighty = 0;
					itemConstraint.fill = GridBagConstraints.NONE;
					itemConstraint.insets = new Insets(0, 2, 0, 12);

					upAndDownButtonPanel.add(downButton, itemConstraint);
				}

				// add spacer panel
				{
					JPanel spacer = new JPanel();

					itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
					itemConstraint.weightx = 0;
					itemConstraint.weighty = 1;
					itemConstraint.fill = GridBagConstraints.VERTICAL;

					upAndDownButtonPanel.add(spacer, itemConstraint);

				}

				itemConstraint = new GridBagConstraints();
				itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
				itemConstraint.weightx = 1;
				itemConstraint.weighty = 1;
				itemConstraint.fill = GridBagConstraints.VERTICAL;

				categoryListPanel.add(upAndDownButtonPanel, itemConstraint);

			}

			itemConstraint = new GridBagConstraints();
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
			itemConstraint.weightx = 1;
			itemConstraint.weighty = 1;
			itemConstraint.fill = GridBagConstraints.BOTH;

			categoryColorsConfigurationPanel.add(categoryListPanel, itemConstraint);
		}

		return categoryColorsConfigurationPanel;
	}

	/**
	 * @return
	 */
	private JPanel createSchemeComboBoxPanel() {
		JPanel schemeListPanel = new JPanel(new GridBagLayout());

		// create categories configuration panel
		{

			GridBagConstraints itemConstraint = new GridBagConstraints();

			// Add active scheme label
			JLabel actviceSchemeLabel = new ResourceLabel("plotter.configuration_dialog.color_scheme_dialog.active_scheme");
			{
				itemConstraint.fill = GridBagConstraints.HORIZONTAL;
				itemConstraint.anchor = GridBagConstraints.WEST;
				itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
				itemConstraint.insets = new Insets(0, 5, 5, 5);
				itemConstraint.weightx = 1.0;

				actviceSchemeLabel.setBackground(Color.red);

				schemeListPanel.add(actviceSchemeLabel, itemConstraint);
			}

			// add button panel
			{
				JPanel buttonPanel = new JPanel(new GridBagLayout());

				// rename scheme button
				{
					renameSchemeButton = new JButton(new ResourceAction(true,
							"plotter.configuration_dialog.color_scheme_dialog.rename_scheme_button") {

						private static final long serialVersionUID = 1L;

						@Override
						public void loggedActionPerformed(ActionEvent e) {
							String newName = createNameDialog(currentActiveColorSchemeName);
							if (newName != null && !newName.equals(currentActiveColorSchemeName)) {
								renameColorSchemeAction(newName);
							}
						}

					});

					itemConstraint = new GridBagConstraints();
					itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
					itemConstraint.fill = GridBagConstraints.NONE;

					buttonPanel.add(renameSchemeButton, itemConstraint);
				}

				// remove scheme button
				{
					removeSchemeButton = new JButton(new ResourceAction(true,
							"plotter.configuration_dialog.color_scheme_dialog.remove_scheme_button") {

						private static final long serialVersionUID = 1L;

						@Override
						public void loggedActionPerformed(ActionEvent e) {
							ConfirmDialog dialog = new ConfirmDialog(
									SwingUtilities.getWindowAncestor((Component) e.getSource()),
									"plotter.configuration_dialog.confirm_color_scheme_delete", ConfirmDialog.YES_NO_OPTION,
									false);
							dialog.setLocationRelativeTo((Component) e.getSource());
							dialog.setVisible(true);
							if (dialog.getReturnOption() == ConfirmDialog.YES_OPTION) {
								removeColorSchemeAction((ColorScheme) colorSchemeComboBox.getSelectedItem());
							}
						}

					});

					itemConstraint = new GridBagConstraints();
					itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
					itemConstraint.fill = GridBagConstraints.NONE;

					buttonPanel.add(removeSchemeButton, itemConstraint);
				}

				itemConstraint = new GridBagConstraints();
				itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
				itemConstraint.fill = GridBagConstraints.NONE;
				itemConstraint.anchor = GridBagConstraints.EAST;
				itemConstraint.insets = new Insets(0, 5, 5, 5);

				schemeListPanel.add(buttonPanel, itemConstraint);

			}

			{

				colorSchemeComboBox = new JComboBox<>(colorSchemeComboBoxModel);
				actviceSchemeLabel.setLabelFor(colorSchemeComboBox);
				colorSchemeComboBox.setRenderer(new ColorSchemeComboBoxRenderer());
				colorSchemeComboBox.addPopupMenuListener(new PopupMenuListener() {

					@Override
					public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
						return;

					}

					@Override
					public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
						Object selectedValue = colorSchemeComboBox.getSelectedItem();
						if (selectedValue instanceof ColorScheme) {
							ColorScheme selection = (ColorScheme) selectedValue;
							if (selection != null) {
								if (!currentActiveColorSchemeName.equals(selection.getName())) {
									currentActiveColorSchemeName = selection.getName();
									adaptModels();
								}
							}
						} else {
							String newName = I18N.getGUILabel("plotter.new_color_scheme_name.label");
							String suffix = "";
							int counter = 1;
							while (currentColorSchemes.get(newName + suffix) != null) {
								suffix = "_" + counter;
								counter++;
							}
							newName += suffix;
							String userSelectedName = createNameDialog(newName);
							if (userSelectedName == null) {
								colorSchemeComboBox.setSelectedItem(getCurrentActiveColorScheme());
								return;
							}
							addNewColorSchemeAction(userSelectedName);
						}
					}

					@Override
					public void popupMenuCanceled(PopupMenuEvent e) {
						return;
					}
				});

				itemConstraint = new GridBagConstraints();
				itemConstraint.fill = GridBagConstraints.BOTH;
				itemConstraint.weightx = 0.0;
				itemConstraint.weighty = 0.0;
				itemConstraint.gridwidth = GridBagConstraints.REMAINDER;

				schemeListPanel.add(colorSchemeComboBox, itemConstraint);
			}

		}
		return schemeListPanel;
	}

	private void createPreviewPlotBackend(Color background, int groupinBins) {
		DataTable dataTable;
		// retrieve data for showing example data
		try {
			ExampleSet exampleSet = (ExampleSet) ((IOObjectEntry) new RepositoryLocation("//Samples/data/Iris")
					.locateEntry()).retrieveData(null);
			dataTable = new DataTableExampleSetAdapter(exampleSet, null);
		} catch (MalformedRepositoryLocationException e) {
			return;
		} catch (RepositoryException e) {
			return;
		}

		if (dataTable == null || dataTable.getColumnNumber() < 2) {
			return;
		}

		// domain and y column
		DataTableColumn domainColumn = new DataTableColumn(dataTable, 0);
		DataTableColumn mainColumn = new DataTableColumn(dataTable, 1);

		Font titleFont = FontTools.getFont("Lucida Sans", Font.PLAIN, 12);

		// configure gradient preview plot
		gradientPlotConfig = new PlotConfiguration(domainColumn);
		gradientPlotConfig.setTitleText(
				I18N.getGUILabel("plotter.configuration_dialog.color_scheme_dialog.numerical_gradient_preview.label"));
		gradientPlotConfig.setFrameBackgroundColor(background);
		gradientPlotConfig.getDomainConfigManager().setLabel("");
		gradientPlotConfig.setTitleFont(titleFont);
		gradientPlotConfig.getLegendConfiguration().setLegendPosition(LegendPosition.NONE);

		RangeAxisConfig rangeAxis = new RangeAxisConfig("", gradientPlotConfig);
		ValueSource valueSource = new ValueSource(gradientPlotConfig, mainColumn, null, false);
		rangeAxis.addValueSource(valueSource, gradientPlotConfig.getAutomaticSeriesFormatForNextValueSource(rangeAxis));
		gradientPlotConfig.addRangeAxisConfig(rangeAxis);

		DefaultDimensionConfig colorDimension = new DefaultDimensionConfig(gradientPlotConfig, mainColumn,
				PlotDimension.COLOR);
		gradientPlotConfig.setDimensionConfig(PlotDimension.COLOR, colorDimension);

		PlotInstance plotInstance = new PlotInstance(gradientPlotConfig, dataTable);

		gradientPlotter = new JFreeChartPlotEngine(plotInstance, true);

		// configure nominal preview plot
		nominalPlotConfig = new PlotConfiguration(domainColumn);
		nominalPlotConfig.setTitleText(
				I18N.getGUILabel("plotter.configuration_dialog.color_scheme_dialog.nominal_color_preview.label"));
		nominalPlotConfig.setFrameBackgroundColor(background);
		nominalPlotConfig.getDomainConfigManager().setLabel("");
		nominalPlotConfig.setTitleFont(titleFont);
		nominalPlotConfig.getLegendConfiguration().setLegendPosition(LegendPosition.NONE);

		EqualDataFractionGrouping edfg;
		try {
			edfg = new EqualDataFractionGrouping(domainColumn, 4, true, DateFormat.getDateTimeInstance());
		} catch (ChartConfigurationException e1) {
			return;
		}
		nominalPlotConfig.getDomainConfigManager().setGrouping(edfg);

		RangeAxisConfig nominalRangeAxis = new RangeAxisConfig("", nominalPlotConfig);
		ValueSource nominalValueSource = new ValueSource(nominalPlotConfig, mainColumn, AggregationFunctionType.count, true);
		nominalValueSource.getSeriesFormat().setSeriesType(VisualizationType.BARS);
		nominalValueSource.getSeriesFormat().setStackingMode(StackingMode.RELATIVE);
		nominalRangeAxis.addValueSource(nominalValueSource,
				nominalPlotConfig.getAutomaticSeriesFormatForNextValueSource(nominalRangeAxis));
		nominalPlotConfig.addRangeAxisConfig(nominalRangeAxis);

		DefaultDimensionConfig nominalColorDimension = new DefaultDimensionConfig(nominalPlotConfig, mainColumn,
				PlotDimension.COLOR);

		EqualDataFractionGrouping edfgColor;
		try {
			edfgColor = new EqualDataFractionGrouping(mainColumn, groupinBins, true, DateFormat.getDateTimeInstance());
		} catch (ChartConfigurationException e1) {
			return;
		}
		nominalColorDimension.setGrouping(edfgColor);
		nominalPlotConfig.setDimensionConfig(PlotDimension.COLOR, nominalColorDimension);

		PlotInstance nominalPlotInstance = new PlotInstance(nominalPlotConfig, dataTable);

		nominalPlotter = new JFreeChartPlotEngine(nominalPlotInstance, true);

		gradientPlotter.endInitializing();
		nominalPlotter.endInitializing();

		return;
	}

	private Color createColorDialog(Color oldColor) {
		return JColorChooser.showDialog(this,
				I18N.getGUILabel("plotter.configuration_dialog.color_scheme_dialog.new_category_color.label"), oldColor);
	}

	private String createNameDialog(String oldName) {
		String newName = SwingTools.showInputDialog(ColorSchemeDialog.this,
				"plotter.configuration_dialog.color_scheme_dialog.rename", oldName);
		if (newName != null) {
			boolean success = currentColorSchemes.get(newName) == null;
			if (newName.equals(oldName)) {
				success = true;
			}
			if (!success) {
				SwingTools.showVerySimpleErrorMessage(ColorSchemeDialog.this, "cannot_rename_entry", oldName, newName);
				return oldName;
			}
			return newName;
		}
		return null;

	}

	private void calculateGradientPreview() {

		Color startColor = (Color) gradientStartColorComboBox.getSelectedItem();
		Color endColor = (Color) gradientEndColorComboBox.getSelectedItem();

		if (startColor != null && endColor != null) {

			int width = preview.getWidth() - 1;

			ContinuousColorProvider colorProvider = new ContinuousColorProvider(1, width, startColor, endColor, 255, false);

			// create paint
			float fractions[] = new float[width];
			Color colors[] = new Color[width];

			for (int i = 0; i < width; ++i) {

				float fraction = i / (width - 1.0f);
				double fractionValue = 1 + fraction * (width - 1);
				colors[i] = colorProvider.getColorForValue(fractionValue);
				fractions[i] = fraction;
			}

			Point leftPoint = new Point(0, 0);
			Point rightPoint = new Point(width, 0);

			LinearGradientPaint gradient = new LinearGradientPaint(leftPoint, rightPoint, fractions, colors,
					CycleMethod.REFLECT);
			preview.setGradientPaint(gradient);
			preview.repaint();
		}
	}

	private ColorScheme getCurrentActiveColorScheme() {
		return currentColorSchemes.get(currentActiveColorSchemeName);
	}

	private void moveSelectedColorUpAction() {

		Color color = colorList.getSelectedValue();
		int oldIndex = colorList.getSelectedIndex();

		if (color != null) {

			// remove element from list
			if (oldIndex == 0) {
				return;
			}

			// remove element
			nominalColorListModel.remove(oldIndex);

			int newIndex = oldIndex - 1;

			// add element at index
			nominalColorListModel.add(newIndex, color);

			// change index in current active color scheme
			getCurrentActiveColorScheme().exchange(oldIndex, newIndex);

			adaptNominalPlot();

			colorList.setSelectedIndex(newIndex);

			// enable save and revert button
			saveButton.setEnabled(true);
			revertButton.setEnabled(true);
		}
	}

	private void moveSelectedColorDownAction() {
		Color color = colorList.getSelectedValue();
		int oldIndex = colorList.getSelectedIndex();

		if (color != null) {

			// remove element from list
			if (oldIndex == nominalColorListModel.getSize() - 1) {
				return;
			}

			// remove element
			nominalColorListModel.remove(oldIndex);

			int newIndex = oldIndex + 1;

			// add element at index
			nominalColorListModel.add(newIndex, color);

			// change index in current active color scheme
			getCurrentActiveColorScheme().exchange(oldIndex, newIndex);

			adaptNominalPlot();

			colorList.setSelectedIndex(newIndex);

			// enable save and revert button
			saveButton.setEnabled(true);
			revertButton.setEnabled(true);
		}
	}

	private void replaceSelectedColorAction() {
		Color oldColor = colorList.getSelectedValue();
		Color newSchemeColor = createColorDialog(oldColor);
		if (newSchemeColor != null && !newSchemeColor.equals(oldColor)) {
			replaceColorAction(newSchemeColor, oldColor);
			adaptPreviewPlots();
		}
	}

	private void removeSelectedColorAction() {
		Color color = colorList.getSelectedValue();
		if (color != null) {

			// enable save and revert button
			saveButton.setEnabled(true);
			revertButton.setEnabled(true);

			// remove element from list
			nominalColorListModel.removeElement(color);

			// remove color from current color scheme
			getCurrentActiveColorScheme().removeColor(ColorRGB.convertColorToColorRGB(color));

			// set gradient start end end color accordingly
			Color oldStartingColor = (Color) gradientStartColorComboBox.getSelectedItem();
			gradientStartColorComboBoxModel.removeElement(color);
			if (!color.equals(oldStartingColor)) {
				gradientStartColorComboBox.setSelectedItem(oldStartingColor);
			}
			gradientStartColorComboBox.setSelectedItem(oldStartingColor);

			Color oldEndingColor = (Color) gradientEndColorComboBox.getSelectedItem();
			gradientEndColorComboBoxModel.removeElement(color);
			if (!color.equals(oldEndingColor)) {
				gradientEndColorComboBox.setSelectedItem(oldEndingColor);
			}

			checkIfButtonsEnabled();

			adaptPreviewPlots();

		}

	}

	/**
	 * @param newSchemeColor
	 */
	private void addColorAction(Color newSchemeColor) {

		// enable apply and revert button
		saveButton.setEnabled(true);
		revertButton.setEnabled(true);

		if (!nominalColorListModel.contains(newSchemeColor)) {
			// add new color to color list
			nominalColorListModel.addElement(newSchemeColor);
			colorList.ensureIndexIsVisible(nominalColorListModel.getSize() - 1);

			// add color to current colorScheme
			getCurrentActiveColorScheme().addColor(ColorRGB.convertColorToColorRGB(newSchemeColor));

			// adapt gradient combo boxes
			Color oldStartingColor = (Color) gradientStartColorComboBox.getSelectedItem();
			gradientStartColorComboBoxModel.addElement(newSchemeColor);
			gradientStartColorComboBox.setSelectedItem(oldStartingColor);

			Color oldEndingColor = (Color) gradientEndColorComboBox.getSelectedItem();
			gradientEndColorComboBoxModel.addElement(newSchemeColor);
			gradientEndColorComboBox.setSelectedItem(oldEndingColor);

			checkIfButtonsEnabled();

			adaptNominalPlot();
		}
	}

	private void renameColorSchemeAction(String newName) {
		saveButton.setEnabled(true);
		revertButton.setEnabled(true);

		ColorScheme currentActiveScheme = getCurrentActiveColorScheme();
		currentColorSchemes.remove(currentActiveColorSchemeName);
		currentActiveScheme.setName(newName);
		currentColorSchemes.put(newName, currentActiveScheme);
		currentActiveColorSchemeName = newName;

		int indexOf = colorSchemeComboBoxModel.getIndexOf(currentActiveScheme);
		colorSchemeComboBoxModel.removeElement(currentActiveScheme);
		colorSchemeComboBoxModel.insertElementAt(currentActiveScheme, indexOf);
		colorSchemeComboBoxModel.setSelectedItem(currentActiveScheme);
	}

	/**
	 * @param newSchemeColor
	 */
	private void replaceColorAction(Color newSchemeColor, Color oldSchemeColor) {

		// enable apply and revert button
		saveButton.setEnabled(true);
		revertButton.setEnabled(true);

		// add new color to color list
		int index = nominalColorListModel.indexOf(oldSchemeColor);
		nominalColorListModel.set(index, newSchemeColor);
		colorList.ensureIndexIsVisible(index);

		// add color to current colorScheme
		getCurrentActiveColorScheme().setColor(ColorRGB.convertColorToColorRGB(oldSchemeColor),
				ColorRGB.convertColorToColorRGB(newSchemeColor));

		// adapt gradient combo boxes
		Color oldStartingColor = (Color) gradientStartColorComboBox.getSelectedItem();
		gradientStartColorComboBoxModel.removeAllElements();

		Color oldEndingColor = (Color) gradientEndColorComboBox.getSelectedItem();
		gradientEndColorComboBoxModel.removeAllElements();

		int size = nominalColorListModel.getSize();
		for (int i = 0; i < size; i++) {
			Color color = nominalColorListModel.getElementAt(i);
			gradientStartColorComboBoxModel.addElement(color);
			gradientEndColorComboBoxModel.addElement(color);
		}

		gradientStartColorComboBox.setSelectedItem(oldStartingColor);
		gradientEndColorComboBox.setSelectedItem(oldEndingColor);

		checkIfButtonsEnabled();
	}

	private void addNewColorSchemeAction(String name) {

		saveButton.setEnabled(true);
		revertButton.setEnabled(true);

		// create new scheme
		ColorScheme newColorScheme = gradientPlotConfig.getDefaultColorScheme();
		newColorScheme.setName(name);

		// add scheme to current color schemes and set active
		currentColorSchemes.put(name, newColorScheme);
		currentActiveColorSchemeName = name;

		adaptModels();

	}

	private void removeColorSchemeAction(ColorScheme schemeToRemove) {

		saveButton.setEnabled(true);
		revertButton.setEnabled(true);

		currentColorSchemes.remove(schemeToRemove.getName());
		colorSchemeComboBoxModel.removeElement(schemeToRemove);
		colorSchemeComboBox.setSelectedIndex(0);
		ColorScheme selectedItem = (ColorScheme) colorSchemeComboBox.getSelectedItem();
		if (selectedItem == null) {
			currentActiveColorSchemeName = gradientPlotConfig.getDefaultColorScheme().getName();
		} else {
			currentActiveColorSchemeName = selectedItem.getName();
		}
		adaptModels();

	}

	/**
	 * Resets the dialog to the initial color schemes and active color scheme
	 */
	private void revert() {
		save(initialColorSchemes, initialActiveColorSchemeName);
	}

	/**
	 * Clones parameter and sets them to current and initial color scheme fields
	 */
	private void save(Map<String, ColorScheme> colorSchemes, String activeSchemeId) {

		saveButton.setEnabled(false);
		revertButton.setEnabled(false);

		Map<String, ColorScheme> initialColorSchemes = new HashMap<String, ColorScheme>();
		Map<String, ColorScheme> currentColorSchemes = new HashMap<String, ColorScheme>();

		// copy color schemes for cloning
		for (ColorScheme scheme : colorSchemes.values()) {
			if (scheme != null) {
				initialColorSchemes.put(scheme.getName(), scheme.clone());
				currentColorSchemes.put(scheme.getName(), scheme.clone());
			}
		}

		this.currentActiveColorSchemeName = activeSchemeId;
		this.initialActiveColorSchemeName = activeSchemeId;
		this.initialColorSchemes = initialColorSchemes;
		this.currentColorSchemes = currentColorSchemes;

		adaptModels();

	}

	@Override
	protected void ok() {

		// set new scheme
		plotConfig.setColorSchemes(currentColorSchemes, currentActiveColorSchemeName);

		super.ok();
	}

	private void adaptModels() {

		adaptingModels = true;

		// clear all models
		nominalColorListModel.clear();
		gradientStartColorComboBoxModel.removeAllElements();
		gradientEndColorComboBoxModel.removeAllElements();
		colorSchemeComboBoxModel.removeAllElements();

		// fill color list and start/end combo box models
		List<ColorRGB> colors = getCurrentActiveColorScheme().getColors();
		for (ColorRGB color : colors) {
			Color convertToColor = ColorRGB.convertToColor(color);
			nominalColorListModel.addElement(convertToColor);
			gradientStartColorComboBoxModel.addElement(convertToColor);
			gradientEndColorComboBoxModel.addElement(convertToColor);
		}
		Color gradientStartColor = ColorRGB.convertToColor(getCurrentActiveColorScheme().getGradientStartColor());
		Color gradientEndColor = ColorRGB.convertToColor(getCurrentActiveColorScheme().getGradientEndColor());

		gradientStartColorComboBox.setSelectedItem(gradientStartColor);
		gradientEndColorComboBox.setSelectedItem(gradientEndColor);

		for (ColorScheme colorScheme : currentColorSchemes.values()) {
			if (colorScheme != null) {
				colorSchemeComboBoxModel.addElement(colorScheme);
			}
		}
		colorSchemeComboBoxModel
				.addElement(I18N.getGUILabel("plotter.configuration_dialog.color_scheme_dialog.add_new_scheme.label"));
		colorSchemeComboBox.setSelectedItem(getCurrentActiveColorScheme());

		calculateGradientPreview();

		checkIfButtonsEnabled();

		adaptPreviewPlots();

		adaptingModels = false;
	}

	private void adaptNominalPlot() {
		if (!initializing) {
			// change chart config
			EqualDataFractionGrouping edfg = (EqualDataFractionGrouping) nominalPlotConfig
					.getDimensionConfig(PlotDimension.COLOR).getGrouping();
			edfg.setBinCount(getCurrentActiveColorScheme().getColors().size());
			nominalPlotConfig.addColorSchemeAndSetActive(getCurrentActiveColorScheme());
			// nominalPlotter.updateChartPanelChart(false);
		}
	}

	private void adaptGradientPlot() {
		if (!initializing) {
			// change chart config
			gradientPlotConfig.addColorSchemeAndSetActive(getCurrentActiveColorScheme());
			// gradientPlotter.updateChartPanelChart(false);
		}
	}

	private void adaptPreviewPlots() {
		adaptNominalPlot();
		adaptGradientPlot();
	}

	/**
	 * check if category or scheme button have to be enabled
	 */
	private void checkIfButtonsEnabled() {
		removeCategoryColorButton.setEnabled(nominalColorListModel.getSize() > 2);
		boolean enableSchemeRemove = !((ColorScheme) colorSchemeComboBox.getSelectedItem()).getName()
				.equals(I18N.getGUILabel("plotter.default_color_scheme_name.label"));
		removeSchemeButton.setEnabled(colorSchemeComboBoxModel.getSize() > 2 && enableSchemeRemove);
		renameSchemeButton.setEnabled(enableSchemeRemove);
	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		PlotConfigurationChangeType type = change.getType();
		switch (type) {
			case COLOR_SCHEME:
				PlotConfiguration plotConfig = change.getSource();
				save(plotConfig.getColorSchemes(), plotConfig.getActiveColorScheme().getName());
				break;
			default:
		}
		return true;
	}
}
