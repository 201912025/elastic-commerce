package com.example.ElasticCommerce.domain.review.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;

@Document(indexName = "reviews")
@Setting(settingPath = "/elasticsearch/review-settings.json")
@Getter
public class ReviewDocument {

    @Id
    @Field(type = FieldType.Keyword)
    private String reviewId;

    @Field(type = FieldType.Keyword)
    private String productId;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Text, analyzer = "review_title_analyzer")
    private String title;

    @Field(type = FieldType.Double)
    private double rating;

    @Field(type = FieldType.Text, analyzer = "review_comment_analyzer")
    private String comment;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private Instant createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private Instant modifiedAt;

    @Builder
    public ReviewDocument(
            String reviewId,
            String productId,
            String userId,
            String title,
            double rating,
            String comment,
            Instant createdAt,
            Instant modifiedAt
    ) {
        this.reviewId = reviewId;
        this.productId = productId;
        this.userId = userId;
        this.title = title;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }
}

