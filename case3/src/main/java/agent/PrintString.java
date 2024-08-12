package agent;

import javassist.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class PrintString {

    static String loadClassName="agent.A";

    // todo: 1、attach模式 2、支持传入参数
    public static void main(String[] args) throws Exception {
        Class.forName(PrintString.loadClassName).newInstance().equals("this is test string".getBytes());  // A实例化成功、调用equals成功
        Thread.currentThread().getContextClassLoader().loadClass(PrintString.loadClassName).newInstance().equals("this is test string".getBytes());
    }

    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        System.out.println("Agent Start...");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Executing shutdown hook...");
            // 执行清理操作
            new A().shutdown();
        }));

        System.setProperty("flag", "false");    // 避免递归调用的标志

        inst.addTransformer(new HookStringConstructor(), true);
        inst.retransformClasses(Class.forName("java.lang.String"));
    }

    static class HookStringConstructor implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            return transformClass(classfileBuffer, className);
        }

        private byte[] transformClass(byte[] classfileBuffer, String className) {
            if (classfileBuffer == null || className == null || !className.endsWith("java.lang.String".replace(".", "/"))) {
                // 不是我们要hook的java.lang.String类
                return classfileBuffer;
            }

            try {
                ClassPool pool = ClassPool.getDefault();
                CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));
                hook(ctClass);
                return ctClass.toBytecode();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private void hook(CtClass ctClass) throws CannotCompileException, NotFoundException {
            CtConstructor[] constructors = ctClass.getDeclaredConstructors();

            for (CtConstructor constructor : constructors) {
                // 如果构造函数有参数
                if (constructor.getParameterTypes().length > 0) {
                    // java.lang.String 字符串值
                    // String insertCode = "{ if(System.getProperty(\"flag\") == \"false\") {System.setProperty(\"flag\", \"true\");System.out.println(\"Creating String: \" + new String(this.value));System.setProperty(\"flag\", \"false\"); }}";
                    // String insertCode = "{ if(System.getProperty(\"flag\") == \"false\") {System.setProperty(\"flag\", \"true\");System.out.println(\"Creating String: \" + java.util.Base64.getEncoder().encodeToString(this.value));System.setProperty(\"flag\", \"false\"); }}";

                    // 通过 java -cp 加载class 、 Class.forName生成实例
                    // String insertCode = "{ if(System.getProperty(\"flag\") == \"false\") {System.setProperty(\"flag\", \"true\");try{Class.forName(\"%s\").newInstance().equals(this.value);}catch (Exception e){System.out.println(e);};System.setProperty(\"flag\", \"false\"); }}";
                    String insertCode = "{ if(System.getProperty(\"flag\") == \"false\") {System.setProperty(\"flag\", \"true\");try{Thread.currentThread().getContextClassLoader().loadClass(\"%s\").newInstance().equals(this.value);}catch (Exception e){System.out.println(e);};System.setProperty(\"flag\", \"false\"); }}";

                    insertCode = insertCode.replace("%s", PrintString.loadClassName);
                    constructor.insertAfter(insertCode);
                }
            }
        }
    }
}
