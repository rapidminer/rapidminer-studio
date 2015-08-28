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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.gui.tour.IntroductoryTour.TourListener;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;


/**
 * This Class manages whether a Tour is new, started or finishid. Also it remembers the number of
 * the highest Step the user made in a Tour and whether the user wants to be asked again by starting
 * RapidMiner if he want to execute a Tour.
 *
 * @author Thilo Kamradt
 */
public class TourManager {

	private static String TOUR_PROPERTIES = "tours.properties";

	// log messages
	private final String logPath = "com.rapidminer.gui.tour.TourManager.";

	private final String NO_FONT = logPath + "font_not_loaded";
	private final String NOT_SAVED = logPath + "could_not_save";
	private final String NOT_STARTED = logPath + "could_not_start";
	private final String NOT_FOUND = logPath + "not_found";
	private final String NO_KEY = logPath + "key_not_found";
	private final String NO_INDEX = logPath + "index_not_found";

	private static TourManager INSTANCE;

	private Properties properties;

	private final HashMap<String, Class<? extends IntroductoryTour>> tours;

	private final ArrayList<String> indexList;

	private LinkedList<String> newTours;

	private IntroductoryTour runningTour = null;

	private TourManager() {
		load();
		tours = new HashMap<String, Class<? extends IntroductoryTour>>();
		indexList = new ArrayList<String>();
		// registers tours from RapidMiner itself
		this.registerTour("RapidMiner", RapidMinerTour.class);
		// check for new or undone Tours
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				TourManager.this.checkTours();
			}
		});
	}

	/**
	 * Method to get the instance of the TourManager. Should be called instead of Constructor !
	 *
	 * @return returns the TourManager
	 */
	public static synchronized TourManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new TourManager();
		}
		return INSTANCE;
	}

	/**
	 * initiates a instance of the TourManager to Load the needed Fonts and check for new Tours
	 */
	public static synchronized void initiate() {
		if (INSTANCE == null) {
			INSTANCE = new TourManager();
		}
	}

	private void load() {
		this.properties = new Properties();
		File file = FileSystemService.getUserConfigFile(TOUR_PROPERTIES);
		try {
			file.createNewFile();
			properties.load(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			LogService.getRoot().log(Level.INFO, NOT_FOUND);
		} catch (IOException e) {
			LogService.getRoot().log(Level.INFO, NOT_FOUND);
		}
	}

	private boolean save() {
		File file = FileSystemService.getUserConfigFile(TOUR_PROPERTIES);
		try {
			properties.store(new FileOutputStream(file), "RapidMiner Datafiles");
			return true;
		} catch (FileNotFoundException e) {
			LogService.getRoot().log(Level.WARNING, NOT_SAVED);
			return false;
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, NOT_SAVED);
			return false;
		}
	}

	/**
	 * changes the Property-file-entry to the given state
	 *
	 * @param tourKey
	 *            key/name of the Tour
	 * @param state
	 *            {@link TourState} to write to the file
	 */
	public void setTourState(final String tourKey, final TourState state) {
		if (state == TourState.NEVER_ASK) {
			properties.setProperty(tourKey + ".ask", state.toString());
			save();
		} else {
			String currentState = properties.getProperty(tourKey);
			if (currentState == null) {
				properties.setProperty(tourKey, state.toString());
				save();
			} else if (!(TourState.valueOf(currentState) == TourState.COMPLETED)) {
				properties.setProperty(tourKey, state.toString());
				save();
			}
		}
	}

	/**
	 * writes the progress of the Tour to the property-file
	 *
	 * @param tourKey
	 *            key/name of the Tour
	 * @param step
	 *            index of the current {@link Step} of the Tour (counting started with 1).
	 */
	public void setTourProgress(final String tourKey, final int step, final int totalLength) {
		int stateBefore = Integer.parseInt(properties.getProperty(tourKey + ".progress", "" + 0));
		if (stateBefore < step) {
			properties.setProperty(tourKey + ".progress", "" + step);
			properties.setProperty(tourKey + ".length", "" + totalLength);
			save();
		}

	}

	/**
	 * @param tourKey
	 *            key / name of the Tour
	 * @return returns the {@link TourState} of the Tour with the given key or TourState.NEW_ONE if
	 *         there was no entry in the Property-file.
	 */
	public TourState getTourState(final String tourKey) {
		String stateKey = properties.getProperty(tourKey);
		if (stateKey == null) {
			setTourState(tourKey, TourState.NEW_ONE);
			return TourState.NEW_ONE;
		} else {
			return TourState.valueOf(stateKey);
		}
	}

	/**
	 * @param tourKey
	 *            key/name of the Tour
	 * @return true if the user wants to be asked again and false otherwise
	 */
	public boolean getAskState(final String tourKey) {
		if (properties.getProperty(tourKey + ".ask", null) == null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @param tourKey
	 *            name/key of the Tour.
	 * @return returns the maximum number of Steps the user has begun (e.g. if the user once made 5
	 *         Steps and another time he made 15 Steps this Method returns 15)
	 */
	public int getProgress(final String tourKey) {
		return Integer.parseInt(properties.getProperty(tourKey + ".progress", "" + 0));
	}

	/**
	 * @return returns the progress in percent rounded to an Integer.
	 */
	public int getProgressInPercent(final String tourKey) {
		int length = this.getTourLength(tourKey);
		if (length == 0) {
			return 0;
		}
		return this.getProgress(tourKey) * 100 / length;
	}

	/**
	 * @return returns the length of the Tour or 0 if the Tour wasn't even started until now.
	 */
	public int getTourLength(final String tourKey) {
		return Integer.parseInt(properties.getProperty(tourKey + ".length", "" + 0));
	}

	/**
	 * It's necessary to call this method in by the start of RapidMiner, to show your Tour in the
	 * Selection-Dialog for Tours.
	 *
	 * @param tourKey
	 *            name/key of your Tour
	 * @param tourClass
	 *            Class-object of your Tour
	 */
	public void registerTour(final String tourKey, final Class<? extends IntroductoryTour> tourClass) {
		tours.put(tourKey, tourClass);
		indexList.add(tourKey);
	}

	/**
	 * @return returns an Array of the keys from all registered Tours
	 */
	public String[] getTourkeys() {
		return tours.keySet().toArray(new String[] {});
	}

	/**
	 *
	 * @return returns the number of registered Tours.
	 */
	public int size() {
		return tours.size();
	}

	/**
	 * @param index
	 *            index of a Tour must be smaller than the value of TourManager.size().
	 * @return return returns the key of the {@link IntroductoryTour} which got the index by
	 *         registration.
	 */
	public String getKeyAt(final int index) {
		String toReturn = "";
		try {
			toReturn = indexList.get(index);
		} catch (IndexOutOfBoundsException e) {
			LogService.getRoot().log(Level.WARNING, NO_INDEX, new Object[] { index });
			throw new IllegalArgumentException();
		}
		return toReturn;
	}

	/**
	 * @param index
	 *            index of a Tour must be smaller than the value of TourManager.size().
	 * @return return returns the key of the {@link IntroductoryTour} which got the index by
	 *         registration.
	 */
	public IntroductoryTour get(final int index) {
		IntroductoryTour tour = null;
		try {
			tour = tours.get(indexList.get(index)).newInstance();
		} catch (IndexOutOfBoundsException e) {
			LogService.getRoot().log(Level.WARNING, NO_INDEX, new Object[] { index });
			throw new IllegalArgumentException();
		} catch (InstantiationException e) {
			LogService.getRoot().log(Level.WARNING, NO_INDEX, new Object[] { index });
			throw new IllegalArgumentException();
		} catch (IllegalAccessException e) {
			LogService.getRoot().log(Level.WARNING, NO_INDEX, new Object[] { index });
			throw new IllegalArgumentException();
		}
		return tour;
	}

	/**
	 * @param tourKey
	 *            key/name of the Tour
	 * @return returns an instance of a subclass of {@link IntroductoryTour} with the given key
	 */
	public IntroductoryTour get(final String tourKey) {
		IntroductoryTour tour = null;
		Class<? extends IntroductoryTour> tourClass = tours.get(tourKey);
		try {
			tour = tourClass.newInstance();
		} catch (InstantiationException e) {
			LogService.getRoot().log(Level.WARNING, NO_KEY, new Object[] { tourKey });
			throw new IllegalArgumentException();
		} catch (IllegalAccessException e) {
			LogService.getRoot().log(Level.WARNING, NO_KEY, new Object[] { tourKey });
			throw new IllegalArgumentException();
		}
		return tour;
	}

	/**
	 * Starts the Tour with the given key and returns a {@link TourRepresenter}
	 *
	 * @param tourKey
	 *            key/name of the Tour
	 * @return returns the running object, null if the Tour could not start (because another Tour is
	 *         running currently) or throws a RuntimeException if the key doesn't match any tour.
	 */
	public IntroductoryTour startTour(final String tourKey) {
		IntroductoryTour tour = null;
		boolean successfulStart = false;
		Class<? extends IntroductoryTour> tourClass = tours.get(tourKey);
		try {
			tour = tourClass.newInstance();
			successfulStart = tour.startTour();
		} catch (InstantiationException e) {
			LogService.getRoot().log(Level.WARNING, NOT_STARTED, new Object[] { tourKey });
			throw new IllegalArgumentException();
		} catch (IllegalAccessException e) {
			LogService.getRoot().log(Level.WARNING, NOT_STARTED, new Object[] { tourKey });
			throw new IllegalArgumentException();
		}
		return successfulStart ? tour : null;
	}

	/**
	 * This Method will check if any Tour is new or wasn't performed until now and will ask the user
	 * whether he wants to perform the Tour right now.
	 */
	public void checkTours() {
		// look for the new tours
		String[] keys = this.getTourkeys();
		newTours = new LinkedList<String>();
		for (int i = 0; i < keys.length; i++) {
			if (this.getTourState(keys[i]).equals(TourState.NEW_ONE) && this.getAskState(keys[i])) {
				newTours.add(keys[i]);
			}
		}
		// start asking for the execution of the tours
		this.startNext();
	}

	/**
	 * Starts the next Tour in the tourList
	 */
	private void startNext() {
		if (!newTours.isEmpty()) {
			String currentTourKey = newTours.remove();
			String propertyKey = "DontAskAgainTourChoosen";
			ParameterService.setParameterValue(propertyKey, "null");
			int returnValue = ConfirmDialog.showConfirmDialogWithOptionalCheckbox(ApplicationFrame.getApplicationFrame(),
					"new_tour_found", ConfirmDialog.YES_NO_OPTION, propertyKey, ConfirmDialog.NO_OPTION, true,
					currentTourKey);
			// save whether we will ask again
			if (!Boolean.parseBoolean(ParameterService.getParameterValue(propertyKey))) {
				this.setTourState(currentTourKey, TourState.NEVER_ASK);
			}
			// handle returnValue
			if (returnValue == ConfirmDialog.YES_OPTION) {
				IntroductoryTour currentTour = this.get(currentTourKey);
				currentTour.addTourListener(new TourListener() {

					@Override
					public void tourClosed() {
						TourManager.this.startNext();
					}
				});
				currentTour.startTour();
			}
			if (returnValue == ConfirmDialog.NO_OPTION) {
				this.startNext();
			}

		}
	}

	/**
	 * should be called by the {@link IntroductoryTour} to ensure no other Tour is running
	 * currently.
	 *
	 * @return returns true if there is no other Tour running and the Tour can Start or false if the
	 *         Tour is not allowed to start
	 */
	public boolean isAnotherTourRunning() {
		if (runningTour == null) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * sets the flag that a Tour is running and adds a listener to the given Tour to detect when the
	 * given Tour stops
	 *
	 * @param startingTour
	 *            the Tour which will start.
	 * @return returns true if there is no other Tour running and the Tour can Start or false if the
	 *         Tour is not allowed to start
	 */
	public boolean tourStarts(final IntroductoryTour startingTour) {
		if (runningTour == null) {
			runningTour = startingTour;
			startingTour.addTourListener(new TourListener() {

				@Override
				public void tourClosed() {
					runningTour.removeTourListener(this);
					runningTour = null;
				}
			});
			return true;
		}
		return false;
	}
}
