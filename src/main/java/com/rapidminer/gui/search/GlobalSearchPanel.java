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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
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
import com.rapidminer.gui.RapidMinerGUI;
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

	/**
	 * Traversal policy for the global search items: either searchField or filterMenu get the focus.
	 * The search results are only accessible via arrow_down from the searchField.
	 *
	 * @author Andreas Timm
	 * @since 9.0.0
	 */
	private class GSPanelFocusTraversalPolicy extends FocusTraversalPolicy {

		@Override
		public Component getComponentAfter(Container aContainer, Component aComponent) {
			if (aComponent == searchField || aComponent == resultDialogFocusHolder) {
				return showFilterMenu();
			} else {
				hideComponents();
				return RapidMinerGUI.getMainFrame();
			}
		}

		@Override
		public Component getComponentBefore(Container aContainer, Component aComponent) {
			if (aComponent == filterButton) {
				hideFiltermenu();
				return searchField;
			} else {
				hideComponents();
				return RapidMinerGUI.getMainFrame();
			}
		}

		@Override
		public Component getFirstComponent(Container aContainer) {
			return searchField;
		}

		@Override
		public Component getLastComponent(Container aContainer) {
			return showFilterMenu();
		}

		@Override
		public Component getDefaultComponent(Container aContainer) {
			return searchField;
		}
	}

	/**
	 * KeyListener for the results dialog. Activate with Enter and navigate with key up / key down. Typing goes back into the searchField.
	 *
	 * @author Andreas Timm
	 * @since 9.0.0
	 */
	private class GSDialogKeyListener extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_DOWN:
					resultDialog.selectNext();
					e.consume();
					break;
				case KeyEvent.VK_UP:
					resultDialog.selectPrevious();
					e.consume();
					break;
				case KeyEvent.VK_ENTER:
					resultDialog.activateSelected();
					e.consume();
					break;
				case KeyEvent.VK_TAB:
					if (e.isShiftDown()) {
						searchField.requestFocusInWindow();
					} else {
						showFilterMenu();
					}
					e.consume();
					break;
				default:
					if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
						putCursorIntoSearchfield();
						final KeyEvent newKeyEvent = new KeyEvent(searchField, e.getID(), e.getWhen(), e.getModifiers(), e.getKeyCode(), e.getKeyChar());
						searchField.dispatchEvent(newKeyEvent);
						e.consume();
						break;
					}

					// remove non-printable characters from Unicode
					String input = ("" + e.getKeyChar()).replaceAll("\\p{C}", "");

					if (input != null && !"".equals(input) && Character.isDefined(input.charAt(0))) {
						e.consume();
						final String text = searchField.getText() + input;
						SwingUtilities.invokeLater(() -> {
							searchField.setText(text);
							putCursorIntoSearchfield();
						});
					}
					break;
			}
		}
	}

	private static GlobalSearchPanel instance;

	private static final Object SINGLETON_LOCK = new Object();

	private static final ImageIcon CLEAR_FILTER_HOVERED_ICON = SwingTools.createIcon("16/x-mark_orange.png");
	private static final ImageIcon DEFAULT_FILTER_ICON = SwingTools.createIcon("16/magnifying_glass.png");
	private static final ImageIcon LOADING_ICON = SwingTools.createIcon("16/loading.gif");

	private static final String CLOSE_FULL_SEARCH_DIALOG = "closeGlobalSearchDialog";
	private static final String MOUSE_FOCUS_TRIGGER = "mouseFocus";

	/**
	 * max height in pixel the result dialog can be. If it needs to be larger, it will use a scrollpane
	 */
	private static final int MAX_RESULT_DIALOG_HEIGHT = 820;
	private static final int SEARCH_FIELD_WIDTH = 205;
	private static final int FILTER_BUTTON_WIDTH = 90;
	private static final int MAX_CATEGORY_I18N_LENGTH = 11;

	public static final int PREFERRED_WIDTH = SEARCH_FIELD_WIDTH + FILTER_BUTTON_WIDTH;

	private FilterTextField searchField;
	private TextFieldWithAction searchActionField;

	private DropDownPopupButton filterButton;
	private JPopupMenu filterMenu;
	private JRadioButtonMenuItem allCategoriesButton;

	private GlobalSearchDialog resultDialog;
	private JComponent resultDialogFocusHolder;

	private transient GlobalSearchModel model;
	private transient GlobalSearchController controller;

	private final ResourceAction clearFilterAction = new ResourceAction(true, "clear_filter") {

		@Override
		public void actionPerformed(final ActionEvent e) {
			searchField.clearFilter();
			searchField.requestFocusInWindow();
		}
	};

	/**
	 * if not {@code null}, determines in which category the Global Search searches for results. If {@code null}, all categories are searched
	 */
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
			reopenResults();
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
						// make sure width is same as this panel + width of the i18n label
						int width = PREFERRED_WIDTH + GlobalSearchCategoryPanel.I18N_NAME_WIDTH + 1;
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
				addCloseFullSearchDialogAction(actionMap);
			}
		}
	}

	/**
	 * Adds an Action to the actionMap of the necessary steps to be executed for closing the global search results dialog
	 *
	 * @param actionMap
	 * 		the ActionMap that should know how to close the global search results dialog
	 */
	private void addCloseFullSearchDialogAction(ActionMap actionMap) {
		actionMap.put(CLOSE_FULL_SEARCH_DIALOG, new ResourceAction("") {
			@Override
			public void actionPerformed(ActionEvent e) {
				searchField.requestFocusInWindow();
				SwingUtilities.invokeLater(() -> {
					GlobalSearchPanel.this.hideComponents();
					logGlobalSearchFocusLost();
				});
			}
		});
	}

	/**
	 * Hides any components that belong to the Global Search, i.e. the result dialog and the filter popup.
	 */
	protected void hideComponents() {
		hideFiltermenu();
		hideResultdialog();
	}

	/**
	 * Hide the results dialog.
	 */
	private void hideResultdialog() {
		if (resultDialog != null) {
			resultDialog.setVisible(false);
		}
	}

	/**
	 * Hide the filter menu.
	 */
	private void hideFiltermenu() {
		if (filterMenu != null) {
			filterMenu.setVisible(false);
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
			// align dialog to the right
			resultDialog.setLocation(searchActionField.getLocationOnScreen().x - GlobalSearchCategoryPanel.I18N_NAME_WIDTH - 1,
					searchActionField.getLocationOnScreen().y + searchActionField.getHeight() - 1);
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
		for (KeyListener keyListener : menu.getKeyListeners()) {
			menu.removeKeyListener(keyListener);
		}
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
			if (filterMenu != null) {
				filterMenu.setVisible(false);
			}
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
			final KeyListener keyAdapter = new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						for (Enumeration<AbstractButton> buttons = categoryGroup.getElements(); buttons.hasMoreElements(); ) {
							AbstractButton button = buttons.nextElement();
							// activate the armed, aka via keyboard arrow keys "selected" but not yet activated, button
							// which is our RadioButton for filtering. Can be only one. ButtonGroup of course can't tell
							// you which one it is directly.
							if (button instanceof JRadioButtonMenuItem && ((JRadioButtonMenuItem) button).isArmed()) {
								button.doClick();
								e.consume();
							}
						}
					}
				}
			};
			menu.addKeyListener(keyAdapter);
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

		searchField = createSearchField();
		searchActionField = createSearchActionField(searchField);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0d;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(searchActionField, gbc);

		gbc.gridx += 1;
		gbc.weightx = 0.0d;
		gbc.fill = GridBagConstraints.NONE;

		resultDialogFocusHolder = createResultDialogFocusHolder();
		add(resultDialogFocusHolder, gbc);

		gbc.gridx += 1;
		filterButton = createFilterButton();
		add(filterButton, gbc);

		InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap actionMap = getActionMap();

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CLOSE_FULL_SEARCH_DIALOG);
		addCloseFullSearchDialogAction(actionMap);

		setFocusCycleRoot(true);
		setFocusTraversalPolicyProvider(true);
		setFocusTraversalPolicy(new GSPanelFocusTraversalPolicy());

		SwingUtilities.invokeLater(() -> {
			RapidMinerGUI.getMainFrame().addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					hideResultdialog();
				}

				@Override
				public void componentHidden(ComponentEvent e) {
					hideResultdialog();
				}

				@Override
				public void componentMoved(ComponentEvent e) {
					hideResultdialog();
				}
			});
		});
	}

	/**
	 * Setup a filter button for categories.
	 *
	 * @return the {@link DropDownPopupButton} to select category filtering.
	 */
	private DropDownPopupButton createFilterButton() {
		DropDownPopupButton.DropDownPopupButtonBuilder builder = new DropDownPopupButton.DropDownPopupButtonBuilder();
		DropDownPopupButton newFilterButton = builder.with(new ResourceActionAdapter("global_search.toolbar")).setComposite(SwingUtilities.RIGHT).build();
		newFilterButton.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				Object source = e.getSource();
				populateFilterPopup((JPopupMenu) source);

				filterMenu = (JPopupMenu) source;
				// align popup to the right and make it at most as wide as the whole search panel
				Dimension preferredSize = filterMenu.getPreferredSize();
				int width = Math.min(PREFERRED_WIDTH, preferredSize.width);
				filterMenu.setPreferredSize(new Dimension(width, preferredSize.height));
				Point buttonLocation = newFilterButton.getLocationOnScreen();
				filterMenu.setLocation(buttonLocation.x + newFilterButton.getWidth() - width, buttonLocation.y + newFilterButton.getHeight() - 1);
				filterMenu.setFocusTraversalKeysEnabled(false);
				filterMenu.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						if (e.getKeyCode() == KeyEvent.VK_TAB) {
							if (e.isShiftDown()) {
								GlobalSearchPanel.this.searchField.requestFocusInWindow();
							} else {
								RapidMinerGUI.getMainFrame().requestFocusInWindow();
							}
							hideFiltermenu();
							e.consume();
						}
					}
				});
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
		newFilterButton.putClientProperty(RapidLookTools.PROPERTY_BUTTON_DARK_BORDER, Boolean.TRUE);
		newFilterButton.setText(I18N.getGUILabel("global_search.filter_all.label"));
		newFilterButton.setMnemonic(KeyEvent.VK_L);
		newFilterButton.setPreferredSize(new Dimension(FILTER_BUTTON_WIDTH, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
		return newFilterButton;
	}

	/**
	 * Create a helper component that holds the focus when browsing the results in the search dialog.
	 *
	 * @return A component to listen to key input and react properly.
	 */
	private JComponent createResultDialogFocusHolder() {
		JPanel newResultDialogFocusHolder = new JPanel();
		newResultDialogFocusHolder.setPreferredSize(new Dimension(0, 0));
		newResultDialogFocusHolder.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (e.getOppositeComponent() != null && newResultDialogFocusHolder.getClientProperty(MOUSE_FOCUS_TRIGGER) == null) {
					resultDialog.selectFirstOrLastEntry(true);
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				// only hide if the opposite component is in Studio (aka not null)
				resultDialog.resetSelection();
				hideGlobalSearchResults(e);
			}
		});
		newResultDialogFocusHolder.setFocusTraversalKeysEnabled(false);
		newResultDialogFocusHolder.addKeyListener(new GSDialogKeyListener());
		return newResultDialogFocusHolder;
	}

	/**
	 * Hide the global search if the focus was lost to some other part of RapidMiner Studio
	 *
	 * @param e
	 * 		The {@link FocusEvent} that occured.
	 */
	private void hideGlobalSearchResults(FocusEvent e) {
		SwingUtilities.invokeLater(() -> {
			final Component focusOwner = RapidMinerGUI.getMainFrame().getFocusOwner();
			if (focusOwner != resultDialogFocusHolder && focusOwner != searchField && e.getOppositeComponent() != null) {
				hideResultdialog();
				logGlobalSearchFocusLost();
				resultDialogFocusHolder.putClientProperty(MOUSE_FOCUS_TRIGGER, null);
			}
		});
	}

	/**
	 * Create a {@link TextFieldWithAction} that searches when typing.
	 *
	 * @param textField
	 * 		the base textfield to be enhanced
	 * @return the {@link TextFieldWithAction}
	 */
	private TextFieldWithAction createSearchActionField(FilterTextField textField) {
		TextFieldWithAction newSearchActionField = new TextFieldWithAction(textField, clearFilterAction, CLEAR_FILTER_HOVERED_ICON) {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(SEARCH_FIELD_WIDTH, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT);
			}
		};
		newSearchActionField.setDefaultIcon(DEFAULT_FILTER_ICON);
		newSearchActionField.putClientProperty(RapidLookTools.PROPERTY_INPUT_DARK_BORDER, Boolean.TRUE);
		newSearchActionField.putClientProperty(RapidLookTools.PROPERTY_INPUT_TYPE_COMPOSITE, SwingConstants.LEFT);
		return newSearchActionField;
	}

	/**
	 * Create a search textfield for the global search
	 *
	 * @return the search input textfield
	 */
	private FilterTextField createSearchField() {
		FilterTextField newSearchField = new FilterTextField(12);
		newSearchField.setDefaultFilterText(I18N.getMessage(I18N.getGUIBundle(), "gui.field.global_search.prompt"));
		// listen for value change events, trigger search with slight delay to not search while user types
		newSearchField.addFilterListener(value -> controller.handleSearch(newSearchField.getText(), categoryFilter));
		// listen for special events that can detect when user pressed Enter. Search straight away in that case w/o delay
		newSearchField.addSelectionNavigationListener(new SelectionNavigationListener() {
			@Override
			public void up() {
				if (resultDialog != null && resultDialog.isVisible()) {
					resultDialogFocusHolder.requestFocusInWindow();
					SwingUtilities.invokeLater(() -> {
						resultDialog.resetSelection();
						resultDialog.selectFirstOrLastEntry(false);
					});
				}
			}

			@Override
			public void down() {
				if (resultDialog != null && resultDialog.isVisible()) {
					resultDialogFocusHolder.requestFocusInWindow();
				}
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
				controller.handleSearch(newSearchField.getText(), categoryFilter);
			}
		});
		// clicking away from the search field should close the search popup
		// this is the next best thing because we cannot detect focus loss on dialog as dialog cannot be focused anymore
		// also open dialog again if the search field gains focus again
		newSearchField.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				hideFiltermenu();
				reopenResults();
			}

			@Override
			public void focusLost(FocusEvent e) {
				hideGlobalSearchResults(e);
			}
		});

		// open dialog after it was closed with escape
		newSearchField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e)) {
					return;
				}
				reopenResults();
			}
		});
		return newSearchField;
	}

	/**
	 * Opens the result dialog if it is not visible right now and the search field is not empty. This method is used to
	 * support one-click functionality, i.e. showing the results after either the search field gains focus again or
	 * was clicked after the result dialog was closed with the ESC key.
	 *
	 * @since 8.2
	 */
	private void reopenResults() {
		if (searchField.getText().trim().isEmpty()) {
			return;
		}
		if (resultDialog != null && resultDialog.isVisible()) {
			return;
		}
		showResults();
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

	/**
	 * Select this category and this index from the global search results.
	 *
	 * @param category
	 * 		name of the category
	 * @param index
	 * 		index of the entry to be selected
	 * @since 9.0.0
	 */
	public void select(String category, int index) {
		if (resultDialog != null) {
			resultDialog.resetSelection();
			resultDialog.select(category, index);

			if (!resultDialogFocusHolder.hasFocus()) {
				resultDialogFocusHolder.putClientProperty(MOUSE_FOCUS_TRIGGER, Boolean.TRUE);
				resultDialogFocusHolder.requestFocusInWindow();
			}
		}
	}

	/**
	 * Show the filterMenu that is shown after clicking the filterButton.
	 *
	 * @return if available returns the filterMenu, else the filterButton
	 */
	private Component showFilterMenu() {
		hideResultdialog();
		filterButton.doClick();
		if (filterMenu != null && filterMenu.getComponentCount() > 0 && filterMenu.getComponent(0) instanceof JRadioButtonMenuItem) {
			return filterMenu;
		} else {
			return filterButton;
		}
	}

	/**
	 * Put the focus and cursor to the searchField but remove the selection to be able to keep typing.
	 */
	void putCursorIntoSearchfield() {
		searchField.requestFocusInWindow();
		SwingUtilities.invokeLater(() -> searchField.select(Integer.MAX_VALUE, Integer.MAX_VALUE));
	}
}
