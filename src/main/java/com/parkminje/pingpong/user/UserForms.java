package com.parkminje.pingpong.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public final class UserForms {

    private UserForms() {
    }

    static final String PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,64}$";
    static final String PASSWORD_MESSAGE = "비밀번호는 영문·숫자·특수문자를 포함해 8자 이상이어야 합니다.";

    @Getter
    @Setter
    public static class SignUp {
        @NotBlank(message = "아이디를 입력하세요.")
        @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "아이디는 영문·숫자·밑줄 4~20자입니다.")
        private String username;

        @NotBlank(message = "비밀번호를 입력하세요.")
        @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE)
        private String password;

        @NotBlank(message = "비밀번호 확인을 입력하세요.")
        private String passwordConfirm;

        @NotBlank(message = "이름을 입력하세요.")
        @Size(max = 20, message = "이름은 20자 이내로 입력하세요.")
        private String name;

        @NotBlank(message = "이메일을 입력하세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 120)
        private String email;
    }

    @Getter
    @Setter
    public static class Profile {
        @NotBlank(message = "이름을 입력하세요.")
        @Size(max = 20, message = "이름은 20자 이내로 입력하세요.")
        private String name;

        @NotBlank(message = "이메일을 입력하세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 120)
        private String email;
    }

    @Getter
    @Setter
    public static class Password {
        @NotBlank(message = "현재 비밀번호를 입력하세요.")
        private String current;

        @NotBlank(message = "새 비밀번호를 입력하세요.")
        @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE)
        private String next;

        @NotBlank(message = "새 비밀번호 확인을 입력하세요.")
        private String confirm;
    }
}
