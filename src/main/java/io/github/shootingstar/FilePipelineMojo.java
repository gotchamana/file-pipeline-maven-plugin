package io.github.shootingstar;

import static java.util.stream.Collectors.toList;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;

import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "pipeline", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class FilePipelineMojo extends AbstractMojo {

    @Parameter(property = "inputDirectory", required = true)
    private File inputDirectory;

    @Parameter(property = "outputDirectory", required = true)
    private File outputDirectory;

    @Parameter(property = "includeFiles", defaultValue = "**")
    private String includeFiles;

    @Parameter(property = "commands")
    private List<Command> commands = List.of();

    @Override
    public void execute() throws MojoExecutionException {
        try {
            checkParameters();

            var inputDir = inputDirectory.toPath().toAbsolutePath().normalize();
            var outputDir = outputDirectory.toPath().toAbsolutePath().normalize();

            var matcher = inputDir.getFileSystem().getPathMatcher("glob:" + includeFiles);
            BiPredicate<Path, BasicFileAttributes> predicate = (path, attrs) ->
                !attrs.isDirectory() && matcher.matches(inputDir.relativize(path));

            try (var stream = Files.find(inputDir, Integer.MAX_VALUE, predicate)) {
                stream.collect(toList())
                    .parallelStream()
                    .forEach(processFile(inputDir, outputDir));
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void checkParameters() throws MojoExecutionException {
        if (!inputDirectory.isDirectory())
            throw new MojoExecutionException("inputDirectory doesn't exist or isn't a directory");

        if (outputDirectory.exists() && !outputDirectory.isDirectory())
            throw new MojoExecutionException("outputDirectory isn't a directory");

        if (includeFiles == null || includeFiles.isBlank())
            throw new MojoExecutionException("includeFiles must not be empty");

        if (commands.isEmpty())
            throw new MojoExecutionException("commands must not be empty");
    }

    private Consumer<Path> processFile(Path inputDir, Path outputDir) {
        return file -> {
            var absFile = file.toAbsolutePath();

            getLog().debug("Process file: " + absFile);

            var processBuilders = IntStream.range(0, commands.size())
                .mapToObj(i -> Map.entry(i, commands.get(i).toList()))
                .map(entry -> entry.getKey() == 0 ? processPlaceholder(entry.getValue(), absFile) : entry.getValue())
                .peek(command -> getLog().debug("Run command: " + command))
                .map(ProcessBuilder::new)
                .collect(toList());

            try {
                var lastProcess = ProcessBuilder.startPipeline(processBuilders)
                    .stream()
                    .reduce((first, second) -> second)
                    .orElseThrow();

                saveProcessOutput(lastProcess.getInputStream(), outputDir.resolve(inputDir.relativize(absFile)));

                lastProcess.onExit().join();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    private List<String> processPlaceholder(List<String> command, Path path) {
        return IntStream.range(0, command.size())
            .mapToObj(i -> Map.entry(i, command.get(i)))
            .map(entry -> entry.getKey() == 0 ? entry.getValue() : entry.getValue().replace("{}", path.toString()))
            .collect(toList());
    }

    private void saveProcessOutput(InputStream in, Path path) throws IOException {
        var parent = path.getParent();
        if (parent != null)
            Files.createDirectories(parent);

        try (var out = Files.newOutputStream(path)) {
            in.transferTo(out);
        }
    }
}