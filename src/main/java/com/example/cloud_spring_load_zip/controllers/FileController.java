package com.example.cloud_spring_load_zip.controllers;

import io.micrometer.core.instrument.*;
import jakarta.servlet.http.HttpServletRequest;
import com.example.cloud_spring_load_zip.model.Product;
import com.example.cloud_spring_load_zip.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    private final Counter requestCounter;
    private final Timer processingTimer;
    private final DistributionSummary responseSizeSummary;
    private final Queue<Product> processingQueue;
    private final AtomicInteger activeRequests;

    public static final Logger logger = LoggerFactory.getLogger(FileController.class);

    public FileController(MeterRegistry registry) {

        // Инициализация счетчика запросов
        this.requestCounter = Counter.builder("api.requests")
                .description("Count of API requests")
                .tag("endpoint", "metrics")
                .register(registry);

        // Инициализация таймера обработки
        this.processingTimer = Timer.builder("api.processing.time")
                .description("Time taken to process API requests")
                .register(registry);

        // Инициализация распределения размеров ответов
        this.responseSizeSummary = DistributionSummary.builder("api.response.size")
                .description("Size of API responses in bytes")
                .baseUnit("bytes")
                .register(registry);

        // Инициализация очереди и gauge для мониторинга ее размера
        this.processingQueue = new ConcurrentLinkedQueue<>();
        Gauge.builder("api.queue.size", processingQueue, Queue::size)
                .description("Current size of processing queue")
                .register(registry);

        // Инициализация счетчика активных запросов
        this.activeRequests = new AtomicInteger(0);
        Gauge.builder("api.active.requests", activeRequests, AtomicInteger::get)
                .description("Number of currently active requests")
                .register(registry);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);
        return ResponseEntity.ok("File uploaded successfully: " + fileName);
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, 
                                               HttpServletRequest request) {
        // Загружаем файл как Resource
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        // Определяем Content-Type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // Если не можем определить тип, используем по умолчанию
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/files")
    public ResponseEntity<List<String>> getListFiles() {
        // Получаем список файлов безопасно
        List<Path> paths = fileStorageService.loadAll(); // теперь возвращает List<Path>

        List<String> fileInfos = paths.stream()
                .map(path -> path.getFileName().toString())
                .map(s->"http://localhost:8081/api/files/download/"+s)
                .collect(Collectors.toList());

        return ResponseEntity.ok(fileInfos);
    }

//    @PostMapping(value = "/uploadWithURI", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @Tag(name = "изменение/добавление/удаление")
//    @Operation(summary = "Добавить новый продукт", description = "В ответе возвращается объект Product c полями id, name, quantity и price.")
//    @ApiResponses({
//            @ApiResponse(responseCode = "201", description = "Продукт успешно создан"),
//            @ApiResponse(responseCode = "400", description = "Некорректные данные продукта"),
//            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера"),
//            @ApiResponse(responseCode = "403", description = "Некорректный путь запроса")
//    })
//    public ResponseEntity<Product> addProduct1(@Parameter(description = "Данные о добавляемом продукте", required = true)
//                                              @RequestBody @Valid Product product, @RequestParam("file") MultipartFile file) {
//        logger.debug("Adding product {}", product);
//        String fileName = fileStorageService.storeFile(file);
//        product.setImageURI("http://localhost:8081/api/files/"+fileName);
//        activeRequests.incrementAndGet();
//        requestCounter.increment();
//        processingQueue.add(product);
//        String result = processingTimer.record(() -> {
//            // Симуляция обработки
//            try {
//                Thread.sleep(100); // Задержка для имитации времени обработки
////                service.saveProduct(product);
//                logger.debug("Saved product {}", product);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                service.saveProduct(null);
//                logger.error("Error while saving product {}", product, e);
//            }
//            logger.debug("Saved product {}", product);
//            return "Processed";
//        });
//        responseSizeSummary.record(result.getBytes(StandardCharsets.UTF_8).length);
//        activeRequests.decrementAndGet();
//        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveProduct(product).orElseThrow(() -> new ProductException("Error while saving product "+product) ));
//    }

//    @PostMapping(value = "/uploadWithURI1", consumes = "multipart/form-data")
//    @Operation(summary = "Добавить новый продукт с изображением")
//    public ResponseEntity<Product> addProductWithFile(
//            @Parameter(description = "Файл изображения продукта", required = true,
//                    content = @Content(mediaType = "multipart/form-data",
//                            schema = @Schema(type = "string", format = "binary")))
//            @RequestParam("file") MultipartFile file,
//
//            @Parameter(description = "id", required = true)
//            @RequestParam("id") int id,
//
//            @Parameter(description = "Название продукта", required = true)
//            @RequestParam("name") String name,
//
//            @Parameter(description = "Количество продукта", required = true)
//            @RequestParam("quantity") Integer quantity,
//
//            @Parameter(description = "Цена продукта", required = true)
//            @RequestParam("price") Double price
//
//    ) {
//
//        // Здесь сохраняем файл и создаём объект продукта
//        String fileName = fileStorageService.storeFile(file);
//        Product product = new Product();
//        product.setId(id);
//        product.setName(name);
//        product.setQuantity(quantity);
//        product.setPrice(price);
//        product.setImageURI("http://localhost:8081/api/files/download/" + fileName);
//
//        Product saved = service.saveProduct(product)
//                .orElseThrow(() -> new ProductException("Ошибка при сохранении продукта"));
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
//    }
}