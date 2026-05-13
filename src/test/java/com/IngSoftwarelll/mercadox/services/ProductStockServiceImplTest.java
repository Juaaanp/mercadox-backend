package com.IngSoftwarelll.mercadox.services;

import com.IngSoftwarelll.mercadox.dtos.products.requests.CreateProductStockRequestDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.BulkStockResponseDTO;
import com.IngSoftwarelll.mercadox.dtos.products.responses.ProductStockResponseDTO;
import com.IngSoftwarelll.mercadox.exceptions.ResourceNotFoundException;
import com.IngSoftwarelll.mercadox.mappers.ProductStockMapper;
import com.IngSoftwarelll.mercadox.models.Product;
import com.IngSoftwarelll.mercadox.models.ProductStock;
import com.IngSoftwarelll.mercadox.models.enums.StockStatus;
import com.IngSoftwarelll.mercadox.repositories.ProductRepository;
import com.IngSoftwarelll.mercadox.repositories.ProductStockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para ProductStockServiceImpl.
 *
 * Casos cubiertos:
 *   getProductStock() — listado con filtros (búsqueda, status, ambos, ninguno)
 *                     — producto no pertenece al admin → ResourceNotFoundException
 *   deleteStockItem() — eliminación exitosa de código AVAILABLE
 *                     — intento de eliminar código SOLD → IllegalStateException
 *                     — código no encontrado → ResourceNotFoundException
 *   addBulkStockItems() — todos los códigos agregados exitosamente
 *                       — código duplicado contabilizado en 'failed'
 *                       — producto no pertenece al admin → ResourceNotFoundException
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductStockServiceImpl — pruebas unitarias")
class ProductStockServiceImplTest {

    @Mock private ProductStockRepository productStockRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductStockMapper productStockMapper;

    @InjectMocks
    private ProductStockServiceImpl productStockService;

    // ─── helpers ──────────────────────────────────────────────

    private Product sampleProduct(Long productId, Long adminId) {
        Product p = new Product();
        p.setId(productId);
        return p;
    }

    private ProductStock stockItem(Long id, StockStatus status) {
        ProductStock s = new ProductStock();
        s.setId(id);
        s.setCode("CODE-" + id);
        s.setStatus(status);
        return s;
    }

    private CreateProductStockRequestDTO stockRequest(String code) {
        CreateProductStockRequestDTO req = mock(CreateProductStockRequestDTO.class);
        when(req.getCode()).thenReturn(code);
        return req;
    }

    // ════════════════════════════════════════════════════════════
    // BLOQUE 1 — getProductStock()
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getProductStock()")
    class GetProductStockTests {

        @Test
        @DisplayName("producto pertenece al admin — sin filtros retorna toda la página")
        void getProductStock_noFilters_returnsAllStock() {
            Pageable pageable = PageRequest.of(0, 10);
            ProductStock s = stockItem(1L, StockStatus.AVAILABLE);
            ProductStockResponseDTO dto = mock(ProductStockResponseDTO.class);

            when(productRepository.findByIdAndAdminId(1L, 99L))
                    .thenReturn(Optional.of(sampleProduct(1L, 99L)));
            when(productStockRepository.findByProductId(1L, pageable))
                    .thenReturn(new PageImpl<>(List.of(s)));
            when(productStockMapper.toProductStockResponseDTO(s)).thenReturn(dto);

            Page<ProductStockResponseDTO> result =
                    productStockService.getProductStock(1L, 99L, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("filtro por search y status aplica el repositorio correcto")
        void getProductStock_withSearchAndStatus_callsCorrectRepository() {
            Pageable pageable = PageRequest.of(0, 10);

            when(productRepository.findByIdAndAdminId(1L, 99L))
                    .thenReturn(Optional.of(sampleProduct(1L, 99L)));
            when(productStockRepository
                    .findByProductIdAndCodeContainingIgnoreCaseAndStatus(1L, "STEAM", StockStatus.AVAILABLE, pageable))
                    .thenReturn(Page.empty());

            Page<ProductStockResponseDTO> result =
                    productStockService.getProductStock(1L, 99L, "STEAM", StockStatus.AVAILABLE, pageable);

            verify(productStockRepository)
                    .findByProductIdAndCodeContainingIgnoreCaseAndStatus(1L, "STEAM", StockStatus.AVAILABLE, pageable);
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("filtro solo por search llama al repositorio de búsqueda por código")
        void getProductStock_withSearchOnly_callsSearchRepository() {
            Pageable pageable = PageRequest.of(0, 10);

            when(productRepository.findByIdAndAdminId(1L, 99L))
                    .thenReturn(Optional.of(sampleProduct(1L, 99L)));
            when(productStockRepository.findByProductIdAndCodeContainingIgnoreCase(1L, "EA", pageable))
                    .thenReturn(Page.empty());

            productStockService.getProductStock(1L, 99L, "EA", null, pageable);

            verify(productStockRepository).findByProductIdAndCodeContainingIgnoreCase(1L, "EA", pageable);
        }

        @Test
        @DisplayName("filtro solo por status llama al repositorio de status")
        void getProductStock_withStatusOnly_callsStatusRepository() {
            Pageable pageable = PageRequest.of(0, 10);

            when(productRepository.findByIdAndAdminId(1L, 99L))
                    .thenReturn(Optional.of(sampleProduct(1L, 99L)));
            when(productStockRepository.findByProductIdAndStatus(1L, StockStatus.SOLD, pageable))
                    .thenReturn(Page.empty());

            productStockService.getProductStock(1L, 99L, null, StockStatus.SOLD, pageable);

            verify(productStockRepository).findByProductIdAndStatus(1L, StockStatus.SOLD, pageable);
        }

        @Test
        @DisplayName("producto no pertenece al admin lanza ResourceNotFoundException")
        void getProductStock_productNotOwnedByAdmin_throwsResourceNotFound() {
            when(productRepository.findByIdAndAdminId(1L, 99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    productStockService.getProductStock(1L, 99L, null, null, PageRequest.of(0, 10)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("1");
        }
    }

    // ════════════════════════════════════════════════════════════
    // BLOQUE 2 — deleteStockItem()
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("deleteStockItem()")
    class DeleteStockItemTests {

        @Test
        @DisplayName("código AVAILABLE puede eliminarse correctamente")
        void deleteStockItem_withAvailableCode_deletesSuccessfully() {
            ProductStock stock = stockItem(10L, StockStatus.AVAILABLE);

            when(productStockRepository.findByIdAndProductIdAndProductAdminId(10L, 1L, 99L))
                    .thenReturn(Optional.of(stock));

            productStockService.deleteStockItem(1L, 10L, 99L);

            verify(productStockRepository).delete(stock);
        }

        @Test
        @DisplayName("código SOLD lanza IllegalStateException — no puede eliminarse")
        void deleteStockItem_withSoldCode_throwsIllegalState() {
            ProductStock stock = stockItem(20L, StockStatus.SOLD);

            when(productStockRepository.findByIdAndProductIdAndProductAdminId(20L, 1L, 99L))
                    .thenReturn(Optional.of(stock));

            assertThatThrownBy(() -> productStockService.deleteStockItem(1L, 20L, 99L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("sold");

            verify(productStockRepository, never()).delete(any());
        }

        @Test
        @DisplayName("código no encontrado para este admin/producto lanza ResourceNotFoundException")
        void deleteStockItem_notFound_throwsResourceNotFound() {
            when(productStockRepository.findByIdAndProductIdAndProductAdminId(99L, 1L, 99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> productStockService.deleteStockItem(1L, 99L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(productStockRepository, never()).delete(any());
        }
    }

    // ════════════════════════════════════════════════════════════
    // BLOQUE 3 — addBulkStockItems()
    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("addBulkStockItems()")
    class AddBulkStockItemsTests {

        @Test
        @DisplayName("todos los códigos nuevos se agregan y successful = total enviado")
        void addBulkStockItems_allNew_allSuccessful() {
            Product product = sampleProduct(1L, 99L);
            List<CreateProductStockRequestDTO> requests = List.of(
                    stockRequest("CODE-A1"),
                    stockRequest("CODE-B2"),
                    stockRequest("CODE-C3")
            );

            when(productRepository.findByIdAndAdminId(1L, 99L)).thenReturn(Optional.of(product));
            when(productStockRepository.existsByProductIdAndCode(eq(1L), anyString())).thenReturn(false);
            when(productStockMapper.toProductStock(any(CreateProductStockRequestDTO.class)))
                    .thenReturn(new ProductStock());

            BulkStockResponseDTO result = productStockService.addBulkStockItems(1L, 99L, requests);

            assertThat(result.getTotalProcessed()).isEqualTo(3);
            assertThat(result.getSuccessfullyAdded()).isEqualTo(3);
            assertThat(result.getFailed()).isZero();
            assertThat(result.getErrors()).isEmpty();

            // Se debe llamar a save por cada código nuevo
            verify(productStockRepository, times(3)).save(any(ProductStock.class));
        }

        @Test
        @DisplayName("código duplicado se contabiliza en 'failed' y se reporta el error")
        void addBulkStockItems_withDuplicate_countedAsFailed() {
            Product product = sampleProduct(1L, 99L);
            List<CreateProductStockRequestDTO> requests = List.of(
                    stockRequest("CODE-NEW"),
                    stockRequest("CODE-DUP")
            );

            when(productRepository.findByIdAndAdminId(1L, 99L)).thenReturn(Optional.of(product));
            // CODE-NEW: nuevo; CODE-DUP: ya existe
            when(productStockRepository.existsByProductIdAndCode(1L, "CODE-NEW")).thenReturn(false);
            when(productStockRepository.existsByProductIdAndCode(1L, "CODE-DUP")).thenReturn(true);
            when(productStockMapper.toProductStock(any(CreateProductStockRequestDTO.class)))
                    .thenReturn(new ProductStock());

            BulkStockResponseDTO result = productStockService.addBulkStockItems(1L, 99L, requests);

            assertThat(result.getTotalProcessed()).isEqualTo(2);
            assertThat(result.getSuccessfullyAdded()).isEqualTo(1);
            assertThat(result.getFailed()).isEqualTo(1);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).containsIgnoringCase("CODE-DUP");
        }

        @Test
        @DisplayName("producto no pertenece al admin lanza ResourceNotFoundException")
        void addBulkStockItems_productNotOwnedByAdmin_throwsResourceNotFound() {
            when(productRepository.findByIdAndAdminId(1L, 99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    productStockService.addBulkStockItems(1L, 99L, List.of()))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(productStockRepository, never()).save(any());
        }

        @Test
        @DisplayName("lista vacía de requests retorna BulkStockResponseDTO con totales en 0")
        void addBulkStockItems_emptyList_returnsZeroTotals() {
            Product product = sampleProduct(1L, 99L);
            when(productRepository.findByIdAndAdminId(1L, 99L)).thenReturn(Optional.of(product));

            BulkStockResponseDTO result = productStockService.addBulkStockItems(1L, 99L, List.of());

            assertThat(result.getTotalProcessed()).isZero();
            assertThat(result.getSuccessfullyAdded()).isZero();
            assertThat(result.getFailed()).isZero();
        }
    }
}