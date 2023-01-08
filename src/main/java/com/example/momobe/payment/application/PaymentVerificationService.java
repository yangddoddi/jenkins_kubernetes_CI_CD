package com.example.momobe.payment.application;

import com.example.momobe.common.exception.enums.ErrorCode;
import com.example.momobe.payment.domain.Payment;
import com.example.momobe.payment.domain.UnableProceedPaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentVerificationService {
    private final PaymentCommonService paymentCommonService;

    @Transactional(readOnly = true)
    public Boolean verify(String orderId, String paymentKey, Long amount, Payment payment) {
        if (!payment.matchAmount(amount)) {
            throw new UnableProceedPaymentException(ErrorCode.AMOUNT_DOSE_NOT_MATCH);
        }

        return Boolean.TRUE;
    }
}
