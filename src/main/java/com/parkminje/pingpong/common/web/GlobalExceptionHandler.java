package com.parkminje.pingpong.common.web;

import com.parkminje.pingpong.bracket.GameScore;
import com.parkminje.pingpong.common.BusinessException;
import com.parkminje.pingpong.common.ForbiddenException;
import com.parkminje.pingpong.common.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import java.net.URI;
import java.util.Map;

/**
 * 예외를 사용자에게 보여줄 형태로 바꾼다.
 * <ul>
 *   <li>업무 규칙 위반: 폼 요청이면 이전 화면으로 돌아가 메시지 표시, AJAX 면 JSON</li>
 *   <li>그 외 예외: 내부 정보 노출 없이 일반 오류 화면</li>
 * </ul>
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private final com.parkminje.pingpong.config.AppProperties props;

    public GlobalExceptionHandler(com.parkminje.pingpong.config.AppProperties props) {
        this.props = props;
    }

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView notFound(NotFoundException e, HttpServletRequest req, HttpServletResponse res) {
        return respond(req, res, HttpStatus.NOT_FOUND, e.getMessage(), false);
    }

    @ExceptionHandler({ForbiddenException.class})
    public ModelAndView forbidden(ForbiddenException e, HttpServletRequest req, HttpServletResponse res) {
        return respond(req, res, HttpStatus.FORBIDDEN, e.getMessage(), true);
    }

    /** 메서드 보안(@PreAuthorize) 거부: 비로그인이면 로그인 화면, 로그인 상태면 403 */
    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView accessDenied(AccessDeniedException e, HttpServletRequest req, HttpServletResponse res) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = auth == null || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;
        if (anonymous && !isAjax(req)) {
            return new ModelAndView("redirect:/user/login");
        }
        return respond(req, res, anonymous ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN,
                anonymous ? "로그인이 필요합니다." : "권한이 없습니다.", false);
    }

    @ExceptionHandler({BusinessException.class, GameScore.ScoreException.class})
    public ModelAndView business(RuntimeException e, HttpServletRequest req, HttpServletResponse res) {
        return respond(req, res, HttpStatus.BAD_REQUEST, e.getMessage(), true);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class,
            CannotAcquireLockException.class})
    public ModelAndView conflict(RuntimeException e, HttpServletRequest req, HttpServletResponse res) {
        log.warn("동시 수정 충돌: {}", e.getMessage());
        return respond(req, res, HttpStatus.CONFLICT, "다른 운영진이 동시에 수정했습니다. 화면을 새로고침한 뒤 다시 시도하세요.", true);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ModelAndView badRequest(Exception e, HttpServletRequest req, HttpServletResponse res) {
        return respond(req, res, HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다.", true);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView noResource(NoResourceFoundException e, HttpServletRequest req, HttpServletResponse res) {
        return respond(req, res, HttpStatus.NOT_FOUND, "페이지를 찾을 수 없습니다.", false);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView unexpected(Exception e, HttpServletRequest req, HttpServletResponse res) {
        log.error("처리되지 않은 오류: {} {}", req.getMethod(), req.getRequestURI(), e);
        return respond(req, res, HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도하세요.", false);
    }

    private ModelAndView respond(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String message,
                                 boolean redirectBackOnPost) {
        if (isAjax(req)) {
            res.setStatus(status.value());
            ModelAndView mv = new ModelAndView(new MappingJackson2JsonView());
            mv.addAllObjects(Map.of("error", message, "status", status.value()));
            return mv;
        }
        if (redirectBackOnPost && "POST".equalsIgnoreCase(req.getMethod())) {
            RequestContextUtils.getOutputFlashMap(req).put("flashError", message);
            return new ModelAndView("redirect:" + safeReferer(req));
        }
        res.setStatus(status.value());
        ModelAndView mv = new ModelAndView(status == HttpStatus.NOT_FOUND ? "error/404"
                : status == HttpStatus.FORBIDDEN ? "error/403"
                : status.is5xxServerError() ? "error/500" : "error/error");
        mv.addObject("message", message);
        mv.addObject("status", status.value());
        mv.addObject("appName", props.name());
        return mv;
    }

    static boolean isAjax(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return "XMLHttpRequest".equals(req.getHeader("X-Requested-With"))
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE) && !accept.contains("text/html"))
                || req.getRequestURI().startsWith(req.getContextPath() + "/api/");
    }

    /** 같은 사이트의 이전 화면 경로만 허용 (오픈 리다이렉트 방지) */
    static String safeReferer(HttpServletRequest req) {
        String referer = req.getHeader("Referer");
        if (referer == null) {
            return "/";
        }
        try {
            URI uri = URI.create(referer);
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(req.getServerName())) {
                return "/";
            }
            String path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
            if (!path.startsWith("/") || path.startsWith("//")) {
                return "/";
            }
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}
