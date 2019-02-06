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
package com.rapidminer.gui.look.painters;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.HashMap;
import java.util.Map;


/**
 * A cached painter.
 *
 * @author Ingo Mierswa
 */
public abstract class AbstractCachedPainter {

	protected static final Map<Object, Cache> cacheMap = new HashMap<Object, Cache>();

	public AbstractCachedPainter(int cacheCount) {
		getCache(getClass()).setMaxCount(cacheCount);
	}

	/** Paints the representation to cache to the supplied Graphics. */
	protected abstract void paintToImage(Component c, Graphics g, int w, int h, Object[] args);

	public void paint(Component c, Graphics g, int x, int y, int w, int h, Object[] args) {
		if (w <= 0 || h <= 0) {
			return;
		}
		Object key = getClass();
		GraphicsConfiguration config = c.getGraphicsConfiguration();
		Cache cache = getCache(key);
		Image image = cache.getImage(key, config, w, h, args);
		int attempts = 0;
		do {
			boolean draw = false;
			if (image instanceof VolatileImage) {
				switch (((VolatileImage) image).validate(config)) {
					case VolatileImage.IMAGE_INCOMPATIBLE:
						((VolatileImage) image).flush();
						image = null;
						break;
					case VolatileImage.IMAGE_RESTORED:
						draw = true;
						break;
				}
			}
			if (image == null) {
				image = createImage(c, w, h, config);
				cache.setImage(key, config, w, h, args, image);
				draw = true;
			}
			if (draw) {
				Graphics g2 = image.getGraphics();
				paintToImage(c, g2, w, h, args);
				g2.dispose();
			}

			paintImage(c, g, x, y, w, h, image, args);
		} while (image instanceof VolatileImage && ((VolatileImage) image).contentsLost() && ++attempts < 3);
	}

	protected void paintImage(Component c, Graphics g, int x, int y, int w, int h, Image image, Object[] args) {
		g.drawImage(image, x, y, null);
	}

	protected Image createImage(Component c, int w, int h, GraphicsConfiguration config) {
		return new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
	}

	public static void clearCache() {
		synchronized (cacheMap) {
			cacheMap.clear();
		}
	}

	private static Cache getCache(Object key) {
		synchronized (cacheMap) {
			Cache cache = cacheMap.get(key);
			if (cache == null) {
				cache = new Cache(1);
				cacheMap.put(key, cache);
			}
			return cache;
		}
	}
}
