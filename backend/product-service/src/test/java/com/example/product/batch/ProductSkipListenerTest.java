package com.example.product.batch;

import com.example.product.domain.Product;
import com.example.product.dto.ProductCsv;
import com.example.product.dto.SkippedRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ProductSkipListenerTest {

    private final ProductSkipListener listener = new ProductSkipListener();

    @BeforeEach
    void reset() {
        listener.reset();
    }

    @Test
    void onSkipInRead_flatFileParseException_addsColumnCountMismatch() {
        listener.onSkipInRead(new FlatFileParseException("parse error", "Metallica,Master Of Puppets,36.99", 4));
        List<SkippedRecord> records = listener.getSkippedRecords();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).identifier()).isEqualTo("Line 4");
        assertThat(records.get(0).reason()).isEqualTo("Wrong number of columns. Required 5, found 3");
    }

    @Test
    void onSkipInRead_otherException_addsNothing() {
        listener.onSkipInRead(new RuntimeException("irrelevant"));
        assertThat(listener.getSkippedRecords()).isEmpty();
    }

    @Test
    void onSkipInProcess_addsLineNumberAndMessage() {
        ProductCsv item = new ProductCsv();
        item.setLineNumber(5);
        listener.onSkipInProcess(item, new IllegalArgumentException("Price is required"));
        List<SkippedRecord> records = listener.getSkippedRecords();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).identifier()).isEqualTo("Line 5");
        assertThat(records.get(0).reason()).isEqualTo("Price is required");
    }

    @Test
    void onSkipInWrite_addsAttributeValues() {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("artist", "Iron Maiden");
        attributes.put("title", "The Number of the Beast");
        Product item = Product.builder()
                .category("vinyl")
                .attributes(attributes)
                .price(BigDecimal.TEN)
                .createdAt(Instant.now())
                .build();
        listener.onSkipInWrite(item, new RuntimeException("DB error"));
        List<SkippedRecord> records = listener.getSkippedRecords();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).identifier()).isEqualTo("Iron Maiden – The Number of the Beast");
        assertThat(records.get(0).reason()).isEqualTo("DB error");
    }

    @Test
    void reset_clearsAllRecords() {
        listener.onSkipInRead(new FlatFileParseException("parse error", "bad,input", 1));
        assertThat(listener.getSkippedRecords()).hasSize(1);
        listener.reset();
        assertThat(listener.getSkippedRecords()).isEmpty();
    }
}