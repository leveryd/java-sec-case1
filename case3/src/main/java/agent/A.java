package agent;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.HashSet;

public class A {
    static HashSet<String> strings = new HashSet<>();

    public boolean equals(Object o){
        try{
            //strings.add(new String((byte[])o));
            String x = Base64.getEncoder().encodeToString((byte[])o) +  "\n\n" + printStackTraceAsString();
            strings.add(filter(x));
            //System.out.println("[HOOK] " + new String((byte[])o));
            //System.out.println("[HOOK] " + Base64.getEncoder().encodeToString(((byte[])o)));

        } catch (Exception e){
            System.out.println(e.getMessage());
        }
        return false;
    }

    public String filter(String x){
        if (x.contains("SwappedDataOutputStream")) {
            return x;
        }
        return "";
    }

    public static String printStackTraceAsString() {
        // 获取当前线程的调用堆栈
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        // 创建 StringBuilder 用于构建字符串
        StringBuilder stackTraceBuilder = new StringBuilder();

        // 遍历堆栈元素并添加到 StringBuilder
        for (StackTraceElement element : stackTraceElements) {
            stackTraceBuilder.append(element.toString());
            stackTraceBuilder.append(System.lineSeparator()); // 添加换行符
        }

        // 返回构建好的字符串
        return stackTraceBuilder.toString();
    }

    public void shutdown() {
        // shutdown过程中,strings可能被修改，遍历strings会异常
        HashSet<String> tmpStrings = (HashSet<String>) strings.clone();

        for(String s:tmpStrings){
            System.out.println("================");
            System.out.println(s);
        }

        try{
            File f = new File("/tmp/strings.txt");
            FileOutputStream fos = new FileOutputStream(f);
            for(String s:tmpStrings){
                fos.write(s.getBytes());
            }
        } catch (Exception e){
            System.out.println(e.getMessage());
        }

    }
}