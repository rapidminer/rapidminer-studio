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
package com.rapidminer.gui.look.fc;

import java.util.Vector;


/**
 * A thread used for generating the file and image thumbs.
 * 
 * @author Ingo Mierswa
 */
public class ThumbGeneratorThread extends Thread {

	private Item tempItem;

	private ItemPanel browsePanel;

	public ThumbGeneratorThread(ItemPanel browsePanel) {
		this.browsePanel = browsePanel;
	}

	@Override
	public void run() {
		Vector<Item> vec = this.browsePanel.getItemsList();
		for (int i = 0; i < vec.size(); i++) {
			try {
				this.tempItem = vec.elementAt(i);
				this.tempItem.updateThumbnail();
				this.tempItem.repaint();
			} catch (Exception ex) {
				// do nothing
			}

			try {
				Thread.sleep(10);
			} catch (Exception exp) {
				// do nothing
			}
		}
	}
}
