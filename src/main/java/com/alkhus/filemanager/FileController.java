package com.alkhus.filemanager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class FileController {
    private final Path rootLocation;

    public FileController(@Value("${app.path}") String path) {
        this.rootLocation = Paths.get(path);
    }

    @GetMapping("/files")
    public ResponseEntity<?> listAllFiles() {
        try {
            List<File> fileList = Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(this.rootLocation::relativize)
                    .map(Path::toFile)
                    .map(file -> new File(file.getName(), file.isDirectory()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(fileList);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            return ResponseEntity.badRequest()
                    .body("Couldn't retrieve files!");
        }
    }

    @GetMapping("/files/{filename}")
    public ResponseEntity<?> getFile(@PathVariable String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            if (Files.notExists(file)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.badRequest()
                        .body("Couldn't retrieve file!");
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid URL!");
        }
    }
}
