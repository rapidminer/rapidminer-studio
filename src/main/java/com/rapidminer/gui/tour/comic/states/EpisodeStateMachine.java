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

import com.rapidminer.gui.tour.comic.ComicManager;
import com.rapidminer.gui.tour.comic.episodes.AbstractEpisode;
import com.rapidminer.gui.tour.comic.episodes.EpisodeEvent;
import com.rapidminer.tools.AbstractObservable;

import java.util.ArrayList;
import java.util.List;


/**
 * The state machine which handles episode state transitions.
 * 
 * @author Marcin Skirzynski
 * 
 */
public class EpisodeStateMachine extends AbstractObservable<EpisodeTransition> {

	private final AbstractEpisode episode;

	private EpisodeErrorState errorState;

	private EpisodeState current;

	private final List<EpisodeState> states;

	public EpisodeStateMachine(final AbstractEpisode episode) {
		this.episode = episode;
		this.errorState = new EpisodeErrorState(this);
		this.states = new ArrayList<>();
	}

	protected void onEvent(final EpisodeEvent event, final Object object) {
		if (getCurrentState() == null) {
			// Ignore event if current state is null
			return;
		}
		if (!getEpisode().isEpisodeInProgress()) {
			// ignore if episode is not yet in progress
			return;
		}

		EpisodeTransition transition = getCurrentState().onEvent(this, event, object);
		handleTransition(transition, event, object);
	}

	/**
	 * Evaluating the given transition.
	 */
	private void handleTransition(final EpisodeTransition transition, final EpisodeEvent event, final Object object) {
		switch (transition) {
			case IGNORE:
				// Do nothing
				break;
			case CURRENT:
				// Will also call the onTransition method.
				setCurrentState(getCurrentState());
				break;
			case NEXT:
				setCurrentState(getNextEpisodeState(getCurrentState()));
				int numberOfStates = getEpisodeStates().size();
				int currentIndex = getEpisodeStates().indexOf(getCurrentState());
				int progress = numberOfStates == 0 ? 100 : ((currentIndex + 1) * 100) / numberOfStates;
				// we are only interested in forward progress
				ComicManager.getInstance().updateMaxProgressForEpisode(getEpisode(), progress);
				break;
			case PREVIOUS:
				setCurrentState(getPreviousEpisodeState(getCurrentState()));
				break;
			case ERROR:
				// Set the current state to error,
				// but do now save process and state.
				setCurrentState(getEpisodeErrorState());
				break;
			default:
				break;
		}

		// Save process and state for forthcoming errors.
		if (transition != EpisodeTransition.IGNORE && getCurrentState().isRestorableState()
				&& transition != EpisodeTransition.ERROR && getCurrentState() != getEpisodeErrorState()) {
			getEpisodeErrorState().saveProcessAndState();
		}
	}

	/**
	 * Returns the state before the given state if available.
	 * 
	 * @throws IllegalStateException
	 *             if there is no previous state.
	 */
	public EpisodeState getPreviousEpisodeState(final EpisodeState state) {
		int previousIndex = getEpisodeStates().indexOf(state) - 1;
		if (previousIndex < 0) {
			throw new IllegalStateException("Cannot do transition to previous state since there is no previous state");
		}
		return getEpisodeStates().get(previousIndex);
	}

	/**
	 * Returns the state after the given state if available.
	 * 
	 * @throws IllegalStateException
	 *             if there is no next state.
	 */
	public EpisodeState getNextEpisodeState(final EpisodeState state) {
		int nextIndex = getEpisodeStates().indexOf(state) + 1;
		if (nextIndex >= getEpisodeStates().size()) {
			throw new IllegalStateException("Cannot do transition to next state since there is no state left");
		}
		return getEpisodeStates().get(nextIndex);
	}

	public List<EpisodeState> getEpisodeStates() {
		return this.states;
	}

	public void addNextEpisodeState(final EpisodeState state) {
		getEpisodeStates().add(state);
	}

	/**
	 * Returns the episode to which this state machine belongs.
	 */
	public AbstractEpisode getEpisode() {
		return this.episode;
	}

	/**
	 * Returns the error state for this episode.
	 */
	private EpisodeErrorState getEpisodeErrorState() {
		return this.errorState;
	}

	/**
	 * Resets the error state.
	 */
	public void resetErrorState() {
		errorState = new EpisodeErrorState(this);
		errorState.saveProcessAndState();
	}

	/**
	 * Returns the current state.
	 */
	public EpisodeState getCurrentState() {
		return this.current;
	}

	/**
	 * Sets the current state and calls the state that an transition happened.
	 */
	public void setCurrentState(final EpisodeState state) {
		if (!getEpisodeStates().contains(state) && !getEpisodeErrorState().equals(state)) {
			throw new IllegalStateException("State is not part of the episode and cannot be set");
		}
		if (state != null) {
			state.onTransition(getEpisode());
		}
		this.current = state;
	}

}
