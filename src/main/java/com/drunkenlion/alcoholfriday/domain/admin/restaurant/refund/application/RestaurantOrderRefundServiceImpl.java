package com.drunkenlion.alcoholfriday.domain.admin.restaurant.refund.application;

import com.drunkenlion.alcoholfriday.domain.admin.restaurant.order.dao.RestaurantOrderRepository;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.order.entity.RestaurantOrder;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.order.enumerated.RestaurantOrderStatus;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.refund.dao.RestaurantOrderRefundDetailRepository;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.refund.dao.RestaurantOrderRefundRepository;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.refund.dto.request.RestaurantOrderRefundCreateRequest;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.refund.dto.request.RestaurantOrderRefundDetailCreateRequest;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.refund.dto.response.RestaurantOrderRefundDetailResponse;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.refund.dto.response.RestaurantOrderRefundResponse;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.refund.dto.response.RestaurantOwnerOrderRefundCancelResponse;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.refund.entity.RestaurantOrderRefund;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.refund.entity.RestaurantOrderRefundDetail;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.refund.enumerated.RestaurantOrderRefundStatus;
import com.drunkenlion.alcoholfriday.domain.product.dao.ProductRepository;
import com.drunkenlion.alcoholfriday.domain.product.entity.Product;
import com.drunkenlion.alcoholfriday.domain.restaurant.dao.RestaurantStockRepository;
import com.drunkenlion.alcoholfriday.domain.restaurant.entity.RestaurantStock;
import com.drunkenlion.alcoholfriday.global.common.response.HttpResponse;
import com.drunkenlion.alcoholfriday.global.exception.BusinessException;
import com.drunkenlion.alcoholfriday.global.file.application.FileService;
import com.drunkenlion.alcoholfriday.global.ncp.dto.NcpFileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RestaurantOrderRefundServiceImpl implements RestaurantOrderRefundService {
    private final RestaurantOrderRefundRepository restaurantOrderRefundRepository;
    private final RestaurantOrderRefundDetailRepository restaurantOrderRefundDetailRepository;
    private final ProductRepository productRepository;
    private final RestaurantOrderRepository restaurantOrderRepository;
    private final RestaurantStockRepository restaurantStockRepository;
    private final FileService fileService;

    @Override
    public Page<RestaurantOrderRefundResponse> getRestaurantOrderRefunds(Long restaurantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RestaurantOrderRefund> refundPage = restaurantOrderRefundRepository.findByRestaurantIdAndDeletedAtIsNull(restaurantId, pageable);

        List<RestaurantOrderRefundResponse> refundResponses = refundPage.getContent().stream()
                .map(refund -> {
                    List<RestaurantOrderRefundDetail> refundDetails = restaurantOrderRefundDetailRepository
                            .findByRestaurantOrderRefundAndDeletedAtIsNull(refund);
                    List<RestaurantOrderRefundDetailResponse> refundDetailResponses = refundDetails.stream()
                            .map(refundDetail -> {
                                NcpFileResponse file = fileService.findOne(refundDetail.getProduct());
                                return RestaurantOrderRefundDetailResponse.of(refundDetail, file);
                            })
                            .collect(Collectors.toList());

                    return RestaurantOrderRefundResponse.of(refund, refundDetailResponses);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(refundResponses, pageable, refundPage.getTotalElements());
    }

    @Override
    @Transactional
    public RestaurantOrderRefundResponse createRestaurantOrderRefund(RestaurantOrderRefundCreateRequest request) {
        if (!checkRefundEligibility(request)) {
            throw BusinessException.builder()
                    .response(HttpResponse.Fail.RESTAURANT_REFUND_FAIL)
                    .build();
        }

        RestaurantOrder order = restaurantOrderRepository.findByIdAndDeletedAtIsNull(request.getOrderId())
                .orElseThrow(() -> BusinessException.builder()
                        .response(HttpResponse.Fail.NOT_FOUND_ORDER)
                        .build());

        BigDecimal totalPrice = request.getRefundDetails().stream()
                .map(detailRequest -> detailRequest.getPrice().multiply(BigDecimal.valueOf(detailRequest.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        RestaurantOrderRefund refund = RestaurantOrderRefundCreateRequest.toEntity(request, order, totalPrice);
        restaurantOrderRefundRepository.save(refund);

        List<RestaurantOrderRefundDetailResponse> refundDetailResponses = new ArrayList<>();
        List<RestaurantOrderRefundDetail> refundDetails = new ArrayList<>();
        List<RestaurantStock> stocks = new ArrayList<>();

        for (RestaurantOrderRefundDetailCreateRequest detailRequest : request.getRefundDetails()) {
            Product product = productRepository.findByIdAndDeletedAtIsNull(detailRequest.getProductId())
                    .orElseThrow(() -> BusinessException.builder()
                            .response(HttpResponse.Fail.NOT_FOUND_PRODUCT)
                            .build());

            RestaurantOrderRefundDetail refundDetail = RestaurantOrderRefundDetailCreateRequest.toEntity(detailRequest, refund, product);
            refundDetails.add(refundDetail);

            NcpFileResponse file = fileService.findOne(refundDetail.getProduct());
            refundDetailResponses.add(RestaurantOrderRefundDetailResponse.of(refundDetail, file));

            RestaurantStock restaurantStock = restaurantStockRepository
                    .findByRestaurantIdAndProductIdAndDeletedAtIsNull(request.getRestaurantId(), detailRequest.getProductId())
                    .orElseThrow(() -> BusinessException.builder()
                            .response(HttpResponse.Fail.NOT_FOUND_RESTAURANT_STOCK)
                            .build());

            // 환불 요청 시 매장의 재고가 마이너스 된다.
            restaurantStock.minusQuantity(detailRequest.getQuantity());
            stocks.add(restaurantStock);
        }

        restaurantOrderRefundDetailRepository.saveAll(refundDetails);
        restaurantStockRepository.saveAll(stocks);

        return RestaurantOrderRefundResponse.of(refund, refundDetailResponses);
    }

    @Override
    @Transactional
    public RestaurantOwnerOrderRefundCancelResponse cancelRestaurantOrderRefund(Long id) {
        RestaurantOrderRefund refund = restaurantOrderRefundRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BusinessException.builder()
                        .response(HttpResponse.Fail.NOT_FOUND_RESTAURANT_REFUND)
                        .build());

        // 환불 승인 대기 이외에는 환불 취소 불가
        if (!refund.getStatus().equals(RestaurantOrderRefundStatus.WAITING_APPROVAL)) {
            throw BusinessException.builder()
                    .response(HttpResponse.Fail.RESTAURANT_REFUND_CANCEL_FAIL)
                    .build();
        }

        List<RestaurantOrderRefundDetail> refundDetails = restaurantOrderRefundDetailRepository.findByRestaurantOrderRefundAndDeletedAtIsNull(refund);

        if (refundDetails.isEmpty()) {
            throw BusinessException.builder()
                    .response(HttpResponse.Fail.NOT_FOUND_RESTAURANT_REFUND_DETAIL)
                    .build();
        }

        List<RestaurantStock> stocks = new ArrayList<>();
        for (RestaurantOrderRefundDetail refundDetail : refundDetails) {
            RestaurantStock restaurantStock = restaurantStockRepository
                    .findByRestaurantIdAndProductIdAndDeletedAtIsNull(refund.getRestaurant().getId(), refundDetail.getProduct().getId())
                    .orElseThrow(() -> BusinessException.builder()
                            .response(HttpResponse.Fail.NOT_FOUND_RESTAURANT_STOCK)
                            .build());

            // 환불 취소 시 매장 재고가 다시 플러스 된다.
            restaurantStock.plusQuantity(refundDetail.getQuantity());
            stocks.add(restaurantStock);
        }

        restaurantStockRepository.saveAll(stocks);

        // 환불 상태 취소로 변경
        refund = refund.toBuilder()
                .status(RestaurantOrderRefundStatus.CANCELLED)
                .build();

        restaurantOrderRefundRepository.save(refund);

        return RestaurantOwnerOrderRefundCancelResponse.of(refund);
    }

    private boolean checkRefundEligibility(RestaurantOrderRefundCreateRequest request) {
        // 1. 발주 완료 이외의 상태는 환불 불가
        if (!request.getStatus().equals(RestaurantOrderStatus.COMPLETED)) {
            return false;
        }

        // 2. 환불은 발주 일자로 부터 7일이 넘으면 환불 불가
        if (request.getOrderDate().plusDays(7).isBefore(LocalDateTime.now())) {
            return false;
        }

        // 3. 환불 제품이 존재하지 않으면 환불 불가
        if (request.getRefundDetails() == null || request.getRefundDetails().isEmpty()) {
            return false;
        }

        // 4. 해당 주문에 진행 중인 환불이 있으면 환불 불가
        boolean hasWaitingRefund = restaurantOrderRefundRepository
                .existsByRestaurantOrderIdAndStatusAndDeletedAtIsNull(request.getOrderId(), RestaurantOrderRefundStatus.WAITING_APPROVAL);

        if (hasWaitingRefund) {
            return false;
        }

        // 5. 환불 요청의 환불 개수가 0개 이면 환불 불가
        for (RestaurantOrderRefundDetailCreateRequest detailRequest : request.getRefundDetails()) {
            if (detailRequest.getQuantity() <= 0) {
                return false;
            }
        }

        // 6. 주문의 환불 가능한 재고 보다 환불 개수가 많으면 환불 불가
        for (RestaurantOrderRefundDetailCreateRequest detailRequest : request.getRefundDetails()) {
            if (detailRequest.getQuantity() > detailRequest.getPossibleQuantity()) {
                return false;
            }
        }

        // 7. 매장의 재고 보다 환불 개수가 많으면 환불 불가
        for (RestaurantOrderRefundDetailCreateRequest detailRequest : request.getRefundDetails()) {
            RestaurantStock restaurantStock = restaurantStockRepository
                    .findByRestaurantIdAndProductIdAndDeletedAtIsNull(request.getRestaurantId(), detailRequest.getProductId())
                    .orElseThrow(() -> BusinessException.builder()
                            .response(HttpResponse.Fail.NOT_FOUND_RESTAURANT_STOCK)
                            .build());

            if (detailRequest.getQuantity() > restaurantStock.getQuantity()) {
                return false;
            }
        }

        return true;
    }
}
