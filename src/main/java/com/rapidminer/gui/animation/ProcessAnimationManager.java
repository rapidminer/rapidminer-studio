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
package com.rapidminer.gui.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorProgress;


/**
 * Manager for the progress animations for operators that are shown in the process view.
 *
 * @author Gisa Schaefer
 * @since 7.1.0
 */
public enum ProcessAnimationManager {

	INSTANCE;

	/** the map from operator name to animation */
	private final Map<String, Animation> animations = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Retrieves the {@link Animation} associated with the operator if it exists.
	 *
	 * @param operator
	 *            the operator for which to get the animation
	 * @return the animation for the operator or {@code null}
	 */
	public Animation getAnimationForOperator(Operator operator) {
		return animations.get(operator.getName());
	}

	/**
	 * Creates an {@link Animation} that displays the progress of the operator and associates it
	 * with the operator.
	 *
	 * @param operator
	 *            the operator for which a animation is added
	 */
	void addAnimationForOperator(Operator operator) {
		Animation operatorAnimation = createAnimationForOperator(operator);
		animations.put(operator.getName(), operatorAnimation);
	}

	/**
	 * Removes the {@link Animation} registered for the operator if it exists.
	 *
	 * @param operator
	 *            the operator for which to remove the animation
	 */
	void removeAnimationForOperator(Operator operator) {
		animations.remove(operator.getName());
	}

	/**
	 * Checks if one of the animations requires a repaint.
	 *
	 * @return whether at least one of the animations need a repaint
	 */
	boolean isRepaintRequired() {
		List<Animation> animationList;
		synchronized (animations) {
			animationList = new ArrayList<>(animations.values());
		}
		for (Animation animation : animationList) {
			if (animation.isRedrawRequired()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates an {@link Animation} that shows the {@link OperatorProgress} of the operator.
	 */
	private Animation createAnimationForOperator(final Operator operator) {
		return new ProgressAnimation(() -> {
			OperatorProgress progress = operator.getProgress();
			if (progress.isIndeterminate()) {
				return 0;
			}
			return progress.getProgress();
		});
	}

}
