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
package com.rapidminer.gui.flow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 *
 * @author Simon Fischer
 */
public class OverviewPanel extends JPanel implements Dockable {

	private static final long serialVersionUID = 1L;

	private static final Color FILL_COLOR = new Color(140, 140, 200, 30);

	private static final Color DRAW_COLOR = new Color(140, 140, 200);

	private final ProcessRendererView processRenderer;

	private Point dragStartMousePos;
	private Rectangle dragStartRect;
	private double scale = 1d;

	public OverviewPanel(ProcessRendererView processRenderer) {
		this.processRenderer = processRenderer;
		addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				dragStartMousePos = e.getPoint();
				dragStartRect = OverviewPanel.this.processRenderer.getVisibleRect();
			}
		});
		addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseDragged(MouseEvent e) {
				double diffX = (e.getX() - dragStartMousePos.getX()) / scale;
				double diffY = (e.getY() - dragStartMousePos.getY()) / scale;
				OverviewPanel.this.processRenderer.scrollRectToVisible(new Rectangle((int) (dragStartRect.getX() + diffX),
						(int) (dragStartRect.getY() + diffY), (int) dragStartRect.getWidth(), (int) dragStartRect
								.getHeight()));
			}

			@Override
			public void mouseMoved(MouseEvent e) {}
		});
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		double scaleX = (double) getWidth() / (double) processRenderer.getWidth();
		double scaleY = (double) getHeight() / (double) processRenderer.getHeight();
		scale = Math.min(scaleX, scaleY);
		double scaledW = processRenderer.getWidth() * scale;
		double scaledH = processRenderer.getHeight() * scale;

		Graphics2D g = (Graphics2D) graphics.create();
		g.translate((getWidth() - scaledW) / 2d, (getHeight() - scaledH) / 2d);
		g.scale(scale, scale);

		g.setRenderingHints(ProcessDrawer.LOW_QUALITY_HINTS);
		processRenderer.getOverviewPanelDrawer().draw(g, true);

		g.setStroke(new BasicStroke((int) (1d / scale)));

		Rectangle visibleRect = processRenderer.getVisibleRect();
		Rectangle drawRect = new Rectangle((int) visibleRect.getX(), (int) visibleRect.getY(),
				(int) visibleRect.getWidth() - 1, (int) visibleRect.getHeight() - 1);

		g.setColor(FILL_COLOR);
		g.fill(drawRect);

		g.setColor(DRAW_COLOR);
		g.draw(drawRect);

		g.dispose();
	}

	public static final String OVERVIEW_DOCK_KEY = "overview";
	private final DockKey DOCK_KEY = new ResourceDockKey(OVERVIEW_DOCK_KEY);
	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

}
