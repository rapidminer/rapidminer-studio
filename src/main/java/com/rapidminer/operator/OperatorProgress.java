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
package com.rapidminer.operator;

import com.rapidminer.tools.AbstractObservable;


/**
 *
 * The {@link OperatorProgress} can be used to report execution progress of an operator. If it is
 * not intialized by calling {@link #setTotal(int)} the operator will be displayed with an
 * indeterminate progress. If the progress has been initialized calls to {@link #setCompleted(int)},
 * {@link #step()} or {@link #step(int)} can be used increase the current operator progress.
 *
 * @author Nils Woehler
 * @since 7.0.0
 *
 */
public final class OperatorProgress extends AbstractObservable<OperatorProgress> {

	/**
	 * This value is the default value for the {@link #total} variable. It indicates that the
	 * {@link OperatorProgress} does not report a progress.
	 */
	public static final int NO_PROGRESS = -1;

	/**
	 * Initially set to {@value #NO_PROGRESS} so Operators that do not use the
	 * {@link OperatorProgress} (i.e. do not call {@link #setTotal(int)} at all) will be displayed
	 * as indeterminate.
	 */
	private int total = NO_PROGRESS;
	private int completed = 0;
	private boolean indeterminate = false;

	private boolean checkForStop = true;

	/**
	 * The parent operator. Used to call {@link Operator#checkForStop()} when setting operator
	 * progress.
	 */
	private final Operator op;

	/**
	 * Constructs a new {@link OperatorProgress} instance
	 *
	 * @param op
	 *            the parent operator
	 */
	OperatorProgress(Operator op) {
		if (op == null) {
			throw new IllegalArgumentException("Operator must not be null");
		}
		this.op = op;
	}

	/**
	 * Changes the total amount of progress and sets the current progress to 0. If this method is
	 * not called or is called with a value <= 0 the progress of the operator will be indeterminate.
	 *
	 * @param total
	 *            the total amount of progress
	 */
	public void setTotal(int total) {
		this.completed = 0;
		if (total > 0) {
			this.total = total;
		} else {
			this.total = NO_PROGRESS;
		}
		fireUpdate(this);
	}

	/**
	 * Changes the completed amount of progress and calls the {@link Operator#checkForStop()} method
	 * if {@link #isCheckForStop()} returns {@code true}. If the provided amount is greater then
	 * {@link #getTotal()}, the amount will be set to the value returned by {@link #getTotal()}.
	 * <p>
	 * <b>CAUTION:</b> The calculation performance might decrease if this method is called too often
	 * and {@link #isCheckForStop()} returns {@code true} (default behavior)
	 *
	 * @throws ProcessStoppedException
	 *             if the process has been stopped
	 */
	public void setCompleted(int completed) throws ProcessStoppedException {
		if (completed <= this.completed) {
			return;
		} else if (completed > this.total) {
			completed = total;
		}
		if (isCheckForStop()) {
			op.checkForStop();
		}
		this.completed = completed;
		fireUpdate(this);
	}

	/**
	 * Completes the current progress by setting total equal to completed.
	 */
	public void complete() {
		this.completed = this.total;
	}

	/**
	 * Increases the completed amount of progress by one. Furthermore the
	 * {@link Operator#checkForStop()} method is called.
	 * <p>
	 * <b>CAUTION:</b> The calculation performance might decrease if this method is called too often
	 * and {@link #isCheckForStop()} returns {@code true} (default behavior)
	 *
	 * @throws ProcessStoppedException
	 *             if the process has been stopped
	 */
	public void step() throws ProcessStoppedException {
		step(1);
	}

	/**
	 * Increases the completed amount of progress by {@code amount}. Furthermore the
	 * {@link Operator#checkForStop()} method is called.
	 * <p>
	 * <b>CAUTION:</b> The calculation performance might decrease if this method is called too often
	 * and {@link #isCheckForStop()} returns {@code true} (default behavior)
	 *
	 * @param amount
	 *            the amount you want to increase the completed progress.
	 *
	 * @throws ProcessStoppedException
	 *             if the process has been stopped
	 */
	public void step(int amount) throws ProcessStoppedException {
		setCompleted(this.completed + amount);
	}

	/**
	 * @return the total progress
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * @return the completed progress
	 */
	public int getCompleted() {
		return completed;
	}

	/**
	 * Returns whether the progress is indeterminate or not. The progress is indeterminate if either
	 * {@link #indeterminate} has been explicitly set to {@code true} or if {@link #getTotal()}
	 * returns a value equal or lower than {@value #NO_PROGRESS}.
	 *
	 * @return whether the current progress is indeterminate or not
	 */
	public boolean isIndeterminate() {
		return indeterminate || total <= NO_PROGRESS;
	}

	/**
	 * Allows to define whether the current progress is indeterminate. Changes the appearance of
	 * operator progress bar.
	 */
	public void setIndeterminate(boolean indeterminate) {
		this.indeterminate = indeterminate;
		fireUpdate(this);
	}

	/**
	 * Checks whether total equals completed
	 *
	 * @return <code>true</code> if {@link #total} equals {@link #completed}.
	 */
	public boolean isCompleted() {
		return total == completed;
	}

	/**
	 * @return the current progress (between 0 and 100)
	 */
	public int getProgress() {
		if (total > 0) {
			// prevent integer overflow of completed * 100
			return (int) (completed * (long) 100 / total);
		}
		return 0;
	}

	/**
	 * Resets completed to {@code 0}. Does not change the total amount of progress.
	 */
	public void reset() {
		this.completed = 0;
		fireUpdate(this);
	}

	/**
	 * @return whether the {@link #setCompleted(int)} method will call
	 *         {@link Operator#checkForStop()}
	 */
	public boolean isCheckForStop() {
		return checkForStop;
	}

	/**
	 * @param checkForStop
	 *            whether the {@link #setCompleted(int)} method will call
	 *            {@link Operator#checkForStop()}
	 */
	public void setCheckForStop(boolean checkForStop) {
		this.checkForStop = checkForStop;
	}

}
