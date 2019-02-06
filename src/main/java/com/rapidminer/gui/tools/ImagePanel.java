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
package com.rapidminer.gui.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.JPanel;


/**
 * This panel can be used to display an image in the background.
 * 
 * @author Ingo Mierswa, Marco Boeck
 */
public class ImagePanel extends JPanel {

	private static final long serialVersionUID = 3903395116300542548L;

	/**
	 * Determines how the panel should calculate its preferred size.
	 */
	public enum ResizeHandling {
		/** uses preferred size determined by its children, i.e. like any other {@link JPanel} */
		CHILDRENS_PREFERRED_SIZE,

		/** sets the preferred size to the image dimension */
		IMAGE_PREFERRED_SIZE,

		/** uses the width determined by its children, but the height of the image */
		IMAGE_PREFERRED_HEIGHT
	}

	/**
	 * Determines where the background image is anchored vertically when it is not stretched.
	 * 
	 */
	public enum VerticalAnchor {
		/** image anchored at the top-left corner */
		TOP,

		/** image anchored at the bottom-left corner */
		BOTTOM;
	}

	/** the background image to display */
	private Image image;

	/** the lowres background image to display if resolution does not meet requirements */
	private Image imageLowResolution;

	/** min width and height of resultion which must be met, otherwise lowres image is used */
	private Dimension minDimension;

	/** how the preferred size should be calculated for this instance */
	private ResizeHandling preferredSizeType = ResizeHandling.CHILDRENS_PREFERRED_SIZE;

	/** if true, the background image will be streched (if needed) */
	private boolean strechImage = true;

	private VerticalAnchor anchor = VerticalAnchor.TOP;

	/**
	 * Creates a panel displaying the given {@link Image} in the background. If preferredSizeType is
	 * set to {@link #CHILDRENS_PREFERRED_SIZE}, stretches the image to match the dimensions of the
	 * panel.
	 * 
	 * @param image
	 * @param preferredSizeType
	 */
	public ImagePanel(Image image, ResizeHandling preferredSizeType) {
		this(image, preferredSizeType, true);
	}

	/**
	 * Creates a panel displaying the given {@link Image} in the background.
	 * 
	 * @param image
	 * @param preferredSizeType
	 * @param stretchImage
	 *            if the preferredSizeType is set to {@link #CHILDRENS_PREFERRED_SIZE}, this
	 *            parameter defines if the image is stretched to match the dimensions of the panel
	 *            or not
	 */
	public ImagePanel(Image image, ResizeHandling preferredSizeType, boolean stretchImage) {
		this.image = image;
		this.preferredSizeType = preferredSizeType;
		this.strechImage = stretchImage;
		setOpaque(true);
	}

	/**
	 * Creates a panel displaying the given {@link Image} in the background.
	 * 
	 * @param image
	 * @param preferredSizeType
	 * @param anchor
	 *            if the preferredSizeType is set to {@link #CHILDRENS_PREFERRED_SIZE}, this
	 *            parameter defines if the image is anchored at the top or at the bottom.
	 */
	public ImagePanel(Image image, ResizeHandling preferredSizeType, VerticalAnchor anchor) {
		this.image = image;
		this.preferredSizeType = preferredSizeType;
		this.anchor = anchor;
		this.strechImage = false;
		setOpaque(true);
	}

	/**
	 * Sets the image which is used for low resolutions as the background and the minimum dimension
	 * (width and height) which are the threshold for using the lowres image. If the width or the
	 * height of the panel is less than specified here, the lowres image is used as a background.
	 * This changes on the fly, e.g. when the user changes his resolution or resizes the panel
	 * 
	 * @param image
	 * @param minDimension
	 */
	public void setLowResolutionImage(Image image, Dimension minDimension) {
		this.imageLowResolution = image;
		this.minDimension = minDimension;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension dimension = super.getPreferredSize();
		switch (preferredSizeType) {
			case IMAGE_PREFERRED_HEIGHT:
				dimension.height = image.getHeight(null);
				break;
			case IMAGE_PREFERRED_SIZE:
				dimension.height = image.getHeight(null);
				dimension.width = image.getWidth(null);
				break;
			case CHILDRENS_PREFERRED_SIZE:
			default:
				break;
		}
		return dimension;
	}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics;
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());

		// determine if to use low res image (if available)
		Image imageToUse = image;
		if (minDimension != null) {
			double panelWidth = getWidth();
			double panelHeight = getHeight();
			if (panelWidth < minDimension.getWidth() || panelHeight < minDimension.getHeight()) {
				imageToUse = imageLowResolution;
			}
		}

		if (imageToUse != null) {
			if (!strechImage) {
				if (anchor == VerticalAnchor.BOTTOM) {
					g.drawImage(imageToUse, 0, getHeight() - imageToUse.getHeight(null), imageToUse.getWidth(null),
							imageToUse.getHeight(null), this);
				} else {
					g.drawImage(imageToUse, 0, 0, imageToUse.getWidth(null), imageToUse.getHeight(null), this);
				}
			} else {
				g.drawImage(imageToUse, 0, 0, getWidth(), getHeight(), this);
			}
		}
		paintChildren(graphics);
	}
}
