package com.drunkenlion.alcoholfriday.domain.cart.entity;

import com.drunkenlion.alcoholfriday.domain.member.entity.Member;
import com.drunkenlion.alcoholfriday.global.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends BaseEntity {
    @Comment("장바구니 주인")
    @OneToOne(fetch = FetchType.LAZY)
    private Member member;

    @Builder.Default
    @Comment("장바구니에 담긴 상품(들)")
    @OneToMany(mappedBy = "cart")
    @ToString.Exclude
    private List<CartDetail> cartDetails = new ArrayList<>();

    // Member가 첫 장바구니 관련 기능을 사용할 때 추가해 주시면 됩니다.
    public static Cart create(Member member) {
        Cart cart = new Cart();
        cart.createCart(member);
        return cart;
    }

    private void createCart(Member member) {
        this.member = member;
    }

    public BigDecimal getTotalCartPrice(List<CartDetail> cartDetails) {
        if (cartDetails.isEmpty()) {
            return BigDecimal.ZERO;
        } else {
            return cartDetails.stream()
                    .map(cartDetail -> cartDetail.getItem().getPrice())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    public Long getTotalCartQuantity(List<CartDetail> cartDetails) {
        if (cartDetails.isEmpty()) {
            return 0L;
        } else {
            return cartDetails.stream()
                    .mapToLong(CartDetail::getQuantity)
                    .sum();
        }
    }
}
