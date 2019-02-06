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
package com.rapidminer.gui.plotter.charts;

import com.rapidminer.gui.tools.SwingTools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

import org.jfree.chart.HashUtilities;
import org.jfree.chart.renderer.xy.XYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.ui.RectangleEdge;


/**
 * The painter for the bar charts.
 * 
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class RapidXYBarPainter implements XYBarPainter {

	/** The division point between the first and second gradient regions. */
	private final double g1;

	/** The division point between the second and third gradient regions. */
	private final double g2;

	/** The division point between the third and fourth gradient regions. */
	private final double g3;

	/**
	 * Creates a new instance.
	 */
	public RapidXYBarPainter() {
		// this(0.10, 0.20, 0.80);
		this(0.0, 0.3, 0.7);
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param g1
	 * @param g2
	 * @param g3
	 */
	public RapidXYBarPainter(double g1, double g2, double g3) {
		this.g1 = g1;
		this.g2 = g2;
		this.g3 = g3;
	}

	/**
	 * Tests this instance for equality with an arbitrary object.
	 * 
	 * @param obj
	 *            the obj (<code>null</code> permitted).
	 * 
	 * @return A boolean.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RapidXYBarPainter)) {
			return false;
		}
		RapidXYBarPainter that = (RapidXYBarPainter) obj;
		if (this.g1 != that.g1) {
			return false;
		}
		if (this.g2 != that.g2) {
			return false;
		}
		if (this.g3 != that.g3) {
			return false;
		}
		return true;
	}

	/**
	 * Returns a hash code for this instance.
	 * 
	 * @return A hash code.
	 */
	@Override
	public int hashCode() {
		int hash = 37;
		hash = HashUtilities.hashCode(hash, this.g1);
		hash = HashUtilities.hashCode(hash, this.g2);
		hash = HashUtilities.hashCode(hash, this.g3);
		return hash;
	}

	@Override
	public void paintBar(Graphics2D g2, XYBarRenderer renderer, int row, int column, RectangularShape bar, RectangleEdge base) {
		Paint itemPaint = renderer.getItemPaint(row, column);

		Color c0 = null;

		if (itemPaint instanceof Color) {
			c0 = (Color) itemPaint;
		} else {
			c0 = SwingTools.DARK_BLUE;
		}

		// as a special case, if the bar color has alpha == 0, we draw
		// nothing.
		if (c0.getAlpha() == 0) {
			return;
		}

		g2.setPaint(c0);
		g2.fill(new Rectangle2D.Double(bar.getMinX(), bar.getMinY(), bar.getWidth(), bar.getHeight()));

		// draw the outline...
		if (renderer.isDrawBarOutline()) {
			Stroke stroke = renderer.getItemOutlineStroke(row, column);
			Paint paint = renderer.getItemOutlinePaint(row, column);
			if (stroke != null && paint != null) {
				g2.setStroke(stroke);
				g2.setPaint(paint);
				g2.draw(bar);
			}
		}
	}

	@Override
	public void paintBarShadow(Graphics2D arg0, XYBarRenderer arg1, int arg2, int arg3, RectangularShape arg4,
			RectangleEdge arg5, boolean arg6) {
		// do nothing
	}
}
