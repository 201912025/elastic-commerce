// domain/order/entity/Order.java
package com.example.ElasticCommerce.domain.order.entity;

import com.example.ElasticCommerce.domain.order.dto.response.OrderDto;
import com.example.ElasticCommerce.domain.order.exception.OrderExceptionType;
import com.example.ElasticCommerce.domain.user.entity.User;
import com.example.ElasticCommerce.global.common.BaseEntity;
import com.example.ElasticCommerce.global.exception.type.BadRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Address address;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Builder
    public Order(User user, List<OrderItem> items, Address address) {
        this.user = user;
        this.status = OrderStatus.CREATED;
        items.forEach(this::addItem);
        this.address = address;
        address.getClass();
    }

    public void addItem(OrderItem item) {
        item.assignOrder(this);
        this.items.add(item);
    }

    public void changeStatus(OrderStatus newStatus) {
        if (this.status == OrderStatus.CANCELLED || this.status == OrderStatus.DELIVERED) {
            throw new BadRequestException(OrderExceptionType.ORDER_CANNOT_CANCEL);
        }
        // 순서 검증 (예: CREATED → PAID → SHIPPED → DELIVERED)
        boolean valid =
                switch (this.status) {
                    case CREATED    -> newStatus == OrderStatus.PAID || newStatus == OrderStatus.CANCELLED;
                    case PAID       -> newStatus == OrderStatus.SHIPPED  || newStatus == OrderStatus.CANCELLED;
                    case SHIPPED    -> newStatus == OrderStatus.DELIVERED;
                    default         -> false;
                };
        if (!valid) {
            throw new BadRequestException(OrderExceptionType.ORDER_CANNOT_CANCEL);
        }
        this.status = newStatus;
    }

    public void cancel() {
        if (this.status != OrderStatus.CREATED && this.status != OrderStatus.PAID) {
            throw new BadRequestException(OrderExceptionType.ORDER_CANNOT_CANCEL);
        }
        this.status = OrderStatus.CANCELLED;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public OrderDto toDto() {
        return OrderDto.from(this);
    }
}
