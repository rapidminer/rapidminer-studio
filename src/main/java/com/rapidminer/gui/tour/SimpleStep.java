/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tour;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;

import java.awt.Window;


/**
 * This Subclass of {@link Step} will open a {@link BubbleWindow} which closes if the given Button
 * was pressed.
 * 
 * @author Philipp Kersting and Thilo Kamradt
 * 
 */

public class SimpleStep extends Step {

	private String buttonKey;
	private String i18nKey;
	private AlignedSide alignment;
	private Window owner;
	private boolean isInMenuBar;

	/**
	 * This Steps creates a Bubble pointing to the Button with the given key which disposes in the
	 * moment the Button will be pressed. The owner is is the Mainframe by default.
	 * 
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which will be shown in the {@link BubbleWindow}.
	 * @param buttonKey
	 *            i18nKey of the Button to which the {@link Step} listens and the
	 *            {@link BubbleWindow} will point to.
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public SimpleStep(AlignedSide preferredAlignment, String i18nKey, String buttonKey, boolean isInMenuBar,
			Object... arguments) {
		this(preferredAlignment, i18nKey, buttonKey, isInMenuBar, RapidMinerGUI.getMainFrame(), arguments);
	}

	/**
	 * This Steps creates a Bubble pointing to the Button with the given key which disposes in the
	 * moment the Button will be pressed. The owner is is the Mainframe by default.
	 * 
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which will be shown in the {@link BubbleWindow}.
	 * @param buttonKey
	 *            i18nKey of the Button to which the {@link Step} listens and the
	 *            {@link BubbleWindow} will point to.
	 * @param owner
	 *            sets the owner of the Bubble from Mainframe to given owner
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public SimpleStep(AlignedSide preferredAlignment, String i18nKey, String buttonKey, boolean isInMenuBar, Window owner,
			Object... arguments) {
		super();
		this.alignment = preferredAlignment;
		this.owner = owner;
		this.i18nKey = i18nKey;
		this.buttonKey = buttonKey;
		this.isInMenuBar = isInMenuBar;
		this.arguments = arguments;
	}

	@Override
	boolean createBubble() {
		bubble = new ButtonBubble(owner, null, alignment, i18nKey, buttonKey, true, !isInMenuBar, arguments);
		return true;
	}

	@Override
	protected void stepCanceled() {
		// the BubbleWindow will do everything what is necessary
	}

	@Override
	public Step[] getPreconditions() {
		return new Step[] {};
	}

}
