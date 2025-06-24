package com.vinaacademy.platform.feature.order_payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vinaacademy.platform.exception.BadRequestException;
import com.vinaacademy.platform.feature.enrollment.dto.EnrollmentRequest;
import com.vinaacademy.platform.feature.enrollment.service.EnrollmentService;
import com.vinaacademy.platform.feature.order_payment.dto.PaymentDto;
import com.vinaacademy.platform.feature.order_payment.dto.PaymentRequest;
import com.vinaacademy.platform.feature.order_payment.entity.Order;
import com.vinaacademy.platform.feature.order_payment.entity.Payment;
import com.vinaacademy.platform.feature.order_payment.enums.OrderStatus;
import com.vinaacademy.platform.feature.order_payment.enums.PaymentStatus;
import com.vinaacademy.platform.feature.order_payment.mapper.PaymentMapper;
import com.vinaacademy.platform.feature.order_payment.repository.OrderRepository;
import com.vinaacademy.platform.feature.order_payment.repository.PaymentRepository;
import com.vinaacademy.platform.feature.order_payment.utils.Utils;
import com.vinaacademy.platform.feature.order_payment.utils.VNPayConfig;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import vn.vinaacademy.common.security.SecurityContextHelper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;

    private final PaymentMapper paymentMapper;

    private final PaymentRepository paymentRepository;

    private final ObjectMapper objectMapper;

    private final SecurityContextHelper securityHelper;

    private final EnrollmentService enrollmentService;

    @Override
    public PaymentDto createPayment(UUID orderId, HttpServletRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BadRequestException.message("Không tìm thấy order id này"));
//		if (StringUtil.isNullOrEmpty(urlChecking))
//			throw BadRequestException.message("Url checking trống!");

        UUID currentUserId = securityHelper.getCurrentUserIdAsUUID();
        if (!currentUserId.equals(order.getUserId()))
            throw BadRequestException.message("Bạn không phải người sở hữu order này");

        String url = VNPayConfig.createPaymentRedirect(order.getTotalAmount().longValue(),
                "Đơn thanh toán cho mã đơn: " + orderId, order.getId().toString(), request);
        ObjectNode data = objectMapper.createObjectNode();
        data.put("urlTransaction", url);
        data.put("vnp_ref", orderId.toString());
        Payment payment = Payment.builder().amount(order.getTotalAmount()).createdAt(LocalDateTime.now())
                .paymentStatus(PaymentStatus.PENDING).paymentMethod("VNPAY").order(order).paymentData(data)
                .transactionId(orderId.toString()).build();
        order.setPayment(payment);
        orderRepository.save(order);
        PaymentDto paymentDto = paymentMapper.toDTO(payment);
        return paymentDto;
    }

    @Override
    public PaymentDto updatePayment(PaymentRequest paymentRequest) {
//		Payment payment = paymentRepository.findById(paymentRequest.getPaymentId()).orElseThrow(
//				() -> BadRequestException.message("Không tìm thấy payment id này"));
//		if (!StringUtil.isNullOrEmpty(paymentRequest.getTransactionId()))
//			payment.setTransactionId(paymentRequest.getTransactionId());
//		
//		if (paymentRequest.getPaymentData() != null && !paymentRequest.getPaymentData().isEmpty())
//			payment.setPaymentData(paymentRequest.getPaymentData());
//		
//		payment.setPaymentStatus(paymentRequest.getPaymentStatus());
//		paymentRepository.save(payment);
//		PaymentDto paymentDto = paymentMapper.toDTO(payment);
//		return paymentDto;
        return null;
    }

    @Override
    public List<PaymentDto> getPaymentList() {
        UUID currentUserId = securityHelper.getCurrentUserIdAsUUID();
        List<Payment> payments = paymentRepository.findAllByUserId(currentUserId);
        List<PaymentDto> paymentDtos = paymentMapper.toDTOList(payments);
        log.debug("Get payment list for user " + currentUserId);
        return paymentDtos;
    }

    @Override
    public PaymentDto getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> BadRequestException.message("Không tìm thấy payment id này"));

        UUID currentUserId = securityHelper.getCurrentUserIdAsUUID();
        if (!currentUserId.equals(payment.getOrder().getUserId()))
            throw BadRequestException.message("Bạn không phải người sở hữu payment này");

        PaymentDto paymentDto = paymentMapper.toDTO(payment);
        return paymentDto;
    }

    @Override
    public PaymentStatus validPayment(Map<String, String> requestParams) {
        Optional<Payment> payment = null;

        if (requestParams.containsKey("vnp_TxnRef")) {
            String vnp_ref = requestParams.get("vnp_TxnRef");

            payment = paymentRepository.findByTransactionId(vnp_ref);
            if (payment.isPresent()) {

            } else
                return PaymentStatus.FAILED;
        }
        Payment pay = payment.get();

        PaymentStatus result = Utils.orderReturn(requestParams);
        pay.setPaymentStatus(result);
        String transactionId = requestParams.get("vnp_TransactionNo");
        pay.setTransactionId(result == PaymentStatus.COMPLETED ? transactionId : pay.getTransactionId());
        if (pay.getPaymentData() != null && pay.getPaymentData() instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) pay.getPaymentData();
            requestParams.forEach(objectNode::put);
            pay.setPaymentData(objectNode);
        }
        Boolean updateEnroll = false;
        if (result == PaymentStatus.COMPLETED) {
            updateEnroll = true;
            pay.getOrder().setStatus(OrderStatus.PAID);
        } else {
            pay.getOrder().setStatus(OrderStatus.FAILED);

        }
        paymentRepository.save(pay);
        if (updateEnroll) {
            pay.getOrder().getOrderItems().forEach(oi -> {
                EnrollmentRequest enrollrequest = EnrollmentRequest.builder().courseId(oi.getCourse().getId()).build();
                UUID userId = pay.getOrder().getUserId();
                if (!enrollmentService.isEnrolled(userId, enrollrequest.getCourseId()))
                    enrollmentService.enrollCourse(enrollrequest, pay.getOrder().getUserId());
            });
        }
        log.debug("update payment {} with status {} ", pay.getId(), result);

        return result;
    }

    @Override
    public String testApi() {
        return VNPayConfig.createPaymentRedirect(Long.parseLong("10000"), "test order", "abcdeg12345678", null);
    }

    @Override
    public PaymentStatus validUrlReturn(Map<String, String> requestParams) {
        return Utils.orderReturn(requestParams);
    }

}
