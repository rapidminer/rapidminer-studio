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
package com.rapidminer.repository.gui.actions;

import com.rapidminer.repository.Entry;
import com.rapidminer.repository.gui.RepositoryTree;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;


/**
 * This action copies from the selected location.
 * 
 * @author Simon Fischer
 */
public class CopyLocationAction extends AbstractRepositoryAction<Entry> {

	private static final long serialVersionUID = 1L;

	public CopyLocationAction(RepositoryTree tree) {
		super(tree, Entry.class, false, "repository_copy_location");
	}

	@Override
	public void actionPerformed(Entry e) {
		String value = e.getLocation().toString();
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(value), new ClipboardOwner() {

			@Override
			public void lostOwnership(Clipboard clipboard, Transferable contents) {}
		});
	}

}
