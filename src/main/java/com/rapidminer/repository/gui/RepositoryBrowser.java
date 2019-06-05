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
package com.rapidminer.repository.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.actions.ImportDataAction;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.dnd.AbstractPatchedTransferHandler;
import com.rapidminer.gui.dnd.DragListener;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.DropDownPopupButton;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.actions.NewRepositoryAction;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * A component to browse through repositories.
 *
 * @author Simon Fischer
 */
public class RepositoryBrowser extends JPanel implements Dockable {

	private static final long serialVersionUID = 1L;

	public static final Action ADD_REPOSITORY_ACTION = new NewRepositoryAction();

	private static final Action SORT_REPOSITORY_ACTION = new ResourceAction(true, "repository_sort_submenu") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {}
	};

	private final RepositoryTree tree;

	/**
	 * Tracking this {@link JPopupMenu} to be able to add items via {@link RepositoryBrowser#addMenuItem(JMenuItem)}
	 *
	 * @since 9.3
	 */
	private final JPopupMenu furtherActionsMenu = new JPopupMenu();

	public RepositoryBrowser() {
		this(null);
	}

	/**
	 * @param dragListener
	 *            registers a dragListener at the repository tree transferhandler. The listener is
	 *            informed when a drag starts and a drag ends.
	 */
	public RepositoryBrowser(DragListener dragListener) {
		tree = new RepositoryTree();
		if (dragListener != null) {
			((AbstractPatchedTransferHandler) tree.getTransferHandler()).addDragListener(dragListener);
		}
		tree.addRepositorySelectionListener(e -> {

			Entry entry = e.getEntry();

			// skip folder double-clicks
			if (entry instanceof Folder) {
				return;
			}

			if (entry instanceof ProcessEntry) {
				RepositoryTree.openProcess((ProcessEntry) entry);
			} else if (entry instanceof ConnectionEntry) {
				OpenAction.showConnectionInformationDialog((ConnectionEntry) entry);
			} else if (entry instanceof IOObjectEntry) {
				OpenAction.showAsResult((IOObjectEntry) entry);
			} else {
				SwingTools.showVerySimpleErrorMessage("no_data_or_process");
			}

		});

		setLayout(new BorderLayout());

		furtherActionsMenu.add(ADD_REPOSITORY_ACTION);
		furtherActionsMenu.add(tree.CREATE_FOLDER_ACTION);
		final JMenu sortActionsMenu = new JMenu(SORT_REPOSITORY_ACTION);
		sortActionsMenu.add(tree.SORT_BY_NAME_ACTION.createMenuItem());
		sortActionsMenu.add(tree.SORT_BY_LAST_MODIFIED_DATE_ACTION.createMenuItem());
		furtherActionsMenu.add(sortActionsMenu);

		furtherActionsMenu.add(tree.REFRESH_ACTION);
		furtherActionsMenu.add(tree.SHOW_PROCESS_IN_REPOSITORY_ACTION);

		JPanel northPanel = new JPanel(new GridBagLayout());
		northPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.insets = new Insets(2, 2, 2, 2);

		JButton addDataButton = new JButton(new ImportDataAction(true));
		addDataButton.setPreferredSize(new Dimension(100, 30));
		northPanel.add(addDataButton, c);

		DropDownPopupButton furtherActionsButton = new DropDownPopupButton("gui.action.further_repository_actions",
				() -> furtherActionsMenu);
		furtherActionsButton.setPreferredSize(new Dimension(50, 30));

		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0;
		northPanel.add(furtherActionsButton, c);

		add(northPanel, BorderLayout.NORTH);
		JScrollPane scrollPane = new ExtendedJScrollPane(tree);
		scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.TEXTFIELD_BORDER));
		add(scrollPane, BorderLayout.CENTER);
	}

	/**
	 * Returns the {@link RepositoryTree} managed by this browser.
	 *
	 * @return the repository tree
	 * @since 7.0.0
	 */
	public RepositoryTree getRepositoryTree() {
		return tree;
	}

	public static final String REPOSITORY_BROWSER_DOCK_KEY = "repository_browser";
	private final DockKey DOCK_KEY = new ResourceDockKey(REPOSITORY_BROWSER_DOCK_KEY);

	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	/**
	 * @param storedRepositoryLocation the repository location that should be displayed in the browser
	 */
	public void expandToRepositoryLocation(RepositoryLocation storedRepositoryLocation) {
		tree.expandAndSelectIfExists(storedRepositoryLocation);
	}

	/**
	 * Add a {@link JMenuItem} to the popup-button in the upper right corner of this RepositoryBrowser
	 *
	 * @param item
	 * 		the {@link JMenuItem} to be added to this {@link RepositoryBrowser}
	 * @since 9.3
	 */
	public void addMenuItem(JMenuItem item) {
		furtherActionsMenu.add(item);
	}

	/**
	 * Remove a {@link JMenuItem} from the popup-button in the upper right corner of this RepositoryBrowser
	 *
	 * @param item
	 * 		the {@link JMenuItem} to be removed from this {@link RepositoryBrowser}
	 * @since 9.3
	 */
	public void removeMenuItem(JMenuItem item) {
		furtherActionsMenu.remove(item);
	}
}
