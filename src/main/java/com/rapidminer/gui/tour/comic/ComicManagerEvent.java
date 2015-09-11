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
package com.rapidminer.gui.tour.comic;

import com.rapidminer.gui.tour.comic.episodes.AbstractEpisode;


/**
 * Event types which can be fired by the {@link ComicManager}.
 * 
 * @author Marco Boeck
 * 
 */
public enum ComicManagerEvent {

	/** fired when a new comic has been added to the manager */
	COMIC_ADDED,

	/** fired when the current episode changes */
	EPISODE_CHANGED,

	/** fired when an episode is started */
	EPISODE_STARTED,

	/** fired when an episode finishes normally */
	EPISODE_FINISHED,

	/** fired when an episode is cancelled */
	EPISODE_CANCELED,

	/** fired when the progress through an episode changes */
	EPISODE_PROGRESS_UPDATE;

	/** the episode for which the event was triggered */
	private AbstractEpisode episode;

	/**
	 * Set the {@link AbstractEpisode} for which this event is.
	 * 
	 * @param episode
	 * @return this instance
	 */
	public ComicManagerEvent setEpisode(AbstractEpisode episode) {
		this.episode = episode;
		return this;
	}

	/**
	 * Return the {@link AbstractEpisode} for which this event is.
	 * 
	 * @return
	 */
	public AbstractEpisode getEpisode() {
		return episode;
	}
}
