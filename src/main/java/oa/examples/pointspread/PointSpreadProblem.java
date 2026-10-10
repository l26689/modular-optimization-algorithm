package oa.examples.pointspread;

import oa.components.problems.coninuousproblem.ContinuousProblem;

/**
 * 2D 正方形内均匀散布 N 个点，使「最大点距 / 最小点距」的比值最小。
 * <p>
 * 解的表示：长度为 {@code 2 * N} 的 {@code double[]}，
 * 第 i 个点的坐标为 {@code (x[2i], x[2i+1])}，坐标范围 [0, 1]（单位正方形）。
 * <p>
 * 目标函数（越小越优）：
 * <pre>{@code
 *   f(x) = maxDist(x) / minDist(x)
 * }</pre>
 * 其中 maxDist / minDist 分别为所有点对之间的最大 / 最小欧氏距离。
 * 若存在重合点（minDist ≈ 0），返回一个极大的惩罚值，避免除零。
 */
public class PointSpreadProblem extends ContinuousProblem {

    /** 点数 */
    public static final int POINT_COUNT = 16;
    /** 正方形网格的边长点数（16 = 4×4），用于网格初始化 */
    public static final int GRID = 4;
    /** 解向量的维度 = 2 * 点数 */
    public static final int DIM = POINT_COUNT * 2;

    public PointSpreadProblem() {
        super(createBounds(DIM, 0.0), createBounds(DIM, 1.0));
    }

    /**
     * 计算所有点对之间的最小与最大欧氏距离。
     *
     * @return {@code double[2]}，[0] 为最小距离，[1] 为最大距离
     */
    public double[] minMaxDist(double[] x) {
        double min = Double.POSITIVE_INFINITY;
        double max = 0.0;
        for (int i = 0; i < POINT_COUNT; i++) {
            double xi = x[2 * i];
            double yi = x[2 * i + 1];
            for (int j = i + 1; j < POINT_COUNT; j++) {
                double dx = xi - x[2 * j];
                double dy = yi - x[2 * j + 1];
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d < min) min = d;
                if (d > max) max = d;
            }
        }
        return new double[]{min, max};
    }

    @Override
    public Double evaluate(double[] x) {
        double[] mm = minMaxDist(x);
        if (mm[0] < 1e-12) {
            // 两点重合的退化情形，返回极大惩罚值
            return 1e12;
        }
        return mm[1] / mm[0];
    }

    @Override
    public double[] copyX(double[] x) {
        return x.clone();
    }
}
