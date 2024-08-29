package net.vicp.biggee.aot.vpn.expressvpn.Dialer.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.concurrent.Executors;

public class RunShellTest {
    String bash = "/bin/sh";

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void run() {
    }

    @Test
    public void initCommand() throws IOException, InterruptedException {
        System.out.println(bash);
        Process started = new ProcessBuilder("bash").start();
        InputStream errorStream = started.getErrorStream();

        InputStream inputStream = started.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        OutputStream outputStream = started.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        Executors.newCachedThreadPool().execute(() -> reader.lines().forEach(System.out::println));

        writer.write("ls");
        writer.newLine();
        writer.flush();
//        outputStream = started.getOutputStream();
//        writer = new BufferedWriter(new OutputStreamWriter(outputStream));
//        reader.lines().forEach(System.out::println);

        writer.write("uname -a");
        writer.newLine();
        writer.flush();

        writer.write("ping -c 10 192.168.0.1");
        writer.newLine();
        writer.flush();

        writer.write("exit");
        writer.newLine();
        writer.flush();
//        outputStream = started.getOutputStream();
//        writer = new BufferedWriter(new OutputStreamWriter(outputStream));
//        reader.lines().forEach(System.out::println);

        started.waitFor();

    }
}