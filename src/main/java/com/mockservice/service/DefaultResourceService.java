package com.mockservice.service;

import com.mockservice.mockconfig.Route;
import com.mockservice.util.ResourceReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.security.CodeSource;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class DefaultResourceService implements ResourceService {

    private static final Logger log = LoggerFactory.getLogger(DefaultResourceService.class);

    private static final String DATA_FOLDER = "data";
    private static final String DATA_FILE_REGEX = ".+" + DATA_FOLDER + "[\\/\\\\](.+\\.json|.+\\.xml)$";

    private final ResourceLoader resourceLoader;
    private final Map<String, Route> dataFiles = new HashMap<>();

    public DefaultResourceService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        findResourceDataFiles();
    }

    private void findResourceDataFiles() {
        Pattern pattern = Pattern.compile(DATA_FILE_REGEX, Pattern.CASE_INSENSITIVE + Pattern.UNICODE_CASE);
        try {
            findResourcesMatchingPattern(this::addRoute, pattern);
        } catch (URISyntaxException | IOException e) {
            log.error("", e);
        }
    }

    private void addRoute(String resource) {
        String[] parts = resource.split("[/\\\\]");
        if (parts.length == 2) {
            String group = parts[0];
            String filename = parts[1];
            dataFiles.put(filename.toLowerCase(), Route.fromFileName(filename).setGroup(group));
        }
    }

    private static void findResourcesMatchingPattern(Consumer<String> consumer, Pattern pattern) throws URISyntaxException, IOException {
        CodeSource src = DefaultResourceService.class.getProtectionDomain().getCodeSource();
        URL url = src.getLocation();
        URI uri = url.toURI();
        if (uri.getScheme().equals("jar")) {
            walkJar(uri, consumer, pattern);
        } else {
            walkPath(Paths.get(uri), consumer, pattern);
        }
    }

    private static void walkJar(URI uri, Consumer<String> consumer, Pattern pattern) throws IOException {
        try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            for (Path path : fileSystem.getRootDirectories()) {
                walkPath(path, consumer, pattern);
            }
        }
    }

    private static void walkPath(Path path, Consumer<String> consumer, Pattern pattern) throws IOException {
        try (Stream<Path> files = Files.walk(path, 10)) {
            for (Iterator<Path> it = files.iterator(); it.hasNext(); ) {
                String file = it.next().toString();
                Matcher matcher = pattern.matcher(file);
                if (matcher.find()) {
                    consumer.accept(matcher.group(1));
                }
            }
        }
    }

    @Override
    public List<Route> files() {
        List<Route> list = new ArrayList<>();
        dataFiles.forEach((k, v) -> list.add(v));
        list.sort(Comparator.comparing(Route::getGroup).thenComparing(Route::getMethod).thenComparing(Route::getPath).thenComparing(Route::getSuffix));
        return list;
    }

    @Override
    public String load(String path) throws IOException {
        Route route = dataFiles.get(path.toLowerCase());
        if (route == null) {
            log.warn("File info not found: {}", path);
            return loadFromResource(path);
        }
        return loadFromResource(route.toPathFileName());
    }

    private String loadFromResource(String path) throws IOException {
        try {
            return ResourceReader.asString(resourceLoader, DATA_FOLDER + File.separator + path);
        } catch (IOException e) {
            throw new IOException("Error loading file: " + path, e);
        }
    }
}
