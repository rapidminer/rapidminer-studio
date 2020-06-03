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
package com.rapidminer.repository.gui;

import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.filechooser.FileView;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.apache.commons.io.FileUtils;

import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.gui.tools.MultiSwingWorker;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.repository.internal.remote.RemoteDataEntry;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.repository.versioned.IOObjectFileTypeHandler;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.TempFileTools;
import com.rapidminer.tools.Tools;
import com.rapidminer.versioning.repository.FileTypeHandlerRegistry;


/**
 * @author Simon Fischer
 */
public class RepositoryTreeCellRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1L;

	private static final Icon ICON_FOLDER_OPEN = SwingTools.createIcon("16/folder_open.png");
	private static final Icon ICON_FOLDER_CLOSED = SwingTools.createIcon("16/folder.png");
	private static final Icon ICON_FOLDER_OPEN_LOCKED = SwingTools.createIcon("16/folder_open_lock.png");
	private static final Icon ICON_FOLDER_CLOSED_LOCKED = SwingTools.createIcon("16/folder_lock.png");
	private static final Icon ICON_CONNECTION_FOLDER = SwingTools.createIcon("16/plug.png");
	private static final Icon ICON_PROCESS = SwingTools.createIcon("16/gearwheels.png");
	private static final Icon ICON_DATA = SwingTools.createIcon("16/data.png");
	private static final Icon ICON_BINARY_FILE = SwingTools.createIcon("16/document_empty.png");
	private static final Icon ICON_IMAGE = SwingTools.createIcon("16/photo_landscape.png");
	private static final Icon ICON_CONNECTION_INFORMATION = SwingTools.createIcon("16/" + ConnectionI18N.CONNECTION_ICON);
	private static final Icon UNKNOWN_ICON = SwingTools.createIcon("16/question.png");

	/** stores the icons for all repository implementations */
	private static final Map<String, Icon> ICON_REPOSITORY_MAP = Collections.synchronizedMap(new HashMap<>());

	/** stores the icons for file suffixes */
	private static final Map<String, Icon> SUFFIX_ICON_CACHE = Collections.synchronizedMap(new HashMap<>());

	static {
		SUFFIX_ICON_CACHE.put("", ICON_BINARY_FILE);
	}

	private static final JFileChooser FILE_CHOOSER = new JFileChooser();
	private static final FileView FILE_VIEW = FILE_CHOOSER.getUI().getFileView(FILE_CHOOSER);
	/** if this is set to true, icon loading took too long at least once and thus will be done async in the future to avoid blocking the EDT */
	private static final AtomicBoolean ASYNC_LOAD_FLAG = new AtomicBoolean(false);
	private static final int ICON_LOADING_ASYNC_THRESHOLD_MS = 100;

	// clone because getDateInstance uses an internal pool which can return the same
	// instance for multiple threads
	private final DateFormat DATE_FORMAT = (DateFormat) DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
			.clone();

	private static final Border ENTRY_BORDER = BorderFactory.createEmptyBorder(1, 0, 1, 0);

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
												  int row, boolean hasFocus) {
		JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		if (value instanceof Entry) {
			Entry entry = (Entry) value;

			StringBuilder labelText = new StringBuilder();
			String escapedString = Tools.escapeHTML(entry.getName());
			escapedString = escapedString.replaceAll("\\s", "&nbsp;");
			labelText.append("<html>").append(escapedString);

			StringBuilder stateStringBuilder = new StringBuilder();
			boolean hasState = false;
			if (entry instanceof Repository) {
				if (entry instanceof RemoteRepository) {
					RemoteRepository remoteRepository = (RemoteRepository) entry;
					final VersionNumber remoteRepositoryVersion = remoteRepository.getKnownServerVersion();
					if (remoteRepositoryVersion != null) {
						String versionNumber = remoteRepositoryVersion.getShortVersion();
						stateStringBuilder.append("v").append(versionNumber).append("&nbsp;");
					}
				}
				String reposState = ((Repository) entry).getState();
				if (reposState != null) {
					stateStringBuilder.append(reposState);
					hasState = true;
				}
			}
			boolean hasOwner = false;
			if (entry.getOwner() != null) {
				if (hasState) {
					appendDash(stateStringBuilder);
				}
				stateStringBuilder.append(entry.getOwner());
				hasOwner = true;
			}
			if (entry instanceof DataEntry) {
				if (hasOwner || hasState) {
					appendDash(stateStringBuilder);
				}
				long date = ((DataEntry) entry).getDate();
				if (date > 0) {
					stateStringBuilder.append(" ").append(DATE_FORMAT.format(new Date(date)));
					appendDash(stateStringBuilder);
				}
				long size = ((DataEntry) entry).getSize();
				if (size >= 0) {
					stateStringBuilder.append(Tools.formatBytes(size));
				} else if (entry instanceof RemoteDataEntry && size < 0) {
					appendDash(stateStringBuilder);
					stateStringBuilder.append("&ge;2 GB");
				}
			}
			if (stateStringBuilder.length() > 0) {
				labelText.append(" <small style=\"color:gray\">(").append(stateStringBuilder).append(")</small>");
			}

			labelText.append("</html>");
			label.setText(labelText.toString());
			Icon icon;
			// folders have their own handling here due to open/closed state and different icons for those
			if (!(entry instanceof Repository) && Folder.TYPE_NAME.equals(entry.getType())) {
				if (((Folder) entry).isSpecialConnectionsFolder()) {
					icon = ICON_CONNECTION_FOLDER;
				} else if (entry.isReadOnly() && !expanded) {
					icon = ICON_FOLDER_CLOSED_LOCKED;
				} else if (entry.isReadOnly() && expanded) {
					icon = ICON_FOLDER_OPEN_LOCKED;
				} else if (!entry.isReadOnly() && expanded) {
					icon = ICON_FOLDER_OPEN;
				} else {
					icon = ICON_FOLDER_CLOSED;
				}
			} else {
				icon = getIconForEntry(entry);
			}
			label.setIcon(icon);
		}

		label.setBorder(ENTRY_BORDER);
		return label;
	}

	/**
	 * Retrieve a cached icon for the given icon name for a repository.
	 *
	 * @param iconName
	 * 		name of the icon as set in {@link Repository#getIconName()}
	 * @return the icon, can be {@code null}
	 * @since 9.3
	 */
	public static Icon getRepositoryIcon(String iconName) {
		if (ICON_REPOSITORY_MAP.get(iconName) == null) {
			ICON_REPOSITORY_MAP.put(iconName, SwingTools.createIcon("16/" + iconName));
		}
		return ICON_REPOSITORY_MAP.get(iconName);
	}

	/**
	 * Tries to fetch an icon for an entry. The icon may not yet be loaded (e.g. for binary entries), and will only
	 * start loading asynchronously after this call. In these cases, a placeholder icon will be returned. Subsequent
	 * calls of this method after async loading has finished will return the actual icon. This call never blocks.
	 *
	 * @param entry the entry, must not be {@code null}
	 * @return the icon, never {@code null}
	 * @since 9.7
	 */
	public static Icon getIconForEntry(Entry entry) {
		if (entry == null) {
			return UNKNOWN_ICON;
		}

		Icon icon;
		if (entry instanceof Repository) {
			icon = getRepositoryIcon(((Repository) entry).getIconName());
		} else if (entry instanceof Folder) {
			if (((Folder) entry).isSpecialConnectionsFolder()) {
				icon = ICON_CONNECTION_FOLDER;
			} else if (entry.isReadOnly()) {
				icon = ICON_FOLDER_OPEN_LOCKED;
			} else {
				icon = ICON_FOLDER_OPEN;
			}
		} else if (entry instanceof ConnectionEntry) {
			icon = getConnectionIcon(entry);
		} else if (entry instanceof IOObjectEntry) {
			IOObjectEntry dataEntry = (IOObjectEntry) entry;
			icon = RendererService.getIcon(dataEntry.getObjectClass());
		} else if (entry instanceof ProcessEntry) {
			icon = ICON_PROCESS;
		} else if (entry instanceof BlobEntry) {
			String mimeType = ((BlobEntry) entry).getMimeType();
			if (mimeType != null) {
				// we keep image icon for old blob entries that were used for process background images
				if (mimeType.startsWith("image/")) {
					icon = ICON_IMAGE;
				} else {
					icon = ICON_BINARY_FILE;
				}
			} else {
				icon = ICON_BINARY_FILE;
			}
		} else if (entry instanceof BinaryEntry) {
			BinaryEntry binEntry = (BinaryEntry) entry;
			String suffix = binEntry.getSuffix() != null ? binEntry.getSuffix() : "";
			icon = getIconForFileSuffix(suffix);
		} else {
			icon = ICON_DATA;
		}

		return icon;
	}

	/**
	 * Tries to fetch an icon for a given file suffix. The icon may not yet be loaded, and will only start loading
	 * asynchronously after this call. In these cases, a placeholder icon will be returned. Subsequent calls of this
	 * method after async loading has finished will return the actual icon. This call never blocks.
	 *
	 * @param suffix the suffix without the dot, e.g. 'py', must not be {@code null}. See {@link
	 *               com.rapidminer.repository.RepositoryTools#getSuffixFromFilename(String)}
	 * @return the icon, never {@code null}
	 * @since 9.7
	 */
	public static Icon getIconForFileSuffix(String suffix) {
		if (suffix == null) {
			return UNKNOWN_ICON;
		}

		Icon icon;
		if (SUFFIX_ICON_CACHE.containsKey(suffix)) {
			icon = SUFFIX_ICON_CACHE.get(suffix);
		} else {
			// we need to load the icon
			// is icon loading fast? Then do it synchronously for better UX
			if (!ASYNC_LOAD_FLAG.get()) {
				long start = System.currentTimeMillis();
				icon = SUFFIX_ICON_CACHE.computeIfAbsent(suffix, RepositoryTreeCellRenderer::getIcon);
				long end = System.currentTimeMillis();
				if (end - start > ICON_LOADING_ASYNC_THRESHOLD_MS) {
					// loading took too long, in the future we load async
					ASYNC_LOAD_FLAG.set(true);
				}
			} else {
				loadFileIconAsync(suffix);
				// use temp icon until it's loaded - we cannot refresh this, so we will place it in the cache and next time it will be used
				icon = SUFFIX_ICON_CACHE.computeIfAbsent(suffix, s -> ICON_BINARY_FILE);
			}
		}
		return icon;
	}

	/**
	 * Returns the connection icon for the entry
	 *
	 * @param entry the entry
	 * @return the icon for the entry or {@link #ICON_CONNECTION_INFORMATION}
	 */
	private static Icon getConnectionIcon(Entry entry) {
		try {
			return ConnectionI18N.getConnectionIcon(((ConnectionEntry) entry).getConnectionType(), IconSize.SMALL);
		} catch (Exception e) {
			// don't care, just show the default icon
			return ICON_CONNECTION_INFORMATION;
		}
	}

	/**
	 * Appends a - to the provided StringBuilder
	 *
	 * @param state
	 *            the StringBuilder to add the - to
	 */
	private static void appendDash(StringBuilder state) {
		state.append(" &ndash; ");
	}

	/**
	 * Loads the repository entry async. This may take a while, especially for slow remote repositories, so don't do it
	 * on the EDT.
	 *
	 * @param suffix the file suffix for which to ask the OS for the icon
	 */
	private static void loadFileIconAsync(final String suffix) {
		MultiSwingWorker<Icon, Void> worker = new MultiSwingWorker<Icon, Void>() {

			@Override
			protected Icon doInBackground() throws Exception {
				return getIcon(suffix);
			}

			@Override
			protected void done() {
				try {
					Icon icon = get();
					if (icon != null) {
						SUFFIX_ICON_CACHE.put(suffix, icon);
					}
				} catch (Exception e) {
					// loading icon has failed, ignore
				}
			}
		};
		worker.start();
	}

	/**
	 * Gets the icon synchronously.
	 *
	 * @param suffix the suffix for which to get the icon
	 * @return the icon, or {@code null}
	 */
	private static Icon getIcon(String suffix) {
		IconProvider iconProvider = RepositoryEntryIconRegistry.getInstance().getCallback(suffix);
		if (iconProvider != null) {
			Icon icon = SwingTools.createIcon("16/" + iconProvider.getIconName());
			if (icon == null) {
				icon = ICON_BINARY_FILE;
			}
			return icon;
		}

		// TODO: refactor, they should return an icon as well
		if (FileTypeHandlerRegistry.getRegisteredSuffixes().contains(suffix)) {
			if (RepositoryTools.getSuffixFromFilename(ProcessEntry.RMP_SUFFIX).equals(suffix)) {
				return ICON_PROCESS;
			} else if (RepositoryTools.getSuffixFromFilename(ConnectionEntry.CON_SUFFIX).equals(suffix)) {
				return ICON_CONNECTION_INFORMATION;
			} else if (RepositoryTools.getSuffixFromFilename(IOObjectEntry.IOO_SUFFIX).equals(suffix)) {
				return ICON_DATA;
			} else if (IOObjectFileTypeHandler.DATA_TABLE_FILE_ENDING.equals(suffix)) {
				return ICON_DATA;
			} else if (RepositoryTools.getSuffixFromFilename(BlobEntry.BLOB_SUFFIX).equals(suffix)) {
				return ICON_BINARY_FILE;
			} else {
				return UNKNOWN_ICON;
			}
		}

		// no icon provided, now we check what the OS would show as an icon in the native file browser
		try {
			Path tempFolder = FileSystemService.getUserRapidMinerDir().toPath().resolve(FileSystemService.RAPIDMINER_INTERNAL_CACHE_TEMP_FULL);
			Path tempFile = Files.createFile(tempFolder.resolve("rm-icon-fetcher-" + UUID.randomUUID().toString() + "." + suffix));
			Icon icon = FILE_VIEW.getIcon(tempFile.toFile());
			if (!FileUtils.deleteQuietly(tempFile.toFile())) {
				TempFileTools.registerCleanup(tempFile);
			}
			if (icon == null || icon.getIconWidth() == 0 || icon.getIconHeight() == 0) {
				icon = ICON_BINARY_FILE;
			}
			return icon;
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.gui.RepositoryTreeCellRenderer.cannot_resolve_icon", e);
		}

		return null;
	}
}
