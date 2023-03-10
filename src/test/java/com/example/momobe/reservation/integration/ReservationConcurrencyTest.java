package com.example.momobe.reservation.integration;

import com.example.momobe.common.resolver.UserInfo;
import com.example.momobe.meeting.domain.Address;
import com.example.momobe.meeting.domain.DateTime;
import com.example.momobe.meeting.domain.DateTimeInfo;
import com.example.momobe.meeting.domain.Meeting;
import com.example.momobe.reservation.application.ReservationLockFacade;
import com.example.momobe.reservation.application.ReservationFindService;
import com.example.momobe.reservation.dto.in.PostReservationDto;
import com.example.momobe.reservation.dto.out.PaymentResponseDto;
import com.example.momobe.user.domain.Email;
import com.example.momobe.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.example.momobe.common.enums.TestConstants.*;
import static com.example.momobe.meeting.domain.enums.Category.AI;
import static com.example.momobe.meeting.domain.enums.DatePolicy.FREE;
import static com.example.momobe.meeting.domain.enums.MeetingState.OPEN;

@Slf4j
@Transactional
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "Local", matches = "local")
public class ReservationConcurrencyTest {
    @Autowired
    private ReservationLockFacade reservationLockFacade;

    @Autowired
    EntityManager em;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    ReservationFindService reservationFindService;

    private Meeting freeOrderMeeting;
    private PostReservationDto reservationDto;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void before() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        User user = User.builder()
                .email(new Email(EMAIL2))
                .build();
        User host = User.builder().build();

        transactionTemplate.executeWithoutResult(
                (status -> {
                    em.persist(user);
                    em.persist(host);

                })
        );

        freeOrderMeeting = Meeting.builder()
                .hostId(host.getId())
                .category(AI)
                .title(CONTENT1)
                .dateTimeInfo(DateTimeInfo.builder()
                        .datePolicy(FREE)
                        .startDate(LocalDate.of(2022, 1, 1))
                        .endDate(LocalDate.of(2022, 1, 10))
                        .startTime(LocalTime.of(10, 0, 0))
                        .endTime(LocalTime.of(18, 0, 0))
                        .maxTime(4)
                        .dateTimes(List.of(new DateTime(LocalDateTime.of(2022, 1, 1, 12, 0, 0))))
                        .build())
                .personnel(1)
                .price(0L)
                .content(CONTENT2)
                .tagIds(List.of(ID1, ID2))
                .meetingState(OPEN)
                .address(new Address(List.of(1L, 2L), "?????????"))
                .build();

        reservationDto = PostReservationDto.builder()
                .reservationMemo(CONTENT1)
                .amount(0L)
                .dateInfo(PostReservationDto.ReservationDateDto.builder()
                        .reservationDate(LocalDate.of(2022, 1, 5))
                        .startTime(LocalTime.of(11, 0, 0))
                        .endTime(LocalTime.of(13, 0, 0))
                        .build())
                .build();

        transactionTemplate.executeWithoutResult(
                (status -> {
                    em.persist(freeOrderMeeting);
                })
        );
    }

    @Test
    @DisplayName("50?????? ???????????? ????????? ?????? ????????? ?????? ??? 1?????? ????????? ????????????")
    void concurrencyTest1() throws InterruptedException {
        //given
        int numberOfThreads = 50;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        UserInfo user = UserInfo.builder()
                .email(EMAIL1)
                .nickname(NICKNAME1)
                .id(ID1)
                .roles(List.of(ROLE_USER))
                .build();

        List<PaymentResponseDto> reservations = new ArrayList<>();

        //when
        for (int i = 0; i < numberOfThreads; i++) {
            service.execute(() -> {
                        transactionTemplate.executeWithoutResult(status -> {
                            reservations.add(reservationLockFacade.reserve(freeOrderMeeting.getId(), reservationDto, user));
                            latch.countDown();
                        }
                        );
                    }
            );
        }

        Thread.sleep(500);

        //then
        Assertions.assertThat(reservations.size()).isOne();
    }
}
