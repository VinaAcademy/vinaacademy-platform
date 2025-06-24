package com.vinaacademy.platform.feature.order_payment.service;

import java.util.List;
import java.util.UUID;

import com.vinaacademy.platform.client.UserClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vinaacademy.platform.exception.BadRequestException;
import com.vinaacademy.platform.feature.cart.repository.CartItemRepository;
import com.vinaacademy.platform.feature.cart.repository.CartRepository;
import com.vinaacademy.platform.feature.order_payment.dto.OrderItemDto;
import com.vinaacademy.platform.feature.order_payment.entity.Order;
import com.vinaacademy.platform.feature.order_payment.mapper.OrderItemMapper;
import com.vinaacademy.platform.feature.order_payment.mapper.OrderMapper;
import com.vinaacademy.platform.feature.order_payment.repository.CouponRepository;
import com.vinaacademy.platform.feature.order_payment.repository.OrderItemRepository;
import com.vinaacademy.platform.feature.order_payment.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import vn.vinaacademy.common.security.SecurityContextHelper;

@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService{

	@Autowired
	private UserClient userClient;
	
	private final OrderItemRepository orderItemRepository;
	
	private final OrderRepository orderRepository;
	
	private final CartRepository cartRepository;
	
	private final CartItemRepository cartItemRepository;
	
	private final OrderItemMapper orderItemMapper;
	
	private final OrderMapper orderMapper;
	
	private final CouponRepository couponRepository;

	@Autowired
	private SecurityContextHelper securityContextHelper;

	
	@Override
	public List<OrderItemDto> getOrderItems(UUID orderId) {
		Order order = orderRepository.findById(orderId).orElseThrow(
				() -> BadRequestException.message("Không tìm thấy order id này"));
		
		UUID currentUserId = securityContextHelper.getCurrentUserIdAsUUID();
		if (!currentUserId.equals(order.getUserId()))
			throw BadRequestException.message("Bạn không phải người sở hữu order này");
		
		List<OrderItemDto> orderItemDtos = orderItemMapper.toOrderItemDtoList(order.getOrderItems());
		return orderItemDtos;
	}

}
