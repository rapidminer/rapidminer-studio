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
package com.rapidminer.gui.new_plotter.templates.style;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.gui.new_plotter.utility.ListUtility;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.AbstractChangeAwareSaveable;
import com.rapidminer.tools.XMLException;


/**
 * Contains a color scheme. May contain the same colors multiple times.
 *
 * @author Marco Boeck, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ColorScheme extends AbstractChangeAwareSaveable<ColorRGB> {

	public static final String XML_TAG_NAME = "color-scheme";
	private static final String GRADIENT_END_COLOR_XML_TAG = "gradient-end-color";
	private static final String GRADIENT_START_COLOR_XML_TAG = "gradient-start-color";
	private static final String COLORS_XML_TAG = "category-colors";
	private static final String NAME_XML_TAG = "name";

	/** the name of the {@link ColorScheme} */
	private String name;

	/** the colors which this scheme contains */
	private List<ColorRGB> listOfColors;

	private ColorRGB gradientStartColor;
	private ColorRGB gradientEndColor;

	private transient String repositoryLocation;
	private transient boolean initialized = false;

	/**
	 * Creates a new {@link ColorScheme}.
	 *
	 * @param name
	 *            the name of the color scheme
	 * @param listOfColors
	 *            a list with the colors the scheme should contain
	 */
	public ColorScheme(String name, List<ColorRGB> listOfColors) {

		if (name == null) {
			throw new IllegalArgumentException("name must not be null!");
		}
		if (listOfColors == null) {
			throw new IllegalArgumentException("listOfColors must not be null!");
		}
		if (listOfColors.size() < 1) {
			throw new IllegalArgumentException("listOfColors must not be empty!");
		}

		this.name = name;
		this.listOfColors = listOfColors;
		this.gradientStartColor = listOfColors.get(0);
		this.gradientEndColor = listOfColors.get(listOfColors.size() - 1);
		this.initialized = true;
	}

	public ColorScheme(String name, List<ColorRGB> listOfColors, ColorRGB gradientStart, ColorRGB gradientEnd) {
		this(name, listOfColors);
		this.gradientStartColor = gradientStart;
		this.gradientEndColor = gradientEnd;
		this.initialized = true;
	}

	public ColorScheme(Element element) throws XMLException {
		if (!XML_TAG_NAME.equals(element.getTagName())) {
			throw new XMLException("<" + XML_TAG_NAME + "> expected.");
		}

		name = XMLTools.getTagContents(element, NAME_XML_TAG);

		Element gradientStartColorElement = XMLTools.getChildTag(element, GRADIENT_START_COLOR_XML_TAG, false);
		gradientStartColor = new ColorRGB(XMLTools.getChildTag(gradientStartColorElement, ColorRGB.XML_TAG_NAME, false));

		Element gradientEndColorElement = XMLTools.getChildTag(element, GRADIENT_END_COLOR_XML_TAG, false);
		gradientEndColor = new ColorRGB(XMLTools.getChildTag(gradientEndColorElement, ColorRGB.XML_TAG_NAME, false));

		listOfColors = new LinkedList<ColorRGB>();
		Element categoryColorsElement = XMLTools.getChildTag(element, COLORS_XML_TAG, false);
		Collection<Element> categoryColors = XMLTools.getChildElements(categoryColorsElement, ColorRGB.XML_TAG_NAME);
		for (Element colorElement : categoryColors) {
			addColor(new ColorRGB(colorElement));
		}
		this.initialized = true;
	}

	/**
	 * Returns the name of this {@link ColorScheme}.
	 *
	 * @return
	 */
	public String getName() {
		return toString();
	}

	/**
	 * @return the gradientStartColor
	 */
	public ColorRGB getGradientStartColor() {
		return gradientStartColor;
	}

	/**
	 * @return the gradientEndColor
	 */
	public ColorRGB getGradientEndColor() {
		return gradientEndColor;
	}

	/**
	 * @param gradientStartColor
	 *            the gradientStartColor to set
	 */
	public void setGradientStartColor(ColorRGB gradientStartColor) {
		this.gradientStartColor = gradientStartColor;
		fireUpdate(this);
	}

	/**
	 * @param gradientEndColor
	 *            the gradientEndColor to set
	 */
	public void setGradientEndColor(ColorRGB gradientEndColor) {
		this.gradientEndColor = gradientEndColor;
		fireUpdate(this);
	}

	/**
	 * Sets the name of this {@link ColorScheme}.
	 *
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
		fireUpdate(this);
	}

	/**
	 * Sets the list of {@link ColorRGB} for this {@link ColorScheme}.
	 *
	 * @param listOfColors
	 */
	public void setColors(List<ColorRGB> listOfColors) {
		if (listOfColors == null) {
			throw new IllegalArgumentException("listOfColors must not be null!");
		}
		this.listOfColors = listOfColors;
		fireUpdate(this);
	}

	/**
	 * Adds a color to the color scheme.
	 */
	public void addColor(ColorRGB color) {
		this.listOfColors.add(color);
		observeForChanges(color);
		fireUpdate(this);
	}

	/**
	 * Adds a color to the color scheme at specified index. Shifts the element currently at that
	 * position (if any) and any subsequent elements to the right (adds one to their indices). If
	 * the color is already present in current color scheme it will be removed from its old position
	 * and add to the new index.
	 */
	public void addColor(int index, ColorRGB color) {
		int oldIdx = listOfColors.indexOf(color);
		if (oldIdx != -1) {
			ListUtility.changeIndex(listOfColors, color, index);
		} else {
			this.listOfColors.add(index, color);
			observeForChanges(color);
		}
		fireUpdate(this);
	}

	/**
	 * Removes the specified color from the {@link ColorScheme}. Will first be checked by class
	 * reference. If reference cannot be found (should not happen), the color is removed via
	 * {@link List#remove(Object)}.
	 */
	public void removeColor(ColorRGB color) {
		this.listOfColors.remove(color);
		stopObservingForChanges(color);
		fireUpdate(this);
	}

	/**
	 * Removes the color at the provided index.
	 */
	public void removeColor(int index) {
		ColorRGB color = this.listOfColors.remove(index);
		stopObservingForChanges(color);
		fireUpdate(this);
	}

	/**
	 * Replaces old color with new color if old color is already in color scheme.
	 */
	public void setColor(ColorRGB oldColor, ColorRGB newColor) {
		int index = listOfColors.indexOf(oldColor);
		if (index != -1) {
			this.listOfColors.set(index, newColor);
		}
		fireUpdate(this);
	}

	/**
	 * Returns a list with all {@link ColorRGB} objects this {@link ColorScheme} consists of.
	 */
	public List<ColorRGB> getColors() {
		return new LinkedList<ColorRGB>(listOfColors);
	}

	/**
	 * @return the category colors as an array of colors in hex presentation without a preceding
	 *         '#'.
	 */
	public String[] getColorsAsHexArray() {
		List<ColorRGB> colors = getColors();
		String[] hexColors = new String[colors.size()];
		int index = 0;
		for (ColorRGB color : colors) {
			hexColors[index] = ColorRGB.convertColorRGBToHex(color);
			++index;
		}
		return hexColors;
	}

	public String getGradientStartColorAsHex() {
		return ColorRGB.convertColorRGBToHex(getGradientStartColor());
	}

	public String getGradientEndColorAsHex() {
		return ColorRGB.convertColorRGBToHex(getGradientEndColor());
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public ColorScheme clone() {
		List<ColorRGB> clonedList = new LinkedList<ColorRGB>();
		for (ColorRGB color : listOfColors) {
			clonedList.add(color.clone());
		}
		return new ColorScheme(name, clonedList, gradientStartColor.clone(), gradientEndColor.clone());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof ColorScheme)) {
			return false;
		}
		ColorScheme givenColorScheme = (ColorScheme) obj;

		if (!name.equals(givenColorScheme.getName())) {
			return false;
		}

		if (!gradientStartColor.equals(givenColorScheme.getGradientStartColor())) {
			return false;
		}

		if (!gradientEndColor.equals(givenColorScheme.getGradientEndColor())) {
			return false;
		}

		if (givenColorScheme.getColors().size() != this.getColors().size()) {
			return false;
		}

		for (int i = 0; i < givenColorScheme.getColors().size(); i++) {
			ColorRGB givenColorRGB = givenColorScheme.getColors().get(i);
			ColorRGB thisColorRGB = this.getColors().get(i);
			if (!givenColorRGB.equals(thisColorRGB)) {
				return false;
			}
		}

		return true;
	}

	public Document toXML() {
		Document doc = XMLTools.createDocument();
		doc.appendChild(toXML(doc));
		return doc;
	}

	public Element toXML(Document doc) {
		Element root = doc.createElement(XML_TAG_NAME);
		XMLTools.setTagContents(root, NAME_XML_TAG, getName());

		Element colors = XMLTools.addTag(root, COLORS_XML_TAG);
		for (ColorRGB color : getColors()) {
			colors.appendChild(color.toXML(doc));
		}

		Element gradientStartColorTag = XMLTools.addTag(root, GRADIENT_START_COLOR_XML_TAG);
		gradientStartColorTag.appendChild(getGradientStartColor().toXML(doc));

		Element gradientEndColorTag = XMLTools.addTag(root, GRADIENT_END_COLOR_XML_TAG);
		gradientEndColorTag.appendChild(getGradientEndColor().toXML(doc));

		return root;
	}

	/**
	 * Parses the output generated by {@link #toXML(Document)}.
	 **/
	public static ColorScheme fromXML(Document doc) throws XMLException {
		return new ColorScheme(doc.getDocumentElement());
	}

	/**
	 * @return the repositoryLocation
	 */
	public String getRepositoryLocation() {
		return this.repositoryLocation;
	}

	/**
	 * @param repositoryLocation
	 *            the repositoryLocation to set
	 */
	public void setRepositoryLocation(String repositoryLocation) {
		this.repositoryLocation = repositoryLocation;
	}

	public void exchange(ColorRGB c1, ColorRGB c2) {
		if (c1 == c2) {
			return;
		}
		int i1 = listOfColors.indexOf(c1);
		int i2 = listOfColors.indexOf(c2);
		if (i1 != -1 && i2 != -1) {
			listOfColors.set(i1, c2);
			listOfColors.set(i2, c1);
		}
		fireUpdate(this);
	}

	/**
	 * Exchanges two colors by index
	 * 
	 * @param i1
	 *            Index of the first color
	 * @param i2
	 *            Index of the second color
	 */
	public void exchange(int i1, int i2) {
		if (i1 == i2) {
			return;
		}
		if (i1 > -1 && i2 > -1 && listOfColors.size() > i1 && listOfColors.size() > i2) {
			ColorRGB c1 = listOfColors.get(i1);
			ColorRGB c2 = listOfColors.get(i2);
			listOfColors.set(i1, c2);
			listOfColors.set(i2, c1);
		}
		fireUpdate(this);
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public String getPath() {
		return getRepositoryLocation();
	}

	@Override
	public boolean isAlreadyStored() {
		return getRepositoryLocation() != null;
	}

}
