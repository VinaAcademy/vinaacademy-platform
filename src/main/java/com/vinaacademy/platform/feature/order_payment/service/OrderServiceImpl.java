package com.vinaacademy.platform.feature.order_payment.service;

import com.vinaacademy.platform.client.UserClient;
import com.vinaacademy.platform.exception.BadRequestException;
import com.vinaacademy.platform.feature.cart.entity.Cart;
import com.vinaacademy.platform.feature.cart.entity.CartItem;
import com.vinaacademy.platform.feature.cart.repository.CartItemRepository;
import com.vinaacademy.platform.feature.cart.repository.CartRepository;
import com.vinaacademy.platform.feature.cart.service.CartItemService;
import com.vinaacademy.platform.feature.course.enums.CourseStatus;
import com.vinaacademy.platform.feature.order_payment.dto.OrderCouponRequest;
import com.vinaacademy.platform.feature.order_payment.dto.OrderDto;
import com.vinaacademy.platform.feature.order_payment.dto.OrderRequest;
import com.vinaacademy.platform.feature.order_payment.entity.Coupon;
import com.vinaacademy.platform.feature.order_payment.entity.Order;
import com.vinaacademy.platform.feature.order_payment.entity.OrderItem;
import com.vinaacademy.platform.feature.order_payment.enums.OrderStatus;
import com.vinaacademy.platform.feature.order_payment.mapper.OrderItemMapper;
import com.vinaacademy.platform.feature.order_payment.mapper.OrderMapper;
import com.vinaacademy.platform.feature.order_payment.repository.CouponRepository;
import com.vinaacademy.platform.feature.order_payment.repository.OrderItemRepository;
import com.vinaacademy.platform.feature.order_payment.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.vinaacademy.common.security.SecurityContextHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

	@Autowired
	private UserClient userClient;

	private final OrderItemRepository orderItemRepository;

	private final OrderRepository orderRepository;

	private final CartRepository cartRepository;

	private final CartItemRepository cartItemRepository;
	
	private final CartItemService cartItemService;

	private final OrderItemMapper orderItemMapper;

	private final OrderMapper orderMapper;

	private final CouponRepository couponRepository;

	@Autowired
	private SecurityContextHelper securityHelper;

	@Override
	public OrderDto createOrder() {
		UUID currentUserId = securityHelper.getCurrentUserIdAsUUID();

		Cart cart = cartRepository.findByUserId(currentUserId)
				.orElseThrow(() -> BadRequestException.message("Không tìm thấy Cart của user id này"));
		List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
		if (cartItems.size() == 0)
			throw BadRequestException.message("Cart không có gì cả");
		
		Order order = Order.builder().status(OrderStatus.PENDING).coupon(cart.getCoupon()).payment(null)
				.orderItems(new ArrayList<>()).userId(currentUserId).build();
		order = orderRepository.save(order);
		List<OrderItem> orderItems = orderItemMapper.toOrderItemList(cartItems, order);
		
		orderItems = orderItems.stream()
				.filter(orderItem -> orderItem.getCourse().getStatus() == CourseStatus.PUBLISHED).toList();
		log.debug(orderItems.size() + " size order");
		
		if (orderItems.size() == 0)
			throw BadRequestException.message("Không có order item nào cả");
		orderItems.forEach(order::addOrderItem);
		order.calculateAmounts();
		order = orderRepository.save(order);

//		//xoa all item trong cart
		
		for (CartItem cartitem: cartItems) {
			log.debug("Xóa "+cartitem.getId());
			cartItemService.deleteCartItem(cartitem.getId());
		}

		OrderDto orderDto = orderMapper.toOrderDto(order);
		orderDto.setOrderItemsDto(orderItemMapper.toOrderItemDtoList(orderItems));
		return orderDto;

	}

	@Override
	public void deleteOrder(UUID orderId) {
//		Order order = orderRepository.findById(orderId).orElseThrow(
//				() -> BadRequestException.message("Không tìm thấy order id này"));
//		orderRepository.delete(order);

	}

	@Override
	public OrderDto updateOrder(OrderRequest orderRequest) {
//		Order order = orderRepository.findById(orderRequest.getOrderId()).orElseThrow(
//				() -> BadRequestException.message("Không tìm thấy order id này"));
//		Coupon coupon = null;
//		if (orderRequest.getCouponId() != null) {
//			coupon = couponRepository.findById(orderRequest.getCouponId())
//					.orElseThrow(() -> BadRequestException.message("Không tìm thấy coupon"));
//		}
//		order.setCoupon(coupon);
//		OrderDto orderDto = orderMapper.toOrderDto(orderRepository.save(order));
//		return orderDto;
		return null;
	}

	@Override
	public OrderDto getOrder(UUID orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> BadRequestException.message("Không tìm thấy order id này"));

		UUID currentUserId = securityHelper.getCurrentUserIdAsUUID();
		if (!currentUserId.equals(order.getUserId()))
			throw BadRequestException.message("Bạn không phải người sở hữu order này");

		OrderDto orderDto = orderMapper.toOrderDto(order);
		orderDto.setOrderItemsDto(orderItemMapper.toOrderItemDtoList(order.getOrderItems()));
		return orderDto;
	}

	@Override
	public Page<OrderDto> getOrders(Pageable pageable) {
		UUID currentUserId = securityHelper.getCurrentUserIdAsUUID();
		Page<Order> orders = orderRepository.findByUserId(currentUserId, pageable);
		return orders.map(order -> {
			OrderDto orderDto = orderMapper.toOrderDto(order);
			orderDto.setOrderItemsDto(orderItemMapper.toOrderItemDtoList(order.getOrderItems()));
			return orderDto;
		});
	}

	@Transactional
	@Override
	public OrderDto updateCoupon(OrderCouponRequest orderCouponRequest) {
		Order order = orderRepository.findById(orderCouponRequest.getOrderId())
				.orElseThrow(() -> BadRequestException.message("Không tìm thấy order id này"));
		
		UUID currentUserId = securityHelper.getCurrentUserIdAsUUID();
		if (!currentUserId.equals(order.getUserId()))
			throw BadRequestException.message("Bạn không phải người sở hữu order này");
		
		if (orderCouponRequest.getCouponId()==null) {
			order.setCoupon(null);
		}else {
			Coupon coupon = couponRepository.findById(orderCouponRequest.getCouponId())
					.orElseThrow(() -> BadRequestException.message("Không tìm thấy order id này"));
			order.setCoupon(coupon);
		}
		order.calculateAmounts();
		order = orderRepository.save(order);
		OrderDto orderDto = orderMapper.toOrderDto(order);
		orderDto.setOrderItemsDto(orderItemMapper.toOrderItemDtoList(order.getOrderItems()));
		return orderDto;
	}

}
