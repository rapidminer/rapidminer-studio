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
package com.rapidminer.gui.tools;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import com.rapidminer.gui.license.LicenseTools;
import com.rapidminer.license.License;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.plugin.Plugin;


/**
 * The splash screen is displayed during start up of RapidMiner. It displays the logo and the some
 * start information. The product logo should have a size of approximately 270 times 70 pixels.
 *
 * @author Ingo Mierswa, Nils Woehler
 */
public class SplashScreen extends JPanel implements ActionListener {

	private static final int EXTENSION_GAP = 400;
	private static final float EXTENSION_FADE_TIME = 1000;

	private static final int MAX_NUMBER_EXTENSION_ICONS_X = 4;
	private static final float MAX_NUMBER_EXTENSION_ICONS_Y = 3f;
	private static final int MAX_NUMBER_EXTENSION_ICONS = (int) (MAX_NUMBER_EXTENSION_ICONS_X * MAX_NUMBER_EXTENSION_ICONS_Y);

	private static final long serialVersionUID = -1525644776910410809L;

	private static final Paint MAIN_PAINT = Color.BLACK;

	public static Image backgroundImage = null;

	private static final int MARGIN = 10;

	private static final String PROPERTY_FILE = "splash_infos.properties";

	static {
		try {
			if (backgroundImage == null) {
				URL url = Tools.getResource("splashscreen_background.png");
				if (url != null) {
					backgroundImage = ImageIO.read(url);
				}
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.tools.SplashScreen.loading_images_for_splash_screen_error");
		}
	}

	private transient Image productLogo;

	private Properties properties;

	private JFrame splashScreenFrame = new JFrame();

	private String message = "Starting...";

	private boolean infosVisible;

	private Timer animationTimer;
	private List<Runnable> animationRenderers = new LinkedList<>();

	private List<Pair<BufferedImage, Long>> extensionIcons = Collections
			.synchronizedList(new LinkedList<Pair<BufferedImage, Long>>());
	private long lastExtensionAdd = 0;
	private License license;
	private String productEdition;
	private String productName;

	public SplashScreen(String productVersion, Image productLogo) {
		this(productLogo, createDefaultProperties(productVersion));
	}

	public SplashScreen(String productVersion, Image productLogo, URL propertyFile) {
		this(productLogo, createProperties(productVersion, propertyFile));
	}

	public SplashScreen(Image productLogo, Properties properties) {
		this.properties = properties;
		this.productLogo = productLogo;

		splashScreenFrame = new JFrame(properties.getProperty("name"));
		splashScreenFrame.getContentPane().add(this);
		SwingTools.setFrameIcon(splashScreenFrame);

		splashScreenFrame.setUndecorated(true);
		if (backgroundImage != null) {
			splashScreenFrame.setSize(backgroundImage.getWidth(this), backgroundImage.getHeight(this));
		} else {
			splashScreenFrame.setSize(450, 350);
		}
		splashScreenFrame.setLocationRelativeTo(null);

		animationTimer = new Timer(10, this);
		animationTimer.setRepeats(true);
		animationTimer.start();
	}

	private static Properties createDefaultProperties(String productVersion) {
		return createProperties(productVersion, Tools.getResource(PROPERTY_FILE));
	}

	private static Properties createProperties(String productVersion, URL propertyFile) {
		Properties properties = new Properties();
		if (propertyFile != null) {
			try {
				InputStream in = propertyFile.openStream();
				properties.load(in);
				in.close();
			} catch (Exception e) {
				LogService.getRoot().log(Level.SEVERE,
						"com.rapidminer.gui.tools.dialogs.SplashScreen.reading_splash_screen_error", e.getMessage());
			}
		}
		properties.setProperty("version", productVersion);
		return properties;
	}

	public void showSplashScreen() {
		splashScreenFrame.setVisible(true);
	}

	public JFrame getSplashScreenFrame() {
		return splashScreenFrame;
	}

	public void dispose() {
		splashScreenFrame.dispose();
		splashScreenFrame = null;
		animationTimer.stop();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2d = (Graphics2D) g.create();
		drawMain(g2d);
		g2d.setColor(Color.black);
		g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

		// draw extensions
		List<Pair<BufferedImage, Long>> currentExtensionIcons = getSynchronizedExtensionIcons();
		int size = currentExtensionIcons.size();
		if (size > 0) {

			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

			g2d.translate(305, 20);
			g2d.scale(0.5, 0.5);
			long currentTimeMillis = System.currentTimeMillis();

			int numberToShow = 0;
			for (Pair<BufferedImage, Long> pair : currentExtensionIcons) {
				if (currentTimeMillis > pair.getSecond()) {
					numberToShow++;
				}
			}

			// now paint other icons
			int shiftX = 51;
			int shiftY = 51;
			for (int i = 0; i < numberToShow; i++) {
				if (numberToShow > i + MAX_NUMBER_EXTENSION_ICONS) {
					// then we have to fade out again
					Pair<BufferedImage, Long> pair = currentExtensionIcons.get(i + MAX_NUMBER_EXTENSION_ICONS);
					float min = Math.min((currentTimeMillis - pair.getSecond()) / EXTENSION_FADE_TIME, 1f);
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - min));
				} else {
					// fade in
					Pair<BufferedImage, Long> pair = currentExtensionIcons.get(i);
					float min = Math.min((currentTimeMillis - pair.getSecond()) / EXTENSION_FADE_TIME, 1f);
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, min));
				}
				int x = i % MAX_NUMBER_EXTENSION_ICONS_X * shiftX;
				int y = (int) (Math.floor(i / MAX_NUMBER_EXTENSION_ICONS_X) % MAX_NUMBER_EXTENSION_ICONS_Y) * shiftY;
				g2d.drawImage(currentExtensionIcons.get(i).getFirst(), null, x, y);
			}
		}

		g2d.dispose();
	}

	public void drawMain(Graphics2D g) {
		g.setPaint(MAIN_PAINT);
		g.fillRect(0, 0, getWidth(), getHeight());

		if (backgroundImage != null) {
			g.drawImage(backgroundImage, 0, 0, this);
		}

		if (productLogo != null) {
			g.drawImage(productLogo, getWidth() / 2 - productLogo.getWidth(this) / 2, 90, this);
		}

		int y = 255;
		g.setColor(SwingTools.BROWN_FONT_COLOR);
		if (message != null) {
			g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11));
			drawString(g, message, y);
			y += 20;
		}
		if (infosVisible) {
			g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
			StringBuilder builder = new StringBuilder();
			builder.append(productName);
			builder.append(" ");
			builder.append(properties.getProperty("version"));
			drawString(g, builder.toString(), y);
			y += 15;
		}
		if (license != null) {
			g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
			drawString(g, productEdition, y);
			y += 15;
			if (license.getLicenseUser().getName() != null) {
				drawString(g, I18N.getGUILabel("registered_to", license.getLicenseUser().getName()), y);
			}
			y += 15;
		} else {
			y += 30;
		}
		if (infosVisible) {
			g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
			drawString(g, properties.getProperty("copyright"), y);
			y += 15;
			drawString(g, properties.getProperty("more"), y);
			y += 15;
		}
	}

	private void drawString(Graphics2D g, String text, int height) {
		if (text == null) {
			return;
		}
		float xPos = MARGIN;
		float yPos = height;
		g.drawString(text, xPos, yPos);
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	public void setInfosVisible(boolean b) {
		this.infosVisible = b;
	}

	public void addExtension(Plugin plugin) {
		ImageIcon extensionIcon = plugin.getExtensionIcon();
		if (extensionIcon != null) {
			long currentTimeMillis = System.currentTimeMillis();
			if (currentTimeMillis < lastExtensionAdd + EXTENSION_GAP) {
				currentTimeMillis = lastExtensionAdd + EXTENSION_GAP;
			}
			lastExtensionAdd = currentTimeMillis;

			BufferedImage bufferedImage = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = bufferedImage.createGraphics();
			graphics.drawImage(extensionIcon.getImage(), 0, 0, null);

			synchronized (extensionIcons) {
				extensionIcons.add(new Pair<>(bufferedImage, currentTimeMillis));
			}
		}
	}

	private List<Pair<BufferedImage, Long>> getSynchronizedExtensionIcons() {
		synchronized (extensionIcons) {
			return new ArrayList<>(extensionIcons);
		}
	}

	public void addAnimationRenderer(Runnable runable) {
		this.animationRenderers.add(runable);
	}

	@Override
	/**
	 * This method is used for being repainted for
	 * splash animation.
	 */
	public void actionPerformed(ActionEvent e) {
		List<Runnable> copiedAnimationRenderers = new LinkedList<>(animationRenderers);
		for (Runnable runnable : copiedAnimationRenderers) {
			runnable.run();
		}
		repaint();
	}

	/**
	 * @param license
	 *            the currently active license
	 */
	public void setLicense(License license) {
		this.license = license;
		this.productEdition = I18N.getGUILabel("license_edition", LicenseTools.translateProductEdition(license));
		this.productName = LicenseTools.translateProductName(license);
	}
}
