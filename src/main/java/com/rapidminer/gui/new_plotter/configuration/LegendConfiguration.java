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
package com.rapidminer.gui.new_plotter.configuration;

import java.awt.Color;
import java.awt.Font;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jfree.ui.RectangleEdge;

import com.rapidminer.gui.new_plotter.listener.LegendConfigurationListener;
import com.rapidminer.gui.new_plotter.listener.events.LegendConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.LegendConfigurationChangeEvent.LegendConfigurationChangeType;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;


/**
 *
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class LegendConfiguration implements Cloneable {

	public enum LegendPosition {
		TOP(I18N.getGUILabel("plotter.legendposition.TOP.label"),RectangleEdge.TOP), BOTTOM(
				I18N.getGUILabel("plotter.legendposition.BOTTOM.label"),
				RectangleEdge.BOTTOM), LEFT(I18N.getGUILabel("plotter.legendposition.LEFT.label"),
						RectangleEdge.LEFT), RIGHT(I18N.getGUILabel("plotter.legendposition.RIGHT.label"),
								RectangleEdge.RIGHT), NONE(I18N.getGUILabel("plotter.legendposition.NONE.label"), null);

		private String name;
		private RectangleEdge position;

		LegendPosition(String name, RectangleEdge position) {
			this.name = name;
			this.position = position;
		}

		public String getName() {
			return name;
		}

		/**
		 * Caution may be <code>null</code>!
		 *
		 * @return
		 */
		public RectangleEdge getPosition() {
			return position;
		}
	}

	public static final Font DEFAULT_LEGEND_FONT = FontTools.getFont(Font.DIALOG, Font.PLAIN, 12);
	private static final boolean DEFAULT_SHOW_DIMENSION_TYPE = true;
	private static final LegendPosition DEFAULT_LEGEND_POSITION = LegendPosition.RIGHT;
	public static final Color DEFAULT_LEGEND_BACKGROUND_COLOR = Color.white;
	public static final Color DEFAULT_LEGEND_FRAME_COLOR = Color.black;
	private static final boolean DEFAULT_SHOW_LEGEND_FRAME = true;
	public static final Color DEFAULT_LEGEND_FONT_COLOR = Color.black;

	private transient List<WeakReference<LegendConfigurationListener>> listeners = new LinkedList<WeakReference<LegendConfigurationListener>>();
	private Font legendFont = DEFAULT_LEGEND_FONT;
	private LegendPosition legendPosition = DEFAULT_LEGEND_POSITION;

	private LegendConfigurationChangeEvent currentEvent = null;

	private boolean showDimensionType = DEFAULT_SHOW_DIMENSION_TYPE;

	private Color legendBackgroundColor = DEFAULT_LEGEND_BACKGROUND_COLOR;

	private Color legendFrameColor = DEFAULT_LEGEND_FRAME_COLOR;
	private boolean showLegendFrame = DEFAULT_SHOW_LEGEND_FRAME;
	private Color legendFontColor = DEFAULT_LEGEND_FONT_COLOR;

	public boolean isShowDimensionType() {
		return showDimensionType;
	}

	public void setShowDimensionType(boolean showDimensionType) {
		if (showDimensionType != this.showDimensionType) {
			this.showDimensionType = showDimensionType;
			fireShowDimensionTypeChanged();
		}
	}

	private void fireShowDimensionTypeChanged() {
		fireLegendConfigurationChanged(new LegendConfigurationChangeEvent(this, showDimensionType,
				LegendConfigurationChangeType.SHOW_DIMENSION_TYPE));
	}

	public LegendConfigurationChangeEvent getCurrentEvent() {
		return currentEvent;
	}

	public Font getLegendFont() {
		return legendFont;
	}

	public void setLegendFont(Font legendFont) {
		if (legendFont != this.legendFont) {
			this.legendFont = legendFont;
			fireLegendFontChanged();
		}
	}

	/**
	 * @return the legendBackgroundColor
	 */
	public Color getLegendBackgroundColor() {
		return this.legendBackgroundColor;
	}

	/**
	 * @return the legendFrameColor
	 */
	public Color getLegendFrameColor() {
		return this.legendFrameColor;
	}

	/**
	 * @param legendBackgroundColor
	 *            the legendBackgroundColor to set
	 */
	public void setLegendBackgroundColor(Color legendBackgroundColor) {
		this.legendBackgroundColor = legendBackgroundColor;
		fireLegendConfigurationChanged(new LegendConfigurationChangeEvent(this, legendBackgroundColor,
				LegendConfigurationChangeType.BACKGROUND_COLOR));
	}

	/**
	 * @param legendFrameColor
	 *            the legendFrameColor to set
	 */
	public void setLegendFrameColor(Color legendFrameColor) {
		this.legendFrameColor = legendFrameColor;
		fireLegendConfigurationChanged(
				new LegendConfigurationChangeEvent(this, legendFrameColor, LegendConfigurationChangeType.FRAME_COLOR));
	}

	public LegendPosition getLegendPosition() {
		return legendPosition;
	}

	public void setLegendPosition(LegendPosition legendPosition) {
		if (legendPosition != this.legendPosition) {
			this.legendPosition = legendPosition;
			fireLegendPositionChanged();
		}
	}

	@Override
	public LegendConfiguration clone() {
		LegendConfiguration clone = new LegendConfiguration();
		clone.legendFont = this.legendFont;
		clone.legendPosition = this.legendPosition;
		clone.showDimensionType = this.showDimensionType;
		clone.legendBackgroundColor = this.legendBackgroundColor;
		clone.legendFrameColor = this.legendFrameColor;
		clone.showLegendFrame = this.showLegendFrame;
		clone.legendFontColor = this.legendFontColor;
		return clone;
	}

	private void fireLegendFontChanged() {
		fireLegendConfigurationChanged(new LegendConfigurationChangeEvent(this, legendFont));
	}

	private void fireLegendPositionChanged() {
		fireLegendConfigurationChanged(new LegendConfigurationChangeEvent(this, legendPosition));
	}

	private void fireLegendConfigurationChanged(LegendConfigurationChangeEvent e) {
		currentEvent = e;
		Iterator<WeakReference<LegendConfigurationListener>> it = listeners.iterator();
		while (it.hasNext()) {
			LegendConfigurationListener l = it.next().get();
			if (l != null) {
				l.legendConfigurationChanged(e);
			} else {
				it.remove();
			}
		}
		currentEvent = null;
	}

	public void addListener(LegendConfigurationListener l) {
		listeners.add(new WeakReference<LegendConfigurationListener>(l));
	}

	public void removeListener(LegendConfigurationListener l) {
		Iterator<WeakReference<LegendConfigurationListener>> it = listeners.iterator();
		while (it.hasNext()) {
			LegendConfigurationListener listener = it.next().get();
			if (listener == null || listener == l) {
				it.remove();
			}
		}
	}

	public void resetToDefaults() {
		setLegendFont(DEFAULT_LEGEND_FONT);
		setLegendPosition(DEFAULT_LEGEND_POSITION);
		setShowDimensionType(DEFAULT_SHOW_DIMENSION_TYPE);
		setLegendFrameColor(DEFAULT_LEGEND_FRAME_COLOR);
		setLegendBackgroundColor(DEFAULT_LEGEND_BACKGROUND_COLOR);
		setShowLegendFrame(DEFAULT_SHOW_LEGEND_FRAME);
	}

	/**
	 * @return
	 */
	public boolean isShowLegendFrame() {
		return showLegendFrame;
	}

	/**
	 * @param showLegendFrame
	 *            the showLegendFrame to set
	 */
	public void setShowLegendFrame(boolean showLegendFrame) {
		this.showLegendFrame = showLegendFrame;
		fireLegendConfigurationChanged(
				new LegendConfigurationChangeEvent(this, showLegendFrame, LegendConfigurationChangeType.SHOW_LEGEND_FRAME));
	}

	public Color getLegendFontColor() {
		return legendFontColor;
	}

	/**
	 * @param newBackgroundColor
	 */
	public void setLegendFontColor(Color newLegendFontColor) {
		this.legendFontColor = newLegendFontColor;
		fireLegendConfigurationChanged(new LegendConfigurationChangeEvent(this, legendFont));
	}
}
