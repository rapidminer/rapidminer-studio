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

import com.rapidminer.Process;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tour.BubbleWindow;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;
import com.rapidminer.gui.tour.BubbleWindow.BubbleListener;
import com.rapidminer.gui.tour.DockableBubble;
import com.rapidminer.gui.tour.comic.ComicManager;
import com.rapidminer.gui.tour.comic.episodes.AbstractEpisode;
import com.rapidminer.gui.tour.comic.episodes.EpisodeEvent;
import com.rapidminer.tools.XMLException;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.SwingUtilities;


/**
 * The error state for a comic. Used to restore previous xml and state.
 * 
 * @author Marcin Skirzynski
 * 
 */
public class EpisodeErrorState implements EpisodeState {

	private final EpisodeStateMachine stateMachine;

	private String lastValidProcessState;

	private EpisodeState lastValidEpisodeState;

	private BubbleWindow bubble;

	public EpisodeErrorState(final EpisodeStateMachine stateMachine) {
		if (stateMachine == null) {
			throw new IllegalArgumentException("Cannot create an episode error state without a not-null state machine");
		}
		this.stateMachine = stateMachine;
	}

	@Override
	public void onTransition(final AbstractEpisode episode) {
		// kill old bubble
		if (bubble != null) {
			bubble.dispose();
			bubble = null;
		}
		// we don't care about this anymore here
		if (!episode.isEpisodeInProgress()) {
			return;
		}

		JButton revertButton = new JButton(new ResourceAction("comic.revert") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				Component source = (Component) e.getSource();
				Container parentContainer = SwingUtilities.getAncestorOfClass(BubbleWindow.class, source);
				if (parentContainer != null) {
					((BubbleWindow) parentContainer).killBubble(false);
				}
				resumeLastValidProcessState();
				resumeLastValidEpisodeState();
			}
		});

		bubble = new DockableBubble(RapidMinerGUI.getMainFrame(), AlignedSide.MIDDLE, "comic.revert", "process_panel",
				new JButton[] { revertButton }, new Object[0]);
		bubble.addBubbleListener(new BubbleListener() {

			@Override
			public void bubbleClosed(final BubbleWindow bw) {
				// x (close) button has been pressed, cancel comic
				ComicManager.getInstance().cancelCurrentEpisode();
			}

			@Override
			public void actionPerformed(final BubbleWindow bw) {}
		});
		bubble.setVisible(true);
	}

	@Override
	public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event, final Object object) {
		return EpisodeTransition.IGNORE;
	}

	/**
	 * Saves the current process and state to be able to go back to the last valid state.
	 */
	protected void saveProcessAndState() {
		setLastValidProcessState(RapidMinerGUI.getMainFrame().getProcess().getRootOperator().getXML(true));
		setLastValidEpisodeState(getEpisodeStateMachine().getCurrentState());
	}

	private String getLastValidProcessState() {
		return lastValidProcessState;
	}

	private void setLastValidProcessState(final String lastValidProcessState) {
		this.lastValidProcessState = lastValidProcessState;
	}

	private void resumeLastValidProcessState() {
		if (getLastValidProcessState() != null) {
			try {
				String processXML = getLastValidProcessState();
				Process currentProcess = RapidMinerGUI.getMainFrame().getProcess();
				Process process = new Process(processXML, currentProcess);
				RapidMinerGUI.getMainFrame().setProcess(process, false);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (XMLException e) {
				e.printStackTrace();
			}
		}
	}

	private EpisodeState getLastValidEpisodeState() {
		return lastValidEpisodeState;
	}

	private void setLastValidEpisodeState(final EpisodeState lastValidEpisodeState) {
		this.lastValidEpisodeState = lastValidEpisodeState;
	}

	protected void resumeLastValidEpisodeState() {
		if (getLastValidEpisodeState() != null) {
			getEpisodeStateMachine().getEpisode().cleanup();
			getEpisodeStateMachine().setCurrentState(getLastValidEpisodeState());
		}
	}

	/**
	 * Returns the state machine this error is associated to.
	 */
	private EpisodeStateMachine getEpisodeStateMachine() {
		return this.stateMachine;
	}

	@Override
	public boolean isRestorableState() {
		return false;
	}

}
