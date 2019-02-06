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

import java.awt.Font;
import java.util.Enumeration;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.rapidminer.gui.tools.Ionicon;
import com.rapidminer.tools.I18N;


/**
 * A {@link CompositeToggleButton} that can be used to display {@link Action}s in a
 * {@link JPopupMenu}. The button itself will only be selected if one of the defined
 * {@link JMenuItem}s items will be selected.
 *
 * Whether the {@code CompositeButton} is the left-most, a center, or the right-most element of the
 * composition can be specified in the constructors via the Swing constants
 * {@link SwingConstants#LEFT}, {@link SwingConstants#CENTER}, and {@link SwingConstants#RIGHT}
 * respectively.
 *
 * @author Marcel Michel
 * @since 7.0.0
 */
class CompositeMenuToggleButton extends CompositeToggleButton {

	private static final long serialVersionUID = 1L;

	private static final String DOWN_ARROW_ADDER = "<html>%s<span style=\"color: 4F4F4F;\">" + Ionicon.ARROW_DOWN_B.getHtml()
			+ "</span></html>";

	private static final int POPUP_CLOSE_DELTA = 250;

	protected int arrowSize;

	protected String text;

	protected final JPopupMenu popupMenu;

	protected final ButtonGroup popupMenuGroup = new ButtonGroup();

	/** Remember the last time the popup was closed. */
	private long lastPopupCloseTime = 0;

	/**
	 * Creates a new {@code CompositeMenuToggleButton} with the given {@link Action} to be used at
	 * the given position.
	 *
	 * @param actions
	 *            the button actions
	 * @param position
	 *            the position in the composite element ({@link SwingConstants#LEFT},
	 *            {@link SwingConstants#CENTER}, or {@link SwingConstants#RIGHT})
	 */
	public CompositeMenuToggleButton(int position, Action... actions) {
		super(I18N.getGUILabel("workspace_more"), position);
		popupMenu = new JPopupMenu();
		addActions(actions);

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

		// display pop up menu below drop down button (if possible)
		addActionListener(e -> {
			setSelected(isPopupMenuItemSelected());
			// hack to prevent filter popup from opening itself again
			// when you click the button to actually close it while it
			// is open
			if (System.currentTimeMillis() - lastPopupCloseTime < POPUP_CLOSE_DELTA) {
				return;
			}
			popupMenu.show(CompositeMenuToggleButton.this, 0, getHeight() - 1);
		});
	}

	/**
	 * Adds the given {@link Action}s to the {@link #popupMenu}.
	 *
	 * @param actions
	 *            the actions which should be added to the menu
	 */
	public void addActions(Action... actions) {
		for (Action action : actions) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(action);

			item.addActionListener(e -> updateSelectionStatus());
			popupMenuGroup.add(item);
			popupMenu.add(item);
		}
	}

	/**
	 * Updates the selection status of the toggle button in regard to the menu selection.
	 */
	protected void updateSelectionStatus() {
		setSelected(isPopupMenuItemSelected());
	}

	/**
	 * Clears the selection of the button and also the toggle button.
	 */
	protected void clearMenuSelection() {
		popupMenuGroup.clearSelection();
		updateSelectionStatus();
	}

	/**
	 * Getter for the selection state of the menu.
	 *
	 * @return {@code true} if an item of the menu is selected otherwise {@code false}
	 */
	public boolean isPopupMenuItemSelected() {
		return popupMenuGroup.getSelection() != null;
	}

	/**
	 * Selects the given action in the menu. This methods also updates the selection status of the
	 * toggle button.
	 *
	 * @param action
	 *            the action which should be selected
	 */
	public void setSelected(Action action) {
		Enumeration<AbstractButton> menuEnum = popupMenuGroup.getElements();
		boolean found = false;
		while (menuEnum.hasMoreElements()) {
			AbstractButton button = menuEnum.nextElement();
			if (action == button.getAction()) {
				button.setSelected(true);
				found = true;
			} else {
				button.setSelected(false);
			}
		}
		if (found) {
			setFont(getFont().deriveFont(Font.BOLD));
			updateSelectionStatus();
		} else {
			setFont(getFont().deriveFont(Font.PLAIN));
			clearMenuSelection();
		}
	}

	@Override
	public void setText(String text) {
		this.text = text;
		if (text == null) {
			text = "";
		}
		if (!text.isEmpty()) {
			// add space between text and arrow
			text = text + "&#160 ";
		}
		super.setText(String.format(DOWN_ARROW_ADDER, text));
	}
}
