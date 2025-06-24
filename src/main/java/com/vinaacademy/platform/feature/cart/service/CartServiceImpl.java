package com.vinaacademy.platform.feature.cart.service;

import com.vinaacademy.platform.exception.BadRequestException;
import com.vinaacademy.platform.feature.cart.dto.CartDto;
import com.vinaacademy.platform.feature.cart.dto.CartRequest;
import com.vinaacademy.platform.feature.cart.entity.Cart;
import com.vinaacademy.platform.feature.cart.mapper.CartMapper;
import com.vinaacademy.platform.feature.cart.repository.CartRepository;
import com.vinaacademy.platform.feature.order_payment.entity.Coupon;
import com.vinaacademy.platform.feature.order_payment.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.vinaacademy.common.security.SecurityContextHelper;

import java.util.UUID;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private SecurityContextHelper securityContextHelper;

    @Override
    public CartDto getCart(UUID userId) {
        UUID currentUserId = securityContextHelper.getCurrentUserIdAsUUID();
        if (!currentUserId.equals(userId)) {
            throw BadRequestException.message("Bạn không có quyền truy cập giỏ hàng của người dùng khác");
        }
        Cart cart = cartRepository.findByUserId(userId)
                .orElse(null);
        CartDto cartDto = cartMapper.toDTO(cart);

        if (cart == null) {
            cartDto = createCart(CartRequest.builder()
                    .user_id(userId)
                    .coupon_id(null)
                    .build());
        }
        return cartDto;
    }

    @Override
    public CartDto createCart(CartRequest request) {
        UUID currentUserId = securityContextHelper.getCurrentUserIdAsUUID();
        Cart cart = Cart.builder().userId(currentUserId).coupon(null).build();
        cartRepository.save(cart);
        return cartMapper.toDTO(cart);
    }

    @Override
    public CartDto updateCart(CartRequest request) {
        UUID currentUserId = securityContextHelper.getCurrentUserIdAsUUID();
        Cart cart = cartRepository.findByUserId(currentUserId)
                .orElseThrow(() -> BadRequestException.message("Không tìm thấy Cart của ID người dùng này"));
        Coupon coupon = null;
        if (request.getCoupon_id() != null) {
            coupon = couponRepository.findById(request.getCoupon_id())
                    .orElseThrow(() -> BadRequestException.message("Không tìm thấy coupon"));
        }
        cart.setCoupon(coupon);
        cart.setUserId(currentUserId);
        cartRepository.save(cart);
        return cartMapper.toDTO(cart);
    }

    @Override
    public void deleteCart(CartRequest request) {
        UUID currentUserId = securityContextHelper.getCurrentUserIdAsUUID();
        Cart cart = cartRepository.findByUserId(currentUserId)
                .orElseThrow(() -> BadRequestException.message("Không tìm thấy Cart của ID người dùng này"));
        cartRepository.delete(cart);
    }

}
