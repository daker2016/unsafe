package net.bramp.unsafe;

import org.openjdk.jmh.util.Utils;
import sun.management.VMManagement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class MemoryUtils {

    /**
     * @return memory usage according to Runtime
     */
    public static String memoryUsage() {
        final Runtime runtime = Runtime.getRuntime();

        runtime.gc();

        final long max = runtime.maxMemory();
        final long total = runtime.totalMemory();
        final long free = runtime.freeMemory();
        final long used = total - free;

        return String.format("%d\t%d\t%d\t%d", max, total, free, used);
    }

    /**
     * @return memory usage based on ps
     * @throws IOException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public static String pidMemoryUsage() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
        return pidMemoryUsage(getPid());
    }

    /**
     * @return the pid of this process
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static int getPid() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        Field jvm = runtime.getClass().getDeclaredField("jvm");
        jvm.setAccessible(true);

        VMManagement mgmt = (VMManagement) jvm.get(runtime);
        Method pid_method = mgmt.getClass().getDeclaredMethod("getProcessId");
        pid_method.setAccessible(true);

        return (Integer) pid_method.invoke(mgmt);
    }


    public static String pidMemoryUsage(int pid) throws IOException {
        Process p = new ProcessBuilder()
            .command("ps", "-o", "pid,rss,vsz", "-p", Long.toString(pid))
            .start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        reader.readLine(); // Skip header

        String line = reader.readLine();
        String[] parts = line.trim().split("\\s+", 3);

        int readPid = Integer.parseInt(parts[0]);
        if (pid != readPid) {
            throw new RuntimeException("`ps` returned something unexpected: '" + line + "'");
        }

        long rss = Long.parseLong(parts[1]) * 1024;
        long vsz = Long.parseLong(parts[2]);

        // 0 pid, 1 rss, 2 vsz
        return String.format("%d\t%d", rss, vsz);
    }

    public static List<String> buildJavaArg() {
        List<String> command = new ArrayList<String>();

        command.add(Utils.getCurrentJvm());
        command.add("-cp");

        if (Utils.isWindows()) {
            command.add('"' + System.getProperty("java.class.path") + '"');
        } else {
            command.add(System.getProperty("java.class.path"));
        }

        command.add(MemoryTest.class.getName());

        return command;
    }

}