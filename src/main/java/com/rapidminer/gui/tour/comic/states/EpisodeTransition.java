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

/**
 * A transition determines which next {@link EpisodeState} will be set by the
 * {@link EpisodeStateMachine}.
 * 
 * @author Marcin Skirzynski
 * 
 */
public enum EpisodeTransition {

	/**
	 * Transition to the current state. Will call the {@link EpisodeState#onTransition(Episode)}
	 * method.
	 */
	CURRENT,

	/**
	 * Nothing will be done for this transition. No method/hook/trigger will be called either.
	 */
	IGNORE,

	/**
	 * The state will be set to the previous state in the episode.
	 */
	PREVIOUS,

	/**
	 * The state will be set to the next state in the episode.
	 */
	NEXT,

	/**
	 * The error state will be set which will show a popup if the user wants to undo the last steps
	 * which caused the error in tutorial.
	 */
	ERROR

}
