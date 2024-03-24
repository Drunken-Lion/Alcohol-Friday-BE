package com.drunkenlion.alcoholfriday.domain.admin.restaurant.cart.application;

import com.drunkenlion.alcoholfriday.domain.admin.restaurant.cart.dao.RestaurantOrderCartDetailRepository;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.cart.dao.RestaurantOrderCartRepository;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.cart.dto.request.RestaurantOrderCartDeleteRequest;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.cart.dto.request.RestaurantOrderCartSaveRequest;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.cart.dto.request.RestaurantOrderCartUpdateRequest;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.cart.dto.response.RestaurantOrderCartListResponse;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.cart.dto.response.RestaurantOrderCartSaveResponse;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.cart.entity.RestaurantOrderCart;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.cart.entity.RestaurantOrderCartDetail;
import com.drunkenlion.alcoholfriday.domain.admin.restaurant.cart.util.RestaurantOrderCartValidator;
import com.drunkenlion.alcoholfriday.domain.member.entity.Member;
import com.drunkenlion.alcoholfriday.domain.product.dao.ProductRepository;
import com.drunkenlion.alcoholfriday.domain.product.entity.Product;
import com.drunkenlion.alcoholfriday.domain.restaurant.dao.RestaurantRepository;
import com.drunkenlion.alcoholfriday.domain.restaurant.entity.Restaurant;
import com.drunkenlion.alcoholfriday.global.common.response.HttpResponse;
import com.drunkenlion.alcoholfriday.global.common.response.HttpResponse.Fail;
import com.drunkenlion.alcoholfriday.global.exception.BusinessException;
import com.drunkenlion.alcoholfriday.global.file.application.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class RestaurantOrderCartServiceImpl implements RestaurantOrderCartService {
    private final ProductRepository productRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantOrderCartRepository restaurantOrderCartRepository;
    private final RestaurantOrderCartDetailRepository restaurantOrderCartDetailRepository;
    private final FileService fileService;

    /**
     * 장바구니 목록 조회
     */
    @Override
    public Page<RestaurantOrderCartListResponse> findRestaurantCart(Long restaurantId, Member member, int page, int size) {
        RestaurantOrderCartValidator.checkedMemberRoleIsOwner(member);

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
                .orElseThrow(() -> new BusinessException(Fail.NOT_FOUND_RESTAURANT));

        RestaurantOrderCartValidator.checkedMemberInRestaurant(restaurant, member);

        restaurantOrderCartRepository.findRestaurantAndMember(restaurant, member)
                .orElseThrow(() -> new BusinessException(Fail.NOT_FOUND_RESTAURANT_ORDER_CART_DETAIL));

        Pageable pageable = PageRequest.of(page, size);
        Page<RestaurantOrderCartDetail> findAll = restaurantOrderCartDetailRepository.findRestaurantAndMember(restaurant, member, pageable);

        return findAll.map(cartDetail -> RestaurantOrderCartListResponse.of(cartDetail, fileService.findOne(cartDetail.getProduct())));
    }

    /**
     * 장바구니 제품 추가
     */
    @Override
    @Transactional
    public RestaurantOrderCartSaveResponse saveRestaurantOrderCart(RestaurantOrderCartSaveRequest request,
                                                                   Member member) {
        RestaurantOrderCartValidator.checkedMemberRoleIsOwner(member);

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(request.getRestaurantId())
                .orElseThrow(() -> new BusinessException(HttpResponse.Fail.NOT_FOUND_RESTAURANT));

        RestaurantOrderCart cart = restaurantOrderCartRepository.findRestaurantAndMember(restaurant, member)
                .orElseGet(() ->
                        restaurantOrderCartRepository.save(RestaurantOrderCart.builder()
                                .member(member)
                                .restaurant(restaurant)
                                .build()));

        Product product = productRepository.findByIdAndDeletedAtIsNull(request.getProductId())
                .orElseThrow(() -> new BusinessException(Fail.NOT_FOUND_PRODUCT));

        RestaurantOrderCartValidator.checkedQuantity(product, request.getQuantity());

        RestaurantOrderCartDetail cartDetail = restaurantOrderCartDetailRepository.findCartAndProduct(cart, product)
                .orElseGet(() ->
                        restaurantOrderCartDetailRepository.save(RestaurantOrderCartDetail.builder()
                                .product(product)
                                .build())
                );

        cartDetail.plusQuantity(request.getQuantity());
        cartDetail.addCart(cart);
        restaurantOrderCartDetailRepository.save(cartDetail);

        return RestaurantOrderCartSaveResponse.of(cartDetail);
    }

    /**
     * 장바구니 제품 수량 변경
     */
    @Override
    @Transactional
    public RestaurantOrderCartSaveResponse updateRestaurantOrderCart(Long id,
                                                                     RestaurantOrderCartUpdateRequest request,
                                                                     Member member) {
        RestaurantOrderCartValidator.checkedMemberRoleIsOwner(member);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(HttpResponse.Fail.NOT_FOUND_PRODUCT));

        RestaurantOrderCart restaurantOrderCart = restaurantOrderCartRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpResponse.Fail.NOT_FOUND_RESTAURANT_ORDER_CART));

        RestaurantOrderCartDetail restaurantOrderCartDetail = restaurantOrderCartDetailRepository.findCartAndProduct(
                        restaurantOrderCart, product)
                .orElseThrow(() -> new BusinessException(HttpResponse.Fail.NOT_FOUND_RESTAURANT_ORDER_CART));

        RestaurantOrderCartValidator.checkedQuantity(product, request.getQuantity());
        RestaurantOrderCartValidator.minimumQuantity(request.getQuantity());

        restaurantOrderCartDetail.updateQuantity(request.getQuantity());

        return RestaurantOrderCartSaveResponse.of(restaurantOrderCartDetail);
    }

    /**
     * 장바구니 제품 삭제
     */
    @Override
    @Transactional
    public RestaurantOrderCartSaveResponse deleteRestaurantOrderCart(Long id,
                                                                     RestaurantOrderCartDeleteRequest request,
                                                                     Member member) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(HttpResponse.Fail.NOT_FOUND_PRODUCT));

        RestaurantOrderCart restaurantOrderCart = restaurantOrderCartRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpResponse.Fail.NOT_FOUND_RESTAURANT_ORDER_CART));

        RestaurantOrderCartDetail restaurantOrderCartDetail = restaurantOrderCartDetailRepository.findCartAndProduct(
                        restaurantOrderCart, product)
                .orElseThrow(() -> new BusinessException(HttpResponse.Fail.NOT_FOUND_RESTAURANT_ORDER_CART));

        restaurantOrderCartDetail.deleteQuantity(product.getQuantity());

        return RestaurantOrderCartSaveResponse.of(restaurantOrderCartDetail);
    }

//    private RestaurantOrderCartDetail findRestaurantOrderCartDetail(Long cartId, Long productId) {
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new BusinessException(HttpResponse.Fail.NOT_FOUND_PRODUCT));
//
//        RestaurantOrderCart restaurantOrderCart = restaurantOrderCartRepository.findById(cartId)
//                .orElseThrow(() -> new BusinessException(HttpResponse.Fail.NOT_FOUND_RESTAURANT_ORDER_CART));
//
//        return restaurantOrderCartDetailRepository.findCartAndProduct(restaurantOrderCart, product)
//                .orElseThrow(() -> new BusinessException(HttpResponse.Fail.NOT_FOUND_RESTAURANT_ORDER_CART));
//    }
}
