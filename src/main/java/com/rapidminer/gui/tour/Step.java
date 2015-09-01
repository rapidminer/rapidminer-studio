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
package com.rapidminer.gui.tour;

import com.rapidminer.gui.tour.BubbleWindow.BubbleListener;
import com.rapidminer.gui.tour.IntroductoryTour.TourListener;

import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;


/**
 * A step consisting of a {@link BubbleWindow} and a follower. This class must be inherited by other
 * steps, which define what action has to be perform to succeed to the next step.
 * 
 * @author Philipp Kersting and Thilo Kamradt
 * 
 */
public abstract class Step {

	public enum BubbleType {
		OPERATOR, BUTTON, DOCKABLE
	}

	protected Step next;
	protected BubbleWindow bubble;
	protected String tourkey;
	protected boolean finalStep;
	protected LinkedList<TourListener> listeners = new LinkedList<>();
	protected int index;
	protected int length;
	protected Object[] arguments;

	/**
	 * builds the Bubble and adds the listeners to notify that step is passed.
	 * 
	 * @return true if step can be started and false if the step should be skipped
	 */
	abstract boolean createBubble();

	/**
	 * Method to get the next {@link Step}.
	 * 
	 * @return the next {@link Step} which will be performed.
	 */
	public Step getNext() {
		return this.next;
	}

	/**
	 * Sets the next {@link Step}
	 * 
	 * @param next
	 *            the next {@link Step} which should be performed after this {@link Step}
	 */
	public void setNext(final Step next) {
		this.next = next;
	}

	/**
	 * This method will start the {@link Step} and calls start() on the next Step if it is
	 * available.
	 */
	public void start() {
		boolean showMe = createBubble();
		if (!showMe) {
			if (getNext() != null) {
				getNext().start();
			} else {
				this.writeStateToFile();
				this.notifyTourListeners();
			}
		} else {
			bubble.addBubbleListener(new BubbleListener() {

				@Override
				public void bubbleClosed(final BubbleWindow bw) {
					bw.removeBubbleListener(this);
					stepCanceled();
					Step.this.writeStateToFile();
					Step.this.notifyTourListeners();
				}

				@Override
				public void actionPerformed(final BubbleWindow bw) {
					if (getNext() != null) {
						new Thread() {

							@Override
							public void run() {
								SwingUtilities.invokeLater(new Runnable() {

									@Override
									public void run() {
										getNext().start();
									}
								});
							};
						}.start();
					} else {
						Step.this.writeStateToFile();
						Step.this.notifyTourListeners();
					}
					bw.removeBubbleListener(this);
				}
			});
			bubble.setVisible(true);
		}
	}

	/**
	 * This method will add a {@link BubbleListener} to the {@link BubbleWindow} of this
	 * {@link Step}
	 * 
	 * @param l
	 *            the {@link BubbleListener} which should be added to the {@link BubbleWindow}
	 */
	public void addBubbleListener(final BubbleListener l) {
		bubble.addBubbleListener(l);
	}

	/**
	 * removes the given {@link BubbleListener} from the {@link BubbleWindow} of this {@link Step}.
	 * 
	 * @param l
	 */
	public void removeBubbleListener(final BubbleListener l) {
		bubble.removeBubbleListener(l);
	}

	/**
	 * places the needed parameters of the {@link Step} should be called by {@link IntroductoryTour}
	 * .
	 * 
	 * @param tourKey
	 *            name and key of the Tour.
	 * @param index
	 *            index of the {@link Step} in the Tour (starts with 1).
	 * @param isfinal
	 *            indicates whether the Step is the last one or not.
	 * @param listener
	 *            List of {@link TourListener} which should be added.
	 */
	public void makeSettings(final String tourKey, final int index, final int totalSteps, final boolean isfinal,
			final List<TourListener> listener) {
		this.tourkey = tourKey;
		this.finalStep = isfinal;
		if (listener != null && !listener.isEmpty()) {
			this.listeners.addAll(listener);
		}
		this.index = index;
		this.length = totalSteps;
	}

	/**
	 * returns true if this {@link Step} is the last Step which will be performed in the Tour.
	 * 
	 * @return whether the step is the final {@link Step}
	 */
	public boolean isFinal() {
		return finalStep;
	}

	/**
	 * writes the state of the Tour to the properties. Should be called when the Tour ends or was
	 * closed.
	 */
	protected void writeStateToFile() {
		// not part of any tour, nothing to save here
		if (tourkey == null) {
			return;
		}
		TourManager tm = TourManager.getInstance();
		tm.setTourState(tourkey, (isFinal() ? TourState.COMPLETED : TourState.NOT_COMPLETED));
		tm.setTourProgress(tourkey, index, length);
	}

	/**
	 * notifies the {@link TourListener}s that the Tour has finished or was closed.
	 */
	protected void notifyTourListeners() {
		if (listeners == null || listeners.isEmpty()) {
			return;
		}
		LinkedList<TourListener> notify = new LinkedList<>(listeners);
		for (TourListener listen : notify) {
			listen.tourClosed();
		}
	}

	protected void conditionComplied() {
		bubble.triggerFire();
		this.stepCanceled();
	}

	public void addTourListener(final TourListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	public void removeTourListener(final TourListener listener) {
		if (listener != null) {
			listeners.remove(listener);
		}
	}

	/**
	 * Cancels this step and also all following steps. Note that this WILL break any tours because
	 * nothing is persisted after this point.
	 */
	public void killStep() {
		if (bubble != null) {
			bubble.dispose();
		}
		stepCanceled();
	}

	/** Can be overridden by subclasses (e.g. to remove Listeners). */
	protected abstract void stepCanceled();

	/**
	 * ensures that the Bubble can be displayed by returning an Array of Pre-Steps (e.g.
	 * NotOnScreenStep or NotViewableStep)
	 */
	public abstract Step[] getPreconditions();

}
