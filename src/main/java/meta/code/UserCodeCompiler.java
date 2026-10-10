package meta.code;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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

import meta.ObjectiveFunction;

/**
 * 把用户提交的 Java 源码在内存中编译并加载为 {@link ObjectiveFunction}。
 *
 * <h3>为什么用内存编译</h3>
 * 用户每点一次「开始搜索」就要编译一次。若落盘会产生临时文件、并发命名冲突
 * 与清理负担。借助 JDK 自带的 {@code javax.tools}，把源码包成字符串对象、
 * 把字节码收进内存 Map，全流程不触碰文件系统。
 *
 * <h3>类名约定</h3>
 * 用户代码的类名必须是 {@value #CLASS_NAME}，且不得写 {@code package} 声明。
 * 固定类名让加载环节不必从源码里猜类名（正则提取类名对嵌套类、注释都很脆弱）。
 *
 * <h3>错误信息面向用户</h3>
 * 编译失败时抛出 {@link CompilationException}，消息里带「第 N 行」。
 * 用户看到的就是 javac 的原始诊断，不带行号几乎无法定位。
 */
public final class UserCodeCompiler {

    /** 用户代码必须使用的类名。 */
    public static final String CLASS_NAME = "UserObjective";

    private UserCodeCompiler() {
    }

    /**
     * 编译并实例化用户代码。
     *
     * @param code 完整的 Java 源码，类名须为 {@value #CLASS_NAME} 且无 package 声明
     * @return 用户代码的实例
     * @throws CompilationException 编译失败、类名不符、未实现接口等
     */
    public static ObjectiveFunction compile(String code) throws CompilationException {
        if (code == null || code.isBlank()) {
            throw new CompilationException("代码为空。");
        }
        // 显式拦截，否则用户会收到一堆莫名其妙的「找不到符号」
        if (code.contains("package ")) {
            throw new CompilationException("请去掉 package 声明——本服务约定类名直接为 "
                    + CLASS_NAME + "，不带包名。");
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new CompilationException(
                    "当前运行环境没有 Java 编译器。需要用 JDK 运行本服务（JRE 不含编译器）。");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager standard =
                compiler.getStandardFileManager(diagnostics, Locale.getDefault(), StandardCharsets.UTF_8);
        MemoryFileManager manager = new MemoryFileManager(standard);

        List<String> options = List.of(
                // 继承本进程的 classpath（构建脚本传的是 bin），用户代码才能 import meta.ObjectiveFunction
                "-classpath", System.getProperty("java.class.path", ""),
                "-proc:none",   // 不做注解处理，避免无关警告淹没用户的真实错误
                "-nowarn");

        JavaCompiler.CompilationTask task = compiler.getTask(
                null, manager, diagnostics, options, null,
                List.of(new StringSource(CLASS_NAME, code)));

        boolean ok = task.call();

        if (hasErrors(diagnostics) || !ok) {
            throw new CompilationException(formatErrors(diagnostics));
        }

        byte[] bytecode = manager.bytecodeOf(CLASS_NAME);
        if (bytecode == null) {
            throw new CompilationException("编译未产生 " + CLASS_NAME
                    + " 类。请确认类名是 " + CLASS_NAME + "，且没有 package 声明。");
        }

        return instantiate(bytecode);
    }

    private static ObjectiveFunction instantiate(byte[] bytecode) throws CompilationException {
        ClassLoader loader = new ClassLoader(UserCodeCompiler.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (CLASS_NAME.equals(name)) {
                    return defineClass(name, bytecode, 0, bytecode.length);
                }
                return super.findClass(name);
            }
        };

        try {
            Class<?> clazz = Class.forName(CLASS_NAME, true, loader);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (!(instance instanceof ObjectiveFunction)) {
                throw new CompilationException("类 " + CLASS_NAME
                        + " 没有实现 meta.ObjectiveFunction 接口。");
            }
            return (ObjectiveFunction) instance;
        } catch (CompilationException e) {
            throw e;
        } catch (NoSuchMethodException e) {
            throw new CompilationException("类 " + CLASS_NAME
                    + " 缺少公开的无参构造函数。请不要自定义构造函数。");
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new CompilationException("加载类 " + CLASS_NAME + " 失败：" + e);
        }
    }

    private static boolean hasErrors(DiagnosticCollector<JavaFileObject> diagnostics) {
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                return true;
            }
        }
        return false;
    }

    private static String formatErrors(DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder sb = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            if (d.getKind() != Diagnostic.Kind.ERROR) {
                continue;
            }
            sb.append("第 ").append(d.getLineNumber()).append(" 行：")
                    .append(d.getMessage(Locale.getDefault()))
                    .append('\n');
        }
        if (sb.length() == 0) {
            return "编译失败（编译器未给出具体错误信息）。";
        }
        return sb.toString().stripTrailing();
    }

    /** 把源码字符串包装成编译器认识的「源文件」。 */
    private static final class StringSource extends SimpleJavaFileObject {
        private final String code;

        StringSource(String className, String code) {
            super(URI.create("string:///" + className + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    /** 把编译产物（字节码）收进内存，而非写文件。 */
    private static final class ByteCode extends SimpleJavaFileObject {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        ByteCode(String className, Kind kind) {
            super(URI.create("bytes:///" + className + kind.extension), kind);
        }

        @Override
        public OutputStream openOutputStream() {
            return out;
        }

        byte[] bytes() {
            return out.toByteArray();
        }
    }

    /** 拦截编译输出，把 class 字节留给调用方。 */
    private static final class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, ByteCode> outputs = new HashMap<>();

        MemoryFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className,
                JavaFileObject.Kind kind, FileObject sibling) {
            ByteCode byteCode = new ByteCode(className, kind);
            outputs.put(className, byteCode);
            return byteCode;
        }

        byte[] bytecodeOf(String className) {
            ByteCode byteCode = outputs.get(className);
            return byteCode == null ? null : byteCode.bytes();
        }
    }

    /** 用户代码编译失败。消息已面向用户格式化（含行号）。 */
    public static final class CompilationException extends Exception {
        private static final long serialVersionUID = 1L;

        public CompilationException(String message) {
            super(message);
        }
    }
}
