package com.example.product.service;

import com.example.product.batch.ProductImportJob;
import com.example.product.batch.ProductSkipListener;
import com.example.product.dto.ImportResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
public class ProductImportService {

    private final JobOperator jobOperator;
    private final Job importProductsJob;
    private final ProductSkipListener skipListener;
    private final Counter importsTotal;
    private final Counter importFailuresTotal;
    private final Counter importedTotal;
    private final Counter skippedTotal;

    public ProductImportService(JobOperator jobOperator, Job importProductsJob,
                                ProductSkipListener skipListener, MeterRegistry registry) {
        this.jobOperator = jobOperator;
        this.importProductsJob = importProductsJob;
        this.skipListener = skipListener;
        this.importsTotal       = registry.counter("products.imports");
        this.importFailuresTotal = registry.counter("products.import.failures");
        this.importedTotal      = registry.counter("products.imported");
        this.skippedTotal       = registry.counter("products.skipped");
    }

    private static final List<String> EXPECTED_HEADERS = List.of(
            "category,artist,title,price,imageUrl",
            "category,manufacturer,name,price,imageUrl"
    );

    // SkipLimitExceededException is never a top-level failure exception —
    // Spring Batch wraps it inside a FatalStepExecutionException — so it has
    // to be found by walking each failure's cause chain, not by checking
    // JobExecution.getAllFailureExceptions() entries directly.
    private static boolean causedBySkipLimitExceeded(Throwable t) {
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            if (cause instanceof SkipLimitExceededException) return true;
        }
        return false;
    }

    private void validateFileType(String filename) {
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Invalid file type — only .csv files are accepted.");
        }
    }

    private void validateHeader(Path file) throws Exception {
        try (Stream<String> lines = Files.lines(file)) {
            String header = lines.findFirst().orElse("").trim();
            if (!EXPECTED_HEADERS.contains(header)) {
                throw new IllegalArgumentException(
                        "Invalid CSV header. Expected one of: " + String.join(" | ", EXPECTED_HEADERS) + ", got: " + header);
            }
        }
    }

    public ImportResult importCsv(MultipartFile file) {
        Path tempFile = null;
        boolean failureCounted = false;
        try {
            tempFile = Files.createTempFile("product-import-", ".csv");
            file.transferTo(tempFile);

            validateFileType(file.getOriginalFilename());
            validateHeader(tempFile);
            skipListener.reset();

            JobParameters params = new JobParametersBuilder()
                    .addString("filePath", tempFile.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution execution = jobOperator.start(importProductsJob, params);
            importsTotal.increment();

            if (execution.getStatus() == BatchStatus.FAILED) {
                importFailuresTotal.increment();
                failureCounted = true;

                boolean skipLimitExceeded = execution.getAllFailureExceptions().stream()
                        .anyMatch(ProductImportService::causedBySkipLimitExceeded);
                if (skipLimitExceeded) {
                    throw new IllegalArgumentException(
                            "Too many invalid rows — more than " + ProductImportJob.SKIP_LIMIT +
                                    " rows failed validation, so the import was aborted. Fix the file and try again.");
                }

                throw new RuntimeException("Import job failed: " + execution.getExitStatus().getExitDescription());
            }

            long written = execution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getWriteCount)
                    .sum();

            ImportResult result = ImportResult.builder()
                    .imported((int) written)
                    .skipped(skipListener.getSkippedRecords().size())
                    .skippedRecords(skipListener.getSkippedRecords())
                    .build();

            importedTotal.increment(result.getImported());
            skippedTotal.increment(result.getSkipped());
            return result;

        } catch (IllegalArgumentException e) {
            log.warn("Product import rejected: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Product import failed", e);
            if (!failureCounted) {
                importFailuresTotal.increment();
            }
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
            }
        }
    }
}
