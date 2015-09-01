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
package com.rapidminer.gui.tour.comic.episodes;

import com.rapidminer.gui.tour.comic.states.EpisodeEventDelegator;
import com.rapidminer.gui.tour.comic.states.EpisodeState;
import com.rapidminer.gui.tour.comic.states.EpisodeStateMachine;


/**
 * Episode events which are fired by the {@link EpisodeEventDelegator} and can be reacted to in the
 * {@link EpisodeState}s via the
 * {@link EpisodeState#onEvent(EpisodeStateMachine, EpisodeEvent, Object)} method.
 * 
 * 
 * @author Marcin Skirzynski, Marco Boeck
 * 
 */
public enum EpisodeEvent {

	/** fired when dragging of an operator started */
	DRAG_OPERATOR_STARTED,

	/** fired when dragging of an operator ended */
	DRAG_ENDED,

	/** fired when dragging of a repository entry started */
	DRAG_REPOSITORY_STARTED,

	/** fired when the process has changed, i.e. a new process has been created */
	PROCESS_CHANGED,

	/** fired when something in the process has changed, e.g. a parameter has been edited */
	PROCESS_UPDATED,

	/** fired when a process started execution */
	PROCESS_STARTED,

	/** fired when a process stopped execution */
	PROCESS_ENDED,

	/** fired when the view on the process has changed, e.g. the user has entered a subprocess */
	PROCESS_VIEW_CHANGED,

	/** fired when an operator started execution */
	PROCESS_STARTED_OPERTOR,

	/** fired when an operator stopped execution */
	PROCESS_ENDED_OPERATOR,

	/** fired when an operator has been (de)selected */
	OPERATORS_SELECTED,

	/** fired when a dockable view state changed, e.g. the user closed a view */
	DOCKACKABLE_STATE_CHANGED,

	/**
	 * fired when the perspective changes, e.g. the user switches from the design to the results
	 * perspective
	 */
	PERSPECTIVE_CHANGED,

	/** fired when a bubble window is closed */
	BUBBLE_EVENT;

}
