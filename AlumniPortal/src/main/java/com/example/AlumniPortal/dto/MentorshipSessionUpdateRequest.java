package com.example.AlumniPortal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MentorshipSessionUpdateRequest {

    @NotBlank(message = "Meeting platform is required")
    @Size(max = 100, message = "Meeting platform must be at most 100 characters")
    private String meetingPlatform;

    @Size(max = 300, message = "Meeting link must be at most 300 characters")
    private String meetingLink;

    @Size(max = 500, message = "Meeting notes must be at most 500 characters")
    private String meetingNotes;
}
