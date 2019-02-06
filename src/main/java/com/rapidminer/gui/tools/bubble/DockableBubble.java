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
package com.rapidminer.gui.tools.bubble;

import java.awt.Point;
import java.awt.Window;

import javax.swing.JButton;


/**
 * This class creates a speech bubble-shaped JDialog, which can be attached to Dockables by using
 * their ID. The bubble triggers two events which are observable by the {@link BubbleListener};
 * either if the close button was clicked, or if the corresponding button was used. The keys for the
 * title and the text must be of format gui.bubble.xxx.body or gui.bubble.xxx.title.
 * 
 * @author Thilo Kamradt
 * 
 */

public class DockableBubble extends BubbleWindow {

	private static final long serialVersionUID = 3888050226315317727L;

	/**
	 * Creates a Bubble which points to the Dockable with the given key
	 * 
	 * @param owner
	 *            the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which should be shown
	 * @param docKey
	 *            key of the Dockable to which this {@link BubbleWindow} should be placed relative
	 *            to.
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public DockableBubble(final Window owner, final AlignedSide preferredAlignment, final String i18nKey,
			final String docKey, final Object... arguments) {
		this(owner, preferredAlignment, i18nKey, docKey, null, arguments);
	}

	/**
	 * Creates a Bubble which points to the Dockable with the given key
	 * 
	 * @param owner
	 *            the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which should be shown
	 * @param docKey
	 *            key of the Dockable to which this {@link BubbleWindow} should be placed relative
	 *            to.
	 * @param buttonsToAdd
	 *            array of JButtons which will be added to the Bubble (null instead of the array
	 *            won't throw an error).
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public DockableBubble(final Window owner, final AlignedSide preferredAlignment, final String i18nKey,
			final String docKey, final JButton[] buttonsToAdd, final Object... arguments) {
		super(owner, preferredAlignment, i18nKey, docKey, buttonsToAdd, arguments);
		if (preferredAlignment != AlignedSide.MIDDLE) {
			if (docKey == null) {
				throw new IllegalArgumentException("key of Dockable can not be null if Alignment is not MIDDLE");
			}
		}
		super.paint(false);
	}

	@Override
	protected void registerSpecificListener() {
		// no Listeners necessary
	}

	@Override
	protected void unregisterSpecificListeners() {
		// no Listeners necessary
	}

	@Override
	protected Point getObjectLocation() {
		if (getDockable().getComponent().isShowing()) {
			return getDockable().getComponent().getLocationOnScreen();
		} else if (getDockingDesktop().getDockableState(getDockable()) == null
				|| getDockingDesktop().getDockableState(getDockable()).isHidden()) {
			return HIDDEN_POS;
		} else if (!getDockable().getComponent().isShowing()) {
			return HIDDEN_POS;
		} else {
			return getDockable().getComponent().getParent().getParent().getLocationOnScreen();
		}
	}

	@Override
	protected int getObjectWidth() {
		if (getDockable() != null) {
			if (getDockable().getComponent().isShowing()) {
				return getDockable().getComponent().getWidth();
			}
			if (getDockingDesktop().getDockableState(getDockable()) == null
					|| getDockingDesktop().getDockableState(getDockable()).isHidden()) {
				return HIDDEN_WIDTH;
			} else {
				return getDockable().getComponent().getWidth();
			}
		} else {
			return 0;
		}
	}

	@Override
	protected int getObjectHeight() {
		if (getDockable() != null) {
			if (getDockable().getComponent().isShowing()) {
				return getDockable().getComponent().getHeight();
			}
			if (getDockingDesktop().getDockableState(getDockable()) == null
					|| getDockingDesktop().getDockableState(getDockable()).isHidden()) {
				return HIDDEN_HEIGHT;
			} else {
				return getDockable().getComponent().getHeight();
			}
		} else {
			return 0;
		}
	}

}
