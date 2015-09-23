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
package com.rapidminer.gui.tour.comic.gui.actions;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tour.comic.ComicManager;
import com.rapidminer.gui.tour.comic.episodes.AbstractEpisode;

import java.awt.event.ActionEvent;


/**
 * Stops the currently running {@link AbstractEpisode}.
 * 
 * @author Marco Boeck
 */
public class StopCurrentEpisodeAction extends ResourceAction {

	private static final long serialVersionUID = 5853959993751513209L;

	/**
	 * Creates a {@link StopCurrentEpisodeAction} instance.
	 */
	public StopCurrentEpisodeAction() {
		super(false, "comic.cancel_comic");
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		stopCurrentEpisode();
	}

	/**
	 * Cancels the current comic episode if one is running.
	 */
	public static void stopCurrentEpisode() {
		ComicManager manager = ComicManager.getInstance();
		if (manager.getCurrentEpisode() != null && manager.getCurrentEpisode().isEpisodeInProgress()) {
			manager.cancelCurrentEpisode();
		}
	}
}
