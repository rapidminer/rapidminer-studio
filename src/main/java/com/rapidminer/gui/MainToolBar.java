/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.rapidminer.gui.actions.AboutAction;
import com.rapidminer.gui.actions.BrowseAction;
import com.rapidminer.gui.actions.RedoAction;
import com.rapidminer.gui.actions.UndoAction;
import com.rapidminer.gui.actions.WorkspaceAction;
import com.rapidminer.gui.actions.startup.TutorialAction;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.osx.OSXAdapter;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.components.DropDownPopupButton.DropDownPopupButtonBuilder;
import com.rapidminer.gui.tools.components.composite.PerspectiveToggleGroup;
import com.rapidminer.gui.tools.components.composite.SplitButton;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;


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

	private static final Dimension PERSPECTIVES_SIZE = new Dimension(120, 32);

	private static final long serialVersionUID = 1L;

	/** Split button containing all run actions. */
	private SplitButton runActions;

	/** Panel which contains the perspective group and a label */
	private JPanel perspectivesPanel;

	/** Panel which contains the resource button */
	private JPanel resourcesPanel;

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

		GridBagConstraints constrainst = new GridBagConstraints();

		// left column
		constrainst.gridx = 0;
		constrainst.weightx = 1;
		constrainst.fill = GridBagConstraints.HORIZONTAL;

		// action button panel
		JPanel actionsPanel;
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

			JButton undoButton = new JButton(new UndoAction(mainframe));
			undoButton.setHideActionText(true);
			actionsPanel.add(undoButton);

			JButton redoButton = new JButton(new RedoAction(mainframe));
			redoButton.setHideActionText(true);
			actionsPanel.add(redoButton);

			actionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));

			runActions = new SplitButton(mainframe.RUN_ACTION);
			runActions.SetHideActionText(true);
			actionsPanel.add(runActions);

			JButton stopButton = new JButton(mainframe.STOP_ACTION);
			stopButton.setHideActionText(true);
			actionsPanel.add(stopButton);

			add(actionsPanel, constrainst);
		}

		// middle column
		constrainst.gridx += 1;
		constrainst.weightx = 0;
		constrainst.fill = GridBagConstraints.NONE;

		// perspectives panel
		{
			FlowLayout perspectiveLayout = new FlowLayout(FlowLayout.LEFT);
			perspectiveLayout.setVgap(0);

			perspectivesPanel = new JPanel(perspectiveLayout);
			perspectivesPanel.setOpaque(false);
			ResourceLabel viewPerspectiveLabel = new ResourceLabel("workspace_views");
			viewPerspectiveLabel.setForeground(Color.GRAY);
			perspectivesPanel.add(viewPerspectiveLabel);

			final PerspectiveController perspectiveController = mainframe.getPerspectiveController();
			PerspectiveModel perspectiveModel = perspectiveController.getModel();
			perspectiveModel.addObserver(new Observer<List<Perspective>>() {

				@Override
				public void update(Observable<List<Perspective>> observable, List<Perspective> perspectives) {
					updatePerspectivePanel(perspectiveController, perspectives);
					Action perspectiveAction = perspectiveActionMap.get(perspectiveName);
					if (perspectiveAction != null) {
						perspectivesGroup.setSelected(perspectiveAction);
					}
				}
			}, true);
			perspectiveModel.addPerspectiveChangeListener(new PerspectiveChangeListener() {

				@Override
				public void perspectiveChangedTo(Perspective perspective) {
					perspectiveName = perspective.getName();
					Action perspectiveAction = perspectiveActionMap.get(perspectiveName);
					if (perspectiveAction != null) {
						perspectivesGroup.setSelected(perspectiveAction);
					}

				}
			});

			updatePerspectivePanel(perspectiveController, perspectiveController.getModel().getAllPerspectives());

			add(perspectivesPanel, constrainst);
		}

		// right column
		constrainst.gridx += 1;
		constrainst.weightx = 1;
		constrainst.fill = GridBagConstraints.HORIZONTAL;

		{
			resourcesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			resourcesPanel.setOpaque(false);
			resourcesPanel.setPreferredSize(actionsPanel.getPreferredSize());

			resourcesPanel.add(createResourcesButton());

			add(resourcesPanel, constrainst);
		}

	}

	/**
	 * @return a dropdown button with a menu containing a links to the tutorial, the online
	 *         documentation, the forum, the support and the about box
	 */
	private Component createResourcesButton() {
		DropDownPopupButtonBuilder builder = new DropDownPopupButtonBuilder();
		builder.with(new ResourceActionAdapter("toolbar_resources"));

		builder.add(new TutorialAction());

		builder.add(new BrowseAction("toolbar_resources.documentation",
				URI.create("http://redirects.rapidminer.com/app/studio/7.2/documentation/main_tool_bar")));
		builder.add(new BrowseAction("toolbar_resources.help_forum",
				URI.create("http://redirects.rapidminer.com/app/studio/7.2/forum/main_tool_bar")));
		builder.add(new BrowseAction("toolbar_resources.support",
				URI.create("http://redirects.rapidminer.com/app/studio/7.2/support/main_tool_bar")));

		// put "About RapidMiner Studio" action as last action if not on ox
		if (SystemInfoUtilities.getOperatingSystem() != OperatingSystem.OSX || !OSXAdapter.isAdapted()) {
			builder.addSeparator();
			builder.add(new AboutAction((MainFrame) ApplicationFrame.getApplicationFrame()));
		}
		return builder.build();
	}

	public void update() {
		List<MenuItemFactory> factories = RunActionRegistry.INSTANCE.getFacories();
		for (int i = 0; i < factories.size(); i++) {
			runActions.getPopupMenu().addSeparator();
			for (MenuItemFactory.MenuEntry entry : factories.get(i).create()) {
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
		List<Action> primaryActionList = new ArrayList<>();
		List<Action> secondaryActionList = new ArrayList<>();
		for (Perspective p : perspectives) {
			String name = p.getName();
			Action action = new WorkspaceAction(perspectiveController, p, name);
			action.putValue(Action.LARGE_ICON_KEY, null);
			action.putValue(Action.SMALL_ICON, null);
			if (p.isUserDefined()) {
				action.putValue(Action.ACTION_COMMAND_KEY, "perspective-" + name);
				action.putValue(Action.NAME, name);
				action.putValue(Action.SHORT_DESCRIPTION,
						I18N.getMessage(I18N.getGUIBundle(), "gui.action.workspace_user.tip", name));
			}
			if (!p.isUserDefined()) {
				primaryActionList.add(action);
			} else {
				secondaryActionList.add(action);
			}
			perspectiveActionMap.put(p.getName(), action);
		}

		if (primaryActionList.size() > 1) {
			perspectivesGroup = new PerspectiveToggleGroup(perspectiveController, PERSPECTIVES_SIZE,
					primaryActionList.toArray(new Action[primaryActionList.size()]));
		}
		if (perspectivesGroup != null && secondaryActionList.size() > 0) {
			perspectivesGroup.addSeconderyActions(secondaryActionList.toArray(new Action[secondaryActionList.size()]));
		}
		if (perspectivesGroup != null) {
			perspectivesPanel.add(perspectivesGroup);
			perspectivesPanel.validate();
			perspectivesPanel.repaint();
		}
	}

}
