package com.example.cloud_spring_load_zip.controllers;

import com.example.cloud_spring_load_zip.service.FileArchiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileArchiveController {

    private final FileArchiveService fileArchiveService;

    @Autowired
    public FileArchiveController(FileArchiveService fileArchiveService) {
        this.fileArchiveService = fileArchiveService;
    }

    @PostMapping(value = "/compress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> compressFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Файл не может быть пустым"));
            }

            String zipFileUri = fileArchiveService.compressFileToZip(file);
            return ResponseEntity.ok()
                    .body(Map.of(
                            "message", "Файл успешно сжат",
                            "zipUri", zipFileUri
                    ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при сжатии файла: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/compress-multiple-alt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> compressMultipleFilesAlternative(
            @RequestParam("file1") MultipartFile file1,
            @RequestParam(value = "file2", required = false) MultipartFile file2,
            @RequestParam(value = "file3", required = false) MultipartFile file3,
            @RequestParam(value = "zipName", required = false, defaultValue = "archive") String zipName) {
        try {
            List<MultipartFile> files = new ArrayList<>();
            files.add(file1);
            if (file2 != null && !file2.isEmpty()) files.add(file2);
            if (file3 != null && !file3.isEmpty()) files.add(file3);

            if (files.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Необходимо загрузить хотя бы один файл"));
            }

            String zipFileUri = fileArchiveService.compressMultipleFilesToZip(
                    files.toArray(new MultipartFile[0]), zipName);

            return ResponseEntity.ok()
                    .body(Map.of(
                            "message", "Файлы успешно сжаты",
                            "zipUri", zipFileUri,
                            "fileCount", files.size()
                    ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при сжатии файлов: " + e.getMessage()));
        }
    }

    @PostMapping("/extract/{zipFileName}")
    public ResponseEntity<Map<String, String>> extractZipArchive(@PathVariable String zipFileName) {
        try {
            String extractedUri = fileArchiveService.extractZipArchive(zipFileName);
            return ResponseEntity.ok()
                    .body(Map.of(
                            "message", "Архив успешно распакован",
                            "extractedUri", extractedUri
                    ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при распаковке архива: " + e.getMessage()));
        }
    }

    @GetMapping("/download/zip/list")
    public ResponseEntity<Map<String, Object>> getFilesList() {
        try {
            List<String> fileUris = fileArchiveService.getFilesList();
            return ResponseEntity.ok()
                    .body(Map.of(
                            "files", fileUris,
                            "totalFiles", fileUris.size()
                    ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при получении списка файлов: " + e.getMessage()));
        }
    }

    @GetMapping("/download/zip/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Resource resource = fileArchiveService.loadFileAsResource(fileName);

            String contentType = "application/octet-stream";
            if (fileName.endsWith(".zip")) {
                contentType = "application/zip";
            } else {
                contentType = "multipart/form-data";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value="/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String,String>> getItems(@RequestParam List<MultipartFile> files, @RequestParam String zipName) {
        try {
            if (files.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Необходимо загрузить хотя бы один файл"));
            }

            String zipFileUri = fileArchiveService.compressMultipleFilesToZip(
                    files.toArray(new MultipartFile[0]), zipName);

            return ResponseEntity.ok()
                    .body(Map.of(
                            "message", "Файлы успешно сжаты",
                            "zipUri", zipFileUri.toString(),
                            "fileCount", String.valueOf(files.size())
                    ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка при сжатии файлов: " + e.getMessage()));
        }
    }
}
