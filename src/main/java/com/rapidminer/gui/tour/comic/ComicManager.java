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
package com.rapidminer.gui.tour.comic;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tour.comic.episodes.AbstractEpisode;
import com.rapidminer.gui.tour.comic.episodes.ApplyModelEpisode;
import com.rapidminer.gui.tour.comic.episodes.ChangeParameterEpisode;
import com.rapidminer.gui.tour.comic.episodes.PreprocessingEpisode;
import com.rapidminer.gui.tour.comic.episodes.RunProcessEpisode;
import com.rapidminer.gui.tour.comic.episodes.ValidationEpisode;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;


/**
 * The manager which handles all {@link AbstractEpisode}s and saves/restores the user progress for
 * them.
 * <p>
 * Can also be used to register new comics at runtime (e.g. by extensions) by calling
 * {@link #addEpisode(AbstractEpisode)}.
 * 
 * @author Marco Boeck
 * 
 */
public class ComicManager extends AbstractObservable<ComicManagerEvent> {

	/** the singleton instance */
	private static ComicManager instance;

	/** name of the comics properties file */
	private static final String PROPERTIES_COMICS = "tutorials.properties";

	/** properties storing the max progress for each comic */
	private Properties properties;

	/** the list of episodes */
	private final List<AbstractEpisode> listOfEpisodes;

	/** the index of the current episode */
	private int currentEpisodeIndex;

	/** flag indicating if an episode is running */
	private boolean episodeRunning;

	/** the process which existed before a tutorial episode was started */
	private Process previousProcess;

	/**
	 * Creates a new {@link ComicManager} instance.
	 */
	private ComicManager() {
		// try to load comic font used by Steps
		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/AlterEgoBB.ttf")));
		} catch (IOException | FontFormatException | RepositoryException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tour.comic.ComicManager.failed_to_load_font", e);
		}

		this.listOfEpisodes = new CopyOnWriteArrayList<>();
		this.currentEpisodeIndex = -1;
		this.episodeRunning = false;

		// try to load properties
		loadProgress();

		/** >>> ADD MORE EPISODES HERE IN THE ORDER THEY SHOULD APPEAR <<< **/
		AbstractEpisode episodeRunProcess = new RunProcessEpisode();
		AbstractEpisode episodeChangeParameter = new ChangeParameterEpisode();
		AbstractEpisode episodeApplyModel = new ApplyModelEpisode();
		AbstractEpisode episodeValidation = new ValidationEpisode();
		AbstractEpisode episodePreprocessing = new PreprocessingEpisode();
		addEpisode(episodeRunProcess, null);
		addEpisode(episodeChangeParameter, episodeRunProcess);
		addEpisode(episodeApplyModel, episodeChangeParameter);
		addEpisode(episodeValidation, null);
		addEpisode(episodePreprocessing, null);
	}

	/**
	 * Returns the first {@link AbstractEpisode} or <code>null</code> if there is none.
	 * 
	 * @return
	 */
	public AbstractEpisode getFirstEpisode() {
		return listOfEpisodes.size() > 0 ? listOfEpisodes.get(0) : null;
	}

	/**
	 * Returns the current {@link AbstractEpisode} or <code>null</code> if there is none.
	 * 
	 * @return
	 */
	public AbstractEpisode getCurrentEpisode() {
		if (currentEpisodeIndex == -1 || listOfEpisodes.size() <= currentEpisodeIndex) {
			return null;
		}
		return listOfEpisodes.get(currentEpisodeIndex);
	}

	/**
	 * Tries to start the given episode. If there is another episode currently running, will do
	 * nothing and return <code>false</code>.
	 * 
	 * @param episode
	 * @return <code>true</code> if the new {@link AbstractEpisode} was started; <code>false</code>
	 *         otherwise
	 */
	public boolean startEpisode(final AbstractEpisode episode) {
		if (episode == null) {
			throw new IllegalArgumentException("episode must not be null!");
		}
		if (!listOfEpisodes.contains(episode)) {
			throw new IllegalArgumentException("ComicManager must know about episode!");
		}

		if (getCurrentEpisode() == null || !getCurrentEpisode().isEpisodeInProgress()) {
			try {
				episode.load();
				setCurrentEpisode(listOfEpisodes.indexOf(episode));

				MainFrame mainFrame = RapidMinerGUI.getMainFrame();
				previousProcess = mainFrame.getProcess();
				try {
					String defaultXML = episode.getDefaultProcessXML();
					if (defaultXML != null) {
						Process startingProcess = new Process(defaultXML);
						episode.getEpisodeEventDelegator().initProcessListener(startingProcess);
						mainFrame.setProcess(startingProcess, false);
					} else {
						mainFrame.setProcess(new Process(), false);
					}
				} catch (IOException | XMLException e) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.gui.tour.comic.ComicManager.failed_to_set_initial_state", e);
					mainFrame.newProcess();
				}
				episode.startEpisode();
				// just to get past the maybe skipped initial transition due to the starting process
				// xml
				episode.getEpisodeEventDelegator().processUpdated(mainFrame.getProcess());

				episodeRunning = true;
				fireUpdate(ComicManagerEvent.EPISODE_STARTED.setEpisode(episode));
				LogService.getRoot().log(Level.FINE, "com.rapidminer.gui.tour.comic.ComicManager.episode_started",
						episode.getTitle());
				return true;
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tour.comic.ComicManager.failed_to_load_episode",
						e);
			}
		}
		return false;
	}

	/**
	 * Finishes the current episode. The next call to {@link #getCurrentEpisode()} will return
	 * <code>null</code>.
	 */
	public void finishCurrentEpisode() {
		if (getCurrentEpisode() != null) {
			AbstractEpisode episode = getCurrentEpisode();
			LogService.getRoot().log(Level.FINE, "com.rapidminer.gui.tour.comic.ComicManager.episode_finished",
					getCurrentEpisode().getTitle());
			stopEpisode();
			fireUpdate(ComicManagerEvent.EPISODE_FINISHED.setEpisode(episode));
		}
	}

	/**
	 * Cancels the current episode. The next call to {@link #getCurrentEpisode()} will return
	 * <code>null</code>.
	 */
	public void cancelCurrentEpisode() {
		if (getCurrentEpisode() != null) {
			AbstractEpisode episode = getCurrentEpisode();
			LogService.getRoot().log(Level.FINE, "com.rapidminer.gui.tour.comic.ComicManager.episode_cancelled",
					getCurrentEpisode().getTitle());
			stopEpisode();
			fireUpdate(ComicManagerEvent.EPISODE_CANCELED.setEpisode(episode));
		}
	}

	/**
	 * Updates the maximum progress in percent [0, 100] for the {@link #getCurrentEpisode()} for the
	 * specified {@link AbstractEpisode}. If the given progress is lower than the currently set one,
	 * ignores the new value. Progress must be between [0, 100].
	 * 
	 * @param episode
	 * @param maxProgress
	 * @throws IllegalArgumentException
	 *             if the max progress is illegal
	 */
	public void updateMaxProgressForEpisode(final AbstractEpisode episode, final int maxProgress)
			throws IllegalArgumentException {
		if (maxProgress < 0 || maxProgress > 100) {
			throw new IllegalArgumentException("maxProgress must not be < 0 or > 100!");
		}
		if (episode == null) {
			throw new IllegalArgumentException("episode must not be null!");
		}
		if (maxProgress <= episode.getMaxAchievedProgress()) {
			return;
		}

		episode.setMaxAchievedProgress(maxProgress);
		fireUpdate(ComicManagerEvent.EPISODE_PROGRESS_UPDATE.setEpisode(episode));
		properties.put(episode.getI18NKey(), String.valueOf(maxProgress));
		saveProgress();
	}

	/**
	 * Returns the number of episodes which are in this series.
	 * 
	 * @return
	 */
	public int getNumberOfEpisodes() {
		return listOfEpisodes.size();
	}

	/**
	 * Returns the {@link AbstractEpisode} specified by the index.
	 * 
	 * @param index
	 * @return
	 */
	public AbstractEpisode getEpisodeByIndex(final int index) {
		if (index < 0 || index >= listOfEpisodes.size()) {
			throw new IllegalArgumentException("index must be between [0, size-1]!");
		}
		return listOfEpisodes.get(index);
	}

	/**
	 * Returns the index of the given {@link AbstractEpisode}. If the episode is unknown, returns
	 * -1.
	 * 
	 * @param episode
	 * @return
	 */
	public int indexOf(AbstractEpisode episode) {
		return listOfEpisodes.indexOf(episode);
	}

	/**
	 * Returns <code>true</code> if an episode is currently running; <code>false</code> otherwise.
	 * 
	 * @return
	 */
	public boolean isEpisodeRunning() {
		return episodeRunning;
	}

	/**
	 * Stops the current episode.
	 */
	private void stopEpisode() {
		getCurrentEpisode().finishEpisode();
		currentEpisodeIndex = -1;
		episodeRunning = false;

		// restore previous process
		if (previousProcess != null) {
			RapidMinerGUI.getMainFrame().setProcess(previousProcess, false);
		}
	}

	/**
	 * Sets the current episode via index.
	 * 
	 * @param index
	 * @return
	 */
	private void setCurrentEpisode(final int index) {
		if (index < 0 || index >= listOfEpisodes.size()) {
			throw new IllegalArgumentException("index must be between [0, size-1]!");
		}
		if (currentEpisodeIndex != index) {
			currentEpisodeIndex = index;
			fireUpdate(ComicManagerEvent.EPISODE_CHANGED.setEpisode(getCurrentEpisode()));
		}
	}

	/**
	 * Adds an implementation of {@link AbstractEpisode} to the comic manager. If the second
	 * paramter is specified, the new episode will be added after it.
	 * 
	 * @param newEpisode
	 * @param before
	 *            the episode after which the new one should be implemented. If <code>null</code>,
	 *            appends it at the end
	 */
	public synchronized void addEpisode(final AbstractEpisode newEpisode, final AbstractEpisode before) {
		if (newEpisode == null) {
			throw new IllegalArgumentException("episode must not be null!");
		}

		Object value = properties.get(newEpisode.getI18NKey());
		if (value != null) {
			try {
				Integer maxProgress = Integer.parseInt(String.valueOf(value));
				if (maxProgress >= 0 && maxProgress <= 100) {
					newEpisode.setMaxAchievedProgress(maxProgress);
				}
			} catch (NumberFormatException e) {
				LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.tour.comic.ComicManager.properties_progress_error",
						newEpisode.getI18NKey());
			}
		}

		if (before != null) {
			newEpisode.setPreviousEpisode(before);
			int index = listOfEpisodes.indexOf(before) + 1;
			if (index >= 0) {
				listOfEpisodes.add(index, newEpisode);
			}
		} else {
			listOfEpisodes.add(newEpisode);
		}
		fireUpdate(ComicManagerEvent.COMIC_ADDED.setEpisode(newEpisode));
	}

	/**
	 * Tries to load the saved progress for the comics.
	 */
	private void loadProgress() {
		this.properties = new Properties();
		File file = FileSystemService.getUserConfigFile(PROPERTIES_COMICS);
		try {
			file.createNewFile();
			properties.load(new FileInputStream(file));
		} catch (IOException e) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.tour.comic.ComicManager.properties_load_error", e);
		}
	}

	/**
	 * Tries to save the progress for the comics.
	 */
	private void saveProgress() {
		File file = FileSystemService.getUserConfigFile(PROPERTIES_COMICS);
		try {
			properties.store(new FileOutputStream(file), null);
		} catch (IOException e) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.tour.comic.ComicManager.properties_save_error", e);
		}
	}

	/**
	 * Returns the {@link ComicManager} instance.
	 * 
	 * @return
	 */
	public static synchronized ComicManager getInstance() {
		if (instance == null) {
			instance = new ComicManager();
		}
		return instance;
	}

}
