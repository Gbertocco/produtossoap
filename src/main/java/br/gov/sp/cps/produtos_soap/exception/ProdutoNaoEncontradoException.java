package br.gov.sp.cps.produtos_soap.exception;

public class ProdutoNaoEncontradoException extends RuntimeException {
    private final int codigo;

    public ProdutoNaoEncontradoException(int codigo) {
        super("Produto não encontrado para o código " + codigo + ".");
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }
}
