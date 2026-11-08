package moa.framework.problem;

/**
 * 优化问题的抽象契约，负责评价解的质量。
 * <p>
 * 本接口将解的表示类型 {@code X} 与评价逻辑彻底解耦，
 * 使框架能够同时处理连续优化和离散组合优化问题。
 *
 * <h3>实现要求</h3>
 * <ul>
 *   <li>{@link #copyX(Object)} 必须返回完全独立的深拷贝对象，
 *   确保主算法保存的历史最优解不会被后续迭代意外修改。</li>
 *   <li>{@link #compare(Object, Object)} 用于比较两个解的优劣，决定哪个解更优。
 *   此方法内部封装了评估与比较逻辑，使得同一问题可支持不同的优化方向（最小化/最大化）
 *   而无需修改评估逻辑。</li>
 * </ul>
 *
 * <h3>最少信息原则</h3>
 * 本接口仅提供解的评价与拷贝能力，不传递任何迭代状态。
 * 组件的其他信息需求（如边界、维度等）由子接口或具体实现额外提供，
 * 遵循"谁需要谁声明"的接口隔离原则。
 *
 * @param <X> 解的表示类型，例如 {@code double[]}、{@code int[]} 或自定义数据结构
 */
public interface Problem<X> {
    /**
     * 创建给定解的完全独立副本（深拷贝）。
     * <p>
     * 主算法在更新最优解时调用本方法，确保返回的副本与原始解互不影响。
     * 对于数组类型，可使用 {@code clone()}；对于复杂对象，需递归复制所有可变字段。
     *
     * @param x 待拷贝的解
     * @return 内容与 {@code x} 完全相同的新对象，两者无共享可变状态
     */
    X copyX(X x);

    /**
     * 比较两个解对应的的目标值的优劣，返回带符号的差值以指示优劣程度。
     * <p>
     * 本方法内部可能会调用 {@link #evaluate} 获取两个解的目标值后进行对比，
     * 将优化方向（最小化/最大化）与评估逻辑解耦：
     * 评估函数始终返回原始目标值，而比较逻辑由本方法单独定义。
     * 这样，同一问题的评估函数无需修改，即可在最小化和最大化之间切换。
     *
     * <h3>返回值约定</h3>
     * <ul>
     *   <li>{@link Comparision#relation} 表示 {@code x1} 与 {@code x2} 的比较结果
     * 其中 {@link Comparision#Relation#LESS} 表示 {@code x1} 劣于 {@code x2}，
     *      {@link Comparision#Relation#GREATER} 表示 {@code x1} 优于 {@code x2}，
     *      {@link Comparision#Relation#EQUAL} 表示两者等优。
     *      {@link Comparision#Relation#UNCOMPARABLE} 表示无法比较（无支配关系）。
     *      本方法仅定义<b>偏序关系</b>（partial order），不要求全序（total order）：
     *   <li>{@link Comparision#gap} 表示 {@code x1} 与 {@code x2} 的目标值差值大小绝对值。
     * </ul>
     * 返回值的 <b>绝对值</b> 表示优劣差距的大小：
     * 绝对值越大，两个解的质量差距越显著。
     * 此信息使模拟退火中的 Metropolis 准则能够根据差距大小动态调整接受概率，
     * 而非仅依赖二值判断。
     * <p>
     * 示例：对于最小化问题，{@code compare(x1, x2) = evaluate(x2) - evaluate(x1)}，
     * 当 {@code evaluate(x1) < evaluate(x2)} 时返回正值，表示 {@code x1}（更小的值）更优。
     *
     * <h3>反对称性的缺失</h3>
     * 本方法 <b>不保证</b> {@code compare(A, B) = -compare(B, A)}。
     * 在单目标全序场景中，反对称性自然成立（如上述最小化示例）；
     * 但在多目标 Pareto 支配等偏序场景中，符号和绝对值都可能不对称。
     * 调用者不应依赖反对称性，而应始终以本方法的直接返回值作为唯一依据。
     *
     * @param x1 第一个解，不可被修改
     * @param x2 第二个解，不可被修改
     * @return 正值表示 {@code evaluate(x1)} 优于 {@code evaluate(x2)}；负值表示 {@code evaluate(x1)} 劣于 {@code evaluate(x2)}；
     *         零表示两者等优或无法比较（无支配关系）；绝对值表示优劣差距大小
     */
    Comparision compare(X x1, X x2);
    /**
     * 比较结果封装类，包含比较结果和优劣差距大小。
     * <p>
     * 本类用于封装 {@link #compare(X, X)} 方法的返回值，
     * 有两个字段：{@link #relation} 和 {@link #gap}。
     * relation 表示 {@code x1} 与 {@code x2} 的比较结果，
     * gap 表示 {@code x1} 与 {@code x2} 的目标值差值大小绝对值。
     * <p>
     * relation的取值范围为 {@link Relation}，
     * 其中 {@link Relation#LESS} 表示 {@code x1} 劣于 {@code x2}、
     *      {@link Relation#GREATER} 表示 {@code x1} 优于 {@code x2}、
     *      {@link Relation#EQUAL} 表示两者等优。
     *      {@link Relation#UNCOMPARABLE} 表示无法比较（无支配关系）。
     * 当relation为EQUAL或UNCOMPARABLE时，gap无意义，请勿依赖；
     * 提供更丰富的比较信息。
     */
}