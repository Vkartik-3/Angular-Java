package com.example.product.service;

import com.example.product.batch.ProductSkipListener;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImportServiceTest {

    @Mock JobOperator jobOperator;
    @Mock Job importProductsJob;
    @Mock ProductSkipListener skipListener;

    ProductImportService service;

    @BeforeEach
    void setUp() {
        service = new ProductImportService(jobOperator, importProductsJob, skipListener, new SimpleMeterRegistry());
    }

    @Test
    void wrongExtension_throwsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile("file", "data.txt", "text/plain",
                "category,artist,title,price,imageUrl\n".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> service.importCsv(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only .csv");
    }

    @Test
    void wrongHeader_throwsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/csv",
                "wrong,header,format,here\n".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> service.importCsv(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid CSV header");
    }

    @Test
    void skipLimitExceeded_throwsClearIllegalArgumentException() throws Exception {
        String content = "category,artist,title,price,imageUrl\nvinyl,Pink Floyd,The Wall,29.99,\n";
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));

        // Mirrors how Spring Batch actually reports this: SkipLimitExceededException
        // is never a top-level failure exception, it's nested as the cause of a
        // FatalStepExecutionException — the detection logic must walk the cause chain.
        SkipLimitExceededException skipLimitExceededException =
                new SkipLimitExceededException(3, new IllegalArgumentException("Name is required"));
        RuntimeException fatalStepExecutionException =
                new RuntimeException("Unable to process chunk", skipLimitExceededException);

        JobExecution execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(execution.getAllFailureExceptions()).thenReturn(List.of(fatalStepExecutionException));
        when(jobOperator.start(any(Job.class), any(JobParameters.class))).thenReturn(execution);

        assertThatThrownBy(() -> service.importCsv(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Too many invalid rows")
                .hasMessageContaining("3");
    }

    @Test
    void correctFile_doesNotThrow() throws Exception {
        String content = "category,artist,title,price,imageUrl\nvinyl,Pink Floyd,The Wall,29.99,\n";
        MockMultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));

        JobExecution execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(execution.getStepExecutions()).thenReturn(List.of());
        when(jobOperator.start(any(Job.class), any(JobParameters.class))).thenReturn(execution);
        when(skipListener.getSkippedRecords()).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> service.importCsv(file));
    }
}