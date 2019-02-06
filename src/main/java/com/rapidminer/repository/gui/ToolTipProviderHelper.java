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

import com.rapidminer.gui.metadata.MetaDataRendererFactoryRegistry;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;

import java.awt.Component;
import java.util.logging.Level;


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
					// LogService.getRoot().log(Level.WARNING,
					// "Cannot fetch meta data for tool tip: " + e, e);
					LogService.getRoot().log(
							Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.repository.gui.ToolTipProviderHelper.fetching_meta_data_for_tool_error",
									e), e);

					return null;
				}
			} else {
				tip.append("<p>Meta data for this object not loaded yet.<br/><a href=\"loadMetaData?");
				tip.append(e.getLocation().toString());
				tip.append("\">Click to load.</a></p>");
			}
			return tip.toString();
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
					Component renderer = MetaDataRendererFactoryRegistry.getInstance().createRenderer(metaData);
					return renderer;
					// if ((metaData != null) && (metaData instanceof ExampleSetMetaData)) {
					// return ExampleSetMetaDataTableModel.makeTableForToolTip((ExampleSetMetaData)
					// metaData);
					// }
				} catch (Exception ex) {
					// LogService.getRoot().log(Level.WARNING, "Error retrieving meta data for " +
					// e.getLocation() + ": " + ex, ex);
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
