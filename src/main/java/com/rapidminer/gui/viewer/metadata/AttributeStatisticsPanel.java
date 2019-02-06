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
package com.rapidminer.gui.viewer.metadata;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.tools.AttributeGuiTools;
import com.rapidminer.gui.tools.AttributeGuiTools.ColorScope;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.viewer.metadata.actions.AttributePopupMenu;
import com.rapidminer.gui.viewer.metadata.actions.CopyAttributeNameAction;
import com.rapidminer.gui.viewer.metadata.actions.CopyDateTimeFromValueAction;
import com.rapidminer.gui.viewer.metadata.actions.CopyDateTimeUntilValueAction;
import com.rapidminer.gui.viewer.metadata.actions.CopyNumAvgValueAction;
import com.rapidminer.gui.viewer.metadata.actions.CopyNumDeviationValueAction;
import com.rapidminer.gui.viewer.metadata.actions.CopyNumMaximumValueAction;
import com.rapidminer.gui.viewer.metadata.actions.CopyNumMinimumValueAction;
import com.rapidminer.gui.viewer.metadata.actions.OpenChartAction;
import com.rapidminer.gui.viewer.metadata.actions.ShowNomValueAction;
import com.rapidminer.gui.viewer.metadata.event.AttributeStatisticsEvent;
import com.rapidminer.gui.viewer.metadata.event.AttributeStatisticsEventListener;
import com.rapidminer.gui.viewer.metadata.model.AbstractAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.DateTimeAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.NominalAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.NumericalAttributeStatisticsModel;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.ValueAndCount;


/**
 * GUI display for meta data statistics of an {@link Attribute} backed by an
 * {@link AbstractAttributeStatisticsModel}.
 * <p>
 * The reason for this being just one class instead of 3 classes extending an abstract one is
 * simple: performance. Removing/adding these panels from/to the {@link MetaDataStatisticsViewer}
 * whenever sorting changes is very costly and takes too much time. Switching the models (and thus
 * basically only updating {@link JLabel}s) is much quicker, however we cannot switch the model
 * between numerical/nominal/date_time attributes when there is not one universal GUI for all 3
 * different attribute value types.
 *
 * @author Marco Boeck
 *
 */
public class AttributeStatisticsPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/** the font size of header labels */
	private static final float FONT_SIZE_LABEL_HEADER = 10;

	/** the font size of value labels */
	private static final float FONT_SIZE_LABEL_VALUE = 14;

	/** the identifier for custom special attributes */
	public static final String GENERIC_SPECIAL_NAME = "special";

	/** the dimension for the attribute name label */
	private static final Dimension DIMENSION_LABEL_ATTRIBUTE = new Dimension(230, 30);

	/** the dimension for the attribute type label */
	private static final Dimension DIMENSION_LABEL_TYPE = new Dimension(90, 20);

	/** the dimension for the attribute missings label */
	private static final Dimension DIMENSION_LABEL_MISSINGS = new Dimension(75, 20);

	/** the dimension for the attribute construction label */
	private static final Dimension DIMENSION_LABEL_CONSTRUCTION = new Dimension(75, 20);

	/** the dimension for chart panels in an enlarged state */
	private static final Dimension DIMENSION_CHART_PANEL_ENLARGED = new Dimension(200, 100);

	/** preferred size of a date_time panel so each column is aligned vertically */
	private static final Dimension DIMENSION_PANEL_DATE_PREF_SIZE = new Dimension(165, 50);

	/** preferred size of a numerical panel so each column is aligned vertically */
	private static final Dimension DIMENSION_PANEL_NUMERIC_PREF_SIZE_ENLARGED = new Dimension(110, 50);

	/** preferred size of a numerical panel so each column is aligned vertically */
	private static final Dimension DIMENSION_PANEL_NUMERIC_PREF_SIZE = new Dimension(165, 50);

	/** preferred size of a numerical panel so each column is aligned vertically */
	private static final Dimension DIMENSION_PANEL_NOMINAL_PREF_SIZE = new Dimension(165, 50);

	/** the dimension of the the filler which is used to correctly align the nominal value column */
	private static final Dimension DIMENSION_FILLER_NOMINAL_VALUE_SIZE = new Dimension(1, 20);

	/** a transparent color */
	private static final Color COLOR_TRANSPARENT = new Color(255, 255, 255, 0);

	/** {@value} */
	private static final String LABEL_DOTS = "...";

	/** used to open an histogram chart for an attribute */
	private static final OpenChartAction OPEN_CHART_ACTION = new OpenChartAction();

	/** the identifier for the numerical card */
	private static final String CARD_NUMERICAL = "numericalCard";

	/** the identifier for the nominal card */
	private static final String CARD_NOMINAL = "nominalCard";

	/** the identifier for the date_time card */
	private static final String CARD_DATE_TIME = "dateTimeCard";

	/** the popup shown on {@link AttributeStatisticsPanel} for numerical attributes */
	private static JPopupMenu popupAttributeNumericalStatPanel;

	/** the popup shown on {@link AttributeStatisticsPanel} for nominal attributes */
	private static JPopupMenu popupAttributeNominalStatPanel;

	/** the popup shown on {@link AttributeStatisticsPanel} for date_time attributes */
	private static JPopupMenu popupAttributeDateTimeStatPanel;

	static {
		// populate global popup menus (used to reduce performance issues so not PAGE_SIZE * 3 popup
		// menues are created)
		popupAttributeNumericalStatPanel = new AttributePopupMenu();
		popupAttributeNumericalStatPanel.add(new CopyAttributeNameAction());
		popupAttributeNumericalStatPanel.add(new CopyNumAvgValueAction());
		popupAttributeNumericalStatPanel.add(new CopyNumDeviationValueAction());
		popupAttributeNumericalStatPanel.add(new CopyNumMinimumValueAction());
		popupAttributeNumericalStatPanel.add(new CopyNumMaximumValueAction());
		popupAttributeNumericalStatPanel.addSeparator();
		popupAttributeNumericalStatPanel.add(OPEN_CHART_ACTION);

		popupAttributeNominalStatPanel = new AttributePopupMenu();
		popupAttributeNominalStatPanel.add(new CopyAttributeNameAction());
		popupAttributeNominalStatPanel.addSeparator();
		popupAttributeNominalStatPanel.add(new ShowNomValueAction(null));
		popupAttributeNominalStatPanel.add(OPEN_CHART_ACTION);

		popupAttributeDateTimeStatPanel = new AttributePopupMenu();
		popupAttributeDateTimeStatPanel.add(new CopyAttributeNameAction());
		popupAttributeDateTimeStatPanel.add(new CopyDateTimeFromValueAction());
		popupAttributeDateTimeStatPanel.add(new CopyDateTimeUntilValueAction());
		// popupAttributeDateTimeStatPanel.addSeparator();
		popupAttributeDateTimeStatPanel.add(OPEN_CHART_ACTION);
	}

	/** true if the mouse is hovering over this panel */
	private boolean hovered;

	/** the mouse listener for this panel to notice hovering/enlarge/shrink/popup requests */
	private MouseAdapter enlargeAndHoverAndPopupMouseAdapter;

	/** list containing all panels which display a chart */
	protected List<JPanel> listOfChartPanels;

	/** list containing all panels which should only displayed if the model is enlarged */
	private List<JPanel> listOfNumStatPanels;

	/** list containing all panels which should only displayed if the model is enlarged */
	private List<JPanel> listOfAdditionalNumStatPanels;

	/** label displaying the attribute header (special role) if it has one */
	private JLabel labelAttHeader;

	/** label displaying the attribute name */
	private JLabel labelAttName;

	/** panel in which name and role reside */
	private JPanel panelAttName;

	/** label displaying the attribute type */
	private JLabel labelAttType;

	/** label displaying the minimum value */
	private JLabel labelStatsMin;

	/** label displaying the maximum value */
	private JLabel labelStatsMax;

	/** label displaying the average value */
	private JLabel labelStatsAvg;

	/** label displaying the deviation */
	private JLabel labelStatsDeviation;

	/** label displaying the missing value count */
	private JLabel labelStatsMissing;

	/** the panel containing the attribute construction label */
	private JPanel panelStatsConstruction;

	/** label displaying the attribute construction value */
	private JLabel labelStatsConstruction;

	/** label displaying the least occuring nominal value */
	private JLabel labelStatsLeast;

	/** label displaying the most occuring nominal value */
	private JLabel labelStatsMost;

	/** label displaying the nominal values */
	private JLabel labelStatsValues;

	/** the details button for nominal values to open the nominal value details dialog */
	private JButton detailsButton;

	/** label displaying the duration value */
	private JLabel labelStatsDuration;

	/** label displaying the from value */
	private JLabel labelStatsFrom;

	/** label displaying the until value */
	private JLabel labelStatsUntil;

	/** label displaying the expansion status of the model */
	private JLabel labelStatsExp;

	/** the panel which contains the cards for the different types */
	private JPanel cardStatsPanel;

	/** the card layout used to switch between stat panels */
	private CardLayout cardLayout;

	/** this component is used to fix the alignment for the nominal value panel */
	private Component nominalValueFiller;

	/** the {@link AbstractAttributeStatisticsModel} backing the GUI */
	private AbstractAttributeStatisticsModel model;

	/** the listener which listens on the model for changes */
	private AttributeStatisticsEventListener listener;

	/**
	 * Creates a new {@link AttributeStatisticsPanel} instance. Before displaying the panel, an
	 * {@link AbstractAttributeStatisticsModel} should be set via
	 * {@link #setModel(AbstractAttributeStatisticsModel, boolean)}.
	 *
	 */
	public AttributeStatisticsPanel() {
		listOfChartPanels = new LinkedList<>();
		listOfNumStatPanels = new LinkedList<>();
		listOfAdditionalNumStatPanels = new LinkedList<>();

		// create listener which listens for hovering/enlarge mouse events on this panel
		enlargeAndHoverAndPopupMouseAdapter = new MouseAdapter() {

			@Override
			public void mousePressed(final MouseEvent e) {
				// only popup trigger for popup menu
				if (e.isPopupTrigger()) {
					handlePopup(e);
				}

				// only left mouse button to enlarge
				if (!SwingUtilities.isLeftMouseButton(e)) {
					return;
				}

				// little hack so hovering over the details button does not remove the hover effect
				// (because MouseExited is called)
				// but clicking the button is still possible and does not enlarge the panel
				if (e.getSource() instanceof JButton) {
					((JButton) e.getSource()).doClick();
					return;
				}

				// change enlarged status
				if (getModel() != null) {
					getModel().setEnlarged(!getModel().isEnlarged());
				}
			}

			@Override
			public void mouseExited(final MouseEvent e) {
				if (SwingTools.isMouseEventExitedToChildComponents(AttributeStatisticsPanel.this, e)) {
					// we are still hovering over the ASP, just a child component
					return;
				}
				hovered = false;
				repaint();
			}

			@Override
			public void mouseEntered(final MouseEvent e) {
				hovered = true;
				repaint();
			}

			@Override
			public void mouseReleased(final MouseEvent e) {
				if (e.isPopupTrigger()) {
					handlePopup(e);
				}
			}

			/**
			 * Handles the popup click event.
			 *
			 * @param e
			 */
			private void handlePopup(final MouseEvent e) {
				if (model.getAttribute().isNumerical()) {
					popupAttributeNumericalStatPanel.show(e.getComponent(), e.getX(), e.getY());
				} else if (model.getAttribute().isNominal()) {
					popupAttributeNominalStatPanel.show(e.getComponent(), e.getX(), e.getY());
				} else if (model.getAttribute().isDateTime()) {
					popupAttributeDateTimeStatPanel.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		};

		// create listener which listens for AttributeStatisticsEvents on the model
		listener = new AttributeStatisticsEventListener() {

			@Override
			public void modelChanged(final AttributeStatisticsEvent e) {
				switch (e.getEventType()) {
					case ALTERNATING_CHANGED:
						repaint();
						break;
					case ENLARGED_CHANGED:
						updateCharts();
						updateExpandInfoLabel();
						updateVisibilityOfChartPanels();
						if (AttributeStatisticsPanel.this.model.getAttribute().isNumerical()) {
							updateVisibilityOfNumStatPanels();
						}
						if (AttributeStatisticsPanel.this.model.getAttribute().isNominal()) {
							displayNominalValues();
						}
						break;
					case SHOW_CONSTRUCTION_CHANGED:
						panelStatsConstruction.setVisible(model.isShowConstruction());
						break;
					case STATISTICS_CHANGED:
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								AbstractAttributeStatisticsModel model = AttributeStatisticsPanel.this.model;
								if (model.getAttribute().isNumerical()) {
									updateNumericalElements(model);
								} else if (model.getAttribute().isNominal()) {
									updateNominalElements(model);
								} else {
									updateDateTimeElements(model);
								}
							}

						});
						break;
					default:
				}
			}
		};

		initGUI();
	}

	/**
	 * Initializes the GUI.
	 *
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initGUI() {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		// add expansion arrow
		labelStatsExp = new JLabel(MetaDataStatisticsViewer.ICON_ARROW_DOWN, SwingConstants.RIGHT);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(3, 10, 3, 10);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weighty = 1.0;
		gbc.gridheight = 2;
		add(labelStatsExp, gbc);

		// add attribute name
		panelAttName = new JPanel();
		panelAttName.setLayout(new BoxLayout(panelAttName, BoxLayout.PAGE_AXIS));
		panelAttName.setOpaque(false);
		// this border is to visualize that the name column can be enlarged/shrinked
		panelAttName.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));

		labelAttHeader = new JLabel(LABEL_DOTS);
		labelAttHeader.setFont(labelAttHeader.getFont().deriveFont(FONT_SIZE_LABEL_HEADER));
		labelAttHeader.setForeground(Color.GRAY);
		panelAttName.add(labelAttHeader);

		labelAttName = new JLabel(LABEL_DOTS);
		labelAttName.setFont(labelAttName.getFont().deriveFont(Font.BOLD, FONT_SIZE_LABEL_VALUE));
		labelAttName.setMinimumSize(DIMENSION_LABEL_ATTRIBUTE);
		labelAttName.setPreferredSize(DIMENSION_LABEL_ATTRIBUTE);
		panelAttName.add(labelAttName);

		gbc.gridx += 1;
		gbc.insets = new Insets(3, 5, 3, 10);
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.0;
		gbc.weighty = 1.0;
		gbc.gridheight = 2;
		add(panelAttName, gbc);

		// create value type name and bring it to a nice to read format (aka uppercase first letter
		// and replace '_' with ' '
		gbc.gridx += 1;
		gbc.insets = new Insets(5, 15, 5, 10);
		labelAttType = new JLabel(LABEL_DOTS);
		labelAttType.setMinimumSize(DIMENSION_LABEL_TYPE);
		labelAttType.setPreferredSize(DIMENSION_LABEL_TYPE);
		add(labelAttType, gbc);

		// missings panel
		JPanel panelStatsMissing = new JPanel();
		panelStatsMissing.setLayout(new BoxLayout(panelStatsMissing, BoxLayout.PAGE_AXIS));
		panelStatsMissing.setOpaque(false);

		labelStatsMissing = new JLabel(LABEL_DOTS);
		labelStatsMissing.setMinimumSize(DIMENSION_LABEL_MISSINGS);
		labelStatsMissing.setPreferredSize(DIMENSION_LABEL_MISSINGS);
		panelStatsMissing.add(labelStatsMissing);

		gbc.gridx += 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		add(panelStatsMissing, gbc);

		// chart panel(s) (only visible when enlarged)
		JPanel chartPanel = new JPanel(new BorderLayout());
		chartPanel.setBackground(COLOR_TRANSPARENT);
		chartPanel.setOpaque(false);
		listOfChartPanels.add(chartPanel);
		updateVisibilityOfChartPanels();

		gbc.fill = GridBagConstraints.NONE;
		gbc.weighty = 0.0;
		gbc.insets = new Insets(0, 10, 0, 10);
		for (JPanel panel : listOfChartPanels) {
			gbc.gridx += 1;
			add(panel, gbc);
		}

		// (hidden) construction panel
		String constructionLabel = I18N.getMessage(I18N.getGUIBundle(),
				"gui.label.attribute_statistics.statistics.construction.label");
		panelStatsConstruction = new JPanel();
		panelStatsConstruction.setLayout(new BoxLayout(panelStatsConstruction, BoxLayout.PAGE_AXIS));
		panelStatsConstruction.setOpaque(false);
		panelStatsConstruction.setVisible(false);

		JLabel labelConstructionHeader = new JLabel(constructionLabel);
		labelConstructionHeader.setFont(labelConstructionHeader.getFont().deriveFont(FONT_SIZE_LABEL_HEADER));
		labelConstructionHeader.setForeground(Color.GRAY);
		panelStatsConstruction.add(labelConstructionHeader);

		labelStatsConstruction = new JLabel(LABEL_DOTS);
		labelStatsConstruction.setFont(labelStatsConstruction.getFont().deriveFont(FONT_SIZE_LABEL_VALUE));
		labelStatsConstruction.setMinimumSize(DIMENSION_LABEL_CONSTRUCTION);
		labelStatsConstruction.setPreferredSize(DIMENSION_LABEL_CONSTRUCTION);
		panelStatsConstruction.add(labelStatsConstruction);

		gbc.gridx += 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		add(panelStatsConstruction, gbc);

		// statistics panel, contains different statistics panels for numerical/nominal/date_time on
		// a card layout
		// needed to switch between for model swapping
		cardStatsPanel = new JPanel();
		cardStatsPanel.setOpaque(false);
		cardLayout = new CardLayout();
		cardStatsPanel.setLayout(cardLayout);

		// numerical version
		JPanel statsNumPanel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints gbcStatPanel = new GridBagConstraints();
		statsNumPanel.setLayout(layout);
		statsNumPanel.setOpaque(false);

		String avgLabel = I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.avg.label");
		String devianceLabel = I18N.getMessage(I18N.getGUIBundle(),
				"gui.label.attribute_statistics.statistics.variance.label");
		String minLabel = I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.min.label");
		String maxLabel = I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.max.label");

		// min value panel
		JPanel panelStatsMin = new JPanel();
		panelStatsMin.setLayout(new BoxLayout(panelStatsMin, BoxLayout.PAGE_AXIS));
		panelStatsMin.setOpaque(false);

		JLabel labelMinHeader = new JLabel(minLabel);
		labelMinHeader.setFont(labelMinHeader.getFont().deriveFont(FONT_SIZE_LABEL_HEADER));
		labelMinHeader.setForeground(Color.GRAY);
		panelStatsMin.add(labelMinHeader);

		labelStatsMin = new JLabel(LABEL_DOTS);
		labelStatsMin.setFont(labelStatsMin.getFont().deriveFont(FONT_SIZE_LABEL_VALUE));
		listOfNumStatPanels.add(panelStatsMin);
		panelStatsMin.add(labelStatsMin);

		// max value panel
		JPanel panelStatsMax = new JPanel();
		panelStatsMax.setLayout(new BoxLayout(panelStatsMax, BoxLayout.PAGE_AXIS));
		panelStatsMax.setOpaque(false);

		JLabel labelMaxHeader = new JLabel(maxLabel);
		labelMaxHeader.setFont(labelMaxHeader.getFont().deriveFont(FONT_SIZE_LABEL_HEADER));
		labelMaxHeader.setForeground(Color.GRAY);
		panelStatsMax.add(labelMaxHeader);

		labelStatsMax = new JLabel(LABEL_DOTS);
		labelStatsMax.setFont(labelStatsMax.getFont().deriveFont(FONT_SIZE_LABEL_VALUE));
		listOfNumStatPanels.add(panelStatsMax);
		panelStatsMax.add(labelStatsMax);

		// average value panel
		JPanel panelStatsAvg = new JPanel();
		panelStatsAvg.setLayout(new BoxLayout(panelStatsAvg, BoxLayout.PAGE_AXIS));
		panelStatsAvg.setOpaque(false);

		JLabel labelAvgHeader = new JLabel(avgLabel);
		labelAvgHeader.setFont(labelAvgHeader.getFont().deriveFont(FONT_SIZE_LABEL_HEADER));
		labelAvgHeader.setForeground(Color.GRAY);
		panelStatsAvg.add(labelAvgHeader);

		labelStatsAvg = new JLabel(LABEL_DOTS);
		labelStatsAvg.setFont(labelStatsAvg.getFont().deriveFont(FONT_SIZE_LABEL_VALUE));
		listOfNumStatPanels.add(panelStatsAvg);
		panelStatsAvg.add(labelStatsAvg);

		// deviance value panel
		JPanel panelStatsDeviance = new JPanel();
		panelStatsDeviance.setLayout(new BoxLayout(panelStatsDeviance, BoxLayout.PAGE_AXIS));
		panelStatsDeviance.setOpaque(false);

		JLabel labelDevianceHeader = new JLabel(devianceLabel);
		labelDevianceHeader.setFont(labelDevianceHeader.getFont().deriveFont(FONT_SIZE_LABEL_HEADER));
		labelDevianceHeader.setForeground(Color.GRAY);
		panelStatsDeviance.add(labelDevianceHeader);

		labelStatsDeviation = new JLabel(LABEL_DOTS);
		labelStatsDeviation.setFont(labelStatsDeviation.getFont().deriveFont(FONT_SIZE_LABEL_VALUE));
		panelStatsDeviance.add(labelStatsDeviation);

		// the deviance value panel should only be visible if the model is enlarged
		listOfAdditionalNumStatPanels.add(panelStatsDeviance);
		listOfNumStatPanels.add(panelStatsDeviance);
		updateVisibilityOfNumStatPanels();

		// add sub panels to stats panel
		gbcStatPanel.gridx = 0;
		gbcStatPanel.weightx = 0.0;
		gbcStatPanel.fill = GridBagConstraints.NONE;
		gbcStatPanel.insets = new Insets(0, 0, 0, 6);
		panelStatsMin.setPreferredSize(DIMENSION_PANEL_NUMERIC_PREF_SIZE);
		statsNumPanel.add(panelStatsMin, gbcStatPanel);
		gbcStatPanel.gridx += 1;
		panelStatsMax.setPreferredSize(DIMENSION_PANEL_NUMERIC_PREF_SIZE);
		statsNumPanel.add(panelStatsMax, gbcStatPanel);
		gbcStatPanel.gridx += 1;
		panelStatsAvg.setPreferredSize(DIMENSION_PANEL_NUMERIC_PREF_SIZE);
		statsNumPanel.add(panelStatsAvg, gbcStatPanel);
		gbcStatPanel.gridx += 1;
		panelStatsDeviance.setPreferredSize(DIMENSION_PANEL_NUMERIC_PREF_SIZE);
		statsNumPanel.add(panelStatsDeviance, gbcStatPanel);
		gbcStatPanel.gridx += 1;
		gbcStatPanel.weightx = 1.0;
		gbcStatPanel.fill = GridBagConstraints.HORIZONTAL;
		gbcStatPanel.anchor = GridBagConstraints.EAST;
		statsNumPanel.add(new JLabel(), gbcStatPanel);
		cardStatsPanel.add(statsNumPanel, CARD_NUMERICAL);

		// nominal version
		JPanel statsNomPanel = new JPanel();
		statsNomPanel.setLayout(layout);
		statsNomPanel.setOpaque(false);
		String leastLabel = I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.least.label");
		String mostLabel = I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.most.label");
		String valuesLabel = I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.values.label");

		// least panel
		JPanel panelStatsLeast = new JPanel();
		panelStatsLeast.setLayout(new BoxLayout(panelStatsLeast, BoxLayout.PAGE_AXIS));
		panelStatsLeast.setOpaque(false);

		JLabel labelLeastHeader = new JLabel(leastLabel);
		labelLeastHeader.setFont(labelLeastHeader.getFont().deriveFont(FONT_SIZE_LABEL_HEADER));
		labelLeastHeader.setForeground(Color.GRAY);
		labelLeastHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
		panelStatsLeast.add(labelLeastHeader);

		labelStatsLeast = new JLabel(LABEL_DOTS);
		labelStatsLeast.setFont(labelStatsLeast.getFont().deriveFont(FONT_SIZE_LABEL_VALUE));
		panelStatsLeast.add(labelStatsLeast);

		// most panel
		JPanel panelStatsMost = new JPanel();
		panelStatsMost.setLayout(new BoxLayout(panelStatsMost, BoxLayout.PAGE_AXIS));
		panelStatsMost.setOpaque(false);

		JLabel labelMostHeader = new JLabel(mostLabel);
		labelMostHeader.setFont(labelMostHeader.getFont().deriveFont(FONT_SIZE_LABEL_HEADER));
		labelMostHeader.setForeground(Color.GRAY);
		labelMostHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
		panelStatsMost.add(labelMostHeader);

		labelStatsMost = new JLabel(LABEL_DOTS);
		labelStatsMost.setFont(labelStatsMost.getFont().deriveFont(FONT_SIZE_LABEL_VALUE));
		panelStatsMost.add(labelStatsMost);

		// values panel
		JPanel panelStatsValues = new JPanel();
		panelStatsValues.setLayout(new BoxLayout(panelStatsValues, BoxLayout.PAGE_AXIS));
		panelStatsValues.setOpaque(false);

		JLabel labelValuesHeader = new JLabel(valuesLabel);
		labelValuesHeader.setFont(labelValuesHeader.getFont().deriveFont(FONT_SIZE_LABEL_HEADER));
		labelValuesHeader.setForeground(Color.GRAY);
		labelValuesHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
		panelStatsValues.add(labelValuesHeader);

		labelStatsValues = new JLabel(LABEL_DOTS);
		labelStatsValues.setFont(labelStatsValues.getFont().deriveFont(FONT_SIZE_LABEL_VALUE));
		panelStatsValues.add(labelStatsValues);

		nominalValueFiller = Box.createRigidArea(DIMENSION_FILLER_NOMINAL_VALUE_SIZE);
		panelStatsValues.add(nominalValueFiller);

		detailsButton = new JButton(new ShowNomValueAction(this));
		detailsButton.setVisible(false);
		detailsButton.setOpaque(false);
		detailsButton.setContentAreaFilled(false);
		detailsButton.setBorderPainted(false);
		detailsButton.addMouseListener(enlargeAndHoverAndPopupMouseAdapter);
		detailsButton.setHorizontalAlignment(SwingConstants.LEFT);
		detailsButton.setHorizontalTextPosition(SwingConstants.LEFT);
		detailsButton.setIcon(null);
		Font font = detailsButton.getFont();
		Map attributes = font.getAttributes();
		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		detailsButton.setFont(font.deriveFont(attributes));
		panelStatsValues.add(detailsButton);
		panelStatsValues.add(new JLabel());

		// add sub panel to stats panel
		gbcStatPanel.gridx = 0;
		gbcStatPanel.weightx = 0.0;
		gbcStatPanel.fill = GridBagConstraints.NONE;
		gbcStatPanel.insets = new Insets(0, 0, 0, 6);
		panelStatsLeast.setPreferredSize(DIMENSION_PANEL_NOMINAL_PREF_SIZE);
		statsNomPanel.add(panelStatsLeast, gbcStatPanel);
		gbcStatPanel.gridx += 1;
		panelStatsMost.setPreferredSize(DIMENSION_PANEL_NOMINAL_PREF_SIZE);
		statsNomPanel.add(panelStatsMost, gbcStatPanel);
		gbcStatPanel.gridx += 1;
		statsNomPanel.add(panelStatsValues, gbcStatPanel);
		gbcStatPanel.gridx += 1;
		gbcStatPanel.weightx = 1.0;
		gbcStatPanel.fill = GridBagConstraints.HORIZONTAL;
		statsNomPanel.add(new JLabel(), gbcStatPanel);
		cardStatsPanel.add(statsNomPanel, CARD_NOMINAL);

		// date_time version
		JPanel statsDateTimePanel = new JPanel();
		statsDateTimePanel.setLayout(layout);
		statsDateTimePanel.setOpaque(false);

		String durationLabel = I18N.getMessage(I18N.getGUIBundle(),
				"gui.label.attribute_statistics.statistics.duration.label");
		String fromLabel = I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.from.label");
		String untilLabel = I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.until.label");

		// min value panel
		JPanel panelStatsFrom = new JPanel();
		panelStatsFrom.setLayout(new BoxLayout(panelStatsFrom, BoxLayout.PAGE_AXIS));
		panelStatsFrom.setOpaque(false);

		JLabel labelFromHeader = new JLabel(fromLabel);
		labelFromHeader.setFont(labelFromHeader.getFont().deriveFont(FONT_SIZE_LABEL_HEADER));
		labelFromHeader.setForeground(Color.GRAY);
		panelStatsFrom.add(labelFromHeader);

		labelStatsFrom = new JLabel(LABEL_DOTS);
		labelStatsFrom.setFont(labelStatsFrom.getFont().deriveFont(FONT_SIZE_LABEL_VALUE));
		panelStatsFrom.add(labelStatsFrom);

		// until value panel
		JPanel panelStatsUntil = new JPanel();
		panelStatsUntil.setLayout(new BoxLayout(panelStatsUntil, BoxLayout.PAGE_AXIS));
		panelStatsUntil.setOpaque(false);

		JLabel labelUntilHeader = new JLabel(untilLabel);
		labelUntilHeader.setFont(labelUntilHeader.getFont().deriveFont(FONT_SIZE_LABEL_HEADER));
		labelUntilHeader.setForeground(Color.GRAY);
		panelStatsUntil.add(labelUntilHeader);

		labelStatsUntil = new JLabel(LABEL_DOTS);
		labelStatsUntil.setFont(labelStatsUntil.getFont().deriveFont(FONT_SIZE_LABEL_VALUE));
		panelStatsUntil.add(labelStatsUntil);

		// duration value panel
		JPanel panelStatsDuration = new JPanel();
		panelStatsDuration.setLayout(new BoxLayout(panelStatsDuration, BoxLayout.PAGE_AXIS));
		panelStatsDuration.setOpaque(false);

		JLabel labelDurationHeader = new JLabel(durationLabel);
		labelDurationHeader.setFont(labelDurationHeader.getFont().deriveFont(FONT_SIZE_LABEL_HEADER));
		labelDurationHeader.setForeground(Color.GRAY);
		panelStatsDuration.add(labelDurationHeader);

		labelStatsDuration = new JLabel(LABEL_DOTS);
		labelStatsDuration.setFont(labelStatsDuration.getFont().deriveFont(FONT_SIZE_LABEL_VALUE));
		panelStatsDuration.add(labelStatsDuration);

		// add sub panels to stats panel
		gbcStatPanel.gridx = 0;
		gbcStatPanel.weightx = 0.0;
		gbcStatPanel.fill = GridBagConstraints.NONE;
		gbcStatPanel.insets = new Insets(0, 0, 0, 6);
		panelStatsFrom.setPreferredSize(DIMENSION_PANEL_DATE_PREF_SIZE);

		statsDateTimePanel.add(panelStatsFrom, gbcStatPanel);
		gbcStatPanel.gridx += 1;
		panelStatsUntil.setPreferredSize(DIMENSION_PANEL_DATE_PREF_SIZE);
		statsDateTimePanel.add(panelStatsUntil, gbcStatPanel);
		gbcStatPanel.gridx += 1;
		panelStatsDuration.setPreferredSize(DIMENSION_PANEL_DATE_PREF_SIZE);
		statsDateTimePanel.add(panelStatsDuration, gbcStatPanel);
		gbcStatPanel.gridx += 1;
		gbcStatPanel.weightx = 1.0;
		gbcStatPanel.fill = GridBagConstraints.HORIZONTAL;
		statsDateTimePanel.add(new JLabel(), gbcStatPanel);
		cardStatsPanel.add(statsDateTimePanel, CARD_DATE_TIME);

		// add stats panel to main gui
		gbc.gridx += 1;
		gbc.insets = new Insets(5, 10, 5, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		add(cardStatsPanel, gbc);

		// needed so we can draw our own background
		setOpaque(false);

		// handle mouse events for hover effect and enlarging/shrinking
		addMouseListener(enlargeAndHoverAndPopupMouseAdapter);

		// change cursor to indicate this component can be clicked
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	/**
	 * Returns the {@link AbstractAttributeStatisticsModel} used by this instance. For numerical
	 * attributes it is a {@link NumericalAttributeStatisticsModel}, for nominal attributes it is a
	 * {@link NominalAttributeStatisticsModel}, and for date_time attributes it is a
	 * {@link DateTimeAttributeStatisticsModel}.
	 *
	 * @return the model or <code>null</code> if called before
	 *         {@link #setModel(AbstractAttributeStatisticsModel, boolean)} has been called
	 */
	public AbstractAttributeStatisticsModel getModel() {
		return model;
	}

	/**
	 * Set the new {@link AbstractAttributeStatisticsModel}. The model must be an implementation of
	 * {@link AbstractAttributeStatisticsModel} which matches the {@link Attribute} value type. In
	 * other words, a numerical attribute must have a {@link NumericalAttributeStatisticsModel}, a
	 * nominal attribute must have a {@link NominalAttributeStatisticsModel}, and a date_time
	 * attribute must have a {@link DateTimeAttributeStatisticsModel}.
	 *
	 * @param model
	 * @param updateGUI
	 *            if <code>true</code>, will update the GUI
	 * @throws IllegalArgumentException
	 *             if the model is of the wrong type
	 */
	public void setModel(final AbstractAttributeStatisticsModel model, final boolean updateGUI)
			throws IllegalArgumentException {
		// make sure model is of correct type
		if (model.getAttribute().isNumerical()) {
			if (!(model instanceof NumericalAttributeStatisticsModel)) {
				throw new IllegalArgumentException(
						"model must be of type " + NumericalAttributeStatisticsModel.class.getName());
			}
		} else if (model.getAttribute().isNominal()) {
			if (!(model instanceof NominalAttributeStatisticsModel)) {
				throw new IllegalArgumentException(
						"model must be of type " + NominalAttributeStatisticsModel.class.getName());
			}
		} else {
			if (!(model instanceof DateTimeAttributeStatisticsModel)) {
				throw new IllegalArgumentException(
						"model must be of type " + DateTimeAttributeStatisticsModel.class.getName());
			}
		}
		// switch listener
		if (this.model != null) {
			this.model.removeEventListener(listener);
		}
		this.model = model;
		this.model.registerEventListener(listener);

		// only update GUI if requested
		if (updateGUI) {
			updateGenericElements(model);
			if (model.getAttribute().isNumerical()) {
				updateNumericalElements(model);
				cardLayout.show(cardStatsPanel, CARD_NUMERICAL);
			} else if (model.getAttribute().isNominal()) {
				updateNominalElements(model);
				cardLayout.show(cardStatsPanel, CARD_NOMINAL);
			} else {
				updateDateTimeElements(model);
				cardLayout.show(cardStatsPanel, CARD_DATE_TIME);
			}

			// make sure to show appropriate charts/data when switching model
			if (model.isEnlarged()) {
				updateCharts();
			}
			updateExpandInfoLabel();
			updateVisibilityOfChartPanels();
			if (model.getAttribute().isNumerical()) {
				updateVisibilityOfNumStatPanels();
			}
			if (model.getAttribute().isNominal()) {
				displayNominalValues();
			}
		}
	}

	/**
	 * Adds a {@link MouseListener} for this component. Makes sure it works on resized components as
	 * well as charts.
	 */
	@Override
	public void addMouseListener(final MouseListener listener) {
		super.addMouseListener(listener);
		// needed because we set the size of them/added tooltips so they intercept events now
		labelAttName.addMouseListener(listener);
		labelStatsMissing.addMouseListener(listener);
		labelStatsConstruction.addMouseListener(listener);
		labelStatsMin.addMouseListener(listener);
		labelStatsMax.addMouseListener(listener);
		labelStatsAvg.addMouseListener(listener);
		labelStatsDeviation.addMouseListener(listener);
		labelStatsLeast.addMouseListener(listener);
		labelStatsMost.addMouseListener(listener);
		labelStatsValues.addMouseListener(listener);
		labelStatsDuration.addMouseListener(listener);
		labelStatsFrom.addMouseListener(listener);
		labelStatsUntil.addMouseListener(listener);
	}

	/**
	 * Removes a {@link MouseListener} from this component. Makes sure it is removed from resized
	 * components as well as from charts.
	 */
	@Override
	public void removeMouseListener(final MouseListener listener) {
		super.removeMouseListener(listener);
		// needed because we set the size of them/added tooltips so they intercept events now
		labelAttName.removeMouseListener(listener);
		labelStatsMissing.removeMouseListener(listener);
		labelStatsConstruction.removeMouseListener(listener);
		labelStatsMin.removeMouseListener(listener);
		labelStatsMax.removeMouseListener(listener);
		labelStatsAvg.removeMouseListener(listener);
		labelStatsDeviation.removeMouseListener(listener);
		labelStatsLeast.removeMouseListener(listener);
		labelStatsMost.removeMouseListener(listener);
		labelStatsValues.removeMouseListener(listener);
		labelStatsDuration.removeMouseListener(listener);
		labelStatsFrom.removeMouseListener(listener);
		labelStatsUntil.removeMouseListener(listener);
	}

	@Override
	public void paintComponent(final Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int x = 0;
		int y = 0;
		int width = (int) getSize().getWidth();
		int height = (int) getSize().getHeight();

		// draw background depending special roles and hovering
		g2.setPaint(AttributeGuiTools.getColorForAttributeRole(mapAttributeRoleName(),
				hovered ? ColorScope.HOVER : ColorScope.BACKGROUND));

		g2.fillRoundRect(x, y, width, height, RapidLookAndFeel.CORNER_DEFAULT_RADIUS,
				RapidLookAndFeel.CORNER_DEFAULT_RADIUS);

		// draw background depending special roles and hovering
		g2.setPaint(AttributeGuiTools.getColorForAttributeRole(mapAttributeRoleName(), ColorScope.BORDER));
		if (hovered) {
			g2.setStroke(new BasicStroke(1.0f));
		} else {
			g2.setStroke(new BasicStroke(0.5f));
		}
		g2.drawRoundRect(x, y, width - 1, height - 1, RapidLookAndFeel.CORNER_DEFAULT_RADIUS,
				RapidLookAndFeel.CORNER_DEFAULT_RADIUS);

		// let Swing draw its components
		super.paintComponent(g2);
	}

	/**
	 * Gets the attribute role name from the current {@link #model}.
	 *
	 * @return the name of the role
	 */
	private String mapAttributeRoleName() {
		if (getModel() != null && getModel().isSpecialAtt()) {
			if (Attributes.LABEL_NAME.equals(getModel().getSpecialAttName())) {
				return Attributes.LABEL_NAME;
			} else if (Attributes.WEIGHT_NAME.equals(getModel().getSpecialAttName())) {
				return Attributes.WEIGHT_NAME;
			} else if (Attributes.PREDICTION_NAME.equals(getModel().getSpecialAttName())) {
				return Attributes.PREDICTION_NAME;
			} else if (getModel().getSpecialAttName().startsWith(Attributes.CONFIDENCE_NAME + "_")) {
				return Attributes.CONFIDENCE_NAME;
			} else if (Attributes.ID_NAME.equals(getModel().getSpecialAttName())) {
				return Attributes.ID_NAME;
			} else {
				return GENERIC_SPECIAL_NAME;
			}
		} else {
			return Attributes.ATTRIBUTE_NAME;
		}
	}

	/**
	 * Updates the visibility of num stat panels depending on enlarged status.
	 */
	private void updateExpandInfoLabel() {
		if (model.isEnlarged()) {
			labelStatsExp.setIcon(MetaDataStatisticsViewer.ICON_ARROW_UP);
		} else {
			labelStatsExp.setIcon(MetaDataStatisticsViewer.ICON_ARROW_DOWN);
		}
	}

	/**
	 * Updates the visibility of num stat panels depending on enlarged status.
	 */
	private void updateVisibilityOfNumStatPanels() {
		for (JPanel statPanel : listOfNumStatPanels) {
			if (getModel() != null && getModel().isEnlarged()) {
				statPanel.setPreferredSize(DIMENSION_PANEL_NUMERIC_PREF_SIZE_ENLARGED);
				if (listOfAdditionalNumStatPanels.contains(statPanel)) {
					statPanel.setVisible(true);
				}
			} else {
				statPanel.setPreferredSize(DIMENSION_PANEL_NUMERIC_PREF_SIZE);
				if (listOfAdditionalNumStatPanels.contains(statPanel)) {
					statPanel.setVisible(false);
				}
			}
		}
	}

	/**
	 * Updates chart panel visibility depending on enlarged status.
	 */
	private void updateVisibilityOfChartPanels() {
		for (JPanel chartPanel : listOfChartPanels) {
			if (getModel() != null && getModel().isEnlarged()) {
				chartPanel.setVisible(true);
			} else {
				chartPanel.setVisible(false);
			}
		}
	}

	/**
	 * Updates the charts.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void updateCharts() {
		for (int i = 0; i < listOfChartPanels.size(); i++) {
			JPanel panel = listOfChartPanels.get(i);
			panel.removeAll();
			final ChartPanel chartPanel = new ChartPanel(getModel().getChartOrNull(i)) {

				private static final long serialVersionUID = -6953213567063104487L;

				@Override
				public Dimension getPreferredSize() {
					return DIMENSION_CHART_PANEL_ENLARGED;
				}
			};
			chartPanel.setPopupMenu(null);
			chartPanel.setBackground(COLOR_TRANSPARENT);
			chartPanel.setOpaque(false);
			chartPanel.addMouseListener(enlargeAndHoverAndPopupMouseAdapter);
			panel.add(chartPanel, BorderLayout.CENTER);

			JPanel openChartPanel = new JPanel(new GridBagLayout());
			openChartPanel.setOpaque(false);

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.fill = GridBagConstraints.NONE;
			gbc.weightx = 1.0;
			gbc.weighty = 1.0;

			JButton openChartButton = new JButton(OPEN_CHART_ACTION);
			openChartButton.setOpaque(false);
			openChartButton.setContentAreaFilled(false);
			openChartButton.setBorderPainted(false);
			openChartButton.addMouseListener(enlargeAndHoverAndPopupMouseAdapter);
			openChartButton.setHorizontalAlignment(SwingConstants.LEFT);
			openChartButton.setHorizontalTextPosition(SwingConstants.LEFT);
			openChartButton.setIcon(null);
			Font font = openChartButton.getFont();
			Map attributes = font.getAttributes();
			attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
			openChartButton.setFont(font.deriveFont(attributes).deriveFont(10.0f));

			openChartPanel.add(openChartButton, gbc);

			panel.add(openChartPanel, BorderLayout.SOUTH);
			panel.revalidate();
			panel.repaint();
		}
	}

	/**
	 * Displays the nominal values according to the current settings.
	 */
	private void displayNominalValues() {
		List<ValueAndCount> nominalValues = ((NominalAttributeStatisticsModel) model).getNominalValuesAndCount();
		String valueString = "";

		// we can render enlarged panels with HTML to format things nicely,
		// unless someone expands thousands of attributes this will not impact
		// performance in a noticeable way
		if (getModel() != null && getModel().isEnlarged()) {
			StringBuilder builderHTML = new StringBuilder();
			builderHTML.append("<html>");

			int maxDisplayValues = NominalAttributeStatisticsModel.DEFAULT_MAX_DISPLAYED_VALUES_ENLARGED;
			Iterator<ValueAndCount> it = new LinkedList<>(nominalValues).iterator();
			int n = 0;
			while (it.hasNext() && n < maxDisplayValues) {
				if (n % (NominalAttributeStatisticsModel.DEFAULT_MAX_DISPLAYED_VALUES_ENLARGED / 2) == 0) {
					builderHTML.append("<br>");
				}
				ValueAndCount value = it.next();
				String tmpString = value.getValue();
				String valueStringTruncated = SwingTools.getShortenedDisplayName(tmpString, 17);
				builderHTML.append(valueStringTruncated + " (" + value.getCount() + ")");

				// add separator if not first or last value
				if (n < Math.min(nominalValues.size() - 1, maxDisplayValues)) {
					builderHTML.append(", ");
				}

				n++;
			}
			// count how many we could not display
			int omittedCount = 0;
			while (it.hasNext()) {
				it.next();
				omittedCount++;
			}
			if (omittedCount > 0) {
				builderHTML.append("<br>");
				builderHTML.append(LABEL_DOTS + "[" + omittedCount + " "
						+ I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.values_more")
						+ "]");
			}
			builderHTML.append("</html>");
			valueString = builderHTML.toString();
		} else {
			StringBuilder builderDefault = new StringBuilder();

			int maxDisplayValues = NominalAttributeStatisticsModel.DEFAULT_MAX_DISPLAYED_VALUES_ENLARGED / 2;
			Iterator<ValueAndCount> it = new LinkedList<>(nominalValues).iterator();
			int n = 0;
			while (it.hasNext() && n < maxDisplayValues) {
				ValueAndCount value = it.next();
				String tmpString = value.getValue();
				String valueStringTruncated = SwingTools.getShortenedDisplayName(tmpString, 17);
				builderDefault.append(valueStringTruncated + " (" + value.getCount() + ")");

				// add separator if not first or last value
				if (n < Math.min(nominalValues.size() - 1, maxDisplayValues)) {
					builderDefault.append(", ");
				}

				n++;
			}
			// count how many we could not display
			int omittedCount = 0;
			while (it.hasNext()) {
				it.next();
				omittedCount++;
			}
			if (omittedCount > 0) {
				builderDefault.append(LABEL_DOTS + "[" + omittedCount + " "
						+ I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.values_more")
						+ "]");
			}
			valueString = builderDefault.toString();
		}

		nominalValueFiller.setVisible(getModel() != null && !getModel().isEnlarged());
		detailsButton.setVisible(getModel() != null && getModel().isEnlarged());
		labelStatsValues.setText(valueString);
		labelStatsValues.setToolTipText(valueString);
		labelStatsValues.setVisible(true);
	}

	/**
	 * Updates the generic gui elements.
	 *
	 * @param model
	 */
	private void updateGenericElements(final AbstractAttributeStatisticsModel model) {
		String attLabel = model.getAttribute().getName();
		String attRole = model.getExampleSetOrNull().getAttributes().getRole(model.getAttribute()).getSpecialName();
		String valueTypeString = Ontology.ATTRIBUTE_VALUE_TYPE.mapIndexToDisplayName(model.getAttribute().getValueType());
		String construction = model.getConstruction();
		construction = construction == null ? "-" : construction;

		labelAttHeader.setText(attRole == null || attRole.isEmpty() ? " "
				: Character.toUpperCase(attRole.charAt(0)) + attRole.substring(1));
		labelAttHeader.setForeground(AttributeGuiTools.getColorForAttributeRole(mapAttributeRoleName(), ColorScope.BORDER));

		panelAttName.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1,
				AttributeGuiTools.getColorForAttributeRole(mapAttributeRoleName(), ColorScope.CONTENT)));

		labelAttName.setText(attLabel);
		labelAttName.setToolTipText(attLabel);
		labelAttType.setText(valueTypeString);
		labelAttType.setIcon(null);
		labelStatsMissing.setText(Tools.formatIntegerIfPossible(model.getNumberOfMissingValues(), 0));
		labelStatsMissing.setToolTipText(labelStatsMissing.getText());
		labelStatsConstruction.setText(construction);
		labelStatsConstruction.setToolTipText(labelStatsConstruction.getText());
	}

	/**
	 * Updates the gui elements for numerical stats.
	 *
	 * @param model
	 */
	private void updateNumericalElements(final AbstractAttributeStatisticsModel model) {
		labelStatsValues.setVisible(false);	// because cardLayout dimensions are determined by all
		// cards, not only the visible one
		labelStatsMin.setText(Tools.formatIntegerIfPossible(((NumericalAttributeStatisticsModel) model).getMinimum()));
		labelStatsMin.setToolTipText(labelStatsMin.getText());
		labelStatsMax.setText(Tools.formatIntegerIfPossible(((NumericalAttributeStatisticsModel) model).getMaximum()));
		labelStatsMax.setToolTipText(labelStatsMax.getText());
		labelStatsAvg.setText(Tools.formatIntegerIfPossible(((NumericalAttributeStatisticsModel) model).getAverage()));
		labelStatsAvg.setToolTipText(labelStatsAvg.getText());
		labelStatsDeviation
		.setText(Tools.formatIntegerIfPossible(((NumericalAttributeStatisticsModel) model).getDeviation()));
		labelStatsDeviation.setToolTipText(labelStatsDeviation.getText());
	}

	/**
	 * Updates the gui elements for nominal stats.
	 *
	 * @param model
	 */
	private void updateNominalElements(final AbstractAttributeStatisticsModel model) {
		String least = ((NominalAttributeStatisticsModel) model).getLeast();
		String leastTruncated = SwingTools.getShortenedDisplayName(least, 17);
		String most = ((NominalAttributeStatisticsModel) model).getMost();
		String mostTruncated = SwingTools.getShortenedDisplayName(most, 17);
		labelStatsLeast.setText(leastTruncated);
		labelStatsLeast.setToolTipText(least);
		labelStatsMost.setText(mostTruncated);
		labelStatsMost.setToolTipText(most);
		displayNominalValues();
	}

	/**
	 * Updates the gui elements for date_time stats.
	 *
	 * @param model
	 */
	private void updateDateTimeElements(final AbstractAttributeStatisticsModel model) {
		// because cardLayout dimensions are determined by all
		// cards, not only the visible one
		nominalValueFiller.setVisible(false);
		labelStatsValues.setVisible(false);

		labelStatsDuration.setText(((DateTimeAttributeStatisticsModel) model).getDuration());
		labelStatsDuration.setToolTipText(labelStatsDuration.getText());
		labelStatsFrom.setText(((DateTimeAttributeStatisticsModel) model).getFrom());
		labelStatsFrom.setToolTipText(labelStatsFrom.getText());
		labelStatsUntil.setText(((DateTimeAttributeStatisticsModel) model).getUntil());
		labelStatsUntil.setToolTipText(labelStatsUntil.getText());
	}

	/**
	 * Sets the minimum and preferred size of the name column. Also revalidates and repaints the
	 * panel. If not yet initialized, does nothing.
	 *
	 * @param nameDim
	 *            the new minimum size. If <code>null</code>, resets to the default.
	 */
	public void updateNameColumnWidth(Dimension nameDim) {
		if (labelAttName != null) {
			if (nameDim == null) {
				labelAttName.setMinimumSize(DIMENSION_LABEL_ATTRIBUTE);
				labelAttName.setPreferredSize(DIMENSION_LABEL_ATTRIBUTE);
			} else {
				// add 20px width to align columns better
				Dimension newDim = new Dimension(nameDim.width + 30, nameDim.height);
				labelAttName.setMinimumSize(newDim);
				labelAttName.setPreferredSize(newDim);
			}

			panelAttName.revalidate();
			panelAttName.repaint();
		}
	}
}
