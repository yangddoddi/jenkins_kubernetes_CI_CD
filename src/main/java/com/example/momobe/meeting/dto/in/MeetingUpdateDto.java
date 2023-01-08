package com.example.momobe.meeting.dto.in;

import com.example.momobe.meeting.domain.enums.Category;
import com.example.momobe.meeting.domain.enums.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Builder
@AllArgsConstructor(access = PRIVATE)
public class MeetingUpdateDto {
    private Category category;
    private String title;
    private String content;
    private List<Tag> tags;
    private MeetingRequestDto.AddressDto address;
    private Integer personnel;
    private Long price;

    @Getter
    @Builder
    @AllArgsConstructor(access = PRIVATE)
    public static class AddressDto {
        private List<Long> addressIds;
        private String addressInfo;
    }
}
