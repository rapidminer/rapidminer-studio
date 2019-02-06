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
package com.rapidminer.gui.tools.components.composite;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.Perspective;
import com.rapidminer.gui.PerspectiveController;
import com.rapidminer.gui.actions.NewPerspectiveAction;
import com.rapidminer.gui.actions.WorkspaceAction;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.tools.I18N;


/**
 * A {@link CompositeMenuToggleButton} that can be used to manage {@link Perspective}s
 *
 * Whether the {@code CompositeButton} is the left-most, a center, or the right-most element of the
 * composition can be specified in the constructors via the Swing constants
 * {@link SwingConstants#LEFT}, {@link SwingConstants#CENTER}, and {@link SwingConstants#RIGHT}
 * respectively.
 *
 * @author Marcel Michel
 * @since 7.0.0
 */
class PerspectiveMenuToggleButton extends CompositeMenuToggleButton {

	private static final long serialVersionUID = 1L;

	private static final ImageIcon REMOVE_PERSPECTIVE_ICON = SwingTools.createIcon("16/x-mark.png");

	private static final ImageIcon REMOVE_PERSPECTIVE_HOVER_ICON = SwingTools.createIcon("16/x-mark_orange.png");

	private static final String BLANKS = "       ";

	private final PerspectiveController applicationPerspectiveController;

	/**
	 * Creates a new {@code PerspectiveMenuToggleButton} with the given {@link Action} to be used at
	 * the given position.
	 *
	 * @param perspectiveController
	 *            the active perspective controller
	 * @param position
	 *            the position in the composite element ({@link SwingConstants#LEFT},
	 *            {@link SwingConstants#CENTER}, or {@link SwingConstants#RIGHT})
	 * @param actions
	 *            the menu actions
	 */
	public PerspectiveMenuToggleButton(PerspectiveController perspectiveController, int position, Action... actions) {
		super(position, actions);
		this.applicationPerspectiveController = perspectiveController;
		this.setToolTipText(I18N.getGUIMessage("gui.split_button.drop_down.tip"));
		addNewPerspectiveAction();
	}

	@Override
	public void addActions(Action... actions) {
		for (final Action action : actions) {
			final JRadioButtonMenuItem item = new JRadioButtonMenuItem(action);
			item.setText(item.getText() + BLANKS);
			item.setLayout(new BorderLayout());

			// the first call to this method is triggered by the super() call in the constructor, thus the perspective controller is null
			// this is the case for non-user defined perspectives which are neither "design" nor "result". Don't want the remove button for those
			boolean userDefined = false;
			if (applicationPerspectiveController != null) {
				Perspective perspective = applicationPerspectiveController.getModel().getPerspective(String.valueOf(action.getValue(ResourceAction.NAME)));
				userDefined = perspective != null && perspective.isUserDefined();
			}

			final JButton removePerspectiveButton = new JButton(REMOVE_PERSPECTIVE_ICON);
			removePerspectiveButton.setMargin(new Insets(0, 0, 0, 0));
			removePerspectiveButton.setBorderPainted(false);
			removePerspectiveButton.setOpaque(false);
			removePerspectiveButton.setContentAreaFilled(false);
			removePerspectiveButton.setVisible(false);
			removePerspectiveButton.addActionListener(e -> {
				if (action instanceof WorkspaceAction) {

					String perspectiveName = ((WorkspaceAction) action).getPerspectiveName();
					popupMenu.setVisible(false);
					removePerspectiveButton.setIcon(REMOVE_PERSPECTIVE_ICON);
					removePerspectiveButton.setVisible(false);
					if (SwingTools.showConfirmDialog("delete_perspective", ConfirmDialog.YES_NO_OPTION, perspectiveName) == ConfirmDialog.YES_OPTION) {
						applicationPerspectiveController.removePerspective(perspectiveName);
					}
				}

			});

			removePerspectiveButton.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseExited(MouseEvent e) {
					removePerspectiveButton.setIcon(REMOVE_PERSPECTIVE_ICON);
					item.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, item));
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					removePerspectiveButton.setIcon(REMOVE_PERSPECTIVE_HOVER_ICON);
					item.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, item));
				}
			});

			// only add the remove button for user-defined perspectives
			if (userDefined) {
				item.add(removePerspectiveButton, BorderLayout.EAST);
			}

			item.addActionListener(e -> {
				removePerspectiveButton.setVisible(false);
				updateSelectionStatus();
			});

			item.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseExited(MouseEvent e) {
					removePerspectiveButton.setVisible(false);

				}

				@Override
				public void mouseEntered(MouseEvent e) {
					removePerspectiveButton.setVisible(true);

				}
			});

			popupMenuGroup.add(item);
			popupMenu.add(item);
		}

	}

	/**
	 * Adds the new perspective action to the menu.
	 */
	private void addNewPerspectiveAction() {
		Action newPerspectiveAction = new NewPerspectiveAction();
		newPerspectiveAction.putValue(Action.LARGE_ICON_KEY, null);
		newPerspectiveAction.putValue(Action.SMALL_ICON, null);

		JMenuItem item = new JMenuItem(newPerspectiveAction);
		item.setPreferredSize(new Dimension(item.getPreferredSize().width + 30, item.getPreferredSize().height));

		popupMenu.addSeparator();
		popupMenu.add(item);
		popupMenu.addSeparator();
	}
}
