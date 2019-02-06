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
package com.rapidminer.gui.new_plotter;

import com.rapidminer.gui.new_plotter.listener.MasterOfDesasterListener;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Tools;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * This class can be used to register errors which occur during plotting and cannot be caught during
 * the configuration of the plot.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class MasterOfDesaster {

	private List<ConfigurationChangeResponse> configChangeResponseList = new LinkedList<ConfigurationChangeResponse>();
	private List<WeakReference<MasterOfDesasterListener>> listeners = new LinkedList<WeakReference<MasterOfDesasterListener>>();
	private boolean calculating = false;

	private static final URL OKAY_ICON_URL = Tools.getResource("icons/16/ok.png");
	private static final URL CALCULATING_ICON_URL = Tools.getResource("icons/16/calculator.png");
	private static final String CALCULATE_LABEL = I18N.getGUILabel("plotter.master_of_desaster.calculating.label");
	private static final String NO_PROBLEMS_LABEL = I18N
			.getGUILabel("plotter.master_of_desaster.no_problems_detected.label");

	public void clearAll() {
		configChangeResponseList.clear();
		fireChanged();
	}

	private void removeEmptyResponses() {
		List<ConfigurationChangeResponse> copy = new LinkedList<ConfigurationChangeResponse>(configChangeResponseList);
		for (ConfigurationChangeResponse change : copy) {
			if (change.isEmpty()) {
				configChangeResponseList.remove(change);
			}
		}
	}

	public void clearWarnings() {
		for (ConfigurationChangeResponse change : configChangeResponseList) {
			change.clearWarnings();
		}
		removeEmptyResponses();
		fireChanged();
	}

	public void clearErrors() {
		for (ConfigurationChangeResponse change : configChangeResponseList) {
			change.clearErrors();
		}
		removeEmptyResponses();
		fireChanged();
	}

	public void removeConfigurationChangeResponse(ConfigurationChangeResponse response) {
		configChangeResponseList.remove(response);
		fireChanged();
	}

	public void registerConfigurationChangeResponse(ConfigurationChangeResponse error) {
		if (calculating) {
			calculating = false;
		}
		configChangeResponseList.add(error);
		fireChanged();
	}

	public List<ConfigurationChangeResponse> getConfigurationChangeResponses() {
		return configChangeResponseList;
	}

	private void fireChanged() {

		// copy listeners
		List<WeakReference<MasterOfDesasterListener>> listeners = new LinkedList<WeakReference<MasterOfDesasterListener>>();
		listeners.addAll(this.listeners);

		Iterator<WeakReference<MasterOfDesasterListener>> it = listeners.iterator();
		while (it.hasNext()) {
			MasterOfDesasterListener listener = it.next().get();
			if (listener == null) {
				it.remove();
			} else {
				listener.masterOfDesasterChanged(this);
			}
		}
	}

	public void addListener(MasterOfDesasterListener l) {
		listeners.add(new WeakReference<MasterOfDesasterListener>(l));
	}

	public void removeListener(MasterOfDesasterListener l) {
		Iterator<WeakReference<MasterOfDesasterListener>> it = listeners.iterator();
		while (it.hasNext()) {
			MasterOfDesasterListener listener = it.next().get();
			if (listener == null || listener == l) {
				it.remove();
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (ConfigurationChangeResponse response : configChangeResponseList) {
			if (response != null) {
				builder.append(response.toString());
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	/**
	 * @return
	 */
	public String toHtmlString() {
		StringBuilder builder = new StringBuilder();
		builder.append("<html>");
		if (calculating) {
			builder.append("<div style=\"color:#0066CC;\">");
			builder.append("<img valign=\"middle\" style=\"vertical-align:middle;\" src=\"" + CALCULATING_ICON_URL
					+ "\"/>&nbsp;");
			builder.append(CALCULATE_LABEL);
			builder.append("</div>");
			return builder.toString();
		}
		if (configChangeResponseList.size() > 0) {
			for (ConfigurationChangeResponse response : configChangeResponseList) {
				if (response != null) {
					builder.append(response.toHtmlString());
				}
			}
		} else {
			builder.append("<div style=\"color:#000000;\">");
			builder.append("<img valign=\"middle\" style=\"vertical-align:middle;\" src=\"" + OKAY_ICON_URL + "\"/>&nbsp;");
			builder.append(NO_PROBLEMS_LABEL);
			builder.append("</div>");
		}
		builder.append("</html>");
		return builder.toString();
	}

	/**
	 * 
	 */
	public void setCalculating(boolean calculating) {
		this.calculating = calculating;
		fireChanged();
	}
}
