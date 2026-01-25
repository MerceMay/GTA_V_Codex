package com.mercemay.aiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TerminalOperationTool {

    @Tool(description = """
            Executes a shell command on the host operating system.
            
            CRITICAL USAGE GUIDELINES:
            - Use this tool to run system commands, build projects, run tests, or manage files via CLI.
            - You are responsible for the SAFETY of the command.
            - Detect the OS (Windows/Linux) and format commands accordingly (e.g., 'dir' vs 'ls').
            - Do NOT run commands that require interactive user input (stdin is not supported).
            - Do NOT run long-running processes (like starting a server) without a way to detach or timeout, as this tool waits for completion.
            
            Returns:
            - Combined Standard Output (stdout) and Standard Error (stderr).
            """)
    public String executeCommand(@ToolParam(description = "The shell command to execute (e.g., 'mvn clean install').") String command) {
        StringBuilder output = new StringBuilder();
        String os = System.getProperty("os.name").toLowerCase();

        List<String> fullCommand = new ArrayList<>();

        if (os.contains("win")) {
            fullCommand.add("cmd.exe");
            fullCommand.add("/c");
        } else {
            fullCommand.add("/bin/sh");
            fullCommand.add("-c");
        }
        fullCommand.add(command);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);
            processBuilder.redirectErrorStream(true); // redirect error stream to output stream
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.insert(0, "[Command exited with code " + exitCode + "]" + System.lineSeparator());
            }
        } catch (IOException | InterruptedException e) {
            output.append("Error executing command: ").append(e.getMessage());
        }
        return output.toString().trim();
    }
}
