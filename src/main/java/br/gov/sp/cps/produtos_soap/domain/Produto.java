package br.gov.sp.cps.produtos_soap.domain;

/** Dados de um produto do catálogo de demonstração. */
public record Produto(int codigo, String nome, String descricao,
                      String marca, int quantidadeEstoque) {
}
