package com.parkminje.pingpong.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Set;

/**
 * 실시간 갱신 신호용 STOMP. 서버 → 클라이언트 방향만 사용한다.
 * 클라이언트는 /topic/tournament/{id} 구독만 할 수 있고 SEND 는 거부한다.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Set<StompCommand> ALLOWED = Set.of(
            StompCommand.CONNECT, StompCommand.STOMP, StompCommand.SUBSCRIBE, StompCommand.UNSUBSCRIBE, StompCommand.DISCONNECT);

    private final AppProperties props;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic").setHeartbeatValue(new long[]{20000, 20000})
                .setTaskScheduler(heartbeatScheduler());
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        var endpoint = registry.addEndpoint("/ws");
        if (!props.allowedOrigins().isEmpty()) {
            endpoint.setAllowedOriginPatterns(props.allowedOrigins().toArray(String[]::new));
        }
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                StompCommand command = accessor.getCommand();
                if (command == null) {
                    return message; // heartbeat
                }
                if (!ALLOWED.contains(command)) {
                    throw new MessageDeliveryException("허용되지 않는 STOMP 명령: " + command);
                }
                if (command == StompCommand.SUBSCRIBE) {
                    String dest = accessor.getDestination();
                    if (dest == null || !dest.matches("^/topic/tournament/\\d+$")) {
                        throw new MessageDeliveryException("구독할 수 없는 경로: " + dest);
                    }
                }
                return message;
            }
        });
    }

    private org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler heartbeatScheduler() {
        var scheduler = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}
