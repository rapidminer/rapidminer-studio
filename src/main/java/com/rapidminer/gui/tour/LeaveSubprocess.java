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
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;


/**
 * @author kamradt
 * 
 */
public class LeaveSubprocess extends Step {

	private final AlignedSide preferredAlignment;
	private final String i18nKey;

	/**
	 * creates a step which points to the arrow to leave a subprocess
	 * 
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which will be shown in the {@link BubbleWindow}.
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	public LeaveSubprocess(final AlignedSide preferredAlignment, final String i18nKey, final Object... arguments) {
		super();
		this.preferredAlignment = preferredAlignment;
		this.i18nKey = i18nKey;
		this.arguments = arguments;
	}

	@Override
	boolean createBubble() {
		if (!"design".equals(RapidMinerGUI.getMainFrame().getPerspectives().getCurrentPerspective().getName())) {
			return false;
		}
		bubble = new ButtonBubble(RapidMinerGUI.getMainFrame(), ProcessPanel.PROCESS_PANEL_DOCK_KEY, preferredAlignment,
				i18nKey, "select_parent", arguments);
		return true;
	}

	@Override
	protected void stepCanceled() {
		// no need to do anything
	}

	@Override
	public Step[] getPreconditions() {
		return new Step[] { new PerspectivesStep(1), new NotShowingStep(ProcessPanel.PROCESS_PANEL_DOCK_KEY) };
	}

}
