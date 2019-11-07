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
package com.rapidminer.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.gui.actions.WorkspaceAction;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.search.GlobalSearchPanel;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.composite.PerspectiveToggleGroup;
import com.rapidminer.gui.tools.components.composite.SplitButton;
import com.rapidminer.tools.I18N;


/**
 * Main tool bar of RapidMiner Studio. The tool bar consist of three columns. Action buttons such as
 * the save or run button are shown on the left, the perspective navigation is shown in the middle,
 * and additional resources are displayed on the right.
 *
 * @author Michael Knopf
 * @see MainFrame
 * @since 7.0.0
 */
public class MainToolBar extends JPanel {

	private static final Dimension PERSPECTIVES_SIZE = new Dimension(100, 32);

	private static final long serialVersionUID = 1L;

	/** How many views should be shown by default as buttons */
	private static final int DEFAULT_PRIMARY_VISIBLE = 3;

	/** Split button containing all run actions. */
	private SplitButton runActions;

	/** Panel containing all clickable actions */
	private JPanel actionsPanel;

	/** Panel which contains the perspective group and a label */
	private JPanel perspectivesPanel;

	/** Label contained in the {@link #perspectivesPanel} */
	private final ResourceLabel perspectivesLabel;

	/** Displays the available perspectives */
	private PerspectiveToggleGroup perspectivesGroup;

	/** Maps the perspective name to the corresponding action */
	private Map<String, Action> perspectiveActionMap = new HashMap<>();

	/** The cached name of the current perspective */
	private String perspectiveName;

	/**
	 * Creates a new tool bar instance. The new instance only contains build-in actions and
	 * perspectives. To display element registered by an extension, the {@link #update()} method
	 * must be invoked (after the extension is fully initialized).
	 *
	 * @param mainframe
	 *            the mainframe that uses this tool bar
	 */
	public MainToolBar(final MainFrame mainframe) {
		// use default look and feel background
		setOpaque(true);
		setBackground(Colors.WINDOW_BACKGROUND);
		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.TAB_BORDER));

		// The three columns are implemented via a grid-bag layout. The idea is that both the left
		// and the right column have the same weight and are both set to grow (fill) horizontally to
		// ensure an equal column whenever possible. As a consequence, the middle column (which is
		// not set to grow) is automatically aligned to the center of the tool bar.
		//
		// To make this work it is necessary that both the left and the right panel have the same
		// preferred width.
		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		// left column
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// action button panel
		{
			FlowLayout actionsLayout = new FlowLayout(FlowLayout.LEFT);
			actionsLayout.setVgap(0);
			actionsPanel = new JPanel(actionsLayout);
			actionsPanel.setOpaque(false);

			JButton newButton = new JButton(mainframe.NEW_ACTION);
			newButton.setHideActionText(true);
			actionsPanel.add(newButton);

			JButton openButton = new JButton(mainframe.OPEN_ACTION);
			openButton.setHideActionText(true);
			actionsPanel.add(openButton);

			SplitButton saveButtons = new SplitButton(mainframe.SAVE_ACTION, mainframe.SAVE_AS_ACTION);
			saveButtons.SetHideActionText(true);
			actionsPanel.add(saveButtons);

			actionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));

			runActions = new SplitButton(mainframe.RUN_ACTION);
			runActions.SetHideActionText(true);
			actionsPanel.add(runActions);

			JButton stopButton = new JButton(mainframe.STOP_ACTION);
			stopButton.setHideActionText(true);
			actionsPanel.add(stopButton);

			add(actionsPanel, gbc);
		}

		// middle column
		gbc.gridx += 1;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;

		// perspectives panel
		{
			FlowLayout perspectiveLayout = new FlowLayout(FlowLayout.LEFT);
			perspectiveLayout.setVgap(0);

			perspectivesPanel = new JPanel(perspectiveLayout);
			perspectivesPanel.setOpaque(false);
			perspectivesLabel = new ResourceLabel("workspace_views");
			perspectivesLabel.setForeground(Color.GRAY);
			perspectivesPanel.add(perspectivesLabel);

			PerspectiveToggleGroup.init();
			final PerspectiveController perspectiveController = mainframe.getPerspectiveController();
			PerspectiveModel perspectiveModel = perspectiveController.getModel();
			perspectiveModel.addObserver((observable, perspectives) -> {
				updatePerspectivePanel(perspectiveController, perspectives);
				updateSelection();
			}, true);
			perspectiveModel.addPerspectiveChangeListener(perspective -> {
				perspectiveName = perspective.getName();
				SwingTools.invokeLater(this::updateSelection);
			});
			updatePerspectivePanel(perspectiveController, perspectiveController.getModel().getAllPerspectives());
			addComponentListener(new MainToolBarResizer(perspectiveController));
			add(perspectivesPanel, gbc);
		}

		// Filler
		gbc.gridx += 1;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(new JLabel(), gbc);

		// Global Search
		gbc.gridx += 1;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 5);
		add(GlobalSearchPanel.getInstance(), gbc);

		// help section
		gbc.gridx += 1;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;

	}

	/**
	 * Updates the {@link #perspectivesGroup} to show which view is selected.
	 *
	 * @since 8.2.1
	 */
	private void updateSelection() {
		Action perspectiveAction = perspectiveActionMap.get(perspectiveName);
		if (perspectiveAction != null) {
			perspectivesGroup.setSelected(perspectiveAction);
		}
	}


	public void update() {
		List<MenuItemFactory> factories = RunActionRegistry.INSTANCE.getFacories();
		for (MenuItemFactory factory : factories) {
			runActions.getPopupMenu().addSeparator();
			for (MenuItemFactory.MenuEntry entry : factory.create()) {
				if (entry.isAction()) {
					runActions.getPopupMenu().add(entry.getAction());
				} else if (entry.isMenu()) {
					runActions.getPopupMenu().add(entry.getMenu());
				} else if (entry.isComponent()) {
					runActions.getPopupMenu().add(entry.getComponent());
				}
			}
		}
	}

	/**
	 * Updates the {@link #perspectivesGroup} in regard to the perspective controller and the given
	 * perspectives.
	 *
	 * @param perspectiveController
	 *            the controller which should be used
	 * @param perspectives
	 *            all available perspectives
	 */
	private void updatePerspectivePanel(final PerspectiveController perspectiveController,
										Collection<Perspective> perspectives) {
		if (perspectivesGroup != null) {
			perspectivesPanel.remove(perspectivesGroup);
		}
		perspectiveActionMap.clear();
		List<Action> knownActionList = new ArrayList<>();
		List<Action> nonUserActionList = new ArrayList<>();
		List<Action> userDefinedActionList = new ArrayList<>();
		for (Perspective p : perspectives) {
			String name = p.getName();
			Action action = new WorkspaceAction(name);
			action.putValue(Action.LARGE_ICON_KEY, null);
			action.putValue(Action.SMALL_ICON, null);
			if (p.isUserDefined()) {
				action.putValue(Action.ACTION_COMMAND_KEY, "perspective-" + name);
				action.putValue(Action.NAME, name);
				action.putValue(Action.SHORT_DESCRIPTION,
						I18N.getMessage(I18N.getGUIBundle(), "gui.action.workspace_user.tip", name));
			}

			// Add Design, Result, Model Wizard and Hadoop Data to known actions, since they are supported and should
			// be in this exact order
			switch (p.getName()) {
				case PerspectiveModel.DESIGN:
				case PerspectiveModel.RESULT:
				case PerspectiveModel.HADOOP_DATA:
					knownActionList.add(action);
					break;
				case PerspectiveModel.TURBO_PREP:
					// ensure model turbo prep is in front of model wizard view
					// design and result are the first to be added (see PerspectiveModel#makePredefined)
					knownActionList.add(Math.min(knownActionList.size(), 2), action);
					break;
				case PerspectiveModel.MODEL_WIZARD:
					// ensure model wizard view is in front of hadoop data view
					// design and result are the first to be added (see PerspectiveModel#makePredefined)
					knownActionList.add(Math.min(knownActionList.size(), 3), action);
					break;
				case PerspectiveModel.DEPLOYMENTS:
					// ensure model deployment management view is in front of hadoop data view
					// design and result are the first to be added (see PerspectiveModel#makePredefined)
					knownActionList.add(Math.min(knownActionList.size(), 4), action);
					break;
				default:
					if (!p.isUserDefined()) {
						nonUserActionList.add(action);
					} else {
						userDefinedActionList.add(action);
					}
			}

			perspectiveActionMap.put(p.getName(), action);
		}

		nonUserActionList.sort(Comparator.comparing(a -> String.valueOf(a.getValue(ResourceAction.NAME))));
		userDefinedActionList.sort(Comparator.comparing(a -> String.valueOf(a.getValue(ResourceAction.NAME))));

		// combine actions that might be displayed as their own buttons
		List<Action> displayableActionList = new ArrayList<>(knownActionList);
		displayableActionList.addAll(nonUserActionList);

		boolean hasUserDefined = !userDefinedActionList.isEmpty();
		VisibilitySettings visibility = calculateVisibility(displayableActionList.size(), hasUserDefined);
		// primary actions and secondary actions
		Action[][] actions = createActionArrays(displayableActionList, visibility.primary);

		perspectivesGroup = null;
		if (actions[0] != null && actions[0].length > 1) {
			perspectivesGroup = new PerspectiveToggleGroup(perspectiveController, PERSPECTIVES_SIZE, actions[0]);
		}
		// check for user views to keep order of popup items the same (non-user, create view, user)
		if (perspectivesGroup != null && (actions[1].length > 0 || hasUserDefined)) {
			perspectivesGroup.addSeconderyActions(actions[1]);
		}
		if (perspectivesGroup != null && hasUserDefined) {
			perspectivesGroup.addSeconderyActions(userDefinedActionList.toArray(new Action[0]));
		}
		if (perspectivesGroup != null) {
			perspectivesLabel.setVisible(visibility.showLabel);
			if (!visibility.fullMoreButton) {
				perspectivesGroup.minimizeSecondaryButton();
			}
			perspectivesPanel.add(perspectivesGroup);
			perspectivesPanel.validate();
			perspectivesPanel.repaint();
		}
		updateSelection();
	}

	/**
	 * Calculates how many actions should be visible (i.e. primary buttons) and if the {@link #perspectivesLabel} and
	 * "More" button (i.e. secondary button) should be visible/minimized. Will return the result in a
	 * {@link VisibilitySettings} object.
	 *
	 * @param numberOfActions
	 * 		number of displayable actions
	 * @param hasUserDefined
	 * 		if user defined views are registered
	 * @return the visibility settings
	 * @since 8.1
	 */
	private VisibilitySettings calculateVisibility(int numberOfActions, boolean hasUserDefined) {
		int toolBarWidth = getWidth();
		if (toolBarWidth == 0) {
			VisibilitySettings visibility = new VisibilitySettings();
			// not yet initialised; build default (3 primary actions, views label, full "More" button)
			visibility.primary = numberOfActions;
			if (visibility.primary > DEFAULT_PRIMARY_VISIBLE) {
				visibility.primary = DEFAULT_PRIMARY_VISIBLE;
			}
			visibility.showLabel = visibility.fullMoreButton = true;
			return visibility;
		}

		VisibilitySettings visibility = new VisibilitySettings();
		int labelWidth = perspectivesLabel.getPreferredSize().width + 5; //insets
		int moreButtonWidth = PerspectiveToggleGroup.getDefaultSecondaryButtonSize().width;
		int moreButtonMinWidth = PerspectiveToggleGroup.getMinimizedSecondaryButtonSize().width;
		int actionsWidth = actionsPanel.getPreferredSize().width;
		int searchWidth = GlobalSearchPanel.PREFERRED_WIDTH + 5; //insets
		int availableWidth = toolBarWidth - actionsWidth - searchWidth - 10; //insets?

		// make sure to not calculate too much
		visibility.primary = Math.min(DEFAULT_PRIMARY_VISIBLE, numberOfActions);
		if (visibility.primary >= numberOfActions && !hasUserDefined) {
			// no secondary actions => no "More" button
			moreButtonWidth = moreButtonMinWidth = 0;
		}

		int primaryWidthNeeded = visibility.primary * PERSPECTIVES_SIZE.width;
		if (availableWidth >= labelWidth + primaryWidthNeeded + moreButtonWidth) {
			// everything can be shown
			visibility.primary = availableWidth - labelWidth - moreButtonWidth;
			visibility.primary /= PERSPECTIVES_SIZE.width;
			visibility.showLabel = visibility.fullMoreButton = true;
		} else {
			// check if label should be hidden or "more" button be minimized
			availableWidth -= primaryWidthNeeded;
			visibility.showLabel = availableWidth >= labelWidth + moreButtonMinWidth;
			if (moreButtonMinWidth != 0) {
				visibility.fullMoreButton = !visibility.showLabel && availableWidth >= moreButtonWidth;
			}
		}
		return visibility;
	}

	/**
	 * Returns a filled action matrix with all provided actions. Will split the actions according to {@code primaryVisible}.
	 *
	 * @param actionList
	 * 		list of displayable actions
	 * @param primaryVisible
	 * 		limit of visible primary actions
	 * @return the filled action matrix
	 * @since 8.1
	 */
	private Action[][] createActionArrays(List<Action> actionList, int primaryVisible) {
		Action[][] actions = new Action[2][];
		Iterator<Action> actionIterator = actionList.iterator();
		if (primaryVisible > actionList.size()) {
			primaryVisible = actionList.size();
		}
		actions[0] = new Action[primaryVisible];
		actions[1] = new Action[actionList.size() - primaryVisible];
		int i = 0;
		for (; i < primaryVisible; i++) {
			actions[0][i] = actionIterator.next();
		}
		while (actionIterator.hasNext()) {
			actions[1][i - primaryVisible] = actionIterator.next();
			i++;
		}
		return actions;
	}

	/**
	 * Simple resize listener for the {@link MainToolBar}. Will update on resize events if {@link #shouldUpdate()}
	 * returns {@code true};
	 *
	 * @author Jan Czogalla
	 * @since 8.1
	 */
	private class MainToolBarResizer extends ComponentAdapter {

		PerspectiveController perspectiveController;

		MainToolBarResizer(PerspectiveController perspectiveController) {
			this.perspectiveController = perspectiveController;
		}

		@Override
		public void componentResized(ComponentEvent e) {
			if (e.getComponent() != MainToolBar.this) {
				return;
			}
			if (shouldUpdate()) {
				updatePerspectivePanel(perspectiveController, perspectiveController.getModel().getAllPerspectives());
			}
		}

		/** Whether to update {@link #perspectivesPanel} on resize. */
		private boolean shouldUpdate() {
			return true;
		}
	}

	/** Simple POJO to store visibility for the {@link #calculateVisibility(int, boolean)} method. */
	private static class VisibilitySettings {
		int primary;
		boolean showLabel;
		boolean fullMoreButton;
	}

}
