package com.example.momobe.reservation.domain;

import com.example.momobe.common.domain.BaseTime;
import com.example.momobe.reservation.domain.enums.ReservationState;
import com.example.momobe.reservation.event.ReservationCanceledEvent;
import lombok.*;

import javax.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

import static com.example.momobe.common.exception.enums.ErrorCode.*;
import static com.example.momobe.reservation.domain.enums.ReservationState.*;
import static javax.persistence.EnumType.STRING;
import static javax.persistence.GenerationType.*;
import static lombok.AccessLevel.*;

@Entity
@Getter
@Builder
@ToString
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
public class Reservation extends BaseTime {
    @Id
    @Column(name = "reservation_id")
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private Long meetingId;

    @Embedded
    private ReservationDate reservationDate;

    @Embedded
    private Money amount;

    @Embedded
    private ReservedUser reservedUser;

    @Embedded
    private ReservationMemo reservationMemo;

    @Enumerated(STRING)
    private ReservationState reservationState;

    private Long paymentId;

    public Reservation(ReservationDate reservationDate, Money amount, ReservedUser reservedUser, ReservationMemo reservationMemo, Long meetingId) {
        this.reservationDate = reservationDate;
        this.amount = amount;
        this.reservedUser = reservedUser;
        this.reservationMemo = reservationMemo;
        this.meetingId = meetingId;
        this.reservationState = (amount.getWon() <= 0) ? PAYMENT_SUCCESS : PAYMENT_BEFORE;
    }

    public Reservation(ReservationDate reservationDate, Money amount, ReservedUser reservedUser, ReservationMemo reservationMemo, ReservationState reservationState, Long meetingId) {
        this.reservationDate = reservationDate;
        this.amount = amount;
        this.reservedUser = reservedUser;
        this.reservationMemo = reservationMemo;
        this.meetingId = meetingId;
        this.reservationState = reservationState;
    }

    public Boolean isCanceledReservation() {
        return this.reservationState.equals(CANCEL);
    }

    public Boolean isPaymentSucceed() {
        return Objects.equals(this.reservationState, PAYMENT_SUCCESS);
    }

    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public ReservationCanceledEvent createCancelEvent(String paymentKey, String reasonMessage) {
        return ReservationCanceledEvent.builder()
                .paymentKey(paymentKey)
                .reason(reasonMessage)
                .paymentId(this.paymentId)
                .build();
    }

    private Boolean canChangeStatus() {
        return this.reservationState.equals(PAYMENT_SUCCESS) && !this.reservationDate.isBeforeThen(LocalDateTime.now());
    }

    private void changeState(ReservationState state) {
        if (!canChangeStatus()) throw new ReservationException(CAN_NOT_CHANGE_RESERVATION_STATE);
        this.reservationState = state;
    }

    public void cancel() {
       changeState(CANCEL);
    }

    public void accept() {
        changeState(ACCEPT);
    }

    public void deny() {
        changeState(DENY);
    }

    public Long getReservedUserId() {
        return this.reservedUser.getUserId();
    }

    public Boolean matchReservedUserId(Long userId) {
        return getReservedUserId().equals(userId);
    }
}
