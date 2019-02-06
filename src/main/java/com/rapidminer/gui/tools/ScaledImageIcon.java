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

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.net.URL;


/**
 * An {@link ImageIcon} that scales the loaded image without modifying the image buffer itself. For
 * instance, a {@link ScaledImageIcon} with an image of size 96x96 and a scaling factor of 4 would
 * be displayed as 24x24 icon. This is useful in environment where the physical resolution of the
 * screen is higher than the logical (e.g., on Apple Retina displays).
 *
 * @author Michael Knopf, Marcel Seifert
 */
public class ScaledImageIcon extends ImageIcon {

    private static final long serialVersionUID = 1L;

    /**
     * The final size for this icon
     */
    private int height;
    private int width;

    /**
     * A not serializable image representation in the final size. The original is kept in super.getImage()
     */
    private transient BufferedImage image;

	/**
	 * Creates a new scaled {@link ImageIcon} from the given URL with the given logical height and width.
	 *
	 * @param url
	 * 		the image URL
	 * @param width
	 * 		the width of the image
	 * @param height
	 * 		the height of the image
	 */
	public ScaledImageIcon(URL url, int height, int width) {
		super(url);
		if (height < 1) {
			throw new IllegalArgumentException("Height must be positive and non-zero!");
		}
		if (width < 1) {
			throw new IllegalArgumentException("Width must be positive and non-zero!");
		}
		this.height = height;
		this.width = width;
	}


	/**
	 * Creates a new scaled {@link ImageIcon} from the given byte array with the given logical height and width.
	 *
	 * @param imageData
	 * 		the image as a byte array
	 * @param width
	 * 		the width of the image
	 * @param height
	 * 		the height of the image
	 * @since 9.0.0
	 */
	public ScaledImageIcon(byte[] imageData, int height, int width) {
		super(imageData);
		if (height < 1) {
			throw new IllegalArgumentException("Height must be positive and non-zero!");
		}
		if (width < 1) {
			throw new IllegalArgumentException("Width must be positive and non-zero!");
		}
		this.height = height;
		this.width = width;
	}

    /**
     * Creates a new scaled {@link ImageIcon} based on the given Image with the given logical height
     * and width.
     *
     * @param image the base image
     */
    public ScaledImageIcon(Image image, int height, int width) {
        super(image);
        if (height < 1) {
            throw new IllegalArgumentException("Height must be positive and non-zero!");
        }
        if (width < 1) {
            throw new IllegalArgumentException("Width must be positive and non-zero!");
        }
        this.height = height;
        this.width = width;
    }

    @Override
    public int getIconHeight() {
        return this.height;
    }

    @Override
    public int getIconWidth() {
        return this.width;
    }

    @Override
    public synchronized void paintIcon(Component c, Graphics graphics, int x, int y) {
        ImageObserver observer = getImageObserver();
        if (observer == null) {
            observer = c;
        }

		Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(super.getImage(), x, y, getIconWidth(), getIconHeight(), observer);
        g.dispose();
    }

    /**
     * Returns this icon's <code>Image</code> scaled down to its logical size for compatibility
     * reasons.
     *
     * @return the <code>Image</code> object for this <code>ImageIcon</code>
     */
    @Override
    public Image getImage() {
        if (image == null) {
            ImageObserver observer = getImageObserver();

            BufferedImage bufferedImage = new BufferedImage(getIconWidth(), getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bufferedImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(super.getImage(), 0, 0, getIconWidth(), getIconHeight(), observer);
            g.dispose();
            image = bufferedImage;
        }
        return image;
    }

    /**
     * Creates a grayed out version of this icon.
     *
     * @return the disabled icon
     */
    public ScaledImageIcon createDisabledIcon() {
        Image grayImage = GrayFilter.createDisabledImage(super.getImage());
        return new ScaledImageIcon(grayImage, height, width);
    }

}
