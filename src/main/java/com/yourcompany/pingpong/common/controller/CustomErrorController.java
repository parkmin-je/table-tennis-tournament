package com.yourcompany.pingpong.common.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            // 로깅
            log.error("Error occurred - Status: {}, Message: {}, URI: {}",
                    statusCode, message, request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));

            if (exception != null) {
                log.error("Exception: ", (Throwable) exception);
            }

            model.addAttribute("status", statusCode);
            model.addAttribute("message", message);
            model.addAttribute("error", exception);

            // 상태 코드별 페이지 분기
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "error/404";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                return "error/403";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return "error/500";
            }
        }

        return "error/error";
    }
}