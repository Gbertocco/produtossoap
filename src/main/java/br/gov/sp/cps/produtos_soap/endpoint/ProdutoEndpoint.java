package br.gov.sp.cps.produtos_soap.endpoint;

import br.gov.sp.cps.produtos_soap.config.WebServiceConfig;
import br.gov.sp.cps.produtos_soap.domain.Produto;
import br.gov.sp.cps.produtos_soap.exception.ProdutoNaoEncontradoException;
import br.gov.sp.cps.produtos_soap.model.ConsultarProdutoRequest;
import br.gov.sp.cps.produtos_soap.model.ConsultarProdutoResponse;
import br.gov.sp.cps.produtos_soap.repository.ProdutoRepository;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class ProdutoEndpoint {
    private final ProdutoRepository repository;

    public ProdutoEndpoint(ProdutoRepository repository) {
        this.repository = repository;
    }

    @PayloadRoot(namespace = WebServiceConfig.NAMESPACE, localPart = "consultarProdutoRequest")
    @ResponsePayload
    public ConsultarProdutoResponse consultarProduto(@RequestPayload ConsultarProdutoRequest request) {
        Produto produto = repository.buscarPorCodigo(request.getCodigo())
                .orElseThrow(() -> new ProdutoNaoEncontradoException(request.getCodigo()));

        ConsultarProdutoResponse response = new ConsultarProdutoResponse();
        response.setNome(produto.nome());
        response.setDescricao(produto.descricao());
        response.setMarca(produto.marca());
        response.setQuantidadeEstoque(produto.quantidadeEstoque());
        return response;
    }
}
