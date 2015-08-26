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
package com.rapidminer.gui.tour.comic.gui;

import com.rapidminer.gui.tour.comic.ComicManager;
import com.rapidminer.gui.tour.comic.episodes.AbstractEpisode;

import javax.swing.DefaultListModel;
import javax.swing.ListModel;


/**
 * The {@link ListModel} for the {@link ComicDialog} list of comics.
 * 
 * @author Marco Boeck
 * 
 */
public class ComicListModel extends DefaultListModel<AbstractEpisode> {

	private static final long serialVersionUID = 1L;

	@Override
	public int getSize() {
		return ComicManager.getInstance().getNumberOfEpisodes();
	}

	@Override
	public AbstractEpisode getElementAt(final int index) {
		return ComicManager.getInstance().getEpisodeByIndex(index);
	}

	/**
	 * Notify the model that this episode has been added.
	 * 
	 * @param episode
	 */
	public void fireEpisodeAdded(AbstractEpisode episode) {
		super.fireIntervalAdded(this, ComicManager.getInstance().indexOf(episode),
				ComicManager.getInstance().indexOf(episode));
	}

}
