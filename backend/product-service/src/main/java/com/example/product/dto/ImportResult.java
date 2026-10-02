package com.example.product.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ImportResult {
    private final int imported;
    private final int skipped;
    private final List<SkippedRecord> skippedRecords;
}