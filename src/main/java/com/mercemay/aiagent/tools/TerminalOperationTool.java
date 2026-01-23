package com.mercemay.aiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TerminalOperationTool {

    @Tool(description = "Execute a terminal command and return the output.")
    public String executeCommand(@ToolParam(description = "The terminal command to execute.") String command) {
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
