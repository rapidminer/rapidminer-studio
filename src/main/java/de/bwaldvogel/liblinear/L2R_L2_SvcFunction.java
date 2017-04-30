/**
 * Copyright (c) 2007-2014 The LIBLINEAR Project. All rights reserved.
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
package de.bwaldvogel.liblinear;

class L2R_L2_SvcFunction implements Function {

	protected final Problem prob;
	protected final double[] C;
	protected final int[] I;
	protected final double[] z;

	protected int sizeI;

	public L2R_L2_SvcFunction(Problem prob, double[] C) {
		int l = prob.l;

		this.prob = prob;

		z = new double[l];
		I = new int[l];
		this.C = C;
	}

	@Override
	public double fun(double[] w) {
		int i;
		double f = 0;
		double[] y = prob.y;
		int l = prob.l;
		int w_size = get_nr_variable();

		Xv(w, z);

		for (i = 0; i < w_size; i++) {
			f += w[i] * w[i];
		}
		f /= 2.0;
		for (i = 0; i < l; i++) {
			z[i] = y[i] * z[i];
			double d = 1 - z[i];
			if (d > 0) {
				f += C[i] * d * d;
			}
		}

		return f;
	}

	@Override
	public int get_nr_variable() {
		return prob.n;
	}

	@Override
	public void grad(double[] w, double[] g) {
		double[] y = prob.y;
		int l = prob.l;
		int w_size = get_nr_variable();

		sizeI = 0;
		for (int i = 0; i < l; i++) {
			if (z[i] < 1) {
				z[sizeI] = C[i] * y[i] * (z[i] - 1);
				I[sizeI] = i;
				sizeI++;
			}
		}
		subXTv(z, g);

		for (int i = 0; i < w_size; i++) {
			g[i] = w[i] + 2 * g[i];
		}
	}

	@Override
	public void Hv(double[] s, double[] Hs) {
		int i;
		int w_size = get_nr_variable();
		double[] wa = new double[sizeI];

		subXv(s, wa);
		for (i = 0; i < sizeI; i++) {
			wa[i] = C[I[i]] * wa[i];
		}

		subXTv(wa, Hs);
		for (i = 0; i < w_size; i++) {
			Hs[i] = s[i] + 2 * Hs[i];
		}
	}

	protected void subXTv(double[] v, double[] XTv) {
		int i;
		int w_size = get_nr_variable();

		for (i = 0; i < w_size; i++) {
			XTv[i] = 0;
		}

		for (i = 0; i < sizeI; i++) {
			for (Feature s : prob.x[I[i]]) {
				XTv[s.getIndex() - 1] += v[i] * s.getValue();
			}
		}
	}

	private void subXv(double[] v, double[] Xv) {

		for (int i = 0; i < sizeI; i++) {
			Xv[i] = 0;
			for (Feature s : prob.x[I[i]]) {
				Xv[i] += v[s.getIndex() - 1] * s.getValue();
			}
		}
	}

	protected void Xv(double[] v, double[] Xv) {

		for (int i = 0; i < prob.l; i++) {
			Xv[i] = 0;
			for (Feature s : prob.x[i]) {
				Xv[i] += v[s.getIndex() - 1] * s.getValue();
			}
		}
	}

}
