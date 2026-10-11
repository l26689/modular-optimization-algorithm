package moa.algorithm.de.components;

import java.util.Random;

import moa.problem.continuous.ContinuousProblem;
import moa.algorithm.pso.api.PSOState;
import moa.algorithm.pso.api.Particle;
import moa.problem.continuous.components.RandomInitializer;

/**
 * DE/best/1 变异粒子 —— 变异基向量来自当前最优个体。
 *
 * <p>继承 RandomInitializer，实现 Particle<X:double[], Prob:ContinuousProblem, S:PSOState<double[]>>。是否为最终类：是。
 *
 * <p>变异公式：v = x_best + F * (x_r1 - x_r2)。具有快速收敛的特性。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>约束绑定 —— X 固定为 double[]，Prob 固定为 ContinuousProblem。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public final class DEBest1Particle extends RandomInitializer<PSOState<double[]>> implements Particle<double[],ContinuousProblem,PSOState<double[]>> {
    private final double F;
    private final double CR;
    private double[] currentPosition;
    private ContinuousProblem problem;
    private int myIndex;

    public DEBest1Particle(double F, double CR,int myIndex) {
        this.F = F;
        this.CR = CR;
        this.myIndex = myIndex;
    }
    
    @Override
    public void init(ContinuousProblem problem,Random random) {
        this.problem = problem;
        super.init(problem,random);
    }

    @Override
    public double[] initialX() {
        currentPosition = super.initialX();
        return currentPosition;
    }


    @Override
    public double[] search(PSOState<double[]> state) {
        double[][] currentXs = state.getCurrentXs();
        int popSize = currentXs.length;

        int bestIdx = 0;
        for (int i = 1; i < popSize; i++) {
            if (problem.compare(currentXs[i], currentXs[bestIdx]) > 0) {
                bestIdx = i;
            }
        }
        double[] xBest = currentXs[bestIdx];

        int r1, r2;
        r1 = random.nextInt(popSize - 1);
        if (r1 >= myIndex) r1++;
        r2 = random.nextInt(popSize - 2);
        if (r2 >= Math.min(myIndex, r1)) r2++;
        if (r2 >= Math.max(myIndex, r1)) r2++;

        double[] xr1 = currentXs[r1];
        double[] xr2 = currentXs[r2];

        int dim = xBest.length;
        double[] v = new double[dim];
        for (int i = 0; i < dim; i++) {
            v[i] = xBest[i] + F * (xr1[i] - xr2[i]);
            if (v[i] < lowerBounds[i]) v[i] = lowerBounds[i];
            if (v[i] > upperBounds[i]) v[i] = upperBounds[i];
        }

        double[] trial = problem.copyX(currentPosition);
        int j = random.nextInt(dim);
        for (int i = 0; i < dim; i++) {
            if (random.nextDouble() < CR || i == j) {
                trial[i] = v[i];
            }
        }

        if (problem.compare(trial, currentPosition) > 0) {
            currentPosition = trial;
        }
        return problem.copyX(currentPosition);
    }
    
}