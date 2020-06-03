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
import java.util.logging.Level;

import com.rapidminer.gui.metadata.MetaDataRendererFactoryRegistry;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.versioned.NewVersionedRepository;
import com.rapidminer.repository.versioned.VersionedRepositoryStatus;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * 
 * @author Simon Fischer
 * 
 */
public class ToolTipProviderHelper {

	public static String getTip(Entry o) {
		if (o instanceof IOObjectEntry) {
			IOObjectEntry e = (IOObjectEntry) o;
			StringBuilder tip = new StringBuilder();
			tip.append("<h3>").append(e.getName()).append("</h3>");
			if (!e.willBlock()) {
				try {
					MetaData metaData = e.retrieveMetaData();
					if (metaData != null) {
						tip.append("<p>");
						if (metaData instanceof ExampleSetMetaData) {
							tip.append(((ExampleSetMetaData) metaData).getShortDescription());
						} else {
							tip.append(metaData.getDescription());
						}
						tip.append("</p>");
					}
				} catch (RepositoryException e1) {
					LogService.getRoot().log(
							Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.repository.gui.ToolTipProviderHelper.fetching_meta_data_for_tool_error",
									e), e);

					return null;
				}
			} else {
				tip.append("<p>Meta data for this object not loaded yet.<br/><a href=\"loadMetaData?");
				tip.append(e.getLocation().toString()).append("|").append(e.getObjectClass().getName());
				tip.append("\">Click to load.</a></p>");
			}
			return tip.toString();
		} else if (o instanceof NewVersionedRepository) {
			NewVersionedRepository repo = (NewVersionedRepository) o;
			VersionedRepositoryStatus versionStatus = repo.getStatus();
			StringBuilder sb = new StringBuilder();
			sb.append("<h3>");
			sb.append(o.getName());
			sb.append("</h3><p>");
			sb.append(o.getDescription());
			sb.append("</p><br>");
			if (!versionStatus.isEncryptionContextKnown()) {
				sb.append(I18N.getGUIMessage("gui.repository.versioned_new.state_encryption_unknown.tip"));
				return sb.toString();
			}
			if (!versionStatus.isRemoteOriginExisting()) {
				sb.append(I18N.getGUIMessage("gui.repository.versioned_new.state_remote_origin_missing.tip"));
				return sb.toString();
			}
			if (!versionStatus.isRemoteTrackingBranchExisting()) {
				sb.append(I18N.getGUIMessage("gui.repository.versioned_new.state_remote_tracking_branch_missing.tip", versionStatus.getCurrentBranch()));
				return sb.toString();
			}
			if (!versionStatus.isConnected()) {
				sb.append(I18N.getGUIMessage("gui.repository.versioned_new.state_not_connected.tip"));
				return sb.toString();
			}

			// all good, no problems
			if (!versionStatus.getCurrentBranch().isEmpty()) {
				sb.append("<p>");
				sb.append(I18N.getGUIMessage("gui.repository.versioned_new.state_branch.label", versionStatus.getCurrentBranch()));
				sb.append("</p><br>");
			}
			if (repo.isReadOnly()) {
				sb.append("<p>");
				sb.append(I18N.getGUIMessage("gui.repository.versioned_new.state_read_only.tip"));
				sb.append("</p><br>");
			}
			if (versionStatus.isUpToDate()) {
				sb.append(I18N.getGUIMessage("gui.repository.versioned_new.state_up_to_date.tip"));
				return sb.toString();
			}
			boolean ahead = versionStatus.getNumberOfLocalChanges() > 0 || versionStatus.getNumberOfCommitsAhead() > 0;
			boolean behind = versionStatus.getNumberOfCommitsBehind() > 0;
			if (behind) {
				sb.append(I18N.getGUIMessage("gui.repository.versioned_new.state_behind.tip", versionStatus.getNumberOfCommitsBehind()));
				return sb.toString();
			}
			if (ahead && !repo.isReadOnly()) {
				sb.append(I18N.getGUIMessage("gui.repository.versioned_new.state_ahead.tip", versionStatus.getNumberOfLocalChanges() + versionStatus.getNumberOfCommitsAhead()));
				return sb.toString();
			}
			return sb.toString();
		} else {
			StringBuilder tip = new StringBuilder();
			tip.append("<h3>").append((o).getName()).append("</h3><p>").append((o).getDescription()).append("</p>");
			if (o instanceof BlobEntry) {
				tip.append("<p><strong>Type:</strong> ").append(((BlobEntry) o).getMimeType()).append("</p>");
			}
			return tip.toString();
		}
	}

	public static Component getCustomComponent(Entry o) {
		if (o instanceof IOObjectEntry) {
			IOObjectEntry e = (IOObjectEntry) o;
			if (!e.willBlock()) {
				try {
					MetaData metaData = e.retrieveMetaData();
					return MetaDataRendererFactoryRegistry.getInstance().createRenderer(metaData);
				} catch (Exception ex) {
					LogService.getRoot().log(
							Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.repository.gui.ToolTipProviderHelper.retrieving_meta_data_error",
									e.getLocation(), ex), ex);

				}
			}
		}
		return null;
	}

}
