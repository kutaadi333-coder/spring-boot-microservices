package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.ProductPageResponse;
import com.ecommerce.productservice.dto.ProductRequestDTO;
import com.ecommerce.productservice.dto.ProductResponseDTO;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.exception.ProductNotFoundException;
import com.ecommerce.productservice.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Service
public class ProductService {

    private static final String CACHE_KEY_PREFIX = "product:";
    private static final Duration CACHE_TTL =
            Duration.ofMinutes(10);

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductService(
            ProductRepository productRepository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // CREATE
    public ProductResponseDTO createProduct(ProductRequestDTO request) {

        Product product = new Product();

        product.setName(request.name());
        product.setPrice(request.price());
        product.setCategory(request.category());

        product.setStock(0);
        product.setStatus("AVAILABLE");

        Product savedProduct = productRepository.save(product);

        return convertToResponse(savedProduct);
    }

    // READ - Get all products with pagination, sorting and filtering
    public ProductPageResponse getAllProducts(
            int page,
            int size,
            String sortBy,
            String sortDir,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String keyword
    ) {

        Sort.Direction direction =
                sortDir.equalsIgnoreCase("desc")
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        Sort sort = Sort.by(direction, sortBy);

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> productPage;

        if (category != null ||
                minPrice != null ||
                maxPrice != null ||
                keyword != null) {

            productPage = productRepository.findProductsWithFilters(
                    category,
                    minPrice,
                    maxPrice,
                    keyword,
                    pageable
            );

        } else {

            productPage = productRepository.findAll(pageable);
        }

        List<ProductResponseDTO> content = productPage
                .getContent()
                .stream()
                .map(this::convertToResponse)
                .toList();

        return new ProductPageResponse(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isLast()
        );
    }

    // READ - Get product by ID with Redis cache
    public ProductResponseDTO getProductById(Long id) {

        String cacheKey = CACHE_KEY_PREFIX + id;

        try {

            // Check Redis first
            String cachedProduct =
                    redisTemplate.opsForValue().get(cacheKey);

            if (cachedProduct != null) {

                System.out.println(
                        "CACHE HIT - Product ID: " + id
                );

                return objectMapper.readValue(
                        cachedProduct,
                        ProductResponseDTO.class
                );
            }

            System.out.println(
                    "CACHE MISS - Product ID: " + id
            );

        } catch (Exception e) {

            System.out.println(
                    "Redis read failed. Falling back to database. "
                            + "Product ID: " + id
            );
        }

        // Cache miss → Database
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        ProductResponseDTO response =
                convertToResponse(product);

        // Store result in Redis
        try {

            String responseJson =
                    objectMapper.writeValueAsString(response);

            redisTemplate.opsForValue().set(
                    cacheKey,
                    responseJson,
                    CACHE_TTL
            );

            System.out.println(
                    "CACHE STORE - Product ID: " + id
                            + " | TTL: " + CACHE_TTL
            );

        } catch (JsonProcessingException e) {

            System.out.println(
                    "Redis write failed. "
                            + "Returning database response. "
                            + "Product ID: " + id
            );
        }

        return response;
    }

    // UPDATE - Full update
    public ProductResponseDTO updateProduct(
            Long id,
            ProductRequestDTO request
    ) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        product.setName(request.name());
        product.setPrice(request.price());
        product.setCategory(request.category());

        Product updatedProduct =
                productRepository.save(product);

        // Invalidate cache
        invalidateProductCache(id);

        return convertToResponse(updatedProduct);
    }

    // UPDATE - Partial update
    public ProductResponseDTO patchProduct(
            Long id,
            ProductRequestDTO request
    ) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        if (request.name() != null) {
            product.setName(request.name());
        }

        if (request.price() != null) {
            product.setPrice(request.price());
        }

        if (request.category() != null) {
            product.setCategory(request.category());
        }

        Product updatedProduct =
                productRepository.save(product);

        // Invalidate cache
        invalidateProductCache(id);

        return convertToResponse(updatedProduct);
    }

    // DELETE
    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        productRepository.delete(product);

        // Invalidate cache
        invalidateProductCache(id);
    }

    // Cache invalidation
    private void invalidateProductCache(Long id) {

        String cacheKey = CACHE_KEY_PREFIX + id;

        try {

            Boolean deleted =
                    redisTemplate.delete(cacheKey);

            System.out.println(
                    "CACHE INVALIDATED - Product ID: " + id
                            + " | Deleted: " + deleted
            );

        } catch (Exception e) {

            System.out.println(
                    "Redis cache invalidation failed "
                            + "for Product ID: " + id
            );
        }
    }

    // Convert Entity to Response DTO
    private ProductResponseDTO convertToResponse(
            Product product
    ) {

        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getCategory()
        );
    }
}