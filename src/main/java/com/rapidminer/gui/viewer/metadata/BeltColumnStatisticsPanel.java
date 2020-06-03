/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.WordUtils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.tools.AttributeGuiTools;
import com.rapidminer.gui.tools.AttributeGuiTools.ColorScope;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.viewer.metadata.actions.BeltColumnPopupMenu;
import com.rapidminer.gui.viewer.metadata.actions.BeltCopyColumnNameAction;
import com.rapidminer.gui.viewer.metadata.actions.BeltCopyDateTimeFromValueAction;
import com.rapidminer.gui.viewer.metadata.actions.BeltCopyDateTimeUntilValueAction;
import com.rapidminer.gui.viewer.metadata.actions.BeltCopyNumAvgValueAction;
import com.rapidminer.gui.viewer.metadata.actions.BeltCopyNumDeviationValueAction;
import com.rapidminer.gui.viewer.metadata.actions.BeltCopyNumMaximumValueAction;
import com.rapidminer.gui.viewer.metadata.actions.BeltCopyNumMinimumValueAction;
import com.rapidminer.gui.viewer.metadata.actions.BeltOpenChartAction;
import com.rapidminer.gui.viewer.metadata.actions.BeltShowNomValueAction;
import com.rapidminer.gui.viewer.metadata.event.AttributeStatisticsEventListener;
import com.rapidminer.gui.viewer.metadata.model.AbstractBeltColumnStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.BeltDateTimeColumnStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.BeltNominalColumnStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.BeltNumericalColumnStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.BeltObjectColumnStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.BeltTimeColumnStatisticsModel;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.ValueAndCount;


/**
 * GUI display for meta data statistics of a {@link Column} backed by an
 * {@link AbstractBeltColumnStatisticsModel}.
 * <p>
 * The reason for this being just one class instead of 3 classes extending an abstract one is
 * simple: performance. Removing/adding these panels from/to the {@link BeltMetaDataStatisticsViewer}
 * whenever sorting changes is very costly and takes too much time. Switching the models (and thus
 * basically only updating {@link JLabel}s) is much quicker, however we cannot switch the model
 * between numerical/nominal/date_time columns when there is not one universal GUI for all 3
 * different column types.
 *
 * @author Marco Boeck, Gisa Meier
 * @since 9.7.0
 */
public class BeltColumnStatisticsPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/** the font size of header labels */
	private static final float FONT_SIZE_LABEL_HEADER = 10;

	/** the font size of value labels */
	private static final float FONT_SIZE_LABEL_VALUE = 14;

	/** the identifier for custom special columns */
	private static final String GENERIC_SPECIAL_NAME = "special";

	/** the dimension for the column name label	 */
	private static final Dimension DIMENSION_LABEL_ATTRIBUTE = new Dimension(230, 30);

	/** the dimension for the column type label */
	private static final Dimension DIMENSION_LABEL_TYPE = new Dimension(90, 20);

	/** the dimension for the column missings label */
	private static final Dimension DIMENSION_LABEL_MISSINGS = new Dimension(75, 20);

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

	/** used to open an histogram chart for a column */
	private static final BeltOpenChartAction OPEN_CHART_ACTION = new BeltOpenChartAction();

	/** the identifier for the numerical card */
	private static final String CARD_NUMERICAL = "numericalCard";

	/** the identifier for the nominal card */
	private static final String CARD_NOMINAL = "nominalCard";

	/** the identifier for the date_time card */
	private static final String CARD_DATE_TIME = "dateTimeCard";

	private static final String CARD_NONE = "none";

	/** the popup shown on {@link BeltColumnStatisticsPanel} for numerical columns */
	private static JPopupMenu popupAttributeNumericalStatPanel;

	/** the popup shown on {@link BeltColumnStatisticsPanel} for nominal columns */
	private static JPopupMenu popupAttributeNominalStatPanel;

	/** the popup shown on {@link BeltColumnStatisticsPanel} for date_time columns */
	private static JPopupMenu popupAttributeDateTimeStatPanel;

	/** the popup shown on {@link BeltColumnStatisticsPanel} for other object columns */
	private static JPopupMenu popupAttributeObjectStatPanel;

	static {
		// populate global popup menus (used to reduce performance issues so not PAGE_SIZE * 3 popup
		// menues are created)
		popupAttributeNumericalStatPanel = new BeltColumnPopupMenu();
		popupAttributeNumericalStatPanel.add(new BeltCopyColumnNameAction());
		popupAttributeNumericalStatPanel.add(new BeltCopyNumAvgValueAction());
		popupAttributeNumericalStatPanel.add(new BeltCopyNumDeviationValueAction());
		popupAttributeNumericalStatPanel.add(new BeltCopyNumMinimumValueAction());
		popupAttributeNumericalStatPanel.add(new BeltCopyNumMaximumValueAction());
		popupAttributeNumericalStatPanel.addSeparator();
		popupAttributeNumericalStatPanel.add(OPEN_CHART_ACTION);

		popupAttributeNominalStatPanel = new BeltColumnPopupMenu();
		popupAttributeNominalStatPanel.add(new BeltCopyColumnNameAction());
		popupAttributeNominalStatPanel.addSeparator();
		popupAttributeNominalStatPanel.add(new BeltShowNomValueAction(null));
		popupAttributeNominalStatPanel.add(OPEN_CHART_ACTION);

		popupAttributeDateTimeStatPanel = new BeltColumnPopupMenu();
		popupAttributeDateTimeStatPanel.add(new BeltCopyColumnNameAction());
		popupAttributeDateTimeStatPanel.add(new BeltCopyDateTimeFromValueAction());
		popupAttributeDateTimeStatPanel.add(new BeltCopyDateTimeUntilValueAction());
		popupAttributeDateTimeStatPanel.add(OPEN_CHART_ACTION);

		popupAttributeObjectStatPanel = new BeltColumnPopupMenu();
		popupAttributeObjectStatPanel.add(new BeltCopyColumnNameAction());
	}

	/** true if the mouse is hovering over this panel */
	private boolean hovered;

	/** the mouse listener for this panel to notice hovering/enlarge/shrink/popup requests */
	private transient MouseAdapter enlargeAndHoverAndPopupMouseAdapter;

	/** list containing all panels which display a chart */
	private List<JPanel> listOfChartPanels;

	/** list containing all panels which should only displayed if the model is enlarged */
	private List<JPanel> listOfNumStatPanels;

	/** list containing all panels which should only displayed if the model is enlarged */
	private List<JPanel> listOfAdditionalNumStatPanels;

	/** label displaying the column header (special role) if it has one */
	private JLabel labelAttHeader;

	/** label displaying the column name */
	private JLabel labelAttName;

	/** panel in which name and role reside */
	private JPanel panelAttName;

	/** label displaying the column type */
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

	/**
	 * the {@link AbstractBeltColumnStatisticsModel} backing the GUI
	 */
	private transient AbstractBeltColumnStatisticsModel model;

	/** the listener which listens on the model for changes */
	private transient AttributeStatisticsEventListener listener;
	private JLabel labelLeastHeader;
	private JLabel labelMostHeader;

	/**
	 * Creates a new {@link BeltColumnStatisticsPanel} instance. Before displaying the panel, an
	 * {@link AbstractBeltColumnStatisticsModel} should be set via
	 * {@link #setModel(AbstractBeltColumnStatisticsModel, boolean)}.
	 *
	 */
	public BeltColumnStatisticsPanel() {
		listOfChartPanels = new LinkedList<>();
		listOfNumStatPanels = new LinkedList<>();
		listOfAdditionalNumStatPanels = new LinkedList<>();
		createMouseAdapter();
		createListener();

		initGUI();
	}

	/**
	 * create listener which listens for AttributeStatisticsEvents on the model
	 */
	private void createListener() {
		listener = e -> {
			switch (e.getEventType()) {
				case ALTERNATING_CHANGED:
					repaint();
					break;
				case ENLARGED_CHANGED:
					updateCharts();
					updateExpandInfoLabel();
					updateVisibilityOfChartPanels();
					if (BeltColumnStatisticsPanel.this.model.getType() == AbstractBeltColumnStatisticsModel.Type.NUMERIC
							|| BeltColumnStatisticsPanel.this.model.getType() == AbstractBeltColumnStatisticsModel.Type.TIME) {
						updateVisibilityOfNumStatPanels();
					}
					if (BeltColumnStatisticsPanel.this.model.getType() == AbstractBeltColumnStatisticsModel.Type.NOMINAL
							|| BeltColumnStatisticsPanel.this.model.getType() == AbstractBeltColumnStatisticsModel.Type.BINOMINAL) {
						displayNominalValues();
					}
					break;
				case STATISTICS_CHANGED:
					SwingUtilities.invokeLater(this::updateByType);
					break;
				default:
			}
		};
	}

	private void updateByType() {
		switch (model.getType()) {
			case NUMERIC:
				updateNumericalElements(model);
				break;
			case NOMINAL:
				updateNominalElements(model);
				break;
			case BINOMINAL:
				updateBinominalElements(model);
				break;
			case TIME:
				updateTimeElements(model);
				break;
			case DATETIME:
				updateDateTimeElements(model);
				break;
			case OTHER_OBJECT:
			default:
				//do nothing
		}
	}

	/**
	 * create listener which listens for hovering/enlarge mouse events on this panel
	 */
	private void createMouseAdapter() {
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
				if (SwingTools.isMouseEventExitedToChildComponents(BeltColumnStatisticsPanel.this, e)) {
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
			 */
			private void handlePopup(final MouseEvent e) {
				handlePopupByType(e);
			}
		};
	}

	/**
	 * Handles the event depending on the model type.
	 */
	private void handlePopupByType(MouseEvent e) {
		switch (model.getType()) {
			case NUMERIC:
				popupAttributeNumericalStatPanel.show(e.getComponent(), e.getX(), e.getY());
				break;
			case NOMINAL:
			case BINOMINAL:
				popupAttributeNominalStatPanel.show(e.getComponent(), e.getX(), e.getY());
				break;
			case DATETIME:
				popupAttributeDateTimeStatPanel.show(e.getComponent(), e.getX(), e.getY());
				break;
			case TIME:
				popupAttributeNumericalStatPanel.show(e.getComponent(), e.getX(), e.getY());
				break;
			case OTHER_OBJECT:
			default:
				popupAttributeObjectStatPanel.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	/**
	 * Initializes the GUI.
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

		// add column name
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

		// statistics panel, contains different statistics panels for numerical/nominal/date_time/other object on
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

		labelLeastHeader = new JLabel(leastLabel);
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

		labelMostHeader = new JLabel(mostLabel);
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

		detailsButton = new JButton(new BeltShowNomValueAction(this));
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

		cardStatsPanel.add(new JLabel(), CARD_NONE);

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
	 * Returns the {@link AbstractBeltColumnStatisticsModel} used by this instance. For numerical
	 * columns it is a {@link BeltNumericalColumnStatisticsModel}, for nominal columns it is a
	 * {@link BeltNominalColumnStatisticsModel}, for date_time columns it is a
	 * {@link BeltDateTimeColumnStatisticsModel}, for time columns it is a {@link BeltTimeColumnStatisticsModel} and
	 * for other object columns it is a {@link BeltObjectColumnStatisticsModel}.
	 *
	 * @return the model or {@code null} if called before
	 *         {@link #setModel(AbstractBeltColumnStatisticsModel, boolean)} has been called
	 */
	public AbstractBeltColumnStatisticsModel getModel() {
		return model;
	}

	/**
	 * Set the new {@link AbstractBeltColumnStatisticsModel}. The model must be an implementation of {@link
	 * AbstractBeltColumnStatisticsModel} which matches the column type. In other words, a numerical column must have a
	 * {@link BeltNumericalColumnStatisticsModel}, a nominal column must have a {@link
	 * BeltNominalColumnStatisticsModel}, and a date_time column must have a {@link BeltDateTimeColumnStatisticsModel}.
	 *
	 * @param model
	 * 		the model
	 * @param updateGUI
	 * 		if {@code true}, will update the GUI
	 */
	public void setModel(final AbstractBeltColumnStatisticsModel model, final boolean updateGUI) {

		// switch listener
		if (this.model != null) {
			this.model.removeEventListener(listener);
		}
		this.model = model;
		this.model.registerEventListener(listener);

		// only update GUI if requested
		if (updateGUI) {
			updateGenericElements(model);
			updateModelByType(model);

			// make sure to show appropriate charts/data when switching model
			if (model.isEnlarged()) {
				updateCharts();
			}
			updateExpandInfoLabel();
			updateVisibilityOfChartPanels();
			if (model.getType() == AbstractBeltColumnStatisticsModel.Type.NUMERIC
					|| model.getType() == AbstractBeltColumnStatisticsModel.Type.TIME) {
				updateVisibilityOfNumStatPanels();
			}
			if (model.getType() == AbstractBeltColumnStatisticsModel.Type.NOMINAL
					|| model.getType() == AbstractBeltColumnStatisticsModel.Type.BINOMINAL) {
				displayNominalValues();
			}
		}
	}

	/**
	 * Updates the model depending on the type.
	 */
	private void updateModelByType(AbstractBeltColumnStatisticsModel model) {
		switch (model.getType()) {
			case NUMERIC:
				updateNumericalElements(model);
				cardLayout.show(cardStatsPanel, CARD_NUMERICAL);
				break;
			case NOMINAL:
				updateNominalElements(model);
				cardLayout.show(cardStatsPanel, CARD_NOMINAL);
				break;
			case BINOMINAL:
				updateBinominalElements(model);
				labelLeastHeader.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.negative.label"));
				labelMostHeader.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.positive.label"));
				cardLayout.show(cardStatsPanel, CARD_NOMINAL);
				break;
			case TIME:
				updateTimeElements(model);
				cardLayout.show(cardStatsPanel, CARD_NUMERICAL);
				break;
			case DATETIME:
				updateDateTimeElements(model);
				cardLayout.show(cardStatsPanel, CARD_DATE_TIME);
				break;
			case OTHER_OBJECT:
			default:
				cardLayout.show(cardStatsPanel, CARD_NONE);
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
	 * Gets the column role name from the current {@link #model}.
	 *
	 * @return the name of the role
	 */
	private String mapAttributeRoleName() {
		if (getModel() != null && getModel().isSpecialColumn()) {
			if (Attributes.LABEL_NAME.equals(getModel().getSpecialColumnName())) {
				return Attributes.LABEL_NAME;
			} else if (Attributes.WEIGHT_NAME.equals(getModel().getSpecialColumnName())) {
				return Attributes.WEIGHT_NAME;
			} else if (Attributes.PREDICTION_NAME.equals(getModel().getSpecialColumnName())) {
				return Attributes.PREDICTION_NAME;
			} else if (ColumnRole.SCORE.toString().equalsIgnoreCase(getModel().getSpecialColumnName())) {
				return Attributes.CONFIDENCE_NAME;
			} else if (Attributes.ID_NAME.equals(getModel().getSpecialColumnName())) {
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
			chartPanel.setVisible(getModel() != null && getModel().isEnlarged());
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
			JFreeChart chartOrNull = getModel().getChartOrNull(i);
			if (chartOrNull != null) {
				final ChartPanel chartPanel = new ChartPanel(chartOrNull) {

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
			}
			panel.revalidate();
			panel.repaint();
		}
	}

	/**
	 * Displays the nominal values according to the current settings.
	 */
	private void displayNominalValues() {
		List<ValueAndCount> nominalValues = ((BeltNominalColumnStatisticsModel) model).getNominalValuesAndCount();
		String valueString = "";

		// we can render enlarged panels with HTML to format things nicely,
		// unless someone expands thousands of columns this will not impact
		// performance in a noticeable way
		if (getModel() != null && getModel().isEnlarged()) {
			valueString = buildEnlargedValueString(nominalValues);
		} else {
			StringBuilder builderDefault = new StringBuilder();

			int maxDisplayValues = BeltNominalColumnStatisticsModel.DEFAULT_MAX_DISPLAYED_VALUES_ENLARGED / 2;
			Iterator<ValueAndCount> it = new LinkedList<>(nominalValues).iterator();
			int n = 0;
			while (it.hasNext() && n < maxDisplayValues) {
				ValueAndCount value = it.next();
				String tmpString = value.getValue();
				String valueStringTruncated = SwingTools.getShortenedDisplayName(tmpString, 17);
				builderDefault.append(valueStringTruncated).append(" (").append(value.getCount()).append(")");

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
				builderDefault.append(LABEL_DOTS + "[").append(omittedCount)
						.append(" ")
						.append(I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.values_more"))
						.append("]");
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
	 * Builds the value String for the enlarged case.
	 */
	private String buildEnlargedValueString(List<ValueAndCount> nominalValues) {
		String valueString;
		StringBuilder builderHTML = new StringBuilder();
		builderHTML.append("<html>");

		int maxDisplayValues = BeltNominalColumnStatisticsModel.DEFAULT_MAX_DISPLAYED_VALUES_ENLARGED;
		Iterator<ValueAndCount> it = new LinkedList<>(nominalValues).iterator();
		int n = 0;
		while (it.hasNext() && n < maxDisplayValues) {
			if (n % (BeltNominalColumnStatisticsModel.DEFAULT_MAX_DISPLAYED_VALUES_ENLARGED / 2) == 0) {
				builderHTML.append("<br>");
			}
			ValueAndCount value = it.next();
			String tmpString = value.getValue();
			String valueStringTruncated = SwingTools.getShortenedDisplayName(tmpString, 17);
			builderHTML.append(valueStringTruncated).append(" (").append(value.getCount()).append(")");

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
			builderHTML.append(LABEL_DOTS + "[").append(omittedCount)
					.append(" ")
					.append(I18N.getMessage(I18N.getGUIBundle(), "gui.label.attribute_statistics.statistics.values_more"))
					.append("]");
		}
		builderHTML.append("</html>");
		valueString = builderHTML.toString();
		return valueString;
	}

	/**
	 * Updates the generic gui elements.
	 */
	private void updateGenericElements(final AbstractBeltColumnStatisticsModel model) {
		String attLabel = model.getColumnName();
		ColumnRole role = model.getTableOrNull().getFirstMetaData(model.getColumnName(),
				ColumnRole.class);
		String attRole = role == null ? null : role.toString().toLowerCase(Locale.ENGLISH);
		Column column = model.getTableOrNull().column(model.getColumnName());
		String valueTypeString = WordUtils.capitalizeFully(column.type().id().toString().toLowerCase(Locale.ENGLISH));
		if (column.type().id() == Column.TypeId.NOMINAL && column.getDictionary().isBoolean()) {
			valueTypeString = "Binominal";
		}

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
	}

	/**
	 * Updates the gui elements for numerical stats.
	 */
	private void updateNumericalElements(final AbstractBeltColumnStatisticsModel model) {
		labelStatsValues.setVisible(false);	// because cardLayout dimensions are determined by all
		// cards, not only the visible one
		labelStatsMin.setText(Tools.formatIntegerIfPossible(((BeltNumericalColumnStatisticsModel) model).getMinimum()));
		labelStatsMin.setToolTipText(labelStatsMin.getText());
		labelStatsMax.setText(Tools.formatIntegerIfPossible(((BeltNumericalColumnStatisticsModel) model).getMaximum()));
		labelStatsMax.setToolTipText(labelStatsMax.getText());
		labelStatsAvg.setText(Tools.formatIntegerIfPossible(((BeltNumericalColumnStatisticsModel) model).getAverage()));
		labelStatsAvg.setToolTipText(labelStatsAvg.getText());
		labelStatsDeviation
		.setText(Tools.formatIntegerIfPossible(((BeltNumericalColumnStatisticsModel) model).getDeviation()));
		labelStatsDeviation.setToolTipText(labelStatsDeviation.getText());
	}

	/**
	 * Updates the gui elements for time stats.
	 */
	private void updateTimeElements(final AbstractBeltColumnStatisticsModel model) {
		labelStatsValues.setVisible(false);	// because cardLayout dimensions are determined by all
		// cards, not only the visible one
		labelStatsMin.setText(Objects.toString(((BeltTimeColumnStatisticsModel) model).getMinimum(), "?"));
		labelStatsMin.setToolTipText(labelStatsMin.getText());
		labelStatsMax.setText(Objects.toString(((BeltTimeColumnStatisticsModel) model).getMaximum(), "?"));
		labelStatsMax.setToolTipText(labelStatsMax.getText());
		labelStatsAvg.setText(Objects.toString(((BeltTimeColumnStatisticsModel) model).getAverage(), "?"));
		labelStatsAvg.setToolTipText(labelStatsAvg.getText());
		labelStatsDeviation
				.setText(Objects.toString(((BeltTimeColumnStatisticsModel) model).getDeviation(), "?"));
		labelStatsDeviation.setToolTipText(labelStatsDeviation.getText());
	}

	/**
	 * Updates the gui elements for nominal stats.
	 */
	private void updateNominalElements(final AbstractBeltColumnStatisticsModel model) {
		String least = ((BeltNominalColumnStatisticsModel) model).getLeast();
		String leastTruncated = SwingTools.getShortenedDisplayName(least, 17);
		String most = ((BeltNominalColumnStatisticsModel) model).getMost();
		String mostTruncated = SwingTools.getShortenedDisplayName(most, 17);
		labelStatsLeast.setText(leastTruncated);
		labelStatsLeast.setToolTipText(least);
		labelStatsMost.setText(mostTruncated);
		labelStatsMost.setToolTipText(most);
		displayNominalValues();
	}

	/**
	 * Updates the gui elements for binominal stats.
	 */
	private void updateBinominalElements(final AbstractBeltColumnStatisticsModel model) {
		String negative = ((BeltNominalColumnStatisticsModel) model).getNegative();
		String negativeTruncated = SwingTools.getShortenedDisplayName(negative, 17);
		String positive = ((BeltNominalColumnStatisticsModel) model).getPositive();
		String positiveTruncated = SwingTools.getShortenedDisplayName(positive, 17);
		labelStatsLeast.setText(negativeTruncated);
		labelStatsLeast.setToolTipText(negative);
		labelStatsMost.setText(positiveTruncated);
		labelStatsMost.setToolTipText(positive);
		displayNominalValues();
	}

	/**
	 * Updates the gui elements for date_time stats.
	 */
	private void updateDateTimeElements(final AbstractBeltColumnStatisticsModel model) {
		// because cardLayout dimensions are determined by all
		// cards, not only the visible one
		nominalValueFiller.setVisible(false);
		labelStatsValues.setVisible(false);

		labelStatsDuration.setText(((BeltDateTimeColumnStatisticsModel) model).getDuration());
		labelStatsDuration.setToolTipText(labelStatsDuration.getText());
		labelStatsFrom.setText(((BeltDateTimeColumnStatisticsModel) model).getFrom());
		labelStatsFrom.setToolTipText(labelStatsFrom.getText());
		labelStatsUntil.setText(((BeltDateTimeColumnStatisticsModel) model).getUntil());
		labelStatsUntil.setToolTipText(labelStatsUntil.getText());
	}

	/**
	 * Sets the minimum and preferred size of the name column. Also revalidates and repaints the
	 * panel. If not yet initialized, does nothing.
	 *
	 * @param nameDim
	 *            the new minimum size. If {@code null}, resets to the default.
	 */
	void updateNameColumnWidth(Dimension nameDim) {
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
