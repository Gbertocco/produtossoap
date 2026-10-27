package br.gov.sp.cps.produtos_soap;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

final class SoapTestMessages {
    static final String USERNAME = "usuario-teste";
    static final String PASSWORD = "senha-apenas-dos-testes";
    private static final SecureRandom RANDOM = new SecureRandom();

    private SoapTestMessages() {
    }

    static String envelope(String content) {
        return envelope(content, USERNAME, PASSWORD, Instant.now(), Instant.now());
    }

    static String envelope(String content, String username, String password,
            Instant timestampCreated, Instant tokenCreated) {
        try {
            timestampCreated = timestampCreated.truncatedTo(ChronoUnit.MILLIS);
            tokenCreated = tokenCreated.truncatedTo(ChronoUnit.MILLIS);
            byte[] nonce = new byte[16];
            RANDOM.nextBytes(nonce);
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(nonce);
            sha1.update(tokenCreated.toString().getBytes(StandardCharsets.UTF_8));
            String digest = Base64.getEncoder().encodeToString(
                    sha1.digest(password.getBytes(StandardCharsets.UTF_8)));
            return """
                    <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
                      xmlns:p="http://cps.sp.gov.br/produtos"
                      xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
                      xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">
                      <s:Header><wsse:Security s:mustUnderstand="1">
                        <wsu:Timestamp wsu:Id="TS"><wsu:Created>%s</wsu:Created><wsu:Expires>%s</wsu:Expires></wsu:Timestamp>
                        <wsse:UsernameToken wsu:Id="UT">
                          <wsse:Username>%s</wsse:Username>
                          <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest">%s</wsse:Password>
                          <wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">%s</wsse:Nonce>
                          <wsu:Created>%s</wsu:Created>
                        </wsse:UsernameToken>
                      </wsse:Security></s:Header>
                      <s:Body><p:consultarProdutoRequest>%s</p:consultarProdutoRequest></s:Body>
                    </s:Envelope>
                    """.formatted(timestampCreated, timestampCreated.plusSeconds(300), username, digest,
                            Base64.getEncoder().encodeToString(nonce), tokenCreated, content);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel gerar a mensagem de teste", ex);
        }
    }
}
