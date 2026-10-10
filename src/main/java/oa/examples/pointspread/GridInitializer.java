package oa.examples.pointspread;

import java.util.Random;

import oa.components.problems.coninuousproblem.ContinuousProblem;
import sa.core.SAInitializer;
import sa.core.SAState;

/**
 * 网格启发式初始化器：把 16 个点放在 4×4 等距网格上，并叠加小幅度随机抖动。
 * <p>
 * 4×4 网格正是该问题的（近似）最优结构，作为热启动可让 SA 快速收敛到
 * 比值 3√2 ≈ 4.2426 附近。抖动保证每次运行起点略有不同，便于多轮重启取最优。
 */
public class GridInitializer implements SAInitializer<double[], ContinuousProblem> {

    private final double jitter;
    private final double initialTemperature;
    private Random random;

    public GridInitializer(double jitter, double initialTemperature) {
        this.jitter = jitter;
        this.initialTemperature = initialTemperature;
    }

    @Override
    public void init(ContinuousProblem problem, Random random) {
        this.random = random;
    }

    @Override
    public double[] initialX() {
        int n = PointSpreadProblem.GRID;
        double spacing = 1.0 / (n - 1);
        double[] x = new double[PointSpreadProblem.DIM];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int p = i * n + j;
                x[2 * p] = clamp(i * spacing + (random.nextDouble() * 2 - 1) * jitter, 0.0, 1.0);
                x[2 * p + 1] = clamp(j * spacing + (random.nextDouble() * 2 - 1) * jitter, 0.0, 1.0);
            }
        }
        return x;
    }

    @Override
    public double initialTemperature() {
        return initialTemperature;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
