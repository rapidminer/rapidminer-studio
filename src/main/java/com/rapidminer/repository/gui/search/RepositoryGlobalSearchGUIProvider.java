/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository.gui.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.InputEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.apache.lucene.document.Document;

import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.dnd.TransferableRepositoryEntry;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.search.GlobalSearchGUIUtilities;
import com.rapidminer.gui.search.GlobalSearchableGUIProvider;
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.gui.tools.MultiSwingWorker;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Model;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.ConnectionRepository;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryTree;
import com.rapidminer.repository.search.RepositoryGlobalSearch;
import com.rapidminer.search.GlobalSearchUtilities;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.usagestats.DefaultUsageLoggable;


/**
 * Provides UI elements to display repository entries in the Global Search results for the {@link RepositoryGlobalSearch}.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public class RepositoryGlobalSearchGUIProvider implements GlobalSearchableGUIProvider {

	/**
	 * Drag & Drop support for repository entries.
	 */
	private static final class RepositoryDragGesture extends DefaultUsageLoggable implements DragGestureListener {

		private final RepositoryLocation location;

		private RepositoryDragGesture(RepositoryLocation location) {
			this.location = location;
		}

		@Override
		public void dragGestureRecognized(DragGestureEvent event) {
			// only allow dragging with left mouse button
			if (event.getTriggerEvent().getModifiers() != InputEvent.BUTTON1_MASK) {
				return;
			}

			// change cursor to drag move
			Cursor cursor = null;
			if (event.getDragAction() == DnDConstants.ACTION_COPY) {
				cursor = DragSource.DefaultCopyDrop;
			}

			// set the repository entry as the Transferable
			TransferableRepositoryEntry transferable = new TransferableRepositoryEntry(location);
			if (usageLogger != null) {
				transferable.setUsageStatsLogger(usageLogger);
			} else if (usageObject != null) {
				transferable.setUsageObject(usageObject);
			}
			event.startDrag(cursor, transferable);
		}

	}

	private static final ImageIcon PROCESS_ICON = SwingTools.createIcon("16/gearwheel.png");
	private static final ImageIcon MODEL_ICON = SwingTools.createIcon("16/lightbulb_on.png");
	private static final ImageIcon DATA_ICON = SwingTools.createIcon("16/data.png");
	private static final ImageIcon UNKNOWN_ICON = SwingTools.createIcon("16/question.png");
	private static final ImageIcon LOADING_ICON = SwingTools.createIcon("16/loading.gif");

	private static final Color LOCATION_COLOR = new Color(62, 62, 62);
	private static final Border ICON_EMPTY_BORDER = BorderFactory.createEmptyBorder(0, 5, 0, 15);
	private static final int MAX_LENGTH_LOCATION = 49;
	private static final float FONT_SIZE_NAME = 14f;
	private static final float FONT_SIZE_LOCATION = 9f;

	private static final Map<String, Icon> ICON_CACHE = new ConcurrentHashMap<>();


	@Override
	public JComponent getGUIListComponentForDocument(final Document document, final String[] bestFragments) {
		JPanel mainListPanel = new JPanel();
		mainListPanel.setOpaque(false);
		String absolutePath = document.get(GlobalSearchUtilities.FIELD_UNIQUE_ID);

		JPanel descriptionPanel = new JPanel();
		descriptionPanel.setOpaque(false);

		mainListPanel.setLayout(new BorderLayout());
		descriptionPanel.setLayout(new BorderLayout());

		JLabel nameListLabel = new JLabel();
		nameListLabel.setFont(nameListLabel.getFont().deriveFont(Font.BOLD).deriveFont(FONT_SIZE_NAME));
		JLabel locationListLabel = new JLabel();
		locationListLabel.setForeground(LOCATION_COLOR);
		locationListLabel.setFont(locationListLabel.getFont().deriveFont(FONT_SIZE_LOCATION));
		JLabel iconListLabel = new JLabel();
		iconListLabel.setBorder(ICON_EMPTY_BORDER);

		descriptionPanel.add(nameListLabel, BorderLayout.CENTER);
		descriptionPanel.add(locationListLabel, BorderLayout.SOUTH);

		mainListPanel.add(iconListLabel, BorderLayout.WEST);
		mainListPanel.add(descriptionPanel, BorderLayout.CENTER);

		nameListLabel.setText(formatName(document.get(GlobalSearchUtilities.FIELD_NAME), bestFragments));

		// either use icon already cached or use loading icon
		Icon icon = ICON_CACHE.get(absolutePath);
		boolean needIconLoading = icon == null;
		icon = needIconLoading ? LOADING_ICON : icon;
		iconListLabel.setIcon(icon);

		// for layout reasons, add whitespace location first
		locationListLabel.setText(" ");
		try {
			RepositoryLocation location = new RepositoryLocation(absolutePath);
			String locString = RepositoryLocation.REPOSITORY_PREFIX + location.getRepositoryName();
			locString = locString + "..." + RepositoryLocation.SEPARATOR + location.getName();

			locationListLabel.setText(SwingTools.getShortenedDisplayName(locString, MAX_LENGTH_LOCATION));
			mainListPanel.setToolTipText(location.getAbsoluteLocation());
		} catch (MalformedRepositoryLocationException e) {
			// should not happen
		}

		// no icon cached yet? Load it async
		if (needIconLoading) {
			// loading the entry may take a while, do it async
			loadEntryDetailsAsync(document, iconListLabel);
		}

		return mainListPanel;
	}

	@Override
	public String getI18nNameForSearchable() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.component.global_search.repository.category.title");
	}

	@Override
	public void searchResultTriggered(final Document document, final Veto veto) {
		// try to open location
		String absoluteLocation = document.get(GlobalSearchUtilities.FIELD_UNIQUE_ID);
		if (absoluteLocation == null) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.global_search.RepositorySearchManager.error.no_key");
			return;
		}

		openEntryAsync(document);
	}

	@Override
	public void searchResultBrowsed(final Document document) {
		// try to open location
		String absoluteLocation = document.get(GlobalSearchUtilities.FIELD_UNIQUE_ID);
		if (absoluteLocation == null) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.global_search.RepositorySearchManager.error.no_key");
			return;
		}

		try {
			RepositoryLocation location = new RepositoryLocation(absoluteLocation);
			try {
				Repository repository = location.getRepository();
				if (repository instanceof ConnectionRepository && !((ConnectionRepository) repository).isConnected()) {
					// skip scrolling if the repository was disconnected
					return;
				}
			} catch (RepositoryException e) {
				// repo not available, no scrolling necessary
				return;
			}
			// scroll to location
			// twice because otherwise the repository browser selects the parent...
			RapidMinerGUI.getMainFrame().getRepositoryBrowser().getRepositoryTree().expandAndSelectIfExists(location);
			RapidMinerGUI.getMainFrame().getRepositoryBrowser().getRepositoryTree().expandAndSelectIfExists(location);
		} catch (MalformedRepositoryLocationException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.global_search.RepositorySearchManager.error.browse_location", e.getMessage());
		}
	}

	@Override
	public boolean isDragAndDropSupported(final Document document) {
		return true;
	}

	@Override
	public DragGestureListener getDragAndDropSupport(final Document document) {
		String absoluteLocation = document.get(GlobalSearchUtilities.FIELD_UNIQUE_ID);
		if (absoluteLocation == null) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.global_search.RepositorySearchManager.error.no_key");
			return null;
		}

		try {
			return new RepositoryDragGesture(new RepositoryLocation(absoluteLocation));
		} catch (MalformedRepositoryLocationException e) {
			return null;
		}
	}

	/**
	 * Loads the repository entry async. This may take a while, especially for slow remote repositories, so don't do it on the EDT.
	 *
	 * @param document
	 * 		the document for which to populate the UI
	 * @param iconListLabel
	 * 		the type icon label
	 */
	private void loadEntryDetailsAsync(final Document document, final JLabel iconListLabel) {
		MultiSwingWorker<Entry, Void> worker = new MultiSwingWorker<Entry, Void>() {

			@Override
			protected Entry doInBackground() throws Exception {
				RepositoryLocation location = new RepositoryLocation(document.get(GlobalSearchUtilities.FIELD_UNIQUE_ID));
				return location.locateEntry();
			}

			@Override
			protected void done() {
				try {
					Entry locatedEntry = get();
					// GlobalSearch uses a different API than the actual repository for some repositories (e.g. RM ServeR)
					// so we can have a hit in the GlobalSearch but the repository does not yet know about it.
					if (locatedEntry == null) {
						iconListLabel.setIcon(UNKNOWN_ICON);
						return;
					}
					// no error here? Entry located successfully.

					// put icon in cache for faster retrieval next time
					Icon icon = getIconForEntry(locatedEntry, document);
					ICON_CACHE.put(locatedEntry.getLocation().getAbsoluteLocation(), icon);

					iconListLabel.setIcon(icon);
				} catch (Exception e) {
					// loading entry has failed, log and use fallback
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.global_search.RepositorySearchManager.error.lazy_loading", e);

					iconListLabel.setIcon(UNKNOWN_ICON);
				}
			}
		};
		worker.start();
	}

	/**
	 * Opens the repository entry async. This may take a while, especially for slow remote repositories, so don't do it on the EDT.
	 *
	 * @param document
	 * 		the document for which to load the repository entry
	 */
	private void openEntryAsync(final Document document) {
		MultiSwingWorker<Entry, Void> worker = new MultiSwingWorker<Entry, Void>() {

			@Override
			protected Entry doInBackground() throws Exception {
				RepositoryLocation location = new RepositoryLocation(document.get(GlobalSearchUtilities.FIELD_UNIQUE_ID));
				return location.locateEntry();
			}

			@Override
			protected void done() {
				try {
					Entry locatedEntry = get();
					// no error here? Entry located successfully.

					if (locatedEntry instanceof ProcessEntry) {
						RepositoryTree.openProcess((ProcessEntry) locatedEntry);
					} else if (locatedEntry instanceof ConnectionEntry) {
						OpenAction.showConnectionInformationDialog((ConnectionEntry) locatedEntry);
					} else if (locatedEntry instanceof IOObjectEntry) {
						OpenAction.showAsResult((IOObjectEntry) locatedEntry);
					} else {
						SwingTools.showVerySimpleErrorMessage("no_data_or_process");
					}
				} catch (Exception e) {
					// loading entry has failed, log and do nothing
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.global_search.RepositorySearchManager.error.open_location", e);
					SwingTools.showVerySimpleErrorMessage("global_search.cannot_open", e.getMessage());
				}
			}
		};
		worker.start();
	}

	/**
	 * Adds the fragment highlighting to the entry name.
	 *
	 * @param name
	 * 		the base name
	 * @param bestFragments
	 * 		the best fragments as returned from the search
	 * @return the new HTML-formatted name or the original name
	 */
	private static String formatName(final String name, final String[] bestFragments) {
		if (bestFragments != null) {
			return GlobalSearchGUIUtilities.INSTANCE.createHTMLHighlightFromString(name, bestFragments);
		}

		return name;
	}

	/**
	 * Returns the icon for the given entry.
	 *
	 * @param entry
	 * 		the entry, must not be {@code null}
	 * @param document
	 * 		the document, must not be {@code null}
	 * @return the icon, never {@code null}
	 */
	private static Icon getIconForEntry(final Entry entry, final Document document) {
		if (entry instanceof ProcessEntry) {
			return PROCESS_ICON;
		} else if (entry instanceof Model) {
			return MODEL_ICON;
		} else if (entry instanceof ConnectionEntry) {
			String connectionType = document.get(RepositoryGlobalSearch.FIELD_CONNECTION_TYPE);
			// server connections do not have the type known unless detailed indexing is enabled, so make sure we show the generic connection icon and not crash
			return ConnectionI18N.getConnectionIcon(connectionType != null ? connectionType : "", IconSize.SMALL);
		} else if (entry instanceof IOObjectEntry) {
			IOObjectEntry dataEntry = (IOObjectEntry) entry;
			return RendererService.getIcon(dataEntry.getObjectClass(), IconSize.SMALL);
		} else {
			return DATA_ICON;
		}
	}
}
