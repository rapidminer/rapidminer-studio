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
package com.rapidminer.io.process;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapidminer.gui.flow.processrendering.background.ProcessBackgroundImage;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;


/**
 * {@link ProcessXMLFilter} to handle background image data.
 *
 * @author Michael Knopf, Marco Boeck, Nils Woehler
 * @since 7.2.0
 */
public class BackgroundImageProcessXMLFilter implements ProcessXMLFilter {

	/** user data key for process background image */
	public static final String KEY_PROCESS_BACKGROUND_IMAGE = "com.rapidminer.io.process.process_background_image";

	private static final String XML_ATTRIBUTE_HEIGHT = "height";
	private static final String XML_ATTRIBUTE_WIDTH = "width";
	private static final String XML_ATTRIBUTE_X_POSITION = "x";
	private static final String XML_ATTRIBUTE_Y_POSITION = "y";
	private static final String XML_ATTRIBUTE_LOCATION = "location";

	private static final String XML_TAG_BACKGROUND = "background";

	@Override
	public void operatorExported(final Operator op, final Element opElement) {}

	@Override
	public void executionUnitExported(final ExecutionUnit process, final Element element) {
		// add background image
		ProcessBackgroundImage image = lookupBackgroundImage(process);
		if (image != null) {
			Element backgroundElement = element.getOwnerDocument().createElement(XML_TAG_BACKGROUND);

			backgroundElement.setAttribute(XML_ATTRIBUTE_X_POSITION, "" + image.getX());
			backgroundElement.setAttribute(XML_ATTRIBUTE_Y_POSITION, "" + image.getY());
			backgroundElement.setAttribute(XML_ATTRIBUTE_WIDTH, "" + image.getOriginalWidth());
			backgroundElement.setAttribute(XML_ATTRIBUTE_HEIGHT, "" + image.getOriginalHeight());
			backgroundElement.setAttribute(XML_ATTRIBUTE_LOCATION, image.getLocation());

			element.appendChild(backgroundElement);
		}
	}

	@Override
	public void operatorImported(final Operator op, final Element opElement) {}

	/**
	 * Extracts port spacings from the XML element.
	 */
	@Override
	public void executionUnitImported(final ExecutionUnit process, final Element element) {
		NodeList children = element.getChildNodes();

		// background image
		children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element backgroundElement = (Element) child;
				if (XML_TAG_BACKGROUND.equals(backgroundElement.getTagName())) {

					String xStr = backgroundElement.getAttribute(XML_ATTRIBUTE_X_POSITION);
					String yStr = backgroundElement.getAttribute(XML_ATTRIBUTE_Y_POSITION);
					String wStr = backgroundElement.getAttribute(XML_ATTRIBUTE_WIDTH);
					String hStr = backgroundElement.getAttribute(XML_ATTRIBUTE_HEIGHT);
					String imgLocStr = backgroundElement.getAttribute(XML_ATTRIBUTE_LOCATION);

					try {
						int xLoc = Integer.parseInt(xStr);
						int yLoc = Integer.parseInt(yStr);
						int wLoc = Integer.parseInt(wStr);
						int hLoc = Integer.parseInt(hStr);
						ProcessBackgroundImage bgImg = new ProcessBackgroundImage(xLoc, yLoc, wLoc, hLoc, imgLocStr,
								process);
						setBackgroundImage(bgImg);
					} catch (NullPointerException | IllegalArgumentException e) {
						// ignore silently
					}
				}
			}
		}
	}

	/**
	 * Returns the background image for the given execution unit.
	 *
	 * @param process
	 *            the execution unit in question
	 * @return the background image or {@code null} if there is none
	 */
	public static ProcessBackgroundImage lookupBackgroundImage(ExecutionUnit process) {
		return (ProcessBackgroundImage) process.getUserData(KEY_PROCESS_BACKGROUND_IMAGE);
	}

	/**
	 * Adds a {@link ProcessBackgroundImage}.
	 *
	 * @param image
	 *            the new background image
	 */
	public static void setBackgroundImage(ProcessBackgroundImage image) {
		if (image == null) {
			throw new IllegalArgumentException("image must not be null!");
		}

		image.getProcess().setUserData(KEY_PROCESS_BACKGROUND_IMAGE, image);
	}

	/**
	 * Removes the given {@link ProcessBackgroundImage}.
	 *
	 * @param process
	 *            the execution unit for which to remove the background image
	 */
	public static void removeBackgroundImage(ExecutionUnit process) {
		process.setUserData(KEY_PROCESS_BACKGROUND_IMAGE, null);
	}
}
