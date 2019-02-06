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
package com.rapidminer.gui.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.logging.Level;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.EventListenerList;

import com.rapidminer.gui.LoggedAbstractAction;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.search.event.GlobalSearchInteractionEvent;
import com.rapidminer.gui.search.event.GlobalSearchInteractionListener;
import com.rapidminer.gui.search.model.GlobalSearchRow;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.components.LinkLocalButton;
import com.rapidminer.search.GlobalSearchCategory;
import com.rapidminer.search.GlobalSearchResult;
import com.rapidminer.search.GlobalSearchUtilities;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector.UsageObject;
import com.rapidminer.tools.usagestats.UsageLoggable;


/**
 * Panel visualizing a single {@link GlobalSearchCategory}.
 *
 * @author Marco Boeck
 * @since 8.1
 */
class GlobalSearchCategoryPanel extends JPanel {

	/**
	 * Simple POJO implementation of {@link UsageObject} to log stats
	 * using {@link ActionStatisticsCollector#logGlobalSearchAction(String, String, String)}.
	 *
	 * @author Jan Czogalla
	 * @since 8.1.2
	 */
	private static final class GlobalSearchActionUsageObject implements UsageObject {

		private final String query;
		private final String categoryID;
		private final String rowID;

		private GlobalSearchActionUsageObject(String query, String categoryID, String rowID) {
			this.query = query;
			this.categoryID = categoryID;
			this.rowID = rowID;
		}

		@Override
		public void logUsage() {
			ActionStatisticsCollector.getInstance().logGlobalSearchAction(query, categoryID, rowID);
		}
	}

	private static final Color BORDER_COLOR = Colors.BUTTON_BORDER_DISABLED;
	private static final Border TOP_BORDER = BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR);
	private static final Border DIVIDER_BORDER = BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_COLOR);
	private static final Border CATEGORY_LABEL_EMPTY_BORDER = BorderFactory.createEmptyBorder(10, 10, 10, 10);
	private static final Border CATEGORY_COMPONENT_EMPTY_BORDER = BorderFactory.createEmptyBorder(4, 5, 4, 0);
	public static final int I18N_NAME_WIDTH = 100;
	private static final Dimension I18N_NAME_SIZE = new Dimension(I18N_NAME_WIDTH, 30);

	/**
	 * event listener for this panel
	 */
	private final EventListenerList eventListener;

	private String categoryId;
	private JPanel contentPanel;

	private final transient GlobalSearchController controller;

	/**
	 * keep track of the selected entry index and the available number of result entries that can be selected, includes link to more
	 */
	private int selectedSearchResultPanelIndex = -1;
	private int availableResultRows = -1;

	/**
	 * Create the panel displaying results for a global search category.
	 *
	 * @param category
	 * 		the category for which this panel is
	 * @param controller
	 * 		the controller instance for UI manipulation
	 * @throws IllegalArgumentException
	 * 		if arguments are {@code null} or invalid; if no GUI provider is registered for the given category
	 */
	protected GlobalSearchCategoryPanel(final GlobalSearchCategory category, final GlobalSearchController controller) {
		if (category == null || category.getCategoryId() == null) {
			throw new IllegalArgumentException("category and its ID must not be null!");
		}
		this.categoryId = category.getCategoryId();
		GlobalSearchableGUIProvider provider = GlobalSearchGUIRegistry.INSTANCE.getGUIProviderForSearchCategoryById(categoryId);
		if (provider == null) {
			throw new IllegalArgumentException("GlobalSearchableGUIProvider must not be null for category " + categoryId + "!");
		}

		this.controller = controller;
		this.eventListener = new EventListenerList();

		initGUI(provider);
	}

	/**
	 * Override to help debugging.
	 */
	@Override
	public String toString() {
		return "GSCatPanel: " + categoryId + " - #children: " + contentPanel.getComponentCount();
	}

	/**
	 * Adds a {@link GlobalSearchInteractionListener} which will be informed of all user interactions with {@link GlobalSearchRow}s.
	 *
	 * @param listener
	 * 		the listener instance to add
	 */
	protected void registerEventListener(final GlobalSearchInteractionListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}
		eventListener.add(GlobalSearchInteractionListener.class, listener);
	}

	/**
	 * Removes the {@link GlobalSearchInteractionListener} from this model.
	 *
	 * @param listener
	 * 		the listener instance to remove
	 */
	protected void removeEventListener(final GlobalSearchInteractionListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}
		eventListener.remove(GlobalSearchInteractionListener.class, listener);
	}

	/**
	 * Sets the given rows as the already displayed results. Overwrites potentially displayed results.
	 *
	 * @param rows
	 * 		the rows to display
	 * @param result
	 * 		the search result instance
	 * @param justAnUpdate
	 * 		informs this method if the results were updated from a show more activation or if a new search was triggered
	 */
	protected void setSearchRows(final List<GlobalSearchRow> rows, final GlobalSearchResult result, boolean justAnUpdate) {
		int previouslySelectedEntry = selectedSearchResultPanelIndex;
		availableResultRows = rows.size();

		contentPanel.removeAll();

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0d;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;

		int index = 0;
		for (GlobalSearchRow row : rows) {
			gbc.gridy += 1;
			contentPanel.add(createUI(row, index), gbc);
			index++;
		}

		long deltaDisplayedResultsVsMaxResults = result.getPotentialNumberOfResults() - rows.size();
		if (deltaDisplayedResultsVsMaxResults > 0) {
			gbc.gridy += 1;

			String remainingNumber = Tools.formatIntegerIfPossible(deltaDisplayedResultsVsMaxResults, 0, true);
			GlobalSearchResultPanel wrapperPanel = new GlobalSearchResultPanel(null);
			final ResourceAction action = new ResourceAction("global_search.load_more", remainingNumber) {

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					controller.loadMoreRows(result, categoryId);
				}
			};
			LinkLocalButton loadMoreButton = new LinkLocalButton(action);

			final int loadMoreIndex = availableResultRows;
			// add mouse listener for hovering and activation
			MouseListener activationMouseListener = new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent e) {
					GlobalSearchPanel.getInstance().select(categoryId, loadMoreIndex);
				}
			};
			loadMoreButton.addMouseListener(activationMouseListener);
			wrapperPanel.setActivationAction(action);
			wrapperPanel.add(loadMoreButton);
			contentPanel.add(wrapperPanel, gbc);

			availableResultRows += 1;
		}

		// fill at bottom so components are at top
		if (!rows.isEmpty()) {
			gbc.gridy += 1;
			gbc.weighty = 1.0d;
			gbc.fill = GridBagConstraints.VERTICAL;
			contentPanel.add(new JLabel(), gbc);
		}

		// fireResultBrowsedInteraction previously selected entry if this was executed as an update or do something meaningful with the index
		if (justAnUpdate) {
			SwingUtilities.invokeLater(() -> setSelectedEntry(previouslySelectedEntry));
		} else {
			resetSelectedEntry();
		}
	}

	/**
	 * Returns if any search results are displayed in this category panel or if a search is pending.
	 *
	 * @return {@code true} if there are currently search results displayed or it is pending; {@code false} otherwise
	 */
	protected boolean hasResults() {
		return contentPanel.getComponentCount() > 0;
	}

	/**
	 * Init the GUI.
	 *
	 * @param provider
	 * 		the provider instance, must not be {@code null}
	 */
	private void initGUI(final GlobalSearchableGUIProvider provider) {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		setOpaque(false);
		setBorder(TOP_BORDER);

		JLabel i18nName = new JLabel();
		i18nName.setBackground(Colors.WINDOW_BACKGROUND);
		i18nName.setForeground(Color.GRAY);
		i18nName.setOpaque(true);
		i18nName.setVerticalAlignment(SwingConstants.TOP);
		i18nName.setHorizontalAlignment(SwingConstants.LEFT);
		i18nName.setFont(i18nName.getFont().deriveFont(Font.BOLD));
		i18nName.setText(provider.getI18nNameForSearchable());
		i18nName.setMinimumSize(I18N_NAME_SIZE);
		i18nName.setPreferredSize(I18N_NAME_SIZE);
		i18nName.setMaximumSize(I18N_NAME_SIZE);
		i18nName.setBorder(CATEGORY_LABEL_EMPTY_BORDER);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weighty = 1.0d;
		gbc.fill = GridBagConstraints.VERTICAL;
		add(i18nName, gbc);

		contentPanel = new JPanel();
		contentPanel.setLayout(new GridBagLayout());
		contentPanel.setBorder(DIVIDER_BORDER);
		contentPanel.setBackground(Colors.WHITE);

		gbc.gridx = 1;
		gbc.weightx = 1.0d;
		gbc.weighty = 1.0d;
		gbc.fill = GridBagConstraints.BOTH;
		add(contentPanel, gbc);
	}

	/**
	 * Creates the ui component for the given document. If the document cannot be mapped to any registered {@link
	 * GlobalSearchableGUIProvider}, it will be silently skipped and this returns {@code null}.
	 *
	 * @param row
	 * 		the search row instance for which to create the UI
	 * @param index
	 * 		position of this entry for selecting the entry correctly
	 * @return the component, never {@code null}
	 */
	private GlobalSearchResultPanel createUI(final GlobalSearchRow row, int index) {
		final GlobalSearchResultPanel wrapperPanel = new GlobalSearchResultPanel(row);

		final GlobalSearchableGUIProvider provider = GlobalSearchGUIRegistry.INSTANCE.getGUIProviderForSearchCategoryById(categoryId);
		JComponent component = provider != null ? provider.getGUIListComponentForDocument(row.getDoc(), row.getBestFragments()) : null;
		if (provider == null || component == null) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.search.globalsearchdialog.no_gui_component", categoryId);
			// to avoid an empty category, provide a very basic UI that only shows the name of a row
			wrapperPanel.add(new JLabel(row.getDoc().get(GlobalSearchUtilities.FIELD_NAME)));
			return wrapperPanel;
		}
		if (component.getPreferredSize().getHeight() > GlobalSearchGUIUtilities.MAX_HEIGHT) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.search.globalsearchdialog.gui_component_too_high", categoryId);
			// component does not obey height limit of 40px according to JavaDoc. Provide a very basic UI that only shows the name of a row
			component = new JLabel(row.getDoc().get(GlobalSearchUtilities.FIELD_NAME));
		}
		if (component.isOpaque()) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.search.globalsearchdialog.gui_component_opaque", categoryId);
			// component is opaque which is also forbidden according to JavaDoc. Provide a very basic UI that only shows the name of a row
			component = new JLabel(row.getDoc().get(GlobalSearchUtilities.FIELD_NAME));
		}
		component.setBorder(CATEGORY_COMPONENT_EMPTY_BORDER);
		wrapperPanel.add(component, BorderLayout.CENTER);

		final Action activationAction = new LoggedAbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				super.actionPerformed(e);
				GlobalSearchableGUIProvider.Veto providerVeto = new GlobalSearchableGUIProvider.Veto();
				provider.searchResultTriggered(row.getDoc(), providerVeto);
				// only fire interaction if provider did not veto it
				if (!providerVeto.isVeto()) {
					fireInteraction(GlobalSearchInteractionEvent.InteractionEvent.RESULT_ACTIVATED, row);
					ActionStatisticsCollector.getInstance().logGlobalSearchAction(controller.getLastQuery(), categoryId, row.getDoc().getField(GlobalSearchUtilities.FIELD_UNIQUE_ID).stringValue());
				}
			}
		};
		wrapperPanel.setActivationAction(activationAction);

		// add mouse listener for hovering and activation
		MouseListener activationMouseListener = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 2) {
					return;
				}
				wrapperPanel.doActivate();
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				GlobalSearchPanel.getInstance().select(categoryId, index);
			}
		};
		component.addMouseListener(activationMouseListener);

		component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		// add drag&drop support if applicable
		if (provider.isDragAndDropSupported(row.getDoc())) {
			DragGestureListener dragSupport = provider.getDragAndDropSupport(row.getDoc());
			if (dragSupport != null) {
				if (dragSupport instanceof UsageLoggable) {
					((UsageLoggable) dragSupport).setUsageObject(new GlobalSearchActionUsageObject(controller.getLastQuery(), categoryId, row.getDoc().getField(GlobalSearchUtilities.FIELD_UNIQUE_ID).stringValue()));
				}
				DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(component, DnDConstants.ACTION_MOVE, dragSupport);
			} else {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.search.globalsearchdialog.missing_drag_support", categoryId);
			}
		}

		if (index == selectedSearchResultPanelIndex) {
			wrapperPanel.highlightPanel(true);
			fireResultBrowsedInteraction(wrapperPanel.getRow());
		}
		return wrapperPanel;
	}

	/**
	 * On browsing the results interaction event listeners are being notified
	 *
	 * @param row
	 * 		the browsed row
	 */
	private void fireResultBrowsedInteraction(GlobalSearchRow row) {
		if (row != null) {
			fireInteraction(GlobalSearchInteractionEvent.InteractionEvent.RESULT_BROWSED, row);
		}
	}

	/**
	 * Fires the given {@link GlobalSearchInteractionEvent.InteractionEvent}.
	 *
	 * @param type
	 * 		the type of the event
	 * @param row
	 * 		the row with which the interaction happened
	 */
	private void fireInteraction(final GlobalSearchInteractionEvent.InteractionEvent type, final GlobalSearchRow row) {
		// Notify the listeners
		for (GlobalSearchInteractionListener listener : eventListener.getListeners(GlobalSearchInteractionListener.class)) {
			GlobalSearchInteractionEvent e = new GlobalSearchInteractionEvent(type, row);
			listener.interaction(e);
		}
	}

	/**
	 * Get access to the contents of the contentpanel
	 *
	 * @return the current contentpanel with all the {@link GlobalSearchResultPanel}
	 * @since 9.0.0
	 */
	JPanel getContentPanel() {
		return contentPanel;
	}

	/**
	 * Access to the currently selected index in the contentpanel from {@link GlobalSearchCategoryPanel#getContentPanel()}
	 *
	 * @return index of the selected entry or -1 if none is selected
	 * @since 9.0.0
	 */
	int getSelectedEntryIndex() {
		return selectedSearchResultPanelIndex;
	}

	/**
	 * Remove selection from this category panel
	 *
	 * @since 9.0.0
	 */
	void resetSelectedEntry() {
		setSelectedEntry(-1);
	}

	/**
	 * Select one entry, for instance when hovering with the mouse, to be able to change the selection with the keyboard afterwards
	 *
	 * @param selectedEntry
	 * 		index of the entry to select
	 * @return true if the selection succeeded
	 * @since 9.0.0
	 */
	boolean setSelectedEntry(int selectedEntry) {
		if (selectedSearchResultPanelIndex >= 0 && selectedSearchResultPanelIndex < availableResultRows && contentPanel.getComponent(selectedSearchResultPanelIndex) instanceof GlobalSearchResultPanel) {
			((GlobalSearchResultPanel) contentPanel.getComponent(selectedSearchResultPanelIndex)).highlightPanel(false);
		}
		if (selectedEntry < 0) {
			selectedSearchResultPanelIndex = -1;
			return true;
		} else if (selectedEntry < availableResultRows && contentPanel.getComponent(selectedEntry) instanceof GlobalSearchResultPanel) {
			selectedSearchResultPanelIndex = selectedEntry;
			final GlobalSearchResultPanel gsrPanel = (GlobalSearchResultPanel) contentPanel.getComponent(selectedSearchResultPanelIndex);
			gsrPanel.highlightPanel(true);
			final GlobalSearchableGUIProvider provider = GlobalSearchGUIRegistry.INSTANCE.getGUIProviderForSearchCategoryById(categoryId);
			if (provider != null && gsrPanel.getRow() != null) {
				provider.searchResultBrowsed(gsrPanel.getRow().getDoc());
			}
			fireResultBrowsedInteraction(gsrPanel.getRow());
			return true;
		}
		resetSelectedEntry();
		return false;
	}

	/**
	 * Select the next entry to be the actively selected entry
	 *
	 * @return true if there was another entry that is now selected, false if there are no further entries in this category
	 * @since 9.0.0
	 */
	boolean selectNext() {
		return selectedSearchResultPanelIndex + 1 < availableResultRows && setSelectedEntry(selectedSearchResultPanelIndex + 1);
	}

	/**
	 * Select the previous entry to be the actively selected entry
	 *
	 * @return true if there was a previous entry that is now selected, false if there was no previous entry available
	 * @since 9.0.0
	 */
	boolean selectPrevious() {
		return selectedSearchResultPanelIndex - 1 >= 0 && setSelectedEntry(selectedSearchResultPanelIndex - 1);
	}

	/**
	 * Shortcut to select index 0
	 *
	 * @return true if this was possible
	 * @since 9.0.0
	 */
	boolean selectFirst() {
		return setSelectedEntry(0);
	}

	/**
	 * Select the last available entry in this category
	 *
	 * @return true if it was possible
	 * @since 9.0.0
	 */
	boolean selectLast() {
		return setSelectedEntry(availableResultRows - 1);
	}

	/**
	 * Run the Action registered for the currently selected {@link GlobalSearchResultPanel}
	 *
	 * @since 9.0.0
	 */
	void activateSelectedEntry() {
		if (selectedSearchResultPanelIndex >= 0) {
			((GlobalSearchResultPanel) contentPanel.getComponent(selectedSearchResultPanelIndex)).doActivate();
		}
	}

	/**
	 * Name of the category this panel was created for
	 *
	 * @return the categoryId
	 * @since 9.0.0
	 */
	String getCategoryId() {
		return categoryId;
	}

}

