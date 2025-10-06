package com.example.cloud_spring_load_zip.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class FileArchiveService {

    private final FileStorageProperties fileStorageProperties;
    private final Path fileStorageLocation;

    @Autowired
    public FileArchiveService(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать директорию для загрузки файлов", e);
        }
    }

    public String compressFileToZip(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String zipFileName = originalFileName + ".zip";
        Path zipFilePath = this.fileStorageLocation.resolve(zipFileName);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
            ZipEntry zipEntry = new ZipEntry(originalFileName);
            zos.putNextEntry(zipEntry);
            zos.write(file.getBytes());
            zos.closeEntry();
        }

        return generateFileUri(zipFileName);
    }

    public String compressMultipleFilesToZip(MultipartFile[] files, String zipFileName) throws IOException {
        if (!zipFileName.endsWith(".zip")) {
            zipFileName += ".zip";
        }

        Path zipFilePath = this.fileStorageLocation.resolve(zipFileName);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
            for (MultipartFile file : files) {
                ZipEntry zipEntry = new ZipEntry(file.getOriginalFilename());
                zos.putNextEntry(zipEntry);
                zos.write(file.getBytes());
                zos.closeEntry();
            }
        }

        return generateFileUri(zipFileName);
    }

    public String extractZipArchive(String zipFileName) throws IOException {
        Path zipFilePath = this.fileStorageLocation.resolve(zipFileName);

        if (!Files.exists(zipFilePath)) {
            throw new FileNotFoundException("ZIP файл не найден: " + zipFilePath);
        }

        String extractDirName = zipFileName.replace(".zip", "_extracted");
        Path extractPath = this.fileStorageLocation.resolve(extractDirName);

        if (!Files.exists(extractPath)) {
            Files.createDirectories(extractPath);
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                Path filePath = extractPath.resolve(zipEntry.getName());

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                zis.closeEntry();
            }
        }

        return generateFileUri(extractDirName);
    }

    public List<String> getFilesList() throws IOException {
        List<String> fileUris = new ArrayList<>();

        Files.list(this.fileStorageLocation)
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    if (fileName.endsWith(".zip")) {
                        String fileUri = generateFileUri(fileName);
                        fileUris.add(fileUri);
                    }
                });

        return fileUris;
    }

    private String generateFileUri(String fileName) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/files/download/")
                .path(fileName)
                .toUriString();
    }

    public Resource loadFileAsResource(String fileName) throws MalformedURLException {
        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists()) {
            return resource;
        } else {
            throw new RuntimeException("Файл не найден: " + fileName);
        }
    }
}
