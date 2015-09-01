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

import com.rapidminer.gui.tools.dialogs.MessageDialog;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * A tour consisting of multiple {@link Step}s explaining the usage of RapidMiner or an Extension.
 * 
 * Implementations of tour must implement a default (no-arg) constructor since they are created
 * reflectively.
 * 
 * 
 * @author Thilo Kamradt
 * 
 */
public abstract class IntroductoryTour {

	public static interface TourListener {

		/**
		 * will be called by a {@link Step} if the Tour was closed or is finished
		 */
		public void tourClosed();

	}

	private int maxSteps;

	/**
	 * This List has to be filled with the steps which should be performed while the Tour. They will
	 * be performed after the FIFO-concept (the first step added ist the first step added)
	 */
	private List<Step> steps = new ArrayList<Step>();

	private String tourKey;

	private boolean completeWindow;

	private List<TourListener> listeners;

	private Step head;

	private TourManager tourManager = TourManager.getInstance();

	/**
	 * This Constructor will initialize the {@link Step} step[] which has to be filled in the
	 * buildTour() Method and adds automatically a {@link FinalStep} to the end of your tour.
	 * 
	 * @param max
	 *            number of steps you want to perform (size of the Array you want to fill)
	 * @param tourName
	 *            name of the your tour (will be used as key as well)
	 */
	public IntroductoryTour(int steps, String tourName) {
		this(steps, tourName, true);
	}

	/**
	 * This Constructor will initialize the {@link Step} step[] which has to be filled in the
	 * buildTour() Method
	 * 
	 * @param steps
	 *            number of Steps you will do (size of the Array you want to fill)
	 * @param tourName
	 *            name of the your tour (will be used as key as well)
	 * @param addComppleteWindow
	 *            indicates whether a {@link FinalStep} with will be added or not.
	 */
	public IntroductoryTour(int steps, String tourName, boolean addComppleteWindow) {
		this.tourKey = tourName;
		this.completeWindow = addComppleteWindow;
		this.maxSteps = steps;
		this.listeners = new LinkedList<TourListener>();
	}

	/**
	 * starts the Tour if there is no other Tour running at the moment
	 */
	public boolean startTour() {
		if (!tourManager.isAnotherTourRunning()) {
			tourManager.tourStarts(this);
			buildTour();
			placeFollowers();
			head.start();
			return true;
		} else {
			new MessageDialog("alreadyRunning").setVisible(true);
			return false;
		}
	}

	/**
	 * This method fills the step[] instances of subclasses of {@link Step} which will guide through
	 * the tour
	 */
	protected abstract void buildTour();

	/**
	 * method to get the key and name of the Tour.
	 * 
	 * @return String with key of the Tour
	 */
	public String getKey() {
		return tourKey;
	}

	/**
	 * This method connects the single steps to a queue and the the needed parameters to the steps.
	 * After calling this method the isFinal-, tourKey-, index- and listeners-parameter of Step is
	 * set.
	 */
	private void placeFollowers() {
		Step tail;
		int counter = 1;
		Step[] currentPreconditions = steps.get(0).getPreconditions();
		head = ((currentPreconditions.length == 0) ? steps.get(0) : currentPreconditions[0]);
		tail = head;
		head.makeSettings(tourKey, ((currentPreconditions.length == 0) ? counter++ : counter), this.getSize(), false,
				listeners);
		// iterate over Array and create a queue
		for (int i = (counter - 1); i < steps.size(); i++) {
			// enqueue the Preconditions
			currentPreconditions = steps.get(i).getPreconditions();
			for (int j = 0; j < currentPreconditions.length; j++) {
				if (i == 0 && j == 0) {
					continue;
				}
				currentPreconditions[j].makeSettings(tourKey, counter, this.getSize(), false, listeners);
				tail.setNext(currentPreconditions[j]);
				tail = tail.getNext();
			}
			// add the current Step to the queue and set the next step
			tail.setNext(steps.get(i));
			tail = tail.getNext();
			if (!completeWindow && i == (steps.size() - 1)) {
				// this is the final step
				tail.makeSettings(tourKey, counter++, this.getSize(), true, listeners);
			} else {
				// this is just a step
				tail.makeSettings(tourKey, counter++, this.getSize(), false, listeners);
			}
		}
		if (completeWindow) {
			tail.setNext(new FinalStep(tourKey));
			tail.getNext().makeSettings(tourKey, counter++, this.getSize(), true, listeners);
		}
	}

	/**
	 * Removes the given {@link TourListener} from the {@link IntroductoryTour}.
	 * 
	 * @param listener
	 *            TourListener
	 */
	public void addTourListener(TourListener listener) {
		if (listener != null) {
			this.listeners.add(listener);
			for (Step step : steps) {
				step.addTourListener(listener);
			}
		}
	}

	/**
	 * Adds a {@link TourListener} to the IntroductoryTour and to all {@link Step}s of the Tour.
	 * 
	 * @param listener
	 *            TourListener
	 */
	public void removeTourListener(TourListener listener) {
		if (listener != null) {
			this.listeners.remove(listener);
			for (Step step : steps) {
				step.removeTourListener(listener);
			}
		}
	}

	/**
	 * 
	 * @return returns the size of the Tour including the {@link FinalStep} if the flag was set.
	 */
	public int getSize() {
		return (completeWindow ? this.maxSteps + 1 : maxSteps);
	}

	protected boolean addStep(Step step) {
		return steps.add(step);
	}

	protected Step getStep(int i) {
		return steps.get(i);
	}

}
