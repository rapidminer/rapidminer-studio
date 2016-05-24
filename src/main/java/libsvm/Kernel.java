/**
 * Copyright (c) 2000-2005 Chih-Chung Chang and Chih-Jen Lin All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither name of copyright holders nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package libsvm;

public abstract class Kernel extends QMatrix {

	private svm_node[][] x;
	private final double[] x_square;

	// svm_parameter
	private final int kernel_type;
	private final int degree;
	private final double gamma;
	private final double coef0;

	@Override
	abstract float[] get_Q(int column, int len);

	@Override
	abstract float[] get_QD();

	@Override
	void swap_index(int i, int j) {
		do {
			svm_node[] _ = x[i];
			x[i] = x[j];
			x[j] = _;
		} while (false);
		if (x_square != null) {
			do {
				double _ = x_square[i];
				x_square[i] = x_square[j];
				x_square[j] = _;
			} while (false);
		}
	}

	private static double powi(double base, int times) {
		double tmp = base, ret = 1.0;

		for (int t = times; t > 0; t /= 2) {
			if (t % 2 != 0) {
				ret *= tmp;
			}
			tmp = tmp * tmp;
		}
		return ret;
	}

	private static double tanh(double x) {
		double e = Math.exp(x);
		return 1.0 - 2.0 / (e * e + 1);
	}

	public double kernel_function(int i, int j) {
		switch (kernel_type) {
			case svm_parameter.LINEAR:
				return dot(x[i], x[j]);
			case svm_parameter.POLY:
				return powi(gamma * dot(x[i], x[j]) + coef0, degree);
			case svm_parameter.RBF:
				return Math.exp(-gamma * (x_square[i] + x_square[j] - 2 * dot(x[i], x[j])));
			case svm_parameter.SIGMOID:
				return tanh(gamma * dot(x[i], x[j]) + coef0);
			case svm_parameter.PRECOMPUTED:
				return x[i][(int) x[j][0].value].value;
			default:
				return 0;	// java
		}
	}

	Kernel(int l, svm_node[][] x_, svm_parameter param) {
		this.kernel_type = param.kernel_type;
		this.degree = param.degree;
		this.gamma = param.gamma;
		this.coef0 = param.coef0;

		x = x_.clone();

		if (kernel_type == svm_parameter.RBF) {
			x_square = new double[l];
			for (int i = 0; i < l; i++) {
				x_square[i] = dot(x[i], x[i]);
			}
		} else {
			x_square = null;
		}
	}

	static double dot(svm_node[] x, svm_node[] y) {
		double sum = 0;
		int xlen = x.length;
		int ylen = y.length;
		int i = 0;
		int j = 0;
		while (i < xlen && j < ylen) {
			if (x[i].index == y[j].index) {
				sum += x[i++].value * y[j++].value;
			} else {
				if (x[i].index > y[j].index) {
					++j;
				} else {
					++i;
				}
			}
		}
		return sum;
	}

	static double k_function(svm_node[] x, svm_node[] y, svm_parameter param) {
		switch (param.kernel_type) {
			case svm_parameter.LINEAR:
				return dot(x, y);
			case svm_parameter.POLY:
				return powi(param.gamma * dot(x, y) + param.coef0, param.degree);
			case svm_parameter.RBF: {
				double sum = 0;
				int xlen = x.length;
				int ylen = y.length;
				int i = 0;
				int j = 0;
				while (i < xlen && j < ylen) {
					if (x[i].index == y[j].index) {
						double d = x[i++].value - y[j++].value;
						sum += d * d;
					} else if (x[i].index > y[j].index) {
						sum += y[j].value * y[j].value;
						++j;
					} else {
						sum += x[i].value * x[i].value;
						++i;
					}
				}

				while (i < xlen) {
					sum += x[i].value * x[i].value;
					++i;
				}

				while (j < ylen) {
					sum += y[j].value * y[j].value;
					++j;
				}

				return Math.exp(-param.gamma * sum);
			}
			case svm_parameter.SIGMOID:
				return tanh(param.gamma * dot(x, y) + param.coef0);
			case svm_parameter.PRECOMPUTED:
				return x[(int) y[0].value].value;
			default:
				return 0;	// java
		}
	}
}
