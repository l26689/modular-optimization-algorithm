package meta;

/**
 * 单个算法组合的评估结果。
 * <p>
 * 元优化层对每个配置重复运行多次（因为优化算法是随机的），
 * 本记录保存其统计量。所有"满意度"字段均遵循
 * {@link ObjectiveFunction} 的约定：<b>越大越满意</b>。
 *
 * <h3>为什么必须记录标准差</h3>
 * 若两个组合的平均满意度接近，但其中一个方差极大，说明它只是偶尔走运，
 * 并不比另一个稳健。只看均值会得出误导性的排名，
 * 因此本记录同时保存标准差与最好/最差情况。
 *
 * @param config             配置本身
 * @param meanSatisfaction   多次运行的平均满意度（排序依据）
 * @param stdDevSatisfaction 满意度的样本标准差
 * @param bestSatisfaction   多次运行中的最好满意度
 * @param worstSatisfaction  多次运行中的最差满意度
 * @param meanMillis         单次运行的平均耗时（毫秒）
 * @param repeats            重复运行次数
 * @param bestX              取得最好满意度的那次运行所返回的解；可能为 null
 */
public record EvaluationResult(
        ConfigCodec.Config config,
        double meanSatisfaction,
        double stdDevSatisfaction,
        double bestSatisfaction,
        double worstSatisfaction,
        double meanMillis,
        int repeats,
        double[] bestX) implements Comparable<EvaluationResult> {

    /**
     * 拷贝传入的解向量。
     * <p>
     * record 不会让数组字段自动变成不可变，若不拷贝，外部数组之后被修改
     * 会静默改变本记录的内容。
     */
    public EvaluationResult {
        bestX = bestX == null ? null : bestX.clone();
    }

    /** 返回取得最好满意度的那次运行的解；每次调用都返回副本。 */
    @Override
    public double[] bestX() {
        return bestX == null ? null : bestX.clone();
    }

    /**
     * 按平均满意度<b>降序</b>排列，使最优组合排在列表最前。
     */
    @Override
    public int compareTo(EvaluationResult other) {
        return Double.compare(other.meanSatisfaction, this.meanSatisfaction);
    }

    /** 返回该组合是否明显优于另一个（均值之差超过两者标准差的较大者）。 */
    public boolean clearlyBetterThan(EvaluationResult other) {
        double buffer = Math.max(this.stdDevSatisfaction, other.stdDevSatisfaction);
        return this.meanSatisfaction - other.meanSatisfaction > buffer;
    }
}
