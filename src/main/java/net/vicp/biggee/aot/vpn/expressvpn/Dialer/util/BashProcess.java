package net.vicp.biggee.aot.vpn.expressvpn.Dialer.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class BashProcess extends Process {
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    private static String bash = "/usr/bin/sh"; //初始化调用which语句
    private final ExecutorService exec;
    private final Process process;
    private final OutputStream outputStream;
    private final InputStream inputStream;
    private final InputStream errorStream;
    private final BufferedWriter cmdWriter;
    private final BufferedReader stdout;
    private final BufferedReader stderr;
    private final BlockingQueue<String> std = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> err = new LinkedBlockingQueue<>();
    private String checkString = UUID.randomUUID().toString();
    private volatile boolean ready = true;

    @SneakyThrows
    public BashProcess() {
        while (!Path.of(bash).toFile().isFile()) {
            Process started = new ProcessBuilder("which", "sh").start();
            bash = new BufferedReader(new InputStreamReader(started.getInputStream())).readLine();
        }
        process = new ProcessBuilder(bash).start();
        outputStream = process.getOutputStream();
        inputStream = process.getInputStream();
        errorStream = process.getErrorStream();
        cmdWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
        stdout = new BufferedReader(new InputStreamReader(inputStream));
        stderr = new BufferedReader(new InputStreamReader(errorStream));
        exec = Executors.newFixedThreadPool(2);
        //noinspection ResultOfMethodCallIgnored
        exec.execute(() -> stdout.lines().forEach(std::offer));
        //noinspection ResultOfMethodCallIgnored
        exec.execute(() -> stderr.lines().forEach(err::offer));
    }

    public static String initCommand(List<String> commands) {
        commands.removeIf(Objects::isNull);
        return String.join(" ", commands);
    }

    public static String initCommand(String... commands) {
        return Arrays.stream(commands)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    public static List<String> read(Queue<?> queue) {
        var list = new ArrayList<String>();

        int stdCnt = queue.size();
        for (int i = 0; i < stdCnt; i++) {
            String poll = String.valueOf(queue.poll());
            //noinspection LoggingSimilarMessage
            log.debug("stdout: {}", poll);
            list.add(poll);
        }
        return list;
    }

    public static List<String> peek(Queue<?> queue) {
        var list = new ArrayList<String>();

        int stdCnt = queue.size();
        for (int i = 0; i < stdCnt; i++) {
            String peek = String.valueOf(queue.peek());
            //noinspection LoggingSimilarMessage
            log.debug("stdout peek: {}", peek);
            list.add(peek);
        }
        return list;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public InputStream getErrorStream() {
        return errorStream;
    }

    @Override
    public int waitFor() throws InterruptedException {
        return process.waitFor();
    }

    @Override
    public int exitValue() {
        return process.exitValue();
    }

    @Override
    public void destroy() {
        process.destroy();
        exec.shutdown();
    }

    public String done() throws IOException {
        String check;
        if (ready) {
            check = UUID.randomUUID().toString();
        } else {
            check = checkString;
        }
        checkString = check;
        var cmd = initCommand("echo", check);
        cmdWriter.write(cmd);
        cmdWriter.newLine();
        cmdWriter.flush();
        return check;
    }

    public synchronized boolean ready() {
        if (ready) {
            return true;
        } else if (std.isEmpty()) {
            ready = true;
            return true;
        }
        return Stream.of(std, err)
                .anyMatch(q -> {
                    for (int i = 0; i < q.size(); i++) {
                        String line = q.peek();
                        if (line != null && line.contains(checkString)) {
                            ready = true;
                            return true;
                        }
                    }
                    return ready;
                });

    }

    public String run(String command) throws IOException {
        log.debug("run command: {}", command);
        cmdWriter.write(command);
        cmdWriter.newLine();
        cmdWriter.flush();
        return done();
    }

    @SuppressWarnings("unused")
    public synchronized List<String> runSync(String... commands) {
        return runSync(initCommand(commands));
    }

    public synchronized List<String> runSync(List<String> commands) {
        return runSync(initCommand(commands));
    }

    public synchronized List<String> runSync(String command) {
        var list = new ArrayList<>(readAll());
        String check;
        try {
            check = run(command);
            ready = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            String take = std.poll(5, TimeUnit.SECONDS);
            Thread.sleep(1000);
            list.add(take);
        } catch (InterruptedException e) {
            log.warn("read error", e);
        }

        List<String> newReturns = readAll();
        while (!newReturns.isEmpty()) {
            list.addAll(newReturns);
            try {
                //noinspection BusyWait
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.warn("read wait error", e);
            }
            newReturns = readAll();
        }

        list.removeIf(Objects::isNull);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        if (!ready) {
            ready = list.stream().anyMatch(l -> l.contains(check));
        }
        log.info("run command output ready {} : {}", ready, list);
        list.removeIf(check::equals);
        return list;
    }

    public List<String> readAll() {
        var list = new ArrayList<String>();
        list.addAll(read(std));
        list.addAll(read(err));
        return list;
    }

    public List<String> peekAll() {
        var list = new ArrayList<String>();
        list.addAll(peek(std));
        list.addAll(peek(err));
        return list;
    }

    public boolean isAlive() {
        ready = false;
        String check = checkString;
        String take = "";

        var echo = new ArrayList<>(readAll());
        try {
            check = run(initCommand("echo", check));
            take = std.poll(5, TimeUnit.SECONDS);
            Thread.sleep(1000);
            echo.add(take);
        } catch (InterruptedException | IOException e) {
            log.warn("read error", e);
        }

        if (String.valueOf(take).contains(check)) {
            readAll();
            ready = true;
            return true;
        }

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.warn("Alive check wait error", e);
        }
        echo.addAll(peekAll());
        echo.removeIf(Objects::isNull);
        String finalCheck = check;
        ready = echo.stream().anyMatch(l -> l.contains(finalCheck));
        return ready;
    }
}
