package br.gov.sp.cps.produtos_soap.exception;

import java.util.Locale;

import br.gov.sp.cps.produtos_soap.model.ConsultarProdutoFault;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.soap.SoapFault;
import org.springframework.ws.soap.server.endpoint.AbstractSoapFaultDefinitionExceptionResolver;
import org.springframework.ws.soap.server.endpoint.SoapFaultDefinition;

public class ProdutoSoapFaultResolver extends AbstractSoapFaultDefinitionExceptionResolver {
    public static final String INTERNAL_FAULT = "Não foi possível consultar o produto. Tente novamente mais tarde.";
    private final Jaxb2Marshaller marshaller;

    public ProdutoSoapFaultResolver() {
        marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(ConsultarProdutoFault.class);
    }

    @Override
    protected SoapFaultDefinition getFaultDefinition(Object endpoint, Exception ex) {
        SoapFaultDefinition definition = new SoapFaultDefinition();
        boolean produtoAusente = ex instanceof ProdutoNaoEncontradoException;
        definition.setFaultCode(produtoAusente ? SoapFaultDefinition.CLIENT : SoapFaultDefinition.SERVER);
        definition.setFaultStringOrReason(produtoAusente ? ex.getMessage() : INTERNAL_FAULT);
        definition.setLocale(Locale.forLanguageTag("pt-BR"));
        return definition;
    }

    @Override
    protected void customizeFault(Object endpoint, Exception ex, SoapFault fault) {
        ConsultarProdutoFault detail = new ConsultarProdutoFault();
        boolean produtoAusente = ex instanceof ProdutoNaoEncontradoException;
        detail.setCodigo(produtoAusente ? "PRODUTO_NAO_ENCONTRADO" : "ERRO_INTERNO");
        detail.setMensagem(produtoAusente ? ex.getMessage() : INTERNAL_FAULT);
        if (ex instanceof ProdutoNaoEncontradoException produtoException) {
            detail.setCodigoProduto(produtoException.getCodigo());
        }
        else {
            logger.error("Erro inesperado ao consultar produto", ex);
        }
        marshaller.marshal(detail, fault.addFaultDetail().getResult());
    }
}
