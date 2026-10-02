package com.example.product.batch;

import com.example.product.domain.Product;
import com.example.product.dto.ProductCsv;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProductItemProcessor implements ItemProcessor<ProductCsv, Product> {

    // Attribute columns each category requires, in display order. Adding a
    // category is just adding an entry here — the reader already forwards
    // whatever columns the CSV header declares.
    private static final Map<String, List<String>> REQUIRED_ATTRIBUTES = Map.of(
            "vinyl", List.of("artist", "title"),
            "turntable", List.of("manufacturer", "name")
    );

    @Override
    public Product process(ProductCsv item) {
        String category = item.getCategory() != null ? item.getCategory().trim() : "";
        List<String> requiredKeys = REQUIRED_ATTRIBUTES.get(category);
        if (requiredKeys == null)
            throw new IllegalArgumentException("Unknown category: \"" + category + "\"");

        Map<String, String> attributes = new LinkedHashMap<>();
        Map<String, String> source = item.getAttributes();
        for (String key : requiredKeys) {
            String value = source != null ? source.get(key) : null;
            if (value == null || value.isBlank())
                throw new IllegalArgumentException(Character.toUpperCase(key.charAt(0)) + key.substring(1) + " is required");
            attributes.put(key, value.trim());
        }

        if (item.getPrice() == null || item.getPrice().isBlank())
            throw new IllegalArgumentException("Price is required");
        BigDecimal price;
        try {
            price = new BigDecimal(item.getPrice().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid price: \"" + item.getPrice().trim() + "\"");
        }

        return Product.builder()
                .category(category)
                .price(price)
                .imageUrl(item.getImageUrl() != null ? item.getImageUrl().trim() : null)
                .attributes(attributes)
                .createdAt(Instant.now())
                .build();
    }
}