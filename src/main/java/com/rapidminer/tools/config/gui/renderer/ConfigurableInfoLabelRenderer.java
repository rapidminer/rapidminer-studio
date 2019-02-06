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
package com.rapidminer.tools.config.gui.renderer;

import java.awt.Component;
import java.awt.Image;
import java.awt.image.ImageObserver;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.config.Configurable;


/**
 * The renderer for the {@link JList} displaying {@link JLabel}s with the state of the
 * {@link Configurable}'s {@link RemoteRepository} (LOADING, FAILED, NO CONNECTIONS)
 *
 * @author Sabrina Kirstein
 *
 */
public class ConfigurableInfoLabelRenderer extends DefaultListCellRenderer {

	public enum ConfigurableInfoLabelType {
		LOADING, FAILED, NO_CONNECTIONS;
	}

	private static final long serialVersionUID = 1L;

	/** icon displayed in case of loading configurables */
	private static final ImageIcon LOADING_ICON = SwingTools.createIcon("16/loading.gif");

	/** icon displayed in case of an empty list of configurables */
	private static final ImageIcon NO_CONNECTIONS_ICON = SwingTools.createIcon("16/information.png");

	/** icon displayed in case of loading configurabled failed */
	private static final ImageIcon CONNECTION_FAILED_ICON = SwingTools.createIcon("16/error.png");

	/** text displayed in case of loading configurables failed */
	private static final String TEXT_LOADING_CONNECTIONS_FAILED = I18N
			.getGUILabel("configurable_dialog.loading_configurables.failed");

	/** text displayed in case of loading configurables */
	private static final String TEXT_LOADING_CONNECTIONS = I18N.getGUILabel("configurable_dialog.loading_configurables");

	/** text displayed in case of an empty list of configurables */
	private static final String TEXT_NO_CONNECTIONS = I18N.getGUILabel("configurable_dialog.loading_configurables.empty");

	@Override
	public Component getListCellRendererComponent(final JList<? extends Object> list, Object value, int index,
			boolean isSelected, boolean cellHasFocus) {

		final JLabel renderer = (JLabel) super.getListCellRendererComponent(list, value, index, false, false);
		String typeString = (String) value;
		ConfigurableInfoLabelType type = ConfigurableInfoLabelType.valueOf(typeString);
		String text;

		switch (type) {
			case FAILED:
				text = TEXT_LOADING_CONNECTIONS_FAILED;
				renderer.setIcon(CONNECTION_FAILED_ICON);
				break;
			case LOADING:
				text = TEXT_LOADING_CONNECTIONS;
				// needs to be done to see the animation of the gif in a cell renderer
				LOADING_ICON.setImageObserver(new ImageObserver() {

					@Override
					public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
						if (renderer.isEnabled() && (infoflags & (FRAMEBITS | ALLBITS)) != 0) {
							renderer.repaint();
							if (list.isShowing()) {
								list.repaint();
							}
						}
						return (infoflags & (ALLBITS | ABORT)) == 0;
					}
				});
				renderer.setIcon(LOADING_ICON);
				break;
			default:
			case NO_CONNECTIONS:
				text = TEXT_NO_CONNECTIONS;
				renderer.setIcon(NO_CONNECTIONS_ICON);
				break;
		}

		renderer.setText(" " + text);
		renderer.setBorder(BorderFactory.createCompoundBorder(renderer.getBorder(),
				BorderFactory.createEmptyBorder(0, 9, 0, 0)));
		return renderer;
	}
}
