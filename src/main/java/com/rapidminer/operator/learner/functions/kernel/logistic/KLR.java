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
package com.rapidminer.operator.learner.functions.kernel.logistic;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.learner.functions.kernel.AbstractMySVMLearner;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExample;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.Kernel;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.svm.SVMInterface;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * The main class for the Kernel Logistic Regression.
 *
 * @author Stefan Rueping
 */
public class KLR implements SVMInterface {

	private static final int OPERATOR_PROGRESS_STEPS = 20;

	protected Kernel kernel;

	protected SVMExamples examples;

	int n; // #examples

	double[] target;

	int n1;

	int n2;

	// internal vars
	double[] alphas;

	double[] hCache;

	boolean[] atBound;

	int iUp;

	int iLow;

	double b;

	double bUp;

	double bLow;

	// user-defined params
	double tol = 1e-3; // Genauigkeit f?r b (convergence_epsilon)

	double C = 1.0d;

	double epsilon; // Genauigkeit f?r t (is_zero)

	double mu; // Genauigkeit f?r bound (is_zero)

	int maxIterations = 100000; // maximale Anzahl Iterationen im
								 // Newton-Raphson-Schritt

	private OperatorProgress operatorProgress;

	public KLR() {}

	public KLR(Operator paramOperator) throws UndefinedParameterError {
		C = paramOperator.getParameterAsDouble(AbstractMySVMLearner.PARAMETER_C);
		tol = paramOperator.getParameterAsDouble(AbstractMySVMLearner.PARAMETER_CONVERGENCE_EPSILON);
		maxIterations = paramOperator.getParameterAsInt(AbstractMySVMLearner.PARAMETER_MAX_ITERATIONS);
		operatorProgress = paramOperator.getProgress();
	}

	@Override
	public void init(Kernel new_kernel, SVMExamples new_examples) {
		kernel = new_kernel;
		examples = new_examples;

		// copy examples to local vars
		target = examples.get_ys();
		alphas = examples.get_alphas();
		n = examples.count_examples();

		epsilon = C * 1e-10; // C*getParam("is_zero")

		mu = epsilon;
		n1 = examples.count_pos_examples();
		n2 = n - n1;
	}

	final double dG(double alpha) {
		// return dG(alpha/C)
		return Math.log(alpha / (C - alpha));
	}

	final double dPhi(double t, int i, int j, double ai, double aj, double Kii, double Kij, double Kjj) {
		// return Hi(alpha(t)) - Hj(alpha(t))
		double result = 0.0;
		double ydG = 0.0;
		if (target[i] > 0) {
			result = Kii - Kij;
			ydG = dG(ai + t) - dG(ai);
		} else {
			result = Kij - Kii;
			ydG = dG(ai) - dG(ai - t);
		}

		if (target[j] > 0) {
			result = Kjj - Kij;
			ydG -= dG(aj - t) - dG(aj);
		} else {
			result = Kij - Kjj;
			ydG -= dG(aj) - dG(aj + t);
		}

		// result *= t;
		result = t * (Kii - 2.0 * Kij + Kjj);
		result += ydG;
		result += hCache[i] - hCache[j]; // value for t=0

		return result;
	}

	final double d2Phi(double t, int i, int j, double ai, double aj, double Kii, double Kij, double Kjj) {
		double result;
		double atilde;
		if (target[i] > 0) {
			atilde = ai + t;
		} else {
			atilde = ai - t;
		}

		result = C / (atilde * (C - atilde));
		if (target[j] > 0) {
			atilde = aj - t;
		} else {
			atilde = aj + t;
		}

		result += C / (atilde * (C - atilde));

		result += Kii - 2.0 * Kij + Kjj; // eta

		return result;
	}

	protected boolean takeStep(int i, int j) {
		double[] kernel_row_i = kernel.get_row(i);
		double[] kernel_row_j = kernel.get_row(j);
		double aio = alphas[i];
		double ajo = alphas[j];
		double yi = target[i];
		double yj = target[j];
		double Hio = hCache[i];
		double Hjo = hCache[j];
		double Kii = kernel_row_i[i];
		double Kij = kernel_row_i[j];
		double Kjj = kernel_row_j[j];
		int takestepFlag = 1;

		// Compute t_min and t_max
		double t_max;
		double t_min;
		double t_tmp;
		if (yi > 0) {
			t_min = mu / 2.0 - aio;
			t_max = C - mu / 2.0 - aio;
		} else {
			t_max = aio - mu / 2.0;
			t_min = aio - (C - mu / 2.0);
		}

		if (yj > 0) {
			t_tmp = ajo - mu / 2.0;
			if (t_tmp < t_max) {
				t_max = t_tmp;
			}

			t_tmp = ajo - (C - mu / 2.0);
			if (t_tmp > t_min) {
				t_min = t_tmp;
			}

		} else {
			t_tmp = mu / 2.0 - ajo;
			if (t_tmp > t_min) {
				t_min = t_tmp;
			}

			t_tmp = C - mu / 2.0 - ajo;
			if (t_tmp < t_max) {
				t_max = t_tmp;
			}

		}

		if (t_max - t_min <= epsilon) {
			return false;
		}

		double t = 0.0;
		double the_dPhi = Hio - Hjo;
		double the_d2Phi = Kii - 2.0 * Kij + Kjj + C / (aio * (C - aio)) + C / (ajo * (C - ajo));
		double dPhi_left = 0.0;
		double d2Phi_left = 0.0;
		double dPhi_right = 0.0;
		double d2Phi_right = 0.0;
		double t_left = 0.0;
		double t_right = 0.0;

		if (the_dPhi > 0) {
			// Compute dPhi(t) and d2Phi(t) at t = t_min and denoted as
			// dPhi_left, d2Phi_left
			dPhi_left = dPhi(t_min, i, j, aio, ajo, Kii, Kij, Kjj);
			d2Phi_left = d2Phi(t_min, i, j, aio, ajo, Kii, Kij, Kjj);
			if (dPhi_left < 0) {
				t_left = t_min;
				t_right = t;
				dPhi_right = the_dPhi;
				d2Phi_right = the_d2Phi;
			} else {
				t = t_min;
				takestepFlag = 2;
			}

		} else if (the_dPhi < 0) {
			// Compute dPhi(t) and d2Phi(t) at t = t_max and denoted as
			// dPhi_right, d2Phi_right
			dPhi_right = dPhi(t_max, i, j, aio, ajo, Kii, Kij, Kjj);
			d2Phi_right = d2Phi(t_max, i, j, aio, ajo, Kii, Kij, Kjj);
			if (dPhi_right > 0) {
				t_left = t;
				t_right = t_max;
				dPhi_left = the_dPhi;
				d2Phi_left = the_d2Phi;
			} else {
				t = t_max;
				takestepFlag = 2;
			}

		} else {
			return false;
		}

		double t0;
		double dt;
		double ai = 0.0;
		double aj = 0.0;
		double Hi;
		double Hj;
		if (takestepFlag == 1) {
			// Choose a better start point
			if (Math.abs(dPhi_left) < Math.abs(dPhi_right)) {
				t0 = t_left;
				the_dPhi = dPhi_left;
				the_d2Phi = d2Phi_left;
			} else {
				t0 = t_right;
				the_dPhi = dPhi_right;
				the_d2Phi = d2Phi_right;
			}
			do {
				dt = -the_dPhi / the_d2Phi; // Newton-Raphson step
				t = t0 + dt;
				if (t <= t_left || t >= t_right) {
					t = (t_left + t_right) / 2.0; // Bisection step
				}

				ai = aio + t / yi;
				aj = ajo - t / yj;

				Hi = Hio + t * (Kii - Kij) + yi * (Math.log(ai / (C - ai)) - Math.log(aio / (C - aio)));
				Hj = Hjo + t * (Kij - Kjj) + yj * (Math.log(aj / (C - aj)) - Math.log(ajo / (C - ajo)));

				the_dPhi = Hi - Hj;
				the_d2Phi = Kii - 2 * Kij + Kjj + C / (ai * (C - ai)) + C / (aj * (C - aj));

				// Update t_left and t_right
				if (the_dPhi * dPhi_left > 0) {
					t_left = t;
					dPhi_left = the_dPhi;
				} else {
					t_right = t;
					dPhi_right = the_dPhi;
				}

				t0 = t;
			} while (Math.abs(the_dPhi) > 0.1 * tol && t_left + epsilon < t_right);
		} else if (takestepFlag == 2) {
			ai = aio + t / yi;
			aj = ajo - t / yj;
			Hi = Hio + t * (Kii - Kij) + yi * (Math.log(ai / (C - ai)) - Math.log(aio / (C - aio)));
			Hj = Hjo + t * (Kij - Kjj) + yj * (Math.log(aj / (C - aj)) - Math.log(ajo / (C - ajo)));
		}

		if (t == 0) {
			return false;
		}

		// Save ai, aj, Hi, Hj
		alphas[i] = ai;
		alphas[j] = aj;
		// Update the boundary property of indices i and j (evt. clip alpha)
		if (ai <= mu) {
			if (target[i] > 0 && target[j] > 0 || target[i] < 0 && target[j] < 0) {
				alphas[j] -= mu - alphas[i];
			} else {
				alphas[j] += mu - alphas[i];
			}
			alphas[i] = mu;
			atBound[i] = true;
		} else if (ai >= C - mu) {
			if (target[i] > 0 && target[j] > 0 || target[i] < 0 && target[j] < 0) {
				alphas[j] -= C - mu - alphas[i];
			} else {
				alphas[j] += C - mu - alphas[i];
			}
			alphas[i] = C - mu;
			atBound[i] = true;
		} else {
			atBound[i] = false;
		}
		if (aj <= mu) {
			if (target[i] > 0 && target[j] > 0 || target[i] < 0 && target[j] < 0) {
				alphas[i] -= mu - alphas[j];
			} else {
				alphas[i] += mu - alphas[j];
			}
			alphas[j] = mu;
			atBound[j] = true;
		} else if (aj >= C - mu) {
			if (target[i] > 0 && target[j] > 0 || target[i] < 0 && target[j] < 0) {
				alphas[i] -= C - mu - alphas[j];
			} else {
				alphas[i] += C - mu - alphas[j];
			}
			alphas[j] = C - mu;
			atBound[j] = true;
		} else {
			atBound[j] = false;
		}
		t = ((alphas[i] - aio) * yi + (ajo - alphas[j]) * yj) / 2.0;

		Hi = Hio + t * (Kii - Kij) + yi * (Math.log(alphas[i] / (C - alphas[i])) - Math.log(aio / (C - aio)));
		Hj = Hjo + t * (Kij - Kjj) + yj * (Math.log(alphas[j] / (C - alphas[j])) - Math.log(ajo / (C - ajo)));

		for (int k = 0; k < n; k++) {
			hCache[k] += t * (kernel_row_i[k] - kernel_row_j[k]);
		}

		hCache[i] = Hi;
		hCache[j] = Hj;

		// Update i_low, i_up, b_low and b_up over indices of non-boundary group
		bUp = Double.NEGATIVE_INFINITY;
		bLow = Double.POSITIVE_INFINITY;
		iUp = 0;
		iLow = 0;
		for (int l = 0; l < n; l++) {
			if (!atBound[l]) {
				if (hCache[l] > bUp) {
					bUp = hCache[l];
					iUp = l;
				}
				if (hCache[l] < bLow) {
					bLow = hCache[l];
					iLow = l;
				}
			}
		}

		return true;
	} // end takeStep procedure

	public void klr() throws ProcessStoppedException {
		// main routine:

		// precond: n, target, examples, kernel intialisiert
		// alphas = new double[n];
		examples.clearAlphas();
		hCache = new double[n];
		atBound = new boolean[n];

		int i;
		int j;
		double alpha_pos = C / n1; // !!! N1 = # pos examples
		double alpha_neg = C / n2; // !!! N2 = # neg examples
		for (i = 0; i < n; i++) {
			if (target[i] > 0) {
				alphas[i] = alpha_pos;
			} else {
				alphas[i] = alpha_neg;
			}

			atBound[i] = false;
		}

		// initialize all Hcache[i]
		double sum_pos_K;
		double sum_neg_K;
		double[] kernel_row;
		bUp = Double.NEGATIVE_INFINITY;
		bLow = Double.POSITIVE_INFINITY;
		iUp = 0;
		iLow = 0;

		if (operatorProgress != null) {
			operatorProgress.setTotal(n);
		}

		for (i = 0; i < n; i++) {
			sum_pos_K = 0.0;
			sum_neg_K = 0.0;
			kernel_row = kernel.get_row(i);
			for (j = 0; j < n; j++) {
				if (target[j] > 0) {
					sum_pos_K += kernel_row[j];
				} else {
					sum_neg_K += kernel_row[j];
				}
			}
			hCache[i] = alpha_pos * sum_pos_K - alpha_neg * sum_neg_K + target[i] * dG(alphas[i]);
			if (hCache[i] > bUp) {
				bUp = hCache[i];
				iUp = i;
			}
			if (hCache[i] < bLow) {
				bLow = hCache[i];
				iLow = i;
			}

			if (operatorProgress != null && i % OPERATOR_PROGRESS_STEPS == 0) {
				operatorProgress.setCompleted(i);
			}
		}

		boolean Flag = false;
		double Hi;
		int numChange;

		int it = maxIterations;

		do {
			// take step with i_low and i_up
			while (2.0 * tol < bUp - bLow) {

				Flag = takeStep(iLow, iUp);
				if (--it < 0 || !Flag) {
					break;
				}
			}

			// check optimality of boundary indices
			numChange = 0;
			for (i = 0; i < n; i++) {
				if (atBound[i]) {
					Hi = hCache[i];
					if (Math.abs(Hi - bLow) >= Math.abs(Hi - bUp)) {
						it--;
						Flag = takeStep(i, iLow);
						if (!Flag) {
							it--;
							Flag = takeStep(i, iUp);
						}
					} else {
						it--;
						Flag = takeStep(i, iUp);
						if (!Flag) {
							it--;
							Flag = takeStep(i, iLow);
						}
					}
					if (!atBound[i]) {
						numChange++;
					}
				}
			}
		} while (numChange != 0 && it > 0);

		b = (bLow + bUp) / 2.0;

	}

	@Override
	public double predict(SVMExample sVMExample) {
		int i;
		SVMExample sv;
		double the_sum = examples.get_b();
		double alpha;
		for (i = 0; i < n; i++) {
			alpha = alphas[i];
			if (alpha != 0) {
				sv = examples.get_example(i);
				the_sum += alphas[i] * kernel.calculate_K(sv, sVMExample);
			}

		}

		the_sum = 1.0 / (1.0 + Math.exp(-the_sum));
		return the_sum;
	}

	@Override
	public void predict(SVMExamples to_predict) {
		int i;
		double prediction;
		SVMExample sVMExample;
		// int size = examples.count_examples(); // IM 04/02/12
		int size = to_predict.count_examples();
		for (i = 0; i < size; i++) {
			sVMExample = to_predict.get_example(i);
			prediction = predict(sVMExample);
			to_predict.set_y(i, prediction);
		}

	}

	@Override
	public void train() throws ProcessStoppedException {
		// train the klr
		klr();

		b = -b; // different b in SVM and KLR, set to SVM standard
		// copy local vars to training_set
		// save y_i*alpha_i instead of alpha_i !!! IM 04/02/12 passiert aber
		// nicht :-) !!!
		examples.set_b(b);
		for (int i = 0; i < n; i++) {
			if (target[i] < 0) {
				alphas[i] = -alphas[i];
			}

		}

	}

	/** Return the weights of the features. */
	@Override
	public double[] getWeights() {
		int dim = examples.get_dim();
		int examples_total = examples.count_examples();
		double[] w = new double[dim];
		for (int j = 0; j < dim; j++) {
			w[j] = 0;
		}
		for (int i = 0; i < examples_total; i++) {
			double[] x = examples.get_example(i).toDense(dim);
			double alpha = alphas[i];
			for (int j = 0; j < dim; j++) {
				w[j] += alpha * x[j];
			}
		}
		return w;
	}

	/** Returns the value of b. */
	@Override
	public double getB() {
		return examples.get_b();
	}

}
