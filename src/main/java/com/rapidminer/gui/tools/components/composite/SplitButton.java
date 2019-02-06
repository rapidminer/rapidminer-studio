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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.rapidminer.gui.tools.Ionicon;
import com.rapidminer.tools.I18N;


/**
 * Composite button that allows to specify one primary and multiple secondary {@link Action}. The
 * button displays the primary action similar to a plain {@link JButton} but with a drop down
 * element on the left that opens a {@link JPopupMenu} containing the secondary actions.
 *
 * @author Michael Knopf
 * @since 7.0.0
 */
public class SplitButton extends JPanel {

	private static final long serialVersionUID = 1L;

	/** Label used for the drop down button. */
	private static final String DROPDOWN_LABEL = "<html><span style=\"color: 4F4F4F;\">" + Ionicon.ARROW_DOWN_B.getHtml()
			+ "</span></html>";

	/** Button to display the primary action. */
	private JButton primaryButton;

	/** Button to access the secondary actions. */
	private JButton dropDownButton;

	/** Popup menu associated with drop down button. */
	private JPopupMenu popupMenu;

	/** Remember the last time the popup was closed. */
	private long lastPopupCloseTime = 0;

	/**
	 * Creates a new {@code SplitButton} with the given primary and secondary {@link Action}.
	 *
	 * @param primaryAction
	 *            the primary action
	 * @param secondaryActions
	 *            one ore more secondary actions
	 */
	public SplitButton(Action primaryAction, Action... secondaryActions) {
		popupMenu = new JPopupMenu();
		popupMenu.add(primaryAction);
		for (Action action : secondaryActions) {
			popupMenu.add(action);
		}
		initSplitButton(primaryAction);
	}

	/**
	 * Creates a new {@link SplitButton} with the given primary {@link Action} and a custom
	 * {@link JPopupMenu}.
	 *
	 * @param primaryAction
	 *            the primary action
	 * @param popupMenu
	 *            the drop down menu
	 */
	public SplitButton(Action primaryAction, JPopupMenu popupMenu) {
		this.popupMenu = popupMenu;
		initSplitButton(primaryAction);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		primaryButton.setEnabled(enabled);
		dropDownButton.setEnabled(enabled);
	}

	public JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	/**
	 * @see AbstractButton#setHideActionText(boolean)
	 */
	public void SetHideActionText(boolean hideActionText) {
		primaryButton.setHideActionText(hideActionText);
	}

	private void initSplitButton(final Action primaryAction) {
		if (primaryAction == null) {
			throw new IllegalArgumentException("Primary action must not be null!");
		}
		if (popupMenu == null) {
			throw new IllegalArgumentException("Popup menu must not be null!");
		}

		popupMenu.addPopupMenuListener(new PopupMenuListener() {

			@Override
			public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {}

			@Override
			public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
				lastPopupCloseTime = System.currentTimeMillis();
			}

			@Override
			public void popupMenuCanceled(final PopupMenuEvent e) {}
		});

		setOpaque(false);

		// simple composition of two buttons
		primaryButton = new CompositeButton(primaryAction, SwingConstants.LEFT);
		dropDownButton = new CompositeButton(DROPDOWN_LABEL, SwingConstants.RIGHT);
		dropDownButton.setToolTipText(I18N.getGUIMessage("gui.split_button.drop_down.tip"));

		// buttons might differ in their height
		Dimension primaryButtonSize = primaryButton.getPreferredSize();
		Dimension dropDownButtonSize = dropDownButton.getPreferredSize();
		if (primaryButtonSize.height > dropDownButtonSize.height) {
			dropDownButtonSize.height = primaryButtonSize.height;
			dropDownButton.setPreferredSize(dropDownButtonSize);
		} else {
			primaryButtonSize.height = dropDownButtonSize.height;
			primaryButton.setPreferredSize(primaryButtonSize);
		}

		// align buttons left to right with no padding
		FlowLayout layout = new FlowLayout(SwingConstants.LEFT);
		layout.setHgap(0);
		setLayout(layout);

		add(primaryButton);
		add(dropDownButton);

		// display pop up menu below drop down button (if possible)
		dropDownButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// hack to prevent filter popup from opening itself again
				// when you click the button to actually close it while it
				// is open
				if (System.currentTimeMillis() - lastPopupCloseTime < 250) {
					return;
				}
				popupMenu.show(dropDownButton, 0, primaryButton.getHeight() - 1);
			}
		});
	}
}
