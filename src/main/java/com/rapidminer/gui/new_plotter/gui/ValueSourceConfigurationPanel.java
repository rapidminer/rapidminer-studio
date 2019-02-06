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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.tree.TreePath;

import com.rapidminer.gui.new_plotter.ChartConfigurationException;
import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.IndicatorType;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.VisualizationType;
import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.configuration.ValueSource.SeriesUsageType;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.gui.cellrenderer.EnumComboBoxCellRenderer;
import com.rapidminer.gui.new_plotter.gui.dnd.DataTableColumnListTransferHandler;
import com.rapidminer.gui.popup.PopupAction;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent.PlotConfigurationChangeType;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.math.function.aggregation.AbstractAggregationFunction;
import com.rapidminer.tools.math.function.aggregation.AbstractAggregationFunction.AggregationFunctionType;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ValueSourceConfigurationPanel extends AbstractTreeSelectionDependentPanel {

	private static final long serialVersionUID = 1L;

	private JLabel seriesTypeLabel;
	private JComboBox<VisualizationType> seriesTypeComboBox;

	private ResourceLabel formatLabel;
	private JToggleButton formatConfigureButton;

	private JLabel aggregationLabel;
	private JComboBox<Object> aggregationComboBox;

	private JLabel windowingLabel;
	private JToggleButton windowingButton;

	private JLabel utilityIndicatorLabel;
	private JComboBox<IndicatorType> utilityIndicatorComboBox;
	private JCheckBox relativeIndicatorCheckBox;

	private JLabel firstUtilityAttributeLabel;
	private JComboBox<AggregationFunctionType> firstUtilityAggregationComboBox;
	private JTextField firstUtilityTextField;

	private JLabel secondUtilityAttributeLabel;
	private JTextField secondUtilityTextField;
	private JComboBox<AggregationFunctionType> secondUtilityAggregationComboBox;

	private JButton firstUtilityRemoveAttributeButton;
	private JButton secondUtilityRemoveAttributeButton;

	private SeriesFormatConfigurationPanel seriesTypeConfigurationPanel;
	private WindowingConfigurationContainer windowConfigurationPanel;

	private DefaultComboBoxModel<Object> aggregationComboBoxModel;

	private ExtendedHTMLJEditorPane configureGroupingButton;

	private final JTree plotConfigurationTree;

	private JLabel valueSourceLabelLabel;

	private JTextField valueSourceLabelTextfield;

	private JCheckBox automateLabelCheckBox;

	public ValueSourceConfigurationPanel(boolean smallIcons, JTree plotConfigurationTree,
			DataTableColumnListTransferHandler th, PlotInstance plotInstance) {
		super(plotConfigurationTree, plotInstance);
		this.plotConfigurationTree = plotConfigurationTree;

		seriesTypeConfigurationPanel = new SeriesFormatConfigurationPanel(smallIcons, plotConfigurationTree, plotInstance);
		addPlotInstanceChangeListener(seriesTypeConfigurationPanel);
		windowConfigurationPanel = new WindowingConfigurationContainer(plotConfigurationTree, plotInstance);
		addPlotInstanceChangeListener(windowConfigurationPanel);

		createComponents(plotConfigurationTree, th);
		registerAsPlotConfigurationListener();
		adaptGUI();
	}

	private void createComponents(JTree plotConfigTree, DataTableColumnListTransferHandler th) {

		// create renaming row
		{

			valueSourceLabelLabel = new JLabel(I18N.getGUILabel("plotter.configuration_dialog.dimension_axis_title.label"));

			JPanel textFieldAndCheckBoxPanel = new JPanel(new GridBagLayout());

			{

				// add text field
				{
					// add y-axis text field
					valueSourceLabelTextfield = new JTextField();
					valueSourceLabelLabel.setLabelFor(valueSourceLabelTextfield);
					valueSourceLabelTextfield.addKeyListener(new KeyListener() {

						@Override
						public void keyTyped(KeyEvent e) {
							return;
						}

						@Override
						public void keyReleased(KeyEvent e) {
							ValueSource selectedValueSource = getSelectedValueSource();
							if (selectedValueSource != null) {
								String newTitle = valueSourceLabelTextfield.getText();
								String titleText = selectedValueSource.getLabel();
								if (titleText != null && !titleText.equals(newTitle) || titleText == null
										&& newTitle.length() > 0) {
									if (newTitle.length() > 0) {
										selectedValueSource.setLabel(newTitle);
									} else {
										selectedValueSource.setLabel(null);
									}
								}
							}
						}

						@Override
						public void keyPressed(KeyEvent e) {
							return;
						}
					});
					valueSourceLabelTextfield.setPreferredSize(new Dimension(100, 22));

					GridBagConstraints itemConstraint = new GridBagConstraints();
					itemConstraint.weightx = 1.0;
					itemConstraint.fill = GridBagConstraints.HORIZONTAL;
					itemConstraint.gridwidth = GridBagConstraints.RELATIVE;

					textFieldAndCheckBoxPanel.add(valueSourceLabelTextfield, itemConstraint);
				}

				// add checkbox
				{
					automateLabelCheckBox = new JCheckBox(
							I18N.getGUILabel("plotter.configuration_dialog.automatic_axis_label.label"));
					automateLabelCheckBox.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							ValueSource selectedValueSource = getSelectedValueSource();
							if (selectedValueSource != null) {
								selectedValueSource.setAutoNaming(automateLabelCheckBox.isSelected());
							}
						}

					});

					GridBagConstraints itemConstraint = new GridBagConstraints();

					itemConstraint.weightx = 0.0;
					itemConstraint.fill = GridBagConstraints.NONE;
					itemConstraint.gridwidth = GridBagConstraints.REMAINDER;

					textFieldAndCheckBoxPanel.add(automateLabelCheckBox, itemConstraint);

				}

			}

			addTwoComponentRow(this, valueSourceLabelLabel, textFieldAndCheckBoxPanel);

		}

		{
			// create series type label
			seriesTypeLabel = new JLabel(I18N.getGUILabel("plotter.configuration_dialog.series_type.label"));
			seriesTypeLabel.setPreferredSize(new Dimension(80, 15));

			// create series type combobox
			seriesTypeComboBox = new JComboBox<>(VisualizationType.values());
			seriesTypeLabel.setLabelFor(seriesTypeComboBox);
			seriesTypeComboBox.setRenderer(new EnumComboBoxCellRenderer<>("plotter.series_type"));
			seriesTypeComboBox.setSelectedIndex(0);
			seriesTypeComboBox.addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					return;

				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					ValueSource selectedValueSource = getSelectedValueSource();
					if (selectedValueSource != null) {
						selectedValueSource.getSeriesFormat().setSeriesType(
								(VisualizationType) seriesTypeComboBox.getSelectedItem());
					}
				}

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
					return;
				}
			});

			// add series type row
			addTwoComponentRow(this, seriesTypeLabel, seriesTypeComboBox);
		}

		// add format button
		{
			formatLabel = new ResourceLabel("plotter.configuration_dialog.format_label");

			formatConfigureButton = new JToggleButton(new PopupAction(true, "plotter.configuration_dialog.format_button",
					seriesTypeConfigurationPanel, PopupAction.PopupPosition.HORIZONTAL));

			formatLabel.setLabelFor(formatConfigureButton);

			addTwoComponentRow(this, formatLabel, formatConfigureButton);

		}

		// add aggregation function
		{
			aggregationLabel = new ResourceLabel("plotter.configuration_dialog.aggregate_function");

			aggregationComboBoxModel = new DefaultComboBoxModel<>();
			aggregationComboBoxModel.addElement(I18N.getGUILabel("plotter.aggregation_function.NONE.label"));
			for (AggregationFunctionType type : AggregationFunctionType.values()) {
				aggregationComboBoxModel.addElement(type);
			}

			JPanel aggragationPanel = new JPanel(new GridBagLayout());
			{

				// create combo box
				aggregationComboBox = new JComboBox<>(aggregationComboBoxModel);
				aggregationLabel.setLabelFor(aggregationComboBox);
				aggregationComboBox.addPopupMenuListener(new PopupMenuListener() {

					@Override
					public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
						return;
					}

					@Override
					public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
						ValueSource selectedValueSource = getSelectedValueSource();
						if (selectedValueSource != null) {
							Object selectedItem = aggregationComboBox.getSelectedItem();

							boolean process = getPlotConfiguration().isProcessingEvents();  // save
																							// old
																							// state
							getPlotConfiguration().setProcessEvents(false); // set process events
																			// false

							if (selectedItem instanceof AggregationFunctionType) {
								AggregationFunctionType type = (AggregationFunctionType) selectedItem;
								selectedValueSource.setAggregationFunction(SeriesUsageType.MAIN_SERIES, type);
								selectedValueSource.setUseDomainGrouping(true);
								configureGroupingButton.setEnabled(true);
							} else {
								selectedValueSource.setUseDomainGrouping(false);
								selectedValueSource.setAggregationFunction(SeriesUsageType.MAIN_SERIES, null);
								configureGroupingButton.setEnabled(false);
							}

							getPlotConfiguration().setProcessEvents(process); // restore old
																				// processing state
						}
					}

					@Override
					public void popupMenuCanceled(PopupMenuEvent e) {
						return;

					}
				});
				aggregationComboBox
						.setSelectedItem(AbstractAggregationFunction.KNOWN_AGGREGATION_FUNCTION_NAMES[AbstractAggregationFunction.COUNT]);

				GridBagConstraints itemConstraint = new GridBagConstraints();
				itemConstraint.gridx = 0;
				itemConstraint.weightx = 1;
				itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
				itemConstraint.fill = GridBagConstraints.HORIZONTAL;
				itemConstraint.insets = new Insets(0, 0, 5, 5);

				aggragationPanel.add(aggregationComboBox, itemConstraint);

				final String content = I18N
						.getGUILabel("plotter.configuration_dialog.value_source_panel.show_grouping_configuration.label");
				configureGroupingButton = new ExtendedHTMLJEditorPane("", buildHTMLString("0000FF", content));
				configureGroupingButton.setEditable(false);
				configureGroupingButton.setFocusable(false);
				configureGroupingButton.setBackground(this.getBackground());
				configureGroupingButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
				configureGroupingButton.addMouseListener(new MouseListener() {

					@Override
					public void mouseReleased(MouseEvent e) {
						// Nothing to be done
					}

					@Override
					public void mousePressed(MouseEvent e) {
						// Nothing to be done
					}

					@Override
					public void mouseExited(MouseEvent e) {
						configureGroupingButton.setText(buildHTMLString("0000FF", content));
					}

					@Override
					public void mouseEntered(MouseEvent e) {
						configureGroupingButton.setText(buildHTMLString("000099", content));
					}

					@Override
					public void mouseClicked(MouseEvent e) {
						if (configureGroupingButton.isEnabled()) {
							PlotConfigurationTreeModel model = (PlotConfigurationTreeModel) plotConfigurationTree.getModel();
							TreePath pathToXDimension = new TreePath(model.getRoot());
							pathToXDimension = pathToXDimension.pathByAddingChild(model.getChild(model.getRoot(), 0));
							plotConfigurationTree.scrollPathToVisible(pathToXDimension);
							plotConfigurationTree.setSelectionPath(pathToXDimension);
						}
					}
				});

				itemConstraint = new GridBagConstraints();
				itemConstraint.gridx = 1;
				itemConstraint.weightx = 0.0;
				itemConstraint.gridwidth = GridBagConstraints.REMAINDER; // end row
				itemConstraint.fill = GridBagConstraints.HORIZONTAL;
				itemConstraint.insets = new Insets(0, 5, 5, 0);
				aggragationPanel.add(configureGroupingButton, itemConstraint);
			}

			addTwoComponentRow(this, aggregationLabel, aggragationPanel);

		}

		// add legend popup button
		{
			windowingLabel = new ResourceLabel("plotter.configuration_dialog.value_source_panel.windowing");

			windowingButton = new JToggleButton(new PopupAction(true, "plotter.configuration_dialog.configure_windowing",
					windowConfigurationPanel, PopupAction.PopupPosition.HORIZONTAL));
			windowingLabel.setLabelFor(windowingButton);

			addTwoComponentRow(this, windowingLabel, windowingButton);

		}

		// add error type combobox
		{
			utilityIndicatorLabel = new ResourceLabel("plotter.configuration_dialog.utility_indicator");

			JPanel utilityIndicatorPanel = new JPanel(new GridBagLayout());
			{

				// create combo box
				utilityIndicatorComboBox = new JComboBox<>(IndicatorType.values());
				utilityIndicatorLabel.setLabelFor(utilityIndicatorComboBox);
				utilityIndicatorComboBox.setRenderer(new EnumComboBoxCellRenderer<>("plotter.error_indicator"));
				utilityIndicatorComboBox.addPopupMenuListener(new PopupMenuListener() {

					@Override
					public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
						return;
					}

					@Override
					public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
						ValueSource selectedValueSource = getSelectedValueSource();
						if (selectedValueSource != null) {
							selectedValueSource.getSeriesFormat().setUtilityUsage(
									(IndicatorType) utilityIndicatorComboBox.getSelectedItem());
						}

					}

					@Override
					public void popupMenuCanceled(PopupMenuEvent e) {
						return;

					}
				});
				utilityIndicatorComboBox.setSelectedItem(IndicatorType.NONE);

				relativeIndicatorCheckBox = new JCheckBox(I18N.getGUILabel("plotter.configuration_dialog.relative.label"));
				relativeIndicatorCheckBox.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						ValueSource selectedValueSource = getSelectedValueSource();
						if (selectedValueSource != null) {
							selectedValueSource.setUseRelativeUtilities(relativeIndicatorCheckBox.isSelected());
						}
					}
				});

				GridBagConstraints itemConstraint = new GridBagConstraints();
				itemConstraint.gridx = 0;
				itemConstraint.weightx = 1;
				itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
				itemConstraint.fill = GridBagConstraints.HORIZONTAL;
				itemConstraint.insets = new Insets(0, 0, 5, 5);

				utilityIndicatorPanel.add(utilityIndicatorComboBox, itemConstraint);

				itemConstraint = new GridBagConstraints();
				itemConstraint.gridx = 1;
				itemConstraint.weightx = 0.0;
				itemConstraint.gridwidth = GridBagConstraints.REMAINDER; // end row
				itemConstraint.fill = GridBagConstraints.HORIZONTAL;
				itemConstraint.insets = new Insets(0, 5, 5, 0);
				utilityIndicatorPanel.add(relativeIndicatorCheckBox, itemConstraint);
			}

			addTwoComponentRow(this, utilityIndicatorLabel, utilityIndicatorPanel);
		}

		GridBagConstraints itemConstraint;
		// add first error attribute
		{
			firstUtilityAttributeLabel = new ResourceLabel("plotter.configuration_dialog.utility1");

			JPanel firstErrorAttributePanel = new JPanel(new GridBagLayout());
			{
				itemConstraint = new GridBagConstraints();

				// create attribute textfield
				firstUtilityTextField = new AttributeDropTextField(plotConfigTree, th, SeriesUsageType.INDICATOR_1);
				firstUtilityTextField.setPreferredSize(new Dimension(100, 18));
				firstUtilityAttributeLabel.setLabelFor(firstUtilityTextField);

				itemConstraint.fill = GridBagConstraints.HORIZONTAL;
				itemConstraint.weightx = 1.0;
				itemConstraint.insets = new Insets(0, 0, 0, 4);

				firstErrorAttributePanel.add(firstUtilityTextField, itemConstraint);

				firstUtilityRemoveAttributeButton = new JButton(new ResourceAction(true,
						"plotter.configuration_dialog.remove_button") {

					private static final long serialVersionUID = 1L;

					@Override
					public void loggedActionPerformed(ActionEvent e) {
						try {
							getSelectedValueSource().setDataTableColumn(SeriesUsageType.INDICATOR_1, null);
						} catch (ChartConfigurationException e1) {
							PlotConfigurationError plotConfigurationError = e1.getResponse().getErrors().get(0);
							SwingTools.showVerySimpleErrorMessage(plotConfigurationError.getErrorId(),
									plotConfigurationError.getMessageParameters());
						}
					}
				});

				itemConstraint.fill = GridBagConstraints.HORIZONTAL;
				itemConstraint.weightx = 0.0;
				itemConstraint.gridwidth = GridBagConstraints.REMAINDER;

				firstErrorAttributePanel.add(firstUtilityRemoveAttributeButton, itemConstraint);

			}

			// create aggregation combobox
			{
				firstUtilityAggregationComboBox = new JComboBox<>(AggregationFunctionType.values());
				firstUtilityAggregationComboBox.addPopupMenuListener(new PopupMenuListener() {

					@Override
					public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
						return;
					}

					@Override
					public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
						ValueSource selectedValueSource = getSelectedValueSource();
						if (selectedValueSource != null) {
							selectedValueSource.setAggregationFunction(SeriesUsageType.INDICATOR_1,
									(AggregationFunctionType) firstUtilityAggregationComboBox.getSelectedItem());
						}
					}

					@Override
					public void popupMenuCanceled(PopupMenuEvent e) {
						return;

					}
				});
			}

			itemConstraint = new GridBagConstraints();
			itemConstraint.gridx = 0;
			itemConstraint.weightx = 0.0;
			itemConstraint.gridwidth = 1;
			itemConstraint.insets = getStandardInsets();
			itemConstraint.anchor = GridBagConstraints.LINE_START;

			this.add(firstUtilityAttributeLabel, itemConstraint);

			itemConstraint = new GridBagConstraints();
			itemConstraint.gridx = 1;
			itemConstraint.weightx = 1.0;
			itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
			itemConstraint.fill = GridBagConstraints.HORIZONTAL;
			itemConstraint.insets = getStandardInsets();

			this.add(firstErrorAttributePanel, itemConstraint);

			itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 0.0;
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
			itemConstraint.anchor = GridBagConstraints.EAST;
			itemConstraint.fill = GridBagConstraints.NONE;
			itemConstraint.insets = getStandardInsets();

			this.add(firstUtilityAggregationComboBox, itemConstraint);

		}

		// add second error attribute
		{
			secondUtilityAttributeLabel = new ResourceLabel("plotter.configuration_dialog.utility2");

			JPanel secondErrorAttributePanel = new JPanel(new GridBagLayout());
			{
				itemConstraint = new GridBagConstraints();

				// create attribute textfield
				secondUtilityTextField = new AttributeDropTextField(plotConfigTree, th, SeriesUsageType.INDICATOR_2);
				secondUtilityTextField.setPreferredSize(new Dimension(100, 18));
				secondUtilityAttributeLabel.setLabelFor(secondUtilityTextField);

				itemConstraint.fill = GridBagConstraints.HORIZONTAL;
				itemConstraint.weightx = 1.0;
				itemConstraint.insets = new Insets(0, 0, 0, 4);

				secondErrorAttributePanel.add(secondUtilityTextField, itemConstraint);

				secondUtilityRemoveAttributeButton = new JButton(new ResourceAction(true,
						"plotter.configuration_dialog.remove_button") {

					private static final long serialVersionUID = 1L;

					@Override
					public void loggedActionPerformed(ActionEvent e) {
						try {
							getSelectedValueSource().setDataTableColumn(SeriesUsageType.INDICATOR_2, null);
						} catch (ChartConfigurationException e1) {
							PlotConfigurationError plotConfigurationError = e1.getResponse().getErrors().get(0);
							SwingTools.showVerySimpleErrorMessage(plotConfigurationError.getErrorId(),
									plotConfigurationError.getMessageParameters());
						}
					}
				});

				itemConstraint.fill = GridBagConstraints.NONE;
				itemConstraint.weightx = 0.0;
				itemConstraint.gridwidth = GridBagConstraints.REMAINDER;

				secondErrorAttributePanel.add(secondUtilityRemoveAttributeButton, itemConstraint);

			}

			// create aggregation combobox
			{
				secondUtilityAggregationComboBox = new JComboBox<>(AggregationFunctionType.values());
				secondUtilityAggregationComboBox.addPopupMenuListener(new PopupMenuListener() {

					@Override
					public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
						return;
					}

					@Override
					public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
						ValueSource selectedValueSource = getSelectedValueSource();
						if (selectedValueSource != null) {
							selectedValueSource.setAggregationFunction(SeriesUsageType.INDICATOR_2,
									(AggregationFunctionType) secondUtilityAggregationComboBox.getSelectedItem());
						}
					}

					@Override
					public void popupMenuCanceled(PopupMenuEvent e) {
						return;

					}
				});

			}

			itemConstraint = new GridBagConstraints();
			itemConstraint.gridx = 0;
			itemConstraint.weightx = 0.0;
			itemConstraint.gridwidth = 1;
			itemConstraint.insets = getStandardInsets();
			itemConstraint.anchor = GridBagConstraints.LINE_START;

			this.add(secondUtilityAttributeLabel, itemConstraint);

			itemConstraint = new GridBagConstraints();
			itemConstraint.gridx = 1;
			itemConstraint.weightx = 1.0;
			itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
			itemConstraint.fill = GridBagConstraints.HORIZONTAL;
			itemConstraint.insets = getStandardInsets();

			this.add(secondErrorAttributePanel, itemConstraint);

			itemConstraint = new GridBagConstraints();
			itemConstraint.weightx = 0.0;
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
			itemConstraint.anchor = GridBagConstraints.EAST;
			itemConstraint.fill = GridBagConstraints.NONE;
			itemConstraint.insets = getStandardInsets();

			this.add(secondUtilityAggregationComboBox, itemConstraint);

			// addThreeComponentRow(this, secondUtilityAttributeLabel, secondErrorAttributePanel,
			// secondUtilityAggregationComboBox);
		}

		// add spacer panel
		{
			JPanel spacerPanel = new JPanel();
			itemConstraint = new GridBagConstraints();
			itemConstraint.fill = GridBagConstraints.BOTH;
			itemConstraint.weightx = 1;
			itemConstraint.weighty = 1;
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
			this.add(spacerPanel, itemConstraint);
		}

	}

	private void useGroupingChanged(boolean grouped) {
		if (!grouped) {
			aggregationComboBox.setSelectedIndex(0);
			configureGroupingButton.setEnabled(false);
		}

		configureGroupingButton.setEnabled(true);

		windowingLabel.setEnabled(grouped);
		windowingButton.setEnabled(grouped);

		if (utilityIndicatorComboBox.getSelectedItem() != IndicatorType.NONE) {
			firstUtilityAggregationComboBox.setEnabled(grouped);
			secondUtilityAggregationComboBox.setEnabled(grouped);
		}
	}

	/**
	 * @param autoNaming
	 */
	private void autoNamingChanged(boolean autoNaming) {
		automateLabelCheckBox.setSelected(autoNaming);

		valueSourceLabelTextfield.setEnabled(!autoNaming);
	}

	/**
	 * @param label
	 */
	private void labelChanged(String label) {
		String oldText = valueSourceLabelTextfield.getText();
		if (!oldText.equals(label)) {
			if (label != null) {
				valueSourceLabelTextfield.setText(label);
			} else {
				valueSourceLabelTextfield.setText("");
			}
		}
	}

	private void firstUtilityDataTableColumnChanged(DataTableColumn column) {
		if (column != null) {
			firstUtilityTextField.setText(column.getName());
		} else {
			firstUtilityTextField.setText("");
		}
	}

	private void secondUtilityDataTableColumnChanged(DataTableColumn column) {
		if (column != null) {
			secondUtilityTextField.setText(column.getName());
		} else {
			secondUtilityTextField.setText("");
		}
	}

	/**
	 * @param hexColor
	 *            for example "000000" for black
	 * @param content
	 * @return
	 */
	private String buildHTMLString(String hexColor, String content) {
		StringBuilder builder = new StringBuilder();
		builder.append("<html>");
		builder.append("<div style=\"color:#");
		builder.append(hexColor);
		builder.append(";text-decoration:underline;font-family:Lucida Sans;font-size:12pt\">");
		builder.append(content);
		builder.append("</div>");
		builder.append("</html>");

		String output = builder.toString();
		return output;
	}

	private void errorIndicatorChanged(IndicatorType utilityUsage) {
		utilityIndicatorComboBox.setSelectedItem(utilityUsage);

		// update first error attribute
		boolean enableFirstUtility = false;
		boolean enableSecondUtility = false;

		if (utilityUsage != IndicatorType.NONE) {
			enableFirstUtility = true;
			enableSecondUtility = true;
		}

		relativeIndicatorCheckBox.setEnabled(enableFirstUtility);

		boolean useGrouping = false;
		ValueSource source = getSelectedValueSource();
		if (source != null) {
			useGrouping = source.isUsingDomainGrouping();
		}

		firstUtilityAggregationComboBox.setEnabled(enableFirstUtility && useGrouping);

		if (enableFirstUtility) {
			DataTableColumn dataTableColumn = getSelectedValueSource().getDataTableColumn(SeriesUsageType.INDICATOR_1);
			if (dataTableColumn != null) {
				firstUtilityTextField.setText(dataTableColumn.getName());
			}
		}

		// update second error attribute
		if (utilityUsage == IndicatorType.DIFFERENCE) {
			enableSecondUtility = false;
		}

		secondUtilityAggregationComboBox.setEnabled(enableSecondUtility && useGrouping);

	}

	private void firstUtilityAggregationChanged(AggregationFunctionType aggreagtionFunctionType) {
		if (aggreagtionFunctionType != null) {
			firstUtilityAggregationComboBox.setSelectedItem(aggreagtionFunctionType);
		} else {
			firstUtilityAggregationComboBox.setSelectedItem(AggregationFunctionType.average);
		}
	}

	private void secondUtilityAggregationChanged(AggregationFunctionType aggreagtionFunctionType) {
		if (aggreagtionFunctionType != null) {
			secondUtilityAggregationComboBox.setSelectedItem(aggreagtionFunctionType);
		} else {
			secondUtilityAggregationComboBox.setSelectedItem(AggregationFunctionType.average);
		}
	}

	private void seriesTypeSelectionChanged(VisualizationType seriesType) {
		seriesTypeComboBox.setSelectedItem(seriesType);
	}

	private void mainAggregationFunctionChanged(AggregationFunctionType aggreagtionFunctionType) {
		ValueSource selectedValueSource = getSelectedValueSource();
		boolean notGrouping = false;
		if (selectedValueSource != null) {
			notGrouping = !selectedValueSource.isUsingDomainGrouping();
		}
		if (aggreagtionFunctionType == null || notGrouping) {
			aggregationComboBox.setSelectedIndex(0);
			configureGroupingButton.setEnabled(false);
		} else {
			aggregationComboBox.setSelectedItem(aggreagtionFunctionType);
			configureGroupingButton.setEnabled(true);
		}
	}

	/**
	 * @param useRelative
	 */
	private void useRelativeUtilitiesChanged(Boolean useRelative) {
		relativeIndicatorCheckBox.setSelected(useRelative);
	}

	@Override
	protected void adaptGUI() {
		ValueSource selectedValueSource = getSelectedValueSource();
		if (selectedValueSource != null) {

			// set name and autonaming
			labelChanged(selectedValueSource.getLabel());
			autoNamingChanged(selectedValueSource.isAutoNaming());

			SeriesFormat format = selectedValueSource.getSeriesFormat();

			// update series type combo box
			seriesTypeSelectionChanged(format.getSeriesType());

			// update grouping depended stuff
			boolean usingDomainGrouping = selectedValueSource.isUsingDomainGrouping();
			useGroupingChanged(usingDomainGrouping);

			// update aggregation function
			mainAggregationFunctionChanged(selectedValueSource.getAggregationFunctionType(SeriesUsageType.MAIN_SERIES));

			// update error type combobox
			errorIndicatorChanged(format.getUtilityUsage());

			// update relative checkbox
			useRelativeUtilitiesChanged(selectedValueSource.isUsingRelativeIndicator());

			// set first utility aggregation and column
			firstUtilityAggregationChanged(selectedValueSource.getAggregationFunctionType(SeriesUsageType.INDICATOR_1));
			firstUtilityDataTableColumnChanged(selectedValueSource.getDataTableColumn(SeriesUsageType.INDICATOR_1));

			// set second utility aggregation and column
			secondUtilityAggregationChanged(selectedValueSource.getAggregationFunctionType(SeriesUsageType.INDICATOR_2));
			secondUtilityDataTableColumnChanged(selectedValueSource.getDataTableColumn(SeriesUsageType.INDICATOR_2));

		}
	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		PlotConfigurationChangeType type = change.getType();
		if (type == PlotConfigurationChangeType.RANGE_AXIS_CONFIG_CHANGED) {
			adaptGUI();
		}
		if (type == PlotConfigurationChangeType.META_CHANGE) {
			processPlotConfigurationMetaChange(change);
		}
		return true;
	}
}
