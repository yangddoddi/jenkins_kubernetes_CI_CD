package com.example.momobe.meeting.ui;

import com.example.momobe.common.config.SecurityTestConfig;
import com.example.momobe.common.resolver.JwtArgumentResolver;
import com.example.momobe.meeting.dao.MeetingHostQueryRepository;
import com.example.momobe.meeting.dao.MeetingParticipantQueryRepository;
import com.example.momobe.meeting.domain.enums.DatePolicy;
import com.example.momobe.meeting.dto.out.MeetingDateTimeDto;
import com.example.momobe.meeting.dto.out.MeetingHostResponseDto;
import com.example.momobe.meeting.dto.out.MeetingInfoDto;
import com.example.momobe.meeting.dto.out.MeetingParticipantResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static com.example.momobe.common.config.ApiDocumentUtils.getDocumentRequest;
import static com.example.momobe.common.config.ApiDocumentUtils.getDocumentResponse;
import static com.example.momobe.common.enums.PageConstants.*;
import static com.example.momobe.common.enums.TestConstants.*;
import static com.example.momobe.meeting.domain.enums.Category.SOCIAL;
import static com.example.momobe.meeting.domain.enums.MeetingState.OPEN;
import static com.example.momobe.meeting.enums.MeetingConstants.*;
import static com.example.momobe.reservation.domain.enums.ReservationState.ACCEPT;
import static com.example.momobe.reservation.domain.enums.ReservationState.PAYMENT_SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeetingMyPageQueryController.class)
@MockBean(JpaMetamodelMappingContext.class)
@Import(SecurityTestConfig.class)
@AutoConfigureRestDocs
class MeetingMyPageQueryControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private JwtArgumentResolver jwtArgumentResolver;
    @MockBean
    private MeetingHostQueryRepository meetingHostQueryRepository;
    @MockBean
    private MeetingParticipantQueryRepository meetingParticipantQueryRepository;

    @Test
    void meetingHostQuery() throws Exception {
        // given
        MeetingHostResponseDto.ApplicationDto applicationDto = new MeetingHostResponseDto.ApplicationDto(
                List.of(new MeetingHostResponseDto.RequestDto(
                        new MeetingInfoDto.ReservationDto(
                                ID1, ID2, NICKNAME1, REMOTE_PATH, EMAIL2, PAYMENT_SUCCESS,
                                LocalDate.now(), START_TIME, END_TIME, "??? ???????????????~"))),
                List.of(new MeetingHostResponseDto.RequestConfirmedDto(
                        new MeetingInfoDto.ReservationDto(
                                ID2, ID3, NICKNAME2, REMOTE_PATH, EMAIL1, ACCEPT,
                                LocalDate.now(), START_TIME, END_TIME, "??? ???????????????~")))
        );
        MeetingHostResponseDto meetingHostResponseDto = new MeetingHostResponseDto(
                ID1, SOCIAL, ID1, NICKNAME, REMOTE_PATH, EMAIL2, TITLE1, CONTENT1, SUB_ADDRESS1, OPEN,
                DatePolicy.FREE, START_DATE, END_DATE, START_TIME, END_TIME, 3, 1000L
        );
        meetingHostResponseDto.init(
                List.of("????????? ?????????", "????????? ?????????"),
                List.of(1, 3, 7), List.of(LocalDate.now(), LocalDate.now().plusDays(1))
                , applicationDto);
        PageRequest pageRequest = PageRequest.of(PAGE - 1, SIZE);

        given(meetingHostQueryRepository.findAll(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(meetingHostResponseDto), pageRequest, 1L));

        // when
        ResultActions actions = mockMvc.perform(
                get("/mypage/meetings/hosts")
                        .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                        .param("page", String.valueOf(PAGE))
                        .param("size", String.valueOf(SIZE))
        );

        // then
        actions.andExpect(status().isOk())
                .andDo(document("mypage/meetings/hosts",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        REQUEST_HEADER_JWT,
                        requestParameters(
                                PWN_PAGE, PWN_SIZE
                        ),
                        responseFields(
                                FWP_CONTENT, FWP_CONTENT_MEETING_ID, FWP_CONTENT_CATEGORY,
                                FWP_CONTENT_HOST, FWP_CONTENT_HOST_USER_ID, FWP_CONTENT_HOST_NICKNAME,
                                FWP_CONTENT_HOST_IMAGE_URL, FWP_CONTENT_HOST_EMAIL,
                                FWP_CONTENT_TITLE, FWP_CONTENT_CONTENT,
                                FWP_CONTENT_ADDRESS, FWP_CONTENT_ADDRESS_ADDRESSES, FWP_CONTENT_ADDRESS_ADDRESS_INFO,
                                FWP_CONTENT_MEETING_STATE, FWP_CONTENT_IS_OPEN,
                                FWP_CONTENT_DATE_TIME, FWP_CONTENT_DATE_TIME_DATE_POLICY, FWP_CONTENT_DATE_TIME_START_DATE,
                                FWP_CONTENT_DATE_TIME_END_DATE, FWP_CONTENT_DATE_TIME_START_TIME, FWP_CONTENT_DATE_TIME_END_TIME,
                                FWP_CONTENT_DATE_TIME_MAX_TIME, FWP_CONTENT_DATE_TIME_DAY_WEEKS, FWP_CONTENT_DATE_TIME_DATES,
                                FWP_CONTENT_PRICE,

                                fieldWithPath("content[].applications").type(OBJECT).description("?????????"),
                                fieldWithPath("content[].applications.requests").type(ARRAY).description("?????? ??????"),
                                fieldWithPath("content[].applications.requests[].reservationId").type(NUMBER).description("?????? ?????????"),
                                fieldWithPath("content[].applications.requests[].userId").type(NUMBER).description("????????? ?????????"),
                                fieldWithPath("content[].applications.requests[].nickname").type(STRING).description("????????? ?????????"),
                                fieldWithPath("content[].applications.requests[].imageUrl").type(STRING).description("????????? ?????????"),
                                fieldWithPath("content[].applications.requests[].reservationState").type(STRING).description("?????? ??????"),
                                fieldWithPath("content[].applications.requests[].message").type(STRING).description("?????? ??????"),
                                fieldWithPath("content[].applications.requests[].dateTimeInfo").type(OBJECT).description("?????? ??????"),
                                fieldWithPath("content[].applications.requests[].dateTimeInfo.date").type(STRING).description("?????? ??????"),
                                fieldWithPath("content[].applications.requests[].dateTimeInfo.time").type(STRING).description("?????? ??????"),

                                fieldWithPath("content[].applications.confirmed").type(ARRAY).description("????????? ????????? ??????"),
                                fieldWithPath("content[].applications.confirmed[].reservationId").type(NUMBER).description("?????? ?????????"),
                                fieldWithPath("content[].applications.confirmed[].userId").type(NUMBER).description("????????? ?????????"),
                                fieldWithPath("content[].applications.confirmed[].nickname").type(STRING).description("????????? ?????????"),
                                fieldWithPath("content[].applications.confirmed[].imageUrl").type(STRING).description("????????? ?????????"),
                                fieldWithPath("content[].applications.confirmed[].reservationState").type(STRING).description("?????? ??????"),
                                fieldWithPath("content[].applications.confirmed[].email").type(STRING).description("????????? ?????????"),
                                fieldWithPath("content[].applications.confirmed[].message").type(STRING).description("?????? ??????"),
                                fieldWithPath("content[].applications.confirmed[].dateTimeInfo").type(OBJECT).description("?????? ??????"),
                                fieldWithPath("content[].applications.confirmed[].dateTimeInfo.date").type(STRING).description("?????? ??????"),
                                fieldWithPath("content[].applications.confirmed[].dateTimeInfo.time").type(STRING).description("?????? ??????"),

                                FWP_PAGE_INFO, FWP_PAGE, FWP_SIZE, FWP_TOTAL_ELEMENTS, FWP_TOTAL_PAGES
                        )
                ));
    }

    @Test
    void meetingParticipantQuery() throws Exception {
        // given
        MeetingParticipantResponseDto.ApplicationDto applicationDto =
                new MeetingParticipantResponseDto.ApplicationDto(
                        ID2, NICKNAME1, REMOTE_PATH, EMAIL1,
                        PAYMENT_SUCCESS, "??? ???????????????~",
                        "ABC123",
                        new MeetingDateTimeDto(
                                LocalDate.now(), START_TIME, END_TIME),
                        ID3);
        MeetingParticipantResponseDto meetingParticipantResponseDto = new MeetingParticipantResponseDto(
                ID1, SOCIAL, ID1, NICKNAME, REMOTE_PATH, EMAIL1, TITLE1, CONTENT1, SUB_ADDRESS1, OPEN,
                DatePolicy.FREE, START_DATE, END_DATE, START_TIME, END_TIME, 3, 1000L, applicationDto
        );
        meetingParticipantResponseDto.init(
                List.of("????????? ?????????", "????????? ?????????"),
                List.of(1, 3, 7), List.of(LocalDate.now(), LocalDate.now().plusDays(1)));
        PageRequest pageRequest = PageRequest.of(PAGE - 1, SIZE);

        given(meetingParticipantQueryRepository.findAll(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(meetingParticipantResponseDto), pageRequest, 1L));

        // when
        ResultActions actions = mockMvc.perform(
                get("/mypage/meetings/participants")
                        .header(JWT_HEADER, BEARER_ACCESS_TOKEN)
                        .param("page", String.valueOf(PAGE))
                        .param("size", String.valueOf(SIZE))
        );

        // then
        actions.andExpect(status().isOk())
                .andDo(document("mypage/meetings/participants",
                        getDocumentRequest(),
                        getDocumentResponse(),
                        REQUEST_HEADER_JWT,
                        requestParameters(
                                PWN_PAGE, PWN_SIZE
                        ),
                        responseFields(
                                FWP_CONTENT, FWP_CONTENT_MEETING_ID, FWP_CONTENT_CATEGORY,
                                FWP_CONTENT_HOST, FWP_CONTENT_HOST_USER_ID, FWP_CONTENT_HOST_NICKNAME,
                                FWP_CONTENT_HOST_IMAGE_URL, FWP_CONTENT_HOST_EMAIL,
                                FWP_CONTENT_TITLE, FWP_CONTENT_CONTENT,
                                FWP_CONTENT_ADDRESS, FWP_CONTENT_ADDRESS_ADDRESSES, FWP_CONTENT_ADDRESS_ADDRESS_INFO,
                                FWP_CONTENT_MEETING_STATE, FWP_CONTENT_IS_OPEN,
                                FWP_CONTENT_DATE_TIME, FWP_CONTENT_DATE_TIME_DATE_POLICY, FWP_CONTENT_DATE_TIME_START_DATE,
                                FWP_CONTENT_DATE_TIME_END_DATE, FWP_CONTENT_DATE_TIME_START_TIME, FWP_CONTENT_DATE_TIME_END_TIME,
                                FWP_CONTENT_DATE_TIME_MAX_TIME, FWP_CONTENT_DATE_TIME_DAY_WEEKS, FWP_CONTENT_DATE_TIME_DATES,
                                FWP_CONTENT_PRICE,

                                fieldWithPath("content[].application").type(OBJECT).description("?????? ??????"),
                                fieldWithPath("content[].application.userId").type(NUMBER).description("????????? ?????????"),
                                fieldWithPath("content[].application.nickname").type(STRING).description("????????? ?????????"),
                                fieldWithPath("content[].application.imageUrl").type(STRING).description("????????? ?????????"),
                                fieldWithPath("content[].application.reservationState").type(STRING).description("?????? ??????"),
                                fieldWithPath("content[].application.email").type(STRING).description("?????????"),
                                fieldWithPath("content[].application.message").type(STRING).description("?????? ??????"),
                                fieldWithPath("content[].application.paymentKey").type(STRING).description("?????? ???"),
                                fieldWithPath("content[].application.dateTimeInfo").type(OBJECT).description("?????? ??????"),
                                fieldWithPath("content[].application.dateTimeInfo.date").type(STRING).description("?????? ??????"),
                                fieldWithPath("content[].application.dateTimeInfo.time").type(STRING).description("?????? ??????"),
                                fieldWithPath("content[].application.reservationId").type(NUMBER).description("?????? ?????????"),

                                FWP_PAGE_INFO, FWP_PAGE, FWP_SIZE, FWP_TOTAL_ELEMENTS, FWP_TOTAL_PAGES
                        )
                ));
    }
}
