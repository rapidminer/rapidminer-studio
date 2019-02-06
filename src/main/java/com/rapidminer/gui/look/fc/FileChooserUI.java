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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.ActionMapUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicFileChooserUI;

import com.rapidminer.gui.LoggedAbstractAction;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.look.borders.Borders;
import com.rapidminer.gui.tools.ExtendedJToolBar;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ResourceActionTransmitter;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.DropDownPopupButton;
import com.rapidminer.gui.tools.components.DropDownPopupButton.PopupMenuProvider;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.io.remote.RemoteFileSystemView;
import com.rapidminer.tools.I18N;

import sun.awt.shell.ShellFolder;


/**
 * The UI for the extended file chooser.
 *
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class FileChooserUI extends BasicFileChooserUI {

	public static final String FILECHOOSER_VIEW_TYPE = "FILECHOOSER_VIEW_TYPE";

	public static final String FILECHOOSER_VIEW_THUMBNAIL = I18N.getMessage(I18N.getGUIBundle(),
			"gui.menu.file_chooser.view.thumbnails.label");

	public static final String FILECHOOSER_VIEW_ICON = I18N.getMessage(I18N.getGUIBundle(),
			"gui.menu.file_chooser.view.icons.label");

	public static final String FILECHOOSER_VIEW_LIST = I18N.getMessage(I18N.getGUIBundle(),
			"gui.menu.file_chooser.view.list.label");

	public static final String FILECHOOSER_VIEW_DETAILS = I18N.getMessage(I18N.getGUIBundle(),
			"gui.menu.file_chooser.view.details.label");

	public static final Icon FILECHOOSER_OPEN_ICON = SwingTools.createIcon("24/folder_open.png");

	public static final Icon FILECHOOSER_SELECT_ICON = SwingTools.createIcon("24/folder_open.png");

	public static final Icon FILECHOOSER_SAVE_ICON = SwingTools.createIcon("24/floppy_disk.png");

	public static final Icon FILECHOOSER_CLOSE_ICON = SwingTools.createIcon("24/delete.png");

	/**
	 * Creates a new folder.
	 */
	private class NewFolderAction extends ResourceAction {

		private static final long serialVersionUID = -119998626996460617L;

		protected NewFolderAction() {
			super("file_chooser.new_folder");
		}

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			if (UIManager.getBoolean("FileChooser.readOnly")) {
				return;
			}
			JFileChooser fc = getFileChooser();
			File currentDirectory = fc.getCurrentDirectory();
			FileSystemView fsv = fc.getFileSystemView();
			File newFolder = null;

			String name = SwingTools.showInputDialog("file_chooser.new_folder", "");

			// abort if cancelled or user entered nothing
			if (name == null || name.isEmpty()) {
				return;
			}

			try {
				newFolder = fsv.createNewFolder(currentDirectory);
				if (newFolder.renameTo(fsv.createFileObject(fsv.getParentDirectory(newFolder), name))) {
					newFolder = fsv.createFileObject(fsv.getParentDirectory(newFolder), name);
				} else {
					SwingTools.showVerySimpleErrorMessage("file_chooser.new_folder.rename", name);
				}
			} catch (IOException exc) {
				SwingTools.showVerySimpleErrorMessage("file_chooser.new_folder.create", name);
				return;
			} catch (Exception exp) {
				// do nothing
			}

			if (fc.isMultiSelectionEnabled()) {
				fc.setSelectedFiles(new File[] { newFolder });
			} else {
				fc.setSelectedFile(newFolder);
			}

			fc.rescanCurrentDirectory();
		}
	}

	private static class AlignedLabel extends JLabel {

		private static final long serialVersionUID = 4912090609095372381L;

		private AlignedLabel[] group;

		private int maxWidth = 0;

		AlignedLabel(String text) {
			super(text);
			setAlignmentX(Component.LEFT_ALIGNMENT);
		}

		@Override
		public Dimension getPreferredSize() {
			Dimension d = super.getPreferredSize();
			// Align the width with all other labels in group.
			return new Dimension(getMaxWidth(), d.height);
		}

		private int getMaxWidth() {
			if (this.maxWidth == 0 && this.group != null) {
				int max = 0;
				for (AlignedLabel element : this.group) {
					max = Math.max(element.getSuperPreferredWidth(), max);
				}
				for (AlignedLabel element : this.group) {
					element.maxWidth = max;
				}
			}
			return this.maxWidth;
		}

		private int getSuperPreferredWidth() {
			if (getText() == null) {
				return super.getPreferredSize().width;
			} else {
				return super.getPreferredSize().width + 11;
			}
		}
	}

	private class DirectoryComboBoxAction extends LoggedAbstractAction {

		private static final long serialVersionUID = -6851838331146924117L;

		protected DirectoryComboBoxAction() {
			super("DirectoryComboBoxAction");
		}

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			File f = (File) FileChooserUI.this.directoryComboBox.getSelectedItem();
			setCurrentDirectoryOfFileChooser(f);
			FileChooserUI.this.fileNameTextField.requestFocus();
		}
	}

	private class FilterComboBoxModel extends AbstractListModel<Object> implements ComboBoxModel<Object>,
			PropertyChangeListener {

		private static final long serialVersionUID = -7578988904254755349L;

		private FileFilter[] filters;

		protected FilterComboBoxModel() {
			super();
			this.filters = getFileChooser().getChoosableFileFilters();
		}

		@Override
		public void propertyChange(PropertyChangeEvent e) {
			String prop = e.getPropertyName();
			if (prop == JFileChooser.CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY) {
				this.filters = (FileFilter[]) e.getNewValue();
				fireContentsChanged(this, -1, -1);
			} else if (prop == JFileChooser.FILE_FILTER_CHANGED_PROPERTY) {
				fireContentsChanged(this, -1, -1);
			}
		}

		@Override
		public void setSelectedItem(Object filter) {
			if (filter != null) {
				getFileChooser().setFileFilter((FileFilter) filter);
				setFileName(null);
				fireContentsChanged(this, -1, -1);
			}
		}

		@Override
		public Object getSelectedItem() {
			FileFilter currentFilter = getFileChooser().getFileFilter();
			boolean found = false;
			if (currentFilter != null) {
				for (FileFilter element : this.filters) {
					if (element == currentFilter) {
						found = true;
					}
				}
				if (found == false) {
					getFileChooser().addChoosableFileFilter(currentFilter);
				}
			}
			return getFileChooser().getFileFilter();
		}

		@Override
		public int getSize() {
			if (this.filters != null) {
				return this.filters.length;
			} else {
				return 0;
			}
		}

		@Override
		public Object getElementAt(int index) {
			if (index > getSize() - 1) {
				// This shouldn't happen. Try to recover gracefully.
				return getFileChooser().getFileFilter();
			}
			if (this.filters != null) {
				return this.filters[index];
			} else {
				return null;
			}
		}
	}

	private class FilterComboBoxRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 7024419790190737084L;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value != null && value instanceof FileFilter) {
				setText(((FileFilter) value).getDescription());
			}
			if (isSelected && index > -1) {
				setBorder(FileChooserUI.this.roundComboboxListRendererBorder);
			}
			return this;
		}
	}

	private class DirectoryComboBoxModel extends AbstractListModel<Object> implements ComboBoxModel<Object> {

		private static final long serialVersionUID = -7566898679781533334L;

		private Vector<File> directories = new Vector<File>();

		private int[] depths = null;

		private File selectedDirectory = null;

		private JFileChooser chooser = getFileChooser();

		private FileSystemView fileSystemView = this.chooser.getFileSystemView();

		public DirectoryComboBoxModel() {
			File dir = getFileChooser().getCurrentDirectory();
			if (dir != null) {
				addItem(dir);
			}
		}

		private void addItem(File directory) {
			if (directory == null) {
				return;
			}

			this.directories.clear();

			File[] baseFolders = this.fileSystemView.getRoots();
			this.directories.addAll(Arrays.asList(baseFolders));

			File canonical = null;
			try {
				canonical = directory.getCanonicalFile();
			} catch (IOException e) {
				canonical = directory;
			}
			File sf;
			if (!(this.fileSystemView instanceof RemoteFileSystemView)) {
				try {
					sf = ShellFolder.getShellFolder(canonical);
				} catch (FileNotFoundException ex) {
					sf = canonical;
				}
			} else {
				sf = canonical;
			}
			File f = sf;
			Vector<File> path = new Vector<File>(10);
			do {
				path.addElement(f);
			} while ((f = fileSystemView.getParentDirectory(f)) != null);

			int pathCount = path.size();
			for (int i = 0; i < pathCount; i++) {
				f = path.get(i);
				if (this.directories.contains(f)) {
					int topIndex = this.directories.indexOf(f);
					for (int j = i - 1; j >= 0; j--) {
						this.directories.insertElementAt(path.get(j), topIndex + i - j);
					}
					break;
				}
			}
			calculateDepths();
			setSelectedItem(sf);
		}

		private void calculateDepths() {
			this.depths = new int[this.directories.size()];
			for (int i = 0; i < this.depths.length; i++) {
				File dir = this.directories.get(i);
				File parent = dir.getParentFile();
				this.depths[i] = 0;
				if (parent != null) {
					for (int j = i - 1; j >= 0; j--) {
						if (parent.equals(this.directories.get(j))) {
							this.depths[i] = this.depths[j] + 1;
							break;
						}
					}
				}
			}
		}

		public int getDepth(int i) {
			return this.depths != null && i >= 0 && i < this.depths.length ? this.depths[i] : 0;
		}

		@Override
		public void setSelectedItem(Object selectedDirectory) {
			this.selectedDirectory = (File) selectedDirectory;
			fireContentsChanged(this, -1, -1);
		}

		@Override
		public Object getSelectedItem() {
			return this.selectedDirectory;
		}

		@Override
		public int getSize() {
			return this.directories.size();
		}

		@Override
		public Object getElementAt(int index) {
			return this.directories.elementAt(index);
		}
	}

	private static class IndentIcon implements Icon {

		private Icon icon;

		private int depth = 0;

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			if (icon != null) {
				if (c.getComponentOrientation().isLeftToRight()) {
					this.icon.paintIcon(c, g, x + this.depth * INDENT_SPACE, y);
				} else {
					this.icon.paintIcon(c, g, x, y);
				}
			}
		}

		@Override
		public int getIconWidth() {
			if (icon == null) {
				return depth * INDENT_SPACE;
			} else {
				return this.icon.getIconWidth() + this.depth * INDENT_SPACE;
			}
		}

		@Override
		public int getIconHeight() {
			if (icon == null) {
				return 0;
			} else {
				return this.icon.getIconHeight();
			}
		}
	}

	private class DirectoryComboBoxRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 4597909127976297943L;

		private IndentIcon indentIcon = new IndentIcon();

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if (value == null) {
				setText("");
				return this;
			}
			File directory = (File) value;
			setText(getFileChooser().getName(directory));
			Icon icon = getFileChooser().getIcon(directory);
			if (icon == null) {
				icon = UIManager.getIcon("FileChooser.defaultDirectoryIcon");
			}

			this.indentIcon.icon = icon;
			this.indentIcon.depth = FileChooserUI.this.directoryComboBoxModel.getDepth(index);
			setIcon(this.indentIcon);

			if (isSelected && index > -1) {
				setBorder(FileChooserUI.this.roundComboboxListRendererBorder);
			}

			return this;
		}
	}

	private class RapidLookFileView extends BasicFileView {

		@Override
		public Icon getIcon(File f) {
			Icon icon = getCachedIcon(f);
			if (icon != null) {
				return icon;
			}
			icon = getFileChooser().getFileSystemView().getSystemIcon(f);
			if (icon == null) {
				icon = super.getIcon(f);
			}
			cacheIcon(f, icon);
			return icon;
		}
	}

	private class BookmarkAction extends ResourceAction {

		private static final long serialVersionUID = -654304868192207741L;

		public BookmarkAction() {
			super("file_chooser.add_to_bookmarks");
		}

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			FileChooserUI.this.fileList.addToBookmarks();
		}
	}

	private class GoBackAction extends ResourceAction {

		private static final long serialVersionUID = 5132122622014626886L;

		protected GoBackAction() {
			super("file_chooser.go_back");
		}

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			goBack();
		}
	}

	private class CancelSelectionAction extends LoggedAbstractAction {

		private static final long serialVersionUID = 2080395201063859907L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			FileChooserUI.this.fileList.stopThumbnailGeneration();
			getFileChooser().cancelSelection();
		}
	}

	private class ExtendedApproveSelectionAction extends ApproveSelectionAction {

		private static final long serialVersionUID = 4061933557078579689L;

		@Override
		public void actionPerformed(ActionEvent e) {
			FileChooserUI.this.fileList.stopThumbnailGeneration();
			super.actionPerformed(e);
		}
	}

	private class ChangeToParentDirectoryAction extends ResourceAction {

		private static final long serialVersionUID = -3805300411336163058L;

		protected ChangeToParentDirectoryAction() {
			super("file_chooser.change_to_parent_directory");
		}

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			getFileChooser().changeToParentDirectory();
		}
	}

	private class changeViewActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			updateView(e.getActionCommand());
		}
	}

	private static class ChangeViewAction extends ResourceActionAdapter {

		private static final long serialVersionUID = 6720057807081456009L;

		public ChangeViewAction() {
			super("file_chooser.view");
		}
	}

	public transient final Action NEW_FOLDER_ACTION = new NewFolderAction();

	public final ResourceActionAdapter CHANGE_VIEW_ACTION = new ChangeViewAction();

	public final Action ADD_TO_BOOKMARKS_ACTION = new BookmarkAction();

	public final Action GO_BACK_ACTION = new GoBackAction();

	public final Action GO_HOME_ACTION = new ResourceActionTransmitter("file_chooser.go_home", super.getGoHomeAction());

	public final Action CHANGE_TO_PARENT_DIRECTORY = new ChangeToParentDirectoryAction();

	private static File userHomeDirectory;

	private final static int INDENT_SPACE = 10;

	private ButtonGroup changeViewButtonGroup;

	public String viewType = FILECHOOSER_VIEW_DETAILS;

	protected Vector<String> backPathVector = new Vector<String>();

	protected JButton bookmarksButton;

	private JPopupMenu changeViewPopup;

	private final Border roundComboboxListRendererBorder = Borders.getComboBoxListCellRendererFocusBorder();

	private DropDownPopupButton changeViewButton;

	protected FileList fileList;

	private JLabel lookInLabel;

	private JComboBox<Object> directoryComboBox;

	private DirectoryComboBoxModel directoryComboBoxModel;

	private Action directoryComboBoxAction = new DirectoryComboBoxAction();

	private FilterComboBoxModel filterComboBoxModel;

	private JTextField fileNameTextField;

	private JButton approveButton;

	private JButton cancelButton;

	private JPanel buttonPanel;

	private JPanel bottomPanel;

	private JComboBox<Object> filterComboBox;

	private int lookInLabelMnemonic = 0;

	private String lookInLabelText = null;

	private String saveInLabelText = null;

	private int fileNameLabelMnemonic = 0;

	private String fileNameLabelText = null;

	private int filesOfTypeLabelMnemonic = 0;

	private String filesOfTypeLabelText = null;

	private BasicFileView fileView = new RapidLookFileView();

	private Action cancelSelectionAction = new CancelSelectionAction();

	private Action approveSelectionAction = new ExtendedApproveSelectionAction();

	private boolean selected = false;

	private LinkedList<ChangeListener> listeners = new LinkedList<ChangeListener>();

	public void addChangeListener(ChangeListener l) {
		listeners.add(l);
	}

	public void removeChangeListener(ChangeListener l) {
		listeners.remove(l);
	}

	protected ActionMap createActions() {
		final Action escAction = new AbstractAction() {

			private static final long serialVersionUID = -3976059968191425942L;

			@Override
			public void actionPerformed(ActionEvent e) {
				FileChooserUI.this.fileList.stopThumbnailGeneration();
				getFileChooser().cancelSelection();
			}

			@Override
			public boolean isEnabled() {
				return getFileChooser().isEnabled();
			}
		};
		final ActionMap map = new ActionMapUIResource();
		map.put("approveSelection", getApproveSelectionAction());
		map.put("cancelSelection", escAction);
		return map;
	}

	@Override
	public Action getCancelSelectionAction() {
		return this.cancelSelectionAction;
	}

	@Override
	public Action getApproveSelectionAction() {
		return this.approveSelectionAction;
	}

	public String getView() {
		return this.viewType;
	}

	public static ComponentUI createUI(JComponent c) {
		return new FileChooserUI((JFileChooser) c);
	}

	public FileChooserUI(JFileChooser filechooser) {
		super(filechooser);
	}

	@Override
	public void installUI(JComponent c) {
		super.installUI(c);
	}

	@Override
	public void uninstallComponents(JFileChooser fc) {
		fc.removeAll();
		this.bottomPanel = null;
		this.buttonPanel = null;
		this.fileList = null;
		super.uninstallComponents(fc);
	}

	@Override
	public void installComponents(JFileChooser fc) {
		FileSystemView fsv = fc.getFileSystemView();
		userHomeDirectory = fsv.getHomeDirectory();

		this.changeViewPopup = createViewPopupMenu();

		fc.setLayout(new BorderLayout());

		// ********************************* //
		// **** Construct the top panel **** //
		// ********************************* //

		// Directory manipulation buttons
		JToolBar topPanel = new ExtendedJToolBar();
		topPanel.setFloatable(false);
		topPanel.setBorder(null);
		topPanel.setOpaque(false);

		JPanel panel = new JPanel(new BorderLayout(0, ButtonDialog.GAP));
		panel.add(topPanel, BorderLayout.NORTH);

		// ComboBox Label
		this.lookInLabel = new JLabel(this.lookInLabelText);
		this.lookInLabel.setDisplayedMnemonic(this.lookInLabelMnemonic);
		topPanel.add(this.lookInLabel, BorderLayout.BEFORE_LINE_BEGINS);

		// CurrentDir ComboBox
		this.directoryComboBox = new JComboBox<>();
		this.directoryComboBox.setOpaque(false);
		this.directoryComboBox.getAccessibleContext().setAccessibleDescription(this.lookInLabelText);
		this.directoryComboBox.putClientProperty("JComboBox.lightweightKeyboardNavigation", "Lightweight");
		this.lookInLabel.setLabelFor(this.directoryComboBox);
		this.directoryComboBoxModel = createDirectoryComboBoxModel(fc);
		this.directoryComboBox.setModel(this.directoryComboBoxModel);
		this.directoryComboBox.addActionListener(this.directoryComboBoxAction);
		this.directoryComboBox.setRenderer(createDirectoryComboBoxRenderer(fc));
		this.directoryComboBox.setMaximumRowCount(9);
		topPanel.add(this.directoryComboBox);
		topPanel.addSeparator();

		// no back button for remote files
		if (!(fsv instanceof RemoteFileSystemView)) {
			// back button
			JButton backButton = new JButton(GO_BACK_ACTION);
			backButton.setText(null);
			backButton.setRolloverEnabled(true);
			backButton.setIcon((Icon) GO_BACK_ACTION.getValue(Action.SMALL_ICON));
			backButton.setToolTipText((String) GO_BACK_ACTION.getValue(Action.SHORT_DESCRIPTION));
			backButton.getAccessibleContext().setAccessibleName((String) GO_BACK_ACTION.getValue(Action.ACCELERATOR_KEY));
			backButton.setAlignmentX(Component.LEFT_ALIGNMENT);
			backButton.setAlignmentY(Component.CENTER_ALIGNMENT);
			backButton.setBackground((Color) UIManager.get("control"));
			backButton.setOpaque(false);
			backButton.setFocusable(false);

			topPanel.add(backButton);
		}

		// Up Button
		JButton upFolderButton = new JButton(CHANGE_TO_PARENT_DIRECTORY);
		upFolderButton.setText(null);
		upFolderButton.setRolloverEnabled(true);
		upFolderButton.setIcon((Icon) CHANGE_TO_PARENT_DIRECTORY.getValue(Action.SMALL_ICON));
		upFolderButton.setToolTipText((String) CHANGE_TO_PARENT_DIRECTORY.getValue(Action.SHORT_DESCRIPTION));
		upFolderButton.getAccessibleContext().setAccessibleName(
				(String) CHANGE_TO_PARENT_DIRECTORY.getValue(Action.ACCELERATOR_KEY));
		upFolderButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		upFolderButton.setAlignmentY(Component.CENTER_ALIGNMENT);
		upFolderButton.setBackground((Color) UIManager.get("control"));
		upFolderButton.setFocusable(false);
		upFolderButton.setOpaque(false);

		topPanel.add(upFolderButton);

		// no bookmarks for remote files
		if (!(fsv instanceof RemoteFileSystemView)) {
			this.bookmarksButton = new JButton(ADD_TO_BOOKMARKS_ACTION);
			this.bookmarksButton.setText(null);
			this.bookmarksButton.setRolloverEnabled(true);
			this.bookmarksButton.setOpaque(false);
			this.bookmarksButton.setIcon((Icon) ADD_TO_BOOKMARKS_ACTION.getValue(Action.SMALL_ICON));
			this.bookmarksButton.setFocusable(false);
			this.bookmarksButton.setToolTipText((String) ADD_TO_BOOKMARKS_ACTION.getValue(Action.SHORT_DESCRIPTION));
			this.bookmarksButton.getAccessibleContext().setAccessibleName(
					(String) ADD_TO_BOOKMARKS_ACTION.getValue(Action.ACCELERATOR_KEY));
			this.bookmarksButton.setAlignmentX(Component.LEFT_ALIGNMENT);
			this.bookmarksButton.setAlignmentY(Component.CENTER_ALIGNMENT);
			this.bookmarksButton.setBackground((Color) UIManager.get("control"));

			topPanel.add(this.bookmarksButton);
		}

		// Home Button
		File homeDir = fsv.getHomeDirectory();
		String toolTipText = (String) GO_HOME_ACTION.getValue(Action.SHORT_DESCRIPTION);
		if (fsv.isRoot(homeDir)) {
			toolTipText = getFileView(fc).getName(homeDir); // Probably "Desktop".
		}

		JButton b = new JButton(GO_HOME_ACTION);
		b.setText("");
		b.setRolloverEnabled(true);
		b.setIcon((Icon) GO_HOME_ACTION.getValue(Action.SMALL_ICON));
		b.setToolTipText(toolTipText);
		b.getAccessibleContext().setAccessibleName((String) GO_HOME_ACTION.getValue(Action.ACCELERATOR_KEY));
		b.setAlignmentX(Component.LEFT_ALIGNMENT);
		b.setAlignmentY(Component.CENTER_ALIGNMENT);
		b.setFocusable(false);
		b.setOpaque(false);

		topPanel.add(b);

		// for remote files only show new Folder button if creation of new folders is possible
		if (!(fsv instanceof RemoteFileSystemView && !((RemoteFileSystemView) fsv).isCreatingNewFolderEnabled())) {
			// New Directory Button
			b = new JButton(NEW_FOLDER_ACTION);
			b.setText(null);
			b.setRolloverEnabled(true);
			b.setIcon((Icon) NEW_FOLDER_ACTION.getValue(Action.SMALL_ICON));
			b.setOpaque(false);
			b.setFocusable(false);

			b.setToolTipText((String) NEW_FOLDER_ACTION.getValue(Action.SHORT_DESCRIPTION));
			b.getAccessibleContext().setAccessibleName((String) NEW_FOLDER_ACTION.getValue(Action.ACCELERATOR_KEY));
			b.setAlignmentX(Component.LEFT_ALIGNMENT);
			b.setAlignmentY(Component.CENTER_ALIGNMENT);

			topPanel.add(b);
		}

		// views button
		this.changeViewButton = new DropDownPopupButton(CHANGE_VIEW_ACTION, new PopupMenuProvider() {

			@Override
			public JPopupMenu getPopupMenu() {
				return changeViewPopup;
			}

		});
		this.changeViewButton.setText("");
		this.changeViewButton.setRolloverEnabled(true);
		this.changeViewButton.setIcon((Icon) CHANGE_VIEW_ACTION.getValue(Action.SMALL_ICON));
		this.changeViewButton.setToolTipText((String) CHANGE_VIEW_ACTION.getValue(Action.SHORT_DESCRIPTION));
		this.changeViewButton.getAccessibleContext().setAccessibleName(
				(String) CHANGE_VIEW_ACTION.getValue(Action.ACCELERATOR_KEY));
		this.changeViewButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		this.changeViewButton.setAlignmentY(Component.CENTER_ALIGNMENT);
		this.changeViewButton.setMaximumSize(new Dimension(50, 30));
		this.changeViewButton.setBackground((Color) UIManager.get("control"));
		this.changeViewButton.setOpaque(false);
		this.changeViewButton.setFocusable(false);
		topPanel.add(this.changeViewButton);

		topPanel.setBackground((Color) UIManager.get("control"));

		// ************************************** //
		// ******* Add the directory pane ******* //
		// ************************************** //
		this.fileList = new FileList(this, fc);
		fc.addPropertyChangeListener(this.fileList);
		this.fileList.add(getAccessoryPanel(), BorderLayout.AFTER_LINE_ENDS);
		JComponent accessory = fc.getAccessory();
		if (accessory != null) {
			getAccessoryPanel().add(accessory);
		}

		// extra border for remote file chooser
		if (fsv instanceof RemoteFileSystemView) {
			panel.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, RapidLookAndFeel.getColors().getCommonBackground()));
		}
		panel.add(this.fileList, BorderLayout.CENTER);

		// ********************************** //
		// **** Construct the bottom panel ** //
		// ********************************** //
		JPanel bottomPanel = new JPanel();
		bottomPanel.setOpaque(false);
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
		bottomPanel.setBorder(null);
		panel.add(bottomPanel, BorderLayout.SOUTH);
		fc.add(panel, BorderLayout.CENTER);

		// FileName label and textfield
		JPanel fileNamePanel = new JPanel();
		fileNamePanel.setOpaque(false);
		fileNamePanel.setLayout(new BoxLayout(fileNamePanel, BoxLayout.LINE_AXIS));
		bottomPanel.add(fileNamePanel);
		bottomPanel.add(Box.createRigidArea(new Dimension(1, ButtonDialog.GAP)));

		AlignedLabel fileNameLabel = new AlignedLabel(this.fileNameLabelText);
		fileNameLabel.setDisplayedMnemonic(this.fileNameLabelMnemonic);
		fileNamePanel.add(fileNameLabel);

		this.fileNameTextField = new JTextField();
		this.fileNameTextField.setPreferredSize(new Dimension(fileNameTextField.getWidth(), 26));
		this.fileNameTextField.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				setFileSelected();
			}

			@Override
			public void keyTyped(KeyEvent e) {}
		});
		fileNamePanel.add(this.fileNameTextField);

		if (fc.isMultiSelectionEnabled()) {
			setFileName(fileNameString(fc.getSelectedFiles()));
		} else {
			setFileName(fileNameString(fc.getSelectedFile()));
		}

		// Filetype label and combobox
		JPanel filesOfTypePanel = new JPanel();
		filesOfTypePanel.setOpaque(false);
		filesOfTypePanel.setLayout(new BoxLayout(filesOfTypePanel, BoxLayout.LINE_AXIS));
		bottomPanel.add(filesOfTypePanel);

		AlignedLabel filesOfTypeLabel = new AlignedLabel(this.filesOfTypeLabelText);
		filesOfTypeLabel.setDisplayedMnemonic(this.filesOfTypeLabelMnemonic);
		filesOfTypePanel.add(filesOfTypeLabel);

		this.filterComboBoxModel = createFilterComboBoxModel();
		fc.addPropertyChangeListener(this.filterComboBoxModel);
		this.filterComboBox = new JComboBox<>(this.filterComboBoxModel);
		this.filterComboBox.setPreferredSize(new Dimension(filterComboBox.getWidth(), 26));
		this.filterComboBox.setOpaque(false);
		this.filterComboBox.getAccessibleContext().setAccessibleDescription(this.filesOfTypeLabelText);
		filesOfTypeLabel.setLabelFor(this.filterComboBox);
		this.filterComboBox.setRenderer(createFilterComboBoxRenderer());
		filesOfTypePanel.add(this.filterComboBox);

		// buttons
		fc.add(getBottomPanel(), BorderLayout.SOUTH);
		getButtonPanel().setLayout(new FlowLayout(FlowLayout.RIGHT));

		this.approveButton = new JButton(getApproveButtonText(fc));
		this.approveButton.setOpaque(false);
		this.approveButton.addActionListener(getApproveSelectionAction());
		this.approveButton.setToolTipText(getApproveButtonToolTipText(fc));
		this.approveButton.setIcon(getApproveButtonIcon(fc));
		this.approveButton.setEnabled(false);
		getButtonPanel().add(this.approveButton);

		this.cancelButton = new JButton(this.cancelButtonText, FILECHOOSER_CLOSE_ICON);
		this.cancelButton.setOpaque(false);
		this.cancelButton.setToolTipText(this.cancelButtonToolTipText);
		this.cancelButton.addActionListener(getCancelSelectionAction());
		getButtonPanel().add(this.cancelButton);

		if (fc.getControlButtonsAreShown()) {
			addControlButtons();
		}

		groupLabels(new AlignedLabel[] { fileNameLabel, filesOfTypeLabel });

		updateView(FILECHOOSER_VIEW_DETAILS);
	}

	@Override
	public Action getNewFolderAction() {
		Action newFolderAction = new NewFolderAction();
		// Note: Don't return null for readOnly, it might
		// break older apps.
		if (UIManager.getBoolean("FileChooser.readOnly")) {
			newFolderAction.setEnabled(false);
		}
		return newFolderAction;
	}

	protected JPanel getButtonPanel() {
		if (this.buttonPanel == null) {
			this.buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			this.buttonPanel.setOpaque(false);
			this.buttonPanel.setBorder(null);
		}
		return this.buttonPanel;
	}

	protected JPanel getBottomPanel() {
		if (this.bottomPanel == null) {
			this.bottomPanel = new JPanel();
			this.bottomPanel.setOpaque(false);
			this.bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
			this.bottomPanel.setBorder(null);
		}
		return this.bottomPanel;
	}

	@Override
	protected void installStrings(JFileChooser fc) {
		super.installStrings(fc);

		Locale l = fc.getLocale();

		this.lookInLabelMnemonic = UIManager.getInt("FileChooser.lookInLabelMnemonic");
		this.lookInLabelText = UIManager.getString("FileChooser.lookInLabelText", l);
		this.saveInLabelText = UIManager.getString("FileChooser.saveInLabelText", l);

		this.fileNameLabelMnemonic = UIManager.getInt("FileChooser.fileNameLabelMnemonic");
		this.fileNameLabelText = UIManager.getString("FileChooser.fileNameLabelText", l);

		this.filesOfTypeLabelMnemonic = UIManager.getInt("FileChooser.filesOfTypeLabelMnemonic");
		this.filesOfTypeLabelText = UIManager.getString("FileChooser.filesOfTypeLabelText", l);
	}

	@Override
	protected void installListeners(JFileChooser fc) {
		super.installListeners(fc);

		ActionMap actionMap = getActions();
		SwingUtilities.replaceUIActionMap(fc, actionMap);
	}

	protected ActionMap getActions() {
		return createActions();
	}

	@Override
	public void uninstallUI(JComponent c) {
		c.removePropertyChangeListener(this.filterComboBoxModel);
		this.cancelButton.removeActionListener(getCancelSelectionAction());
		this.approveButton.removeActionListener(getApproveSelectionAction());
		this.fileNameTextField.removeActionListener(getApproveSelectionAction());
		super.uninstallUI(c);
	}

	@Override
	public Dimension getMaximumSize(JComponent c) {
		return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	private void setFileSelected() {
		this.selected = fileNameTextField.getText() != null && !"".equals(fileNameTextField.getText());
		approveButton.setEnabled(selected);
		for (ChangeListener l : listeners) {
			l.stateChanged(new ChangeEvent(this));
		}
	}

	public boolean isFileSelected() {
		return this.selected;
	}

	private String fileNameString(File file) {
		if (file == null) {
			return null;
		} else {
			JFileChooser fc = getFileChooser();
			if (fc.isDirectorySelectionEnabled() && !fc.isFileSelectionEnabled()) {
				return file.getPath();
			} else {
				return file.getName();
			}
		}
	}

	private String fileNameString(File[] files) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; files != null && i < files.length; i++) {
			if (i > 0) {
				buf.append(" ");
			}
			if (files.length > 1) {
				buf.append("\"");
			}
			buf.append(fileNameString(files[i]));
			if (files.length > 1) {
				buf.append("\"");
			}
		}
		return buf.toString();
	}

	/* The following methods are used by the PropertyChange Listener */

	private void doSelectedFileChanged(PropertyChangeEvent e) {
		File f = (File) e.getNewValue();
		JFileChooser fc = getFileChooser();
		if (f != null
				&& (fc.isFileSelectionEnabled() && !f.isDirectory() || f.isDirectory() && fc.isDirectorySelectionEnabled())) {
			setFileName(fileNameString(f));
			setFileSelected();
		}
	}

	private void doSelectedFilesChanged(PropertyChangeEvent e) {
		File[] files = (File[]) e.getNewValue();
		JFileChooser fc = getFileChooser();
		if (files != null && files.length > 0
				&& (files.length > 1 || fc.isDirectorySelectionEnabled() || !files[0].isDirectory())) {
			setFileName(fileNameString(files));
		} else {
			setFileName("");
		}
		setFileSelected();
	}

	private void doDirectoryChanged(PropertyChangeEvent e) {
		JFileChooser fc = getFileChooser();
		FileSystemView fsv = fc.getFileSystemView();

		clearIconCache();

		File currentDirectory = fc.getCurrentDirectory();
		this.fileList.updatePath(currentDirectory);

		if (currentDirectory != null) {
			this.directoryComboBoxModel.addItem(currentDirectory);
			getNewFolderAction().setEnabled(currentDirectory.canWrite());
			getChangeToParentDirectoryAction().setEnabled(!fsv.isRoot(currentDirectory));
			getChangeToParentDirectoryAction().setEnabled(!fsv.isRoot(currentDirectory));
			getGoHomeAction().setEnabled(!userHomeDirectory.equals(currentDirectory));

			if (fc.isDirectorySelectionEnabled()) {
				if (fc.isFileSelectionEnabled()) {
					setFileName(null);
				} else {
					if (fsv.isFileSystem(currentDirectory)) {
						setFileName(currentDirectory.getPath());
					} else {
						setFileName(null);
					}
				}
				setFileSelected();
			}
		}
	}

	private void doFilterChanged(PropertyChangeEvent e) {
		this.fileList.doFilterChanged();
	}

	private void doFileSelectionModeChanged(PropertyChangeEvent e) {
		doFilterChanged(e);

		JFileChooser fc = getFileChooser();
		File currentDirectory = fc.getCurrentDirectory();
		if (currentDirectory != null && fc.isDirectorySelectionEnabled() && !fc.isFileSelectionEnabled()
				&& fc.getFileSystemView().isFileSystem(currentDirectory)) {
			setFileName(currentDirectory.getPath());
		} else {
			setFileName(null);
		}
		setFileSelected();
	}

	private void doMultiSelectionChanged(PropertyChangeEvent e) {
		if (getFileChooser().isMultiSelectionEnabled()) {
		} else {
			getFileChooser().setSelectedFiles(null);
		}
	}

	private void doAccessoryChanged(PropertyChangeEvent e) {
		if (getAccessoryPanel() != null) {
			if (e.getOldValue() != null) {
				getAccessoryPanel().remove((JComponent) e.getOldValue());
			}
			JComponent accessory = (JComponent) e.getNewValue();
			if (accessory != null) {
				getAccessoryPanel().add(accessory, BorderLayout.CENTER);
			}
		}
	}

	private Icon getApproveButtonIcon(JFileChooser fc) {
		if (fc.getDialogType() == JFileChooser.OPEN_DIALOG) {
			return FILECHOOSER_OPEN_ICON;
		}
		if (fc.getDialogType() == JFileChooser.SAVE_DIALOG) {
			return FILECHOOSER_SAVE_ICON;
		}
		if (fc.getDialogType() == JFileChooser.CUSTOM_DIALOG) {
			return FILECHOOSER_SELECT_ICON;
		}
		return FILECHOOSER_SELECT_ICON;
	}

	private void doApproveButtonTextChanged(PropertyChangeEvent e) {
		JFileChooser chooser = getFileChooser();
		this.approveButton.setText(getApproveButtonText(chooser));
		this.approveButton.setToolTipText(getApproveButtonToolTipText(chooser));
		this.approveButton.setIcon(getApproveButtonIcon(chooser));
	}

	private void doDialogTypeChanged(PropertyChangeEvent e) {
		JFileChooser chooser = getFileChooser();
		this.approveButton.setText(getApproveButtonText(chooser));
		this.approveButton.setToolTipText(getApproveButtonToolTipText(chooser));
		this.approveButton.setIcon(getApproveButtonIcon(chooser));
		if (chooser.getDialogType() == JFileChooser.SAVE_DIALOG) {
			this.lookInLabel.setText(this.saveInLabelText);
		} else {
			this.lookInLabel.setText(this.lookInLabelText);
		}
	}

	private void doApproveButtonMnemonicChanged(PropertyChangeEvent e) {}

	private void doControlButtonsChanged(PropertyChangeEvent e) {
		if (getFileChooser().getControlButtonsAreShown()) {
			addControlButtons();
		} else {
			removeControlButtons();
		}
	}

	@Override
	public PropertyChangeListener createPropertyChangeListener(JFileChooser fc) {
		return new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent e) {
				String s = e.getPropertyName();

				if (s.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
					doSelectedFileChanged(e);
				} else if (s.equals(JFileChooser.SELECTED_FILES_CHANGED_PROPERTY)) {
					doSelectedFilesChanged(e);
				} else if (s.equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
					doDirectoryChanged(e);
				} else if (s.equals(JFileChooser.FILE_FILTER_CHANGED_PROPERTY)) {
					doFilterChanged(e);
				} else if (s.equals(JFileChooser.FILE_SELECTION_MODE_CHANGED_PROPERTY)) {
					doFileSelectionModeChanged(e);
				} else if (s.equals(JFileChooser.MULTI_SELECTION_ENABLED_CHANGED_PROPERTY)) {
					doMultiSelectionChanged(e);
				} else if (s.equals(JFileChooser.ACCESSORY_CHANGED_PROPERTY)) {
					doAccessoryChanged(e);
				} else if (s.equals(JFileChooser.APPROVE_BUTTON_TEXT_CHANGED_PROPERTY)
						|| s.equals(JFileChooser.APPROVE_BUTTON_TOOL_TIP_TEXT_CHANGED_PROPERTY)) {
					doApproveButtonTextChanged(e);
				} else if (s.equals(JFileChooser.DIALOG_TYPE_CHANGED_PROPERTY)) {
					doDialogTypeChanged(e);
				} else if (s.equals(JFileChooser.APPROVE_BUTTON_MNEMONIC_CHANGED_PROPERTY)) {
					doApproveButtonMnemonicChanged(e);
				} else if (s.equals(JFileChooser.CONTROL_BUTTONS_ARE_SHOWN_CHANGED_PROPERTY)) {
					doControlButtonsChanged(e);
				} else if (s.equals(JFileChooser.FILE_HIDING_CHANGED_PROPERTY)) {
					FileChooserUI.this.fileList.doFilterChanged();
				} else if (s.equals("componentOrientation")) {
					ComponentOrientation o = (ComponentOrientation) e.getNewValue();
					JFileChooser cc = (JFileChooser) e.getSource();
					if (o != (ComponentOrientation) e.getOldValue()) {
						cc.applyComponentOrientation(o);
					}
				} else if (s.equals("ancestor")) {
					if (e.getOldValue() == null && e.getNewValue() != null) {
						// Ancestor was added, set initial focus
						FileChooserUI.this.fileNameTextField.selectAll();
						FileChooserUI.this.fileList.itemPanel.requestFocus();
					}
				}
			}
		};
	}

	protected void removeControlButtons() {
		getBottomPanel().remove(getButtonPanel());
	}

	protected void addControlButtons() {
		getBottomPanel().add(getButtonPanel());
	}

	public JButton getApproveButton() {
		return this.approveButton;
	}

	public JButton getCancelButton() {
		return this.cancelButton;
	}

	@Override
	public void ensureFileIsVisible(JFileChooser fc, File f) {}

	@Override
	public void rescanCurrentDirectory(JFileChooser fc) {
		this.fileList.rescanDirectory();
	}

	@Override
	public String getFileName() {
		if (this.fileNameTextField != null) {
			return this.fileNameTextField.getText();
		} else {
			return null;
		}
	}

	@Override
	public void setFileName(String filename) {
		if (this.fileNameTextField != null) {
			this.fileNameTextField.setText(filename);
		}
	}

	@Override
	protected void setDirectorySelected(boolean directorySelected) {
		super.setDirectorySelected(directorySelected);
		JFileChooser chooser = getFileChooser();
		if (directorySelected) {
			this.approveButton.setText(this.directoryOpenButtonText);
			this.approveButton.setToolTipText(this.directoryOpenButtonToolTipText);
		} else {
			this.approveButton.setText(getApproveButtonText(chooser));
			this.approveButton.setToolTipText(getApproveButtonToolTipText(chooser));
		}
	}

	@Override
	public String getDirectoryName() {
		return null;
	}

	@Override
	public void setDirectoryName(String dirname) {}

	protected DirectoryComboBoxRenderer createDirectoryComboBoxRenderer(JFileChooser fc) {
		return new DirectoryComboBoxRenderer();
	}

	protected DirectoryComboBoxModel createDirectoryComboBoxModel(JFileChooser fc) {
		return new DirectoryComboBoxModel();
	}

	protected FilterComboBoxRenderer createFilterComboBoxRenderer() {
		return new FilterComboBoxRenderer();
	}

	protected FilterComboBoxModel createFilterComboBoxModel() {
		return new FilterComboBoxModel();
	}

	public void valueChanged(ListSelectionEvent e) {
		JFileChooser fc = getFileChooser();
		File f = fc.getSelectedFile();
		if (!e.getValueIsAdjusting() && f != null && !getFileChooser().isTraversable(f)) {
			setFileName(fileNameString(f));
			setFileSelected();
		}
	}

	protected void setCurrentDirectoryOfFileChooser(File f) {
		getFileChooser().setCurrentDirectory(f);
	}

	@Override
	protected JButton getApproveButton(JFileChooser fc) {
		return this.approveButton;
	}

	private static void groupLabels(AlignedLabel[] group) {
		for (AlignedLabel element : group) {
			element.group = group;
		}
	}

	@Override
	public FileView getFileView(JFileChooser fc) {
		return this.fileView;
	}

	public void goBack() {
		if (this.backPathVector.size() > 0) {
			setCurrentDirectoryOfFileChooser(new File(this.backPathVector.elementAt(this.backPathVector.size() - 1)));

			if (this.backPathVector.size() > 1) {
				this.backPathVector.setSize(this.backPathVector.size() - 2);
			} else {
				this.backPathVector.setSize(this.backPathVector.size() - 1);
			}

			if (this.backPathVector.size() <= 0) {
				getGoBackAction().setEnabled(false);
			}
		}
	}

	public Action getGoBackAction() {
		return GO_BACK_ACTION;
	}

	@Override
	public Action getGoHomeAction() {
		return GO_HOME_ACTION;
	}

	@Override
	public Action getChangeToParentDirectoryAction() {
		return CHANGE_TO_PARENT_DIRECTORY;
	}

	public JPopupMenu createViewPopupMenu() {
		JMenuItem menuItem;
		changeViewActionListener mal = new changeViewActionListener();

		this.changeViewPopup = new JPopupMenu();
		this.changeViewButtonGroup = new ButtonGroup();

		menuItem = new JRadioButtonMenuItem(FILECHOOSER_VIEW_THUMBNAIL);
		this.changeViewButtonGroup.add(menuItem);
		menuItem.setActionCommand(FILECHOOSER_VIEW_THUMBNAIL);
		menuItem.addActionListener(mal);
		this.changeViewPopup.add(menuItem);

		menuItem = new JRadioButtonMenuItem(FILECHOOSER_VIEW_ICON);
		menuItem.setActionCommand(FILECHOOSER_VIEW_ICON);
		this.changeViewButtonGroup.add(menuItem);
		menuItem.addActionListener(mal);
		this.changeViewPopup.add(menuItem);

		menuItem = new JRadioButtonMenuItem(FILECHOOSER_VIEW_LIST);
		menuItem.setActionCommand(FILECHOOSER_VIEW_LIST);
		this.changeViewButtonGroup.add(menuItem);
		menuItem.addActionListener(mal);
		menuItem.setSelected(true);
		this.changeViewPopup.add(menuItem);

		menuItem = new JRadioButtonMenuItem(FILECHOOSER_VIEW_DETAILS);
		menuItem.setActionCommand(FILECHOOSER_VIEW_DETAILS);
		this.changeViewButtonGroup.add(menuItem);
		menuItem.addActionListener(mal);
		this.changeViewPopup.add(menuItem);

		return this.changeViewPopup;
	}

	protected void updateView(String s) {
		if (!s.equals(FILECHOOSER_VIEW_DETAILS) && !s.equals(FILECHOOSER_VIEW_ICON) && !s.equals(FILECHOOSER_VIEW_LIST)
				&& !s.equals(FILECHOOSER_VIEW_THUMBNAIL)) {
			return;
		}

		this.viewType = s;
		this.fileList.changeCardForView();

		// synchronizing menu's
		JRadioButtonMenuItem rbm;
		Enumeration<AbstractButton> en = this.changeViewButtonGroup.getElements();
		while (en.hasMoreElements()) {
			rbm = (JRadioButtonMenuItem) en.nextElement();

			if (rbm.getActionCommand().equals(this.getView())) {
				this.changeViewButtonGroup.setSelected(rbm.getModel(), true);
			}
		}
	}
}
