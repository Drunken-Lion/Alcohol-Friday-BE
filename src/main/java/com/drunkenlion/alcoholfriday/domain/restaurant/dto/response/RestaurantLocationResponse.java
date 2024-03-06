package com.drunkenlion.alcoholfriday.domain.restaurant.dto.response;


import com.drunkenlion.alcoholfriday.domain.restaurant.entity.Restaurant;
import com.drunkenlion.alcoholfriday.domain.restaurant.vo.TimeData;
import com.drunkenlion.alcoholfriday.global.ncp.dto.NcpFileResponse;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.locationtech.jts.geom.Point;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "전체 레스토랑 조회 항목")
public class RestaurantLocationResponse {

    @Schema(description = "레스토랑 고유아이디")
    private Long id;

    @Schema(description = "회원 고유아이디")
    private Long memberId;

    @Schema(description = "매장 카테고리")
    private String category;

    @Schema(description = "매장 이름")
    private String name;

    @Schema(description = "매장 주소")
    private String address;

    @Schema(description = "매장 위치(위도)")
    private Double latitude;

    @Schema(description = "매장 위치(경도)")
    private Double longitude;

    @Schema(description = "매장 연락처")
    private Long contact;

    @Schema(description = "매장 목록")
    private Map<String, Object> menu;

    @Schema(description = "영업 시간")
    private Map<String, Object> time;

    @Schema(description = "편의시설 목록")
    private Map<String, Object> provision;

    @Schema(description = "상품에 대한 상세 정보")
    private List<RestaurantStockResponse> stockResponses;

    @Schema(description = "레스토랑 영업여부")
    private String status;

    @Schema(description = "상품에 포함된 이미지")
    private List<NcpFileResponse> files;

    public void setRestaurantStatus(String status) {
        this.status = status;
    }

    public static RestaurantLocationResponse of(Restaurant restaurant, List<NcpFileResponse> files) {
        List<RestaurantStockResponse> collect = restaurant
                .getRestaurantStocks()
                .stream()
                .map(RestaurantStockResponse::of)
                .collect(Collectors.toList());

        return RestaurantLocationResponse.builder()
                .id(restaurant.getId())
                .memberId(restaurant.getMembers().getId())
                .category(restaurant.getCategory())
                .name(restaurant.getName())
                .address(restaurant.getAddress())
                .latitude(restaurant.getLocation().getY())
                .longitude(restaurant.getLocation().getX())
                .contact(restaurant.getContact())
                .menu(restaurant.getMenu())
                .time(restaurant.getTime())
                .provision(restaurant.getProvision())
                .stockResponses(collect)
                .files(files)
                .build();

    }
}
