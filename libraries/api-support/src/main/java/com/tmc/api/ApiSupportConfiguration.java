package com.tmc.api;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
public class ApiSupportConfiguration {

    public static final String TRACE_ATTRIBUTE = "tmc.traceId";

    @Bean
    FilterRegistrationBean<OncePerRequestFilter> apiTraceFilter() {
        var filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain chain) throws ServletException, IOException {
                String incoming = request.getHeader("X-Correlation-ID");
                String traceId = incoming != null && incoming.matches("[a-zA-Z0-9-]{1,64}")
                        ? incoming : UUID.randomUUID().toString();
                request.setAttribute(TRACE_ATTRIBUTE, traceId);
                response.setHeader("X-Correlation-ID", traceId);
                chain.doFilter(request, response);
            }
        };
        var registration = new FilterRegistrationBean<OncePerRequestFilter>(filter);
        registration.setOrder(Integer.MIN_VALUE + 20);
        return registration;
    }

    @Bean
    EnvelopeAdvice envelopeAdvice(ObjectMapper mapper) {
        return new EnvelopeAdvice(mapper);
    }

    @Bean
    ApiSecurityHandlers apiSecurityHandlers(ObjectMapper mapper) {
        return new ApiSecurityHandlers(mapper);
    }

    @RestControllerAdvice(basePackages = "com.tmc")
    public static class EnvelopeAdvice implements ResponseBodyAdvice<Object> {

        private final ObjectMapper mapper;

        public EnvelopeAdvice(ObjectMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public boolean supports(MethodParameter method, Class<? extends HttpMessageConverter<?>> converterType) {
            return true;
        }

        @Override
        public Object beforeBodyWrite(Object body, MethodParameter method, MediaType contentType,
                Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request, ServerHttpResponse response) {
            String path = request.getURI().getPath();
            if (path.startsWith("/oauth2/") || path.startsWith("/.well-known/")
                    || path.startsWith("/actuator/") || path.equals("/internal/qr/jwks") || body instanceof ApiResponse) {
                return body;
            }
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            int status = ((ServletServerHttpResponse) response).getServletResponse().getStatus();
            if (request.getMethod() == org.springframework.http.HttpMethod.HEAD || status == 204 || status == 304 || status < 200) {
                return null;
            }
            String code = status < 400 ? "SUCCESS" : "REQUEST_FAILED";
            String message = status < 400 ? "Xử lý thành công" : "Không thể xử lý yêu cầu";
            Object data = body;
            if (body instanceof ProblemDetail problem) {
                code = safeCode(problem.getDetail(), status);
                message = errorMessage(status);
                data = null;
            }
            var envelope = new ApiResponse(code, message, data, Instant.now(), traceId(servletRequest), null);
            if (StringHttpMessageConverter.class.isAssignableFrom(converterType)) {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return mapper.writeValueAsString(envelope);
            }
            return envelope;
        }

        @ExceptionHandler(ResponseStatusException.class)
        ResponseEntity<ApiResponse> statusException(ResponseStatusException exception, HttpServletRequest request) {
            int status = exception.getStatusCode().value();
            return ResponseEntity.status(status).body(new ApiResponse(safeCode(exception.getReason(), status),
                    errorMessage(status), null, Instant.now(), traceId(request), null));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        ResponseEntity<ApiResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
            var errors = exception.getBindingResult().getFieldErrors().stream()
                    .map(error -> new ApiResponse.FieldError(error.getField(), "Giá trị không hợp lệ")).distinct().toList();
            return ResponseEntity.badRequest().body(new ApiResponse("VALIDATION_ERROR", "Dữ liệu không hợp lệ",
                    null, Instant.now(), traceId(request), errors));
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        ResponseEntity<ApiResponse> malformed(HttpServletRequest request) {
            return ResponseEntity.badRequest().body(new ApiResponse("INVALID_JSON", "Dữ liệu không hợp lệ",
                    null, Instant.now(), traceId(request), null));
        }

        @ExceptionHandler(AccessDeniedException.class)
        ResponseEntity<ApiResponse> forbidden(HttpServletRequest request) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse("FORBIDDEN", errorMessage(403),
                    null, Instant.now(), traceId(request), null));
        }

        @ExceptionHandler(Exception.class)
        ResponseEntity<ApiResponse> unexpected(Exception exception, HttpServletRequest request) {
            String traceId = traceId(request);
            if (exception instanceof org.springframework.web.ErrorResponse errorResponse) {
                int status = errorResponse.getStatusCode().value();
                return ResponseEntity.status(status).body(new ApiResponse("HTTP_" + status, errorMessage(status),
                        null, Instant.now(), traceId, null));
            }
            if (exception instanceof org.springframework.web.method.annotation.MethodArgumentTypeMismatchException) {
                return ResponseEntity.badRequest().body(new ApiResponse("INVALID_PARAMETER", errorMessage(400),
                        null, Instant.now(), traceId, null));
            }
            org.slf4j.LoggerFactory.getLogger(EnvelopeAdvice.class).error("Unhandled API error traceId={} type={}",
                    traceId, exception.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(new ApiResponse("INTERNAL_ERROR", errorMessage(500),
                    null, Instant.now(), traceId, null));
        }
    }

    static String traceId(HttpServletRequest request) {
        Object trace = request.getAttribute(TRACE_ATTRIBUTE);
        return trace == null ? UUID.randomUUID().toString() : trace.toString();
    }

    static String safeCode(String reason, int status) {
        return reason != null && reason.matches("[A-Z][A-Z0-9_]{1,79}") ? reason : "HTTP_" + status;
    }

    static String errorMessage(int status) {
        return switch (status) {
            case 400 -> "Dữ liệu không hợp lệ";
            case 401 -> "Yêu cầu xác thực";
            case 403 -> "Không có quyền truy cập";
            case 404 -> "Không tìm thấy dữ liệu";
            case 409 -> "Dữ liệu xung đột";
            case 503 -> "Dịch vụ tạm thời không sẵn sàng";
            default -> "Không thể xử lý yêu cầu";
        };
    }
}
