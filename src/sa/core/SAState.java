package sa.core;

import java.lang.reflect.Array;

import oa.api.optimizationalgorithm.State;

/**
 * 模拟退火算法的迭代状态封装。
 * <p>
 * 本类实现自 {@link State}，在基础解信息之上扩展了模拟退火特有的状态字段：
 * 温度和接受标志。主算法在每次求解（solve）中创建<b>一个</b>本类实例，
 * 在各轮迭代中通过 {@link #set(Object, double, boolean)} 更新状态，
 * 并将其传递给各组件（扰动器、冷却策略、终止条件），使组件能获取当前迭代的完整上下文。
 *
 * <h3>状态字段说明</h3>
 * <ul>
 *   <li>{@code currentXs} - 当前解的数组（SA 中始终只包含一个元素，通过 {@code getCurrentXs()[0]} 访问）</li>
 *   <li>{@link #temperature} - 当前系统温度，用于控制扰动幅度和接受概率</li>
 *   <li>{@link #isAccepted} - 上一轮迭代是否接受了新解</li>
 * </ul>
 *
 * <h3>冷启动约定</h3>
 * 首次迭代前，主算法会将 {@code isAccepted} 初始化为 {@code false}，
 * 表示"尚无历史"。各组件应能正确处理此初始状态。
 *
 * <h3>使用约束</h3>
 * 本实例在整个求解过程中被复用，其内部状态（包括 {@code currentXs} 数组引用、
 * 解引用）会随迭代推进而变化。各组件<b>不应</b>保存对本实例或其
 * {@link #getCurrentXs()} 返回数组的引用跨迭代使用。
 * 如需持久化保存当前解，组件必须对解对象进行<b>深拷贝</b>，而非保存引用。
 *
 * @param <X> 解的表示类型（例如 {@code double[]}、{@code int[]}）
 */
public class SAState<X> implements State<X> {
    private X[] currentXs;

    /** 当前系统温度，用于控制扰动幅度和接受概率 */
    private double temperature;
    /** 上一轮迭代是否接受了新解；首次迭代时为 {@code false} */
    private boolean isAccepted;

    /**
     * 构造一个模拟退火迭代状态对象。
     *
     * @param currentX    当前解，不为 {@code null}
     * @param temperature 当前系统温度
     * @param isAccepted  上一轮迭代是否接受了新解；首次迭代时应为 {@code false}
     */
    public SAState(X currentX, double temperature, boolean isAccepted) {
        this.currentXs = (X[]) Array.newInstance(currentX.getClass(), 1);
        this.currentXs[0] = currentX;
        this.temperature = temperature;
        this.isAccepted = isAccepted;
    }


    public void set(X currentX, double temperature, boolean isAccepted) {
        this.currentXs[0] = currentX;
        this.temperature = temperature;
        this.isAccepted = isAccepted;
    }

    @Override
    public final X[] getCurrentXs() {
        return currentXs;
    }

    public final double getTemperature() {
        return temperature;
    }
    public final boolean getIsAccepted() {
        return isAccepted;
    }

}