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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.gui.CleanupRequiringComponent;
import com.rapidminer.gui.actions.CopyStringToClipboardAction;
import com.rapidminer.gui.actions.export.PrintableComponent;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.MultiSwingWorker;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ScrollableJPopupMenu;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.TextFieldWithAction;
import com.rapidminer.gui.tools.components.DropDownPopupButton;
import com.rapidminer.gui.tools.components.DropDownPopupButton.PopupMenuProvider;
import com.rapidminer.gui.viewer.metadata.actions.ShowConstructionValueAction;
import com.rapidminer.gui.viewer.metadata.event.AttributeStatisticsEvent;
import com.rapidminer.gui.viewer.metadata.event.AttributeStatisticsEvent.EventType;
import com.rapidminer.gui.viewer.metadata.event.AttributeStatisticsEventListener;
import com.rapidminer.gui.viewer.metadata.event.MetaDataStatisticsEvent;
import com.rapidminer.gui.viewer.metadata.event.MetaDataStatisticsEventListener;
import com.rapidminer.gui.viewer.metadata.model.AbstractAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.DateTimeAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.MetaDataStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.MetaDataStatisticsModel.SortingDirection;
import com.rapidminer.gui.viewer.metadata.model.MetaDataStatisticsModel.SortingType;
import com.rapidminer.gui.viewer.metadata.model.NominalAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.NumericalAttributeStatisticsModel;
import com.rapidminer.report.Renderable;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


/**
 * This is the GUI to display meta data statistics for {@link ExampleSet}s. Note that EDT blocking
 * is minimal because the GUI for each {@link Attribute} and the subsequent population with the data
 * are done in {@link MultiSwingWorker}s. The attributes are displayed via a pagination system, i.e. when
 * more than {@link MetaDataStatisticsModel#PAGE_SIZE} attributes exist they are displayed on pages
 * which show up to {@link MetaDataStatisticsModel#PAGE_SIZE}. This prevents performance problems
 * even when the number of attributes exceeds <code>100,000</code>.
 * <p>
 * The GUI itself does allow filtering as well as sorting via attribute type, name and missing
 * values.
 *
 * @author Marco Boeck
 *
 */
public class MetaDataStatisticsViewer extends JPanel implements Renderable, PrintableComponent, CleanupRequiringComponent {

	private static final long serialVersionUID = -1027619839144846140L;

	/** the background color */
	private static final Color COLOR_BACKGROUND = Color.WHITE;

	/** the dimension for the attribute name header */
	private static final Dimension DIMENSION_HEADER_ATTRIBUTE_NAME = new Dimension(200, 30);

	/** the dimension for the attribute type header */
	private static final Dimension DIMENSION_HEADER_TYPE = new Dimension(90, 20);

	/** the dimension for the attribute missings label */
	private static final Dimension DIMENSION_HEADER_MISSINGS = new Dimension(75, 20);

	/** the minimum size for the search field */
	private static final Dimension DIMENSION_SEARCH_FIELD = new Dimension(140, 20);

	/** minimum margin which must be left when enlarging the name column */
	private static final int RESIZE_MARGIN_ENLARGE = 450;

	/** minimum margin which must be left when shrinking the name column */
	private static final int RESIZE_MARGIN_SHRINK = 50;

	/** the font size of the special/regular labels */
	private static final float FONT_SIZE_LABEL = 12;

	/** arrow icon with two arrows pointing left */
	private static final ImageIcon ICON_ARROW_FIRST = SwingTools.createIcon("16/" + "navigate_left2.png");

	/** arrow icon with an arrow pointing left */
	static final ImageIcon ICON_ARROW_LEFT = SwingTools.createIcon("16/" + "navigate_left.png");

	/** arrow icon with an arrow pointing right */
	static final ImageIcon ICON_ARROW_RIGHT = SwingTools.createIcon("16/" + "navigate_right.png");

	/** arrow icon with two arrows pointing right */
	static final ImageIcon ICON_ARROW_LAST = SwingTools.createIcon("16/" + "navigate_right2.png");

	/** arrow icon with an arrow pointing up */
	static final ImageIcon ICON_ARROW_UP = SwingTools.createIcon("16/" + "navigate_up.png");

	/** arrow icon with an arrow pointing down */
	static final ImageIcon ICON_ARROW_DOWN = SwingTools.createIcon("16/" + "navigate_down.png");

	/** x-like red icon for cancellation */
	private static final ImageIcon CANCELLATION_ICON = SwingTools.createIcon("16/" + "delete.png");

	/**
	 * icon used in the {@link TextFieldWithAction} when the filter remove action is hovered
	 */
	private final ImageIcon CLEAR_FILTER_HOVERED_ICON = SwingTools.createIcon("16/x-mark_orange.png");

	/**
	 * the delay before filtering is started after the user finished typing in milliseconds: * * * *
	 * * {@value}
	 */
	private static final int FILTER_TIMER_DELAY = 500;

	/** the identifier of the search focus action */
	private static final String ACTION_NAME_SEARCH = "focusSearchField";

	/** the client property key to indicate the scrollbar should scroll down */
	private static final String PROPERTY_SCROLL_DOWN = "scrollDown";

	/** header panel which contains column header components */
	private JPanel columnHeaderPanel;

	/** the attribute name sorting label */
	private JLabel sortingLabelAttName;

	/** the attribute type sorting label */
	private JLabel sortingLabelAttType;

	/** the attribute missing count sorting label */
	private JLabel sortingLabelAttMissings;

	/** the resize label for the name column */
	private JLabel resizeNameColumnLabel;

	/**
	 * the label displaying the number of currently displayed attributes (taking filters into
	 * account)
	 */
	private JLabel filterLabel;

	/** the panel housing the attribute panels */
	private JPanel attPanel;

	/** the loading placeholder label */
	private JLabel labelLoading;

	/** the label indicating no attributes are there */
	private JLabel labelNoAttributes;

	/** the label displaying the currently shown attributes on the page */
	private JLabel labelDisplayedAttribute;

	/** the combobox used for pagination */
	private JComboBox<Integer> pagesComboBox;

	/** the page change listener for the page combobox */
	private ItemListener pageComboListener;

	/** the panel housing the pagination elements */
	private JPanel paginationPanel;

	/** the button to switch to the first page */
	private JButton buttonFirstPage;

	/** the button to switch to the previous page */
	private JButton buttonPreviousPage;

	/** the button to switch to the next page */
	private JButton buttonNextPage;

	/** the button to switch to the last page */
	private JButton buttonLastPage;

	/** the {@link GridBagConstraints} for the attribute panel */
	private GridBagConstraints gbcAttPanel;

	/** the backing model of the GUI */
	private final MetaDataStatisticsModel model;

	/** the controller between GUI and model */
	private final MetaDataStatisticsController controller;

	/** the scrollpane in which this entire panel is placed */
	private JScrollPane scrollPane;

	/** if set, the custom dimension for the attribute name column */
	private Dimension nameDim;

	/**
	 * this map maps the respective {@link AttributeStatisticsPanel} to their {@link Integer} index
	 */
	private final Map<Integer, AttributeStatisticsPanel> mapOfAttributeStatisticsPanels;

	private JPanel outerPanel;


	private final class HoverBorderMouseListener extends MouseAdapter {

		private final JButton button;

		public HoverBorderMouseListener(final JButton pageButton) {
			this.button = pageButton;

		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			if (!button.isEnabled()) {
				button.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			}
			super.mouseReleased(e);
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			button.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
			super.mouseExited(e);
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
			if (button.isEnabled()) {
				button.setBorder(BorderFactory.createLineBorder(Color.gray, 1, true));
			}
			super.mouseEntered(e);
		}
	}

	private final class ResizeAttributeNameMouseListener extends MouseAdapter {

		int startingX = -1;

		@Override
		public void mousePressed(final MouseEvent e) {
			if (!SwingUtilities.isLeftMouseButton(e)) {
				return;
			}

			// we somehow missed the drag end event, so finish resize now
			if (startingX > -1) {
				int diff = e.getX() - startingX;
				resizeNameColumn(diff, true);
			}

			// start new resize in any case
			startingX = e.getX();
		}

		@Override
		public void mouseDragged(final MouseEvent e) {
			if (!SwingUtilities.isLeftMouseButton(e)) {
				return;
			}
			// if dragging is in progress, visualize in header column
			if (startingX > -1) {
				int diff = e.getX() - startingX;
				resizeNameColumn(diff, false);
			}
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			if (!SwingUtilities.isLeftMouseButton(e)) {
				return;
			}

			// if a new drag was started it ends here
			if (startingX > -1) {
				int diff = e.getX() - startingX;
				resizeNameColumn(diff, true);
			}
			startingX = -1;
		}

		/**
		 * Called when resizing event occurs to resize the attribute name column. Resizing is
		 * restricted to a mininmum and a maximum amount depending on the actual size of the header
		 * panel.
		 *
		 * @param diff
		 *            the horizontal movement in px (can be negative to shrink).
		 * @param resizeStatisticPanels
		 *            if <code>true</code> will update the ASPs as well (costly)
		 *
		 */
		private void resizeNameColumn(int diff, boolean resizeStatisticPanels) {
			if (diff != 0) {
				if (nameDim == null) {
					nameDim = new Dimension(DIMENSION_HEADER_ATTRIBUTE_NAME.width + diff,
							DIMENSION_HEADER_ATTRIBUTE_NAME.height);
				} else {
					int newWidth = nameDim.width + diff;
					int minWidth = RESIZE_MARGIN_SHRINK;
					// max width is the size of the header panel minus the size
					// of the other
					// elements to the right
					int maxWidth = columnHeaderPanel.getWidth() - (DIMENSION_HEADER_MISSINGS.width
							+ DIMENSION_HEADER_TYPE.width + DIMENSION_SEARCH_FIELD.width + RESIZE_MARGIN_ENLARGE);
					// do not allow shrinking/enlarging over a limit to prevent
					// GUI breaking
					if (newWidth > maxWidth) {
						newWidth = maxWidth;
					}
					if (newWidth < minWidth) {
						newWidth = minWidth;
					}
					nameDim = new Dimension(newWidth, nameDim.height);
				}
				sortingLabelAttName.setMinimumSize(nameDim);
				sortingLabelAttName.setPreferredSize(nameDim);

				// update header panel
				columnHeaderPanel.revalidate();
				columnHeaderPanel.repaint();
			}

			if (resizeStatisticPanels) {
				revalidateAttributePanels();
			}
		}

	};

	/**
	 * Creates a new {@link MetaDataStatisticsViewer} instance.
	 *
	 * @param model
	 *            the {@link MetaDataStatisticsModel} backing the GUI
	 */
	public MetaDataStatisticsViewer(final MetaDataStatisticsModel model) {
		this.model = model;
		this.controller = new MetaDataStatisticsController(this, model);

		mapOfAttributeStatisticsPanels = new HashMap<>();

		MetaDataStatisticsEventListener listener = new MetaDataStatisticsEventListener() {

			@Override
			public void modelChanged(final MetaDataStatisticsEvent e) {
				switch (e.getEventType()) {
					case INIT_DONE:
						revalidateAttributePanels();
						break;
					case FILTER_CHANGED:
						revalidateAttributePanels();
						break;
					case ORDER_CHANGED:
						// this is fired when UpdateQueue triggers, which is
						// outside the EDT
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								updateSortingIcons();
								revalidateAttributePanels();
							}

						});
						break;
					case PAGINATION_CHANGED:
						revalidateAttributePanels();
						break;
					default:
						break;
				}

			}
		};
		model.registerEventListener(listener);

		initGUI();
		createAttributeStatisticsPanels();
	}

	/**
	 * Creates all {@link AttributeStatisticsPanel}s in a {@link MultiSwingWorker}.
	 */
	private void createAttributeStatisticsPanels() {
		final MultiSwingWorker<Boolean, AttributeStatisticsPanel> worker = new MultiSwingWorker<Boolean,
				AttributeStatisticsPanel>() {

			@Override
			protected Boolean doInBackground() throws Exception {
				// prepare attribute lists and settings
				List<AbstractAttributeStatisticsModel> orderedAttributeStatisticsModelList = new LinkedList<>();
				List<Attribute> listOfAttributes = new LinkedList<>();

				List<AttributeRole> listOfAttributeRoles = new ArrayList<>(
						model.getExampleSetOrNull().getAttributes().specialSize());
				Iterator<AttributeRole> specialAttributes = model.getExampleSetOrNull().getAttributes().specialAttributes();
				while (specialAttributes.hasNext()) {
					listOfAttributeRoles.add(specialAttributes.next());
				}
				Collections.sort(listOfAttributeRoles, ExampleSetUtilities.SPECIAL_ATTRIBUTES_ROLE_COMPARATOR);
				for (int i = 0; i < listOfAttributeRoles.size(); i++) {
					listOfAttributes.add(listOfAttributeRoles.get(i).getAttribute());
				}

				Iterator<AttributeRole> regularAttributes = model.getExampleSetOrNull().getAttributes().regularAttributes();
				while (regularAttributes.hasNext()) {
					listOfAttributes.add(regularAttributes.next().getAttribute());
				}

				// we want to be notified of enlarge events, because we want to
				// keep the scrollbar
				// at the bottom
				AttributeStatisticsEventListener attEventListener = new AttributeStatisticsEventListener() {

					@Override
					public void modelChanged(final AttributeStatisticsEvent e) {
						if (e.getEventType() == EventType.ENLARGED_CHANGED) {
							JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
							// if the scrollbar was at the bottom before, ask it
							// to place itself at
							// the bottom again next adjustment
							if (scrollBar.getValue() + scrollBar.getHeight() >= scrollBar.getMaximum()) {
								scrollBar.putClientProperty(PROPERTY_SCROLL_DOWN, true);
							}
						}
					}
				};
				// iterate over all attributes, create models for them
				for (Attribute att : listOfAttributes) {
					AbstractAttributeStatisticsModel statModel;
					if (att.isNumerical()) {
						statModel = new NumericalAttributeStatisticsModel(model.getExampleSetOrNull(), att);
					} else if (att.isNominal()) {
						statModel = new NominalAttributeStatisticsModel(model.getExampleSetOrNull(), att);
					} else {
						statModel = new DateTimeAttributeStatisticsModel(model.getExampleSetOrNull(), att);
					}
					statModel.registerEventListener(attEventListener);
					orderedAttributeStatisticsModelList.add(statModel);
				}
				controller.setAttributeStatisticsModels(orderedAttributeStatisticsModelList);

				// iterate over all attributes and create the GUI for them
				int index = 0;
				for (int i = 0; i < Math.min(MetaDataStatisticsModel.PAGE_SIZE,
						orderedAttributeStatisticsModelList.size()); i++) {
					AttributeStatisticsPanel asp = new AttributeStatisticsPanel();
					mapOfAttributeStatisticsPanels.put(index++, asp);
					publish(asp);
				}

				// wait until statistics calculation is done or aborted
				return controller.waitAtBarrier();
			}

			@Override
			public void process(final List<AttributeStatisticsPanel> list) {
				// add the AttributeStatisticsPanel on the attribute panel to
				// display them
				for (AttributeStatisticsPanel asp : list) {
					gbcAttPanel.gridy += 1;
					attPanel.add(asp, gbcAttPanel);
				}
			}

			@Override
			protected void done() {
				try {
					boolean statisticsSuccess = get();
					if (statisticsSuccess) {
						// remove placeholder
						attPanel.remove(labelLoading);

						// once all are done refresh the GUI so they are shown
						MetaDataStatisticsViewer.this.revalidate();
						MetaDataStatisticsViewer.this.repaint();

						// allow resizing now
						resizeNameColumnLabel.setVisible(true);
					} else {
						labelLoading.setText(I18N.getMessage(I18N.getGUIBundle(),
								"gui.label.meta_data_stats.cancelled.label"));
						labelLoading.setIcon(CANCELLATION_ICON);
					}
				} catch (Exception e) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.meta_data_view.calc_error", e);
				}
			}
		};
		worker.start();
	}


	/**
	 * Setup the GUI. This does NOT include creating the {@link AttributeStatisticsPanel}s, as that
	 * is done via a {@link MultiSwingWorker} above. Reason is that we do not want to risk GUI freezes.
	 *
	 */
	private void initGUI() {
		outerPanel = new JPanel();
		outerPanel.setLayout(new GridBagLayout());
		outerPanel.setBackground(COLOR_BACKGROUND);

		attPanel = new JPanel();
		attPanel.setOpaque(false);
		attPanel.setLayout(new GridBagLayout());
		JPanel footerPanel = new JPanel();
		footerPanel.setLayout(new GridBagLayout());
		footerPanel.setOpaque(false);
		footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.TEXTFIELD_BORDER));

		GridBagConstraints gbcOuter = new GridBagConstraints();
		gbcOuter.insets = new Insets(5, 30, 5, 30);
		gbcOuter.anchor = GridBagConstraints.WEST;
		gbcOuter.fill = GridBagConstraints.NONE;
		gbcOuter.weightx = 1.0;
		gbcOuter.gridx = 0;
		gbcOuter.gridy = 0;

		// create attribute 'column' headers
		columnHeaderPanel = new JPanel();
		columnHeaderPanel.setOpaque(false);
		columnHeaderPanel.setLayout(new GridBagLayout());
		columnHeaderPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 5));
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(2, 53, 2, 10);
		gbc.weightx = 0.0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.NONE;
		sortingLabelAttName = new JLabel(
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.headers.name.label"));
		sortingLabelAttName
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.headers.name.tip"));
		sortingLabelAttName.setMinimumSize(DIMENSION_HEADER_ATTRIBUTE_NAME);
		sortingLabelAttName.setPreferredSize(DIMENSION_HEADER_ATTRIBUTE_NAME);
		sortingLabelAttName.setFont(sortingLabelAttName.getFont().deriveFont(FONT_SIZE_LABEL));
		sortingLabelAttName.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		sortingLabelAttName.setHorizontalTextPosition(SwingConstants.LEADING);
		sortingLabelAttName.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(final MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e)) {
					return;
				}
				controller.cycleAttributeNameSorting();
			}

		});
		columnHeaderPanel.add(sortingLabelAttName, gbc);

		// this label can be dragged and dropped to resize the attribute name
		// column
		resizeNameColumnLabel = new JLabel(
				SwingTools.createIcon("16/" + I18N.getGUIMessage("gui.label.meta_data_stats.resize.icon")));
		resizeNameColumnLabel.setToolTipText(I18N.getGUIMessage("gui.label.meta_data_stats.resize.tip"));
		resizeNameColumnLabel.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
		// only show once GUI is initialized
		resizeNameColumnLabel.setVisible(false);

		ResizeAttributeNameMouseListener resizeMouseListener = new ResizeAttributeNameMouseListener();
		resizeNameColumnLabel.addMouseListener(resizeMouseListener);
		resizeNameColumnLabel.addMouseMotionListener(resizeMouseListener);

		gbc.gridx += 1;
		gbc.insets = new Insets(0, 10, 0, 10);
		columnHeaderPanel.add(resizeNameColumnLabel, gbc);

		gbc.gridx += 1;
		gbc.insets = new Insets(2, 10, 2, 10);
		sortingLabelAttType = new JLabel(
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.headers.type.label"));
		sortingLabelAttType
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.headers.type.tip"));
		sortingLabelAttType.setMinimumSize(DIMENSION_HEADER_TYPE);
		sortingLabelAttType.setPreferredSize(DIMENSION_HEADER_TYPE);
		sortingLabelAttType.setFont(sortingLabelAttType.getFont().deriveFont(FONT_SIZE_LABEL));
		sortingLabelAttType.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		sortingLabelAttType.setHorizontalTextPosition(SwingConstants.LEADING);
		sortingLabelAttType.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(final MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e)) {
					return;
				}
				controller.cycleAttributeTypeSorting();
			}
		});
		columnHeaderPanel.add(sortingLabelAttType, gbc);

		gbc.gridx += 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0.0;
		sortingLabelAttMissings = new JLabel(
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.headers.missing.label"));
		sortingLabelAttMissings
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.headers.missing.tip"));
		sortingLabelAttMissings.setMinimumSize(DIMENSION_HEADER_MISSINGS);
		sortingLabelAttMissings.setPreferredSize(DIMENSION_HEADER_MISSINGS);
		sortingLabelAttMissings.setFont(sortingLabelAttMissings.getFont().deriveFont(FONT_SIZE_LABEL));
		sortingLabelAttMissings.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		sortingLabelAttMissings.setHorizontalTextPosition(SwingConstants.LEADING);
		sortingLabelAttMissings.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(final MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e)) {
					return;
				}
				controller.cycleAttributeMissingSorting();
			}
		});
		columnHeaderPanel.add(sortingLabelAttMissings, gbc);

		gbc.gridx += 1;
		gbc.insets = new Insets(2, 10, 2, 10);
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		JLabel labelAttStats = new JLabel(
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.headers.stats.label"));
		labelAttStats.setFont(labelAttStats.getFont().deriveFont(FONT_SIZE_LABEL));
		columnHeaderPanel.add(labelAttStats, gbc);

		// create dropdown filters
		filterLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.filter.filters.label"));
		gbc.gridx += 1;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.insets = new Insets(2, 5, 2, 10);
		columnHeaderPanel.add(filterLabel, gbc);

		List<JCheckBox> listOfValueTypeCheckboxses = new LinkedList<>();
		for (final String valueTypeName : Ontology.ATTRIBUTE_VALUE_TYPE.getNames()) {
			// we only want filters for numerical/nominal/date_time
			if (Ontology.ATTRIBUTE_VALUE_TYPE.mapName(valueTypeName) != Ontology.NUMERICAL
					&& Ontology.ATTRIBUTE_VALUE_TYPE.mapName(valueTypeName) != Ontology.NOMINAL
					&& Ontology.ATTRIBUTE_VALUE_TYPE.mapName(valueTypeName) != Ontology.DATE_TIME) {
				continue;
			}

			String valueTypeString = valueTypeName;
			valueTypeString = valueTypeString.replaceAll("_", " ");
			final JCheckBox filterValueTypeCheckbox = new JCheckBox(I18N.getMessage(I18N.getGUIBundle(),
					"gui.label.meta_data_stats.filter.value_type.label", valueTypeString));
			filterValueTypeCheckbox.setSelected(true);
			filterValueTypeCheckbox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {
					controller.setAttributeTypeVisibility(Ontology.ATTRIBUTE_VALUE_TYPE.mapName(valueTypeName),
							filterValueTypeCheckbox.isSelected());
				}
			});

			listOfValueTypeCheckboxses.add(filterValueTypeCheckbox);
		}

		final JCheckBox filterMissingsCheckbox = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.filter.missings.label"));
		filterMissingsCheckbox.setSelected(false);
		filterMissingsCheckbox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				controller.setShowOnlyMissingsAttributes(filterMissingsCheckbox.isSelected());
			}
		});

		final JCheckBox filterSpecialCheckbox = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.filter.special.label"));
		filterSpecialCheckbox.setSelected(true);
		filterSpecialCheckbox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				controller.setShowSpecialAttributes(filterSpecialCheckbox.isSelected());
			}
		});

		final JCheckBox filterRegularCheckbox = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.filter.regular.label"));
		filterRegularCheckbox.setSelected(true);
		filterRegularCheckbox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				controller.setShowRegularAttributes(filterRegularCheckbox.isSelected());
			}
		});

		final ScrollableJPopupMenu filterMenu = new ScrollableJPopupMenu();

		for (JCheckBox valueTypeBox : listOfValueTypeCheckboxses) {
			filterMenu.add(valueTypeBox);
		}
		filterMenu.addSeparator();
		filterMenu.add(filterMissingsCheckbox);
		filterMenu.addSeparator();
		filterMenu.add(filterSpecialCheckbox);
		filterMenu.add(filterRegularCheckbox);

		final JTextField filterNameField = new JTextField(10);
		filterNameField.setMinimumSize(new Dimension(300, 20));
		filterNameField.setPreferredSize(new Dimension(300, 20));

		final ResourceAction filterAction = new ResourceAction(true, "meta_data_stats.filter") {

			private static final long serialVersionUID = 5334802828535128169L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {
				controller.setFilterNameString(filterNameField.getText());
			}

		};
		filterNameField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.filter_field.tip"));
		filterNameField.addActionListener(filterAction);
		filterNameField.getDocument().addDocumentListener(new DocumentListener() {

			private Timer updateTimer;

			{
				updateTimer = new Timer(FILTER_TIMER_DELAY, filterAction);
				updateTimer.setRepeats(false);
			}

			@Override
			public void removeUpdate(final DocumentEvent e) {
				updateTimer.restart();
			}

			@Override
			public void insertUpdate(final DocumentEvent e) {
				updateTimer.restart();
			}

			@Override
			public void changedUpdate(final DocumentEvent e) {
				updateTimer.restart();
			}
		});
		filterNameField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
				ACTION_NAME_SEARCH);
		filterNameField.getActionMap().put(ACTION_NAME_SEARCH, new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				filterNameField.requestFocusInWindow();
			}
		});
		SwingTools.setPrompt(I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.filter_field.prompt"),
				filterNameField);

		ResourceAction deleteFilterAction = new ResourceAction(true, "meta_data_stats.filter_delete") {

			private static final long serialVersionUID = 8540175790623212824L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {
				filterNameField.setText("");
			}
		};
		gbc.gridx += 1;
		gbc.insets = new Insets(2, 0, 2, 0);
		TextFieldWithAction searchField = new TextFieldWithAction(filterNameField, deleteFilterAction,
				CLEAR_FILTER_HOVERED_ICON);
		searchField.setMinimumSize(DIMENSION_SEARCH_FIELD);
		searchField.setPreferredSize(DIMENSION_SEARCH_FIELD);
		columnHeaderPanel.add(searchField, gbc);

		final DropDownPopupButton filterDropdownButton = new DropDownPopupButton("gui.label.meta_data_stats.filter_select",
				new PopupMenuProvider() {

					@Override
					public JPopupMenu getPopupMenu() {
						return filterMenu;
					}
				});
		filterDropdownButton.setArrowSize(15);
		filterDropdownButton.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		filterDropdownButton.addMouseListener(new HoverBorderMouseListener(filterDropdownButton));

		gbc.gridx += 1;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.weightx = 0.0;
		gbc.insets = new Insets(2, 5, 2, 5);
		columnHeaderPanel.add(filterDropdownButton, gbc);

		// add copy all meta data to clipboard popup
		MouseListener copyMetaDataListener = new MouseAdapter() {

			@Override
			public void mouseReleased(final MouseEvent e) {
				handlePopup(e);
			}

			@Override
			public void mousePressed(final MouseEvent e) {
				handlePopup(e);
			}

			private void handlePopup(final MouseEvent e) {
				if (e.isPopupTrigger()) {
					JPopupMenu menu = new JPopupMenu();
					menu.add(new CopyStringToClipboardAction(true, "meta_data_stats.copy_all_metadata", MetaDataStatisticsViewer.this::createClipboardData));
					menu.add(new ShowConstructionValueAction(model));
					menu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		};
		columnHeaderPanel.addMouseListener(copyMetaDataListener);
		attPanel.addMouseListener(copyMetaDataListener);
		outerPanel.addMouseListener(copyMetaDataListener);

		// prepare attributes GUI
		gbcAttPanel = (GridBagConstraints) gbcOuter.clone();
		gbcAttPanel.gridx = 0;
		gbcAttPanel.gridy = 0;
		gbcAttPanel.fill = GridBagConstraints.HORIZONTAL;
		gbcAttPanel.insets = new Insets(3, 0, 0, 0);
		gbcAttPanel.weightx = 1.0;
		// add placeholder loading label to top of panel
		labelLoading = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.loading.label"));
		labelLoading.setIcon(SwingTools
				.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.loading.icon")));
		labelLoading.setHorizontalAlignment(SwingConstants.CENTER);
		attPanel.add(labelLoading, gbcAttPanel);
		// add no attributes label
		labelNoAttributes = new JLabel(
				I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.empty_filtered_attributes.label"));
		labelNoAttributes.setVisible(false);
		labelNoAttributes.setIcon(SwingTools.createIcon(
				"16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.empty_filtered_attributes.icon")));
		labelNoAttributes.setHorizontalAlignment(SwingConstants.CENTER);
		gbcAttPanel.gridy += 1;
		attPanel.add(labelNoAttributes, gbcAttPanel);

		// add to outer panel
		gbcOuter.gridy += 1;
		gbcOuter.insets = new Insets(0, 10, 5, 10);
		gbcOuter.fill = GridBagConstraints.HORIZONTAL;
		outerPanel.add(attPanel, gbcOuter);

		// add filler so elements added stay at the top
		gbcOuter.weighty = 1.0;
		gbcOuter.insets = new Insets(20, 0, 5, 0);
		gbcOuter.fill = GridBagConstraints.BOTH;
		gbcOuter.gridy += 1;
		outerPanel.add(Box.createVerticalBox(), gbcOuter);

		// footer panel
		JPanel footerStatPanel = new JPanel();
		footerStatPanel.setOpaque(false);
		footerStatPanel.setLayout(new GridBagLayout());

		GridBagConstraints gbcFooterStat = new GridBagConstraints();
		gbcFooterStat.gridx = 0;
		gbcFooterStat.gridy = 0;
		gbcFooterStat.weightx = 1.0;
		gbcFooterStat.fill = GridBagConstraints.HORIZONTAL;
		footerStatPanel.add(Box.createVerticalBox(), gbcFooterStat);

		String countExaHeader = I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.count_examples.label",
				model.getExampleSetOrNull().size());
		JLabel countExaLabel = new JLabel(countExaHeader);
		gbcFooterStat.gridx += 1;
		gbcFooterStat.insets = new Insets(2, 5, 2, 5);
		gbcFooterStat.weightx = 0.0;
		gbcFooterStat.fill = GridBagConstraints.NONE;
		footerStatPanel.add(countExaLabel, gbcFooterStat);

		String countSpecAttHeader = I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.count_att_special.label",
				model.getExampleSetOrNull().getAttributes().specialSize());
		JLabel countSpecAttLabel = new JLabel(countSpecAttHeader);
		gbcFooterStat.gridx += 1;
		footerStatPanel.add(countSpecAttLabel, gbcFooterStat);

		String countRegAttHeader = I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.count_att_regular.label",
				model.getExampleSetOrNull().getAttributes().size());
		JLabel countRegAttLabel = new JLabel(countRegAttHeader);
		gbcFooterStat.gridx += 1;
		gbcFooterStat.insets = new Insets(2, 5, 2, 10);
		footerStatPanel.add(countRegAttLabel, gbcFooterStat);

		// add footer stat panel
		GridBagConstraints gbcFooter = new GridBagConstraints();
		gbcFooter.gridx = 0;
		gbcFooter.gridy = 0;
		gbcFooter.fill = GridBagConstraints.HORIZONTAL;
		gbcFooter.weightx = 1.0;
		gbcFooter.insets = new Insets(2, 10, 5, 0);
		labelDisplayedAttribute = new JLabel("...");
		labelDisplayedAttribute.setMinimumSize(footerStatPanel.getPreferredSize());
		labelDisplayedAttribute.setPreferredSize(footerStatPanel.getPreferredSize());
		labelDisplayedAttribute.setHorizontalTextPosition(SwingConstants.LEFT);
		footerPanel.add(labelDisplayedAttribute, gbcFooter);

		paginationPanel = new JPanel();
		paginationPanel.setOpaque(false);
		paginationPanel.setLayout(new GridBagLayout());
		buttonFirstPage = new JButton(ICON_ARROW_FIRST);
		buttonFirstPage.setContentAreaFilled(false);
		buttonFirstPage.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.page_first.tip"));
		buttonFirstPage.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				controller.setCurrentPageIndexToFirstPage();
			}

		});
		buttonFirstPage.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		buttonFirstPage.addMouseListener(new HoverBorderMouseListener(buttonFirstPage));
		GridBagConstraints gbcPagination = new GridBagConstraints();
		gbcPagination.gridx = 0;
		gbcPagination.gridy = 0;
		paginationPanel.add(buttonFirstPage, gbcPagination);

		buttonPreviousPage = new JButton(ICON_ARROW_LEFT);
		buttonPreviousPage.setContentAreaFilled(false);
		buttonPreviousPage
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.page_previous.tip"));
		buttonPreviousPage.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				controller.decrementCurrentPageIndex();
			}

		});
		buttonPreviousPage.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		buttonPreviousPage.addMouseListener(new HoverBorderMouseListener(buttonPreviousPage));
		gbcPagination.gridx += 1;
		paginationPanel.add(buttonPreviousPage, gbcPagination);

		pageComboListener = new ItemListener() {

			@Override
			public void itemStateChanged(final ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					controller.jumpToHumanPageIndex((Integer) pagesComboBox.getSelectedItem());
				}
			}
		};
		pagesComboBox = new JComboBox<>();
		pagesComboBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.page_select.tip"));
		pagesComboBox.addItemListener(pageComboListener);
		gbcPagination.gridx += 1;
		paginationPanel.add(pagesComboBox, gbcPagination);

		buttonNextPage = new JButton(ICON_ARROW_RIGHT);
		buttonNextPage.setContentAreaFilled(false);
		buttonNextPage.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.page_next.tip"));
		buttonNextPage.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				controller.incrementCurrentPageIndex();
			}
		});
		buttonNextPage.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		buttonNextPage.addMouseListener(new HoverBorderMouseListener(buttonNextPage));
		gbcPagination.gridx += 1;
		paginationPanel.add(buttonNextPage, gbcPagination);

		buttonLastPage = new JButton(ICON_ARROW_LAST);
		buttonLastPage.setContentAreaFilled(false);
		buttonLastPage.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.page_last.tip"));
		buttonLastPage.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				controller.setCurrentPageIndexToLastPage();
			}

		});
		buttonLastPage.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		buttonLastPage.addMouseListener(new HoverBorderMouseListener(buttonLastPage));
		gbcPagination.gridx += 1;
		paginationPanel.add(buttonLastPage, gbcPagination);
		gbcFooter.gridx += 1;
		gbcFooter.fill = GridBagConstraints.NONE;
		gbcFooter.weightx = 0.0;
		gbcFooter.insets = new Insets(0, 0, 0, 0);
		gbcFooter.anchor = GridBagConstraints.CENTER;
		footerPanel.add(paginationPanel, gbcFooter);

		// add filler
		gbcFooter.gridx += 1;
		gbcFooter.fill = GridBagConstraints.HORIZONTAL;
		gbcFooter.weightx = 1.0;
		footerPanel.add(footerStatPanel, gbcFooter);

		// build main GUI
		setLayout(new BorderLayout());
		// add header panel
		add(columnHeaderPanel, BorderLayout.NORTH);

		// add outer panel which contains all attribute stat panels to
		// scrollpane
		scrollPane = new ExtendedJScrollPane(outerPanel);
		scrollPane.setOpaque(false);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(final AdjustmentEvent e) {
				JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
				if (scrollBar.getClientProperty(PROPERTY_SCROLL_DOWN) != null) {
					scrollBar.setValue(scrollBar.getMaximum());
					scrollBar.putClientProperty(PROPERTY_SCROLL_DOWN, null);
				}

			}
		});
		add(scrollPane, BorderLayout.CENTER);

		// add footer panel
		add(footerPanel, BorderLayout.SOUTH);

		setBackground(COLOR_BACKGROUND);
	}

	@Override
	public void prepareRendering() {}

	@Override
	public void finishRendering() {}

	@Override
	public void render(final Graphics graphics, final int width, final int height) {
		paintComponent(graphics);
	}

	@Override
	public int getRenderWidth(final int preferredWidth) {
		return (int) getPreferredSize().getWidth();
	}

	@Override
	public int getRenderHeight(final int preferredHeight) {
		return (int) getPreferredSize().getHeight();
	}

	/**
	 * Updates the sorting icons.
	 */
	private void updateSortingIcons() {
		for (SortingType type : SortingType.values()) {
			SortingDirection direction = model.getSortingDirection(type);
			ImageIcon icon;
			switch (direction) {
				case DESCENDING:
					icon = ICON_ARROW_DOWN;
					break;
				case ASCENDING:
					icon = ICON_ARROW_UP;
					break;
				case UNDEFINED:
					icon = null;
					break;
				default:
					icon = null;
			}
			switch (type) {
				case NAME:
					sortingLabelAttName.setIcon(icon);
					break;
				case TYPE:
					sortingLabelAttType.setIcon(icon);
					break;
				case MISSING:
					sortingLabelAttMissings.setIcon(icon);
					break;
				default:
					sortingLabelAttName.setIcon(null);
					sortingLabelAttType.setIcon(null);
					sortingLabelAttMissings.setIcon(null);
			}
		}
	}

	/**
	 * Reorders the attribute panels according to the current sorting and respect filtering.
	 */
	private void revalidateAttributePanels() {
		// update pagination system to current data
		int pages = model.getNumberOfPages();
		Vector<Integer> pagesVector = new Vector<>();
		for (int j = 1; j <= pages; j++) {
			pagesVector.add(j);
		}
		if (pagesComboBox.getSelectedItem() != null) {
			pagesComboBox.setModel(new DefaultComboBoxModel<>(pagesVector));

			// try to restore selection after model change
			pagesComboBox.removeItemListener(pageComboListener);
			pagesComboBox.setSelectedItem(model.getCurrentPageIndex() + 1);
			pagesComboBox.addItemListener(pageComboListener);
		} else {
			pagesComboBox.setModel(new DefaultComboBoxModel<>(pagesVector));
		}
		updatePagingDisplay();

		int index = 0;
		List<AbstractAttributeStatisticsModel> orderedList = controller.getPagedAndVisibleAttributeStatisticsModels();
		for (AbstractAttributeStatisticsModel statModel : orderedList) {
			mapOfAttributeStatisticsPanels.get(index++).setModel(statModel, true);
		}

		// update alternating and visibility
		int i = 0;
		for (AbstractAttributeStatisticsModel statModel : orderedList) {
			mapOfAttributeStatisticsPanels.get(i).setVisible(true);
			statModel.setAlternating(i % 2 == 1);
			i++;
		}
		// we may have less than Math.min(size, PAGE_SIZE) panels to show, hide
		// unused ones
		for (; i < Math.min(model.getTotalSize(), MetaDataStatisticsModel.PAGE_SIZE); i++) {
			mapOfAttributeStatisticsPanels.get(i).setVisible(false);
		}

		// show a "no attributes visible in filter" when nothing is to display
		if (model.getVisibleSize() == 0 && model.isFiltering()) {
			labelNoAttributes.setVisible(true);
		} else {
			labelNoAttributes.setVisible(false);
		}

		// make sure attribute name column width is updated
		for (AttributeStatisticsPanel asp : mapOfAttributeStatisticsPanels.values()) {
			// sync size of header column with actual ASP columns
			asp.updateNameColumnWidth(nameDim);
		}

		// repaint everything
		revalidate();
		repaint();
	}

	/**
	 * Updates the paging buttons state as well as the label displaying the currently visible number
	 * of attributes.
	 */
	private void updatePagingDisplay() {
		// only enable first page button if we are not on the first page
		buttonFirstPage.setEnabled(model.getCurrentPageIndex() != 0);
		// only enable previous button if there are previous pages
		buttonPreviousPage.setEnabled(model.getCurrentPageIndex() != 0);
		// only enable next button if we are not on the last page
		buttonNextPage.setEnabled(model.getCurrentPageIndex() < model.getNumberOfPages() - 1);
		// only enable last page button if we are not on the last page
		buttonLastPage.setEnabled(model.getCurrentPageIndex() < model.getNumberOfPages() - 1);

		// hide pagination system if only one page is to show
		paginationPanel.setVisible(model.getNumberOfPages() > 1);

		// show how many attributes are visible / total
		filterLabel.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.meta_data_stats.filter.filters.label",
				model.getVisibleSize(), model.getTotalSize()));

		// show the number of attributes displayed on the current page
		int minNumber = Math.min(model.getCurrentPageIndex() * MetaDataStatisticsModel.PAGE_SIZE + 1,
				model.getVisibleSize());
		int maxNumber = Math.min((model.getCurrentPageIndex() + 1) * MetaDataStatisticsModel.PAGE_SIZE,
				model.getVisibleSize());
		labelDisplayedAttribute.setText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.label.meta_data_stats.filter.showing_attributes.label", minNumber, maxNumber));
	}

	@Override
	public Component getExportComponent() {
		return outerPanel;
	}

	@Override
	public String getExportName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.cards.result_view.meta_data_view.title");
	}

	@Override
	public String getIdentifier() {
		ExampleSet exampleSetOrNull = model.getExampleSetOrNull();
		if (exampleSetOrNull != null) {
			return exampleSetOrNull.getSource();
		}
		return null;
	}

	@Override
	public String getExportIconName() {
		return I18N.getGUIMessage("gui.cards.result_view.meta_data_view.icon");
	}

	@Override
	public void cleanUp() {
		stop();
	}

	/**
	 * Stops the statistics calculation.
	 */
	public void stop() {
		controller.stop();
	}

	/**
	 * Creates the MD that goes into the clipboard on copy.
	 */
	private String createClipboardData() {
		StringBuilder sb = new StringBuilder();
		for (AbstractAttributeStatisticsModel statModel : model.getOrderedAttributeStatisticsModels()) {
			// append general stats like name, type, missing values
			sb.append(statModel.getAttribute().getName());
			sb.append("\t");

			String valueTypeString = Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(statModel.getAttribute().getValueType());
			valueTypeString = valueTypeString.replaceAll("_", " ");
			valueTypeString = String.valueOf(valueTypeString.charAt(0)).toUpperCase() + valueTypeString.substring(1);
			sb.append(valueTypeString);
			sb.append("\t");

			sb.append(Tools.formatIntegerIfPossible(statModel.getNumberOfMissingValues()));
			sb.append("\t");

			// if construction is shown, also add it
			if (statModel.isShowConstruction()) {
				String construction = statModel.getConstruction();
				construction = construction == null ? "-" : construction;
				sb.append(construction);
				sb.append("\t");
			}

			// append value type specific stuff
			if (NumericalAttributeStatisticsModel.class.isAssignableFrom(statModel.getClass())) {
				sb.append(((NumericalAttributeStatisticsModel) statModel).getAverage());
				sb.append("\t");

				sb.append(((NumericalAttributeStatisticsModel) statModel).getDeviation());
				sb.append("\t");

				sb.append(((NumericalAttributeStatisticsModel) statModel).getMinimum());
				sb.append("\t");

				sb.append(((NumericalAttributeStatisticsModel) statModel).getMaximum());
			} else if (NominalAttributeStatisticsModel.class.isAssignableFrom(statModel.getClass())) {
				int count = 0;
				List<String> valueStrings = ((NominalAttributeStatisticsModel) statModel).getValueStrings();
				for (String valueString : valueStrings) {
					sb.append(valueString);
					if (count < valueStrings.size() - 1) {
						sb.append(", ");
					}

					count++;
				}
			} else if (DateTimeAttributeStatisticsModel.class.isAssignableFrom(statModel.getClass())) {
				sb.append(((DateTimeAttributeStatisticsModel) statModel).getDuration());
				sb.append("\t");

				sb.append(((DateTimeAttributeStatisticsModel) statModel).getFrom());
				sb.append("\t");

				sb.append(((DateTimeAttributeStatisticsModel) statModel).getUntil());
			}

			// next row for next attribute
			sb.append(System.lineSeparator());
		}

		return sb.toString();
	}
}
