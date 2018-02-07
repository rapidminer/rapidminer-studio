/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.search.event.GlobalSearchCategoryEvent;
import com.rapidminer.gui.search.event.GlobalSearchEventListener;
import com.rapidminer.gui.search.event.GlobalSearchModelEvent;
import com.rapidminer.gui.search.model.GlobalSearchModel;
import com.rapidminer.gui.tools.FilterTextField;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.SelectionNavigationListener;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.TextFieldWithAction;
import com.rapidminer.gui.tools.components.DropDownPopupButton;
import com.rapidminer.gui.tools.components.composite.CompositeButton;
import com.rapidminer.search.GlobalSearchCategory;
import com.rapidminer.search.GlobalSearchRegistry;
import com.rapidminer.search.GlobalSearchResult;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * Panel that visualizes the Global Search feature and results.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public final class GlobalSearchPanel extends JPanel {

	private static GlobalSearchPanel instance;

	private static final Object SINGLETON_LOCK = new Object();

	private static final ImageIcon CLEAR_FILTER_HOVERED_ICON = SwingTools.createIcon("16/x-mark_orange.png");
	private static final ImageIcon DEFAULT_FILTER_ICON = SwingTools.createIcon("16/magnifying_glass.png");
	private static final ImageIcon LOADING_ICON = SwingTools.createIcon("16/loading.gif");

	private static final String CLOSE_FULL_SEARCH_DIALOG = "closeGlobalSearchDialog";

	/** max height in pixel the result dialog can be. If it needs to be larger, it will use a scrollpane */
	private static final int MAX_RESULT_DIALOG_HEIGHT = 820;
	private static final int SEARCH_FIELD_WIDTH = 205;
	private static final int FILTER_BUTTON_WIDTH = 90;
	private static final int SEARCH_BUTTON_WIDTH = 90;
	private static final int MAX_CATEGORY_I18N_LENGTH = 11;

	public static final int PREFERRED_WIDTH = SEARCH_FIELD_WIDTH + FILTER_BUTTON_WIDTH + SEARCH_BUTTON_WIDTH;


	private FilterTextField searchField;
	private TextFieldWithAction searchActionField;

	private DropDownPopupButton filterButton;
	private JButton searchButton;
	private JPopupMenu filterMenu;
	private JRadioButtonMenuItem allCategoriesButton;

	private GlobalSearchDialog resultDialog;
	private transient GlobalSearchModel model;
	private transient GlobalSearchController controller;

	private final ResourceAction clearFilterAction = new ResourceAction(true, "clear_filter") {

		@Override
		public void actionPerformed(final ActionEvent e) {
			searchField.clearFilter();
			searchField.requestFocusInWindow();
		}
	};

	/** if not {@code null}, determines in which category the Global Search searches for results. If {@code null}, all categories are searched */
	private String categoryFilter = null;

	/**
	 * Should only be created by {@link #getInstance()} ()}.
	 */
	private GlobalSearchPanel() {
		// result display
		model = new GlobalSearchModel();
		controller = new GlobalSearchController(this, model);
		model.registerEventListener(new GlobalSearchEventListener() {

			@Override
			public void modelChanged(GlobalSearchModelEvent e) {
				switch (e.getEventType()) {
					case ALL_CATEGORIES_REMOVED:
						hideComponents();
						break;
					case ERROR_STATUS_CHANGED:
						// if error was reset but there are still pending searches, don't open dialog
						if (model.hasError()) {
							showResults();
						}
						break;
					default:
						// do nothing
				}
			}

			@Override
			public void categoryChanged(String categoryId, GlobalSearchCategoryEvent e, GlobalSearchResult result) {
				switch (e.getEventType()) {
					case CATEGORY_PENDING_STATUS_CHANGED:
						setPending(model.isAnyCategoryPending());
						// there was an error and now a new search has been started. Close dialog that displayed error and wait for new results
						if (model.hasError()) {
							hideComponents();
						}
						break;
					case CATEGORY_ROWS_CHANGED:
					case CATEGORY_ROWS_APPENDED:
						// first search will fire for empty result as well for each category
						// only show dialog if results are there or no more pending categories exist
						if (model.hasCategoryResults(categoryId) || !model.isAnyCategoryPending()) {
							showResults();
						}
						break;
					default:
						// do nothing
				}
			}
		});

		initGUI();
	}

	@Override
	public boolean requestFocusInWindow() {
		if (!searchField.isFocusOwner()) {
			return searchField.requestFocusInWindow();
		} else {
			searchField.selectAll();
			return true;
		}
	}

	/**
	 * Sets the text of the Global Search field. Will trigger a new search.
	 *
	 * @param searchTerm
	 * 		the term to search. Can be {@code null} to reset the search
	 */
	public void setSearchTerm(String searchTerm) {
		searchField.setText(searchTerm);
	}

	/**
	 * Initializes the Global Search result visualization. Has no effect if it already is visualized.
	 */
	public void initializeSearchResultVisualization() {
		synchronized (SINGLETON_LOCK) {
			if (resultDialog == null) {
				resultDialog = new GlobalSearchDialog(ApplicationFrame.getApplicationFrame(), controller) {

					@Override
					public Dimension getPreferredSize() {
						// make sure width is same as this panel
						int width = searchActionField.getWidth() + filterButton.getWidth() + searchButton.getWidth();
						Dimension prefSize = super.getPreferredSize();
						prefSize.width = width;

						// make sure height does not exceed certain amount
						prefSize.height = Math.min(prefSize.height, MAX_RESULT_DIALOG_HEIGHT);

						return prefSize;
					}
				};
				resultDialog.setFocusableWindowState(false);

				// even though dialog cannot be focused, it should hide when RM Studio is minimized
				ApplicationFrame.getApplicationFrame().addWindowListener(new WindowAdapter() {

					@Override
					public void windowIconified(WindowEvent e) {
						GlobalSearchPanel.this.hideComponents();
					}
				});

				// also close everything if results are displayed
				InputMap inputMap = resultDialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
				ActionMap actionMap = resultDialog.getRootPane().getActionMap();

				inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CLOSE_FULL_SEARCH_DIALOG);
				actionMap.put(CLOSE_FULL_SEARCH_DIALOG, new ResourceAction("") {
					@Override
					public void actionPerformed(ActionEvent e) {
						GlobalSearchPanel.this.hideComponents();

						logGlobalSearchFocusLost();
					}
				});
			}
		}
	}

	/**
	 * Hides any components that belong to the Global Search, i.e. the result dialog and the filter popup.
	 */
	protected void hideComponents() {
		if (filterMenu != null) {
			filterMenu.setVisible(false);
		}
		if (resultDialog != null) {
			resultDialog.setVisible(false);
		}
	}

	/**
	 * Toggles the search "All Studio" category button as if the user clicked it.
	 */
	protected void searchAll() {
		allCategoriesButton.doClick();
	}

	/**
	 * Sets the panel to the pending state when any category is pending. Changes the icon of the search field to a loading icon.
	 *
	 * @param pending
	 * 		{@code true} if any search is pending; {@code false} if no search is pending
	 */
	private void setPending(final boolean pending) {
		if (pending) {
			searchActionField.setForceIcon(LOADING_ICON);
		} else {
			searchActionField.setForceIcon(null);
		}
	}

	/**
	 * Shows the result dialog popup.
	 */
	private void showResults() {
		// show result dialog immediately to visualize search is ongoing
		if (resultDialog != null) {
			resultDialog.setLocation(searchActionField.getLocationOnScreen().x, searchActionField.getLocationOnScreen().y + searchActionField.getHeight() - 1);
			resultDialog.setVisible(true);
		}
	}

	/**
	 * Populate the filter popup. This is needed because search categories may have been registered/removed in the meantime.
	 *
	 * @param menu
	 * 		the menu to populate, must not be {@code null}
	 */
	private void populateFilterPopup(final JPopupMenu menu) {
		menu.removeAll();
		ButtonGroup categoryGroup = new ButtonGroup();

		// create "All" categories button
		String allCategoriesI18N = I18N.getGUILabel("global_search.filter_all.label");
		allCategoriesButton = new JRadioButtonMenuItem(allCategoriesI18N);
		allCategoriesButton.setMnemonic(KeyEvent.VK_L);
		allCategoriesButton.addActionListener(e -> {

			// remember if user clicked already selected filter again, or if it was truly a change
			boolean changed = categoryFilter != null;

			categoryFilter = null;
			filterButton.setText(allCategoriesI18N);

			// category filter changed, clear all previous search categories
			if (changed) {
				model.clearAllCategories();
			}

			GlobalSearchPanel.this.requestFocusInWindow();
			controller.handleSearch(searchField.getText(), categoryFilter);
		});
		categoryGroup.add(allCategoriesButton);
		if (categoryFilter == null) {
			allCategoriesButton.setSelected(true);
		}

		menu.add(allCategoriesButton);
		menu.addSeparator();

		// create buttons for each registered search category
		List<GlobalSearchCategory> categories = GlobalSearchGUIUtilities.INSTANCE.sortCategories(GlobalSearchRegistry.INSTANCE.getAllSearchCategories());
		for (GlobalSearchCategory cat : categories) {
			String catID = cat.getCategoryId();
			GlobalSearchableGUIProvider guiProvider = GlobalSearchGUIRegistry.INSTANCE.getGUIProviderForSearchCategoryById(catID);
			if (guiProvider == null) {
				// skip categories with no GUI
				continue;
			}
			String i18nName = guiProvider.getI18nNameForSearchable();

			JRadioButtonMenuItem catButton = new JRadioButtonMenuItem(i18nName);
			catButton.setMnemonic(i18nName.charAt(0));
			catButton.addActionListener(e -> {

				// remember if user clicked already selected filter again, or if it was truly a change
				boolean changed = !catID.equals(categoryFilter);

				categoryFilter = catID;
				String text = i18nName;

				// cut off too long name
				if (text.length() > MAX_CATEGORY_I18N_LENGTH) {
					text = text.substring(0, MAX_CATEGORY_I18N_LENGTH - 1) + "..";
				}
				filterButton.setText(text);

				// category filter changed, clear all previous search categories
				if (changed) {
					model.clearAllCategories();
				}

				GlobalSearchPanel.this.requestFocusInWindow();
				controller.handleSearch(searchField.getText(), categoryFilter);
			});
			categoryGroup.add(catButton);

			if (catID.equals(categoryFilter)) {
				catButton.setSelected(true);
			}

			menu.add(catButton);
		}
	}

	/**
	 * Set up the UI.
	 */
	private void initGUI() {
		GridBagConstraints gbc = new GridBagConstraints();
		setLayout(new GridBagLayout());

		searchField = new FilterTextField(12);
		searchField.setDefaultFilterText(I18N.getMessage(I18N.getGUIBundle(), "gui.field.global_search.prompt"));
		// listen for value change events, trigger search with slight delay to not search while user types
		searchField.addFilterListener(value -> controller.handleSearch(searchField.getText(), categoryFilter));
		// listen for special events that can detect when user pressed Enter. Search straight away in that case w/o delay
		searchField.addSelectionNavigationListener(new SelectionNavigationListener() {
			@Override
			public void up() {
				// ignore
			}

			@Override
			public void down() {
				// ignore
			}

			@Override
			public void left() {
				// ignore
			}

			@Override
			public void right() {
				// ignore
			}

			@Override
			public void selected() {
				// user pressed Enter key, trigger search straight away
				controller.handleSearch(searchField.getText(), categoryFilter);
			}
		});
		// clicking away from the search field should close the search popup
		// this is the next best thing because we cannot detect focus loss on dialog as dialog cannot be focused anymore
		searchField.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				// not needed
			}

			@Override
			public void focusLost(FocusEvent e) {
				// only hide if the opposite component is in Studio (aka not null)
				if (e.getOppositeComponent() != null) {
					hideComponents();

					logGlobalSearchFocusLost();
				}
			}
		});

		searchActionField = new TextFieldWithAction(searchField, clearFilterAction, CLEAR_FILTER_HOVERED_ICON) {

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(SEARCH_FIELD_WIDTH, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT);
			}
		};
		searchActionField.setDefaultIcon(DEFAULT_FILTER_ICON);
		searchActionField.putClientProperty(RapidLookTools.PROPERTY_INPUT_DARK_BORDER, Boolean.TRUE);
		searchActionField.putClientProperty(RapidLookTools.PROPERTY_INPUT_TYPE_COMPOSITE, SwingConstants.LEFT);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0d;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(searchActionField, gbc);

		gbc.gridx += 1;
		gbc.weightx = 0.0d;
		gbc.fill = GridBagConstraints.NONE;
		DropDownPopupButton.DropDownPopupButtonBuilder builder = new DropDownPopupButton.DropDownPopupButtonBuilder();
		filterButton = builder.with(new ResourceActionAdapter("global_search.toolbar")).setComposite(SwingUtilities.CENTER).build();
		filterButton.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				Object source = e.getSource();
				populateFilterPopup((JPopupMenu) source);

				filterMenu = (JPopupMenu) source;
				filterMenu.setPreferredSize(new Dimension(filterButton.getWidth() + searchButton.getWidth(), filterMenu.getPreferredSize().height));
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				filterMenu = null;
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				// not needed
			}
		});
		filterButton.putClientProperty(RapidLookTools.PROPERTY_BUTTON_DARK_BORDER, Boolean.TRUE);
		filterButton.setText(I18N.getGUILabel("global_search.filter_all.label"));
		filterButton.setMnemonic(KeyEvent.VK_L);
		filterButton.setPreferredSize(new Dimension(FILTER_BUTTON_WIDTH, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
		add(filterButton, gbc);

		gbc.gridx += 1;
		searchButton = new CompositeButton(I18N.getGUILabel("global_search.run_search.label"), SwingConstants.RIGHT);
		searchButton.setToolTipText(I18N.getGUILabel("global_search.run_search.tip"));
		searchButton.addActionListener(e -> {

			controller.handleSearch(searchField.getText(), categoryFilter);
			// give focus back to search field
			requestFocusInWindow();
		});
		searchButton.setPreferredSize(new Dimension(SEARCH_BUTTON_WIDTH, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
		searchButton.putClientProperty(RapidLookTools.PROPERTY_BUTTON_HIGHLIGHT, Boolean.TRUE);
		searchButton.putClientProperty(RapidLookTools.PROPERTY_BUTTON_DARK_BORDER, Boolean.TRUE);
		searchButton.setForeground(Colors.WHITE);
		add(searchButton, gbc);

		InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap actionMap = getActionMap();

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CLOSE_FULL_SEARCH_DIALOG);
		actionMap.put(CLOSE_FULL_SEARCH_DIALOG, new ResourceAction("") {
			@Override
			public void actionPerformed(ActionEvent e) {
				GlobalSearchPanel.this.hideComponents();

				logGlobalSearchFocusLost();
			}
		});
	}

	/**
	 * Log the values in case the global search focus was lost intentionally. Make sure this is only called if the user
	 * cancels it.
	 */
	private void logGlobalSearchFocusLost() {
		String lastQuery = controller.getLastQuery();
		if (lastQuery == null) {
			return;
		}
		String categoryId = controller.getLastCategoryFilter();
		long amount = 0;
		if (categoryId == null) {
			categoryId = ActionStatisticsCollector.ARG_GLOBAL_SEARCH_CATEGORY_ALL;
			for (String category : controller.getModel().getActiveCategories()) {
				amount += controller.getModel().getRowsForCategory(category).size();
			}
		} else {
			amount = controller.getModel().getRowsForCategory(categoryId).size();
		}
		ActionStatisticsCollector.getInstance().logGlobalSearch(ActionStatisticsCollector.VALUE_FOCUS_LOST, lastQuery, categoryId, amount);
	}

	/**
	 * Returns the global instance of the search panel.
	 *
	 * @return the instance, never {@code null}
	 */
	public static GlobalSearchPanel getInstance() {
		synchronized (SINGLETON_LOCK) {
			if (instance == null) {
				instance = new GlobalSearchPanel();
			}

			return instance;
		}
	}
}
