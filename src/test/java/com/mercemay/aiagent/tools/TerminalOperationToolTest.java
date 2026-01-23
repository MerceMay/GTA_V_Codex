package com.mercemay.aiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest
public class TerminalOperationToolTest {

    @Test
    public void executeCommand() {
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        String command = "echo Hello, World!";
        String output = terminalOperationTool.executeCommand(command);
        System.out.println("Command output: " + output);
        assertEquals("Hello, World!", output);
    }
}
