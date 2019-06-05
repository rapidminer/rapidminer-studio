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
package com.rapidminer.gui.look.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.logging.Level;
import javax.swing.JPanel;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.tools.LogService;


/**
 * A component which can be used for multistep progress bars for indicating where the user
 * currently is in a multistep progress.
 *
 * @author Ingo Mierswa
 * @since 8.2
 */
public class MultiStepProgressBar extends JPanel {

	private static final long serialVersionUID = -3058771473146212336L;

	private static final Color BACKGROUND_COLOR = Colors.MULTI_STEP_PROGRESSBAR_NEUTRAL_LIGHT;

	// 16 pixel additional empty space on each side
	private static final int PADDING = 16;

	// assume 8 pixel per letter should be sufficient
	private static final int LETTER_SIZE = 8;

	private static final int HEIGHT = 56;

	private static final int LARGE_BUBBLE_DIAMETER = 28;

	private static final int BUBBLE_DIAMETER = 16;

	private static final int MAX_ANIMATION_STEPS = 24;

	private static final long ANIMATION_THREAD_SLEEP_MS = 18;

	private String[] stepNames;

	private Color color;

	private int currentStep;

	private int longestStep;

	private Dimension preferredSize;

	// fields needed for animation handling
	private int targetStep;
	private boolean currentlyInSubprocess = false;
	private boolean indeterminateSubprocess = false;
	private int animationStep;
	private int currentSubstepToNextStep;
	private int maxSubstepsToNextStep;
	private boolean stopTransition;
	private int indeterminateAnimationPhase;
	private int bubbleAnimationPhase;
	private boolean inTransition;

	/**
	 * Creates a new multi step progress bar for the given step names.  Uses the specified color.
	 *
	 * @param stepNames
	 * 		the step names
	 * @param color
	 * 		the color of the bar
	 */
	public MultiStepProgressBar(String[] stepNames, Color color) {
		this.stepNames = stepNames;
		this.color = color;
		this.currentStep = 0;

		longestStep = -1;
		for (String step : stepNames) {
			if (step.length() > longestStep) {
				longestStep = step.length();
			}
		}

		preferredSize = new Dimension(longestStep * stepNames.length * LETTER_SIZE + 2 * PADDING, HEIGHT);

		setFont(getFont().deriveFont(Font.BOLD, 13));
	}

	/**
	 * Starts a new subprocess which shows an animation towards the next bubble depending on the progress in a subprocess.
	 * Use {@link MultiStepProgressBar#startIndeterminateSubprocess} for indeterminate amount of steps.
	 *
	 * @param maxSubstepsToNextStep
	 * 		the maximal number of steps until the subprocess is ended
	 * @param targetStep
	 * 		the first step index
	 */
	public void startSubprocess(int maxSubstepsToNextStep, int targetStep) {
		this.targetStep = targetStep;
		this.maxSubstepsToNextStep = maxSubstepsToNextStep;
		this.indeterminateSubprocess = false;
		startSubprocessThread();
	}

	/**
	 * Sets the current step in an determinate subprocess. Moves the bar one step closer to the next major step.
	 * Calling this method might artificially take some small amounts of time to ensure that fast
	 * determined tasks with only a few substeps are feeling similar in length than the short transition
	 * phase.
	 *
	 * @param stepInSubprocess
	 * 		the step in the current subprocess
	 */
	public synchronized void setSubprocessStep(int stepInSubprocess) {
		this.currentSubstepToNextStep = stepInSubprocess;
		waitForNextIteration();
	}

	/**
	 * Returns the current step in the current subprocess or -1 if this is currently not in a subprocess.
	 *
	 * @return the current subprocess step
	 */
	public synchronized int getSubprocessStep() {
		return this.currentSubstepToNextStep;
	}

	/**
	 * Starts a new subprocess which shows an animation towards the next bubble depending on the progress in a subprocess.
	 */
	public void startIndeterminateSubprocess(int targetStep) {
		this.targetStep = targetStep;
		this.indeterminateSubprocess = true;
		this.indeterminateAnimationPhase = 0;
		startSubprocessThread();
	}

	/**
	 * Starts a new animation thread.
	 */
	private void startSubprocessThread() {
		this.animationStep = -1;
		this.bubbleAnimationPhase = 0;
		this.currentlyInSubprocess = true;
		new Thread(() -> {
			// stop if subprocess was stopped but always finish the first animation of an indeterminate subprocess...
			while (currentlyInSubprocess) {
				// update the animation counter
				animationStep++;
				if (animationStep >= MAX_ANIMATION_STEPS) {
					animationStep = 0;
				}

				// repaint the bar
				repaint();

				waitForNextIteration();
			}

			// still in first animation phase of indeterminate animation?  Finish it!
			if (indeterminateSubprocess && indeterminateAnimationPhase == 0) {
				for (int a = animationStep; a < MAX_ANIMATION_STEPS; a++) {
					// update the animation counter
					animationStep++;

					// repaint the bar
					repaint();

					waitForNextIteration();
				}
			}

			indeterminateSubprocess = false;
			indeterminateAnimationPhase = 0;
			bubbleAnimationPhase = 0;
			animationStep = -1;

			maxSubstepsToNextStep = -1;
			currentSubstepToNextStep = -1;

			currentStep = targetStep;
			repaint();
		}).start();
	}

	/**
	 * Waits before going on with execution. Handles the {@link InterruptedException}.
	 */
	private void waitForNextIteration() {
		try {
			Thread.sleep(ANIMATION_THREAD_SLEEP_MS);
		} catch (InterruptedException e) {
			currentlyInSubprocess = false;
			Thread.currentThread().interrupt();
			LogService.getRoot().log(Level.WARNING, "Interrupt in progress bar animation thread.", e);
		}
	}

	/**
	 * Returns the step index for the given step name.  If multiple steps have the same name, this method will return
	 * the index of the first occurrence.
	 *
	 * @param stepName
	 * 		the step name
	 * @return the corresponding step index
	 */
	public int getIndexForName(String stepName) {
		return ArrayUtils.indexOf(stepNames, stepName);
	}

	/**
	 * Sets the current step index.  Ends a subprocess animation if there is one or shows a quick transition animation
	 * if there was no subprocess.
	 *
	 * @param index
	 * 		the index of the current step
	 */
	public void setCurrentStep(int index) {
		setCurrentStep(index, true);
	}

	/**
	 * Sets the current step index.  Ends a subprocess animation if there is one or shows a quick transition animation
	 * if there was no subprocess.
	 *
	 * @param stepName
	 * 		the name of the current step
	 */
	public void setCurrentStep(String stepName) {
		setCurrentStep(getIndexForName(stepName), true);
	}

	/**
	 * Sets the current step index.  Ends a subprocess animation if there is one or shows a quick transition animation
	 * if there was no subprocess.
	 *
	 * @param stepName
	 * 		the index of the current step
	 * @param animated
	 * 		indicates if an animation should be shown for the transition
	 */
	public void setCurrentStep(String stepName, boolean animated) {
		setCurrentStep(getIndexForName(stepName), animated);
	}

	/**
	 * Sets the current step index.  Ends a subprocess animation if there is one or shows a quick transition animation
	 * if there was no subprocess.
	 *
	 * @param index
	 * 		the index of the current step
	 * @param animated
	 * 		indicates if an animation should be shown for the transition
	 */
	public void setCurrentStep(int index, boolean animated) {
		if (!animated) {
			currentStep = index;
			repaint();
			return;
		}
		if (this.currentlyInSubprocess) {
			// if a subprocess is running -> kill it and jump to result
			this.currentlyInSubprocess = false;
		} else if (index == currentStep + 1) {
			// if no subprocess is running -> show a quick animation for the transition
			// start new transition thread if new step is one bigger than the current one,
			// just jump if user jumped over steps quickly
			this.targetStep = index;
			startTransitionAnimation();
		} else {
			this.stopTransition = true;
			this.animationStep = -1;
			currentStep = index;
			targetStep = index;
			repaint();
		}
	}

	/**
	 * Starts a quick animation which makes the transition to the next step.
	 */
	private void startTransitionAnimation() {
		stopTransition = false;
		bubbleAnimationPhase = 0;
		new Thread(() -> {
			inTransition = true;
			for (animationStep = 0; animationStep < MAX_ANIMATION_STEPS; animationStep++) {
				if (stopTransition) {
					inTransition = false;
					return;
				}

				// repaint the bar
				repaint();

				waitForNextIteration();
			}

			if (!stopTransition) {
				currentStep = targetStep;
				animationStep = -1;
				bubbleAnimationPhase = 0;
				repaint();
			}
			inTransition = false;
		}).start();
	}

	/**
	 * Returns the preferred size which is based on the number of steps and the number of letters in the steps.
	 *
	 * @return the preferred size
	 */
	@Override
	public Dimension getPreferredSize() {
		return preferredSize;
	}

	/**
	 * Paints the component.
	 *
	 * @param g
	 * 		the g to paint on
	 */
	@Override
	public void paint(Graphics g) {
		super.paint(g);

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHints(ProcessDrawer.HI_QUALITY_HINTS);

		int widthPerStep = longestStep * LETTER_SIZE;

		int counter = 0;
		int xPos = PADDING;
		for (String step : stepNames) {
			// ===========
			// Step Name
			// ===========
			int textY = paintStepLabel(g2, counter, widthPerStep, xPos, step);

			// ===========
			// Bars
			// ===========
			if (counter < stepNames.length - 1) { // not the last step
				if (counter == currentStep) {
					if (currentlyInSubprocess) {
						if (indeterminateSubprocess) {
							// indeterminate sub-process -> alternating gray and orange growing bars
							paintIndeterminatedSubprocessBar(g2, widthPerStep, xPos, textY);
							// update the indeterminate animation phase counter
							if (animationStep >= MAX_ANIMATION_STEPS - 1) {
								indeterminateAnimationPhase++;
							}
						} else {
							// determinate subprocess -> use progress in current subprocess
							paintDeterminateSubprocessBar(g2, widthPerStep, xPos, textY);
						}
					} else {
						// not in subprocess but still animation going on? -> currently in transition animation
						paintTransitionBar(g2, widthPerStep, xPos, textY);
					}
				} else {
					// no subprocess for this bar? Done -> Orange; Not Done -> Gray
					drawSimpleBar(g2, counter, widthPerStep, xPos, textY);
				}
			}

			// no animation running and it is the current bubble -> gradient border to indicate the current bubble
			drawCurrentBubbleIndicator(g2, counter, widthPerStep, xPos, textY);

			// =============
			// Bubbles
			// =============
			if ((counter == currentStep + 1) && ((currentlyInSubprocess && !indeterminateSubprocess) || (currentlyInSubprocess && indeterminateAnimationPhase > 0))) {
				// currently in subprocess towards the next step? -> Animated Bubble
				paintAnimatedBubble(g2, widthPerStep, xPos, textY);
				// update bubble animation phase
				if (animationStep >= MAX_ANIMATION_STEPS - 1) {
					bubbleAnimationPhase++;
				}
			} else {
				// no subprocess towards this bubble? -> Just draw the full bubble then
				paintFullBubble(g2, counter, widthPerStep, xPos, textY);
			}

			// prepare next step
			xPos += widthPerStep;
			counter++;
		}
	}

	/**
	 * Paints the step label and delivers the y-position of the label.
	 *
	 * @param g2
	 * 		the graphics to paint on
	 * @param counter
	 * 		the current step counter
	 * @param widthPerStep
	 * 		the width per step (depending on the used step labels)
	 * @param xPos
	 * 		the current x-position
	 * @return the y-position of the label
	 */
	private int paintStepLabel(Graphics2D g2, int counter, int widthPerStep, int xPos, String step) {
		FontMetrics metrics = g2.getFontMetrics(getFont());
		Rectangle2D stepBounds = metrics.getStringBounds(step, g2);
		int textX = (int) Math.round(xPos + widthPerStep / 2.0d - stepBounds.getWidth() / 2.0d);
		int textY = (int) Math.round(2 + stepBounds.getHeight());

		if ((counter <= currentStep) ||
				((counter == currentStep + 1) &&
						(currentlyInSubprocess || indeterminateSubprocess || inTransition))) {
			// done or currently in subprocess towards this step? -> dark color
			g2.setColor(Colors.MULTI_STEP_PROGRESSBAR_NEUTRAL);
		} else {
			// not done yet? -> lighter color
			g2.setColor(Colors.MULTI_STEP_PROGRESSBAR_NEUTRAL_LIGHT);
		}
		g2.drawString(step, textX, textY);
		return textY;
	}

	/**
	 * Paint the transition bar for an indeterminate subprocess (no sub-steps but long computation in between).
	 *
	 * @param g2
	 * 		the graphics to paint on
	 * @param widthPerStep
	 * 		the width per step (depending on the used step labels)
	 * @param xPos
	 * 		the current x-position
	 * @param textY
	 * 		the y-position of the texts
	 */
	private void paintIndeterminatedSubprocessBar(Graphics2D g2, int widthPerStep, int xPos, int textY) {
		// alternating gray and orange growing bars (good style match to cases where the indeterminate time is short)
		double percentage = animationStep / (double) MAX_ANIMATION_STEPS;
		final boolean inEvenAnimationPhase = indeterminateAnimationPhase % 2 == 0;
		g2.setColor(inEvenAnimationPhase ? color : BACKGROUND_COLOR);

		int barX = (int) Math.round(xPos + widthPerStep / 2.0d);
		int barY = (int) Math.round(textY + 8 + LARGE_BUBBLE_DIAMETER / 2.0d - 3);
		g2.fillRect(barX, barY, (int) Math.round(percentage * widthPerStep), 6);

		g2.setColor(inEvenAnimationPhase ? BACKGROUND_COLOR : color);
		barX = (int) Math.round(xPos + widthPerStep / 2.0d) + (int) Math.round(percentage * widthPerStep);
		barY = (int) Math.round(textY + 8 + LARGE_BUBBLE_DIAMETER / 2.0d - 3);
		g2.fillRect(barX, barY, widthPerStep - (int) Math.round(percentage * widthPerStep), 6);
	}

	/**
	 * Paint the transition bar for a determinate subprocess (multiple sub-steps).
	 *
	 * @param g2
	 * 		the graphics to paint on
	 * @param widthPerStep
	 * 		the width per step (depending on the used step labels)
	 * @param xPos
	 * 		the current x-position
	 * @param textY
	 * 		the y-position of the texts
	 */
	private void paintDeterminateSubprocessBar(Graphics2D g2, int widthPerStep, int xPos, int textY) {
		double percentage = currentSubstepToNextStep / (double) maxSubstepsToNextStep;
		g2.setColor(color);
		int barX = (int) Math.round(xPos + widthPerStep / 2.0d);
		int barY = (int) Math.round(textY + 8 + LARGE_BUBBLE_DIAMETER / 2.0d - 3);
		g2.fillRect(barX, barY, (int) Math.round(percentage * widthPerStep), 6);

		g2.setColor(BACKGROUND_COLOR);
		barX = (int) Math.round(xPos + widthPerStep / 2.0d) + (int) Math.round(percentage * widthPerStep);
		barY = (int) Math.round(textY + 8 + LARGE_BUBBLE_DIAMETER / 2.0d - 3);
		g2.fillRect(barX, barY, widthPerStep - (int) Math.round(percentage * widthPerStep), 6);
	}

	/**
	 * Paint the transition animation bar (quick transition shown between steps without substeps or long computations).
	 *
	 * @param g2
	 * 		the graphics to paint on
	 * @param widthPerStep
	 * 		the width per step (depending on the used step labels)
	 * @param xPos
	 * 		the current x-position
	 * @param textY
	 * 		the y-position of the texts
	 */
	private void paintTransitionBar(Graphics2D g2, int widthPerStep, int xPos, int textY) {
		if (animationStep >= 0) {
			double percentage = animationStep / (double) MAX_ANIMATION_STEPS;
			g2.setColor(color);
			int barX = (int) Math.round(xPos + widthPerStep / 2.0d);
			int barY = (int) Math.round(textY + 8 + LARGE_BUBBLE_DIAMETER / 2.0d - 3);
			g2.fillRect(barX, barY, (int) Math.round(percentage * widthPerStep), 6);

			g2.setColor(BACKGROUND_COLOR);
			barX = (int) Math.round(xPos + widthPerStep / 2.0d) + (int) Math.round(percentage * widthPerStep);
			barY = (int) Math.round(textY + 8 + LARGE_BUBBLE_DIAMETER / 2.0d - 3);
			g2.fillRect(barX, barY, widthPerStep - (int) Math.round(percentage * widthPerStep), 6);
		} else {
			// not (yet) in transition -> just draw the gray bar
			g2.setColor(BACKGROUND_COLOR);
			int barX = (int) Math.round(xPos + widthPerStep / 2.0d);
			int barY = (int) Math.round(textY + 8 + LARGE_BUBBLE_DIAMETER / 2.0d - 3);
			g2.fillRect(barX, barY, widthPerStep, 6);
		}
	}

	/**
	 * Draws the simple bar either in gray background (not there yet) or in highlight color if this step
	 * has been done already.
	 *
	 * @param g2
	 * 		the graphics to paint on
	 * @param counter
	 * 		the current step counter
	 * @param widthPerStep
	 * 		the width per step (depending on the used step labels)
	 * @param xPos
	 * 		the current x-position
	 * @param textY
	 * 		the y-position of the texts
	 */
	private void drawSimpleBar(Graphics2D g2, int counter, int widthPerStep, int xPos, int textY) {
		if (counter < currentStep) { // already done? highlight color
			g2.setColor(color);
		} else { // post the current step --> gray
			g2.setColor(BACKGROUND_COLOR);
		}
		int barX = (int) Math.round(xPos + widthPerStep / 2.0d);
		int barY = (int) Math.round(textY + 8 + LARGE_BUBBLE_DIAMETER / 2.0d - 3);
		g2.fillRect(barX, barY, widthPerStep, 6);
	}

	/**
	 * Paint the circular gradient indicator around the bubble of the current step.
	 *
	 * @param g2
	 * 		the graphics to paint on
	 * @param counter
	 * 		the current step counter
	 * @param widthPerStep
	 * 		the width per step (depending on the used step labels)
	 * @param xPos
	 * 		the current x-position
	 * @param textY
	 * 		the y-position of the texts
	 */
	private void drawCurrentBubbleIndicator(Graphics2D g2, int counter, int widthPerStep, int xPos, int textY) {
		if ((counter != currentStep) || (animationStep >= 0)) {
			return;
		}
		int bubbleX = (int) Math.round(xPos + widthPerStep / 2.0d - LARGE_BUBBLE_DIAMETER / 2.0d);
		int bubbleY = textY + 8;

		Point2D.Float point = new Point2D.Float(bubbleX + LARGE_BUBBLE_DIAMETER / 2.0f, bubbleY + LARGE_BUBBLE_DIAMETER / 2.0f);
		float[] fractions = {0.0f, 1.0f};
		Color color1 = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
		Color color2 = new Color(color.getRed(), color.getGreen(), color.getBlue(), 90);
		Color[] colors = {color1, color2};
		RadialGradientPaint paint = new RadialGradientPaint(point.x, point.y, LARGE_BUBBLE_DIAMETER / 2.0f, fractions, colors);
		g2.setPaint(paint);
		g2.fillOval(bubbleX, bubbleY, LARGE_BUBBLE_DIAMETER, LARGE_BUBBLE_DIAMETER);
	}

	/**
	 * Paint the animated bubble indicating that the progress is currently made towards this bubble.
	 *
	 * @param g2
	 * 		the graphics to paint on
	 * @param widthPerStep
	 * 		the width per step (depending on the used step labels)
	 * @param xPos
	 * 		the current x-position
	 * @param textY
	 * 		the y-position of the texts
	 */
	private void paintAnimatedBubble(Graphics2D g2, int widthPerStep, int xPos, int textY) {
		// fill gray bubble with orange and then with gray again
		final boolean inOddAnimationPhase = bubbleAnimationPhase % 2 == 1;

		g2.setColor(inOddAnimationPhase ? BACKGROUND_COLOR : color);
		int bubbleX = (int) Math.round(xPos + widthPerStep / 2.0d - BUBBLE_DIAMETER / 2.0d);
		int bubbleY = (int) Math.round(textY + 8 + (LARGE_BUBBLE_DIAMETER - BUBBLE_DIAMETER) / 2.0d);
		g2.fillOval(bubbleX, bubbleY, BUBBLE_DIAMETER, BUBBLE_DIAMETER);

		// then orange partly filled bubble
		double percentage = animationStep / (double) MAX_ANIMATION_STEPS;
		double extent = 360d * percentage;

		g2.setColor(inOddAnimationPhase ? color : BACKGROUND_COLOR);
		Arc2D arc = new Arc2D.Double(bubbleX, bubbleY, BUBBLE_DIAMETER, BUBBLE_DIAMETER, 90, -extent, Arc2D.PIE);
		g2.fill(arc);
	}

	/**
	 * Draws the full bubble which is done when there is no subprocess indicator to this bubble.
	 * The bubble is either drawn in the background color if not reached yet of in the highlight
	 * color if it has been reached already.
	 *
	 * @param g2
	 * 		the graphics to paint on
	 * @param counter
	 * 		the current step counter
	 * @param widthPerStep
	 * 		the width per step (depending on the used step labels)
	 * @param xPos
	 * 		the current x-position
	 * @param textY
	 * 		the y-position of the texts
	 */
	private void paintFullBubble(Graphics2D g2, int counter, int widthPerStep, int xPos, int textY) {
		if (counter <= currentStep) {
			g2.setColor(color);
		} else {
			g2.setColor(BACKGROUND_COLOR);
		}
		int bubbleX = (int) Math.round(xPos + widthPerStep / 2.0d - BUBBLE_DIAMETER / 2.0d);
		int bubbleY = (int) Math.round(textY + 8 + (LARGE_BUBBLE_DIAMETER - BUBBLE_DIAMETER) / 2.0d);
		g2.fillOval(bubbleX, bubbleY, BUBBLE_DIAMETER, BUBBLE_DIAMETER);
	}
}
