package processThreadVisualizer.PTV.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // /topic prefix — browser subscribes to these to RECEIVE data
        config.enableSimpleBroker("/topic");
        // /app prefix — browser sends messages TO server using this
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // /ws is the WebSocket handshake endpoint
        // withSockJS() adds SockJS fallback for older browsers
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}

