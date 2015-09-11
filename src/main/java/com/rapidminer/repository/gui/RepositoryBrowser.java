/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
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
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.dnd.AbstractPatchedTransferHandler;
import com.rapidminer.gui.dnd.DragListener;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJToolBar;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.components.DropDownButton;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizardFactory;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizardRegistry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryLocation;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * A component to browse through repositories.
 *
 * @author Simon Fischer
 */
public class RepositoryBrowser extends JPanel implements Dockable {

	private static final long serialVersionUID = 1L;

	public static final Action ADD_REPOSITORY_ACTION = new ResourceAction(true, "add_repository") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			addRepository();
		}
	};

	private final RepositoryTree tree;

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
		tree.addRepositorySelectionListener(new RepositorySelectionListener() {

			@Override
			public void repositoryLocationSelected(RepositorySelectionEvent e) {
				Entry entry = e.getEntry();
				if (entry instanceof ProcessEntry) {
					RepositoryTree.openProcess((ProcessEntry) entry);
				} else if (entry instanceof IOObjectEntry) {
					OpenAction.showAsResult((IOObjectEntry) entry);
				}
			}
		});

		setLayout(new BorderLayout());
		JToolBar toolBar = new ExtendedJToolBar();
		toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
		toolBar.add(ADD_REPOSITORY_ACTION);
		DropDownButton dropButton = new DropDownButton(new ResourceActionAdapter(true, "import")) {

			private static final long serialVersionUID = -5482452738124971463L;

			@Override
			protected JPopupMenu getPopupMenu() {
				JPopupMenu menu = new JPopupMenu();
				DataImportWizardRegistry registry = RapidMinerGUI.getMainFrame().getDataImportWizardRegistry();
				for (DataImportWizardFactory factory : registry.getFactories()) {
					menu.add(factory.createAction());
				}
				return menu;
			}
		};
		dropButton.setUsePopupActionOnMainButton();
		dropButton.addToToolBar(toolBar);
		toolBar.add(tree.OPEN_ACTION);
		toolBar.add(tree.REFRESH_ACTION);
		toolBar.add(tree.CREATE_FOLDER_ACTION);
		toolBar.add(tree.SHOW_PROCESS_IN_REPOSITORY_ACTION);

		add(toolBar, BorderLayout.NORTH);
		JScrollPane scrollPane = new ExtendedJScrollPane(tree);
		scrollPane.setBorder(null);
		add(scrollPane, BorderLayout.CENTER);
	}

	private static void addRepository() {
		NewRepositoryDialog.createNew();
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
	 * @param storedRepositoryLocation
	 */
	public void expandToRepositoryLocation(RepositoryLocation storedRepositoryLocation) {
		tree.expandAndSelectIfExists(storedRepositoryLocation);
	}
}
