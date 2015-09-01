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
package com.rapidminer.gui.tour.comic.states;

import com.rapidminer.gui.tour.comic.episodes.AbstractEpisode;
import com.rapidminer.gui.tour.comic.episodes.EpisodeEvent;


/**
 * Interface for comic tutorial episodes. Consists of a method which gets called when the state
 * becomes active in the {@link EpisodeStateMachine} and a method which is called whenenver one of
 * the {@link EpisodeEvent}s is triggered.
 * 
 * 
 * @author Marcin Skirzynski, Marco Boeck
 * 
 */
public interface EpisodeState {

	/**
	 * Will be called if a transition into <code>this</code> state happened.
	 * 
	 * @param episode
	 *            the episode this state is part of
	 */
	public void onTransition(AbstractEpisode episode);

	/**
	 * Will be called on any event which occurs.
	 * 
	 * @param episodeStateMachine
	 *            the {@link EpisodeStateMachine}
	 * @param event
	 *            the type of the event
	 * @param object
	 *            the payload
	 * 
	 * @return the {@link EpisodeTransition} which has to be determined by the comic tutorial
	 *         episode implementations.
	 */
	public EpisodeTransition onEvent(EpisodeStateMachine episodeStateMachine, EpisodeEvent event, Object object);

	/**
	 * Returns a flag indicating if this is an episode state which can be used to go back to it in
	 * case an error happens in the next step. This is useful if this is an intermediate step, e.g.
	 * one which is only shown while the user drags an operator. If this returns <code>false</code>,
	 * the {@link EpisodeStateMachine} will not save it as a state to go back to.
	 * 
	 * @return
	 */
	public boolean isRestorableState();

}
