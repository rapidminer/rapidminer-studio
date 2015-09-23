/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tools.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.rapidminer.gui.license.LicenseTools;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.LinkButton;
import com.rapidminer.license.License;
import com.rapidminer.license.LicenseConstants;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.PlatformUtilities;
import com.rapidminer.tools.RMUrlHandler;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.plugin.Plugin;


/**
 * This dialog displays some informations about the product. The product logo should have a size of
 * approximately 270 times 70 pixels.
 *
 * @author Ingo Mierswa
 */
public class AboutBox extends JDialog {

	private static final long serialVersionUID = -3889559376722324215L;

	private static final int MAX_SHOWN_LINK_LENGTH = 50;

	private static final String PROPERTY_FILE = "about_infos.properties";

	private static final String RAPID_MINER_LOGO_NAME = "rapidminer_logo.png";
	public static final Image RAPID_MINER_LOGO;
	public static Image backgroundImage = null;
	static {
		URL url = Tools.getResource(RAPID_MINER_LOGO_NAME);
		Image rmLogo = null;
		if (url != null) {
			try {
				rmLogo = ImageIO.read(url);
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.dialogs.AboutBox.loading_logo_error");
			}
		}
		RAPID_MINER_LOGO = rmLogo;
		url = Tools.getResource("about_background.png");
		if (url != null) {
			try {
				backgroundImage = ImageIO.read(url);
			} catch (IOException e) {
				LogService.getRoot()
				.log(Level.WARNING, "com.rapidminer.gui.tools.dialogs.AboutBox.loading_background_error");
			}
		}
	}

	private final ContentPanel contentPanel;

	private static class ContentPanel extends JPanel {

		private static final long serialVersionUID = -1763842074674706654L;

		private static final int LOGO_INSET_Y = 20;
		private static final int LOGO_INSET_X = 25;

		private static final Paint MAIN_PAINT = Color.LIGHT_GRAY;

		private static final int MARGIN = 10;

		private final Properties properties;

		private transient Image productLogo;

		public ContentPanel(Properties properties, Image productLogo) {
			this.properties = properties;
			this.productLogo = productLogo;

			int width = 450;
			int height = 350;
			if (backgroundImage != null) {
				width = backgroundImage.getWidth(this);
				height = backgroundImage.getHeight(this);
			}
			setPreferredSize(new Dimension(width, height));
			setMinimumSize(new Dimension(width, height));
			setMaximumSize(new Dimension(width, height));
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			drawMain((Graphics2D) g);
			g.setColor(Color.black);
			g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
		}

		public void drawMain(Graphics2D g) {
			g.setPaint(MAIN_PAINT);
			g.fillRect(0, 0, getWidth(), getHeight());

			if (backgroundImage != null) {
				g.drawImage(backgroundImage, 0, 0, this);
			}

			g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 26));
			g.setColor(SwingTools.RAPIDMINER_GRAY);
			if (productLogo != null) {
				if ("true".equals(properties.getProperty("textNextToLogo"))) {
					g.drawImage(productLogo, LOGO_INSET_X, LOGO_INSET_Y, this);
					g.drawString(properties.getProperty("name"), LOGO_INSET_X + productLogo.getWidth(null) + 10,
							LOGO_INSET_Y + 34);
				} else {
					g.drawImage(productLogo, LOGO_INSET_X, LOGO_INSET_Y, this);
				}
			}

			int y = 255;
			g.setColor(SwingTools.BROWN_FONT_COLOR);
			g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11));
			StringBuilder builder = new StringBuilder();
			builder.append(properties.getProperty("name"));
			builder.append(" ");
			builder.append(properties.getProperty("version"));
			String revision = properties.getProperty("revision");
			if (revision != null) {
				builder.append(" (rev: ");
				builder.append(revision.substring(0, 6));
				String platform = properties.getProperty("platform");
				if (platform != null) {
					builder.append(", platform: ");
					builder.append(platform);
				}
				builder.append(")");
			}

			drawString(g, builder.toString(), y);
			y += 20;

			g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
			y = drawStringAndAdvance(g, properties.getProperty("edition"), y);
			if (properties.getProperty("registered_to") != null) {
				y = drawStringAndAdvance(g, I18N.getGUILabel("registered_to", properties.getProperty("registered_to")), y);
			}
			y = drawStringAndAdvance(g, properties.getProperty("copyright"), y);
			y = drawStringAndAdvance(g, properties.getProperty("licensor"), y);
			y = drawStringAndAdvance(g, properties.getProperty("license"), y);
			y = drawStringAndAdvance(g, properties.getProperty("warranty"), y);
			y = drawStringAndAdvance(g, properties.getProperty("more"), y);
		}

		private int drawStringAndAdvance(Graphics2D g, String string, int y) {
			if (string == null) {
				return y;
			} else {
				List<String> lines = new LinkedList<>();
				String[] words = string.split("\\s+");
				String current = "";
				for (String word : words) {
					if (current.length() + word.length() < 80) {
						current += word + " ";
					} else {
						lines.add(current);
						current = word + " ";
					}
				}
				if (!current.isEmpty()) {
					lines.add(current);
				}
				for (String line : lines) {
					drawString(g, line, y);
					y += 15;
				}
				return y;
			}
		}

		private void drawString(Graphics2D g, String text, int y) {
			if (text == null) {
				return;
			}
			float xPos = MARGIN;
			float yPos = y;
			g.drawString(text, xPos, yPos);
		}
	}

	public AboutBox(Frame owner, String productName, String productVersion, String licensor, String url, String text,
			boolean renderTextNextToLogo, Image productLogo) {
		this(owner, createProperties(productName, productVersion, licensor, url, text, renderTextNextToLogo), productLogo);
	}

	public AboutBox(Frame owner, String productVersion, License license, Image productLogo) {
		this(owner, createProperties(productVersion, license), productLogo);
	}

	public AboutBox(Frame owner, Properties properties, Image productLogo) {
		super(owner, "About", true);
		setResizable(false);

		setLayout(new BorderLayout());

		String name = properties.getProperty("name");
		if (name != null) {
			setTitle("About " + name);
		}
		contentPanel = new ContentPanel(properties, productLogo);
		add(contentPanel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final String url = properties.getProperty("url");
		if (url != null) {
			String shownURL = url;
			if (shownURL.length() > MAX_SHOWN_LINK_LENGTH) {
				shownURL = shownURL.substring(0, MAX_SHOWN_LINK_LENGTH) + "...";
			}
			buttonPanel.add(new LinkButton(new ResourceAction("link_action", url, shownURL) {

				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						RMUrlHandler.browse(new URI(url));
					} catch (Exception e1) {
						SwingTools.showSimpleErrorMessage("cannot_open_browser", e1);
					}
				}
			}));
		}

		ResourceAction closeAction = new ResourceAction("close") {

			private static final long serialVersionUID = 1407089394491740308L;

			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};
		JButton closeButton = new JButton(closeAction);
		buttonPanel.add(closeButton);

		add(buttonPanel, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(closeButton);
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", closeAction);

		pack();
		setLocationRelativeTo(owner);
	}

	public static Properties createProperties(InputStream inputStream, String productVersion) {
		Properties properties = new Properties();
		if (inputStream != null) {
			try {
				properties.load(inputStream);
			} catch (Exception e) {
				LogService.getRoot().log(Level.SEVERE,
						"com.rapidminer.gui.tools.dialogs.AboutBox.reading_splash_screen_error", e.getMessage());
			}
		}
		properties.setProperty("version", productVersion);
		Plugin.initAboutTexts(properties);
		return properties;
	}

	private static Properties createProperties(String productVersion, License license) {
		Properties properties = new Properties();
		try {
			URL propUrl = Tools.getResource(PROPERTY_FILE);
			if (propUrl != null) {
				InputStream in = propUrl.openStream();
				properties.load(in);
				in.close();
			}
		} catch (Exception e) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.tools.dialogs.AboutBox.reading_splash_screen_error",
					e.getMessage());
		}
		properties.setProperty("name", LicenseTools.translateProductName(license));
		properties.setProperty("version", productVersion);
		if (LicenseConstants.DEFAULT_PRODUCT_ID.equals(license.getProductId())
				&& PlatformUtilities.getReleaseRevision() != null) {
			properties.setProperty("revision", PlatformUtilities.getReleaseRevision());
			properties.setProperty("platform", PlatformUtilities.getReleasePlatform().toString());
		}
		properties
		.setProperty("edition", I18N.getGUILabel("license_edition", LicenseTools.translateProductEdition(license)));
		if (license.getLicenseUser().getName() != null) {
			properties.setProperty("registered_to", license.getLicenseUser().getName());
		}
		Plugin.initAboutTexts(properties);
		return properties;
	}

	private static Properties createProperties(String productName, String productVersion, String licensor, String url,
			String text, boolean renderTextNextToLogo) {
		Properties properties = new Properties();
		properties.setProperty("name", productName);
		properties.setProperty("version", productVersion);
		properties.setProperty("licensor", licensor);
		properties.setProperty("license", "URL: " + url);
		properties.setProperty("more", text);
		properties.setProperty("textNextToLogo", "" + renderTextNextToLogo);
		properties.setProperty("url", url);
		return properties;
	}

}
