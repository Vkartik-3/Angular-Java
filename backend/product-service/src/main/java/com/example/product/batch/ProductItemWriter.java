package com.example.product.batch;

import com.example.product.domain.Product;
import com.example.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductItemWriter implements ItemWriter<Product> {

    private final ProductRepository repository;

    @Override
    public void write(Chunk<? extends Product> chunk) {
        repository.saveAll(chunk.getItems());
    }
}