package com.example.ElasticCommerce.domain.product.entity;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.elasticsearch.annotations.*;

@Document(indexName = "products")
@Setting(settingPath = "/elasticsearch/product-settings.json")
@Getter
public class ProductDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String productCode;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "products_name_analyzer"),
        otherFields = {
            @InnerField(suffix = "auto_complete", type = FieldType.Search_As_You_Type, analyzer = "nori")
        }
    )
    private String name;

    @Field(type = FieldType.Text, analyzer = "products_description_analyzer")
    private String description;

    @Field(type = FieldType.Long)
    private Long price;

    @Field(type = FieldType.Double)
    private double rating;

    @MultiField(mainField = @Field(type = FieldType.Text, analyzer = "products_category_analyzer"),
            otherFields = {
                    @InnerField(suffix = "raw", type = FieldType.Keyword)
            }
    )
    private String category;

    @Field(type = FieldType.Integer)
    private Integer stockQuantity;

    @Field(type = FieldType.Text, analyzer = "products_brand_analyzer")
    private String brand;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Boolean)
    private Boolean available;

    @Builder
    public ProductDocument(String id, String productCode, String name, String description, Long price,
                           double rating, String category, Integer stockQuantity, String brand, String imageUrl,
                           boolean available) {
        this.id = id;
        this.productCode = productCode;
        this.name = name;
        this.description = description;
        this.price = price;
        this.rating = rating;
        this.category = category;
        this.stockQuantity = stockQuantity;
        this.brand = brand;
        this.imageUrl = imageUrl;
        this.available = available;
    }

    public void highlighting(String highlightedName) {
        this.name = highlightedName;
    }
}
