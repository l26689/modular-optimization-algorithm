package sa.components;

import java.util.Random;

import oa.api.problem.Problem;
import sa.core.SACoolingSchedule;
import sa.core.SAState;

/**
 * 简单几何冷却策略：每次迭代温度乘以固定的衰减率 {@code rate}。
 * <p>
 * 与 {@link SABasicCoolingSchedule}（分段冷却，每 N 次调用降温一次）相对：
 * 本类<b>每次调用都降温</b>，且不持有任何可变状态（字段全为 {@code final}），
 * 因此同一个实例可以安全地跨多次运行复用。
 */
public final class GeometricCoolingSchedule
        implements SACoolingSchedule<double[], Problem<double[]>, SAState<double[]>> {

    private final double rate;

    public GeometricCoolingSchedule(double rate) {
        this.rate = rate;
    }

    @Override
    public void init(Problem<double[]> problem, Random random) {
        // 无额外初始化
    }

    @Override
    public double cool(SAState<double[]> state) {
        return state.getTemperature() * rate;
    }
}
