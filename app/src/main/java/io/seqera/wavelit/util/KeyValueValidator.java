package io.seqera.wavelit.util;

import picocli.CommandLine;

import java.util.Stack;

public class KeyValueValidator implements CommandLine.IParameterConsumer {
    @Override
    public void consumeParameters(Stack<String> args, CommandLine.Model.ArgSpec argSpec, CommandLine.Model.CommandSpec commandSpec) {
        String optionValue = args.pop();
        if (!optionValue.contains("=")) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(), "Option"+argSpec.getValue()+"requires values in the format key=value");
        }
        argSpec.setValue(optionValue);
    }
}

