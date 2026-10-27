package br.gov.sp.cps.produtos_soap.config;

import java.util.Map;

import br.gov.sp.cps.produtos_soap.security.ProdutoSecurityInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.ws.soap.security.wss4j2.cache.ConcurrentMapReplayCache;
import org.springframework.ws.soap.security.wss4j2.callback.SimplePasswordValidationCallbackHandler;

@Configuration
public class SecurityConfig {
    @Bean
    public ProdutoSecurityInterceptor securityInterceptor(
            @Value("${soap.security.username}") String username,
            @Value("${soap.security.password}") String password) {
        Assert.hasText(username, "Defina SOAP_SECURITY_USERNAME para iniciar o servico.");
        Assert.hasText(password, "Defina SOAP_SECURITY_PASSWORD para iniciar o servico.");

        SimplePasswordValidationCallbackHandler callback = new SimplePasswordValidationCallbackHandler();
        callback.setUsersMap(Map.of(username, password));

        ProdutoSecurityInterceptor interceptor = new ProdutoSecurityInterceptor();
        interceptor.setValidationActions("Timestamp UsernameToken");
        interceptor.setValidationCallbackHandler(callback);
        interceptor.setValidationTimeToLive(300);
        interceptor.setFutureTimeToLive(60);
        interceptor.setTimestampStrict(true);
        // Compartilhado entre requisicoes para recusar a reutilizacao de nonces.
        interceptor.setValidationReplayCache(new ConcurrentMapReplayCache());
        return interceptor;
    }
}
