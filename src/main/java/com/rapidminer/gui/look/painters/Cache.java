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

import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


/**
 * A cache for the cached painter.
 *
 * @author Ingo Mierswa
 */
public class Cache {

	private int maxCount;

	private List<WeakReference<Cache.Entry>> entries;

	Cache(int maxCount) {
		this.maxCount = maxCount;
		this.entries = new ArrayList<WeakReference<Cache.Entry>>(maxCount);
	}

	void setMaxCount(int maxCount) {
		this.maxCount = maxCount;
	}

	private Cache.Entry getEntry(Object key, GraphicsConfiguration config, int w, int h, Object[] args) {
		synchronized (this) {
			Cache.Entry entry;

			for (int counter = this.entries.size() - 1; counter >= 0; counter--) {

				entry = this.entries.get(counter).get();

				if (entry == null) {
					this.entries.remove(counter);
				} else if (entry.equals(config, w, h, args)) {
					return entry;
				}
			}
			entry = new Entry(config, w, h, args);
			if (this.entries.size() == this.maxCount) {
				this.entries.remove(0);
			}
			this.entries.add(new WeakReference<Cache.Entry>(entry));
			return entry;
		}
	}

	public Image getImage(Object key, GraphicsConfiguration config, int w, int h, Object[] args) {
		Cache.Entry entry = getEntry(key, config, w, h, args);
		return entry.getImage();
	}

	public void setImage(Object key, GraphicsConfiguration config, int w, int h, Object[] args, Image image) {
		Cache.Entry entry = getEntry(key, config, w, h, args);
		entry.setImage(image);
	}

	private static class Entry {

		private GraphicsConfiguration config;

		private Object[] args;

		private Image image;

		private int w;

		private int h;

		Entry(GraphicsConfiguration config, int w, int h, Object[] args) {
			this.config = config;
			this.args = args;
			this.w = w;
			this.h = h;
		}

		public void setImage(Image image) {
			this.image = image;
		}

		public Image getImage() {
			return this.image;
		}

		@Override
		public String toString() {
			StringBuffer value = new StringBuffer(super.toString() + "[ graphicsConfig=" + this.config + ", image="
					+ this.image + ", w=" + this.w + ", h=" + this.h);
			if (this.args != null) {
				for (Object element : this.args) {
					value.append(", " + element);
				}
			}
			value.append("]");
			return value.toString();
		}

		public boolean equals(GraphicsConfiguration config, int w, int h, Object[] args) {
			if (this.w == w && this.h == h
					&& (this.config != null && this.config.equals(config) || this.config == null && config == null)) {
				if (this.args == null && args == null) {
					return true;
				}
				if (this.args != null && args != null && this.args.length == args.length) {
					for (int counter = args.length - 1; counter >= 0; counter--) {
						if (!this.args[counter].equals(args[counter])) {
							return false;
						}
					}
					return true;
				}
			}
			return false;
		}
	}
}
