package com.evdealer.evdealermanagement.entity.product;

import com.evdealer.evdealermanagement.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import net.minidev.json.annotate.JsonIgnore;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "product"})
public class ProductImages extends BaseEntity {

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name="public_id", length=255)
    private String publicId;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    //  Nhiều image thuộc về 1 product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonBackReference
    private Product product;

    private Integer width;
    private Integer height;
    private Integer bytes;
    private String format;

    private Integer position;
}
