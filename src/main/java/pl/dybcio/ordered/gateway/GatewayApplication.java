package pl.dybcio.ordered.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import pl.dybcio.ordered.gateway.security.GatewaySecurityProperties;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}