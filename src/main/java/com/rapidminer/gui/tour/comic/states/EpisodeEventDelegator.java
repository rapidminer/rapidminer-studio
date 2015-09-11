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
import com.rapidminer.ProcessListener;
import com.rapidminer.gui.Perspective;
import com.rapidminer.gui.PerspectiveChangeListener;
import com.rapidminer.gui.dnd.TransferableOperator;
import com.rapidminer.gui.processeditor.ExtendedProcessEditor;
import com.rapidminer.gui.tour.BubbleWindow;
import com.rapidminer.gui.tour.BubbleWindow.BubbleListener;
import com.rapidminer.gui.tour.comic.episodes.EpisodeEvent;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.RepositoryLocation;
import com.vlsolutions.swing.docking.event.DockableStateChangeEvent;
import com.vlsolutions.swing.docking.event.DockableStateChangeListener;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;


/**
 * Delegates events to the episode state machine.
 * 
 * @author Marcin Skirzynski
 * 
 */
public class EpisodeEventDelegator implements ExtendedProcessEditor, DockableStateChangeListener, PerspectiveChangeListener,
		BubbleListener {

	private final EpisodeStateMachine stateMachine;

	private Process process;

	private final ProcessListener processListener = new ProcessListener() {

		@Override
		public void processStarts(final Process process) {
			delegateEvent(EpisodeEvent.PROCESS_STARTED, process);
		}

		@Override
		public void processStartedOperator(final Process process, final Operator op) {
			delegateEvent(EpisodeEvent.PROCESS_STARTED_OPERTOR, process);
		}

		@Override
		public void processFinishedOperator(final Process process, final Operator op) {
			delegateEvent(EpisodeEvent.PROCESS_ENDED_OPERATOR, process);
		}

		@Override
		public void processEnded(final Process process) {
			delegateEvent(EpisodeEvent.PROCESS_ENDED, process);
		}
	};

	private volatile boolean enableDelegation;

	public EpisodeEventDelegator(final EpisodeStateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}

	/**
	 * Parses the drag event and delegates to the appropriate method (either
	 * {@link #dragStartedFromOperators(Operator)} or
	 * {@link #dragStartedFromRepository(RepositoryLocation)}).
	 */
	public void dragStarted(final Transferable t) {
		DataFlavor[] transferDataFlavors = t.getTransferDataFlavors();
		for (DataFlavor flavor : transferDataFlavors) {
			if (flavor == TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR) {
				try {

					// get repository location
					RepositoryLocation location = (RepositoryLocation) t.getTransferData(flavor);
					delegateEvent(EpisodeEvent.DRAG_REPOSITORY_STARTED, location);

				} catch (UnsupportedFlavorException e) {
				} catch (IOException e) {
				}
			}

			if (flavor == TransferableOperator.LOCAL_TRANSFERRED_OPERATORS_FLAVOR) {
				Operator[] operators;
				try {
					operators = (Operator[]) t.getTransferData(flavor);

					// we assume that only one operator can be dragged at a time
					Operator draggedOperator = operators[0];
					delegateEvent(EpisodeEvent.DRAG_OPERATOR_STARTED, draggedOperator);

				} catch (UnsupportedFlavorException e) {
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Delegates the event to the current state.
	 */
	public void dragEnded() {
		delegateEvent(EpisodeEvent.DRAG_ENDED, null);
	}

	@Override
	public void processChanged(final Process process) {
		initProcessListener(process);
		delegateEvent(EpisodeEvent.PROCESS_CHANGED, process);
	}

	@Override
	public void setSelection(final List<Operator> selection) {
		delegateEvent(EpisodeEvent.OPERATORS_SELECTED, selection);
	}

	@Override
	public void processUpdated(final Process process) {
		delegateEvent(EpisodeEvent.PROCESS_UPDATED, process);
	}

	@Override
	public void processViewChanged(final Process process) {
		delegateEvent(EpisodeEvent.PROCESS_VIEW_CHANGED, process);
	}

	@Override
	public void dockableStateChanged(final DockableStateChangeEvent event) {
		delegateEvent(EpisodeEvent.DOCKACKABLE_STATE_CHANGED, event);
	}

	@Override
	public void perspectiveChangedTo(final Perspective perspective) {
		delegateEvent(EpisodeEvent.PERSPECTIVE_CHANGED, perspective);
	}

	/**
	 * Inits the {@link ProcessListener} for this delegate.
	 * 
	 * @param process
	 */
	public void initProcessListener(final Process process) {
		removeProcessListener();
		this.process = process;
		if (this.process != null) {
			this.process.getRootOperator().addProcessListener(processListener);
		}
	}

	public void removeProcessListener() {
		if (this.process != null) {
			this.process.getRootOperator().removeProcessListener(processListener);
		}
	}

	/**
	 * Starts event delegation.
	 */
	public void startEventDelegation() {
		enableDelegation = true;
	}

	/**
	 * Stops event delegation, so events are no longer recieved by the {@link EpisodeStateMachine}.
	 */
	public void stopEventDelegation() {
		enableDelegation = false;
	}

	/**
	 * Delegates the event to the {@link EpisodeStateMachine}.
	 * 
	 * @param event
	 * @param object
	 */
	private void delegateEvent(EpisodeEvent event, Object object) {
		if (enableDelegation) {
			stateMachine.onEvent(event, object);
		}
	}

	@Override
	public void bubbleClosed(BubbleWindow bw) {
		delegateEvent(EpisodeEvent.BUBBLE_EVENT, bw);
	}

	@Override
	public void actionPerformed(BubbleWindow bw) {
		delegateEvent(EpisodeEvent.BUBBLE_EVENT, bw);
	}
}
