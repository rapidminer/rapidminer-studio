/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.search;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.search.event.GlobalSearchCategoryEvent;
import com.rapidminer.gui.search.event.GlobalSearchEventListener;
import com.rapidminer.gui.search.event.GlobalSearchInteractionListener;
import com.rapidminer.gui.search.event.GlobalSearchModelEvent;
import com.rapidminer.gui.search.model.GlobalSearchModel;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.ExtendedCardLayout;
import com.rapidminer.gui.tools.components.LinkLocalButton;
import com.rapidminer.search.GlobalSearchCategory;
import com.rapidminer.search.GlobalSearchRegistry;
import com.rapidminer.search.GlobalSearchResult;
import com.rapidminer.search.event.GlobalSearchRegistryEvent;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * Dialog that visualizes the Global Search results.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public class GlobalSearchDialog extends JDialog {

	private static final ImageIcon INFORMATION_ICON = SwingTools.createIcon("24/information.png");

	private static final Color ERROR_TEXT_COLOR = new Color(255, 63, 58);
	private static final Color TOP_BORDER_COLOR = Colors.BUTTON_BORDER_DISABLED;
	private static final Color SIDE_BORDER_COLOR = Colors.PANEL_BORDER;
	private static final Border TOP_BORDER = BorderFactory.createMatteBorder(1, 0, 0, 0, TOP_BORDER_COLOR);
	private static final Border SIDE_BORDER = BorderFactory.createMatteBorder(0, 1, 1, 1, SIDE_BORDER_COLOR);
	private static final Border MAIN_EMPTY_BORDER = BorderFactory.createEmptyBorder(0, 0, 0, 2);
	private static final String NO_ERROR_MESSAGE = " ";
	private static final int FILLER_GRID_Y = 10_000;
	private static final float FONT_SIZE_NO_RESULTS = 15f;

	private static final Object CATEGORY_COMPONENT_LOCK = new Object();

	private static final String CARD_ERROR = "card_error";
	private static final String CARD_NO_RESULTS_GLOBALLY = "card_no_results_all";
	private static final String CARD_NO_RESULTS_IN_CATEGORY = "card_no_results_category";
	private static final String CARD_RESULTS = "card_results";


	private final transient GlobalSearchModel model;
	private final transient GlobalSearchController controller;

	private JPanel rootPanel;
	private CardLayout layout;

	private JTextArea errorText;
	private JPanel mainPanel;

	private GridBagConstraints mainPanelGbc;

	private Map<String, GlobalSearchCategoryPanel> categoryComponentMap;
	private transient GlobalSearchInteractionListener interactionListener;


	/**
	 * Creates a new dialog displaying the contents of the given {@link GlobalSearchModel}.
	 *
	 * @param owner
	 * 		the frame owner for the dialog
	 * @param controller
	 * 		the controller instance from which the model will be used
	 */
	public GlobalSearchDialog(final Window owner, final GlobalSearchController controller) {
		super(owner, I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.global_search.title"), ModalityType.MODELESS);

		this.controller = controller;
		this.model = this.controller.getModel();
		this.categoryComponentMap = new LinkedHashMap<>();

		// add listener for model changes
		model.registerEventListener(new GlobalSearchEventListener() {

			@Override
			public void modelChanged(final GlobalSearchModelEvent e) {
				SwingTools.invokeLater(() -> {

					handleModelChanged(e);

					pack();
				});
			}

			@Override
			public void categoryChanged(final String categoryId, final GlobalSearchCategoryEvent e, final GlobalSearchResult result) {
				SwingTools.invokeLater(() -> {

					if (handleCategoryChanged(categoryId, e, result)) {
						return;
					}

					pack();
				});
			}
		});

		// update UI based on registration/un-registration events
		GlobalSearchRegistry.INSTANCE.addEventListener((e, category) -> {
			if (e.getEventType() == GlobalSearchRegistryEvent.RegistrationEvent.SEARCH_CATEGORY_REGISTERED) {
				addGUIForCategory(category);
			} else if (e.getEventType() == GlobalSearchRegistryEvent.RegistrationEvent.SEARCH_CATEGORY_UNREGISTERED) {
				removeGUIForCategory(category);
			}
		});

		// hide this dialog if user activated search result
		interactionListener = e -> {
			switch (e.getEventType()) {
				case RESULT_BROWSED:
					// ignore
					break;
				case RESULT_ACTIVATED:
					GlobalSearchDialog.this.setVisible(false);
					break;
				default:
					// do nothing
			}
		};

		initGUI();
	}

	/**
	 * Set up the GUI.
	 */
	private void initGUI() {
		setLocationRelativeTo(getOwner());
		setUndecorated(true);

		layout = new ExtendedCardLayout();
		rootPanel = new JPanel();
		rootPanel.setBackground(Colors.WINDOW_BACKGROUND);
		rootPanel.setLayout(layout);
		getRootPane().setBorder(SIDE_BORDER);

		setupErrorGUI();
		setupNoResultGloballyGUI();
		setupNoResultCategoryGUI();
		setupResultsGUI();

		setLayout(new BorderLayout());
		JScrollPane scrollPane = new ExtendedJScrollPane(rootPanel);
		// no border and never a horizontal scrollbar. This will cut off a tiny portion of the UI but fuck horizontal scrollbars
		scrollPane.setBorder(null);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		// if scrolled down, we need a border at top, otherwise there is no border between search panel and dialog
		scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
			if (e.getValueIsAdjusting()) {
				return;
			}

			if (scrollPane.getVerticalScrollBar().getValue() > scrollPane.getVerticalScrollBar().getMinimum()) {
				scrollPane.setBorder(TOP_BORDER);
			} else {
				scrollPane.setBorder(null);
			}
		});
		add(scrollPane, BorderLayout.CENTER);
	}

	/**
	 * Handles a model changed event for the Global Search model.
	 *
	 * @param e
	 * 		the event
	 */
	private void handleModelChanged(final GlobalSearchModelEvent e) {
		switch (e.getEventType()) {
			case ERROR_STATUS_CHANGED:
				String error = model.getError();
				if (error != null) {
					errorText.setText(error);
					layout.show(rootPanel, CARD_ERROR);
				} else {
					errorText.setText(NO_ERROR_MESSAGE);
					layout.show(rootPanel, CARD_RESULTS);
				}
				break;
			case ALL_CATEGORIES_REMOVED:
				synchronized (CATEGORY_COMPONENT_LOCK) {
					// make all components invisible
					for (Map.Entry<String, GlobalSearchCategoryPanel> entry : categoryComponentMap.entrySet()) {
						if (entry.getValue() != null) {
							entry.getValue().setVisible(false);
						}
					}

				}
				break;
			default:
				// do nothing
		}
	}

	/**
	 * Handles a model changed event for the Global Search model.
	 *
	 * @param categoryId
	 * 		the category for which the event was fired
	 * @param e
	 * 		the event
	 * @param result
	 * 		the search result
	 * @return {@code true} if caller should return after this call; {@code false} otherwise
	 */
	private boolean handleCategoryChanged(final String categoryId, final GlobalSearchCategoryEvent e, final GlobalSearchResult result) {
		GlobalSearchCategoryPanel gsPanel;
		// avoid ConcurrentModificationException if a category is registered while we iterate
		synchronized (CATEGORY_COMPONENT_LOCK) {
			gsPanel = categoryComponentMap.get(categoryId);
		}

		if (gsPanel == null) {
			// try to build UI for next search, it should be registered by now
			boolean success = addGUIForCategory(GlobalSearchRegistry.INSTANCE.getSearchCategoryById(categoryId));
			if (success) {
				// we built the UI now, so handle this event again
				return handleCategoryChanged(categoryId, e, result);
			} else {
				// display no results if we still cannot create UI
				if (model.getActiveCategories().isEmpty() && !model.isAnyCategoryPending()) {
					displayNoResults();
				}
				return false;
			}
		}

		switch (e.getEventType()) {
			case CATEGORY_PENDING_STATUS_CHANGED:
				// if something is set to pending, we don't care here. If no longer pending, the rest of the method is relevant though
				if (model.isPending(categoryId)) {
					return true;
				}
				break;
			case CATEGORY_ROWS_CHANGED:
				gsPanel.setSearchRows(model.getRowsForCategory(categoryId), result, false);
				break;
			case CATEGORY_ROWS_APPENDED:
				gsPanel.setSearchRows(model.getRowsForCategory(categoryId), result, true);
				break;
			default:
				// do nothing
		}

		if (model.getActiveCategories().isEmpty() && !model.isAnyCategoryPending()) {
			// if no search results whatsoever have been found, display special "no results" page
			displayNoResults();
		} else {
			// either show or hide the category panels depending on whether they have results or not
			gsPanel.setVisible(gsPanel.hasResults());
			layout.show(rootPanel, CARD_RESULTS);
		}
		return false;
	}

	/**
	 * Sets up the GUI card responsible for displaying the actual search results to the user.
	 */
	private void setupResultsGUI() {
		mainPanel = new JPanel();
		mainPanel.setName(CARD_RESULTS);
		mainPanel.setLayout(new GridBagLayout());
		mainPanel.setBorder(MAIN_EMPTY_BORDER);

		mainPanelGbc = new GridBagConstraints();
		mainPanelGbc.gridx = 0;
		mainPanelGbc.gridy = 0;
		mainPanelGbc.weightx = 1.0d;
		mainPanelGbc.fill = GridBagConstraints.HORIZONTAL;
		mainPanelGbc.anchor = GridBagConstraints.NORTH;

		// add category panels now
		List<GlobalSearchCategory> categories = GlobalSearchGUIUtilities.INSTANCE.sortCategories(GlobalSearchRegistry.INSTANCE.getAllSearchCategories());
		for (GlobalSearchCategory category : categories) {
			addGUIForCategory(category);
		}

		// filler at bottom so they are sitting nicely at the top
		int previousGridY = mainPanelGbc.gridy;
		mainPanelGbc.gridy = FILLER_GRID_Y;
		mainPanelGbc.weighty = 1.0d;
		mainPanelGbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(new JLabel(), mainPanelGbc);

		// reset layout so later additions are above the filler
		mainPanelGbc.gridy = previousGridY;
		mainPanelGbc.weighty = 0.0d;
		mainPanelGbc.fill = GridBagConstraints.NONE;

		rootPanel.add(mainPanel, CARD_RESULTS);
	}

	/**
	 * Sets up the GUI card responsible for displaying no result information on a specific category search to the user.
	 */
	private void setupNoResultCategoryGUI() {
		JPanel noResultPanel = new JPanel();
		noResultPanel.setName(CARD_NO_RESULTS_IN_CATEGORY);
		noResultPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		noResultPanel.setOpaque(false);
		noResultPanel.setBackground(Colors.TEXT_HIGHLIGHT_BACKGROUND);
		noResultPanel.setBorder(TOP_BORDER);

		// no results label
		JLabel noResults = new JLabel(I18N.getGUIMessage("gui.dialog.global_search.no_results.label"));
		noResults.setIcon(INFORMATION_ICON);
		noResults.setFont(noResults.getFont().deriveFont(FONT_SIZE_NO_RESULTS));
		noResults.setHorizontalAlignment(SwingConstants.LEFT);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.0d;
		gbc.weighty = 1.0d;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(3, 10, 3, 0);
		noResultPanel.add(noResults, gbc);

		// filler in middle
		gbc.gridx += 1;
		gbc.weightx = 1.0d;
		gbc.weighty = 1.0d;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 0, 0, 0);
		noResultPanel.add(new JLabel(), gbc);

		// "Try searching everywhere" button
		final ResourceAction action = new ResourceAction("global_search.search_all_instead") {

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				controller.searchAllCategories();
			}
		};
		LinkLocalButton searchAllButton = new LinkLocalButton(action);
		GlobalSearchResultPanel wrapperPanel = new GlobalSearchResultPanel(null);
		wrapperPanel.add(searchAllButton);
		wrapperPanel.setActivationAction(action);

		gbc.gridx += 1;
		gbc.gridy = 0;
		gbc.weightx = 1.0d;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.insets = new Insets(3, 0, 3, 5);
		gbc.anchor = GridBagConstraints.EAST;
		noResultPanel.add(wrapperPanel, gbc);

		rootPanel.add(noResultPanel, CARD_NO_RESULTS_IN_CATEGORY);
	}

	/**
	 * Sets up the GUI card responsible for displaying no result information on an "All Studio" search to the user.
	 */
	private void setupNoResultGloballyGUI() {
		JPanel noResultPanel = new JPanel();
		noResultPanel.setName(CARD_NO_RESULTS_GLOBALLY);
		noResultPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		noResultPanel.setOpaque(false);
		noResultPanel.setBorder(TOP_BORDER);

		// no results label
		JLabel noResults = new JLabel(I18N.getGUIMessage("gui.dialog.global_search.no_results.label"));
		noResults.setIcon(INFORMATION_ICON);
		noResults.setFont(noResults.getFont().deriveFont(FONT_SIZE_NO_RESULTS));
		noResults.setHorizontalAlignment(SwingConstants.LEFT);
		gbc.weightx = 1.0d;
		gbc.weighty = 1.0d;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 10, 3, 10);
		noResultPanel.add(noResults, gbc);

		rootPanel.add(noResultPanel, CARD_NO_RESULTS_GLOBALLY);
	}

	/**
	 * Sets up the GUI card responsible for displaying error information to the user.
	 */
	private void setupErrorGUI() {
		JPanel errorPanel = new JPanel();
		errorPanel.setName(CARD_ERROR);
		errorPanel.setLayout(new GridBagLayout());
		errorPanel.setOpaque(false);
		errorPanel.setBorder(TOP_BORDER);

		errorText = new JTextArea();
		errorText.setLineWrap(true);
		errorText.setWrapStyleWord(true);
		errorText.setForeground(ERROR_TEXT_COLOR);
		errorText.setBackground(Colors.WINDOW_BACKGROUND);
		errorText.setEditable(false);
		errorText.setBorder(null);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1.0d;
		gbc.weighty = 1.0d;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(5, 5, 5, 5);
		errorPanel.add(errorText, gbc);

		rootPanel.add(errorPanel, CARD_ERROR);
	}

	/**
	 * Display the appropriate "No Results" card.
	 */
	private void displayNoResults() {
		if (controller.getLastCategoryFilter() != null) {
			layout.show(rootPanel, CARD_NO_RESULTS_IN_CATEGORY);
		} else {
			layout.show(rootPanel, CARD_NO_RESULTS_GLOBALLY);
		}
	}

	/**
	 * Creates the UI for the given category. Call when a category has been registered to the {@link GlobalSearchRegistry}.
	 *
	 * @param category
	 * 		the category for which the UI should be added
	 * @return {@code true} if the UI was successfully added; {@code false} otherwise
	 */
	private boolean addGUIForCategory(final GlobalSearchCategory category) {
		// Check if category is visible
		if (!category.isVisible()) {
			return false;
		}
		String categoryId = category.getCategoryId();
		try {
			GlobalSearchCategoryPanel catPanel = new GlobalSearchCategoryPanel(category, controller);
			// close dialog if user clicked on a search result
			catPanel.registerEventListener(interactionListener);
			catPanel.setVisible(false);

			mainPanelGbc.gridy += 1;
			mainPanel.add(catPanel, mainPanelGbc);
			synchronized (CATEGORY_COMPONENT_LOCK) {
				categoryComponentMap.put(categoryId, catPanel);
			}
			return true;
		} catch (IllegalArgumentException e) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.search.globalsearchdialog.no_gui_provider", categoryId);
			return false;
		}
	}

	/**
	 * Removes the UI for the given category. Call when a category has been unregistered from the {@link GlobalSearchRegistry}.
	 *
	 * @param category
	 * 		the category for which the UI should be removed
	 */
	private void removeGUIForCategory(final GlobalSearchCategory category) {
		synchronized (CATEGORY_COMPONENT_LOCK) {
			GlobalSearchCategoryPanel removed = categoryComponentMap.remove(category.getCategoryId());
			if (removed != null) {
				removed.removeEventListener(interactionListener);
				mainPanel.remove(removed);
			}
		}
	}

	/**
	 * remember the selected category
	 */
	private String selectedSearchCategory;

	/**
	 * Change the selection to this entry
	 *
	 * @param category
	 * 		name of the category that should be selected
	 * @param resultIndex
	 * 		index of the entry that should be selected
	 * @since 9.0.0
	 */
	public void select(String category, int resultIndex) {
		resetSelection();
		if(!isVisible()) {
			return;
		}
		if (category == null) {
			return;
		}
		final GlobalSearchCategoryPanel globalSearchCategoryPanel = categoryComponentMap.get(category);
		if (globalSearchCategoryPanel != null) {
			selectedSearchCategory = category;
			globalSearchCategoryPanel.setSelectedEntry(resultIndex);
		}
	}

	/**
	 * Reset the selection to select no entry anymore.
	 *
	 * @since 9.0.0
	 */
	void resetSelection() {
		final Component displayedComponent = SwingTools.findDisplayedComponent(rootPanel);
		if (displayedComponent != null && CARD_NO_RESULTS_IN_CATEGORY.equals(displayedComponent.getName())) {
			highlight(displayedComponent, false);
		}
		if (selectedSearchCategory == null) {
			return;
		}
		final GlobalSearchCategoryPanel globalSearchCategoryPanel = categoryComponentMap.get(selectedSearchCategory);
		if (globalSearchCategoryPanel != null) {
			globalSearchCategoryPanel.resetSelectedEntry();
		}
		selectedSearchCategory = null;
	}

	/**
	 * Select the next entry in the results list regardless of the category, go to the next category if there are no further entries in this category
	 *
	 * @since 9.0.0
	 */
	public void selectNext() {
		selectNextOrPrevious(true);
	}

	/**
	 * Select the previous entry in the results list regardless of the category. Go to the previous category if there is no previous entry in this category.
	 *
	 * @since 9.0.0
	 */
	public void selectPrevious() {
		selectNextOrPrevious(false);
	}

	/**
	 * Change the selected entry step by step, true = forward, false = backwards
	 *
	 * @param next
	 * 		should the selection move down the list
	 * @since 9.0.0
	 */
	private void selectNextOrPrevious(boolean next) {
		if(!isVisible()) {
			return;
		}
		final Component displayedComponent = SwingTools.findDisplayedComponent(rootPanel);
		if (displayedComponent != null && CARD_NO_RESULTS_IN_CATEGORY.equals(displayedComponent.getName())) {
			highlight(displayedComponent, false);
		}
		if (mainPanel == null) {
			return;
		}

		GlobalSearchCategoryPanel globalSearchCategoryPanel = categoryComponentMap.get(selectedSearchCategory);
		if (globalSearchCategoryPanel != null && !globalSearchCategoryPanel.isVisible()) {
			resetSelection();
			globalSearchCategoryPanel = null;
		}
		if (globalSearchCategoryPanel == null) {
			selectFirstOrLastEntry(next);
		} else {
			boolean movedSelectionSuccessfully = next ? globalSearchCategoryPanel.selectNext() : globalSearchCategoryPanel.selectPrevious();
			if (!movedSelectionSuccessfully) {
				// move to the next category
				resetSelection();
				int categoryIndex = globalSearchCategoryPanel.getAccessibleContext().getAccessibleIndexInParent();
				int nextCategoryIndex = next ? categoryIndex + 1 : categoryIndex - 1;

				final Container parent = globalSearchCategoryPanel.getParent();
				final int componentCount = parent.getComponentCount();
				while (nextCategoryIndex >= 0 && componentCount > nextCategoryIndex
						&& parent.getComponent(nextCategoryIndex) instanceof GlobalSearchCategoryPanel
						&& (((GlobalSearchCategoryPanel) parent.getComponent(nextCategoryIndex)).getComponentCount() == 0
						|| !parent.getComponent(nextCategoryIndex).isVisible())) {
					nextCategoryIndex = next ? nextCategoryIndex + 1 : nextCategoryIndex - 1;
				}
				if (nextCategoryIndex >= 0 && componentCount > nextCategoryIndex
						&& parent.getComponent(nextCategoryIndex) instanceof GlobalSearchCategoryPanel) {
					final GlobalSearchCategoryPanel nextGlobalSearchCategoryPanel = (GlobalSearchCategoryPanel) parent.getComponent(nextCategoryIndex);
					selectedSearchCategory = nextGlobalSearchCategoryPanel.getCategoryId();
					nextGlobalSearchCategoryPanel.resetSelectedEntry();
					if (next) {
						nextGlobalSearchCategoryPanel.selectFirst();
					} else {
						nextGlobalSearchCategoryPanel.selectLast();
					}
				} else {
					selectFirstOrLastEntry(next);
				}
			}
		}
		if (selectedSearchCategory == null) {
			GlobalSearchPanel.getInstance().putCursorIntoSearchfield();
		} else {
			scrollToSelectedEntry();
		}
	}

	/**
	 * If there are too many entries in the results and the selection is changed the scrollpane moves to the selected entry with this method.
	 *
	 * @since 9.0.0
	 */
	private void scrollToSelectedEntry() {
		final GlobalSearchCategoryPanel globalSearchCategoryPanel = categoryComponentMap.get(selectedSearchCategory);
		if (globalSearchCategoryPanel != null) {
			final Rectangle categoryBounds = globalSearchCategoryPanel.getBounds();
			final int selectedEntryIndex = globalSearchCategoryPanel.getSelectedEntryIndex();
			if (selectedEntryIndex >= 0) {
				final Rectangle entryBounds = globalSearchCategoryPanel.getContentPanel().getComponent(selectedEntryIndex).getBounds();
				final Rectangle aRect = new Rectangle(categoryBounds.x, categoryBounds.y + entryBounds.y, categoryBounds.width, entryBounds.height);
				SwingUtilities.invokeLater(() -> rootPanel.scrollRectToVisible(aRect));
			}
		}
	}

	/**
	 * Select the first or last entry of the result list.
	 *
	 * @param first
	 * 		true = select first entry, false = select last entry
	 * @since 9.0.0
	 */
	void selectFirstOrLastEntry(boolean first) {
		if(!isVisible()) {
			return;
		}
		final Component displayedComponent = SwingTools.findDisplayedComponent(rootPanel);
		if (displayedComponent != null && CARD_NO_RESULTS_IN_CATEGORY.equals(displayedComponent.getName())) {
			highlight(displayedComponent, true);
			return;
		}

		resetSelection();
		if (mainPanel != null && mainPanel.getComponentCount() > 0) {
			Component[] components = mainPanel.getComponents();
			if (!first) {
				final List<Component> list = Arrays.asList(components);
				Collections.reverse(list);
				components = list.toArray(new Component[components.length]);
			}

			for (Component component : components) {
				if (component instanceof GlobalSearchCategoryPanel && component.isVisible()
						&& ((GlobalSearchCategoryPanel) component).getContentPanel().getComponentCount() > 0) {
					final GlobalSearchCategoryPanel searchCategoryPanel = (GlobalSearchCategoryPanel) component;
					selectedSearchCategory = searchCategoryPanel.getCategoryId();
					if (first) {
						searchCategoryPanel.resetSelectedEntry();
						searchCategoryPanel.selectNext();
					} else {
						searchCategoryPanel.selectLast();
					}
					break;
				}
			}
		}
		if (selectedSearchCategory == null) {
			GlobalSearchPanel.getInstance().putCursorIntoSearchfield();
		} else {
			scrollToSelectedEntry();
		}
	}

	/**
	 * Highlight a {@link GlobalSearchResultPanel}
	 *
	 * @param displayedComponent
	 * 		parent of the {@link GlobalSearchResultPanel}
	 * @param b
	 * 		if true, do highlight. If false reset highlight.
	 * @since 9.0.0
	 */
	private void highlight(Component displayedComponent, boolean b) {
		if (displayedComponent != null) {
			displayedComponent.setBackground(Colors.TEXT_HIGHLIGHT_BACKGROUND);
			if (displayedComponent instanceof Container) {
				for (Component component : ((Container) displayedComponent).getComponents()) {
					if (component instanceof GlobalSearchResultPanel) {
						((JComponent) component).setOpaque(b);
					}
				}
				displayedComponent.repaint();
			}
		}
	}

	/**
	 * Activate the action associated with the selected entry of the results.
	 *
	 * @since 9.0.0
	 */
	public void activateSelected() {
		final Component displayedComponent = SwingTools.findDisplayedComponent(rootPanel);
		if (displayedComponent == null) {
			return;
		}
		if (CARD_RESULTS.equals(displayedComponent.getName())) {
			final GlobalSearchCategoryPanel globalSearchCategoryPanel = categoryComponentMap.get(selectedSearchCategory);
			if (globalSearchCategoryPanel != null) {
				globalSearchCategoryPanel.activateSelectedEntry();
			}
		} else if (CARD_NO_RESULTS_IN_CATEGORY.equals(displayedComponent.getName())) {
			Container displayedContainer = (Container) displayedComponent;
			if (displayedContainer.getComponentCount() > 1 && displayedContainer.getComponent(2) instanceof GlobalSearchResultPanel) {
				((GlobalSearchResultPanel) displayedContainer.getComponent(2)).doActivate();
			}
		}
	}

	/**
	 * Reset the selection for a new list of results being shown.
	 *
	 * @since 9.0.0
	 */
	@Override
	public void setVisible(boolean b) {
		boolean resetSelectionLater = !isVisible();
		super.setVisible(b);
		if (resetSelectionLater) {
			resetSelection();
		}
	}
}
