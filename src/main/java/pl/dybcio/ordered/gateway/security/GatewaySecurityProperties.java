package pl.dybcio.ordered.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "ordered.gateway.security")
public record GatewaySecurityProperties(
        String jwtSecret,
        List<String> publicPaths
) {
}