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

import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tour.BubbleWindow;
import com.rapidminer.gui.tour.comic.episodes.AbstractEpisode;
import com.rapidminer.tools.Tools;


/**
 * Abstract class for an episode state which can display a comic which can be represented by an
 * image.
 *
 * @author Marcin Skirzynski, Marco Boeck
 *
 */
public abstract class EpisodePanel implements EpisodeState {

	/**
	 * the list of resource locations for the images. Relative to
	 * "resources/com/rapidminer/resources".
	 */
	private final List<String> listOfResourceLocations = new LinkedList<>();

	/** the list of images for this instance */
	private final List<Image> listOfImages = new LinkedList<>();

	/**
	 * Creates a comic {@link EpisodePanel} without any images. Apart from that, works exactly like
	 * an {@link EpisodePanel} with images.
	 */
	public EpisodePanel() {
		this(new String[] {});
	}

	/**
	 * Creates a comic {@link EpisodePanel} with the given number of images. Images are represented
	 * by their resource location(s) as {@link String}s. A panel must have the same number of images
	 * as the number of splitted process panels.
	 * <p>
	 * In other words, an episode panel for a state where the user is watching the whole process in
	 * the {@link ProcessRendererView} must have exactly one image, an episode panel for a state
	 * where the user is watching for example an x-validation (which is displayed in a splitted
	 * panel which consists of 2 seperate panels) must have exactly two images.
	 * <p>
	 * More formally, the number of images must always be equal to the number of subprocesses
	 * rendered in the {@link ProcessRendererView}. See {@link ProcessRendererModel#getProcesses()}.
	 * <p>
	 * If the numbers do not match, the images are <i>NOT</i> shown, however the
	 * {@link EpisodePanel} will work regardless.
	 *
	 * @param resourceLocations
	 *            the location for the image(s) for splitted panels; relative to
	 *            "resources/com/rapidminer/resources"
	 */
	public EpisodePanel(final String... resourceLocations) {
		if (resourceLocations == null) {
			throw new IllegalArgumentException("resourceLocations must not be null");
		}

		for (String location : resourceLocations) {
			listOfResourceLocations.add(location);
		}
	}

	/**
	 * Preloads and stores the associated image(s).
	 */
	public final void loadImage() throws IOException {
		listOfImages.clear();
		for (String location : listOfResourceLocations) {
			if (location != null) {
				listOfImages.add(ImageIO.read(Tools.getResource(location)));
			}
		}
	}

	/**
	 * As a default action this will set the current panel of the episode to the loaded image (or
	 * <code>null</code> if it isn't loaded yet). <br/>
	 * Override to add custom behaviour when this state is reached in a comic tutorial. Make sure to
	 * call this method afterwards, otherwise comic images will not be shown.
	 */
	@Override
	public void onTransition(final AbstractEpisode episode) {
		episode.setImages(listOfImages.toArray(new Image[listOfImages.size()]));
	}

	@Override
	public String toString() {
		return listOfResourceLocations.size() == 0 ? super.toString() : listOfResourceLocations.get(0);
	}

	/**
	 * Returns a flag indicating if this is an episode state which can be used to go back to it in
	 * case an error happens in the next step. This is useful if this is an intermediate step, e.g.
	 * one which is only shown while the user drags an operator. If this returns <code>false</code>,
	 * the {@link EpisodeStateMachine} will not save it as a state to go back to. <br/>
	 * Default implementation returns <code>true</code>.
	 *
	 * @return
	 */
	@Override
	public boolean isRestorableState() {
		return true;
	}

	/**
	 * Returns <code>true</code> if this {@link EpisodePanel} can be shown inside a splitted panel
	 * (e.g. an x-validation). This is automatically determined by the number of resource locations
	 * which were given via the constructor - only an instance which has more than one image can be
	 * a splitted episode panel.
	 *
	 * @return
	 */
	public final boolean isSplittedEpisodePanel() {
		return listOfResourceLocations.size() > 1;
	}

	/**
	 * Returns a button can be used to display a generic "next" button in a BubbleWindow. It does
	 * nothing else than kill the bubble after it was pressed.
	 *
	 * @param notifyListeners
	 *            if <code>true</code>, listeners will be notified that the bubble was killed
	 * @return
	 */
	protected JButton getBubbleWindowNextButton(final boolean notifyListeners) {
		return new JButton(new ResourceAction("comic.generic_next") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				Component source = (Component) e.getSource();
				Container parentContainer = SwingUtilities.getAncestorOfClass(BubbleWindow.class, source);
				if (parentContainer != null) {
					((BubbleWindow) parentContainer).killBubble(notifyListeners);
				}
			}
		});
	}

	/**
	 * Returns a button can be used to display a generic "got it" button in a BubbleWindow. It does
	 * nothing else than kill the bubble after it was pressed.
	 *
	 * @param notifyListeners
	 *            if <code>true</code>, listeners will be notified that the bubble was killed
	 * @return
	 */
	protected JButton getBubbleWindowOKButton(final boolean notifyListeners) {
		return new JButton(new ResourceAction("comic.generic_ok") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				Component source = (Component) e.getSource();
				Container parentContainer = SwingUtilities.getAncestorOfClass(BubbleWindow.class, source);
				if (parentContainer != null) {
					((BubbleWindow) parentContainer).killBubble(notifyListeners);
				}
			}
		});
	}

}
