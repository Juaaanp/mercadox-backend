package com.IngSoftwarelll.mercadox.services;

import com.IngSoftwarelll.mercadox.dtos.EntityCreatedResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.requests.CreateProductRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductSummaryResponseDTO;
import com.IngSoftwarelll.mercadox.exceptions.ResourceNotFoundException;
import com.IngSoftwarelll.mercadox.exceptions.UnauthorizedException;
import com.IngSoftwarelll.mercadox.mappers.ProductMapper;
import com.IngSoftwarelll.mercadox.models.Product;
import com.IngSoftwarelll.mercadox.models.ProductCategory;
import com.IngSoftwarelll.mercadox.models.User;
import com.IngSoftwarelll.mercadox.models.enums.UserRole;
import com.IngSoftwarelll.mercadox.repositories.ProductRepository;
import com.IngSoftwarelll.mercadox.services.interfaces.ProductCategoryService;
import com.IngSoftwarelll.mercadox.services.interfaces.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para ProductServiceImpl.
 *
 * Casos cubiertos:
 *   CP-12 — getAllProducts() retorna página con estructura paginada
 *   CP-13 — filterProducts() filtra correctamente
 *   CP-14 — detailProduct() retorna vista detallada de producto existente
 *   CP-15 — detailProduct() / getById() lanza 404 para producto inexistente
 *           Admin puede crear producto
 *           No-admin no puede crear producto (UnauthorizedException)
 *           Admin puede eliminar producto
 *           No-admin no puede eliminar producto
 *           getById() con id inválida lanza IllegalArgumentException
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl — pruebas unitarias")
class ProductServiceImplTest {

    @Mock private UserService userService;
    @Mock private ProductCategoryService productCategoryService;
    @Mock private ProductRepository productRepository;
    @Mock private ProductMapper productMapper;
    @Mock private CloudinaryService cloudinaryService;

    @InjectMocks
    private ProductServiceImpl productService;

    // ─── helpers ──────────────────────────────────────────────
    private User adminUser() {
        User u = new User();
        u.setId(1L);
        u.setRole(UserRole.ADMIN);
        return u;
    }

    private User consumerUser() {
        User u = new User();
        u.setId(2L);
        u.setRole(UserRole.CONSUMER);
        return u;
    }

    private Product sampleProduct(Long id) {
        Product p = new Product();
        p.setId(id);
        p.setName("Adobe Creative Cloud");
        p.setPrice(new BigDecimal("150000"));
        return p;
    }

    // ════════════════════════════════════════════════════════════
    // BLOQUE 1 — createProduct()
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("createProduct()")
    class CreateProductTests {

        @Test
        @DisplayName("admin puede crear un producto correctamente")
        void createProduct_withAdmin_returnsEntityCreatedResponse() {
            Long adminId = 1L;
            CreateProductRequestDTO request = mock(CreateProductRequestDTO.class);
            when(request.getProductCategoryId()).thenReturn(10L);

            MultipartFile image = mock(MultipartFile.class);
            Product product = sampleProduct(null);
            product.setId(99L);

            when(userService.getUserById(adminId)).thenReturn(adminUser());
            when(cloudinaryService.uploadImage(image)).thenReturn("https://cdn.img/photo.jpg");
            when(productMapper.toProduct(request)).thenReturn(product);
            when(productCategoryService.getById(10L)).thenReturn(new ProductCategory());

            EntityCreatedResponseDTO response = productService.createProduct(adminId, request, image);

            assertThat(response.getId()).isEqualTo(99L);
            assertThat(response.getMessage()).containsIgnoringCase("created");
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("no-admin lanza UnauthorizedException al crear producto")
        void createProduct_withConsumer_throwsUnauthorizedException() {
            Long consumerId = 2L;
            when(userService.getUserById(consumerId)).thenReturn(consumerUser());

            assertThatThrownBy(() ->
                    productService.createProduct(consumerId, mock(CreateProductRequestDTO.class), mock(MultipartFile.class)))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("admins");

            verify(productRepository, never()).save(any());
        }
    }

    // ════════════════════════════════════════════════════════════
    // BLOQUE 2 — deleteProduct()
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("deleteProduct()")
    class DeleteProductTests {

        @Test
        @DisplayName("admin puede eliminar un producto por id")
        void deleteProduct_withAdmin_deletesCorrectly() {
            when(userService.getUserById(1L)).thenReturn(adminUser());

            productService.deleteProduct(1L, 42L);

            verify(productRepository).deleteById(42L);
        }

        @Test
        @DisplayName("no-admin lanza UnauthorizedException al eliminar producto")
        void deleteProduct_withConsumer_throwsUnauthorizedException() {
            when(userService.getUserById(2L)).thenReturn(consumerUser());

            assertThatThrownBy(() -> productService.deleteProduct(2L, 42L))
                    .isInstanceOf(UnauthorizedException.class);

            verify(productRepository, never()).deleteById(any());
        }
    }

    // ════════════════════════════════════════════════════════════
    // BLOQUE 3 — getById()
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("CP-14 — retorna producto cuando el id es válido y existe")
        void getById_withExistingId_returnsProduct() {
            Product product = sampleProduct(5L);
            when(productRepository.findById(5L)).thenReturn(Optional.of(product));

            Product result = productService.getById(5L);

            assertThat(result.getId()).isEqualTo(5L);
        }

        /**
         * CP-15: producto inexistente lanza ResourceNotFoundException.
         */
        @Test
        @DisplayName("CP-15 — id inexistente lanza ResourceNotFoundException")
        void getById_withNonExistingId_throwsResourceNotFoundException() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("id null lanza IllegalArgumentException")
        void getById_withNullId_throwsIllegalArgument() {
            assertThatThrownBy(() -> productService.getById(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("id negativo lanza IllegalArgumentException")
        void getById_withNegativeId_throwsIllegalArgument() {
            assertThatThrownBy(() -> productService.getById(-1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ════════════════════════════════════════════════════════════
    // BLOQUE 4 — getAllProducts()
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getAllProducts()")
    class GetAllProductsTests {

        /**
         * CP-12: estructura paginada correcta.
         */
        @Test
        @DisplayName("CP-12 — retorna página de productos con estructura paginada")
        void getAllProducts_returnsPaginatedPage() {
            Product p = sampleProduct(1L);
            ProductSummaryResponseDTO dto = mock(ProductSummaryResponseDTO.class);

            Page<Product> productPage = new PageImpl<>(List.of(p));
            when(productRepository.findAllProducts(any(Pageable.class))).thenReturn(productPage);
            when(productMapper.toProductSummaryResponseDTO(p)).thenReturn(dto);

            Page<ProductSummaryResponseDTO> result = productService.getAllProducts(0);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(dto);
        }

        @Test
        @DisplayName("CP-12 — página vacía no lanza excepción")
        void getAllProducts_emptyPage_returnsEmptyPageGracefully() {
            when(productRepository.findAllProducts(any(Pageable.class)))
                    .thenReturn(Page.empty());

            Page<ProductSummaryResponseDTO> result = productService.getAllProducts(0);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ════════════════════════════════════════════════════════════
    // BLOQUE 5 — filterProducts()
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("filterProducts()")
    class FilterProductsTests {

        /**
         * CP-13: filtrado por nombre, categoría y precio máximo.
         */
        @Test
        @DisplayName("CP-13 — filtra correctamente por searchQuery, categoryId y maxPrice")
        void filterProducts_withAllFilters_returnsFilteredPage() {
            Product p = sampleProduct(1L);
            ProductSummaryResponseDTO dto = mock(ProductSummaryResponseDTO.class);

            Page<Product> productPage = new PageImpl<>(List.of(p));

            when(productRepository.searchProducts(
                    eq("steam"), eq(1L), eq(new BigDecimal("50000")), any(Pageable.class)))
                    .thenReturn(productPage);
            when(productMapper.toProductSummaryResponseDTO(p)).thenReturn(dto);

            Page<ProductSummaryResponseDTO> result =
                    productService.filterProducts("steam", 1L, new BigDecimal("50000"), 0, "price", "asc");

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("sortBy inválido usa 'createdAt' por defecto")
        void filterProducts_withInvalidSortBy_defaultsToCreatedAt() {
            when(productRepository.searchProducts(any(), any(), any(), any()))
                    .thenReturn(Page.empty());

            // No debe lanzar excepción con campo de ordenamiento no permitido
            Page<ProductSummaryResponseDTO> result =
                    productService.filterProducts(null, null, null, 0, "nombre_invalido", "desc");

            assertThat(result).isNotNull();
        }
    }

    // ════════════════════════════════════════════════════════════
    // BLOQUE 6 — detailProduct()
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("detailProduct()")
    class DetailProductTests {

        @Test
        @DisplayName("CP-14 — retorna ProductResponseDTO para producto existente")
        void detailProduct_withExistingId_returnsDTO() {
            Product product = sampleProduct(7L);
            ProductResponseDTO dto = mock(ProductResponseDTO.class);

            when(productRepository.findById(7L)).thenReturn(Optional.of(product));
            when(productMapper.toProductResponseDTO(product)).thenReturn(dto);

            ProductResponseDTO result = productService.detailProduct(7L);

            assertThat(result).isEqualTo(dto);
        }

        @Test
        @DisplayName("CP-15 — id inexistente lanza ResourceNotFoundException")
        void detailProduct_withNonExistingId_throwsResourceNotFoundException() {
            when(productRepository.findById(888L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.detailProduct(888L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("888");
        }

        @Test
        @DisplayName("id cero lanza IllegalArgumentException")
        void detailProduct_withZeroId_throwsIllegalArgument() {
            assertThatThrownBy(() -> productService.detailProduct(0L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
