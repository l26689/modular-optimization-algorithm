package meta;

/**
 * 满意度函数 —— 元优化框架的<b>唯一中间表示</b>。
 * <p>
 * 把一个解映射为一个"满意度"分数，<b>越大表示越满意</b>。
 * <p>
 * 本接口是"用户如何描述目标"与"框架如何使用目标"之间的接缝。
 * 无论目标来自预设、表达式还是用户代码，最终都归结为这一个函数对象，
 * 因此后续新增输入方式时，只需新增一个实现类，
 * {@link RuntimeProblem}、{@link MetaProblem} 等消费方一律无需改动。
 *
 * <h3>与框架语义的换算</h3>
 * 框架内部（{@link oa.components.problems.coninuousproblem.ContinuousProblem#compare}）
 * 采用<b>最小化</b>语义，而本接口采用<b>最大化</b>（满意度）语义。
 * 二者的换算是取负号，且<b>只是取负号</b>：
 * <pre>{@code
 *   cost(x) = -satisfaction(x)
 * }</pre>
 * 该换算由 {@link RuntimeProblem} 统一完成。
 *
 * <h3>⚠️ 为什么必须只取负号，不能换别的形式</h3>
 * 模拟退火的接受概率为 {@code exp(compare / T)}，其中 {@code compare}
 * 的<b>绝对尺度</b>直接决定接受行为。若把满意度改写成 {@code 1/(1+cost)}
 * 这类形式，数值会被压缩到 0~1 之间，温度参数将失去意义，算法行为严重偏离。
 * 取负号则保持尺度不变：
 * {@code compare(x1,x2) = S(x1) - S(x2) = cost(x2) - cost(x1)}，与原语义完全一致。
 *
 * <h3>实现契约</h3>
 * <ul>
 *   <li><b>必须是纯函数</b>：相同输入必须返回相同输出，不得依赖随机数、
 *       时间、外部可变状态。框架依赖此性质保证优化结果可复现。</li>
 *   <li><b>必须返回有限值</b>：不允许返回 {@code NaN} 或无穷。
 *       若某解在数学上退化（如除零），应在函数内部返回一个有限的大惩罚值。</li>
 * </ul>
 *
 * @see RuntimeProblem
 */
@FunctionalInterface
public interface ObjectiveFunction {

    /**
     * 计算一个解的满意度分数。
     *
     * @param x 待评估的解，长度等于问题维度
     * @return 满意度分数，<b>越大越满意</b>；必须是有限值
     */
    double evaluate(double[] x);

    /**
     * 把解翻译成人能读懂的描述，用于结果展示。
     * <p>
     * 默认实现直接打印解向量的各分量。若你的问题有专门的解码方式
     * （例如把连续向量「排序取秩」得到一个排列），应当重写本方法——
     * 解码规则只存在于你的 {@link #evaluate} 里，框架无从得知，
     * 否则界面只能给你一串原始小数。
     * <p>
     * 本方法为 {@code default}，因此不影响本接口是函数式接口。
     *
     * @param x 待描述的解
     * @return 一句话描述，不得为 null
     */
    default String describe(double[] x) {
        return java.util.Arrays.toString(x);
    }
}
