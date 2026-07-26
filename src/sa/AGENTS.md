# AI Agent 使用指南 — 模块化模拟退火（MSA）

本文档面向 AI Agent 和开发者，提供快速理解和扩展 MSA 框架的实用指南。

## 🎯 核心认知

MSA 是一个**模块化优化算法框架**，将模拟退火拆解为四个可替换组件：

| 组件 | 职责 | 调用时机 |
|------|------|----------|
| `SAInitializer` | 生成初始解和初始温度 | 算法启动时 |
| `SAPerturbation` | 从当前解生成候选解 | 每轮迭代 |
| `SACoolingSchedule` | 降低温度 | 每轮迭代后 |
| `SATerminationCondition` | 判断是否停止 | 每轮迭代后 |

## 📐 接口契约速查

### 泛型参数说明

所有组件和核心类使用三个泛型参数：

| 参数 | 含义 | 典型值 |
|------|------|--------|
| `X` | 解的表示类型 | `double[]`、`int[]`、自定义数据结构 |
| `Y` | 目标函数返回值类型 | `Double`（单目标）、`double[]`（多目标） |
| `Prob` | 问题类型，必须实现 `Problem<X, Y>` | `ContinuousProblem`、自定义问题类 |

示例：`SAPerturbation<double[], Double, ContinuousProblem>` 表示一个处理连续空间、单目标优化的扰动器。

### 组件通用模式

所有组件遵循相同的生命周期：

```
构造 → init(problem, random) → 核心方法被反复调用 → (可选) reset() → 重新使用
```

### 关键方法签名

```java
// 初始化器
X initialX();
double initialTemperature();

// 扰动器
X perturb(SAState<X> state);

// 冷却策略
double cool(SAState<X> state);

// 终止条件
boolean check(SAState<X> state);
```

### SAState 提供的信息

```java
state.currentX()      // 当前解（只读）
state.temperature()   // 当前温度
state.isAccepted()    // 上一轮是否接受新解（见下方冷启动说明）
```

**冷启动细节**：
- `perturb()` 和 `check()` 的首次调用中，`isAccepted` 为 `false`（表示"尚无历史"）
- `cool()` 的首次调用发生在第一轮迭代**之后**，此时 `isAccepted` 已是 Metropolis 准则的真实结果，**不是**默认 `false`

## 🔑 最少信息原则

组件接口**只传递无法自行推导的信息**：

- ❌ 不传递目标函数值 → 组件可通过 `Problem.evaluate()` 获取
- ❌ 不传递是否改进 → 组件可通过 `Problem.compare()` 比较
- ❌ 不传递迭代次数 → 组件内部维护计数器

- ✅ 传递 `currentX`、`temperature`、`isAccepted`

## 🛠️ 常见任务

### 任务一：定义新问题

```java
public class MyProblem extends ContinuousProblem {
    public MyProblem(int dimension) {
        super(createBounds(dimension, -100), createBounds(dimension, 100));
    }

    @Override
    public Double evaluate(double[] x) {
        // 实现目标函数（必须是纯函数）
        double sum = 0.0;
        for (double v : x) sum += v * v;
        return sum;
    }

    @Override
    public double[] copyX(double[] x) {
        return x.clone();
    }

    private static double[] createBounds(int dim, double value) {
        double[] bounds = new double[dim];
        java.util.Arrays.fill(bounds, value);
        return bounds;
    }
}
```

### 任务二：自定义扰动器

```java
public class GaussianPerturbation extends SAPerturbation<double[], Double, ContinuousProblem> {
    private ContinuousProblem problem;
    private Random random;

    @Override
    protected void init(ContinuousProblem problem, Random random) {
        this.problem = problem;
        this.random = random;
    }

    @Override
    protected double[] perturb(SAState<double[]> state) {
        double[] x = state.currentX();
        double[] newX = problem.copyX(x);
        double temperature = state.temperature();

        for (int i = 0; i < newX.length; i++) {
            newX[i] += random.nextGaussian() * temperature * 0.1;
        }
        return newX;
    }
}
```

### 任务三：自适应冷却策略

```java
public class AdaptiveCooling extends SACoolingSchedule<double[], Double, ContinuousProblem> {
    private double baseRate;
    private int acceptedCount;
    private int totalCalls;

    public AdaptiveCooling(double baseRate) {
        this.baseRate = baseRate;
        this.acceptedCount = 0;
        this.totalCalls = 0;
    }

    @Override
    protected void init(ContinuousProblem problem, Random random) {}

    @Override
    protected double cool(SAState<double[]> state) {
        totalCalls++;
        if (state.isAccepted()) acceptedCount++;

        double rate = baseRate;
        if (totalCalls > 100) {
            double acceptRate = (double) acceptedCount / totalCalls;
            rate = acceptRate > 0.5 ? 0.95 : 0.99;
        }
        return state.temperature() * rate;
    }
}
```

### 任务四：运行优化

```java
MyProblem problem = new MyProblem(2);
SimulatedAnnealing<double[], Double> sa =
    new SimulatedAnnealing<>(
        problem,
        new SABasicInitializer(100),
        new SABasicPerturbation(),
        new SABasicCoolingSchedule(0.99, 100),
        new SABasicTerminationCondition(10000)
    );

BestRecorder<double[], Double> recorder = new BestRecorder<>(problem);
sa.solve(recorder);
System.out.println("最优值: " + recorder.getBestY());
```

### 任务五：可复现结果

```java
SimulatedAnnealing<double[], Double> sa =
    new SimulatedAnnealing<>(
        new Random(42),  // 固定种子
        problem,
        new SABasicInitializer(100),
        new SABasicPerturbation(),
        new SABasicCoolingSchedule(0.99, 100),
        new SABasicTerminationCondition(10000)
    );
```

## ⚠️ 关键约束

| 约束 | 说明 |
|------|------|
| 冷启动 | 详见上方 SAState 冷启动说明 |
| 不可变性 | 不得原地修改 `state.currentX()`，必须返回新对象 |
| 纯函数 | `evaluate()` 必须是纯函数，相同输入 -> 相同输出，且每次返回的 `Y` 都必须是独立的新对象 |
| 随机数 | 使用注入的 `Random`，不得自行创建 |
| 线程安全 | 框架单线程运行，组件内部状态需自行同步 |

## 📋 核心方法语义

### Problem.compare(X x1, X x2)

比较两个解对应的的目标值的优劣，返回带符号的差值以指示优劣程度。本方法内部可能会调用 `evaluate()` 获取目标值后进行对比：

| 返回值 | 含义 |
|--------|------|
| `> 0`（正值） | `x1` 优于 `x2` |
| `< 0`（负值） | `x1` 劣于 `x2` |
| `= 0` | 两者等优 **或** 无法比较（无支配关系） |

- 返回值的**绝对值**表示优劣差距的大小，绝对值越大差距越显著
- 此方法**不保证**反对称性（即 `compare(A, B) != -compare(B, A)` 可能成立）
- 示例：最小化问题中 `compare(x1, x2) = evaluate(x2) - evaluate(x1)`，当 `evaluate(x1) < evaluate(x2)` 时返回正值

### SATerminationCondition.check(SAState state)

判断算法是否应当终止：

| 返回值 | 含义 |
|--------|------|
| `true` | 满足终止条件，算法将停止迭代 |
| `false` | 继续迭代 |

- 首次调用时 `state.isAccepted()` 为 `false`（冷启动），不应据此决定是否终止
- 实现可通过内部计数器统计调用次数来推导迭代次数

## 📚 相关文件

| 文件 | 用途 |
|------|------|
| [README.md](README.md) | 模块详细文档 |
| [../../README.md](../../README.md) | 项目总览 |
| [../../QUICKSTART.md](../../QUICKSTART.md) | 快速开始 |
| `core/SimulatedAnnealing.java` | 主循环实现 |
| `core/SAState.java` | 状态封装 |
| `components/basiccomponents/` | 内置组件实现 |