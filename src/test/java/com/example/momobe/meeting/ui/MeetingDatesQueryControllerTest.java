package com.example.momobe.meeting.ui;

import com.example.momobe.common.config.ApiDocumentUtils;
import com.example.momobe.common.config.SecurityTestConfig;
import com.example.momobe.common.exception.ui.ExceptionController;
import com.example.momobe.common.resolver.JwtArgumentResolver;
import com.example.momobe.meeting.dao.MonthlyMeetingScheduleInquiry;
import com.example.momobe.meeting.domain.enums.Category;
import com.example.momobe.meeting.domain.enums.DatePolicy;
import com.example.momobe.meeting.dto.out.ResponseMeetingDatesDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.example.momobe.common.enums.TestConstants.*;
import static org.aspectj.apache.bcel.generic.ObjectType.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
@AutoConfigureRestDocs
@Import({SecurityTestConfig.class})
@MockBean(JpaMetamodelMappingContext.class)
@WebMvcTest({MeetingDatesQueryController.class, ExceptionController.class})
class MeetingDatesQueryControllerTest {
    @MockBean
    JwtArgumentResolver jwtArgumentResolver;

    @MockBean
    MonthlyMeetingScheduleInquiry monthlyMeetingScheduleInquiry;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("?????? ????????? ?????? ?????????")
    void getMeetingDates() throws Exception {
        //given
        Long meetingId = 1L;
        LocalDate localDate = LocalDate.now();
        ResponseMeetingDatesDto dto1 = ResponseMeetingDatesDto.builder()
                .dateTime(LocalDateTime.of(localDate.getYear(), localDate.getMonth(), localDate.getDayOfMonth(), 10, 0))
                .date(LocalDate.now())
                .time(LocalTime.of(10, 0))
                .availability("true")
                .datePolicy(DatePolicy.PERIOD.toString())
                .category(Category.DESIGN.toString())
                .currentStaff(2)
                .maxTime(2)
                .personnel(4)
                .price(10000)
                .build();

        ResponseMeetingDatesDto dto2 = ResponseMeetingDatesDto.builder()
                .dateTime(LocalDateTime.of(localDate.getYear(), localDate.getMonth(), localDate.getDayOfMonth(), 11, 0))
                .date(LocalDate.now())
                .time(LocalTime.of(11, 0))
                .availability("true")
                .datePolicy(DatePolicy.PERIOD.toString())
                .category(Category.DESIGN.toString())
                .currentStaff(2)
                .maxTime(4)
                .personnel(4)
                .price(10000)
                .build();

        List<ResponseMeetingDatesDto> respone = List.of(dto1, dto2);

        BDDMockito.given(monthlyMeetingScheduleInquiry.getSchedules(meetingId, localDate.getMonthValue())).willReturn(respone);

        //when
        ResultActions perform = mockMvc.perform(get("/meetings/{meetingId}/reservations/dates/{date}", meetingId, localDate)
                .header(JWT_HEADER, BEARER_ACCESS_TOKEN));

        //then
        perform.andExpect(status().isOk())
                .andDo(document("getMeetingDates/200",
                        ApiDocumentUtils.getDocumentRequest(),
                        ApiDocumentUtils.getDocumentResponse(),
                        requestHeaders(
                                headerWithName(JWT_HEADER).description(ACCESS_TOKEN)
                        ),
                        responseFields(
                                fieldWithPath("[].dateTime").type(STRING).description("????????? ?????????"),
                                fieldWithPath("[].date").type(STRING).description("?????????"),
                                fieldWithPath("[].time").type(STRING).description("?????????"),
                                fieldWithPath("[].availability").type(STRING).description("?????? ?????? ??????"),
                                fieldWithPath("[].datePolicy").type(STRING).description("?????? ??????"),
                                fieldWithPath("[].category").type(STRING).description("????????????"),
                                fieldWithPath("[].currentStaff").type(INTEGER).description("?????? ?????? ?????????"),
                                fieldWithPath("[].maxTime").type(INTEGER).description("?????? ?????? ?????? ??????"),
                                fieldWithPath("[].personnel").type(INTEGER).description("?????? ??????"),
                                fieldWithPath("[].price").type(INTEGER).description("?????? ?????? (????????? ?????? ????????? ???????????? ??????)")
                        )
                        ));
    }
}