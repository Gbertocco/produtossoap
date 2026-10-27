package br.gov.sp.cps.produtos_soap;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.SchemaFactory;

import br.gov.sp.cps.produtos_soap.config.SecurityConfig;
import br.gov.sp.cps.produtos_soap.config.WebServiceConfig;
import br.gov.sp.cps.produtos_soap.exception.ProdutoSoapFaultResolver;
import br.gov.sp.cps.produtos_soap.repository.ProdutoRepository;
import br.gov.sp.cps.produtos_soap.security.ProdutoSecurityInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "soap.security.username=" + SoapTestMessages.USERNAME,
        "soap.security.password=" + SoapTestMessages.PASSWORD
})
class ProdutoSecurityHttpTest {
    private static final String BODY = "<p:codigo>1</p:codigo>";
    private static final String SOAP = "http://schemas.xmlsoap.org/soap/envelope/";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @LocalServerPort
    private int port;

    @MockitoSpyBean
    private ProdutoRepository repository;

    @Test
    void aceitaCredenciaisValidasPorHttp() throws Exception {
        HttpResponse<String> response = send(SoapTestMessages.envelope(BODY));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(xml(response).getElementsByTagNameNS(WebServiceConfig.NAMESPACE, "nome")
                .item(0).getTextContent()).isEqualTo("Notebook");
        verify(repository).buscarPorCodigo(1);
    }

    @Test
    void rejeitaMensagemSemSegurancaAntesDeValidarPayload() throws Exception {
        assertSecurityFault(send("""
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" xmlns:p="http://cps.sp.gov.br/produtos">
                  <s:Header/><s:Body><p:consultarProdutoRequest><p:codigo>abc</p:codigo></p:consultarProdutoRequest></s:Body>
                </s:Envelope>
                """));
    }

    @ParameterizedTest
    @ValueSource(strings = {"usuario", "senha"})
    void rejeitaCredenciaisIncorretas(String campo) throws Exception {
        assertSecurityFault(send(SoapTestMessages.envelope(BODY,
                campo.equals("usuario") ? "desconhecido" : SoapTestMessages.USERNAME,
                campo.equals("senha") ? "incorreta" : SoapTestMessages.PASSWORD,
                Instant.now(), Instant.now())));
    }

    @Test
    void rejeitaPasswordText() throws Exception {
        String request = SoapTestMessages.envelope(BODY)
                .replace("#PasswordDigest", "#PasswordText")
                .replaceAll("(?s)(<wsse:Password[^>]*>).*?(</wsse:Password>)",
                        "$1" + SoapTestMessages.PASSWORD + "$2");
        assertSecurityFault(send(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Timestamp", "UsernameToken", "Nonce", "Password"})
    void exigeElementosDeSeguranca(String elemento) throws Exception {
        String prefix = elemento.equals("Timestamp") ? "wsu" : "wsse";
        String request = SoapTestMessages.envelope(BODY).replaceAll(
                "(?s)<" + prefix + ":" + elemento + "\\b[^>]*>.*?</" + prefix + ":" + elemento + ">", "");
        assertSecurityFault(send(request));
    }

    @Test
    void rejeitaTimestampExpirado() throws Exception {
        assertSecurityFault(send(SoapTestMessages.envelope(BODY, SoapTestMessages.USERNAME,
                SoapTestMessages.PASSWORD, Instant.now().minusSeconds(600), Instant.now())));
    }

    @Test
    void rejeitaTimestampFuturo() throws Exception {
        assertSecurityFault(send(SoapTestMessages.envelope(BODY, SoapTestMessages.USERNAME,
                SoapTestMessages.PASSWORD, Instant.now().plusSeconds(600), Instant.now())));
    }

    @Test
    void rejeitaTokenAntigoMesmoComTimestampAtual() throws Exception {
        assertSecurityFault(send(SoapTestMessages.envelope(BODY, SoapTestMessages.USERNAME,
                SoapTestMessages.PASSWORD, Instant.now(), Instant.now().minusSeconds(600))));
    }

    @Test
    void rejeitaTokenFuturoMesmoComTimestampAtual() throws Exception {
        assertSecurityFault(send(SoapTestMessages.envelope(BODY, SoapTestMessages.USERNAME,
                SoapTestMessages.PASSWORD, Instant.now(), Instant.now().plusSeconds(600))));
    }

    @Test
    void rejeitaReutilizacaoDeMensagem() throws Exception {
        String request = SoapTestMessages.envelope(BODY);
        assertThat(send(request).statusCode()).isEqualTo(200);
        HttpResponse<String> response = send(request);
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(xml(response).getElementsByTagName("faultstring").item(0).getTextContent())
                .isEqualTo(ProdutoSecurityInterceptor.SECURITY_FAULT);
        verify(repository).buscarPorCodigo(1);
    }

    @Test
    void produtoInexistenteDevolveFaultTipadoNoContrato() throws Exception {
        HttpResponse<String> response = send(SoapTestMessages.envelope("<p:codigo>999</p:codigo>"));
        Document document = assertFault(response, "Client", "Produto não encontrado para o código 999.");
        assertThat(document.getElementsByTagNameNS(WebServiceConfig.NAMESPACE, "codigo")
                .item(0).getTextContent()).isEqualTo("PRODUTO_NAO_ENCONTRADO");
        assertThat(document.getElementsByTagNameNS(WebServiceConfig.NAMESPACE, "codigoProduto")
                .item(0).getTextContent()).isEqualTo("999");
        validateFaultDetail(document);
    }

    @Test
    void erroInesperadoDevolveFaultSemDetalhesInternos() throws Exception {
        doThrow(new IllegalStateException("detalhe-interno-confidencial")).when(repository).buscarPorCodigo(1);
        HttpResponse<String> response = send(SoapTestMessages.envelope(BODY));
        Document document = assertFault(response, "Server", ProdutoSoapFaultResolver.INTERNAL_FAULT);
        assertThat(response.body()).doesNotContain("detalhe-interno-confidencial", "IllegalStateException");
        assertThat(document.getElementsByTagNameNS(WebServiceConfig.NAMESPACE, "codigo")
                .item(0).getTextContent()).isEqualTo("ERRO_INTERNO");
        validateFaultDetail(document);
    }

    @Test
    void wsdlContinuaAcessivelEDeclaraFault() throws Exception {
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/ws/produtos.wsdl")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("consultarProdutoFault", "soap:fault", "http://localhost:" + port + "/ws");
    }

    @Test
    void exigeUsuarioESenhaConfigurados() {
        SecurityConfig config = new SecurityConfig();
        assertThatIllegalArgumentException().isThrownBy(() -> config.securityInterceptor("", "senha"))
                .withMessageContaining("SOAP_SECURITY_USERNAME");
        assertThatIllegalArgumentException().isThrownBy(() -> config.securityInterceptor("usuario", " "))
                .withMessageContaining("SOAP_SECURITY_PASSWORD");
    }

    private HttpResponse<String> send(String request) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/ws"))
                .timeout(Duration.ofSeconds(15)).header("Content-Type", "text/xml; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(request)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertSecurityFault(HttpResponse<String> response) throws Exception {
        assertFault(response, "Client", ProdutoSecurityInterceptor.SECURITY_FAULT);
        verify(repository, never()).buscarPorCodigo(anyInt());
    }

    private Document assertFault(HttpResponse<String> response, String code, String message) throws Exception {
        assertThat(response.statusCode()).isEqualTo(500);
        Document document = xml(response);
        assertThat(document.getElementsByTagNameNS(SOAP, "Fault").getLength()).isEqualTo(1);
        assertThat(document.getElementsByTagName("faultcode").item(0).getTextContent()).endsWith(":" + code);
        assertThat(document.getElementsByTagName("faultstring").item(0).getTextContent()).isEqualTo(message);
        return document;
    }

    private Document xml(HttpResponse<String> response) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(response.body())));
    }

    private void validateFaultDetail(Document document) throws Exception {
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(new ClassPathResource("produtos.xsd").getURL()).newValidator()
                .validate(new DOMSource(document.getElementsByTagNameNS(
                        WebServiceConfig.NAMESPACE, "consultarProdutoFault").item(0)));
    }
}
