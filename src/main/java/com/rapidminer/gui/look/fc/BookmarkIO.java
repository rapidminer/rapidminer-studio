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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.filechooser.FileSystemView;


/**
 * A IO helper class for the bookmarks.
 * 
 * @author Ingo Mierswa
 */
public class BookmarkIO {

	public static final String PROPERTY_BOOKMARKS_DIR = "bookmarks.dir";

	public static final String PROPERTY_BOOKMARKS_FILE = ".bookmarks";

	private FileSystemView fileSystemView = FileSystemView.getFileSystemView();

	private File bookmarkFile;

	private List<Bookmark> bookmarks = new LinkedList<Bookmark>();

	public BookmarkIO() {
		File tempFile = this.fileSystemView.getHomeDirectory();
		tempFile = tempFile.getAbsoluteFile();
		File parentTempFile = tempFile.getParentFile();

		String bookmarksDirProperty = System.getProperty(PROPERTY_BOOKMARKS_DIR);
		if ((bookmarksDirProperty != null) && (bookmarksDirProperty.length() > 0)) {
			File applicationSpecifiedDir = new File(bookmarksDirProperty);
			if (applicationSpecifiedDir.exists()) {
				parentTempFile = applicationSpecifiedDir;
			}
		}

		try {
			parentTempFile = parentTempFile.getCanonicalFile();
		} catch (Exception exp) {
		}

		if ((parentTempFile != null) && parentTempFile.exists()
				&& this.fileSystemView.isTraversable(parentTempFile).booleanValue()) {
			this.bookmarkFile = new File(parentTempFile, PROPERTY_BOOKMARKS_FILE);

			try {
				this.bookmarkFile.createNewFile();
			} catch (IOException ex2) {
			}

			if (!this.bookmarkFile.exists()) {
				this.bookmarkFile.delete();
				this.bookmarkFile = new File(tempFile, PROPERTY_BOOKMARKS_FILE);
			}
		} else {
			this.bookmarkFile = new File(tempFile, PROPERTY_BOOKMARKS_FILE);
		}

		if (!this.bookmarkFile.exists()) {
			try {
				this.bookmarkFile.createNewFile();
			} catch (IOException ex) {
				// do nothing
			}
		} else {
			this.bookmarks = readBookmarks(this.bookmarkFile);
		}
	}

	public Collection<Bookmark> getBookmarks() {
		return this.bookmarks;
	}

	public void addToList(String name, String path) {
		Bookmark bookmark = new Bookmark(name, path);
		this.bookmarks.add(bookmark);
		Collections.sort(this.bookmarks);
		writeBookmarks(this.bookmarks, this.bookmarkFile);
	}

	public void deleteBookmark(Bookmark bookmark) {
		this.bookmarks.remove(bookmark);
		if (bookmark != null) {
			writeBookmarks(this.bookmarks, this.bookmarkFile);
		}
	}

	public void renameBookmark(Bookmark bookmark, String name) {
		if (bookmark != null) {
			bookmark.setName(name);
			writeBookmarks(this.bookmarks, this.bookmarkFile);
		}
	}

	public static List<Bookmark> readBookmarks(File bookmarkFile) {
		List<Bookmark> bookmarks = new LinkedList<Bookmark>();
		try (FileReader fr = new FileReader(bookmarkFile); BufferedReader in = new BufferedReader(fr)) {
			String line = in.readLine();
			if (line != null) {
				int numberOfBookmarks = Integer.parseInt(line);
				for (int i = 0; i < numberOfBookmarks; i++) {
					String name = in.readLine();
					String path = in.readLine();
					if (name != null && path != null) {
						bookmarks.add(new Bookmark(name, path));
					}
				}
			}
			in.close();
			Collections.sort(bookmarks);
		} catch (Exception e) {
			bookmarks.clear();
		}
		return bookmarks;
	}

	public static void writeBookmarks(Collection<Bookmark> bookmarks, File bookmarkFile) {
		try (FileWriter fw = new FileWriter(bookmarkFile); PrintWriter out = new PrintWriter(fw)) {
			out.println(bookmarks.size());
			for (Bookmark bookmark : bookmarks) {
				out.println(bookmark.getName());
				out.println(bookmark.getPath());
			}
			out.close();
		} catch (Exception e) {
			// do nothing
		}
	}
}
