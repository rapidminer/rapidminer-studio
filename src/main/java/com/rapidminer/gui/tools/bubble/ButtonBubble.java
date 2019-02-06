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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JButton;


/**
 * This class creates a speech bubble-shaped JDialog, which can is attached to Buttons by using its
 * ID. The bubble triggers two events which are obserable by the {@link BubbleListener}; either if
 * the close button was clicked, or if the corresponding button was used. The keys for the title and
 * the text must be of format gui.bubble.xxx.body or gui.bubble.xxx.title .
 * 
 * @author Thilo Kamradt
 * 
 */

public class ButtonBubble extends BubbleWindow {

	private static final long serialVersionUID = 8601169454504964237L;

	private ActionListener buttonListener;
	private AbstractButton button = null;
	private String buttonKey;

	private boolean addListener;

	/**
	 * Creates a Bubble which points to a Button.This Bubble will listen to the given Button and
	 * calls {@link BubbleListener}.actionPerformed() if the Button is pressed. Also this Bubble
	 * only will be viewable in one Perspective.
	 * 
	 * @param owner
	 *            the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable
	 * @param i18nKey
	 *            of the message which should be shown
	 * @param buttonKeyToAttach
	 *            i18nKey of the Button to which this {@link BubbleWindow} should be placed relative
	 *            to
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public ButtonBubble(Window owner, String nextDockableKey, AlignedSide preferredAlignment, String i18nKey,
			String buttonKeyToAttach, Object... arguments) {
		this(owner, nextDockableKey, preferredAlignment, i18nKey, buttonKeyToAttach, true, arguments);
	}

	/**
	 * Creates a Bubble which points to a Button. This Bubble only will be viewable in one
	 * Perspective.
	 * 
	 * @param owner
	 *            the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable
	 * @param i18nKey
	 *            of the message which should be shown
	 * @param buttonKeyToAttach
	 *            i18nKey of the Button to which this {@link BubbleWindow} should be placed relative
	 *            to
	 * @param addListener
	 *            indicates whether the {@link BubbleWindow} closes if the Button was pressed or
	 *            when another Listener added by a subclass of {@link Step} is fired.
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public ButtonBubble(Window owner, String nextDockableKey, AlignedSide preferredAlignment, String i18nKey,
			String buttonKeyToAttach, boolean addListener, Object... arguments) {
		this(owner, nextDockableKey, preferredAlignment, i18nKey, buttonKeyToAttach, addListener, true, arguments);
	}

	/**
	 * Creates a Bubble which points to a Button.
	 * 
	 * @param owner
	 *            the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable
	 * @param i18nKey
	 *            of the message which should be shown
	 * @param buttonKeyToAttach
	 *            i18nKey of the Button to which this {@link BubbleWindow} should be attached
	 * @param addListener
	 *            indicates whether the {@link BubbleWindow} closes if the Button was pressed or
	 *            when another Listener added by a subclass of {@link Step} is fired.
	 * @param listenToPerspective
	 *            if true the {@link BubbleWindow} is only in one Perspective viewable
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public ButtonBubble(Window owner, String nextDockableKey, AlignedSide preferredAlignment, String i18nKey,
			String buttonKeyToAttach, boolean addListener, boolean listenToPerspective, Object... arguments) {
		this(owner, nextDockableKey, preferredAlignment, i18nKey, buttonKeyToAttach, addListener, listenToPerspective, null,
				arguments);
	}

	/**
	 * Creates a Bubble which points to a Button.
	 * 
	 * @param owner
	 *            the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable
	 * @param i18nKey
	 *            of the message which should be shown
	 * @param buttonKeyToAttach
	 *            i18nKey of the Button to which this {@link BubbleWindow} should be attached
	 * @param addListener
	 *            indicates whether the {@link BubbleWindow} closes if the Button was pressed or
	 *            when another Listener added by a subclass of {@link Step} is fired.
	 * @param listenToPerspective
	 *            if true the {@link BubbleWindow} is only in one Perspective viewable
	 * @param buttonsToAdd
	 *            array of JButton's which will be added to the Bubble (null instead of the array
	 *            won't throw an error).
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public ButtonBubble(Window owner, String nextDockableKey, AlignedSide preferredAlignment, String i18nKey,
			String buttonKeyToAttach, boolean addListener, boolean listenToPerspective, JButton[] buttonsToAdd,
			Object... arguments) {
		super(owner, preferredAlignment, i18nKey, nextDockableKey, buttonsToAdd, arguments);
		if (preferredAlignment != AlignedSide.MIDDLE) {
			// Bubble will be bind to a Component
			this.buttonKey = buttonKeyToAttach;
			this.addListener = addListener;
			if (buttonKey == null || buttonKey.equals("")) {
				throw new IllegalArgumentException("key of the Button can not be null if the Alignment is not MIDDLE");
			} else {
				this.button = BubbleWindow.findButton(buttonKey, owner);
			}
		}
		setAddPerspectiveListener(listenToPerspective);
		super.paint(false);
	}

	@Override
	protected Point getObjectLocation() {
		return button.getLocationOnScreen();
	}

	@Override
	protected int getObjectWidth() {
		return button.getWidth();
	}

	@Override
	protected int getObjectHeight() {
		return button.getHeight();
	}

	@Override
	protected void unregisterSpecificListeners() {
		if (addListener) {
			button.removeActionListener(buttonListener);
		}
	}

	@Override
	protected void registerSpecificListener() {
		if (addListener) {
			buttonListener = new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					fireEventActionPerformed();
				}
			};
			button.addActionListener(buttonListener);
		}
	}

}
