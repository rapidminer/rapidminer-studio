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

import java.awt.Component;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.internal.remote.RemoteDataEntry;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.tools.Tools;


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
	private static final Icon ICON_BLOB = SwingTools.createIcon("16/document_empty.png");
	private static final Icon ICON_TEXT = SwingTools.createIcon("16/text.png");
	private static final Icon ICON_TABLE = SwingTools.createIcon("16/spreadsheet.png");
	private static final Icon ICON_IMAGE = SwingTools.createIcon("16/photo_landscape.png");
	private static final Icon ICON_CONNECTION_INFORMATION = SwingTools.createIcon("16/" + ConnectionI18N.CONNECTION_ICON);

	/** stores the icons for all repository implementations */
	private static Map<String, Icon> ICON_REPOSITORY_MAP = Collections.synchronizedMap(new HashMap<>());

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
			labelText.append("<html>").append(entry.getName());

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
				int revision = ((DataEntry) entry).getRevision();
				if (hasOwner || hasState) {
					appendDash(stateStringBuilder);
				}
				stateStringBuilder.append("v").append(revision);
				long date = ((DataEntry) entry).getDate();
				if (date >= 0) {
					stateStringBuilder.append(", ").append(DATE_FORMAT.format(new Date(date)));
				}
				long size = ((DataEntry) entry).getSize();
				if (size > 0) {
					appendDash(stateStringBuilder);
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
			if (entry instanceof Repository) {
				Repository repo = (Repository) entry;
				if (ICON_REPOSITORY_MAP.get(repo.getIconName()) == null) {
					ICON_REPOSITORY_MAP.put(repo.getIconName(), SwingTools.createIcon("16/" + repo.getIconName()));
				}
				label.setIcon(ICON_REPOSITORY_MAP.get(repo.getIconName()));
			} else if (entry.getType().equals(Folder.TYPE_NAME)) {
				if (((Folder) entry).isSpecialConnectionsFolder()) {
					label.setIcon(ICON_CONNECTION_FOLDER);
				}else if (entry.isReadOnly() && !expanded) {
					label.setIcon(ICON_FOLDER_CLOSED_LOCKED);
				} else if (entry.isReadOnly() && expanded) {
					label.setIcon(ICON_FOLDER_OPEN_LOCKED);
				} else if (!entry.isReadOnly() && expanded) {
					label.setIcon(ICON_FOLDER_OPEN);
				} else {
					label.setIcon(ICON_FOLDER_CLOSED);
				}
			} else if (entry.getType().equals(IOObjectEntry.TYPE_NAME)) {
				if (entry instanceof IOObjectEntry) {
					IOObjectEntry dataEntry = (IOObjectEntry) entry;
					label.setIcon(RendererService.getIcon(dataEntry.getObjectClass()));
				} else {
					label.setIcon(ICON_DATA);
				}
			} else if (entry.getType().equals(ProcessEntry.TYPE_NAME)) {
				label.setIcon(ICON_PROCESS);
			} else if (entry instanceof BlobEntry) {
				String mimeType = ((BlobEntry) entry).getMimeType();
				if (mimeType != null) {
					if (mimeType.startsWith("text/") || "application/pdf".equals(mimeType)
							|| "application/rtf".equals(mimeType)) {
						label.setIcon(ICON_TEXT);
					} else if (mimeType.equals("application/msexcel")) {
						label.setIcon(ICON_TABLE);
					} else if (mimeType.startsWith("image/")) {
						label.setIcon(ICON_IMAGE);
					} else {
						label.setIcon(ICON_BLOB);
					}
				} else {
					label.setIcon(ICON_BLOB);
				}
			} else if (entry.getType().equals(ConnectionEntry.TYPE_NAME)) {
				label.setIcon(getConnectionIcon(entry));
			} else {
				label.setIcon(null);
			}
		}

		label.setBorder(ENTRY_BORDER);
		return label;
	}

	/**
	 * Retrieve a cached icon for the given icon name for a repository.
	 *
	 * @param iconName
	 * 		name of the icon as set in {@link Repository#getIconName()}
	 * @return the icon, can be null
	 * @since 9.3
	 */
	public static Icon getRepositoryIcon(String iconName) {
		return ICON_REPOSITORY_MAP.get(iconName);
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
}
