package com.yourcompany.pingpong.modules.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerSimpleDTO {
    private Long id;
    private String name;
    private Integer ranking;
    private String gender;
    private String clubName;
}