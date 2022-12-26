package com.example.momobe.meeting.enums;

import com.example.momobe.meeting.domain.enums.Category;
import com.example.momobe.meeting.domain.enums.PricePolicy;
import com.example.momobe.meeting.domain.enums.Tag;
import com.example.momobe.meeting.dto.MeetingRequestDto;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static com.example.momobe.common.enums.TestConstants.*;

public class MeetingConstant {
    public static final MeetingRequestDto.PriceDto PRICE_DTO = MeetingRequestDto.PriceDto.builder()
            .pricePolicy(PricePolicy.HOUR)
            .price(1000L)
            .build();
    public static final MeetingRequestDto.LocationDto LOCATION_DTO = MeetingRequestDto.LocationDto.builder()
            .address1(ADDRESS1)
            .address2(ADDRESS2)
            .build();
    public static final MeetingRequestDto MEETING_REQUEST_DTO = MeetingRequestDto.builder()
            .category(Category.MENTORING)
            .title(TITLE1)
            .content(CONTENT1)
            .tags(List.of(Tag.LIFESTYLE, Tag.MEDIA, Tag.EDU))
            .priceInfo(PRICE_DTO)
            .locations(List.of(LOCATION_DTO))
            .dateTimes(List.of(NOW_TIME, NOW_TIME.plus(1L, ChronoUnit.HOURS)))
            .notice("전달 사항")
            .build();
}