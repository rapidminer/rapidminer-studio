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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.image.ImageObserver;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.Perspective;
import com.rapidminer.gui.PerspectiveChangeListener;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawDecorator;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.flow.processrendering.view.RenderPhase;
import com.rapidminer.gui.processeditor.ExtendedProcessEditor;
import com.rapidminer.gui.tour.comic.ComicManager;
import com.rapidminer.gui.tour.comic.ComicManagerEvent;
import com.rapidminer.gui.tour.comic.episodes.AbstractEpisode;
import com.rapidminer.gui.tour.comic.episodes.EpisodeEvent;
import com.rapidminer.gui.tour.comic.episodes.EpisodeHelper;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.vlsolutions.swing.docking.event.DockableStateChangeEvent;
import com.vlsolutions.swing.docking.event.DockableStateChangeListener;


/**
 * Handles the rendering of all comic images.
 *
 * @author Marcin Skirzynski, Marco Boeck
 *
 */
public class ComicRenderer {

	/** the {@link ProcessRendererView} instance */
	private final ProcessRendererView processRenderer;

	/** the {@link ProcessDrawDecorator} instance */
	private ProcessDrawDecorator decorator;

	/** the {@link MainFrame} instance */
	private final MainFrame mainFrame;

	/** indicates if the listeners have already been initialized */
	private boolean initialized;

	/**
	 * Creates a new {@link ComicRenderer} instance.
	 *
	 * @param processRenderer
	 * @param mainFrame
	 */
	public ComicRenderer(final ProcessRendererView processRenderer, final MainFrame mainFrame) {
		this.processRenderer = processRenderer;
		this.mainFrame = mainFrame;
		this.initialized = false;
	}

	/**
	 * Starts the listeners for the comic tutorials. Listeners can only be started once, so multiple
	 * calls have no effect.
	 */
	public synchronized void init() {
		if (initialized) {
			return;
		}
		// add listener for every change in the process and delegate it to the current episode.
		mainFrame.addExtendedProcessEditor(new ExtendedProcessEditor() {

			@Override
			public void setSelection(final List<Operator> selection) {
				if (getCurrentEpisode() != null) {
					getCurrentEpisode().getEpisodeEventDelegator().setSelection(selection);
				}
			}

			@Override
			public void processUpdated(final Process process) {
				if (getCurrentEpisode() != null) {
					getCurrentEpisode().getEpisodeEventDelegator().processUpdated(process);
				}
			}

			@Override
			public void processChanged(final Process process) {
				if (getCurrentEpisode() != null) {
					getCurrentEpisode().getEpisodeEventDelegator().processChanged(process);
				}
			}

			@Override
			public void processViewChanged(final Process process) {
				if (getCurrentEpisode() != null) {
					getCurrentEpisode().getEpisodeStateMachine().onEvent(EpisodeEvent.PROCESS_VIEW_CHANGED, process);
				}
			}
		});
		// add listener for every dockable state change
		mainFrame.getDockingDesktop().addDockableStateChangeListener(new DockableStateChangeListener() {

			@Override
			public void dockableStateChanged(final DockableStateChangeEvent event) {
				if (getCurrentEpisode() != null) {
					getCurrentEpisode().getEpisodeEventDelegator().dockableStateChanged(event);
				}
			}

		});
		// add listener for perspective change
		mainFrame.getPerspectives().addPerspectiveChangeListener(new PerspectiveChangeListener() {

			@Override
			public void perspectiveChangedTo(final Perspective perspective) {
				if (getCurrentEpisode() != null) {
					getCurrentEpisode().getEpisodeEventDelegator().perspectiveChangedTo(perspective);
				}
			}
		});
		// add listener for comic events and make sure the latest image is painted
		ComicManager.getInstance().addObserver(new Observer<ComicManagerEvent>() {

			@Override
			public void update(final Observable<ComicManagerEvent> observable, final ComicManagerEvent arg) {
				RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer().getModel().fireMiscChanged();
			}
		}, true);
		// add process draw decorator to draw comic tutorial
		decorator = new ProcessDrawDecorator() {

			@Override
			public void draw(ExecutionUnit process, Graphics2D g2, ProcessRendererModel model) {
				if (g2 == null) {
					throw new IllegalArgumentException("graphics must not be null!");
				}

				int splitPanelCount = EpisodeHelper.getNumberOfDisplayedSplitPanels();
				if (hasImageToDraw(splitPanelCount)) {
					int index = model.getProcessIndex(process);
					// different drawing depending on whether it is a splitted panel or not
					Image comic = getImagesToDraw(splitPanelCount).get(index);
					int originalComicWidth = comic.getWidth(null);
					int originalComicHeight = comic.getHeight(null);
					int comicWidth = originalComicWidth;
					int comicHeight = originalComicHeight;

					double processWidth = model.getProcessWidth(process);
					double processHeight = model.getProcessHeight(process);
					int widthDifference = (int) processWidth - 2 * ProcessDrawer.WALL_WIDTH - originalComicWidth;
					int heightDifference = (int) processHeight - 2 * ProcessDrawer.PADDING - originalComicHeight;

					// scale comic if it is too wide/high
					if (widthDifference < 0 || heightDifference < 0) {
						if (widthDifference - heightDifference > 0) {
							comicHeight = (int) processHeight - 2 * ProcessDrawer.PADDING;
							comic = comic.getScaledInstance(-1, comicHeight, Image.SCALE_SMOOTH);
							comicWidth = comic.getWidth(null);
						} else {
							comicWidth = (int) processWidth - 2 * ProcessDrawer.WALL_WIDTH;
							comic = comic.getScaledInstance(comicWidth, -1, Image.SCALE_SMOOTH);
							comicHeight = comic.getHeight(null);
						}
					}

					// center comic
					Graphics2D translated = (Graphics2D) g2.create();
					translated.translate((int) processWidth / 2 - comicWidth / 2, (int) processHeight / 2 - comicHeight / 2);

					translated.drawImage(comic, 0, 0, new ImageObserver() {

						@Override
						public boolean imageUpdate(final Image img, final int infoflags, final int x, final int y,
								final int width, final int height) {
							return false;
						}
					});
					translated.dispose();
				}
			}

			@Override
			public void print(ExecutionUnit process, Graphics2D g2, ProcessRendererModel model) {
				draw(process, g2, model);
			}

			/**
			 * Returns the images for the specified number of split panels. If the number of
			 * splitted process panels does NOT match the number of images the episode provides,
			 * returns <code>null</code>.
			 *
			 * @param splitPanelCount
			 * @return
			 */
			private List<Image> getImagesToDraw(final int splitPanelCount) {
				if (getCurrentEpisode() == null || getCurrentEpisode().getNumberOfImages() != splitPanelCount) {
					return null;
				}

				List<Image> listOfImages = new LinkedList<>();
				for (int i = 0; i < splitPanelCount; i++) {
					listOfImages.add(getCurrentEpisode().getImage(i));
				}
				return listOfImages;
			}

			/**
			 * Returns <code>true</code> if there is a comic image to draw. Only returns
			 * <code>true</code> if the number of images from the episode matches the number of
			 * split panels currently shown.
			 *
			 * @param splitPanelCount
			 *            the number of splitted process renderers, e.g. 2 for x-val or 1 for a
			 *            normal process
			 */
			private boolean hasImageToDraw(final int splitPanelCount) {
				return getImagesToDraw(splitPanelCount) != null && getImagesToDraw(splitPanelCount).size() > 0;
			}

		};
		// add process drawer decorator to actually draw the comic
		processRenderer.addDrawDecorator(decorator, RenderPhase.BACKGROUND);

		initialized = true;
	}

	/**
	 * Renders the comic image.
	 *
	 * @param graphics
	 * @param splitPanelCount
	 *            the number of splitted process renderers, e.g. 2 for x-validation
	 * @param index
	 *            the index of the subprocess panel we are drawing
	 */
	public void draw(final Graphics graphics, final int splitPanelCount, final int index) {

	}

	/**
	 * Called on a started drag event. Will propagate the event to the current episode.
	 */
	public void dragStarted(final Transferable t) {
		if (getCurrentEpisode() != null) {
			try {
				getCurrentEpisode().getEpisodeEventDelegator().dragStarted(t);
			} catch (Exception e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tour.comic.ComicRenderer.drag_fail", e);
			}
		}
	}

	/**
	 * Called on a finished drag event. Will propagate the event to the current episode.
	 */
	public void dragEnded() {
		if (getCurrentEpisode() != null) {
			try {
				getCurrentEpisode().getEpisodeEventDelegator().dragEnded();
			} catch (Exception e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tour.comic.ComicRenderer.drag_fail", e);
			}
		}
	}

	/**
	 * Returns the current episode or <code>null</code>.
	 */
	private AbstractEpisode getCurrentEpisode() {
		return ComicManager.getInstance().getCurrentEpisode();
	}

}
