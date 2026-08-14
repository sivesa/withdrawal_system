package com.enviro.assessment.junior.sive.repository;

import com.enviro.assessment.junior.sive.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
