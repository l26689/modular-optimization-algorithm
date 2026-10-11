package moa.meta;

import java.util.Random;

import moa.framework.api.TerminationCondition;
import moa.framework.engine.State;
import moa.framework.problem.Problem;

/**
 * 按「目标函数评估次数」终止的终止条件，替代按「迭代轮数」的
 * {@code MaxCallTerminationCondition}。
 * <p>
 * 三种内层算法（SA / PSO / DE）每一轮消耗的评估次数不同（SA 约 4 次、
 * PSO 约 2N² 次），所以用相同轮数终止会让 PSO 偷跑十几倍算力。
 * 本条件改为直接盯着 {@link CountingProblem} 的累计评估次数，
 * 达到预算就停止，让三种算法在<b>相同的算力预算</b>下公平竞争。
 * <p>
 * {@link #check} 只读计数、不自增（幂等），计数完全由
 * {@link CountingProblem#evaluate} 负责递增。
 */
public final class EvaluationBudgetTerminationCondition
        implements TerminationCondition<double[], Problem<double[]>, State<double[]>> {

    /** 计数装饰器，和它共享同一个计数器。 */
    private final CountingProblem counting;

    /** 允许的总评估次数。 */
    private final int budget;

    public EvaluationBudgetTerminationCondition(CountingProblem counting, int budget) {
        this.counting = counting;
        this.budget = budget;
    }

    @Override
    public void init(Problem<double[]> prob, Random random) {
        // 无状态，无需初始化
    }

    @Override
    public boolean check(State<double[]> state) {
        return counting.getEvalCount() >= budget;
    }
}
