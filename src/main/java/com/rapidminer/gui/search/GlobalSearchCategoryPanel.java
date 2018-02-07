/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.EventListenerList;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.search.event.GlobalSearchInteractionEvent;
import com.rapidminer.gui.search.event.GlobalSearchInteractionEvent.InteractionEvent;
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


/**
 * Panel visualizing a single {@link GlobalSearchCategory}.
 *
 * @author Marco Boeck
 * @since 8.1
 */
class GlobalSearchCategoryPanel extends JPanel {


	private static final Color BORDER_COLOR = Colors.BUTTON_BORDER_DISABLED;
	private static final Border TOP_BORDER = BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR);
	private static final Border DIVIDER_BORDER = BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_COLOR);
	private static final Border CATEGORY_LABEL_EMPTY_BORDER = BorderFactory.createEmptyBorder(10, 10, 10, 10);
	private static final Border CATEGORY_COMPONENT_EMPTY_BORDER = BorderFactory.createEmptyBorder(4, 5, 4, 0);
	private static final Dimension I18N_NAME_SIZE = new Dimension(100, 30);

	/** event listener for this panel */
	private final EventListenerList eventListener;

	private String categoryId;
	private JPanel contentPanel;

	private final transient GlobalSearchController controller;


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
	 *            the listener instance to add
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
	 *            the listener instance to remove
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
	 */
	protected void setSearchRows(final List<GlobalSearchRow> rows, final GlobalSearchResult result) {
		contentPanel.removeAll();

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0d;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;

		for (GlobalSearchRow row : rows) {
			gbc.gridy += 1;
			contentPanel.add(createUI(row), gbc);
		}

		long deltaDisplayedResultsVsMaxResults = result.getPotentialNumberOfResults() - rows.size();
		if (deltaDisplayedResultsVsMaxResults > 0) {
			gbc.gridy += 1;

			String remainingNumber = Tools.formatIntegerIfPossible(deltaDisplayedResultsVsMaxResults, 0, true);
			LinkLocalButton loadMoreButton = new LinkLocalButton(new ResourceAction("global_search.load_more", remainingNumber) {

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					controller.loadMoreRows(result, categoryId);
				}
			});
			contentPanel.add(loadMoreButton, gbc);
		}

		// fill at bottom so components are at top
		if (!rows.isEmpty()) {
			gbc.gridy += 1;
			gbc.weighty = 1.0d;
			gbc.fill = GridBagConstraints.VERTICAL;
			contentPanel.add(new JLabel(), gbc);
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
	 * @return the component, never {@code null}
	 */
	private JComponent createUI(final GlobalSearchRow row) {
		final JPanel wrapperPanel = new JPanel();
		wrapperPanel.setOpaque(false);
		wrapperPanel.setBackground(Colors.TEXT_HIGHLIGHT_BACKGROUND);
		wrapperPanel.setLayout(new BorderLayout());

		final GlobalSearchableGUIProvider provider = GlobalSearchGUIRegistry.INSTANCE.getGUIProviderForSearchCategoryById(categoryId);
		JComponent component = provider != null ? provider.getGUIListComponentForDocument(row.getDoc(), row.getBestFragments()) : null;
		if (provider == null || component == null) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.search.globalsearchdialog.no_gui_component", categoryId);
			// to avoid an empty category, provide a very basic UI that only shows the name of a row
			return new JLabel(row.getDoc().get(GlobalSearchUtilities.FIELD_NAME));
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

		// add mouse listener for hovering and activation
		MouseListener activationMouseListener = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 2) {
					return;
				}

				GlobalSearchableGUIProvider.Veto providerVeto = new GlobalSearchableGUIProvider.Veto();
				provider.searchResultTriggered(row.getDoc(), providerVeto);
				// only fire interaction if provider did not veto it
				if (!providerVeto.isVeto()) {
					ActionStatisticsCollector.getInstance().logGlobalSearchAction(controller.getLastQuery(), categoryId, row.getDoc().getField(GlobalSearchUtilities.FIELD_NAME).stringValue());

					fireInteraction(InteractionEvent.RESULT_ACTIVATED, row);

					wrapperPanel.setOpaque(false);
					wrapperPanel.repaint();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				wrapperPanel.setOpaque(true);
				wrapperPanel.repaint();

				provider.searchResultBrowsed(row.getDoc());
				fireInteraction(InteractionEvent.RESULT_BROWSED, row);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				wrapperPanel.setOpaque(false);
				wrapperPanel.repaint();
			}
		};
		component.addMouseListener(activationMouseListener);
		component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		// add drag&drop support if applicable
		if (provider.isDragAndDropSupported(row.getDoc())) {
			DragGestureListener dragSupport = provider.getDragAndDropSupport(row.getDoc());
			if (dragSupport != null) {
				DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(component, DnDConstants.ACTION_MOVE, dragSupport);
			} else {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.search.globalsearchdialog.missing_drag_support", categoryId);
			}
		}


		return wrapperPanel;
	}

	/**
	 * Fires the given {@link InteractionEvent}.
	 *
	 * @param type
	 * 		the type of the event
	 * @param row
	 * 		the row with which the interaction happened
	 */
	private void fireInteraction(final InteractionEvent type, final GlobalSearchRow row) {
		// Notify the listeners
		for (GlobalSearchInteractionListener listener : eventListener.getListeners(GlobalSearchInteractionListener.class)) {
			GlobalSearchInteractionEvent e = new GlobalSearchInteractionEvent(type, row);
			listener.interaction(e);
		}
	}

}
