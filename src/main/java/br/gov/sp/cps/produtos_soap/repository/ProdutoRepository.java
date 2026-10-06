package br.gov.sp.cps.produtos_soap.repository;

import java.util.Map;
import java.util.Optional;

import br.gov.sp.cps.produtos_soap.domain.Produto;
import org.springframework.stereotype.Repository;

@Repository
public class ProdutoRepository {
    // Dados fictícios em memória, seguindo a consulta didática da aula.
    private final Map<Integer, Produto> produtos = Map.of(
            1, new Produto(1, "Notebook",
                    "Notebook com 8 GB de memória RAM e SSD de 256 GB.", "TechMax", 15),
            2, new Produto(2, "Mouse",
                    "Mouse sem fio com conexão USB.", "ClickPro", 40),
            3, new Produto(3, "Teclado",
                    "Teclado USB com layout ABNT2.", "KeyPlus", 0)
    );

    public Optional<Produto> buscarPorCodigo(int codigo) {
        return Optional.ofNullable(produtos.get(codigo));
    }
}
