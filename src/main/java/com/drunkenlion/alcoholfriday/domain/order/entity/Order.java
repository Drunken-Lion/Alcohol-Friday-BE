package com.drunkenlion.alcoholfriday.domain.order.entity;

import com.drunkenlion.alcoholfriday.domain.member.entity.Member;
import com.drunkenlion.alcoholfriday.global.common.entity.BaseEntity;
import com.drunkenlion.alcoholfriday.global.common.enumerated.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders")
public class Order extends BaseEntity {
    @Comment("주문 고유번호")
    @Column(name = "order_no", columnDefinition = "VARCHAR(200)")
    private String orderNo;

    @Comment("주문 상태정보")
    @Column(name = "status", columnDefinition = "VARCHAR(20)")
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Comment("주문 상품 총 금액")
    @Column(name = "price", columnDefinition = "DECIMAL(64, 3)")
    private BigDecimal price;

    @Comment("배송 금액")
    @Column(name = "delivery_price", columnDefinition = "DECIMAL(64, 3)")
    private BigDecimal deliveryPrice;

    @Comment("배송비 포함 주문 총 금액")
    @Column(name = "total_price", columnDefinition = "DECIMAL(64, 3)")
    private BigDecimal totalPrice;

    @Comment("배송받는 사람")
    @Column(name = "recipient", columnDefinition = "VARCHAR(50)")
    private String recipient;

    @Comment("배송받는 사람의 연락처")
    @Column(name = "phone", columnDefinition = "BIGINT")
    private Long phone;

    @Comment("배송지 주소")
    @Column(name = "address", columnDefinition = "VARCHAR(200)")
    private String address;

    @Comment("배송지 상세 주소")
    @Column(name = "address_detail", columnDefinition = "VARCHAR(200)")
    private String addressDetail ;

    @Comment("배송시 주의사항")
    @Column(name = "description", columnDefinition = "MEDIUMTEXT")
    private String description;

    @Comment("배송지 우편번호")
    @Column(name = "postcode", columnDefinition = "BIGINT")
    private Long postcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", columnDefinition = "BIGINT", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private Member member;

    @OneToMany(mappedBy = "order")
    @Builder.Default
    private List<OrderDetail> orderDetails = new ArrayList<>();

    public void addMember(Member member) {
        this.member = member;
        member.getOrders().add(this);
    }

    public void genOrderNo(Long id) {
        // TODO orderNo를 주문 접수할 때 만들고 클라이언트에 내려주기 (결제 요청 전)
        // yyyy-MM-dd 형식의 DateTimeFormatter 생성
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        this.orderNo = getCreatedAt().format(formatter) + "__" + id;
    }

    public void addPrice(List<OrderDetail> orderDetailList) {
        this.price = getTotalOrderPrice(orderDetailList);
    }

    public BigDecimal getTotalOrderPrice(List<OrderDetail> orderDetailList) {
        if (orderDetailList.isEmpty()) {
            return BigDecimal.ZERO;
        } else {
            return orderDetailList.stream()
                    .map(OrderDetail::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    public Long getTotalOrderQuantity(List<OrderDetail> orderDetailList) {
        if (orderDetailList.isEmpty()) {
            return 0L;
        } else {
            return orderDetailList.stream()
                    .mapToLong(OrderDetail::getQuantity)
                    .sum();
        }
    }
}
