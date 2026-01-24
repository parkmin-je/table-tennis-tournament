package com.yourcompany.pingpong.modules.user.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateForm {

    @NotEmpty(message = "아이디를 입력하세요.")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$",
             message = "아이디는 영문, 숫자, 밑줄(_)만 사용 가능하며 4~20자여야 합니다.")
    private String username;

    @NotEmpty(message = "비밀번호를 입력하세요.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,20}$",
             message = "비밀번호는 영문, 숫자, 특수문자를 포함해 6~20자여야 합니다.")
    private String password1;

    @NotEmpty(message = "비밀번호 확인을 입력하세요.")
    private String password2;

    @NotEmpty(message = "이름을 입력하세요.")
    private String name;

    @NotEmpty(message = "이메일을 입력하세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
}
