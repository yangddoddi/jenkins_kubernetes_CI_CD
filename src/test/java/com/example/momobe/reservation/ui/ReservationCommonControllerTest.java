package com.example.momobe.reservation.ui;

import com.example.momobe.common.config.ApiDocumentUtils;
import com.example.momobe.common.config.SecurityTestConfig;
import com.example.momobe.common.exception.ui.ExceptionController;
import com.example.momobe.common.resolver.JwtArgumentResolver;
import com.example.momobe.meeting.domain.MeetingException;
import com.example.momobe.payment.domain.PaymentException;
import com.example.momobe.reservation.application.ReservationCancelService;
import com.example.momobe.reservation.application.ReservationConfirmService;
import com.example.momobe.reservation.application.ReservationBookService;
import com.example.momobe.reservation.application.ReservationLockFacade;
import com.example.momobe.reservation.domain.ReservationException;
import com.example.momobe.reservation.dto.in.DeleteReservationDto;
import com.example.momobe.reservation.dto.in.PatchReservationDto;
import com.example.momobe.reservation.dto.in.PostReservationDto;
import com.example.momobe.reservation.dto.out.PaymentResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static com.example.momobe.common.enums.TestConstants.*;
import static com.example.momobe.common.exception.enums.ErrorCode.*;
import static org.aspectj.apache.bcel.generic.ObjectType.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
@AutoConfigureRestDocs
@Import(SecurityTestConfig.class)
@MockBean(JpaMetamodelMappingContext.class)
@WebMvcTest({ReservationCommonController.class, ExceptionController.class})
class ReservationCommonControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    ReservationBookService reservationBookService;

    @MockBean
    ReservationConfirmService reservationConfirmService;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    JwtArgumentResolver jwtArgumentResolver;

    @MockBean
    ReservationCancelService reservationCancelService;

    @MockBean
    ReservationLockFacade reservationLockFacade;

    @Test
    @DisplayName("????????? ????????? ????????? ?????? 400 ??????")
    void postReservation_fail1() throws Exception {
        //given
        PostReservationDto request = PostReservationDto.builder()
                .dateInfo(PostReservationDto.ReservationDateDto.builder()
                        .build())
                .reservationMemo(CONTENT1)
                .build();

        String json = objectMapper.writeValueAsString(request);

        //when
        ResultActions perform = mockMvc.perform(post("/meetings/{meetingId}/reservations", 1)
                .contentType(APPLICATION_JSON)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .content(json));

        //then
        perform.andExpect(status().isBadRequest())
                .andDo(document("postReservation/400",
                                ApiDocumentUtils.getDocumentRequest(),
                                ApiDocumentUtils.getDocumentResponse(),
                                requestHeaders(
                                        headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                                ),
                                requestFields(
                                        fieldWithPath("dateInfo.reservationDate").description("???????????? ??? ????????????."),
                                        fieldWithPath("dateInfo.startTime").description("???????????? ??? ????????????."),
                                        fieldWithPath("dateInfo.endTime").description("???????????? ??? ????????????."),
                                        fieldWithPath("amount").description("???????????? ??? ????????????."),
                                        fieldWithPath("reservationMemo").description("???????????? ????????? ??????")
                                )
                        )
                );
    }

    @Test
    @DisplayName("?????? ?????? ????????? ????????? ???????????? ?????? 409 ??????")
    void postReservation_fail2() throws Exception {
        //given
        given(reservationLockFacade.reserve(anyLong(), any(PostReservationDto.class), any()))
                .willThrow(new ReservationException(FULL_OF_PEOPLE));

        PostReservationDto request = PostReservationDto.builder()
                .dateInfo(PostReservationDto.ReservationDateDto.builder()
                        .reservationDate(LocalDate.now())
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plus(1, ChronoUnit.HOURS))
                        .build())
                .amount(1000L)
                .reservationMemo(CONTENT1)
                .build();

        String json = objectMapper.writeValueAsString(request);

        //when
        ResultActions perform = mockMvc.perform(post("/meetings/{meetingId}/reservations", 1)
                .contentType(APPLICATION_JSON)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .content(json));

        //then
        perform.andExpect(status().isConflict())
                .andDo(document("postReservation/409/full",
                                ApiDocumentUtils.getDocumentRequest(),
                                ApiDocumentUtils.getDocumentResponse(),
                                requestHeaders(
                                        headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                                ),
                                requestFields(
                                        fieldWithPath("dateInfo.reservationDate").description("?????????"),
                                        fieldWithPath("dateInfo.startTime").description("?????? ?????? ??????"),
                                        fieldWithPath("dateInfo.endTime").description("?????? ?????? ??????"),
                                        fieldWithPath("amount").description("??????"),
                                        fieldWithPath("reservationMemo").description("???????????? ????????? ??????")
                                )
                        )
                );
    }

    @Test
    @DisplayName("?????? ????????? ?????? ????????? ????????? ???????????? ?????? (????????? ????????? ???????????? ??????) 409 ??????")
    void postReservation_fail3() throws Exception {
        //given
        given(reservationLockFacade.reserve(anyLong(), any(PostReservationDto.class), any()))
                .willThrow(new ReservationException(INVALID_RESERVATION_TIME));

        PostReservationDto request = PostReservationDto.builder()
                .dateInfo(PostReservationDto.ReservationDateDto.builder()
                        .reservationDate(LocalDate.now())
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plus(1, ChronoUnit.HOURS))
                        .build())
                .amount(1000L)
                .reservationMemo(CONTENT1)
                .build();

        String json = objectMapper.writeValueAsString(request);

        //when
        ResultActions perform = mockMvc.perform(post("/meetings/{meetingId}/reservations", 1)
                .contentType(APPLICATION_JSON)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .content(json));

        //then
        perform.andExpect(status().isConflict())
                .andDo(document("postReservation/409/invalid",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                        ),
                        requestFields(
                                fieldWithPath("dateInfo.reservationDate").description("????????? ??? ?????? ?????????/?????? (?????? ????????? ???????????? ????????? ??????)"),
                                fieldWithPath("dateInfo.startTime").description("????????? ??? ?????? ?????????/?????? (?????? ????????? ???????????? ????????? ??????)"),
                                fieldWithPath("dateInfo.endTime").description("????????? ??? ?????? ?????????/?????? (?????? ????????? ???????????? ????????? ??????)"),
                                fieldWithPath("amount").description("??????"),
                                fieldWithPath("reservationMemo").description("???????????? ????????? ??????")
                                )
                        )
                );
    }

    @Test
    @DisplayName("?????? ?????? ??? ????????? ????????? ?????? ??????????????? ????????? ???????????? ?????? ?????? 409 ??????")
    void postReservation_fail4() throws Exception {
        //given
        given(reservationLockFacade.reserve(anyLong(), any(PostReservationDto.class), any()))
                .willThrow(new ReservationException(AMOUNT_DOSE_NOT_MATCH));

        PostReservationDto request = PostReservationDto.builder()
                .dateInfo(PostReservationDto.ReservationDateDto.builder()
                        .reservationDate(LocalDate.now())
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plus(1, ChronoUnit.HOURS))
                        .build())
                .amount(1000L)
                .reservationMemo(CONTENT1)
                .build();

        String json = objectMapper.writeValueAsString(request);

        //when
        ResultActions perform = mockMvc.perform(post("/meetings/{meetingId}/reservations", 1)
                .contentType(APPLICATION_JSON)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .content(json));

        //then
        perform.andExpect(status().isConflict())
                .andDo(document("postReservation/409/money",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                        ),
                        requestFields(
                                fieldWithPath("dateInfo.reservationDate").description("?????????"),
                                fieldWithPath("dateInfo.startTime").description("?????? ?????? ??????"),
                                fieldWithPath("dateInfo.endTime").description("?????? ?????? ??????"),
                                fieldWithPath("amount").description("???????????? ????????? ????????? ???????????? ??????"),
                                fieldWithPath("reservationMemo").description("???????????? ????????? ??????")
                        )
                        )
                );
    }

    @Test
    @DisplayName("?????? ????????? ??????")
    void postReservation_success() throws Exception {
        //given
        PostReservationDto request = PostReservationDto.builder()
                .dateInfo(PostReservationDto.ReservationDateDto.builder()
                        .reservationDate(LocalDate.now())
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plus(1, ChronoUnit.HOURS))
                        .build())
                .amount(1000L)
                .reservationMemo(CONTENT1)
                .build();

        PaymentResponseDto response = PaymentResponseDto.builder()
                .orderName(CONTENT1)
                .orderId(ID)
                .successUrl("/testpage")
                .failUrl("/testpage")
                .customerName(NICKNAME)
                .amount(1000L)
                .customerEmail(EMAIL1)
                .build();

        String json = objectMapper.writeValueAsString(request);
        given(reservationLockFacade.reserve(anyLong(), any(PostReservationDto.class), any())).willReturn(response);

        //when
        ResultActions perform = mockMvc.perform(post("/meetings/{meetingId}/reservations", 1)
                .contentType(APPLICATION_JSON)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .content(json));

        //then
        perform.andExpect(status().isCreated())
                .andDo(document("postReservation/201",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                        ),
                        requestFields(
                                fieldWithPath("dateInfo.reservationDate").description("?????????"),
                                fieldWithPath("dateInfo.startTime").description("?????? ?????? ??????"),
                                fieldWithPath("dateInfo.endTime").description("?????? ?????? ??????"),
                                fieldWithPath("amount").description("???????????? ????????? ????????? ???????????? ??????"),
                                fieldWithPath("reservationMemo").description("???????????? ????????? ??????")
                        ),
                        responseFields(
                                fieldWithPath("amount").type(LONG).description("?????? ??????"),
                                fieldWithPath("orderId").type(STRING).description("?????? ?????? ?????????"),
                                fieldWithPath("orderName").type(STRING).description("?????? ?????? ??????"),
                                fieldWithPath("customerEmail").type(STRING).description("?????? ????????? ??????"),
                                fieldWithPath("customerName").type(STRING).description("?????? ??????"),
                                fieldWithPath("successUrl").type(STRING).description("?????? ??? ?????? url"),
                                fieldWithPath("failUrl").type(STRING).description("?????? ??? ?????? url")
                        )
                        )
                );
    }

    @Test
    @DisplayName("????????? meeting??? ?????? ?????? ?????? 404 ??????")
    void postReservation_fail5() throws Exception {
        //given
        given(reservationLockFacade.reserve(anyLong(), any(PostReservationDto.class), any()))
                .willThrow(new MeetingException(DATA_NOT_FOUND));

        PostReservationDto request = PostReservationDto.builder()
                .dateInfo(PostReservationDto.ReservationDateDto.builder()
                        .reservationDate(LocalDate.now())
                        .startTime(LocalTime.now())
                        .endTime(LocalTime.now().plus(1, ChronoUnit.HOURS))
                        .build())
                .amount(1000L)
                .reservationMemo(CONTENT1)
                .build();

        String json = objectMapper.writeValueAsString(request);

        //when
        ResultActions perform = mockMvc.perform(post("/meetings/{meetingId}/reservations", 1)
                .contentType(APPLICATION_JSON)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .content(json));

        //then
        perform.andExpect(status().isNotFound())
                .andDo(document("postReservation/404",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                        ),
                        requestFields(
                                fieldWithPath("dateInfo.reservationDate").description("?????????"),
                                fieldWithPath("dateInfo.startTime").description("?????? ??????"),
                                fieldWithPath("dateInfo.endTime").description("????????? ??????"),
                                fieldWithPath("amount").description("??????"),
                                fieldWithPath("reservationMemo").description("???????????? ????????? ??????")
                        )
                        )
                );
    }

    @Test
    @DisplayName("?????? ??????/?????? 400 ?????? ????????????")
    void confirm_fail1() throws Exception {
        //given
        PatchReservationDto request = new PatchReservationDto("truw");
        String json = objectMapper.writeValueAsString(request);

        //when
        ResultActions perform = mockMvc.perform(patch("/meetings/{meetingId}/reservations/{reservationId}", 1, 1)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .contentType(APPLICATION_JSON)
                .content(json));

        //then
        perform.andExpect(status().isBadRequest())
                .andDo(document("patchReservation/400",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                        ),
                        pathParameters(
                                parameterWithName("meetingId").description("?????? ?????????"),
                                parameterWithName("reservationId").description("?????? ?????????")
                        ),
                        requestFields(
                                fieldWithPath("isAccepted").description("????????? ????????? ???/????????? ?????? ?????? true or false?????? ?????????."),
                                fieldWithPath("message").description("?????? ??????")
                        )
                        ));
    }

    @Test
    @DisplayName("?????? ??????/?????? 403 ?????? ????????????")
    void confirm_fail2() throws Exception {
        //given
        PatchReservationDto request = new PatchReservationDto("true");
        String json = objectMapper.writeValueAsString(request);
        willThrow(new ReservationException(REQUEST_DENIED)).given(reservationConfirmService).confirm(anyLong(), anyLong(), any(), any(PatchReservationDto.class));

        //when
        ResultActions perform = mockMvc.perform(patch("/meetings/{meetingId}/reservations/{reservationId}",1L,1L)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
                .content(json));

        //then
        perform.andExpect(status().isForbidden())
                .andDo(document("patchReservation/403",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description("?????? ????????? id??? ?????? ????????? id??? ???????????? ??????")
                        ),
                        pathParameters(
                                parameterWithName("meetingId").description("?????? ?????????"),
                                parameterWithName("reservationId").description("?????? ?????????")
                        ),
                        requestFields(
                                fieldWithPath("isAccepted").description("????????? ????????? ???/????????? ?????? ?????? true or false?????? ?????????."),
                                fieldWithPath("message").description("?????? ??????")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ??????/?????? 404 ?????? ????????????")
    void confirm_fail3() throws Exception {
        //given
        PatchReservationDto request = new PatchReservationDto("true");
        String json = objectMapper.writeValueAsString(request);
        willThrow(new MeetingException(DATA_NOT_FOUND)).given(reservationConfirmService).confirm(anyLong(), anyLong(), any(), any(PatchReservationDto.class));

        //when
        ResultActions perform = mockMvc.perform(patch("/meetings/{meetingId}/reservations/{reservationId}",1L,1L)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
                .content(json));

        //then
        perform.andExpect(status().isNotFound())
                .andDo(document("patchReservation/404",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                        ),
                        pathParameters(
                               parameterWithName("meetingId").description("???????????? ?????? ?????? ?????????"),
                               parameterWithName("reservationId").description("???????????? ?????? ?????? ?????????")
                        ),
                        requestFields(
                                fieldWithPath("isAccepted").description("true(??????), false(??????)"),
                                fieldWithPath("message").description("?????? ??????")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ??????/?????? 409 ?????? ????????????")
    void confirm_fail4() throws Exception {
        //given
        PatchReservationDto request = new PatchReservationDto("true");
        String json = objectMapper.writeValueAsString(request);
        willThrow(new ReservationException(CONFIRMED_RESERVATION)).given(reservationConfirmService).confirm(anyLong(), anyLong(), any(), any(PatchReservationDto.class));

        //when
        ResultActions perform = mockMvc.perform(patch("/meetings/{meetingId}/reservations/{reservationId}",1L,1L)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .contentType(APPLICATION_JSON)
                .content(json));

        //then
        perform.andExpect(status().isConflict())
                .andDo(document("patchReservation/409",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                        ),
                        pathParameters(
                                parameterWithName("meetingId").description("?????? ?????????"),
                                parameterWithName("reservationId").description("?????? ?????????")
                        ),
                        requestFields(
                                fieldWithPath("isAccepted").description("true(??????), false(??????)"),
                                fieldWithPath("message").description("?????? ??????")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ??????/?????? 200 ?????? ????????????")
    void confirm_success() throws Exception {
        //given
        PatchReservationDto request = new PatchReservationDto("true");
        String json = objectMapper.writeValueAsString(request);

        //when
        ResultActions perform = mockMvc.perform(patch("/meetings/{meetingId}/reservations/{reservationId}",1L,1L)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
                .content(json));

        //then
        perform.andExpect(status().isOk())
                .andDo(document("patchReservation/200",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                        ),
                        pathParameters(
                                parameterWithName("meetingId").description("?????? ?????????"),
                                parameterWithName("reservationId").description("?????? ?????????")
                        ),
                        requestFields(
                                fieldWithPath("isAccepted").description("true(??????), false(??????)"),
                                fieldWithPath("message").description("?????? ??????")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ?????? 400 ????????????")
    void cancel_fail_400() throws Exception {
        //given
        DeleteReservationDto request = DeleteReservationDto.builder()
                .paymentKey(" ")
                .cancelReason(" ")
                .build();
        String json = objectMapper.writeValueAsString(request);

        //when
        ResultActions perform = mockMvc.perform(delete("/meetings/{meetingId}/reservations/{reservationId}",1L,1L)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
                .content(json));

        //then
        perform.andExpect(status().isBadRequest())
                .andDo(document("deleteReservation/400",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                        ),
                        pathParameters(
                                parameterWithName("meetingId").description("?????? ?????????"),
                                parameterWithName("reservationId").description("?????? ?????????")
                        ),
                        requestFields(
                                fieldWithPath("paymentKey").description("paymentKey??? null, emtpy, white space??? ??? ????????????."),
                                fieldWithPath("cancelReason").description("cancelReason??? null, emtpy, white space??? ??? ????????????.")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ?????? 403 ????????????")
    void cancel_fail_403() throws Exception {
        //given
        DeleteReservationDto request = DeleteReservationDto.builder()
                .paymentKey(CONTENT1)
                .cancelReason(CONTENT2)
                .build();
        String json = objectMapper.writeValueAsString(request);

        willThrow(new PaymentException(REQUEST_DENIED))
                .given(reservationCancelService).cancelReservation(any(), any(), any());

        //when
        ResultActions perform = mockMvc.perform(delete("/meetings/{meetingId}/reservations/{reservationId}",1L,1L)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
                .content(json));

        //then
        perform.andExpect(status().isForbidden())
                .andDo(document("deleteReservation/403",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description("?????? ?????? ??????")
                        ),
                        pathParameters(
                                parameterWithName("meetingId").description("?????? ?????????"),
                                parameterWithName("reservationId").description("?????? ?????????")
                        ),
                        requestFields(
                                fieldWithPath("paymentKey").description("paymentKey"),
                                fieldWithPath("cancelReason").description("cancelReason")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ?????? 404 ????????????")
    void cancel_fail_404() throws Exception {
        //given
        DeleteReservationDto request = DeleteReservationDto.builder()
                .paymentKey(CONTENT1)
                .cancelReason(CONTENT2)
                .build();
        String json = objectMapper.writeValueAsString(request);

        willThrow(new PaymentException(DATA_NOT_FOUND))
                .given(reservationCancelService).cancelReservation(any(), any(), any());

        //when
        ResultActions perform = mockMvc.perform(delete("/meetings/{meetingId}/reservations/{reservationId}",1L,1L)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
                .content(json));

        //then
        perform.andExpect(status().isNotFound())
                .andDo(document("deleteReservation/404",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description("????????? ??????")
                        ),
                        pathParameters(
                                parameterWithName("meetingId").description("?????? ?????????"),
                                parameterWithName("reservationId").description("???????????? ?????? ??????")
                        ),
                        requestFields(
                                fieldWithPath("paymentKey").description("paymentKey"),
                                fieldWithPath("cancelReason").description("cancelReason")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ?????? 409 ????????????")
    void cancel_fail_409() throws Exception {
        //given
        DeleteReservationDto request = DeleteReservationDto.builder()
                .paymentKey(CONTENT1)
                .cancelReason(CONTENT2)
                .build();
        String json = objectMapper.writeValueAsString(request);

        willThrow(new PaymentException(CONFIRMED_RESERVATION))
                .given(reservationCancelService).cancelReservation(any(), any(), any());

        //when
        ResultActions perform = mockMvc.perform(delete("/meetings/{meetingId}/reservations/{reservationId}",1L,1L)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
                .content(json));

        //then
        perform.andExpect(status().isConflict())
                .andDo(document("deleteReservation/409",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description("????????? ??????")
                        ),
                        pathParameters(
                                parameterWithName("meetingId").description("?????? ?????????"),
                                parameterWithName("reservationId").description("?????? ?????? ????????? ??????")
                        ),
                        requestFields(
                                fieldWithPath("paymentKey").description("paymentKey"),
                                fieldWithPath("cancelReason").description("cancelReason")
                        )
                ));
    }

    @Test
    @DisplayName("?????? ?????? 204 ????????????")
    void cancel_fail_204() throws Exception {
        //given
        DeleteReservationDto request = DeleteReservationDto.builder()
                .paymentKey(CONTENT1)
                .cancelReason(CONTENT2)
                .build();
        String json = objectMapper.writeValueAsString(request);

        //when
        ResultActions perform = mockMvc.perform(delete("/meetings/{meetingId}/reservations/{reservationId}",1L,1L)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
                .content(json));

        //then
        perform.andExpect(status().isNoContent())
                .andDo(document("deleteReservation/204",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description("????????? ??????")
                        ),
                        pathParameters(
                                parameterWithName("meetingId").description("?????? ?????????"),
                                parameterWithName("reservationId").description("?????? ?????????")
                        ),
                        requestFields(
                                fieldWithPath("paymentKey").description("paymentKey"),
                                fieldWithPath("cancelReason").description("cancelReason")
                        )
                ));
    }
}