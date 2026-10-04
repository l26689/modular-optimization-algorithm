package de;

import java.util.Random;

import oa.components.initializers.RandomInitializer;
import oa.components.problems.coninuousproblem.ContinuousProblem;
import pso.core.PSOState;
import pso.core.Particle;

public final class DECurrentToBest1Particle extends RandomInitializer<PSOState<double[]>> implements Particle<double[],ContinuousProblem,PSOState<double[]>> {
    private final double F;
    private final double CR;
    private double[] currentPosition;
    private ContinuousProblem problem;
    private int myIndex;

    public DECurrentToBest1Particle(double F, double CR,int myIndex) {
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
            v[i] = currentPosition[i] + F * (xBest[i] - currentPosition[i]) + F * (xr1[i] - xr2[i]);
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