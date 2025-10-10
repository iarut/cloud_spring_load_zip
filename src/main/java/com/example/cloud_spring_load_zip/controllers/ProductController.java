package com.example.cloud_spring_load_zip.controllers;

import io.micrometer.core.instrument.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;
import com.example.cloud_spring_load_zip.exception.ProductException;
import com.example.cloud_spring_load_zip.model.Product;
import com.example.cloud_spring_load_zip.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Validated
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private ProductService service;

    private final Counter requestCounter;
    private final Timer processingTimer;
    private final DistributionSummary responseSizeSummary;
    private final Queue<Product> processingQueue;
    private final AtomicInteger activeRequests;

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    public ProductController(ProductService service, MeterRegistry registry) {
        this.service = service;

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

    @PostMapping
    @Tag(name = "изменение/добавление/удаление")
    @Operation(summary = "Добавить новый продукт", description = "В ответе возвращается объект Product c полями id, name, quantity и price.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Продукт успешно создан"),
        @ApiResponse(responseCode = "400", description = "Некорректные данные продукта"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера"),
        @ApiResponse(responseCode = "403", description = "Некорректный путь запроса")
    })
    public ResponseEntity<Product> addProduct(@Parameter(description = "Данные о добавляемом продукте", required = true)
                                                  @RequestBody @Valid Product product) {
        logger.debug("Adding product {}", product);
        activeRequests.incrementAndGet();
        requestCounter.increment();
        processingQueue.add(product);
        String result = processingTimer.record(() -> {
            // Симуляция обработки
            try {
                Thread.sleep(100); // Задержка для имитации времени обработки
//                service.saveProduct(product);
                logger.debug("Saved product {}", product);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                service.saveProduct(null);
                logger.error("Error while saving product {}", product, e);
            }
            logger.debug("Saved product {}", product);
            return "Processed";
        });
        responseSizeSummary.record(result.getBytes(StandardCharsets.UTF_8).length);
        activeRequests.decrementAndGet();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveProduct(product).orElseThrow(() -> new ProductException("Error while saving product "+product) ));
    }

    @Tag(name = "get", description = "GET-методы Employee API")
    @GetMapping
    @Operation(summary = "Получить список продуктов", description = "В ответе возвращается список объектов Product c полями id, name, quantity и price.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Список продуктов успешно найден"),
            @ApiResponse(responseCode = "404", description = "Список продуктов пуст"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера"),
            @ApiResponse(responseCode = "403", description = "Некорректный путь запроса")
    })
    public ResponseEntity<List<Product>> findAllProducts() {
        activeRequests.incrementAndGet();
        requestCounter.increment();

        String result = processingTimer.record(() -> {
            // Симуляция обработки
            try {
                Thread.sleep(100);
                // Задержка для имитации времени обработки
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Processed";
        });
        responseSizeSummary.record(result.getBytes(StandardCharsets.UTF_8).length);
        activeRequests.decrementAndGet();
        logger.debug("Returning list of products {}", service.getProducts());
        return ResponseEntity.status(HttpStatus.FOUND).body(service.getProducts().orElseThrow(() -> new ProductException("No products in list")));
    }

    @Tag(name = "get", description = "GET-методы Employee API")
    @GetMapping("{id}")
    @Operation(summary = "Поиск продукта по id", description = "В ответе возвращается объект Product c полями id, name, quantity и price.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Продукт успешно создан"),
            @ApiResponse(responseCode = "404", description = "Не найден продукт с таким id"),
            @ApiResponse(responseCode = "400", description = "Некорректный id продукта"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера"),
            @ApiResponse(responseCode = "403", description = "Некорректный путь запроса")
    })
    public ResponseEntity<Product> findProductById(@Parameter(
            description = "ID продукта, данные по которому запрашиваются",
            required = true)@PathVariable @Valid @Max(100) int id) {
        activeRequests.incrementAndGet();
        requestCounter.increment();

        String result = processingTimer.record(() -> {
            // Симуляция обработки
            try {
                Thread.sleep(100); // Задержка для имитации времени обработки
                logger.debug("Returning product by id {}", id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("No such product {}", id);
            }
            return "Processed";
        });
        responseSizeSummary.record(result.getBytes(StandardCharsets.UTF_8).length);
        activeRequests.decrementAndGet();
        return ResponseEntity.status(HttpStatus.FOUND).body(service.getProductById(id).orElseThrow(() -> new ProductException("No product with such id "+id) ));
    }


    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Продукт успешно изменён"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные продукта"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера"),
            @ApiResponse(responseCode = "403", description = "Некорректный путь запроса")
    })
    @PutMapping("/{id}")
    @Operation(summary = "Обновить данные о продукте", description = "Обновляет или создаёт продукт по ID, принимая параметры отдельно.")
    @Tag(name = "изменение/добавление/удаление")
    public ResponseEntity<Product> updateProduct(
            @Parameter(description = "ID продукта", required = true)
            @PathVariable int id,
            @Parameter(description = "Имя продукта", required = true)
            @RequestParam String name,
            @Parameter(description = "Количество продукта", required = true)
            @RequestParam int quantity,
            @Parameter(description = "Цена продукта", required = true)
            @RequestParam double price) {

        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setQuantity(quantity);
        product.setPrice(price);

        activeRequests.incrementAndGet();
        requestCounter.increment();
        processingQueue.add(product);

        String result = processingTimer.record(() -> {
            try {
                Thread.sleep(100);
                logger.debug("Saved product {}", product);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Error while saving product {}", product, e);
            }
            return "Processed";
        });

        responseSizeSummary.record(result.getBytes(StandardCharsets.UTF_8).length);
        activeRequests.decrementAndGet();

        return ResponseEntity.status(HttpStatus.FOUND)
                .body(service.updateProduct(id, product)
                        .orElseThrow(() -> new ProductException("No product with such id " + id)));
    }

    @DeleteMapping("{id}")
    @Tag(name = "изменение/добавление/удаление")
    @Operation(summary = "Удалить данные о продукте", description = "В ответе возвращается сообщение об успешном удалении.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Продукт успешно удалён"),
            @ApiResponse(responseCode = "404", description = "Продукт с таким id не найден"),
            @ApiResponse(responseCode = "400", description = "Некорректный id продукта"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера"),
            @ApiResponse(responseCode = "403", description = "Некорректный путь запроса")
    })
    public ResponseEntity<String> deleteProduct(@Parameter(
            description = "ID продукта, который надо удалить",
            required = true)@PathVariable @Valid @Max(100) int id) {
        activeRequests.incrementAndGet();
        requestCounter.increment();
        processingQueue.add(new Product());
//        Добавляем заглушку, так как в очередь можно добавить только Product
        String result = processingTimer.record(() -> {
            // Симуляция обработки
            try {
                Thread.sleep(100); // Задержка для имитации времени обработки
                boolean deleted=service.deleteProduct(id);
                if (!deleted){
                    logger.debug("No product with such id {}", id);
                    throw new ProductException("No product with such id "+id);
                }
                else {
                    logger.debug("Deleted successfully {}", id);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Error while deleting product {}", id, e);
                throw new ProductException("Error while deleting product "+id);
            }
            return "Product with id " + id + " successfully deleted";
        });
        responseSizeSummary.record(result.getBytes(StandardCharsets.UTF_8).length);
        activeRequests.decrementAndGet();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    @Tag(name = "get", description = "GET-методы Employee API")
    @GetMapping("/queue/stats")
    @Operation(summary = "Получение данных о состоянии очереди обработки запросов", description = "В ответе возвращается кол-во ативных запросов и длина очереди запросов.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Данные о состоянии очереди успешно получены"),
            @ApiResponse(responseCode = "404", description = "Данные не найдены")
    })
    public ResponseEntity<QueueStats> getQueueStats() {
        QueueStats stats = new QueueStats();
        stats.setQueueSize(processingQueue.size());
        stats.setActiveRequests(activeRequests.get());

        // Также можно записать эти данные как метрики
        responseSizeSummary.record(stats.toString().getBytes().length);

        return ResponseEntity.ok(stats);
    }

    @Tag(name = "get", description = "GET-методы Employee API")
    @RequestMapping(method = RequestMethod.GET, value = "/byname", produces = "application/json")
    @Operation(summary = "Список продуктов с сортировкой по имени", description = "В ответе возвращается список объектов Product c полями id, name, quantity и price.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Сортировка прошла успешно"),
            @ApiResponse(responseCode = "404", description = "Что-то пошло не так")
    })
    public ResponseEntity<Map<String, List<Product>>> getProductsByName() {
        logger.debug("Getting products by name");
        try{
            Thread.sleep(100);
            if (service.getProductsByName().isEmpty()) {
                logger.debug("No products found");
                throw new ProductException("No products in list");
            }
            else {
                logger.debug("Products sorted by name");
                return ResponseEntity.status(HttpStatus.FOUND).body(service.getProductsByName());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Error while getting products by name", e);
            throw new ProductException("Error while getting products by name");
        }
    }

    @Tag(name = "get", description = "GET-методы Employee API")
    @RequestMapping(method = RequestMethod.GET, value = "/byprice", produces = "application/json")
    @Operation(summary = "Список продуктов с сортировкой по цене", description = "В ответе возвращается список объектов Product c полями id, name, quantity и price.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Сортировка прошла успешно"),
            @ApiResponse(responseCode = "404", description = "Что-то пошло не так")
    })
    public ResponseEntity<Map<Double, List<Product>>> getProductsByPrice() {
        try{
            logger.debug("Getting products by price");
            Thread.sleep(100);
            if (service.getProductByPrice().isEmpty()) {
                logger.debug("No products found");
                throw new ProductException("No products in list");
            }
            else {
                logger.debug("Products sorted by price");
                return ResponseEntity.status(HttpStatus.FOUND).body(service.getProductByPrice());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Error while getting products by price", e);
            throw new ProductException("Error while getting products by price");
        }
    }

    @Tag(name = "get", description = "GET-методы Employee API")
    @RequestMapping(method = RequestMethod.GET, value = "/byquantity", produces = "application/json")
    @Operation(summary = "Список продуктов с сортировкой по количеству", description = "В ответе возвращается список объектов Product c полями id, name, quantity и price.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Сортировка прошла успешно"),
            @ApiResponse(responseCode = "404", description = "Что-то пошло не так")
    })
    public ResponseEntity<Map<Integer, List<Product>>> getProductsByQuantity() {
        logger.debug("Getting products by quantity");
        try {
            Thread.sleep(100);
            if (service.getProductsByQuantity().isEmpty()) {
                logger.debug("No products found");
                throw new ProductException("No products in list");
            } else {
                logger.debug("Products sorted by quantity");
                return ResponseEntity.status(HttpStatus.FOUND).body(service.getProductsByQuantity());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Error while getting products by quantity", e);
            throw new ProductException("Error while getting products by quantity");
        }
    }

    @Tag(name = "get", description = "GET-методы Employee API")
    @RequestMapping(method = RequestMethod.GET, value = "/byid", produces = "application/json")
    @Operation(summary = "Список продуктов с сортировкой по id", description = "В ответе возвращается список объектов Product c полями id, name, quantity и price.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Сортировка прошла успешно"),
            @ApiResponse(responseCode = "404", description = "Что-то пошло не так")
    })
    public ResponseEntity<Map<Integer, List<Product>>> getProductsById() {
        logger.debug("Getting products by id");
        try{
            Thread.sleep(100);
            if (service.getProductsById().isEmpty()) {
                logger.debug("No products found");
                throw new ProductException("No products in list");
            }
            else {
                logger.debug("Products sorted by id");
                return ResponseEntity.status(HttpStatus.FOUND).body(service.getProductsById());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Error while getting products by id", e);
            throw new ProductException("Error while getting products by id");
        }
    }


    // Вспомогательный класс для статистики
    @Setter
    @Getter
    static class QueueStats {
        private int queueSize;
        private int activeRequests;

        @Override
        public String toString() {
            return String.format("QueueSize: %d, ActiveRequests: %d", queueSize, activeRequests);
        }
    }
}
