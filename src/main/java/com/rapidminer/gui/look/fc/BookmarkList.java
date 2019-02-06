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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;


/**
 * The list containing the bookmarks.
 *
 * @author Ingo Mierswa
 */
public class BookmarkList extends JList<Bookmark> implements ListSelectionListener, MouseListener {

	private static final long serialVersionUID = -7109320787696008679L;

	private static class BookmarkCellRenderer implements ListCellRenderer<Bookmark> {

		private JLabel label = new JLabel();

		public BookmarkCellRenderer() {
			this.label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			this.label.setOpaque(true);
			this.label.setIcon(SwingTools.createIcon("16/star.png"));
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends Bookmark> list, Bookmark bookmark, int index,
				boolean isSelected, boolean cellHasFocus) {
			String name = bookmark.getName();
			final String path = bookmark.getPath();

			if (isSelected) {
				label.setBackground(Colors.TEXT_HIGHLIGHT_BACKGROUND);
				label.setForeground(Colors.TEXT_HIGHLIGHT_FOREGROUND);
			} else {
				label.setBackground(UIManager.getColor("List.background"));
				label.setForeground(UIManager.getColor("List.foreground"));
			}

			if (!new File(path).exists()) {
				label.setForeground(Color.GRAY);
			}

			label.setText(name);
			label.setToolTipText("<html><b>" + name + "</b><br>" + path + "</html>");
			return label;
		}

	}

	private static final ListCellRenderer<Bookmark> RENDERER = new BookmarkCellRenderer();

	private FileList fileList;

	public BookmarkList(BookmarkListModel model, FileList fileList) {
		super(model);
		this.fileList = fileList;
		addListSelectionListener(this);
		addMouseListener(this);
	}

	@Override
	public ListCellRenderer<Bookmark> getCellRenderer() {
		return RENDERER;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false) {
			Bookmark selectedBookmark = getSelectedValue();
			if (selectedBookmark != null) {
				String path = selectedBookmark.getPath();
				File bookmarkFile = new File(path);
				if (bookmarkFile.exists() && bookmarkFile.canRead()) {
					fileList.filechooserUI.setCurrentDirectoryOfFileChooser(bookmarkFile);
				} else {
					JOptionPane.showConfirmDialog(fileList.fc, "Cannot access selected bookmark directory.",
							"Cannot access directory", JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		int index = locationToIndex(e.getPoint());
		setSelectedIndex(index);
		evaluatePopup(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		evaluatePopup(e);
	}

	/**
	 * Checks if the given mouse event is a popup trigger and creates a new popup menu if necessary.
	 */
	private void evaluatePopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			JPopupMenu menu = createBookmarkPopupMenu();
			if (menu != null) {
				menu.show(this, e.getX(), e.getY());
			}
		}
	}

	private JPopupMenu createBookmarkPopupMenu() {
		final Bookmark bookmark = getSelectedValue();
		if (bookmark != null) {
			JPopupMenu bookmarksPopup = new JPopupMenu();
			bookmarksPopup.add(new JMenuItem(new ResourceAction("file_chooser.rename_bookmark") {

				private static final long serialVersionUID = -3728467995967823779L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					fileList.renameBookmark(bookmark);
				}
			}));
			bookmarksPopup.add(new JMenuItem(new ResourceAction("file_chooser.delete_bookmark") {

				private static final long serialVersionUID = 5432105038105200178L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					fileList.deleteBookmark(bookmark);
				}
			}));
			return bookmarksPopup;
		} else {
			return null;
		}
	}
}
