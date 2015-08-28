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

import com.rapidminer.Process;
import com.rapidminer.gui.Perspective;
import com.rapidminer.gui.Perspectives;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tour.BubbleWindow;
import com.rapidminer.gui.tour.comic.ComicManager;
import com.rapidminer.gui.tour.comic.states.EpisodeErrorState;
import com.rapidminer.gui.tour.comic.states.EpisodeEventDelegator;
import com.rapidminer.gui.tour.comic.states.EpisodePanel;
import com.rapidminer.gui.tour.comic.states.EpisodeState;
import com.rapidminer.gui.tour.comic.states.EpisodeStateMachine;
import com.rapidminer.gui.tour.comic.states.EpisodeTransition;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;

import java.awt.Image;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.ImageIcon;


/**
 * 
 * Base class for every comic tutorial episode. Provides renderer, delegator and state machines.
 * <p>
 * Extend this class to create a comic tutorial episode. An episode usually consists of a number of
 * {@link EpisodePanel}s, which each represent a state in the comic tutorial.
 * <p>
 * State transition is managed by a {@link EpisodeStateMachine}, which in turn transitions according
 * to the {@link EpisodeTransition}s specified in each {@link EpisodePanel} in the
 * {@link EpisodeState#onEvent(EpisodeStateMachine, com.rapidminer.gui.tour.comic.EpisodeEvent, Object)}
 * method.
 * <p>
 * These states then have to be added to the state machine (see {@link #getEpisodeStateMachine()})
 * in the order they should appear in the comic tutorial. Finally, in the {@link #initState()}
 * method the first state needs to be set via
 * {@link EpisodeStateMachine#setCurrentState(EpisodeState)}.
 * 
 * @author Marcin Skirzynski, Marco Boeck
 * 
 */
public abstract class AbstractEpisode {

	/**
	 * Enum for the starting {@link Perspective} of a comic tutorial episode.
	 */
	public enum StartingPerspective {
		DESIGN, RESULT, BUSINESS, WELCOME;
	}

	/**
	 * Delegator for all events.
	 */
	private final EpisodeEventDelegator eventDelegator;

	/**
	 * State machine for the current episode.
	 */
	private final EpisodeStateMachine stateMachine;

	/** indicates if the episode is in progress */
	private boolean inProgress;

	/**
	 * The comic images which have to be displayed.
	 */
	private final List<Image> listOfImages;

	/** the maximum achieved progress in %, i.e. value [0, 100] for this episode */
	private int maxAchievedProgress;

	/** the {@link Process} which has been built during the episode */
	private String builtProcessXML;

	/** the previous Episode for this episode */
	private AbstractEpisode previousEpisode;

	/** the starting perspective for this episode */
	private final StartingPerspective startingPerspective;

	/**
	 * Creates an episode skeleton which subclasses can use to implement a comic tutorial episode.
	 * 
	 * @param startingPerspective
	 *            the starting perspective for this episode.
	 */
	public AbstractEpisode(final StartingPerspective startingPerspective) {
		this.stateMachine = new EpisodeStateMachine(this);
		this.eventDelegator = new EpisodeEventDelegator(this.stateMachine);
		this.maxAchievedProgress = 0;
		this.listOfImages = new LinkedList<>();
		this.startingPerspective = startingPerspective;
	}

	/**
	 * Starts and displays the current episode. <br/>
	 * Subclasses needing additional steps when starting an episode can add them here but must still
	 * call this method afterwards.
	 */
	public synchronized void startEpisode() {
		inProgress = true;
		switchToStartingPerspective();
		eventDelegator.initProcessListener(RapidMinerGUI.getMainFrame().getProcess());
		initState();
		stateMachine.resetErrorState();
		eventDelegator.startEventDelegation();
	}

	/**
	 * Loads the episode data, namely the images of each {@link EpisodeState}. <br/>
	 * Subclasses needing additional preparations can add it here but must still call this method
	 * afterwards.
	 * 
	 * @throws IOException
	 */
	public void load() throws IOException {
		for (EpisodeState state : getEpisodeStateMachine().getEpisodeStates()) {
			if (state instanceof EpisodePanel) {
				((EpisodePanel) state).loadImage();
			}
		}
	}

	/**
	 * Returns the {@link EpisodeEventDelegator} which is in charge of collecting and delegating all
	 * events.
	 */
	public EpisodeEventDelegator getEpisodeEventDelegator() {
		return this.eventDelegator;
	}

	/**
	 * Returns the {@link EpisodeStateMachine} which is handling the state and transitions for the
	 * episode.
	 */
	public EpisodeStateMachine getEpisodeStateMachine() {
		return this.stateMachine;
	}

	/**
	 * Returns the current process.
	 */
	protected Process getProcess() {
		return RapidMinerGUI.getMainFrame().getProcess();
	}

	/**
	 * Returns all operators withing the current process.
	 */
	public Collection<Operator> getAllOperatorsInCurrentProcess() {
		return getProcess().getRootOperator().getAllInnerOperators();
	}

	/**
	 * Returns the number of images for this episode.
	 * 
	 * @return
	 */
	public int getNumberOfImages() {
		return listOfImages.size();
	}

	/**
	 * Sets the images which will be rendered by the comic renderer. The number of images must be
	 * the same as split panels are shown in the process view, otherwise the images will not be
	 * shown. Example: To show two images (one on each side, from left to right) inside of an
	 * x-validation, the corresponding {@link EpisodePanel} must provide two images.
	 * 
	 * @param images
	 */
	public void setImages(final Image... images) {
		if (images == null) {
			throw new IllegalArgumentException("images must not be null!");
		}

		listOfImages.clear();
		for (Image image : images) {
			if (image != null) {
				this.listOfImages.add(image);
			}
		}
	}

	/**
	 * Returns the image for the given index which will be rendered by the comic renderer in a
	 * splitted panel.
	 * 
	 * @return
	 * @throws IndexOutOfBoundsException
	 *             if the index is < 0 or >= {@link #getNumberOfImages()}.
	 */
	public Image getImage(final int index) throws IndexOutOfBoundsException {
		return listOfImages.get(index);
	}

	/**
	 * Finishes the current episode by setting it to not in progress and removing the process
	 * listener from the event delegator. <br/>
	 * Subclasses needing additional cleanup can add their own cleanup here but must still call this
	 * method afterwards.
	 */
	public synchronized void finishEpisode() {
		builtProcessXML = getProcess().getRootOperator().getXML(true);
		inProgress = false;
		eventDelegator.removeProcessListener();
		eventDelegator.stopEventDelegation();
	}

	/**
	 * Returns <code>true</code> if the {@link AbstractEpisode} is in progress; <code>false</code>
	 * otherwise.
	 * 
	 * @return
	 */
	public final boolean isEpisodeInProgress() {
		return inProgress;
	}

	/**
	 * Return the title of the {@link AbstractEpisode}.
	 * <p>
	 * Looks in the gui.properties for the key "gui.comic." + {@link #getI18NKey()} + ".title".
	 * 
	 * @return
	 */
	public String getTitle() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.comic." + getI18NKey() + ".title");
	}

	/**
	 * Return the goals of the {@link AbstractEpisode}. Goals are short bulletin points of what the
	 * episode should teach the user. <br/>
	 * Each goal should consist of max. 2 words and each goal should be separated with an
	 * <code>;</code> <br/>
	 * Example: <code>connect operators;run process</code>
	 * <p>
	 * Looks in the gui.properties for the key "gui.comic." + {@link #getI18NKey()} + ".goals".
	 * 
	 * @return
	 */
	public String getGoals() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.comic." + getI18NKey() + ".goals");
	}

	/**
	 * Return the component name this {@link AbstractEpisode} is a tutorial for, e.g. '
	 * <code>RapidMiner Studio</code>' for RM core comics.
	 * <p>
	 * Looks in the gui.properties for the key "gui.comic." + {@link #getI18NKey()} + ".component".
	 * 
	 * @return
	 */
	public String getComponentName() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.comic." + getI18NKey() + ".component");
	}

	/**
	 * Return a description text of the {@link AbstractEpisode}.
	 * <p>
	 * Looks in the gui.properties for the key "gui.comic." + {@link #getI18NKey()} +
	 * ".description".
	 * 
	 * @return
	 */
	public String getDescription() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.comic." + getI18NKey() + ".description");
	}

	/**
	 * Return the preview icon for this {@link AbstractEpisode} or <code>null</code>. The preview
	 * icon is displayed along with the title and the description of the episode.
	 * 
	 * @return
	 */
	public ImageIcon getPreviewIcon() {
		return getPreviewImageIcon();
	}

	/**
	 * Gets the maximum achieved progress, which is between [0, 100]. Progress is remembered in a
	 * properties file by the {@link ComicManager}.
	 * 
	 * @return
	 */
	public final int getMaxAchievedProgress() {
		return maxAchievedProgress;
	}

	/**
	 * Gets the {@link Process} the user built during the episode. If the episode has not yet been
	 * finished once, returns <code>null</code>.
	 * 
	 * @return
	 */
	public final Process getBuiltProcessOrNull() {
		Process builtProcess = null;
		if (builtProcessXML != null) {
			try {
				builtProcess = new Process(builtProcessXML);
			} catch (IOException | XMLException e) {
				LogService.getRoot().log(Level.INFO,
						"com.rapidminer.gui.tour.comic.episodes.AbstractEpisode.failed_to_get_built_process");
			}
		}

		return builtProcess;
	}

	/**
	 * Sets the maximum achieved progress. If the given progress is lower than the currently set
	 * one, ignores the new value. Progress must be between [0, 100].
	 * 
	 * @param maxAchievedProgress
	 * @throws IllegalArgumentException
	 *             if the maxAchievedProgress value is illegal
	 */
	public final void setMaxAchievedProgress(final int maxAchievedProgress) throws IllegalArgumentException {
		if (maxAchievedProgress < 0 || maxAchievedProgress > 100) {
			throw new IllegalArgumentException("maxAchievedProgress must not be < 0 or > 100!");
		}

		if (maxAchievedProgress > this.maxAchievedProgress) {
			this.maxAchievedProgress = maxAchievedProgress;
		}
	}

	/**
	 * Returns the previous {@link AbstractEpisode} for this episode (if one has been set).
	 */
	public final AbstractEpisode getPreviousEpisode() {
		return previousEpisode;
	}

	/**
	 * Sets the pprevious {@link AbstractEpisode} for this episode.
	 * 
	 * @param previousEpisode
	 */
	public final void setPreviousEpisode(final AbstractEpisode previousEpisode) {
		this.previousEpisode = previousEpisode;
	}

	@Override
	public String toString() {
		return getTitle();
	}

	/**
	 * Switches to the desired starting perspective for the episode. If the starting perspective was
	 * set to <code>null</code>, does nothing. Called after {@link #initState()}.
	 */
	private void switchToStartingPerspective() {
		if (startingPerspective == null) {
			return;
		}

		switch (startingPerspective) {
			case WELCOME:
				RapidMinerGUI.getMainFrame().getPerspectives().showPerspective(Perspectives.WELCOME);
				break;
			case BUSINESS:
				RapidMinerGUI.getMainFrame().getPerspectives().showPerspective(Perspectives.BUSINESS);
				break;
			case RESULT:
				RapidMinerGUI.getMainFrame().getPerspectives().showPerspective(Perspectives.RESULT);
				break;
			case DESIGN:
			default:
				RapidMinerGUI.getMainFrame().getPerspectives().showPerspective(Perspectives.DESIGN);
				break;
		}
	}

	/**
	 * Returns the {@link I18N} key.
	 * 
	 * @return
	 */
	public abstract String getI18NKey();

	/**
	 * Get the preview icon for this episode or <code>null</code>.
	 * 
	 * @return
	 */
	protected abstract ImageIcon getPreviewImageIcon();

	/**
	 * Returns the default process xml. Will be used as a starting point for this episode. If this
	 * parameter is <code>null</code>, will start with an empty process.
	 * 
	 * @return
	 */
	public abstract String getDefaultProcessXML();

	/**
	 * Inits the first state of the {@link AbstractEpisode}. The implementation must tell the
	 * {@link EpisodeStateMachine} which state is the first by calling
	 * {@link EpisodeStateMachine#setCurrentState(EpisodeState)}. <br/>
	 * This method is automatically invoked during {@link #startEpisode()}.
	 */
	public abstract void initState();

	/**
	 * Called when the user clicks 'Try again' after an {@link EpisodeErrorState} has been achieved.
	 * Cleanup during an episode needs to go here, for example removing leftover
	 * {@link BubbleWindow}s.
	 */
	public abstract void cleanup();

}
