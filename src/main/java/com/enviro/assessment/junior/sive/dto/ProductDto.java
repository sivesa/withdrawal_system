package com.enviro.assessment.junior.sive.dto;

import com.enviro.assessment.junior.sive.entity.ProductType;

/** Read-only product reference, used by the admin UI to populate the "add holding" product dropdown. */
public class ProductDto {

    private Long id;
    private String name;
    private ProductType type;

    public ProductDto() {
    }

    public ProductDto(Long id, String name, ProductType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProductType getType() {
        return type;
    }

    public void setType(ProductType type) {
        this.type = type;
    }
}
