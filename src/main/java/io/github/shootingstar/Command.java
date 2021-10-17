package io.github.shootingstar;

import java.util.*;

public class Command {

    private String executable;

    private List<String> arguments = List.of();

    public List<String> toList() {
        var list = new ArrayList<String>(arguments.size() + 1);
        list.add(executable);
        list.addAll(arguments);
        return list;
    }

    public String getExecutable() {
        return this.executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public List<String> getArguments() {
        return this.arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    public String toString() {
        return "{ executable='" + getExecutable() + "', arguments='" + getArguments() + "'}";
    }
}