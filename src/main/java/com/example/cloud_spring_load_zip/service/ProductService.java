package com.example.cloud_spring_load_zip.service;

import com.example.cloud_spring_load_zip.model.Product;
import com.example.cloud_spring_load_zip.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    @Autowired
    private ProductRepository repository;

    public Optional<Product> saveProduct(Product product) {
        logger.debug("Service working on adding product {}", product);
        if (product.getName() == null) {
            logger.error("Product name is null");
            return Optional.empty();
        }
        if (product.getPrice() <= 0) {
            logger.error("Product price is less than or equal to zero");
            return Optional.empty();
        }
        if (product.getQuantity() < 0) {
            logger.error("Product quantity is less than zero");
            return Optional.empty();
        }
//        if (product.getId() == null) {
//            logger.error("Product id is null");
//            return Optional.empty();
//        }
        return Optional.ofNullable(repository.save(product));
    }

    public Optional<List<Product>> getProducts() {
        logger.debug("Service returning list of products{}", repository.getAllProducts());
        return repository.getAllProducts();
    }

    public Optional<Product> getProductById(int id) {
        logger.debug("Service searching for product {}", id);
        return repository.findById(id);
    }

    public boolean deleteProduct(int id) {
        String message = repository.delete(id);
        logger.debug("Service deleting product {}", id);
        return message.equals("Product with id " + id + " deleted");
    }

    public Optional<Product> updateProduct(int id, Product product) {
        logger.debug("Service updating {}", product);

        if (product.getName() == null) {
            logger.error("Product name is null");
            return Optional.empty();
        }
        if (product.getPrice() <= 0) {
            logger.error("Product price is less than or equal to zero");
            return Optional.empty();
        }
        if (product.getQuantity() < 0) {
            logger.error("Product quantity is less than zero");
            return Optional.empty();
        }

        return Optional.ofNullable(repository.update(id, product));
    }

    public Map<String, List<Product>> getProductsByName() {
        logger.debug("Service getting products by name");
        return repository.getProductsByName();
    }

    public Map<Double, List<Product>> getProductByPrice() {
        logger.debug("Service getting products by price ");
        return repository.getProductsByPrice();
    }

    public Map<Integer, List<Product>> getProductsByQuantity() {
        logger.debug("Service getting products by quantity ");
        return repository.getProductsByQuantity();
    }

    public Map<Integer, List<Product>> getProductsById() {
        logger.debug("Service getting products by id ");
        return repository.getProductsById();
    }
}
