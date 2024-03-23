package com.drunkenlion.alcoholfriday.domain.admin.restaurant.order.dto.response;

import com.drunkenlion.alcoholfriday.domain.restaurant.order.entity.RestaurantOrder;
import com.drunkenlion.alcoholfriday.domain.restaurant.order.enumerated.RestaurantOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "관리자 레스토랑 발주 취소 반환 객체")
public class RestaurantOwnerOrderCancelResponse {
    @Schema(description = "레스토랑 발주 고유 식별 ID")
    private Long id;

    @Schema(description = "레스토랑 사업자 이름")
    private String businessName;

    @Schema(description = "발주 상태")
    private RestaurantOrderStatus status;

    public static RestaurantOwnerOrderCancelResponse of(RestaurantOrder order) {
        return RestaurantOwnerOrderCancelResponse.builder()
                .id(order.getId())
                .businessName(order.getRestaurant().getBusinessName())
                .status(order.getOrderStatus())
                .build();
    }
}
