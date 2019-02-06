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
package com.rapidminer.gui.new_plotter.engine.jfreechart.legend;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.jfree.chart.block.Arrangement;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BlockResult;
import org.jfree.chart.block.EntityBlockParams;
import org.jfree.chart.block.EntityBlockResult;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.StandardEntityCollection;


/**
 * A {@link BlockContainer} with a colored background. Additionally brutally enforces a vertically
 * centered alignment of its contents, and thus probably only works as long as the contents is
 * arranged in exactly one horizontal line.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class ColoredBlockContainer extends BlockContainer {

	private static final long serialVersionUID = 1L;
	private Paint fillPaint;

	public ColoredBlockContainer(Paint fillPaint) {
		super();
		this.fillPaint = fillPaint;
	}

	public ColoredBlockContainer(Paint fillPaint, Arrangement arrangement) {
		super(arrangement);
		this.fillPaint = fillPaint;
	}

	public Paint getFillPaint() {
		return fillPaint;
	}

	public void setFillPaint(Paint fillPaint) {
		this.fillPaint = fillPaint;
	}

	@Override
	public void draw(Graphics2D g2, Rectangle2D area) {
		area = drawFill(g2, area);
		super.draw(g2, area);
	}

	/**
	 * Draws a colored background. Returns the area wich has been filled.
	 */
	private Rectangle2D drawFill(Graphics2D g2, Rectangle2D area) {
		Rectangle2D filledArea = (Rectangle2D) area.clone();
		filledArea = trimMargin(filledArea);
		filledArea = trimBorder(filledArea);
		area = trimPadding(area);
		g2.setPaint(this.fillPaint);
		g2.fill(filledArea);
		drawBorder(g2, filledArea);
		return filledArea;
	}

	/**
	 * Disclaimer: this is a "works for me" implementation, and probably only works as long as the
	 * items are arranged horizontally in exactly one line, since it brutally enforces the items to
	 * be aligned vertically centered.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object draw(Graphics2D g2, Rectangle2D area, Object params) {
		area = drawFill(g2, area);

		// check if we need to collect chart entities from the container
		EntityBlockParams ebp = null;
		StandardEntityCollection sec = null;
		if (params instanceof EntityBlockParams) {
			ebp = (EntityBlockParams) params;
			if (ebp.getGenerateEntities()) {
				sec = new StandardEntityCollection();
			}
		}
		Rectangle2D contentArea = (Rectangle2D) area.clone();
		contentArea = trimMargin(contentArea);
		drawBorder(g2, contentArea);
		contentArea = trimBorder(contentArea);
		contentArea = trimPadding(contentArea);
		for (Block block : (List<Block>) getBlocks()) {
			Rectangle2D bounds = block.getBounds();

			// enforce vertically centered alignment
			double y = area.getY() + (area.getHeight() - bounds.getHeight()) / 2.0;

			Rectangle2D drawArea = new Rectangle2D.Double(bounds.getX() + area.getX(), y, bounds.getWidth(),
					bounds.getHeight());
			Object r = block.draw(g2, drawArea, params);
			if (sec != null) {
				if (r instanceof EntityBlockResult) {
					EntityBlockResult ebr = (EntityBlockResult) r;
					EntityCollection ec = ebr.getEntityCollection();
					sec.addAll(ec);
				}
			}
		}
		BlockResult result = null;
		if (sec != null) {
			result = new BlockResult();
			result.setEntityCollection(sec);
		}
		return result;
	}
}
