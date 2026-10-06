# Web Service SOAP de Produtos

Implementação da atividade baseada no exemplo de consulta de alunos da aula 5.
O serviço recebe um código de produto e devolve **nome, descrição, marca e
quantidade em estoque**. Utiliza Java 17, Spring Boot 3.5.16, Spring Web Services,
Maven, XML, XSD e SOAP 1.1.

## Executar o arquivo pronto

1. Extraia todo o ZIP.
2. Tenha Java 17 ou superior instalado, disponível no `PATH` ou configurado em `JAVA_HOME`.
3. No Windows, abra `iniciar.bat`. Em outros sistemas, ou para executar pelo terminal,
   na pasta `produtos-soap` use:

```bash
java -jar executavel/produtos-soap.jar
```

O JAR inclui as dependências e não exige Maven para executar.
Mantenha o terminal aberto enquanto estiver usando o serviço; `Ctrl+C` encerra a aplicação.

- **WSDL:** http://localhost:8080/ws/produtos.wsdl
- **Endereço para enviar requisições SOAP:** http://localhost:8080/ws
- **Operação:** `consultarProduto`
- **Namespace:** `http://cps.sp.gov.br/produtos`

O namespace é o identificador do contrato. Para chamar o serviço, utilize o endereço local `/ws`.

## Compilar ou alterar o código

Requisitos: JDK 17 ou superior. O Maven Wrapper incluído baixa o Maven 3.9.11 na
primeira utilização. É necessário acesso à internet para baixar Maven e dependências.

No Windows, dentro da pasta do projeto:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS:

```bash
sh mvnw clean verify
sh mvnw spring-boot:run
```

Se já tiver Maven instalado, pode usar `mvn clean verify` e `mvn spring-boot:run`.
O comando `clean verify` gera as classes JAXB a partir do XSD, compila o código,
executa os testes e cria `target/produtos-soap.jar`.
Depois de alterar o projeto, execute esse novo JAR ou copie-o para `executavel/`
para atualizar a versão usada pelo `iniciar.bat`.

Também é possível importar `pom.xml` em uma IDE como projeto Maven.
As classes de mensagem `ConsultarProdutoRequest` e `ConsultarProdutoResponse`
são geradas automaticamente em `target/generated-sources/jaxb`; não devem ser editadas à mão.

## Produtos para testar

O catálogo utiliza dados fictícios em memória, como no exemplo didático da aula.
Não requer banco de dados. Para alterar os produtos, edite `ProdutoRepository.java`
e compile novamente.

| Código | Nome | Descrição | Marca | Quantidade em estoque |
| --- | --- | --- | --- | ---: |
| 1 | Notebook | Notebook com 8 GB de memória RAM e SSD de 256 GB. | TechMax | 15 |
| 2 | Mouse | Mouse sem fio com conexão USB. | ClickPro | 40 |
| 3 | Teclado | Teclado USB com layout ABNT2. | KeyPlus | 0 |

## Testar pelo SoapUI

1. Inicie o serviço.
2. No SoapUI, escolha **New SOAP Project**.
3. Informe `http://localhost:8080/ws/produtos.wsdl` no campo **Initial WSDL**.
4. Abra a requisição da operação `consultarProduto`.
5. Informe `1`, `2` ou `3` no elemento `codigo` e envie a requisição.

Requisição para o produto 1, também disponível em `exemplos/consultar-produto.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:prod="http://cps.sp.gov.br/produtos">
    <soapenv:Header/>
    <soapenv:Body>
        <prod:consultarProdutoRequest>
            <prod:codigo>1</prod:codigo>
        </prod:consultarProdutoRequest>
    </soapenv:Body>
</soapenv:Envelope>
```

Resposta esperada (os prefixos XML podem variar sem alterar o significado):

```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
    <SOAP-ENV:Header/>
    <SOAP-ENV:Body>
        <prod:consultarProdutoResponse xmlns:prod="http://cps.sp.gov.br/produtos">
            <prod:nome>Notebook</prod:nome>
            <prod:descricao>Notebook com 8 GB de memória RAM e SSD de 256 GB.</prod:descricao>
            <prod:marca>TechMax</prod:marca>
            <prod:quantidadeEstoque>15</prod:quantidadeEstoque>
        </prod:consultarProdutoResponse>
    </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

## Testar pelo terminal

Com a aplicação em execução, na pasta do projeto:

Windows:

```powershell
curl.exe -H "Content-Type: text/xml; charset=UTF-8" --data-binary "@exemplos/consultar-produto.xml" http://localhost:8080/ws
```

Linux ou macOS:

```bash
curl -H "Content-Type: text/xml; charset=UTF-8" --data-binary "@exemplos/consultar-produto.xml" http://localhost:8080/ws
```

## Códigos inválidos e produtos inexistentes

- `codigo` é obrigatório e deve ser um inteiro entre 1 e 2147483647.
- Código inexistente, como `999`, devolve um SOAP Fault `Client` com a mensagem
  `Produto não encontrado para o código 999.`.
- Código zero, negativo, texto, valor acima do limite ou campo ausente é rejeitado
  pela validação XSD e devolve um SOAP Fault `Client`.
- Produto com estoque zero é encontrado normalmente, com `quantidadeEstoque` igual a `0`.
- O status HTTP de uma falha SOAP 1.1 é `500`; o motivo está no corpo XML.

## Organização

```text
produtos-soap/
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/      Maven Wrapper
├── iniciar.bat
├── executavel/produtos-soap.jar
├── exemplos/                   Mensagens e contrato SOAP
└── src/
    ├── main/
    │   ├── java/br/gov/sp/cps/produtos_soap/
    │   │   ├── ProdutosSoapApplication.java
    │   │   ├── config/WebServiceConfig.java
    │   │   ├── domain/Produto.java
    │   │   ├── repository/ProdutoRepository.java
    │   │   ├── endpoint/ProdutoEndpoint.java
    │   │   └── exception/ProdutoNaoEncontradoException.java
    │   └── resources/
    │       ├── produtos.xsd
    │       └── application.properties
    └── test/java/br/gov/sp/cps/produtos_soap/ProdutoWebServiceTest.java
```

O XSD define os dados; o Maven gera as classes JAXB; o Spring publica o WSDL;
o endpoint consulta o repositório e monta a resposta SOAP.
O mesmo namespace é usado no XSD, WSDL e endpoint.

## Validação

Os testes automatizados cobrem os quatro campos da resposta, consulta a outros
produtos, estoque zero, produto inexistente e entradas inválidas.
Para executá-los: `mvnw.cmd test` no Windows ou `sh mvnw test` no Linux/macOS.

O arquivo entregue passou em 10 testes automatizados e 26 verificações HTTP do
JAR em execução com Java 17. Os resultados estão em `VALIDACAO.txt`.
`exemplos/resposta-produto.xml` contém uma resposta recebida do serviço;
`exemplos/produtos.wsdl` contém o contrato publicado, com o endereço padrão da porta 8080.

Se a porta 8080 estiver ocupada, execute:

```bash
java -jar executavel/produtos-soap.jar --server.port=8081
```

Nesse caso, use `8081` nas URLs do serviço e do WSDL.

## Referências

- Material fornecido: `Web_Service_SOAP_Spring_Boot.md`, exemplo da aula de SOAP com Spring Boot.
- Documentação oficial: https://spring.io/guides/gs/producing-web-service/
- Spring Web Services: https://docs.spring.io/spring-ws/docs/current/reference/html/
