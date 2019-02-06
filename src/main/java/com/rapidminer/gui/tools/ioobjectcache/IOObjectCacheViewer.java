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
package com.rapidminer.gui.tools.ioobjectcache;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJToolBar;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.ioobjectcache.actions.ClearCacheAction;
import com.rapidminer.gui.tools.ioobjectcache.actions.OpenCacheEntryAction;
import com.rapidminer.gui.tools.ioobjectcache.actions.RemoveCacheEntryAction;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectMap;
import com.rapidminer.operator.IOObjectMapEvent;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * Simple viewer for {@link IOObjectMap}s. Allows for the deletion for single entries and the entire
 * map, as well as to open entries in the results perspective.
 *
 * @author Michael Knopf
 */
public class IOObjectCacheViewer extends JPanel implements Dockable {

	private static final long serialVersionUID = 1L;

	public static final String IOOBJECT_CACHE_VIEWER_DOCK_KEY = "ioobject_cache_viewer";

	private final DockKey dockKey = new ResourceDockKey(IOOBJECT_CACHE_VIEWER_DOCK_KEY);
	{
		dockKey.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	/**
	 * Utility class that manages all updates of the {@link IOObjectCacheViewer}. It ensures that no
	 * more than two updates are scheduled at the same time and that the view is not updated if it
	 * is invisible. FOr this purpose, it observes the {@link IOObjectMap} itself as well as the
	 * Swing hierarchy.
	 *
	 * @author Michael Knopf
	 */
	private class UpdateManager implements Observer<IOObjectMapEvent>, HierarchyListener {

		/** Number of currently scheduled updates (should never exceed two). */
		private int scheduledUpdates = 0;

		/** Indicates whether updates have been skipped while the viewer was invisible. */
		private boolean isDirty = false;

		/** Lock to manage the access to the above counter and flag. */
		private final Object updateLock = new Object();

		/** Invokes an update of the viewer. */
		private final Runnable updateEntries = new Runnable() {

			@Override
			public void run() {
				synchronized (updateLock) {
					scheduledUpdates--;
					isDirty = false;
				}
				scrollPane.setViewportView(createEntriesPanel());
				scrollPane.getViewport().revalidate();
				scrollPane.getViewport().repaint();
			}
		};

		@Override
		public void hierarchyChanged(HierarchyEvent e) {
			// update whenever the component visibility changes, the view is dirty (prior updates
			// have been ignored), and not more than one update is scheduled
			synchronized (updateLock) {
				if ((HierarchyEvent.SHOWING_CHANGED & e.getChangeFlags()) != 0 && isDirty && scheduledUpdates <= 1) {
					scheduledUpdates++;
					SwingUtilities.invokeLater(updateEntries);
				}
			}
		}

		@Override
		public void update(Observable<IOObjectMapEvent> observable, IOObjectMapEvent event) {
			synchronized (updateLock) {
				// only update if the viewer is visible and not more than one update is currently
				// scheduled
				if (!IOObjectCacheViewer.this.isShowing()) {
					// remember that we skipped an update
					isDirty = true;
				} else if (scheduledUpdates <= 1) {
					scheduledUpdates++;
					SwingUtilities.invokeLater(updateEntries);
				}
			}
		}
	}

	private static final Icon UNKNOWN_TYPE = SwingTools.createIcon("16/"
			+ I18N.getGUIMessage("gui.icon.ioobject_viewer.unknown_type.icon"));

	/** The corresponding {@link IOObjectMap}. */
	private final IOObjectMap map;

	/** The scroll pane containing the list of cache entries. */
	private JScrollPane scrollPane;

	/** Initializes the GUI components of the viewer. */
	private void initView() {
		// the viewer's toolbar
		JToolBar toolBar = new ExtendedJToolBar(true);
		toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.TEXTFIELD_BORDER));

		// add actions that clears all entries
		Action clearAction = new ClearCacheAction(map);
		toolBar.add(clearAction);

		// setup the header column (reuse the layout of the entries)
		JPanel headerPanel = new JPanel(IOObjectCacheEntryPanel.ENTRY_LAYOUT);
		headerPanel.add(Box.createVerticalStrut(16), IOObjectCacheEntryPanel.ICON_CONSTRAINTS);
		JLabel typeLabel = new JLabel(I18N.getGUILabel("ioobject_viewer.type"));
		typeLabel.setFont(getFont().deriveFont(Font.ITALIC));
		headerPanel.add(typeLabel, IOObjectCacheEntryPanel.TYPE_CONSTRAINTS);
		JLabel keyLabel = new JLabel(I18N.getGUILabel("ioobject_viewer.key"));
		keyLabel.setFont(getFont().deriveFont(Font.ITALIC));
		headerPanel.add(keyLabel, IOObjectCacheEntryPanel.KEY_CONSTRAINTS);
		headerPanel.add(Box.createVerticalStrut(24), IOObjectCacheEntryPanel.REMOVE_BUTTON_CONSTRAINTS);

		// create entries panel and embed in scroll pane
		scrollPane = new ExtendedJScrollPane(createEntriesPanel());
		scrollPane.setBorder(null);

		// panel containing the header row and the actual entries
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(headerPanel, BorderLayout.NORTH);
		contentPanel.add(scrollPane, BorderLayout.CENTER);

		// put everything together
		add(toolBar, BorderLayout.NORTH);
		add(contentPanel, BorderLayout.CENTER);
	}

	/** Updates the contents of the entries panel. */
	private JPanel createEntriesPanel() {
		GridBagLayout layout = new GridBagLayout();

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		JPanel entriesPanel = new JPanel();
		entriesPanel.setLayout(layout);

		List<String> keys = new ArrayList<>(map.getAll().keySet());
		Collections.sort(keys);
		boolean alternatingRow = true;
		for (String key : keys) {
			IOObject object = map.get(key);
			if (object == null) {
				// do not display empty results
				continue;
			}

			// look up icon
			Icon icon = RendererService.getIcon(object.getClass());
			if (icon == null) {
				icon = UNKNOWN_TYPE;
			}

			// look up name
			String type = RendererService.getName(object.getClass());
			if (type == null) {
				type = object.getClass().getSimpleName();
			}

			Action removeAction = new RemoveCacheEntryAction(map, key);
			Action openAction = new OpenCacheEntryAction(map, key);

			IOObjectCacheEntryPanel entry = new IOObjectCacheEntryPanel(icon, type, openAction, removeAction);
			if (alternatingRow) {
				entry.setDefaultBackground(Colors.WHITE);
			}
			alternatingRow = !alternatingRow;

			entriesPanel.add(entry, gbc);
			gbc.gridy += 1;
		}

		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		entriesPanel.add(Box.createVerticalGlue(), gbc);

		return entriesPanel;
	}

	/**
	 * Creates a new viewer for the specified {@link IOObjectMap}.
	 *
	 * @param map
	 *            The corresponding {@link IOObjectMap};
	 *
	 * @throws NPE
	 *             If the provided map is <code>null</code>.
	 */
	public IOObjectCacheViewer(IOObjectMap map) {
		super(new BorderLayout());
		Objects.requireNonNull(map);
		this.map = map;

		// initialize GUi elements
		initView();

		// listen to changes of the map and of the GUI hierarchy
		UpdateManager updateManager = new UpdateManager();
		map.addMapObserver(updateManager);
		addHierarchyListener(updateManager);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return dockKey;
	}

}
