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

import java.awt.Rectangle;


/**
 * A thread for dragging.
 *
 * @author Ingo Mierswa
 */
public class DragSelectionThread implements Runnable {

	private FileList fileList;

	private Rectangle rect;

	private int callCounts = 0;

	public DragSelectionThread(ItemPanel p, FileList fileList) {
		this.fileList = fileList;
	}

	public void startThread(Rectangle r) {
		this.callCounts++;
		this.rect = r;
		run();
	}

	@Override
	public void run() {
		this.callCounts--;
		if (this.callCounts > 0) {
			return;
		}

		for (Item tempItem : fileList.visibleItemsList) {
			if (this.rect.intersects(tempItem.getBounds())) {
				if (!this.fileList.selectedFilesVector.contains(tempItem)) {
					if (!tempItem.getSelectionMode()) {
						tempItem.updateSelectionMode(true);
					}
					this.fileList.selectedFilesVector.add(tempItem);
				}
			} else {
				if (this.fileList.selectedFilesVector.contains(tempItem)) {
					if (tempItem.getSelectionMode()) {
						tempItem.updateSelectionMode(false);
					}
					this.fileList.selectedFilesVector.remove(tempItem);
				}
			}
		}
		this.fileList.synchFilechoserSelection();
	}
}
