# MOA 框架设计文档

## 1. 架构概览

MOA（Modular Optimization Algorithm）将优化算法拆解为可替换的组件。一个算法被视为**一组组件按固定顺序的装配**，而非一个封闭的黑盒。

### 目录结构

```
src/main/java/moa/
├── framework/      抽象层 —— 接口与抽象类，通用
│   ├── api/        SPI 组件契约（Component, Initializer, Recorder, ...）
│   ├── engine/     算法运行时抽象（OptimizationAlgorithm, State）
│   └── problem/    问题域定义（Problem, Evaluable）
│
├── components/     无约束的通用实现
│
├── problem/        问题定义 + 问题约束组件
│
├── algorithm/      算法模块
│   ├── sa/  api / components / engine
│   ├── pso/ api / components / engine
│   ├── ga/  api / components / engine
│   └── ……
│
├── example/        演示用法
└── benchmark/      实验脚本
```

### 算法模块内部统一三层结构

| 层 | 职责 | final |
|---|---|---|
| `api/` | 该算法的专属接口（契约） | 否 |
| `components/` | 该算法的内置组件实现（零件） | 是 |
| `engine/` | 算法主类 + State 实现（引擎） | 是 |

## 2. 架构的抽象

框架将优化算法抽象为**一组按某种顺序协作的 SPI 接口**。每个接口定义算法流程中的一个独立职责。

### 接口清单

| 接口 | 方法 | 职责 |
|---|---|---|
| `Component` | `init(Prob, Random)` | 基契约：绑定问题上下文和随机源 |
| `Initializer` | `initializeX()` | 搜索起点：生成初始解 |
| `SearchOperator` | `search(State)` | 搜索步进：执行一次状态转移 |
| `Recorder` | `record(State)` | 过程记录：保存或输出算法状态 |
| `TerminationCondition` | `check()` | 搜索终点：判定是否停止 |
| `Reusable` | `reset()` | 回到可用状态：支持多轮运行 |

### 组合顺序

```
init → initializeX → [循环: search → record → check] → ( reset → 下一轮 )
```

- **init** 在 `solve()` 入口由引擎统一调用，为所有组件注入 Problem 和 Random
- **initializeX** 仅在优化开始时调用一次
- **search → record → check** 构成主循环，具体调用频率由引擎决定
- **reset** 清除累积的内部状态，使组件回到可用状态

所有具体算法（SA、PSO、GA、DE）都是对上述接口的不同装配方式——算法差异体现为组件实现的不同，而非接口的不同。

---

## 3. 泛型约束系统

### 设计目的

用泛型在编译期表达组件之间的兼容性依赖——接口签名即约束声明，不需要运行时 `instanceof` 检查。

### 三个通用参数

```
Component<X, Prob extends Problem<X>, S extends State<X>>
```

| 参数 | 约束 | 含义 |
|---|---|---|
| `X` | — | 解的表示类型（例如 `double[]`） |
| `Prob` | `extends Problem<X>` | 问题类型 |
| `S` | `extends State<X>` | 状态类型 |

算法模块可通过自己的 API 接口**进一步收窄**这些约束。例如 `SAInitializer<X, Prob>` 要求 `S = SAState<X>`，比框架层的 `State<X>` 更具体。

### PECS 与通配符

引擎是组件数据的**生产者**（向组件写入 Problem 和 State），组件是**消费者**。因此使用下界通配符（`? super`）：

```java
// 引擎 solve() 签名
void solve(Recorder<X, ? super Prob, ? super S> recorder);

// 组件注入
SAInitializer<X, ? super Prob> initializer;
SacoolingSchedule<X, ? super Prob, ? super S> cooling;
```

效果：一个声明了 `Recorder<double[], Problem<double[]>, State<double[]>>` 的通用 Recorder，可以被以 `ContinuousProblem` 构造的 SA 实例使用——因为 `Problem<double[]>` 是 `ContinuousProblem` 的父类型。

### Javadoc 中的参数绑定

组件类的 Javadoc 应直接写出其参数的具体绑定，让用户一眼看懂适用场景：

```
实现 {@link SearchOperator}{@code <X, Prob:Problem<X>, S:SAState<X>>}。
```

| 写法 | 含义 |
|---|---|
| `X` | 无约束 |
| `X:double[]` | 绑定为具体类型 |
| `Prob:Problem<X>` | 框架级约束（未收窄） |
| `Prob:ContinuousProblem` | 收窄为具体问题类型 |
| `S:SAState<X>` | 收窄为具体算法状态 |

详细规则与示例见 [§7 Javadoc 规范](#7-javadoc-规范)。

### final 约定


> 在此项目中，`final` 的语义是"**这是一个实函数（concrete implementation），不是等待被实现的抽象契约**"。


## 4. 组件生命周期

### 三阶段模型

```
构造(纯参数) → init(Prob, Random) → 业务方法(反复调用) → reset()
```

| 阶段 | 调用者 | 说明 |
|---|---|---|
| 构造 | 用户 | 纯参数，不绑定 Problem/Random。此时组件处于**未就绪**状态 |
| init | 引擎 `solve()` 入口 | 绑定问题上下文和随机源。此后组件**就绪** |
| 业务方法 | 引擎迭代中 | 取决于组件角色（search/record/check 等），反复调用 |
| reset | 用户/引擎 | 清除累积的内部状态，组件回到**未就绪**状态 |

### reset 的语义

**回到可以使用的状态。** reset 清除累积数据（计数器、缓存、滑动窗口等），并等待下一次被组装。

问题和随机源的重新绑定由引擎在下一轮 `solve()` 时**自动调用 `init()`** 完成——引擎持有 Problem 和 Random 的完全控制权，reset 无需也不应该关心绑定层面的事。

```java
// 典型 reset 实现
@Override
public void reset() {
    this.callCount = 0;        // 清空计数器
    this.bestX = null;         // 丢弃历史
    this.history.clear();      // 清空缓存
    // 不重设 problem 和 random —— 引擎会在下一轮 solve() 时自动调用 init()
}
```

对于纯函数式（无内部状态）的组件，`reset()` 是空操作，但**仍应实现** `Reusable` 接口——这是对框架的统一承诺："此组件可安全复用"，外部调用者无需区分有状态与无状态。

---

## 5. State 与安全规则

### 数组模式

```
State.getCurrentXs() 返回 X[]
  单解算法（如SA）   → [currentX]
  群体算法（如PSO）  → [particle1, particle2, ...]
```

设计意图：统一单解和群体算法的访问方式。编写通用组件时使用 for-each 遍历，编写算法特化组件时可利用数组固定索引（如 `[0]`）。

### 唯一安全规则

> **对 State 数据进行"非读"操作前，必须深拷贝。**

任何组件保存 State 引用、保存数组引用、或保存解引用都是不安全的。除了保存以外，也应该注意搜索算子，如果需要长期持有，应当保证返回的解不会被修改，推荐直接copy产生深拷贝副本
| 操作 | 推荐做法 |
|---|---|
| 保存最优解 | `problem.copyX(x)` |
| 保存历史数据 | 深拷贝后存储，不引用原始数组 |
| 跨迭代持有 | 禁止直接持有，必须拷贝 |

```java
// 正确：深拷贝保存
this.bestX = problem.copyX(state.getCurrentXs()[0]);

// 错误：直接引用——State 被引擎复用后此引用即失效
this.bestX = state.getCurrentXs()[0];
```

---

## 6. 可复现性

### 单一随机源

```
引擎持有 Random → init(Prob, Random) 注入所有组件 → 组件内部不得自建 Random
```

- 所有组件的随机行为由同一 `Random` 实例控制
- 外部传入固定种子的 `Random` → 整个优化过程完全可复现
- 组件内调用 `Math.random()` 或 `new Random()` 将破坏可复现性

---

## 7. Javadoc 规范

### API 类（interface / abstract class）

```java
/**
 * [职责描述 —— 一句话说明该接口定义了什么能力]
 *
 * [详细描述 —— 业务语义、设计意图、与框架其他部分的关系]
 *
 * 提示
 *
 * [子项] —— [说明]
 *
 * 参见文档 §X：[简单引用]
 */
```

> API 类本身即契约，无需写"契约描述"和"是否为最终类"。

### 非 API 类（component / engine）

```java
/**
 * [职责描述 —— 一句话]
 *
 * 实现/继承 {@link 父类或接口}{@code <X:..., Prob:..., S:...>}。
 * 是否为最终类：是/否。
 *
 * [详细描述 —— 使用场景、核心逻辑、与其他组件的关系]
 *
 * 提示
 *
 * [子项] —— [说明]
 *
 * 参见文档 §X：[引用]
 */
```

### 参数绑定格式

直接写出 Component 参数的最终绑定，用 `:` 分隔参数名与约束：

```
{@code <X:double[], Prob:ContinuousProblem, S:SAState<double[]>>}
```

约定如下：

| 写法 | 含义 |
|---|---|
| `X` | 无约束（用户自由决定） |
| `X:double[]` | 绑定为具体类型 |
| `Prob:Problem<X>` | 框架级约束（未收窄） |
| `Prob:ContinuousProblem` | 收窄为具体问题类型 |
| `S:SAState<X>` | 收窄为具体算法状态 |

泛型参数规则的完整说明见本文档 §3。

### 提示子项

提示后的子项按需取用，两个换行分隔：

| 子项 | 内容 |
|---|---|
| 安全性 | 线程安全、引用持有、防御性拷贝要求 |
| 可复现 | 随机数使用约定 |
| 组装说明 | 引擎需要的组件插槽及注入顺序（引擎类） |
| 已知限制 | 当前版本的已知缺陷或暂不支持的功能 |
| 性能 | 时间复杂度、内存占用（当有非平凡开销时） |

通用规范（如生命周期、PECS 规则）不应在 Javadoc 中完整复制，用"参见文档 §X"引用即可。
