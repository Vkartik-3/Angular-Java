package com.example.product.batch;

import com.example.product.domain.Product;
import com.example.product.dto.ProductCsv;
import com.example.product.dto.SkippedRecord;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Getter
public class ProductSkipListener implements SkipListener<ProductCsv, Product> {

    // Both supported formats (category,artist,title,price,imageUrl and
    // category,manufacturer,name,price,imageUrl) are always 5 columns wide.
    private static final int EXPECTED_COLUMNS = 5;

    private final List<SkippedRecord> skippedRecords = new ArrayList<>();

    @Override
    public void onSkipInRead(@NonNull Throwable t) {
        if (t instanceof FlatFileParseException e) {
            int found = e.getInput().split(",", -1).length;
            skippedRecords.add(new SkippedRecord(
                    "Line " + e.getLineNumber(),
                    "Wrong number of columns. Required " + EXPECTED_COLUMNS + ", found " + found
            ));
        }
    }

    @Override
    public void onSkipInProcess(@NonNull ProductCsv item, @NonNull Throwable t) {
        skippedRecords.add(new SkippedRecord(
                "Line " + item.getLineNumber(),
                t.getMessage()
        ));
    }

    @Override
    public void onSkipInWrite(@NonNull Product item, @NonNull Throwable t) {
        skippedRecords.add(new SkippedRecord(
                String.join(" – ", item.getAttributes().values()),
                t.getMessage()
        ));
    }

    public void reset() {
        skippedRecords.clear();
    }
}