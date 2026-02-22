package com.yourcompany.pingpong.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /* ─────────────────────────────────────────────────────────────
     * 404 - 리소스 없음
     * ───────────────────────────────────────────────────────────── */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(HttpServletRequest req, Model model) {
        model.addAttribute("errorCode", 404);
        model.addAttribute("errorMessage", "요청하신 페이지를 찾을 수 없습니다.");
        model.addAttribute("path", req.getRequestURI());
        return "error/404";
    }

    /* ─────────────────────────────────────────────────────────────
     * 403 - 접근 거부
     * ───────────────────────────────────────────────────────────── */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(HttpServletRequest req, Model model) {
        log.warn("ACCESS DENIED: {} → {}", req.getRemoteAddr(), req.getRequestURI());
        model.addAttribute("errorCode", 403);
        model.addAttribute("errorMessage", "접근 권한이 없습니다.");
        model.addAttribute("path", req.getRequestURI());
        return "error/403";
    }

    /* ─────────────────────────────────────────────────────────────
     * 비즈니스 로직 오류 (IllegalArgumentException)
     * - REST API 요청: JSON 응답
     * - 일반 요청: 에러 페이지
     * ───────────────────────────────────────────────────────────── */
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(HttpServletRequest req, IllegalArgumentException ex, Model model) {
        log.warn("ILLEGAL_ARGUMENT: {} → {}", req.getRequestURI(), ex.getMessage());
        if (isApiRequest(req)) {
            return ResponseEntity.badRequest().body(errorBody(400, ex.getMessage(), req));
        }
        model.addAttribute("errorCode", 400);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }

    /* ─────────────────────────────────────────────────────────────
     * 상태 오류 (IllegalStateException)
     * ───────────────────────────────────────────────────────────── */
    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalState(HttpServletRequest req, IllegalStateException ex, Model model) {
        log.warn("ILLEGAL_STATE: {} → {}", req.getRequestURI(), ex.getMessage());
        if (isApiRequest(req)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(409, ex.getMessage(), req));
        }
        model.addAttribute("errorCode", 409);
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/error";
    }

    /* ─────────────────────────────────────────────────────────────
     * 유효성 검증 오류 (@Valid)
     * ───────────────────────────────────────────────────────────── */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Map<String, Object>> handleValidation(HttpServletRequest req, BindException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("VALIDATION: {} → {}", req.getRequestURI(), details);
        return ResponseEntity.badRequest().body(errorBody(400, "입력값 검증 실패: " + details, req));
    }

    /* ─────────────────────────────────────────────────────────────
     * 그 외 모든 예외 (500)
     * ───────────────────────────────────────────────────────────── */
    @ExceptionHandler(Exception.class)
    public Object handleGeneral(HttpServletRequest req, Exception ex, Model model) {
        log.error("UNHANDLED_EXCEPTION: {} → {}", req.getRequestURI(), ex.getMessage(), ex);
        if (isApiRequest(req)) {
            return ResponseEntity.internalServerError().body(errorBody(500, "서버 내부 오류가 발생했습니다.", req));
        }
        model.addAttribute("errorCode", 500);
        model.addAttribute("errorMessage", "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        return "error/500";
    }

    /* ─────────────────────────────────────────────────────────────
     * 유틸
     * ───────────────────────────────────────────────────────────── */
    private boolean isApiRequest(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        String xReq   = req.getHeader("X-Requested-With");
        return (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equals(xReq);
    }

    private Map<String, Object> errorBody(int status, String message, HttpServletRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("message", message);
        body.put("path", req.getRequestURI());
        return body;
    }
}
