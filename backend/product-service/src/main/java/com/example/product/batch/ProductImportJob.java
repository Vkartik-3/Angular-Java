package com.example.product.batch;

import com.example.product.domain.Product;
import com.example.product.dto.ProductCsv;
import com.example.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.infrastructure.item.file.mapping.FieldSetMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class ProductImportJob {

    private static final int CHUNK_SIZE = 5;
    public static final int SKIP_LIMIT = 3;

    // Columns handled as dedicated Product fields; every other column from the
    // CSV header becomes an attributes[] entry, keyed by its own name — this is
    // what lets vinyl (artist, title) and turntable (manufacturer, name) share
    // the same reader with no per-category mapping code.
    private static final Set<String> RESERVED_COLUMNS = Set.of("category", "price", "imageUrl");

    // No relational database backs this service (MongoDB holds the product
    // data), so there's nothing for a real PlatformTransactionManager to
    // manage — Spring Batch's own ResourcelessJobRepository (the Batch 6
    // default when no DataSource is present) pairs with this no-op manager.
    private final PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

    private final JobRepository jobRepository;
    private final ProductRepository productRepository;
    private final ProductItemProcessor processor;
    private final ProductItemWriter writer;
    private final ProductSkipListener skipListener;

    @Bean
    public Job importProductsJob() {
        return new JobBuilder("importProductsJob", jobRepository)
                .start(clearStep())
                .next(importStep())
                .build();
    }

    @Bean
    public Step clearStep() {
        Tasklet deleteAll = (_, _) -> {
            productRepository.deleteAll();
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder("clearStep", jobRepository)
                .tasklet(deleteAll, transactionManager)
                .build();
    }

    @Bean
    public Step importStep() {
        return new StepBuilder("importStep", jobRepository)
                .<ProductCsv, Product>chunk(CHUNK_SIZE)
                .reader(csvReader(""))
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skip(IllegalArgumentException.class)
                .skipLimit(SKIP_LIMIT)
                .listener(skipListener)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<ProductCsv> csvReader(
            @Value("#{jobParameters['filePath']}") String filePath) {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

        FieldSetMapper<ProductCsv> fieldSetMapper = fieldSet -> {
            ProductCsv item = new ProductCsv();
            item.setCategory(fieldSet.readString("category"));
            item.setPrice(fieldSet.readString("price"));
            item.setImageUrl(fieldSet.readString("imageUrl"));

            Map<String, String> attributes = new LinkedHashMap<>();
            for (String name : fieldSet.getNames()) {
                if (!RESERVED_COLUMNS.contains(name)) {
                    attributes.put(name, fieldSet.readString(name));
                }
            }
            item.setAttributes(attributes);
            return item;
        };

        DefaultLineMapper<ProductCsv> defaultMapper = new DefaultLineMapper<>();
        defaultMapper.setLineTokenizer(tokenizer);
        defaultMapper.setFieldSetMapper(fieldSetMapper);

        return new FlatFileItemReaderBuilder<ProductCsv>()
                .name("productCsvReader")
                .resource(new FileSystemResource(filePath))
                .lineMapper((line, lineNumber) -> {
                    ProductCsv item = defaultMapper.mapLine(line, lineNumber);
                    item.setLineNumber(lineNumber);
                    return item;
                })
                .linesToSkip(1)
                // The header row IS the column mapping — whatever names it
                // declares become the tokenizer's field names, so a CSV's own
                // header decides what ends up in attributes[].
                .skippedLinesCallback(header -> tokenizer.setNames(header.split(",")))
                .build();
    }
}