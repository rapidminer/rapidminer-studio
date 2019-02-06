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
package com.rapidminer.tools;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.imageio.ImageIO;


/**
 * Creates dynamic icons based on the hostname and the url path elements. Before you can use any
 * custom dynamic icons, register them by calling {@link #registerDynamicIcon(String, DynamicIcon)}. <br/>
 * Use {@link #IDENTIFIER_PROGRESS} or {@link #IDENTIFIER_QUOTA} for existing dynamic icon
 * implementations.
 * <p>
 * Usage: <br/>
 * If the identifier is <code>progress</code>, the HTML icon tag would look like this:
 * </p>
 * <p>
 * <code>&lt;img src=\"dynicon://progress/100/14/50&gt;</code>
 * </p>
 * <p>
 * where the first number after the identifier is the width of the icon in px, the second number is
 * the height in px and the third number is the percentage (0-100).
 * </p>
 *
 * @author Thilo Kamradt, Simon Fischer, Marco Boeck
 *
 */
public class DynamicIconUrlStreamHandler extends URLStreamHandler {

	/** the identifier of the progress bar icon (fading from green to very dark green) */
	public static final String IDENTIFIER_PROGRESS = "progress";

	/** the identifier of the quota bar icon (first 50% green, last 50% fading from green to red) */
	public static final String IDENTIFIER_QUOTA = "quota";

	/**
	 * the identifier of the quota bar icon (always one color only: green (0-70%), orange (70-90%),
	 * red (90-100%)
	 */
	public static final String IDENTIFIER_QUOTA_UNI = "quota_uni";

	/** mapping between identifiers and the dynicon implementations */
	private static final Map<String, DynamicIcon> mapping = new HashMap<>();

	static {
		// register default implementation o
		mapping.put(IDENTIFIER_PROGRESS, new DynamicIcon() {

			@Override
			public void drawIcon(Graphics2D g2, int width, int height, int percentage) {
				g2.setColor(Color.WHITE);
				g2.setPaint(new GradientPaint(0, 0, Color.WHITE.darker(), 0, (float) (height * 0.5), Color.WHITE, true));
				g2.fillRect(0, 0, width, height);
				g2.setColor(Color.GREEN);
				g2.setPaint(new GradientPaint(0, 0, Color.GREEN.darker(), percentage, (float) (height * 0.5), Color.GREEN
						.darker().darker(), false));
				g2.fillRect(0, 0, (int) (percentage * 200d / 100d), height);
				g2.setColor(Color.BLACK);
				g2.drawRect(0, 0, width - 1, height - 1);
				g2.dispose();
			}
		});
		mapping.put(IDENTIFIER_QUOTA, new DynamicIcon() {

			@Override
			public void drawIcon(Graphics2D g2, int width, int height, int percentage) {
				// prepare background
				g2.setColor(Color.WHITE);
				g2.fillRect(0, 0, width, height);

				// 0-50 % is the same color, only the last 50% fade into red
				Color startColor = new Color(0f, .5f, 0, 0.7f);
				g2.setColor(startColor);
				g2.setPaint(new LinearGradientPaint(width / 2, 0f, width, 0f, new float[] { 0f, 0.5f, 1f }, new Color[] {
						new Color(0f, .5f, 0, 0.7f), new Color(1f, 1f, 0f, 0.7f), new Color(1f, 0, 0, 0.7f) }));
				g2.fillRect(0, 0, (int) (percentage / 100d * width), height);

				// draw border
				g2.setColor(Color.GRAY);
				g2.drawRect(0, 0, width - 1, height - 1);
				g2.dispose();
			}
		});
		mapping.put(IDENTIFIER_QUOTA_UNI, new DynamicIcon() {

			@Override
			public void drawIcon(Graphics2D g2, int width, int height, int percentage) {
				// prepare background
				g2.setColor(Color.WHITE);
				g2.fillRect(0, 0, width, height);

				// 0-70 % is the green color; 70-90% is gradient orange; 90-100% is red
				float red = 0.0f;
				float green = 0.0f;
				float blue = 0.0f;
				float percentageFloat = percentage / 100f;
				if (percentage < 70) {
					green = 0.75f;
				} else if (percentage >= 70 && percentage < 90) {
					red = 1.0f;
					green = 1.0f - 2 * (percentageFloat - 0.5f);
				} else if (percentage >= 90) {
					red = 1.0f;
				}
				Color fillColor = new Color(red, green, blue);
				g2.setColor(fillColor);
				g2.fillRect(0, 0, (int) (percentage / 100d * width), height);

				// draw border
				g2.setColor(Color.GRAY);
				g2.drawRect(0, 0, width - 1, height - 1);
				g2.dispose();
			}
		});
	}

	/**
	 * Register the specified {@link DynamicIcon} under the given identifier. For instructions on
	 * how to use the icon in an HTML document, see {@link DynamicIcon}.
	 *
	 * @param identifier
	 *            the identifier of the dynamic icon
	 * @param dynIcon
	 *            the implementation of the icon
	 */
	public static void registerDynamicIcon(String identifier, DynamicIcon dynIcon) {
		if (identifier == null || identifier.isEmpty()) {
			throw new IllegalArgumentException("identifier must not be null or empty!");
		}
		if (dynIcon == null) {
			throw new IllegalArgumentException("dynIcon must not be null!");
		}
		if (IDENTIFIER_PROGRESS.equals(identifier) || IDENTIFIER_QUOTA.equals(identifier)
				|| IDENTIFIER_QUOTA_UNI.equals(identifier)) {
			throw new IllegalArgumentException("cannot override default implementations");
		}
		mapping.put(identifier, dynIcon);
	}

	@Override
	protected URLConnection openConnection(final URL u) throws IOException {
		return new URLConnection(u) {

			@Override
			public InputStream getInputStream() throws IOException {
				String[] parameter = u.getFile().substring(1).split("/");
				int width = Integer.parseInt(parameter[0]);
				int height = Integer.parseInt(parameter[1]);
				int percentage = Integer.parseInt(parameter[2]);

				// make sure parameters are valid, otherwise throw
				if (width <= 0) {
					throw new IllegalArgumentException("dynicon width must not be <= 0");
				}
				if (height <= 0) {
					throw new IllegalArgumentException("dynicon height must not be <= 0");
				}
				if (percentage < 0 || percentage > 100) {
					throw new IllegalArgumentException("dynicon percentage must not be < 0 or > 100");
				}
				BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
				Graphics2D g2 = (Graphics2D) img.getGraphics();
				String type = u.getHost();

				// check if we have a registered implementation and use it
				boolean found = false;
				for (String key : mapping.keySet()) {
					if (key.equals(type)) {
						mapping.get(key).drawIcon(g2, width, height, percentage);
						found = true;
						break;
					}
				}

				// unknown dynicon type
				if (!found) {
					throw new IOException("Unknown dynamic icon type: " + type);
				}

				// create image from graphics and return it
				try {
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					ImageIO.write(img, "png", buffer);
					buffer.close();
					return new ByteArrayInputStream(buffer.toByteArray());
				} catch (IOException e) {
					LogService.getRoot()
							.log(Level.WARNING, "com.rapidminer.tools.DynamicIconUrlStreamHandler.icon_error", e);
					e.printStackTrace();
					return null;
				}
			}

			@Override
			public void connect() throws IOException {
				// no-op
			}
		};
	}
}
