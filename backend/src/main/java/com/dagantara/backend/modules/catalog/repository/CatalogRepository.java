package com.dagantara.backend.modules.catalog.repository;

import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Category;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.Option;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.OptionValue;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.ProductStatus;
import com.dagantara.backend.modules.catalog.dto.CatalogResponses.ProductSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CatalogRepository {
    private static final String PRODUCT_SELECT = """
            SELECT id, name, description, price_idr, product_code, status, primary_category_id
            FROM products WHERE tenant_id = :tenantId AND id = :productId
            """;

    private final JdbcClient jdbc;

    public CatalogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Category insertCategory(UUID tenantId, String name) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO categories (id, tenant_id, name) VALUES (:id, :tenantId, :name)")
                .param("id", id).param("tenantId", tenantId).param("name", name).update();
        return new Category(id, name);
    }

    public Page<Category> listCategories(UUID tenantId, Pageable page) {
        List<Category> items = jdbc.sql("""
                SELECT id, name FROM categories WHERE tenant_id = :tenantId
                ORDER BY name, id LIMIT :size OFFSET :offset
                """).param("tenantId", tenantId).param("size", page.getPageSize())
                .param("offset", page.getOffset())
                .query((row, index) -> new Category(row.getObject("id", UUID.class), row.getString("name"))).list();
        long total = jdbc.sql("SELECT count(*) FROM categories WHERE tenant_id = :tenantId")
                .param("tenantId", tenantId).query(Long.class).single();
        return new PageImpl<>(items, page, total);
    }

    public boolean containsCategories(UUID tenantId, Collection<UUID> categoryIds) {
        return jdbc.sql("SELECT count(*) FROM categories WHERE tenant_id = :tenantId AND id IN (:categoryIds)")
                .param("tenantId", tenantId).param("categoryIds", categoryIds)
                .query(Long.class).single() == categoryIds.size();
    }

    public void insertProduct(UUID tenantId, Product product) {
        jdbc.sql("""
                INSERT INTO products (id, tenant_id, name, description, price_idr, product_code, primary_category_id)
                VALUES (:productId, :tenantId, :name, :description, :price, :code, :primaryId)
                """).param("productId", product.id()).param("tenantId", tenantId)
                .param("name", product.name()).param("description", product.description())
                .param("price", product.priceIdr()).param("code", product.productCode())
                .param("primaryId", product.primaryCategoryId()).update();
    }

    public void updateDetails(UUID tenantId, UUID productId, String name, String description, Long price, String code) {
        jdbc.sql("""
                UPDATE products SET name = :name, description = :description, price_idr = :price, product_code = :code
                WHERE tenant_id = :tenantId AND id = :productId
                """).param("tenantId", tenantId).param("productId", productId).param("name", name)
                .param("description", description).param("price", price).param("code", code).update();
    }

    public void updatePublication(UUID tenantId, UUID productId, ProductStatus status) {
        jdbc.sql("""
                UPDATE products SET status = :status WHERE tenant_id = :tenantId AND id = :productId
                """).param("tenantId", tenantId).param("productId", productId).param("status", status.name()).update();
    }

    public Page<ProductSummary> listProducts(UUID tenantId, Pageable page) {
        List<ProductSummary> items = jdbc.sql("""
                SELECT id, name, price_idr, status, primary_category_id FROM products WHERE tenant_id = :tenantId
                ORDER BY name, id LIMIT :size OFFSET :offset
                """).param("tenantId", tenantId).param("size", page.getPageSize()).param("offset", page.getOffset())
                .query((row, index) -> new ProductSummary(row.getObject("id", UUID.class), row.getString("name"),
                        row.getObject("price_idr", Long.class), ProductStatus.valueOf(row.getString("status")),
                        row.getObject("primary_category_id", UUID.class))).list();
        long total = jdbc.sql("SELECT count(*) FROM products WHERE tenant_id = :tenantId")
                .param("tenantId", tenantId).query(Long.class).single();
        return new PageImpl<>(items, page, total);
    }

    public Optional<Product> findProduct(UUID tenantId, UUID productId) {
        return productQuery(PRODUCT_SELECT, tenantId, productId);
    }

    public Optional<Product> findPublishedProduct(UUID tenantId, UUID productId) {
        return productQuery(PRODUCT_SELECT + " AND status = 'PUBLISHED'", tenantId, productId);
    }

    public List<Category> listProductCategories(UUID tenantId, UUID productId) {
        return jdbc.sql("""
                SELECT c.id, c.name FROM product_categories pc
                JOIN categories c ON c.tenant_id = pc.tenant_id AND c.id = pc.category_id
                WHERE pc.tenant_id = :tenantId AND pc.product_id = :productId ORDER BY c.name, c.id
                """).param("tenantId", tenantId).param("productId", productId)
                .query((row, index) -> new Category(row.getObject("id", UUID.class), row.getString("name"))).list();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Product> lockProduct(UUID tenantId, UUID productId) {
        return productQuery(PRODUCT_SELECT + " FOR NO KEY UPDATE", tenantId, productId);
    }

    private Optional<Product> productQuery(String sql, UUID tenantId, UUID productId) {
        return jdbc.sql(sql).param("tenantId", tenantId).param("productId", productId)
                .query((row, index) -> new Product(row.getObject("id", UUID.class), row.getString("name"),
                        row.getString("description"), row.getObject("price_idr", Long.class),
                        row.getString("product_code"), ProductStatus.valueOf(row.getString("status")),
                        row.getObject("primary_category_id", UUID.class))).optional();
    }

    public void replaceCategories(UUID tenantId, UUID productId, Collection<UUID> categoryIds, UUID primaryId) {
        jdbc.sql("""
                DELETE FROM product_categories
                WHERE tenant_id = :tenantId AND product_id = :productId AND category_id NOT IN (:categoryIds)
                """).param("tenantId", tenantId).param("productId", productId).param("categoryIds", categoryIds).update();
        jdbc.sql("""
                INSERT INTO product_categories (tenant_id, product_id, category_id)
                SELECT tenant_id, :productId, id FROM categories
                WHERE tenant_id = :tenantId AND id IN (:categoryIds)
                ON CONFLICT (product_id, category_id) DO NOTHING
                """).param("tenantId", tenantId).param("productId", productId).param("categoryIds", categoryIds).update();
        jdbc.sql("""
                UPDATE products SET primary_category_id = :primaryId WHERE tenant_id = :tenantId AND id = :productId
                """).param("tenantId", tenantId).param("productId", productId).param("primaryId", primaryId).update();
    }

    public List<UUID> listCategoryIds(UUID tenantId, UUID productId) {
        return jdbc.sql("""
                SELECT category_id FROM product_categories
                WHERE tenant_id = :tenantId AND product_id = :productId ORDER BY category_id
                """).param("tenantId", tenantId).param("productId", productId).query(UUID.class).list();
    }

    public UUID insertOption(UUID tenantId, UUID productId, String name, int position) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO product_options (id, product_id, name, position)
                SELECT :id, id, :name, :position FROM products WHERE tenant_id = :tenantId AND id = :productId
                """).param("id", id).param("tenantId", tenantId).param("productId", productId)
                .param("name", name).param("position", position).update();
        return id;
    }

    public boolean containsOption(UUID tenantId, UUID productId, UUID optionId) {
        return jdbc.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM product_options o JOIN products p ON p.id = o.product_id
                    WHERE p.tenant_id = :tenantId AND p.id = :productId AND o.id = :optionId
                )
                """).param("tenantId", tenantId).param("productId", productId).param("optionId", optionId)
                .query(Boolean.class).single();
    }

    public long countOptions(UUID tenantId, UUID productId) {
        return jdbc.sql("""
                SELECT count(*) FROM product_options o JOIN products p ON p.id = o.product_id
                WHERE p.tenant_id = :tenantId AND p.id = :productId
                """).param("tenantId", tenantId).param("productId", productId).query(Long.class).single();
    }

    public OptionValue insertOptionValue(UUID tenantId, UUID productId, UUID optionId, String label, String code) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO product_option_values (id, product_id, option_id, label, code)
                SELECT :id, o.product_id, o.id, :label, :code
                FROM product_options o JOIN products p ON p.id = o.product_id
                WHERE p.tenant_id = :tenantId AND p.id = :productId AND o.id = :optionId
                """).param("id", id).param("tenantId", tenantId).param("productId", productId)
                .param("optionId", optionId).param("label", label).param("code", code).update();
        return new OptionValue(id, label, code);
    }

    public boolean updateOptionValue(UUID tenantId, UUID productId, UUID optionId, UUID id, String label, String code) {
        return jdbc.sql("""
                UPDATE product_option_values v SET label = :label, code = :code FROM products p
                WHERE v.product_id = p.id AND p.tenant_id = :tenantId AND p.id = :productId
                    AND v.option_id = :optionId AND v.id = :id
                """).param("tenantId", tenantId).param("productId", productId).param("optionId", optionId)
                .param("id", id).param("label", label).param("code", code).update() == 1;
    }

    public List<Option> listOptions(UUID tenantId, UUID productId) {
        var values = jdbc.sql("""
                SELECT v.id, v.option_id, v.label, v.code
                FROM product_option_values v JOIN products p ON p.id = v.product_id
                WHERE p.tenant_id = :tenantId AND p.id = :productId ORDER BY v.id
                """).param("tenantId", tenantId).param("productId", productId)
                .query(CatalogRepository::readOptionValue).list().stream()
                .collect(Collectors.groupingBy(SelectedValue::optionId,
                        Collectors.mapping(SelectedValue::value, Collectors.toList())));
        return jdbc.sql("""
                SELECT o.id, o.name FROM product_options o JOIN products p ON p.id = o.product_id
                WHERE p.tenant_id = :tenantId AND p.id = :productId ORDER BY o.position
                """).param("tenantId", tenantId).param("productId", productId).query((row, index) -> {
                    UUID id = row.getObject("id", UUID.class);
                    return new Option(id, row.getString("name"), values.getOrDefault(id, List.of()));
                }).list();
    }

    public List<SelectedValue> findSelectedValues(UUID tenantId, UUID productId, Collection<UUID> valueIds) {
        if (valueIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("""
                SELECT v.id, v.option_id, v.label, v.code
                FROM product_option_values v
                JOIN product_options o ON o.product_id = v.product_id AND o.id = v.option_id
                JOIN products p ON p.id = o.product_id
                WHERE p.tenant_id = :tenantId AND p.id = :productId AND v.id IN (:valueIds)
                ORDER BY o.position
                """).param("tenantId", tenantId).param("productId", productId).param("valueIds", valueIds)
                .query(CatalogRepository::readOptionValue).list();
    }

    private static SelectedValue readOptionValue(ResultSet row, int index) throws SQLException {
        return new SelectedValue(row.getObject("option_id", UUID.class),
                new OptionValue(row.getObject("id", UUID.class), row.getString("label"), row.getString("code")));
    }

    public record SelectedValue(UUID optionId, OptionValue value) {
    }

    public record Product(UUID id, String name, String description, Long priceIdr, String productCode,
                          ProductStatus status, UUID primaryCategoryId) {
    }
}
