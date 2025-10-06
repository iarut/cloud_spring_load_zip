package com.example.cloud_spring_load_zip.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Validated
@Schema(description = "Product entity")
public class Product implements AutoCloseable {
    @Schema(description = "Unique identifier of the product", example = "1")
    @Positive(message = "Price must be greater than 0")
    private int id;

    @Schema(description = "Name of the product", example = "Laptop")
    private String name;

    @Schema(description = "Available quantity of the product", example = "50")
    @Min(0)
    private int quantity;

    @Schema(description = "Price of the product", example = "999.99")
    @Min(1)
    @Positive(message = "Price must be greater than 0")
    private double price;

    @Override
    public void close() throws Exception {

    }

    private String imageURI;

}