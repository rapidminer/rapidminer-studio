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

import java.awt.Window;

import com.rapidminer.Process;
import com.rapidminer.ProcessStorageListener;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;


/**
 * This subclass of {@link Step} will open a {@link BubbleWindow} which closes if the user has
 * opened a process.
 * 
 * @author Philipp Kersting and Thilo Kamradt
 * 
 */

public class OpenProcessStep extends Step {

	private AlignedSide alignment;
	private Window owner = RapidMinerGUI.getMainFrame();
	private String i18nKey;
	private String attachToKey;
	private ProcessStorageListener listener = null;

	/**
	 * 
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which will be shown in the {@link BubbleWindow}.
	 * @param buttonKey
	 *            i18nKey of the Button to which the {@link BubbleWindow} should point to.
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public OpenProcessStep(AlignedSide preferredAlignment, String i18nKey, String buttonKey, Object... arguments) {
		this.alignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.attachToKey = buttonKey;
		this.arguments = arguments;
	}

	@Override
	boolean createBubble() {
		if (attachToKey == null) {
			throw new IllegalArgumentException("no component to attach !");
		}
		bubble = new ButtonBubble(owner, null, alignment, i18nKey, attachToKey, false, false, arguments);
		listener = new ProcessStorageListener() {

			@Override
			public void stored(Process process) {

			}

			@Override
			public void opened(Process process) {
				bubble.triggerFire();
				RapidMinerGUI.getMainFrame().removeProcessStorageListener(listener);
			}
		};
		RapidMinerGUI.getMainFrame().addProcessStorageListener(listener);
		return true;
	}

	@Override
	protected void stepCanceled() {
		if (listener != null) {
			RapidMinerGUI.getMainFrame().removeProcessStorageListener(listener);
		}
	}

	@Override
	public Step[] getPreconditions() {
		return new Step[] {};
	}
}
