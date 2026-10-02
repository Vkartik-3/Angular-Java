package com.example.product.batch;

import com.example.product.domain.Product;
import com.example.product.dto.ProductCsv;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ProductItemProcessorTest {

    private final ProductItemProcessor processor = new ProductItemProcessor();

    private ProductCsv csv(String category, String artist, String title, String price, String imageUrl) {
        ProductCsv item = new ProductCsv();
        item.setCategory(category);
        Map<String, String> attributes = new LinkedHashMap<>();
        if (artist != null) attributes.put("artist", artist);
        if (title != null) attributes.put("title", title);
        item.setAttributes(attributes);
        item.setPrice(price);
        item.setImageUrl(imageUrl);
        return item;
    }

    @Test
    void validRow_mapsToProductWithTrimmedFields() {
        Product result = processor.process(csv("vinyl", " Pink Floyd ", " The Wall ", " 29.99 ", "http://img.example.com/cover.jpg"));
        assertThat(result.getCategory()).isEqualTo("vinyl");
        assertThat(result.getAttributes()).containsEntry("artist", "Pink Floyd").containsEntry("title", "The Wall");
        assertThat(result.getPrice()).isEqualByComparingTo("29.99");
        assertThat(result.getImageUrl()).isEqualTo("http://img.example.com/cover.jpg");
    }

    @Test
    void validTurntableRow_mapsToProductWithManufacturerAndName() {
        ProductCsv item = new ProductCsv();
        item.setCategory("turntable");
        item.setAttributes(Map.of("manufacturer", " Pro-Ject ", "name", " Debut PRO B "));
        item.setPrice("1088.00");
        item.setImageUrl("/assets/products/ProJect-Debut-PRO-B.webp");

        Product result = processor.process(item);
        assertThat(result.getCategory()).isEqualTo("turntable");
        assertThat(result.getAttributes()).containsEntry("manufacturer", "Pro-Ject").containsEntry("name", "Debut PRO B");
        assertThat(result.getPrice()).isEqualByComparingTo("1088.00");
    }

    @Test
    void unknownCategory_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> processor.process(csv("cd", "Pro-Ject", "Debut PRO B", "1088.00", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown category");
    }

    @Test
    void blankArtist_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> processor.process(csv("vinyl", "   ", "The Wall", "29.99", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Artist");
    }

    @Test
    void blankTitle_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> processor.process(csv("vinyl", "Pink Floyd", "   ", "29.99", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Title");
    }

    @Test
    void invalidPrice_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> processor.process(csv("vinyl", "Pink Floyd", "The Wall", "not-a-number", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("price");
    }

    @Test
    void nullImageUrl_preservedAsNull() {
        Product result = processor.process(csv("vinyl", "Pink Floyd", "The Wall", "29.99", null));
        assertThat(result.getImageUrl()).isNull();
    }

    @Test
    void priceWithWhitespace_parsesCorrectly() {
        Product result = processor.process(csv("vinyl", "Pink Floyd", "The Wall", "  29.99  ", null));
        assertThat(result.getPrice()).isEqualByComparingTo("29.99");
    }
}