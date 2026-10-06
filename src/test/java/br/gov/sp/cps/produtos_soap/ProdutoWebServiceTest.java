package br.gov.sp.cps.produtos_soap;

import java.util.Map;

import br.gov.sp.cps.produtos_soap.config.WebServiceConfig;
import br.gov.sp.cps.produtos_soap.endpoint.ProdutoEndpoint;
import br.gov.sp.cps.produtos_soap.repository.ProdutoRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.xml.transform.StringSource;

import static org.springframework.ws.test.server.RequestCreators.withPayload;
import static org.springframework.ws.test.server.ResponseMatchers.clientOrSenderFault;
import static org.springframework.ws.test.server.ResponseMatchers.noFault;
import static org.springframework.ws.test.server.ResponseMatchers.validPayload;
import static org.springframework.ws.test.server.ResponseMatchers.xpath;

class ProdutoWebServiceTest {
    private static final Map<String, String> NAMESPACES = Map.of("p", WebServiceConfig.NAMESPACE);
    private static AnnotationConfigApplicationContext context;
    private static MockWebServiceClient client;

    @BeforeAll
    static void iniciar() {
        context = new AnnotationConfigApplicationContext(
                WebServiceConfig.class, ProdutoEndpoint.class, ProdutoRepository.class);
        client = MockWebServiceClient.createClient(context);
    }

    @AfterAll
    static void encerrar() {
        if (context != null) {
            context.close();
        }
    }

    private StringSource request(String conteudo) {
        return new StringSource("<p:consultarProdutoRequest xmlns:p=\""
                + WebServiceConfig.NAMESPACE + "\">" + conteudo + "</p:consultarProdutoRequest>");
    }

    @Test
    void retornaOsQuatroDadosDoProduto() throws Exception {
        client.sendRequest(withPayload(request("<p:codigo>1</p:codigo>")))
                .andExpect(noFault())
                .andExpect(validPayload(new ClassPathResource("produtos.xsd")))
                .andExpect(xpath("/p:consultarProdutoResponse/p:nome", NAMESPACES)
                        .evaluatesTo("Notebook"))
                .andExpect(xpath("/p:consultarProdutoResponse/p:descricao", NAMESPACES)
                        .evaluatesTo("Notebook com 8 GB de memória RAM e SSD de 256 GB."))
                .andExpect(xpath("/p:consultarProdutoResponse/p:marca", NAMESPACES)
                        .evaluatesTo("TechMax"))
                .andExpect(xpath("/p:consultarProdutoResponse/p:quantidadeEstoque", NAMESPACES)
                        .evaluatesTo("15"));
    }

    @Test
    void consultaOutroCodigo() {
        client.sendRequest(withPayload(request("<p:codigo>2</p:codigo>")))
                .andExpect(noFault())
                .andExpect(xpath("/p:consultarProdutoResponse/p:nome", NAMESPACES)
                        .evaluatesTo("Mouse"))
                .andExpect(xpath("/p:consultarProdutoResponse/p:quantidadeEstoque", NAMESPACES)
                        .evaluatesTo("40"));
    }

    @Test
    void produtoSemEstoqueContinuaExistindo() {
        client.sendRequest(withPayload(request("<p:codigo>3</p:codigo>")))
                .andExpect(noFault())
                .andExpect(xpath("/p:consultarProdutoResponse/p:nome", NAMESPACES)
                        .evaluatesTo("Teclado"))
                .andExpect(xpath("/p:consultarProdutoResponse/p:quantidadeEstoque", NAMESPACES)
                        .evaluatesTo("0"));
    }

    @Test
    void informaQuandoProdutoNaoExiste() {
        client.sendRequest(withPayload(request("<p:codigo>999</p:codigo>")))
                .andExpect(clientOrSenderFault("Produto não encontrado para o código 999."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "2147483648"})
    void rejeitaCodigoInvalido(String codigo) {
        client.sendRequest(withPayload(request("<p:codigo>" + codigo + "</p:codigo>")))
                .andExpect(clientOrSenderFault());
    }

    @Test
    void exigeCodigoNaRequisicao() {
        client.sendRequest(withPayload(request("")))
                .andExpect(clientOrSenderFault());
    }

    @Test
    void exigeNamespaceCorretoNoCodigo() {
        client.sendRequest(withPayload(request("<codigo>1</codigo>")))
                .andExpect(clientOrSenderFault());
    }
}
