package com.example.cloud_spring_load_zip.repository;

import com.example.cloud_spring_load_zip.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ProductRepository {
    private List<Product> list = new ArrayList<Product>();

    private static final Logger logger = LoggerFactory.getLogger(ProductRepository.class);

    public ProductRepository() {createProducts();}



    public void createProducts() {
        logger.debug("Repository working on adding products");
        list = new ArrayList<>(List.of(
                new Product(1, "aproduct 1", 10, 1000, ""),
                new Product(2, "bproduct 2", 20, 2000, ""),
                new Product(3, "cproduct 3", 30, 3000, "")
        ));
    }

    public Optional<List<Product>> getAllProducts() {
        logger.debug("Repository returning list of products product {}", list);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        else {
            return Optional.of(list);
        }
    }

    public Optional<Product> findById(int id){
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == (id)) {
                logger.debug("Repository searching for item by id {}", id);
                return Optional.ofNullable(list.get(i));
            }
        }
        logger.info("No item found by id {}", id);
        return Optional.empty();
    }

    public List<Product> search(String name) {
        logger.debug("Repository searching for item by name {}", name);
        return list.stream().filter(x -> x.getName().startsWith(name)).collect(Collectors.toList());
    }

    public Product save(Product p) {
        Product product = new Product();
        product.setId(p.getId());
        product.setName(p.getName());
        product.setQuantity(p.getQuantity());
        product.setPrice(p.getPrice());
        product.setImageURI(p.getImageURI());
        list.add(product);
        logger.debug("Repository adding new product {}", product);
        return product;
    }

    public String delete(Integer id) {
        logger.debug("Repository deleting product by id {}", id);
        if (list.removeIf(x -> x.getId() == (id))){
            logger.info("Repository successfully deleted product by id {}", id);
            return "Product with id " + id + " deleted";
        }
        else{
            logger.info("Product with id {} not found", id);
            return "Product with id " + id + " not found";
        }
    }

    public Product update(Product product) {
        int idx = 0;
        int id = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == (product.getId())) {
                id = product.getId();
                idx = i;
                logger.info("Repository successfully updated product {}", product);
                break;
            }
        }

        Product product1 = new Product();
        product1.setId(id);
        product1.setName(product.getName());
        product1.setQuantity(product.getQuantity());
        product1.setPrice(product.getPrice());
        list.set(idx, product);
        logger.debug("Repository added new product {}", product1);
        return product1;
    }

    public Map<String, List<Product>> getProductsByName() {
        return list.stream().collect(Collectors.groupingBy(Product::getName));
    }

    public Map<Double, List<Product>> getProductsByPrice() {
        return list.stream().collect(Collectors.groupingBy(Product::getPrice));
    }

    public Map<Integer, List<Product>> getProductsByQuantity() {
        return list.stream().collect(Collectors.groupingBy(Product::getQuantity));
    }

    public Map<Integer, List<Product>> getProductsById() {
        return list.stream().collect(Collectors.groupingBy(Product::getId));
    }
}