/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.flow.processrendering.background;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ProgressThreadListener;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.UserData;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * Container for a process background image.
 *
 * @author Marco Boeck
 * @since 7.0.0
 *
 */
public class ProcessBackgroundImage implements UserData<Object> {

	private int x;
	private int y;
	private int w;
	private int h;
	private String location;

	/** the process this background image is located in */
	private ExecutionUnit process;

	/** the loaded image or an error image */
	private volatile Image img;

	/** the image during loading */
	private final Image loadingImg;

	private int loadingW;
	private int loadingH;
	private int errorW;
	private int errorH;

	/** whether the image has been loaded successfully */
	private volatile boolean finishedImageLoading = false;

	/** whether the image failed to be loaded */
	private volatile boolean errorImageLoading = false;

	/** whether loading of the image is in progress */
	private AtomicBoolean loaded = new AtomicBoolean(false);

	/**
	 * Creates a process background image at the given location.
	 *
	 * @param x
	 *            upper left x coordinate of the image. If set to {@code -1}, the image will be
	 *            centered horizontally
	 * @param y
	 *            upper left y coordinate of the image. If set to {@code -1}, the image will be
	 *            centered vertically
	 * @param w
	 *            width of the image. If set to {@code -1}, the width will be determined by the
	 *            image
	 * @param h
	 *            height of the image. If set to {@code -1}, the height will be determined by the
	 *            image
	 * @param location
	 *            repository location of the image
	 * @param process
	 *            the process for which the image is
	 */
	public ProcessBackgroundImage(int x, int y, int w, int h, String location, ExecutionUnit process) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.location = location;
		this.process = process;

		this.loadingImg = createImageFromString(I18N.getGUILabel("process_background.loading.label"));
	}

	/**
	 *
	 * @return x-coordinate of the background image. If {@code -1}, the image is centered
	 */
	public int getX() {
		return x;
	}

	/**
	 *
	 * @return y-coordinate of the background image. If {@code -1}, the image is centered
	 */
	public int getY() {
		return y;
	}

	/**
	 *
	 * @return width of the background image
	 */
	public int getWidth() {
		if (finishedImageLoading) {
			return w;
		}
		if (errorImageLoading) {
			return errorW;
		}

		return loadingW;
	}

	/**
	 *
	 * @return height of the background/loading/error image
	 */
	public int getHeight() {
		if (finishedImageLoading) {
			return h;
		}
		if (errorImageLoading) {
			return errorH;
		}

		return loadingH;
	}

	/**
	 *
	 * @return original width of the background image
	 */
	public int getOriginalWidth() {
		return w;
	}

	/**
	 *
	 * @return original height of the background image
	 */
	public int getOriginalHeight() {
		return h;
	}

	/**
	 *
	 * @return the repository location of the background image
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Returns the background image. If it has not yet been loaded, will start asynchronous loading
	 * of it and return a placeholder image until loading is complete. In case an error occurs
	 * during loading, it will return an error image.
	 *
	 * @param listener
	 *            the listener for the {@link ProgressThread} loading the image. If no image needs
	 *            to be loaded, does nothing with it. Can be {@code null}
	 * @return an image, never {@code null}
	 */
	public Image getImage(ProgressThreadListener listener) {
		if (finishedImageLoading) {
			return img;
		}
		if (errorImageLoading) {
			return img;
		}

		// only load once
		if (loaded.compareAndSet(false, true)) {
			ProgressThread pg = new ProgressThread("process_background.loading") {

				@Override
				public void run() {
					try {
						RepositoryLocation location = new RepositoryLocation(ProcessBackgroundImage.this.getLocation());
						Entry entry = location.locateEntry();
						if (entry == null) {
							LogService.getRoot().log(Level.WARNING,
									"com.rapidminer.gui.flow.processrendering.background_image.ProcessBackgroundImageDecorator.missing");
							img = createImageFromString(I18N.getGUILabel("process_background.loading.error.label"));
							errorImageLoading = true;
							return;
						}

						if (entry instanceof BlobEntry) {
							BlobEntry blob = (BlobEntry) entry;
							// try and create actual image
							img = createImageFromBlob(blob);

							if (img == null) {
								LogService.getRoot().log(Level.WARNING,
										"com.rapidminer.gui.flow.processrendering.background_image.ProcessBackgroundImageDecorator.invalid_type");
								img = createImageFromString(I18N.getGUILabel("process_background.loading.error.label"));
								errorImageLoading = true;
								return;
							}

							finishedImageLoading = true;
							if (w == -1 || h == -1) {
								w = img.getWidth(null);
								h = img.getHeight(null);
							}
						} else {
							LogService.getRoot().log(Level.WARNING,
									"com.rapidminer.gui.flow.processrendering.background_image.ProcessBackgroundImageDecorator.invalid_type");
							img = createImageFromString(I18N.getGUILabel("process_background.loading.error.label"));
							errorImageLoading = true;
						}
					} catch (RepositoryException | MalformedRepositoryLocationException e) {
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.gui.flow.processrendering.background_image.ProcessBackgroundImageDecorator.invalid_loc",
								ProcessBackgroundImage.this.getLocation());
						img = createImageFromString(I18N.getGUILabel("process_background.loading.error.label"));
						errorImageLoading = true;
					} catch (IOException e) {
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.gui.flow.processrendering.background_image.ProcessBackgroundImageDecorator.invalid_type");
						img = createImageFromString(I18N.getGUILabel("process_background.loading.error.label"));
						errorImageLoading = true;
					}
				}
			};
			if (listener != null) {
				pg.addProgressThreadListener(listener);
			}
			pg.setIndeterminate(true);
			pg.start();
		}

		return loadingImg;
	}

	/**
	 * Returns the process this background image is in.
	 *
	 * @return the process
	 */
	public ExecutionUnit getProcess() {
		return process;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @param newParent
	 *            must be an {@link ExecutionUnit}.
	 */
	@Override
	public UserData<Object> copyUserData(Object newParent) {
		ProcessBackgroundImage copy = new ProcessBackgroundImage(x, y, w, h, location, process);
		return copy;
	}

	/**
	 * Creates an {@link Image} which displays the given {@link String}.
	 *
	 * @param text
	 *            this text will be displayed in the image
	 * @return the image, never {@code null}
	 */
	private Image createImageFromString(String text) {
		// to know bounds of desired text we need Graphics context so create fake one
		Graphics2D g2 = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		Font font = FontTools.getFont("Arial", Font.PLAIN, 24);
		g2.setFont(font);
		FontMetrics fm = g2.getFontMetrics();
		// set intermediate width and height so we don't lose original height of background image
		// while loading and/or in error case
		loadingW = fm.stringWidth(text);
		loadingH = fm.getHeight();
		errorW = loadingW;
		errorH = loadingH;
		g2.dispose();

		// create actual image now that text bounds are known
		BufferedImage img = new BufferedImage(loadingW, loadingH, BufferedImage.TYPE_INT_ARGB);
		g2 = img.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setFont(font);
		fm = g2.getFontMetrics();
		g2.setColor(Colors.TEXT_FOREGROUND);
		g2.drawString(text, 0, fm.getAscent());
		g2.dispose();

		return img;
	}

	/**
	 * Creates an {@link Image} from the given {@link BlobEntry}.
	 *
	 * @param entry
	 *            the blob entry which is expected to be an image
	 * @return the image or {@code null} if it is not an image
	 * @throws IOException
	 *             if the entry does not contain valid image data
	 * @throws RepositoryException
	 *             if the entry could not be read
	 */
	private Image createImageFromBlob(BlobEntry entry) throws IOException, RepositoryException {
		InputStream is = entry.openInputStream();
		BufferedImage img = ImageIO.read(is);
		is.close();

		return img;
	}
}
