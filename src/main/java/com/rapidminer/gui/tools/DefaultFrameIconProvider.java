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
package com.rapidminer.gui.tools;

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;

import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.imageio.ImageIO;


/**
 * The default frame icon provider.
 * 
 * @author Ingo Mierswa
 */
public class DefaultFrameIconProvider implements FrameIconProvider {

	private final String frameIconBaseName;

	public DefaultFrameIconProvider(String frameIconBaseName) {
		this.frameIconBaseName = frameIconBaseName;
	}

	@Override
	public List<Image> getFrameIcons() {
		try {
			List<Image> frameIcons = new LinkedList<Image>();
			for (String size : FRAME_ICON_SIZES) {
				URL url = Tools.getResource(frameIconBaseName + size + ".png");
				if (url != null) {
					frameIcons.add(ImageIO.read(url));
				}
			}
			return frameIcons;
		} catch (IOException e) {
			// ignore this and do not use frame icons
			// LogService.getRoot().warning("Cannot load frame icons. Skipping...");
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.tools.DefaultFrameiconProvider.loading_frame_icons_error");
			return new LinkedList<Image>();
		}
	}
}
