package moa.example;

import moa.problem.continuous.AckleyProblem;
import moa.algorithm.pso.components.StandardPSOParticle;
import moa.algorithm.pso.engine.ParticleSwarmOptimization;
import moa.components.recorder.BestRecorder;
import moa.components.terminator.MaxCallTerminationCondition;

import java.util.Arrays;
import java.util.Random;

public class AckleyPSOComparison {

    private static final double META_OMEGA = 0.635335;
    private static final double META_C1    = 2.500000;
    private static final double META_C2    = 1.231464;

    private static final double ACADEMIC_OMEGA = 0.72984;
    private static final double ACADEMIC_C1    = 1.49445;
    private static final double ACADEMIC_C2    = 1.49445;

    private static final double[] OMEGA_VALUES = {0.4, 0.7, 1.0};
    private static final double[] C1_VALUES    = {1.0, 2.0, 3.0};
    private static final double[] C2_VALUES    = {1.0, 2.0, 3.0};

    private static final int DIM          = 10;
    private static final int SWARM_SIZE   = 30;
    private static final int BUDGET_FINAL = 1000;
    private static final int BUDGET_FAST  = 200;
    private static final int FAST_RUNS    = 3;
    private static final int FINAL_RUNS   = 50;

    void main() {
        System.out.println("=== Ackley " + DIM + "D PSO \u4e09\u65b9\u5bf9\u6bd4 ===");
        System.out.println("\u7c92\u5b50\u6570: " + SWARM_SIZE + "  |  \u6700\u7ec8\u5bf9\u6bd4\u8f6e\u6570: " + FINAL_RUNS
                + "  |  \u9884\u7b97: " + BUDGET_FINAL + " \u8f6e \u00d7 " + SWARM_SIZE + " \u7c92\u5b50");
        System.out.println("\u5b66\u754c\u57fa\u51c6: Clerc & Kennedy (2002) \u6536\u7f29\u56e0\u5b50, "
                + "\u03c9=\u03c7=" + ACADEMIC_OMEGA + " c1=c2=\u03c7\u00d7\u03c62.05=" + ACADEMIC_C1);
        System.out.println();

        String bestLabel = gridSearch();
        System.out.println("\u7f51\u683c\u641c\u7d22\u6700\u4f73: " + bestLabel);
        System.out.println();

        double[] metaResults     = new double[FINAL_RUNS];
        double[] manualResults   = new double[FINAL_RUNS];
        double[] academicResults = new double[FINAL_RUNS];

        for (int run = 0; run < FINAL_RUNS; run++) {
            long seed = run * 1000L + 42L;
            metaResults[run]     = runPSO(META_OMEGA,     META_C1,     META_C2,     seed, BUDGET_FINAL);
            manualResults[run]   = runPSO(bestOmega,      bestC1,      bestC2,      seed, BUDGET_FINAL);
            academicResults[run] = runPSO(ACADEMIC_OMEGA, ACADEMIC_C1, ACADEMIC_C2, seed, BUDGET_FINAL);

            if ((run + 1) % 10 == 0) {
                System.out.printf("  \u5df2\u5b8c\u6210 %2d/%d \u8f6e ...%n", run + 1, FINAL_RUNS);
            }
        }

        System.out.println();
        System.out.println("=== \u6700\u7ec8\u5bf9\u6bd4 (\u5404 " + FINAL_RUNS + " \u8f6e, \u9884\u7b97 "
                + BUDGET_FINAL + " \u8fed\u4ee3 \u00d7 " + SWARM_SIZE + " \u7c92\u5b50 = "
                + (BUDGET_FINAL * SWARM_SIZE) + " \u6b21\u8bc4\u4f30) ===");
        System.out.println();
        printStats("\u5143\u641c\u7d22  (\u03c9=" + fmt(META_OMEGA) + " c1=" + fmt(META_C1) + " c2=" + fmt(META_C2) + ")",
                metaResults);
        printStats("\u7f51\u683c\u6700\u4f73  (\u03c9=" + fmt(bestOmegaRound) + " c1=" + fmt(bestC1Round)
                + " c2=" + fmt(bestC2Round) + ")",
                manualResults);
        printStats("\u5b66\u754c\u57fa\u51c6  (\u03c9=" + fmt(ACADEMIC_OMEGA) + " c1=" + fmt(ACADEMIC_C1)
                + " c2=" + fmt(ACADEMIC_C2) + ")",
                academicResults);

        System.out.println();
        System.out.println("=== \u80dc\u8d1f\u7edf\u8ba1 (\u540c\u8f6e\u5171\u7528\u79cd\u5b50, \u521d\u59cb\u4f4d\u7f6e\u4e00\u81f4) ===");
        int[] wins = new int[3];
        String[] names = {"\u5143\u641c\u7d22", "\u7f51\u683c\u6700\u4f73", "\u5b66\u754c\u57fa\u51c6"};
        for (int i = 0; i < FINAL_RUNS; i++) {
            double a = metaResults[i], b = manualResults[i], c = academicResults[i];
            double best = Math.min(Math.min(a, b), c);
            if (a == best) wins[0]++;
            if (b == best) wins[1]++;
            if (c == best) wins[2]++;
        }
        for (int i = 0; i < 3; i++) {
            System.out.printf("%s: %2d / %d%n", names[i], wins[i], FINAL_RUNS);
        }
    }

    private static String fmt(double d) {
        if (d == (long) d) return String.format("%.1f", d);
        return String.format("%.6f", d);
    }

    private double bestOmega, bestC1, bestC2;
    private double bestOmegaRound, bestC1Round, bestC2Round;

    private String gridSearch() {
        double bestMean = Double.POSITIVE_INFINITY;

        int total = OMEGA_VALUES.length * C1_VALUES.length * C2_VALUES.length;
        System.out.printf("=== \u9636\u6bb5\u4e00: \u7f51\u683c\u7a77\u4e3e (%d \u79cd\u7ec4\u5408, \u5404 %d \u6b21\u5feb\u901f\u8fd0\u884c,"
                + " \u9884\u7b97 %d \u8fed\u4ee3) ===%n",
                total, FAST_RUNS, BUDGET_FAST);

        String bestLabel = "";
        int count = 0;

        for (double omega : OMEGA_VALUES) {
            for (double c1 : C1_VALUES) {
                for (double c2 : C2_VALUES) {
                    count++;
                    double[] scores = new double[FAST_RUNS];
                    for (int r = 0; r < FAST_RUNS; r++) {
                        scores[r] = runPSO(omega, c1, c2, (long) count * 100 + r, BUDGET_FAST);
                    }
                    double m = mean(scores);

                    System.out.printf("  [%2d/%d] \u03c9=%.1f c1=%.1f c2=%.1f  \u5747\u503c=%.6f%n",
                            count, total, omega, c1, c2, m);

                    if (m < bestMean) {
                        bestMean = m;
                        bestOmega = omega;
                        bestC1 = c1;
                        bestC2 = c2;
                        bestLabel = String.format("\u03c9=%.1f c1=%.1f c2=%.1f (\u5747\u503c %.6f)",
                                omega, c1, c2, m);
                    }
                }
            }
        }

        bestOmegaRound = bestOmega;
        bestC1Round = bestC1;
        bestC2Round = bestC2;
        System.out.println();
        return bestLabel;
    }

    private static double runPSO(double omega, double c1, double c2, long seed, int budget) {
        AckleyProblem problem = new AckleyProblem(DIM);

        StandardPSOParticle[] particles = new StandardPSOParticle[SWARM_SIZE];
        for (int i = 0; i < SWARM_SIZE; i++) {
            particles[i] = new StandardPSOParticle(omega, c1, c2);
        }

        ParticleSwarmOptimization<double[], AckleyProblem> pso =
                new ParticleSwarmOptimization<>(
                        new Random(seed),
                        problem,
                        particles,
                        new MaxCallTerminationCondition<double[]>(budget)
                );

        BestRecorder<double[]> recorder = new BestRecorder<>();
        pso.solve(recorder);
        return problem.evaluate(recorder.getBestX());
    }

    private static void printStats(String label, double[] data) {
        double[] sorted = data.clone();
        Arrays.sort(sorted);
        System.out.printf("%s: \u6700\u4f18=%.6f  \u6700\u5dee=%.6f  \u5747\u503c=%.6f"
                + "  \u4e2d\u4f4d\u6570=%.6f  \u6807\u51c6\u5dee=%.6f%n",
                label,
                sorted[0],
                sorted[sorted.length - 1],
                mean(data),
                median(sorted),
                std(data));
    }

    private static double mean(double[] arr) {
        double sum = 0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }

    private static double median(double[] sorted) {
        int n = sorted.length;
        if (n % 2 == 0) {
            return (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
        }
        return sorted[n / 2];
    }

    private static double std(double[] arr) {
        double m = mean(arr);
        double sumSq = 0;
        for (double v : arr) sumSq += (v - m) * (v - m);
        return Math.sqrt(sumSq / (arr.length - 1));
    }
}