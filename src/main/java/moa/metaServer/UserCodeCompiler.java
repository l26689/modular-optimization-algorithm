package moa.metaServer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * 把用户粘贴的 Java 代码<b>内存编译</b>成一个 {@link ObjectiveFunction} 实例，并做三道体检。
 * <p>
 * 用户代码约定：类名 {@code UserObjective}、无 {@code package} 声明、实现 {@link ObjectiveFunction}。
 * 全程不落磁盘（编译产物放在内存字节数组里），每次点搜索都重新编译一次。
 *
 * <h3>三道体检（沿旧网页设计，防呆）</h3>
 * <ol>
 *   <li><b>编译校验</b>：语法/类型错误 → 报「第 N 行」；</li>
 *   <li><b>运行时探针</b>：用全下界 / 全上界 / 中点三个解调 {@code evaluate}，
 *       要求不抛异常、返回有限值（抓到"除以零、越界、返回 NaN"这类问题）；</li>
 *   <li><b>纯函数</b>：中点解连调两次，要求结果一致——专抓 {@code Math.random()}
 *       之类会让搜索悄悄失效的写法（它们不报错，只是让结果全是噪声）。</li>
 * </ol>
 */
public final class UserCodeCompiler {

    private UserCodeCompiler() {
    }

    /** 约定好的类名。 */
    public static final String CLASS_NAME = "UserObjective";

    /**
     * 编译 + 加载 + 体检，返回可用的目标函数。
     *
     * @param source 用户粘贴的完整 Java 源码
     * @param dim    维度（探针要用）
     * @param lower  各维下界（探针要用）
     * @param upper  各维上界（探针要用）
     * @return 编译并体检通过的目标函数
     * @throws IllegalArgumentException 编译失败或体检不通过（消息已带行号/原因）
     */
    public static ObjectiveFunction compileAndCheck(
            String source, int dim, double[] lower, double[] upper) {

        Map<String, byte[]> bytecode = compile(source);
        ObjectiveFunction fn = load(bytecode);
        checkProbes(fn, dim, lower, upper);
        return fn;
    }

    // =====================================================================
    // 第一道：编译
    // =====================================================================

    private static Map<String, byte[]> compile(String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalArgumentException("当前不是 JDK 环境（找不到系统编译器），无法动态编译用户代码");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager stdManager =
                compiler.getStandardFileManager(diagnostics, null, null);
        InMemoryFileManager manager = new InMemoryFileManager(stdManager);

        JavaFileObject sourceObj = new SourceFromString(CLASS_NAME, source);
        List<String> options = List.of(
                "-classpath", System.getProperty("java.class.path"),
                "-encoding", "UTF-8");

        boolean ok = compiler.getTask(
                null, manager, diagnostics, options, null, List.of(sourceObj)).call();

        if (!ok) {
            StringBuilder sb = new StringBuilder("编译失败：\n");
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                sb.append("第 ").append(d.getLineNumber())
                  .append(" 行: ").append(d.getMessage(Locale.ROOT))
                  .append('\n');
            }
            throw new IllegalArgumentException(sb.toString());
        }

        Map<String, byte[]> bytecode = manager.bytecode();
        if (!bytecode.containsKey(CLASS_NAME)) {
            throw new IllegalArgumentException(
                    "找不到类 " + CLASS_NAME + "。请确认：类名必须是 UserObjective，且不要写 package 声明。");
        }
        return bytecode;
    }

    // =====================================================================
    // 第二道：加载
    // =====================================================================

    private static ObjectiveFunction load(Map<String, byte[]> bytecode) {
        ByteArrayClassLoader loader =
                new ByteArrayClassLoader(bytecode, ObjectiveFunction.class.getClassLoader());
        try {
            Class<?> cls = loader.loadClass(CLASS_NAME);
            Object instance = cls.getDeclaredConstructor().newInstance();
            if (!(instance instanceof ObjectiveFunction)) {
                throw new IllegalArgumentException(
                        CLASS_NAME + " 没有实现 moa.metaServer.ObjectiveFunction 接口");
            }
            return (ObjectiveFunction) instance;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("实例化 " + CLASS_NAME + " 失败: " + e);
        }
    }

    // =====================================================================
    // 第三道：运行时体检
    // =====================================================================

    private static void checkProbes(ObjectiveFunction fn, int dim, double[] lower, double[] upper) {
        double[] probeLow = lower.clone();
        double[] probeHigh = upper.clone();
        double[] probeMid = new double[dim];
        for (int i = 0; i < dim; i++) {
            probeMid[i] = (lower[i] + upper[i]) / 2.0;
        }

        // 有限值 + 不抛异常
        for (double[] probe : new double[][]{probeLow, probeHigh, probeMid}) {
            double v;
            try {
                v = fn.evaluate(probe);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                        "目标函数在解 " + Arrays.toString(probe) + " 处抛异常: " + e);
            }
            if (!Double.isFinite(v)) {
                throw new IllegalArgumentException(
                        "目标函数在解 " + Arrays.toString(probe) + " 处返回非有限值: " + v);
            }
        }

        // 纯函数：同一输入连调两次必须一致
        double a = fn.evaluate(probeMid);
        double b = fn.evaluate(probeMid);
        if (Double.compare(a, b) != 0) {
            throw new IllegalArgumentException(
                    "目标函数不是纯函数：同一输入两次求值结果不同（" + a + " vs " + b
                    + "）。可能含 Math.random()、System.nanoTime() 或读全局可变状态。");
        }
    }

    // =====================================================================
    // 内存编译的支撑类型
    // =====================================================================

    /** 内存里的源码对象。 */
    private static final class SourceFromString extends SimpleJavaFileObject {
        private final String code;

        SourceFromString(String className, String code) {
            super(URI.create("string:///" + className + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    /** 内存里的 .class 输出对象，编译结果写进字节数组。 */
    private static final class ClassFileObject extends SimpleJavaFileObject {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        ClassFileObject(String className, Kind kind) {
            super(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind);
        }

        @Override
        public OutputStream openOutputStream() {
            return out;
        }

        byte[] bytes() {
            return out.toByteArray();
        }
    }

    /** 把编译输出收进内存的文件管理器。 */
    private static final class InMemoryFileManager
            extends ForwardingJavaFileManager<StandardJavaFileManager> {

        private final Map<String, ClassFileObject> outputs = new HashMap<>();

        InMemoryFileManager(StandardJavaFileManager delegate) {
            super(delegate);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(
                JavaFileManager.Location location, String className,
                JavaFileObject.Kind kind, FileObject sibling) {
            ClassFileObject obj = new ClassFileObject(className, kind);
            outputs.put(className, obj);
            return obj;
        }

        Map<String, byte[]> bytecode() {
            Map<String, byte[]> result = new HashMap<>();
            for (Map.Entry<String, ClassFileObject> e : outputs.entrySet()) {
                result.put(e.getKey(), e.getValue().bytes());
            }
            return result;
        }
    }

    /** 从内存字节数组定义类的加载器。 */
    private static final class ByteArrayClassLoader extends ClassLoader {
        private final Map<String, byte[]> bytecode;

        ByteArrayClassLoader(Map<String, byte[]> bytecode, ClassLoader parent) {
            super(parent);
            this.bytecode = bytecode;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = bytecode.get(name);
            if (bytes == null) {
                throw new ClassNotFoundException(name);
            }
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
