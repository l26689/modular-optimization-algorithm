package meta;

/**
 * 配置编解码器 —— 在「算法组合」与「连续解向量」之间转换。
 * <p>
 * 元优化层用模拟退火去搜索"最优算法组合"，而模拟退火只认识连续向量
 * （见 {@link oa.api.problem.Problem}），因此需要把配置编码成一个
 * 固定长度的 {@code double[]} 供其搜索，并在评估时解码回真实组件。
 *
 * <h3>编码方案：归一化 + 固定预留</h3>
 * 所有维度统一归一化到 <b>[0, 1]</b>，这样元层的扰动算子在各个维度上
 * 尺度一致，一个扰动幅度即可通吃（若直接用原始尺度，温度维度跨度 1000、
 * 类型维度跨度 1，同一个 σ 会完全失效）。
 * <p>
 * {@link #decode} 负责把 [0,1] 映射到实际的参数取值域。
 *
 * <h3>固定预留而非变长编码</h3>
 * 各组件参数个数不同（如"分段冷却"需要步长、"几何冷却"不需要），
 * 本实现选择为每个槽位<b>固定预留</b>维度，未使用的维度被忽略。
 * 这浪费少量搜索空间，但实现简单、无变长编码的解析复杂度，是刻意的取舍。
 *
 * <h3>量化</h3>
 * 每个维度先量化到 1000 级网格再解码，使近邻配置归并为同一个
 * {@link Config} 对象。这既保证了缓存的命中率（{@link Config} 是 record，
 * 用值相等性做缓存键），也让最终排名表不至于被高度相似的重复项淹没。
 *
 * <h3>如何扩展</h3>
 * 新增可搜索的组件时：在 {@link ComponentRegistry} 登记实现，
 * 并在此处增加一个索引常量与对应的解码分支。
 */
public final class ConfigCodec {

    /** 配置向量的维度（= 可搜索参数个数） */
    public static final int DIM = 6;

    // ---- 配置向量各槽位的索引 ----
    /** [0] 初始温度（对数均匀） */
    public static final int I_TEMP = 0;
    /** [1] 冷却策略类型（离散） */
    public static final int I_COOLING_TYPE = 1;
    /** [2] 冷却率 */
    public static final int I_COOLING_RATE = 2;
    /** [3] 分段冷却的步长（仅类型 1 使用） */
    public static final int I_STEP_SIZE = 3;
    /** [4] 扰动器类型（离散） */
    public static final int I_PERTURBATION_TYPE = 4;
    /** [5] 高斯扰动的标准差比例（仅类型 1 使用） */
    public static final int I_PERTURBATION_SCALE = 5;

    // ---- 各参数的取值域 ----
    private static final double TEMP_MIN = 0.01;
    private static final double TEMP_MAX = 1000.0;
    private static final double RATE_MIN = 0.90;
    private static final double RATE_MAX = 0.9999;
    private static final int STEP_MIN = 1;
    private static final int STEP_MAX = 200;
    private static final double PERT_SCALE_MIN = 0.005;
    private static final double PERT_SCALE_MAX = 0.5;

    /** 每个维度的量化级数 */
    private static final int QUANT_LEVELS = 1000;

    private ConfigCodec() {
    }

    /**
     * 一个具体的算法组合。
     * <p>
     * 作为 {@code record}，其相等性基于字段值，可直接用作缓存键。
     * 注意：所有字段都经过了 {@link #decode} 的量化，因此两个数值上
     * 极为接近的原始向量会解码为同一个 {@code Config}。
     *
     * @param initialTemp       模拟退火初始温度
     * @param coolingType       冷却策略类型（见 {@link ComponentRegistry}）
     * @param coolingRate       冷却率
     * @param stepSize          分段冷却的步长（每多少次调用降温一次）
     * @param perturbationType  扰动器类型（见 {@link ComponentRegistry}）
     * @param perturbationScale 高斯扰动的标准差比例；均匀扰动器忽略此参数
     */
    public record Config(
            double initialTemp,
            int coolingType,
            double coolingRate,
            int stepSize,
            int perturbationType,
            double perturbationScale) {

        /** 人类可读的描述，用于排名表展示。 */
        public String describe() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("温度=%.3f", initialTemp));
            sb.append(" | ").append(ComponentRegistry.coolingName(coolingType));
            if (coolingType == ComponentRegistry.COOLING_STEPPED) {
                sb.append("(率=").append(String.format("%.4f", coolingRate))
                        .append(", 步长=").append(stepSize).append(')');
            } else {
                sb.append("(率=").append(String.format("%.4f", coolingRate)).append(')');
            }
            sb.append(" | ").append(ComponentRegistry.perturbationName(perturbationType));
            if (perturbationType == ComponentRegistry.PERTURBATION_GAUSSIAN) {
                sb.append("(σ=").append(String.format("%.4f", perturbationScale)).append(')');
            }
            return sb.toString();
        }
    }

    /**
     * 把任意连续向量解码为一个具体的算法组合。
     * <p>
     * 输入会被裁剪到 [0,1] 并量化，因此同一个配置可能对应多个原始向量。
     *
     * @param raw 原始配置向量，长度须为 {@link #DIM}
     * @return 解码并量化后的具体组合
     * @throws IllegalArgumentException 长度不符
     */
    public static Config decode(double[] raw) {
        if (raw == null || raw.length != DIM) {
            throw new IllegalArgumentException(
                    "配置向量长度必须为 " + DIM + "，实际为 "
                            + (raw == null ? "null" : raw.length));
        }

        double uTemp = quantize(raw[I_TEMP]);
        double uCool = quantize(raw[I_COOLING_TYPE]);
        double uRate = quantize(raw[I_COOLING_RATE]);
        double uStep = quantize(raw[I_STEP_SIZE]);
        double uPert = quantize(raw[I_PERTURBATION_TYPE]);
        double uScale = quantize(raw[I_PERTURBATION_SCALE]);

        // 温度采用对数均匀分布：低温区与高温区获得同等的搜索分辨率
        double initialTemp = TEMP_MIN * Math.pow(TEMP_MAX / TEMP_MIN, uTemp);
        int coolingType = uCool < 0.5 ? ComponentRegistry.COOLING_GEOMETRIC
                : ComponentRegistry.COOLING_STEPPED;
        double coolingRate = RATE_MIN + uRate * (RATE_MAX - RATE_MIN);
        int stepSize = STEP_MIN + (int) Math.round(uStep * (STEP_MAX - STEP_MIN));
        int perturbationType = uPert < 0.5 ? ComponentRegistry.PERTURBATION_UNIFORM
                : ComponentRegistry.PERTURBATION_GAUSSIAN;
        double perturbationScale = PERT_SCALE_MIN + uScale * (PERT_SCALE_MAX - PERT_SCALE_MIN);

        return new Config(initialTemp, coolingType, coolingRate, stepSize,
                perturbationType, perturbationScale);
    }

    /**
     * 返回一个合理的默认配置（用于对照展示，通常取线性冷却 + 均匀扰动）。
     *
     * @return 默认组合
     */
    public static Config defaultConfig() {
        return decode(new double[]{0.5, 0.25, 0.96, 0.0, 0.25, 0.0});
    }

    /** 裁剪到 [0,1] 并量化到 {@link #QUANT_LEVELS} 级网格。 */
    private static double quantize(double v) {
        if (Double.isNaN(v)) {
            return 0.0;
        }
        double clamped = v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
        return Math.round(clamped * (QUANT_LEVELS - 1)) / (double) (QUANT_LEVELS - 1);
    }
}
