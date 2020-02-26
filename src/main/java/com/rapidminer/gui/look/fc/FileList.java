/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileSystemView;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.ui.TableHeaderUI;
import com.rapidminer.gui.osx.OSXAdapter;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ScaledImageIcon;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.io.remote.RemoteFileSystemView;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.SystemInfoUtilities;

import sun.awt.shell.ShellFolder;



/**
 * The actual file selection list.
 *
 * @author Ingo Mierswa
 */
public class FileList extends JPanel implements PropertyChangeListener {

	/** Action command for order */
	private static final String ORDER = "ORDER:";
	/** Action command for viewType */
	private static final String VIEW_TYPE = "viewType:";
	/** Scroll Pane component names */
	private static final String TABLE_SCROLL_PANE = "tableScrollPane";
	private static final String BROWSE_SCROLL_PANE = "browseScrollPane";

	private static class MenuListener implements ActionListener {

		private final FileList fileList;

		private MenuListener(FileList fileList) {
			this.fileList = fileList;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if (e.getActionCommand().startsWith(ORDER)) {
					fileList.orderBy(e.getActionCommand().substring(ORDER.length()), false);
					fileList.arrangeTheFiles();
				} else if (e.getActionCommand().startsWith(VIEW_TYPE)) {
					fileList.filechooserUI.updateView(e.getActionCommand().substring(VIEW_TYPE.length()));
				}
			} catch (Exception exp) {
				// ignore
			}
		}
	}

	private static final long serialVersionUID = 8893252970970228545L;

	private static final ImageIcon SMALL_FILE_IMAGE = SwingTools.createImage("plaf/document_empty_16.png");

	private static final ImageIcon SMALL_FOLDER_IMAGE = SwingTools.createImage("plaf/folder_open_16.png");

	private static final ImageIcon BIG_FILE_IMAGE = SwingTools.createImage("plaf/document_empty_32.png");

	private static final ImageIcon BIG_FOLDER_IMAGE = SwingTools.createImage("plaf/folder_open_32.png");

	/** Logical size of a small icon */
	private static final int SMALL_ICON_SIZE = 16;
	/** Logical size of a big icon */
	private static final int BIG_ICON_SIZE = 32;
	/** Physical size of small icon */
	private static final int SCALED_SMALL_ICON_SIZE = (int) (SwingTools.getGUIScaling().getScalingFactor() * SMALL_ICON_SIZE);
	/** Physical size of a big icon */
	private static final int SCALED_BIG_ICON_SIZE = (int) (SwingTools.getGUIScaling().getScalingFactor() * BIG_ICON_SIZE);

	private static final boolean IS_MAC_OS = SystemInfoUtilities.getOperatingSystem() == SystemInfoUtilities.OperatingSystem.OSX;

	private final Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);

	private final Cursor normalCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

	private ButtonGroup viewButtonGroup;

	private ButtonGroup orderButtonGroup;

	protected ItemPanel itemPanel;

	private ThumbGeneratorThread thumbGenerator = new ThumbGeneratorThread(this.itemPanel);

	private final BookmarkIO bookmarksIO = new BookmarkIO();

	private final BookmarkListModel bookmarkListModel = new BookmarkListModel();

	private final JList<Bookmark> bookmarkList = new BookmarkList(bookmarkListModel, this);

	private final JSplitPane mainSplitPane = new JSplitPane();

	protected JPanel cardPanel = new JPanel(new CardLayout());

	private final JScrollPane tableScrollPane = new JScrollPane();

	protected FileTable tablePanel;

	protected JScrollPane browseScrollPane = new JScrollPane();

	private final TreeMap<String, Object[]> systemInfoCach = new TreeMap<>();

	protected ItemPanelKeyboardListener keyListener = new ItemPanelKeyboardListener();

	protected Vector<Item> completeItemsList = new Vector<>(20);

	protected Vector<Item> visibleItemsList = new Vector<>(20);

	protected Item lastSelected;

	protected JPopupMenu panePopup;

	private JCheckBoxMenuItem showHiddenMenuItem;

	protected JFileChooser fc;

	protected FileChooserUI filechooserUI;

	protected File currentFile = null;

	protected Vector<Item> selectedFilesVector = new Vector<>();

	public static String ORDER_BY_FILE_NAME = I18N.getGUIMessage("gui.menu.file_chooser.sort_by.file_name");

	public static String ORDER_BY_FILE_TYPE = I18N.getGUIMessage("gui.menu.file_chooser.sort_by.file_type");

	public static String ORDER_BY_FILE_SIZE = I18N.getGUIMessage("gui.menu.file_chooser.sort_by.file_size");

	public static String ORDER_BY_FILE_MODIFIED = I18N.getGUIMessage("gui.menu.file_chooser.sort_by.last_modified");

	private static String FILE_CHOOSER_ROOT = I18N.getGUIMessage("gui.io.dataimport.source.local_file.label");

	protected String ORDER_BY = ORDER_BY_FILE_NAME;

	private JCheckBoxMenuItem autoArrangeCheckBox;

	private transient final Action SHOW_HIDDEN_ACTION = new ResourceAction("file_chooser.show_hidden") {

		private static final long serialVersionUID = 2591227051998175245L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			boolean showHidden = FileList.this.fc.isFileHidingEnabled();
			FileList.this.showHiddenMenuItem.setSelected(showHidden);
			FileList.this.fc.setFileHidingEnabled(!showHidden);
		}
	};

	private transient final Action REFRESH_ACTION = new ResourceAction("file_chooser.refresh") {

		private static final long serialVersionUID = 2591227051998175245L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			rescanDirectory();
		}
	};

	private transient final Action SELECT_ALL_ACTION = new ResourceAction("file_chooser.select_all") {

		private static final long serialVersionUID = 732148144067893679L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			selectAll();
		}
	};

	private transient final Action ORDER_BY_ACTION = new ResourceActionAdapter("file_chooser.sort_by");

	// public FileList() {
	// bookmarkList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	// bookmarkList.setLayoutOrientation(JList.VERTICAL);
	// bookmarkList.setVisibleRowCount(-1);
	// }

	protected ImageIcon getSystemIcon(File file, String filename, boolean isDir, boolean bigIcon) throws Exception {
		if (isDir) {
			ImageIcon icon = getIcon(file, bigIcon);
			if (icon != null) {
				return icon;
			}
			return bigIcon ? BIG_FOLDER_IMAGE : SMALL_FOLDER_IMAGE;
		}

		final String fileExtension;
		if (filename.indexOf('.') > -1) {
			fileExtension = filename.substring(1 + file.getName().indexOf('.'));
		} else {
			fileExtension = "Type is : ." + this.fc.getFileSystemView().getSystemTypeDescription(file);
		}

		if (systemInfoCach.containsKey(fileExtension)) {
			cachSystemDetails(file, filename);
		}

		// That is used instead of an object to store
		// (file extension or type, description, small icon, big icon)
		Object[] tempArray = systemInfoCach.get(fileExtension);

		final int index = bigIcon ? 3 : 2;

		if (tempArray[index] instanceof ImageIcon) {
			return (ImageIcon) tempArray[index];
		}
		ImageIcon icon = getIcon(file, bigIcon);
		if (icon == null) {
			icon = bigIcon ? BIG_FILE_IMAGE : SMALL_FILE_IMAGE;
		}
		tempArray[index] = icon;
		return icon;
	}

	/**
	 * Returns an icon for the file
	 *
	 * @param file
	 * 		the file
	 * @param bigIcon
	 * 		if a big icon is required
	 * @return the icon or {@code null}
	 */
	private ImageIcon getIcon(File file, boolean bigIcon) {
		// Large (and any) image hack for macOS
		if (IS_MAC_OS) {
			final int physicalSize = bigIcon ? SCALED_BIG_ICON_SIZE : SCALED_SMALL_ICON_SIZE;
			Image image = OSXAdapter.createImageOfFile(file, physicalSize, physicalSize);
			if (image != null) {
				final int logicalSize = bigIcon ? BIG_ICON_SIZE : SMALL_ICON_SIZE;
				return new ScaledImageIcon(image, logicalSize, logicalSize);
			}
		}

		// Large file hack for Windows / Linux
		Icon icon = Optional.ofNullable(getShellFolder(file))
				.map(sf -> sf.getIcon(bigIcon))
				.map(ImageIcon::new).orElse(null);

		if (icon == null) {
			icon = fc.getIcon(file);
		}

		if (icon == null) {
			return null;
		}

		// rescale image
		Image image = Tools.asImage(icon);
		try {
			return bigIcon ? Tools.getBigSystemIcon(image) : Tools.getSmallSystemIcon(image);
		} catch (Exception e) {
			LogService.log(LogService.getRoot(), Level.WARNING, e, "com.rapidminer.gui.look.fc.FileList.scaling_failed", file);
			return new ImageIcon(image);
		}
	}

	private void updateViewMenuItemsGroup() {
		Enumeration<AbstractButton> en = viewButtonGroup.getElements();
		while (en.hasMoreElements()) {
			JRadioButtonMenuItem rbm = (JRadioButtonMenuItem) en.nextElement();

			if (rbm.getActionCommand().equals(VIEW_TYPE + filechooserUI.getView())) {
				viewButtonGroup.setSelected(rbm.getModel(), true);
			}
		}
	}

	protected void changeCardForView() {
		stopThumbnailGeneration();

		if (filechooserUI.getView().equals(FileChooserUI.FILECHOOSER_VIEW_DETAILS)) {
			CardLayout cl = (CardLayout) cardPanel.getLayout();
			cl.show(cardPanel, TABLE_SCROLL_PANE);

			updateTableData();
			tableScrollPane.getViewport().setViewPosition(new Point());

			tablePanel.requestFocusInWindow();

		} else {
			CardLayout cl = (CardLayout) cardPanel.getLayout();
			cl.show(cardPanel, BROWSE_SCROLL_PANE);

			itemPanel.updateViewType();
			browseScrollPane.getViewport().setViewPosition(new Point());

			itemPanel.requestFocusInWindow();
		}
		updateViewMenuItemsGroup();
	}

	protected Object[] cachSystemDetails(File file, String filename) {
		final String tempExtension;
		if (filename.indexOf('.') > -1) {
			tempExtension = filename.substring(1 + file.getName().indexOf('.'));
		} else {
			tempExtension = "Type is : ." + fc.getFileSystemView().getSystemTypeDescription(file);
		}

		if (!systemInfoCach.containsKey(tempExtension)) {
			try {
				String tempDesc;
				try {
					tempDesc = fc.getFileSystemView().getSystemTypeDescription(file);
				} catch (Exception exp) {
					tempDesc = filename;
				}

				if (IS_MAC_OS) {
					tempDesc = fc.getUI().getFileView(fc).getDescription(file);
				}

				if (tempDesc == null) {
					tempDesc = filename;
				}

				systemInfoCach.put(tempExtension, new Object[] { tempExtension, tempDesc, null, null });
			} catch (Exception ex) {
				// do nothing
			}
		}
		return systemInfoCach.get(tempExtension);
	}

	private ShellFolder getShellFolder(File f) {
		try {
			return ShellFolder.getShellFolder(f);
		} catch (FileNotFoundException | InternalError e) {
			return null;
		}
	}

	public void addToBookmarks() {
		addToBookmarks(fc.getCurrentDirectory());
	}

	public void addToBookmarks(File newBookmark) {
		newBookmark = canonical(newBookmark);
		String name = SwingTools.showInputDialog("file_chooser.bookmark_name", fc.getFileSystemView()
				.getSystemDisplayName(newBookmark), fc.getFileSystemView().getSystemDisplayName(newBookmark));
		if (name != null && !name.trim().equals("")) {
			bookmarksIO.addToList(name, newBookmark.getPath());
			updateBookmarks();
		}
	}

	private void updateBookmarks() {
		bookmarkListModel.removeAllBookmarks();
		for (Bookmark bookmark : bookmarksIO.getBookmarks()) {
			bookmarkListModel.addBookmark(bookmark);
		}
	}

	public void deleteBookmark(Bookmark bookmark) {
		bookmarksIO.deleteBookmark(bookmark);
		updateBookmarks();
	}

	public void renameBookmark(Bookmark bookmark) {
		Container topLevelAncestor = getTopLevelAncestor();

		BookmarkDialog dialog;
		if (topLevelAncestor instanceof Frame) {
			dialog = new BookmarkDialog((Frame) topLevelAncestor, true);
		} else if (topLevelAncestor instanceof Dialog) {
			dialog = new BookmarkDialog((Dialog) topLevelAncestor, true);
		} else {
			dialog = new BookmarkDialog((Frame) null, true);
		}

		dialog.setLocationRelativeTo(topLevelAncestor);
		dialog.updateDefaults(bookmark.getName(), bookmark.getPath());

		dialog.setVisible(true);

		if (dialog.isNameChanged()) {
			bookmarksIO.renameBookmark(bookmark, dialog.getNewName());
			updateBookmarks();
		}
	}

	public FileList(FileChooserUI tfcui, JFileChooser fc) {
		this.fc = fc;
		this.filechooserUI = tfcui;
		init();
		updateBookmarks();
	}

	private void init() {
		setLayout(new BorderLayout());
		mainSplitPane.setName("");
		mainSplitPane.setAutoscrolls(true);
		mainSplitPane.setBorder(null);
		mainSplitPane.setMinimumSize(new Dimension(40, 25));
		mainSplitPane.setOpaque(true);
		mainSplitPane.setContinuousLayout(false);

		// the bookmarks split divider location
		mainSplitPane.setDividerLocation(170);
		mainSplitPane.setLastDividerLocation(170);

		tableScrollPane.getViewport().setBackground(Color.white);
		tableScrollPane.setFocusable(false);
		tableScrollPane.getVerticalScrollBar().setUnitIncrement(10);
		tableScrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Colors.TAB_BORDER));

		browseScrollPane.setName("");
		browseScrollPane.getVerticalScrollBar().setUnitIncrement(10);
		browseScrollPane.setBorder(null);

		JPanel bookmarkPanel = new JPanel(new BorderLayout());
		JLabel bookmarkLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.label.file_chooser.bookmarks.label"),
				SwingConstants.CENTER) {

			private static final long serialVersionUID = -5903685281066236757L;

			{
				setBorder(BorderFactory.createEmptyBorder(2, 2, 1, 2));
			}

			@Override
			public void paint(Graphics g) {
				int h = this.getHeight();
				int w = this.getWidth();

				Graphics2D g2 = (Graphics2D) g;

				Paint gp = new GradientPaint(0, 0, Colors.TABLE_HEADER_BACKGROUND_GRADIENT_START, 0, h,
						Colors.TABLE_HEADER_BACKGROUND_GRADIENT_END);
				g2.setPaint(gp);
				g2.fill(TableHeaderUI.createHeaderShape(0, 0, w, h, true, true));
				g2.setColor(Colors.TABLE_HEADER_BORDER);
				g2.draw(TableHeaderUI.createHeaderShape(0, 0, w, h, true, true));

				super.paint(g);
			}
		};
		bookmarkPanel.add(bookmarkLabel, BorderLayout.NORTH);
		JScrollPane bookmarkPane = new JScrollPane(bookmarkList);
		bookmarkPane.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 0, Colors.TAB_BORDER));
		bookmarkPanel.add(bookmarkPane, BorderLayout.CENTER);

		tablePanel = new FileTable(this);
		itemPanel = new ItemPanel(this);
		cardPanel.add(browseScrollPane, BROWSE_SCROLL_PANE);
		browseScrollPane.setActionMap(null);
		cardPanel.add(tableScrollPane, TABLE_SCROLL_PANE);
		tableScrollPane.setActionMap(null);
		tableScrollPane.getViewport().add(tablePanel);
		browseScrollPane.getViewport().add(itemPanel);

		// no bookmarks for remote files
		if (fc.getFileSystemView() instanceof RemoteFileSystemView) {
			setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));
			add(cardPanel, BorderLayout.CENTER);
		} else {
			add(mainSplitPane, BorderLayout.CENTER);
			mainSplitPane.add(cardPanel, JSplitPane.RIGHT);
			mainSplitPane.add(bookmarkPanel, JSplitPane.LEFT);
		}

		fc.setPreferredSize(new Dimension(780, 510));
		updateTablePanelSize();

		this.cardPanel.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				FileList.this.itemPanel.arrangeTheFiles((int) FileList.this.cardPanel.getSize().getWidth());
			}
		});

		this.tableScrollPane.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				updateTablePanelSize();
				if (FileChooserUI.FILECHOOSER_VIEW_DETAILS.equals(filechooserUI.viewType)) {
					tablePanel.requestFocusInWindow();
				}
			}
		});
	}

	protected void updateTablePanelSize() {
		if (tablePanel.getInitialHeight() < tableScrollPane.getSize().getHeight()) {
			tablePanel.setPreferredSize(new Dimension(tablePanel.getWidth(), tableScrollPane.getHeight()
					- tablePanel.getTableHeader().getHeight()));
			tablePanel.setMinimumSize(tablePanel.getPreferredSize());
			tablePanel.setSize(tablePanel.getPreferredSize());
		} else {
			tablePanel.setPreferredSize(new Dimension(tablePanel.getWidth(), tablePanel.getInitialHeight()));
			tablePanel.setMinimumSize(tablePanel.getPreferredSize());
			tablePanel.setSize(tablePanel.getPreferredSize());
		}
	}

	public void generateThumbs() {
		thumbGenerator = new ThumbGeneratorThread(itemPanel);
		thumbGenerator.start();
	}

	public void setSelectedFile(File f) {
		filechooserUI.setCurrentDirectoryOfFileChooser(f);
	}

	public void updatePath(File file) {
		fc.setCursor(waitCursor);
		clearEveryThing();
		exploreFolder(file);
		fc.setCursor(normalCursor);
	}

	@SuppressWarnings("deprecation")
	protected void updateThumbnail() {
		if (FileChooserUI.FILECHOOSER_VIEW_THUMBNAIL.equals(itemPanel.getCurrentView())) {
			thumbGenerator.stop();
			thumbGenerator = new ThumbGeneratorThread(this.itemPanel);
			thumbGenerator.start();
		}
	}

	public void rescanDirectory() {
		File currentDirectory = canonical(this.fc.getCurrentDirectory());

		File[] files = fc.getFileSystemView().getFiles(currentDirectory, false);
		Set<File> allItems = new HashSet<>();
		for (Item item : completeItemsList) {
			allItems.add(item.getFile());
		}

		Set<File> currentFiles = new HashSet<>();
		for (File file : files) {
			file = canonical(file);
			currentFiles.add(file);
			if (!allItems.contains(file)) {
				Item item = new Item(itemPanel, file);
				completeItemsList.add(item);
				visibleItemsList.add(item);
				item.addKeyListener(this.keyListener);
				itemPanel.add(item);
				itemPanel.findBestConfig(item);
				itemPanel.repaint();
				scrollRectToVisible(item.getBounds());
			}
		}

		// deleted items
		List<Item> removingItems = new ArrayList<>();

		for (Item item : completeItemsList) {
			if (!currentFiles.contains(item.getFile())) {
				removingItems.add(item);
			}
		}

		for (Item item : removingItems) {
			completeItemsList.remove(item);
			visibleItemsList.remove(item);
			itemPanel.remove(item);
			selectedFilesVector.remove(item);
		}

		if (FileChooserUI.FILECHOOSER_VIEW_DETAILS.equals(filechooserUI.getView())) {
			updateTableData();
		} else if (FileChooserUI.FILECHOOSER_VIEW_THUMBNAIL.equals(filechooserUI.getView())) {
			stopThumbnailGeneration();
			generateThumbs();
		}

		orderBy(ORDER_BY, true);
		arrangeTheFiles();

		repaint();
	}

	private void exploreFolder(File folder) {
		folder = canonical(folder);
        boolean hasPreviousFile = currentFile != null;
		if (hasPreviousFile) {
			filechooserUI.backPathVector.add(currentFile.getPath());
		}
		filechooserUI.getGoBackAction().setEnabled(hasPreviousFile);

		currentFile = folder;

		File[] files = fc.getFileSystemView().getFiles(folder, false);
		if (files != null) {
			for (File file : files) {
				file = canonical(file);

				if (FILE_CHOOSER_ROOT.equals(currentFile.toString()) || (file.exists() && file.canRead())) {
					Item item = new Item(itemPanel, file);
					item.addKeyListener(keyListener);
					completeItemsList.add(item);
				}
			}
		}

		doDefaults();
		requestFocus();
		repaint();
	}

	public FileSystemView getFSV() {
		return this.fc.getFileSystemView();
	}

	@SuppressWarnings("deprecation")
	public void stopThumbnailGeneration() {
		thumbGenerator.stop();
		thumbGenerator = new ThumbGeneratorThread(this.itemPanel);
	}

	// //------------------------------------------------------------------------------

	public void clearEveryThing() {
		itemPanel.removeAll();
		lastSelected = null;

		completeItemsList.removeAllElements();
		visibleItemsList.removeAllElements();
		selectedFilesVector.removeAllElements();

		// this.fc.setSelectedFiles(new File[] {});
		completeItemsList.removeAllElements();

		for (Item t : completeItemsList) {
			t.finalizeAll();
		}
	}

	public void clearSelectedItemsList() {
		for (Item item : selectedFilesVector) {
			item.updateSelectionMode(false);
			item.repaint();
		}

		selectedFilesVector.clear();

		fc.setSelectedFiles(null);
		repaint();
	}

	public boolean isItemSelected(Item item) {
		return selectedFilesVector.contains(item);
	}

	protected void updateFilechooserSelectedItems(Item t, boolean ctrl) {
		if (!ctrl || !fc.isMultiSelectionEnabled()) {
			for (Item item : completeItemsList) {
				item.updateSelectionMode(false);
			}
			selectedFilesVector.removeAllElements();
		}

		t.updateSelectionMode(!t.getSelectionMode());

		if (t.getSelectionMode()) {
			itemPanel.scrollRectToVisible(t.getBounds());
			if (!selectedFilesVector.contains(t)) {
				selectedFilesVector.add(t);
			}
		} else {
			selectedFilesVector.remove(t);
		}
		lastSelected = t;
		synchFilechoserSelection();
	}

	@Override
	public void requestFocus() {
		if (FileChooserUI.FILECHOOSER_VIEW_DETAILS.equals(filechooserUI.viewType)) {
			tablePanel.requestFocus();
		} else {
			itemPanel.requestFocus();
		}
	}

	protected void synchFilechoserSelection() {
		List<File> files = new ArrayList<>();

		for (Item item : selectedFilesVector) {
			if (fc.isDirectorySelectionEnabled() && item.isDirectory()
					|| fc.isFileSelectionEnabled() && !item.isDirectory()) {
				files.add(item.getFile());
			}
		}

		fc.setSelectedFiles(files.toArray(new File[0]));
	}

	protected JPopupMenu getPanePopup() {
		if (panePopup != null) {
			return panePopup;
		}

		MenuListener menuListener = new MenuListener(this);
		panePopup = new JPopupMenu();

		// for remote files only show new Folder if it is possible to create one
		JMenuItem menuItem;
		if (!(fc.getFileSystemView() instanceof RemoteFileSystemView && !((RemoteFileSystemView) this.fc
				.getFileSystemView()).isCreatingNewFolderEnabled())) {
			menuItem = new JMenuItem(filechooserUI.NEW_FOLDER_ACTION);
			panePopup.add(menuItem);
		}

		// no bookmarks or hidden files for remote files
		if (!(fc.getFileSystemView() instanceof RemoteFileSystemView)) {
			menuItem = new JMenuItem(filechooserUI.ADD_TO_BOOKMARKS_ACTION);
			panePopup.add(menuItem);

			panePopup.addSeparator();

			showHiddenMenuItem = new JCheckBoxMenuItem(SHOW_HIDDEN_ACTION);
			showHiddenMenuItem.setSelected(!fc.isFileHidingEnabled());
			panePopup.add(this.showHiddenMenuItem);
		}

		menuItem = new JMenuItem(REFRESH_ACTION);
		panePopup.add(menuItem);

		menuItem = new JMenuItem(SELECT_ALL_ACTION);
		panePopup.add(menuItem);

		JMenu orderMenuItem = new JMenu(ORDER_BY_ACTION);
		panePopup.add(orderMenuItem);
		orderButtonGroup = new ButtonGroup();

		menuItem = new JRadioButtonMenuItem(FileList.ORDER_BY_FILE_NAME);
		menuItem.setSelected(true);
		menuItem.addActionListener(menuListener);
		menuItem.setActionCommand(ORDER + FileList.ORDER_BY_FILE_NAME);
		orderMenuItem.add(menuItem);
		orderButtonGroup.add(menuItem);

		menuItem = new JRadioButtonMenuItem(FileList.ORDER_BY_FILE_TYPE);
		menuItem.addActionListener(menuListener);
		menuItem.setActionCommand(ORDER + FileList.ORDER_BY_FILE_TYPE);
		orderMenuItem.add(menuItem);
		orderButtonGroup.add(menuItem);

		menuItem = new JRadioButtonMenuItem(FileList.ORDER_BY_FILE_SIZE);
		menuItem.addActionListener(menuListener);
		menuItem.setActionCommand(ORDER + FileList.ORDER_BY_FILE_SIZE);
		orderMenuItem.add(menuItem);
		orderButtonGroup.add(menuItem);

		menuItem = new JRadioButtonMenuItem(FileList.ORDER_BY_FILE_MODIFIED);
		menuItem.addActionListener(menuListener);
		menuItem.setActionCommand(ORDER + FileList.ORDER_BY_FILE_MODIFIED);
		orderMenuItem.add(menuItem);
		orderButtonGroup.add(menuItem);

		panePopup.addSeparator();

		JMenu viewMenuItem = new JMenu(filechooserUI.CHANGE_VIEW_ACTION);
		panePopup.add(viewMenuItem);
		viewButtonGroup = new ButtonGroup();

		menuItem = new JRadioButtonMenuItem(FileChooserUI.FILECHOOSER_VIEW_THUMBNAIL);
		menuItem.addActionListener(menuListener);
		menuItem.setActionCommand(VIEW_TYPE + FileChooserUI.FILECHOOSER_VIEW_THUMBNAIL);
		viewMenuItem.add(menuItem);
		viewButtonGroup.add(menuItem);

		menuItem = new JRadioButtonMenuItem(FileChooserUI.FILECHOOSER_VIEW_ICON);
		menuItem.addActionListener(menuListener);
		menuItem.setActionCommand(VIEW_TYPE + FileChooserUI.FILECHOOSER_VIEW_ICON);
		viewMenuItem.add(menuItem);
		viewButtonGroup.add(menuItem);

		menuItem = new JRadioButtonMenuItem(FileChooserUI.FILECHOOSER_VIEW_LIST);
		menuItem.addActionListener(menuListener);
		menuItem.setSelected(true);
		menuItem.setActionCommand(VIEW_TYPE + FileChooserUI.FILECHOOSER_VIEW_LIST);
		viewMenuItem.add(menuItem);
		viewButtonGroup.add(menuItem);

		menuItem = new JRadioButtonMenuItem(FileChooserUI.FILECHOOSER_VIEW_DETAILS);
		menuItem.addActionListener(menuListener);
		menuItem.setActionCommand(VIEW_TYPE + FileChooserUI.FILECHOOSER_VIEW_DETAILS);
		viewMenuItem.add(menuItem);
		viewButtonGroup.add(menuItem);

		viewMenuItem.addSeparator();

		autoArrangeCheckBox = new JCheckBoxMenuItem(new ResourceActionAdapter("file_chooser.auto_arrange"));
		autoArrangeCheckBox.setSelected(true);
		viewMenuItem.add(autoArrangeCheckBox);
		return panePopup;

	}

	public boolean isAutoArrange() {
		return autoArrangeCheckBox.isSelected();
	}

	public void orderBy(String newOrderBy, boolean newPath) {
		if (!newPath && ORDER_BY.equals(newOrderBy)) {
			// toggle asc/desc
			Vector<Item> newList = new Vector<>(20);
			for (int i = completeItemsList.size() - 1; i >= 0; i--) {
				newList.add(completeItemsList.elementAt(i));
			}
			completeItemsList = newList;
		} else {
			Item[] allItems = completeItemsList.toArray(new Item[0]);
			for (Item allItem : allItems) {
				allItem.setCompare_type(newOrderBy);
			}
			Arrays.sort(allItems);
			completeItemsList = new Vector<>(Arrays.asList(allItems));
		}
		ORDER_BY = newOrderBy;

		JRadioButtonMenuItem rbm;
		Enumeration<AbstractButton> en = orderButtonGroup.getElements();
		while (en.hasMoreElements()) {
			rbm = (JRadioButtonMenuItem) en.nextElement();

			if (rbm.getActionCommand().equals(ORDER + getOrder())) {
				orderButtonGroup.setSelected(rbm.getModel(), true);
			}
		}
		findVisibleItems();
	}

	private void arrangeTheFiles() {
		if (FileChooserUI.FILECHOOSER_VIEW_DETAILS.equals(filechooserUI.viewType)) {
			changeCardForView();
		} else {
			itemPanel.arrangeTheFiles();
		}
	}

	public void doFilterChanged() {
		findVisibleItems();
		changeCardForView();

		if (!visibleItemsList.isEmpty()) {
			lastSelected = visibleItemsList.get(0);
		}
	}

	public void selectAll() {
		if (fc.isMultiSelectionEnabled()) {
			if (FileChooserUI.FILECHOOSER_VIEW_DETAILS.equals(filechooserUI.getView())) {
				tablePanel.selectAll();
			} else {
				clearSelectedItemsList();
				for (Item item : visibleItemsList) {
					item.updateSelectionMode(true);
					selectedFilesVector.add(item);
					item.repaint();
				}
				synchFilechoserSelection();
			}
		}
	}

	public void doDefaults() {
		orderBy(ORDER_BY, true);
		changeCardForView();
		if (!visibleItemsList.isEmpty()) {
			lastSelected = visibleItemsList.get(0);
		}
	}

	protected void findVisibleItems() {
		visibleItemsList.clear();
		itemPanel.removeAll();

		for (Item item : completeItemsList) {
			if ((fc.isFileHidingEnabled() && item.getFile().isHidden())
					|| (fc.getFileFilter() != null && !fc.getFileFilter().accept(item.getFile()))
					|| (!fc.isFileSelectionEnabled() && item.getFile().isFile())) {
				item.setVisible(false);
			} else {
				item.setVisible(true);
				visibleItemsList.add(item);
				itemPanel.add(item);
			}
		}
	}

	protected void updateTableData() {

		int ni = 0;
		int si = 1;
		int ti = 2;
		int li = 3;

		try {
			ni = tablePanel.getColumnModel().getColumnIndex(FileTable.FILE_NAME);
		} catch (Exception exp) {
			// do nothing
		}
		try {
			si = tablePanel.getColumnModel().getColumnIndex(FileTable.SIZE);
		} catch (Exception exp) {
			// do nothing
		}
		try {
			ti = tablePanel.getColumnModel().getColumnIndex(FileTable.TYPE);
		} catch (Exception exp) {
			// do nothing
		}
		try {
			li = tablePanel.getColumnModel().getColumnIndex(FileTable.LAST_MODIFIED);
		} catch (Exception exp) {
			// do nothing
		}

		List<Object[]> allItems = new ArrayList<>();
		for (Item tc : visibleItemsList) {
			Object[] item = new Object[4];
			item[ni] = new FileTableLabel(tc.getFileName(), tc.getSmallSystemIcon(), SwingConstants.LEFT);
			item[si] = tc.convertToCorrectFormat(tc.getFileSize());
			item[ti] = tc.getFileType();
			if (tc.getLastModificationTime() > 0) {
				item[li] = java.text.DateFormat.getDateInstance().format(new Date(tc.getLastModificationTime()));
			} else {
				item[li] = "";
			}

			allItems.add(item);
		}

		updateTablePanelSize();
		tablePanel.updateData(allItems.toArray(new Object[0][]));
		updateTablePanelSize();

		for (Item item : selectedFilesVector) {
			int index = visibleItemsList.indexOf(item);
			if (index >= 0) {
				tablePanel.updateSelectionInterval(index, true);
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(FileChooserUI.FILECHOOSER_VIEW_TYPE)) {
			filechooserUI.updateView(evt.getNewValue().toString());
		}
	}

	public String getOrder() {
		return ORDER_BY;
	}

	/**
	 * Converts the file into a canonical file if possible
	 *
	 * @param file
	 * 		the file
	 * @return the canonical file, or the file
	 */
	private File canonical(File file) {
		try {
			file = file.getCanonicalFile();
		} catch (IOException ex) {
			// might also work without canonical
		}
		return file;
	}
}
