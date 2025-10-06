package com.example.cloud_spring_load_zip.service;

import com.example.cloud_spring_load_zip.exception.FileStorageException;
import com.example.cloud_spring_load_zip.exception.MyFileNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        // Создаем путь к директории для хранения файлов
        String originalPath = fileStorageProperties.getUploadDir();
        String systemSeparator = FileSystems.getDefault().getSeparator();

        // Нормализуем путь, заменяя все возможные разделители на системные
        String normalizedPath = originalPath.replace("/", systemSeparator)
                .replace("\\", systemSeparator);

        this.fileStorageLocation = Paths.get(normalizedPath)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        // Нормализуем имя файла
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Проверяем на наличие недопустимых символов в пути
            if (fileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            // Проверяем, не пустой ли файл
            if (file.isEmpty()) {
                throw new FileStorageException("Cannot store empty file: " + fileName);
            }

            // Дополнительная проверка безопасности - предотвращение Path Traversal
            Path targetLocation = this.fileStorageLocation.resolve(fileName).normalize();
            if (!targetLocation.startsWith(this.fileStorageLocation)) {
                throw new FileStorageException("Cannot store file outside current directory.");
            }

            // Копируем файл с правильной обработкой Stream
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            // Проверка безопасности имени файла
            if (fileName.contains("..")) {
                throw new MyFileNotFoundException("Invalid file name: " + fileName);
            }

            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            
            // Дополнительная проверка безопасности
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new MyFileNotFoundException("Access denied: " + fileName);
            }

            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new MyFileNotFoundException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("File not found: " + fileName, ex);
        }
    }

    // Дополнительные методы для управления файлами

    public boolean deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            
            // Проверка безопасности
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new SecurityException("Cannot delete file outside storage directory");
            }

            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new FileStorageException("Could not delete file: " + fileName, ex);
        }
    }

    public boolean fileExists(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            
            // Проверка безопасности
            if (!filePath.startsWith(this.fileStorageLocation)) {
                return false;
            }

            return Files.exists(filePath) && Files.isRegularFile(filePath);
        } catch (Exception ex) {
            return false;
        }
    }

    // Получение размера файла
    public long getFileSize(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new SecurityException("Access denied");
            }

            if (Files.exists(filePath)) {
                return Files.size(filePath);
            } else {
                throw new MyFileNotFoundException("File not found: " + fileName);
            }
        } catch (IOException ex) {
            throw new FileStorageException("Could not get file size: " + fileName, ex);
        }
    }

    public void deleteAll() {
        FileSystemUtils.deleteRecursively(this.fileStorageLocation.toFile());
    }


    public List<Path> loadAll() {
        try (Stream<Path> stream = Files.walk(this.fileStorageLocation, 1)) {
            return stream
                    .filter(path -> !path.equals(this.fileStorageLocation))
                    .map(this.fileStorageLocation::relativize)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Could not load the files!", e);
        }
    }
}