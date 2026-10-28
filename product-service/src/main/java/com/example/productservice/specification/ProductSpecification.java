
package com.example.productservice.specification;

import com.example.productservice.entity.Product;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

    private ProductSpecification() {
        // Utility class
    }

    public static Specification<Product> filter(
            String search,
            String category,
            Double minPrice,
            Double maxPrice
    ) {

        return (root, query, cb) -> {

            // Only show active products
            var predicate = cb.isTrue(root.get("active"));

            // Search by product name or description
            if (search != null && !search.isBlank()) {

                String keyword =
                        "%" + search.trim().toLowerCase() + "%";

                predicate = cb.and(
                        predicate,
                        cb.or(
                                cb.like(
                                        cb.lower(root.get("name")),
                                        keyword
                                ),
                                cb.like(
                                        cb.lower(root.get("description")),
                                        keyword
                                )
                        )
                );
            }

            // Filter by category
            if (category != null && !category.isBlank()) {

                predicate = cb.and(
                        predicate,
                        cb.equal(
                                cb.lower(root.get("category")),
                                category.trim().toLowerCase()
                        )
                );
            }

            // Minimum price
            if (minPrice != null) {

                predicate = cb.and(
                        predicate,
                        cb.ge(
                                root.get("price"),
                                minPrice
                        )
                );
            }

            // Maximum price
            if (maxPrice != null) {

                predicate = cb.and(
                        predicate,
                        cb.le(
                                root.get("price"),
                                maxPrice
                        )
                );
            }

            return predicate;
        };
    }
}
