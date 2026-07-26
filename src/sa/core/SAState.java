package sa.core;

import oa.api.State;

/**
 * 模拟退算法的迭代状态封装。
 * <p>
 * 本类继承自 {@link State}，在基础解信息之上扩展了模拟退火特有的状态字段：
 * 温度和接受标志。主算法在每次迭代中创建此对象，并将其传递给各组件
 * （扰动器、冷却策略、终止条件），使组件能获取当前迭代的完整上下文。
 *
 * <h3>状态字段说明</h3>
 * <ul>
 *   <li>{@code currentX} - 当前解（继承自父类），通过 {@link #currentX()} 访问</li>
 *   <li>{@code temperature} - 当前系统温度，通过 {@link #temperature()} 访问</li>
 *   <li>{@code isAccepted} - 上一轮迭代是否接受了新解，通过 {@link #isAccepted()} 访问</li>
 * </ul>
 *
 * <h3>不可变性</h3>
 * 本类所有字段均为 {@code final}，创建后不可修改，确保组件接收到的状态信息
 * 在单次迭代内保持一致，避免并发或引用传递导致的意外修改。
 *
 * <h3>冷启动约定</h3>
 * 首次迭代前，主算法会将 {@code isAccepted} 初始化为 {@code false}，
 * 表示"尚无历史"。各组件应能正确处理此初始状态。
 *
 * @param <X> 解的表示类型（例如 {@code double[]}、{@code int[]}）
 */
public class SAState<X> extends State<X> {
    /** 当前系统温度，用于控制扰动幅度和接受概率 */
    private final double temperature;
    /** 上一轮迭代是否接受了新解；首次迭代时为 {@code false} */
    private final boolean isAccepted;

    /**
     * 构造一个模拟退火迭代状态对象。
     *
     * @param currentX    当前解，不为 {@code null}
     * @param temperature 当前系统温度
     * @param isAccepted  上一轮迭代是否接受了新解；首次迭代时应为 {@code false}
     */
    public SAState(X currentX, double temperature, boolean isAccepted) {
        super(currentX);
        this.temperature = temperature;
        this.isAccepted = isAccepted;
    }

    /**
     * 获取当前系统温度。
     * <p>
     * 温度值影响扰动器的步长选择和 Metropolis 接受准则的概率计算。
     * 随着迭代进行，温度通常逐步降低（由冷却策略控制）。
     *
     * @return 当前温度值
     */
    public double temperature() { return temperature; }

    /**
     * 获取上一轮迭代的接受结果。
     * <p>
     * 该值反映刚结束的迭代中新候选解是否被接受：
     * <ul>
     *   <li>{@code true} - 新解被接受（优于当前解或按 Metropolis 准则接受）</li>
     *   <li>{@code false} - 新解未被接受</li>
     * </ul>
     * 首次迭代时此值固定为 {@code false}，表示"尚无历史"，
     * 组件应将此视为冷启动信号，使用默认策略。
     *
     * @return 上一轮迭代是否接受了新解
     */
    public boolean isAccepted() { return isAccepted; }
}