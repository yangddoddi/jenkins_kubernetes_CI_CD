package com.example.momobe.meeting.integration;

import com.example.momobe.meeting.enums.MeetingConstant;
import com.example.momobe.security.domain.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static com.example.momobe.common.enums.TestConstants.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
public class MeetingRegistration_IntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private String accessToken;

    @BeforeEach
    void init() {
        accessToken = jwtTokenUtil.createAccessToken(EMAIL1, ID1, ROLE_USER_LIST, NICKNAME1);
    }

    @Test
    public void meetingRegistrationWithOneDay() throws Exception {
        // given
        String content = objectMapper.writeValueAsString(MeetingConstant.MEETING_REQUEST_DTO_WITH_ONE_DAY);

        // when
        ResultActions actions = mockMvc.perform(
                post("/meetings")
                        .content(content)
                        .header(JWT_HEADER, accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        actions.andExpect(status().isCreated())
                .andDo(print());
    }

}
