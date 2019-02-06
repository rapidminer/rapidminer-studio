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
package com.rapidminer.studio.io.gui.internal.steps;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jdesktop.swingx.JXTextField;

import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.core.io.data.source.DataSourceFactory;
import com.rapidminer.core.io.data.source.DataSourceFactoryRegistry;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.LinkRemoteButton;
import com.rapidminer.studio.io.gui.internal.DataImportWizardUtils;
import com.rapidminer.studio.io.gui.internal.DataWizardEventType;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.update.internal.UpdateManagerRegistry;


/**
 * The type selection view which is shown within the {@link ImportWizard} to allow the selection of
 * the {@link DataSource} type.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
public final class TypeSelectionView extends JPanel {

	private static final Dimension SCROLL_PANE_SIZE = new Dimension(700, 400);
	private static final Dimension SEARCH_FIELD_SIZE = new Dimension(550, 40);
	private static final Dimension TYPE_BUTTON_DIMENSION = new Dimension(280, 60);

	/** re-use same log timer for all data import wizard dialogs */
	private static final Timer SEARCH_ACTION_LOG_TIMER = new Timer(500, null);

	private final ResourceAction searchInMarketplaceAction = new ResourceAction(true,
			"io.dataimport.step.type_selection.search_in_mp") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			openMarketplaceDialog();
		}

	};

	private final ResourceAction tryMarketplaceSearchAction = new ResourceAction(true,
			"io.dataimport.step.type_selection.try_searching_marketplace") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			openMarketplaceDialog();
		}

	};

	// Disable search text field until there are more data sources available
	// (just uncomment code below in case it should be added again)

	// private final ResourceAction CLEAR_SEARCH_ACTION = new ResourceAction(true, "clear_filter") {
	//
	// private static final long serialVersionUID = 1L;
	//
	// @Override
	// public void actionPerformed(final ActionEvent e) {
	// searchTextField.setText(null);
	// searchTextField.requestFocusInWindow();
	// }
	// };
	// private final ImageIcon CLEAR_SEARCH_HOVERED_ICON =
	// SwingTools.createIcon("16/x-mark_orange.png");

	private static final long serialVersionUID = 1L;

	private final JXTextField searchTextField;
	private final ImportWizard wizard;
	private final JPanel mainContentPanel;
	private final List<JButton> dataSourceSelectionButtons = new ArrayList<>();

	/**
	 * Constructs a new type selection view instance.
	 *
	 * @param wizard
	 *            the data import wizard
	 */
	public TypeSelectionView(final ImportWizard wizard) {
		this.wizard = wizard;

		JPanel dataSourcesPanel = new JPanel(new GridBagLayout());
		GridBagConstraints constraint = new GridBagConstraints();

		// Search field
		{

			this.searchTextField = new JXTextField(I18N.getGUILabel("io.dataimport.step.type_selection.search_placeholder"));
			this.searchTextField.getDocument().addUndoableEditListener(e -> {
				String searchTerm = searchTextField.getText();
				if (searchTerm != null && !searchTerm.trim().isEmpty() && searchTerm.trim().length() > 1) {
					updateTypeSelectionContentPanel(searchTerm, false);
				} else {
					updateTypeSelectionContentPanel(null, false);
				}
			});

			searchTextField.setMinimumSize(SEARCH_FIELD_SIZE);
			searchTextField.setPreferredSize(SEARCH_FIELD_SIZE);

			// Disable search text field until there are more data sources available
			// (just uncomment code below in case it should be added again)

			// TextFieldWithAction outerTextField = new TextFieldWithAction(searchTextField,
			// CLEAR_SEARCH_ACTION,
			// CLEAR_SEARCH_HOVERED_ICON);
			//
			// constraint.weightx = 1;
			// constraint.weighty = 0;
			// constraint.gridwidth = GridBagConstraints.REMAINDER;
			// constraint.fill = GridBagConstraints.VERTICAL;
			// constraint.insets = new Insets(30, 0, 20, 0);
			// dataSourcesPanel.add(outerTextField, constraint);
		}

		// Center panel
		{
			JPanel centerPanel = new JPanel(new GridBagLayout());

			GridBagConstraints centerConstraint = new GridBagConstraints();
			centerConstraint.fill = GridBagConstraints.BOTH;
			centerConstraint.weightx = 1;
			centerConstraint.gridy = 0;

			JPanel leftFillPanel = new JPanel();
			centerPanel.add(leftFillPanel, centerConstraint);

			centerConstraint.fill = GridBagConstraints.BOTH;
			centerConstraint.weightx = 0;
			centerConstraint.insets = new Insets(20, 0, 0, 0);
			this.mainContentPanel = new JPanel(new GridBagLayout());
			centerPanel.add(mainContentPanel, centerConstraint);

			centerConstraint.fill = GridBagConstraints.BOTH;
			centerConstraint.weightx = 1;
			centerConstraint.insets = new Insets(0, 0, 0, 0);

			JPanel rightFillPanel = new JPanel();
			centerPanel.add(rightFillPanel, centerConstraint);

			centerConstraint.fill = GridBagConstraints.BOTH;
			centerConstraint.gridy += 1;
			centerConstraint.weightx = 1;
			centerConstraint.weighty = 1;
			centerConstraint.gridwidth = GridBagConstraints.REMAINDER;

			JPanel bottomFillPanel = new JPanel();
			centerPanel.add(bottomFillPanel, centerConstraint);

			constraint.weighty = 1;
			constraint.gridwidth = GridBagConstraints.REMAINDER;
			constraint.fill = GridBagConstraints.BOTH;
			constraint.insets = new Insets(0, 0, 0, 0);
			dataSourcesPanel.add(centerPanel, constraint);
		}

		add(dataSourcesPanel);

		updateTypeSelectionContentPanel(null, true);
	}

	private JButton createDataSourceSelectionButton(@SuppressWarnings("rawtypes") final DataSourceFactory factory) {
		String label = DataImportWizardUtils.getFactoryLabel(factory);
		String description = DataImportWizardUtils.getFactoryDescription(factory);

		JButton typeSelectionButton = new JButton(new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent e) {
				enableDataSourceButtons(false);

				// update the wizard by setting the selected factory
				wizard.setDataSource(factory.createNew(), factory);

				// switch to the next wizard step (location selection)
				wizard.nextStep();
			}

		});

		typeSelectionButton.setText(label);
		typeSelectionButton.setToolTipText(description);

		typeSelectionButton.setMinimumSize(TYPE_BUTTON_DIMENSION);
		typeSelectionButton.setPreferredSize(TYPE_BUTTON_DIMENSION);
		typeSelectionButton.setMaximumSize(TYPE_BUTTON_DIMENSION);
		typeSelectionButton.setIcon(DataImportWizardUtils.getFactoryIcon(factory));

		return typeSelectionButton;
	}

	private void enableDataSourceButtons(boolean enable) {
		SwingTools.invokeLater(() -> {
			for (JButton button : dataSourceSelectionButtons) {
				button.setEnabled(enable);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	private List<DataSourceFactory> getFilteredFactories(String searchTerm) {
		List<DataSourceFactory<?>> factories = new LinkedList<>(DataSourceFactoryRegistry.INSTANCE.getFactories());
		List<DataSourceFactory> result = new LinkedList<>();

		if (searchTerm == null || searchTerm.trim().isEmpty()) {
			result.addAll(factories);
		} else {
			// filter factories according to search term
			for (DataSourceFactory factory : factories) {
				String label = DataImportWizardUtils.getFactoryLabel(factory);
				String description = DataImportWizardUtils.getFactoryDescription(factory);
				boolean labelMatches = label.toLowerCase(Locale.ENGLISH).contains(searchTerm.toLowerCase(Locale.ENGLISH));
				boolean descriptionMatches = description.toLowerCase(Locale.ENGLISH)
						.contains(searchTerm.toLowerCase(Locale.ENGLISH));
				boolean matchesSearchTerm = labelMatches || descriptionMatches;
				if (matchesSearchTerm) {
					result.add(factory);
				}
			}
		}
		return result;
	}

	private void updateTypeSelectionContentPanel(final String searchTerm, final boolean requestFocusForButton) {

		// stop timer
		SEARCH_ACTION_LOG_TIMER.stop();

		// schedule new task to log search term
		if (searchTerm != null) {

			// remove all action listeners
			for (ActionListener l : SEARCH_ACTION_LOG_TIMER.getActionListeners()) {
				SEARCH_ACTION_LOG_TIMER.removeActionListener(l);
			}

			// add new action listener
			SEARCH_ACTION_LOG_TIMER.addActionListener(e -> DataImportWizardUtils.logStats(DataWizardEventType.SEARCH_TYPE, searchTerm));

			// start countdown
			SEARCH_ACTION_LOG_TIMER.setRepeats(false);
			SEARCH_ACTION_LOG_TIMER.start();
		}

		SwingTools.invokeLater(() -> {

			JButton focusButton = null;

			boolean resultEmpty;

			// clear panel
			mainContentPanel.removeAll();

			// add factory buttons panel
			{

				JPanel factoryButtonPanel = new JPanel(new GridBagLayout());
				JScrollPane scrollPane = new JScrollPane(factoryButtonPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
						JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				scrollPane.setMaximumSize(SCROLL_PANE_SIZE);
				scrollPane.setBorder(BorderFactory.createEmptyBorder());

				GridBagConstraints constraint = new GridBagConstraints();
				constraint.gridx = 0;
				constraint.gridy = 0;
				constraint.insets = new Insets(5, 0, 5, 15);
				constraint.fill = GridBagConstraints.NONE;

				// retrieve factories
				List<DataSourceFactory> factoryMatches = getFilteredFactories(searchTerm);

				// reverse list so first registered factories are displayed at the beginning
				Collections.reverse(factoryMatches);

				resultEmpty = factoryMatches.isEmpty();

				// set preferred size for scroll pane in case of more than 10 factories.
				// Otherwise the scrollbar won't show up.
				if (factoryMatches.size() > 10) {
					scrollPane.setPreferredSize(SCROLL_PANE_SIZE);
				}

				dataSourceSelectionButtons.clear();
				// show a type selection button for each data source factory
				for (DataSourceFactory factory : factoryMatches) {
					JButton typeSelectionButton = createDataSourceSelectionButton(factory);
					dataSourceSelectionButtons.add(typeSelectionButton);

					if (focusButton == null) {
						focusButton = typeSelectionButton;
					}
					factoryButtonPanel.add(typeSelectionButton, constraint);

					// update constraints for next button
					constraint.gridx += 1;
					constraint.insets = new Insets(5, 0, 5, 0);
					if (constraint.gridx > 1) {
						constraint.gridx = 0;
						constraint.gridy += 1;
						constraint.insets = new Insets(5, 0, 5, 15);
					}
				}

				// fix for uneven number of data sources
				if (factoryMatches.size() % 2 == 1) {
					constraint.gridx = 0;
					constraint.gridy += 1;
					constraint.insets = new Insets(5, 0, 5, 15);
				}

				constraint = new GridBagConstraints();
				constraint.fill = GridBagConstraints.NONE;
				constraint.gridwidth = GridBagConstraints.REMAINDER;
				constraint.weightx = 1;
				constraint.weighty = 0;

				mainContentPanel.add(scrollPane, constraint);
			}

			// add link button below factory buttons
			{
				JPanel linkButtonPanel = new JPanel();

				// Add "empty result" text in case the search was empty
				if (resultEmpty) {
					JPanel noResultsPanel = new JPanel(new BorderLayout());

					JLabel noResultsSymbol = new ResourceLabel("io.dataimport.step.type_selection.empty_search_symbol");
					noResultsSymbol.setHorizontalAlignment(SwingConstants.CENTER);
					noResultsSymbol.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
					noResultsPanel.add(noResultsSymbol, BorderLayout.NORTH);

					JLabel noResultsLabel = new ResourceLabel("io.dataimport.step.type_selection.empty_search",
							searchTerm);
					noResultsPanel.add(noResultsLabel, BorderLayout.CENTER);

					JPanel searchInMpPanel = new JPanel();
					LinkRemoteButton searchInMarketplace = new LinkRemoteButton(tryMarketplaceSearchAction);
					searchInMarketplace.setAlignmentX(SwingConstants.CENTER);
					searchInMpPanel.add(searchInMarketplace);
					noResultsPanel.add(searchInMpPanel, BorderLayout.SOUTH);

					linkButtonPanel.add(noResultsPanel);
				} else {
					// add "search marketplace" link button
					LinkRemoteButton searchInMarketplace = new LinkRemoteButton(searchInMarketplaceAction);
					searchInMarketplace.setAlignmentX(SwingConstants.CENTER);
					linkButtonPanel.add(searchInMarketplace);

				}

				GridBagConstraints constraint = new GridBagConstraints();
				constraint.insets = new Insets(10, 0, 0, 0);
				constraint.gridwidth = GridBagConstraints.REMAINDER;
				constraint.fill = GridBagConstraints.BOTH;
				constraint.weighty = 1;
				mainContentPanel.add(linkButtonPanel, constraint);
			}

			mainContentPanel.revalidate();
			mainContentPanel.repaint();

			if (requestFocusForButton && focusButton != null) {
				SwingUtilities.invokeLater(focusButton::requestFocusInWindow);
			}
		});

	}

	private void openMarketplaceDialog() {
		try {
			UpdateManagerRegistry.INSTANCE.get().showUpdateDialog(false);
			updateTypeSelectionContentPanel(null, false);
		} catch (URISyntaxException | IOException e1) {
			SwingTools.showSimpleErrorMessage("io.dataimport.step.type_selection.marketplace_connection_error", e1);
		}
	}

	/**
	 * Ensures that the data source buttons are enabled.
	 */
	void enableButtons() {
		enableDataSourceButtons(true);
	}

}
