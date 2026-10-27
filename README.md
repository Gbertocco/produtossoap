# Web Service SOAP de Produtos com WS-Security

Consulta de produtos por código com Java 17, Spring Boot 3.5.16, Spring Web Services
4.1.4, Apache WSS4J, JAXB e SOAP 1.1. A resposta contém **nome, descrição, marca e
quantidade em estoque**. O catálogo é fictício, em memória, sem banco de dados.

## Executar

Instale Java 17 ou superior. Defina as mesmas credenciais no servidor e no cliente.
O serviço **não inicia com usuário ou senha vazios**; não há senha padrão no código.
Os valores abaixo são exemplos locais: escolha sua própria senha.

Windows / PowerShell, na pasta do projeto:

```powershell
$env:SOAP_SECURITY_USERNAME = 'aluno'
$env:SOAP_SECURITY_PASSWORD = 'substitua-por-uma-senha-forte'
.\iniciar.bat
```

Linux / macOS:

```bash
export SOAP_SECURITY_USERNAME='aluno'
export SOAP_SECURITY_PASSWORD='substitua-por-uma-senha-forte'
java -jar executavel/produtos-soap.jar
```

O JAR inclui as dependências. Encerre com `Ctrl+C`.

- WSDL: `http://localhost:8080/ws/produtos.wsdl` (consulta pública do contrato).
- Endpoint SOAP: `http://localhost:8080/ws`.
- Namespace: `http://cps.sp.gov.br/produtos`.
- Operação: `consultarProduto`.

Se precisar de outra porta, execute `java -jar executavel/produtos-soap.jar --server.port=8081`
e ajuste a URL do cliente.

## Como funciona a segurança

O `SecurityConfig` registra o `ProdutoSecurityInterceptor` **antes** da validação XSD
e do endpoint. Cada requisição precisa de um cabeçalho `wsse:Security` contendo:

1. `wsu:Timestamp`, com `Created` e `Expires` em UTC, válido por até cinco minutos.
2. `wsse:UsernameToken`, com usuário, `PasswordDigest`, `Nonce` aleatório e `Created`.

O digest segue o perfil UsernameToken: `Base64(SHA1(nonce + created + senha))`.
`nonce` nessa fórmula são os bytes originais, antes da conversão para Base64.
O SHA-1 é utilizado por esse perfil de interoperabilidade; não é armazenamento de senha.
`PasswordText` é recusado. São aceitos no máximo 60 segundos de diferença futura de relógio.
Tokens sem campos obrigatórios, antigos, futuros, com credenciais incorretas ou com
nonce já utilizado são recusados antes de consultar o catálogo.

A proteção contra repetição utiliza `ConcurrentMapReplayCache`, compartilhado entre
requisições da instância. Em implantação com várias instâncias, utilize um cache
distribuído. O cache local é perdido ao reiniciar o processo.

Esta implementação autentica as mensagens; não assina nem criptografa o corpo SOAP.
Em uso remoto, publique o serviço com **HTTPS**, por exemplo atrás de um proxy com TLS,
para proteger o conteúdo e o transporte das mensagens. Não publique senhas no Git.

## Consultar pelo PowerShell

Em outro terminal, configure as mesmas variáveis de ambiente e execute:

```powershell
$env:SOAP_SECURITY_USERNAME = 'aluno'
$env:SOAP_SECURITY_PASSWORD = 'substitua-por-uma-senha-forte'
.\exemplos\consultar-produto.ps1 -Codigo 1
.\exemplos\consultar-produto.ps1 -Codigo 999
```

A primeira consulta retorna Notebook; a segunda exibe um SOAP Fault de produto inexistente.
O script gera novo nonce, digest e timestamp em cada chamada. Para outra porta:

```powershell
.\exemplos\consultar-produto.ps1 -Codigo 2 -Url 'http://localhost:8081/ws'
```

Para gerar um XML válido para uma única chamada e enviar com `curl`:

```powershell
.\exemplos\consultar-produto.ps1 -Codigo 1 -GerarXml | Set-Content -Encoding utf8 "$env:TEMP\consulta-produto.xml"
curl.exe -H "Content-Type: text/xml; charset=UTF-8" --data-binary "@$env:TEMP\consulta-produto.xml" http://localhost:8080/ws
```

Gere outra mensagem antes de cada envio. Reenviar o mesmo XML deve falhar.
Os arquivos `exemplos/consultar-produto.xml` e `exemplos/produto-nao-encontrado.xml`
são **modelos com placeholders**: não podem ser enviados sem preencher o cabeçalho.
O script preenche o primeiro modelo automaticamente.

## Testar pelo SoapUI

1. Inicie o servidor com as variáveis de ambiente configuradas.
2. Crie um **New SOAP Project** com `http://localhost:8080/ws/produtos.wsdl`.
3. Nas configurações do projeto, abra **WS-Security Configurations > Outgoing WS-Security Configurations**.
4. Crie a configuração `seguranca` e adicione **Timestamp** (TTL de 300 segundos).
5. Adicione **Username**, preencha as credenciais e selecione **PasswordDigest**, **Add Nonce** e **Add Created**.
6. Na requisição, selecione `seguranca` em **Outgoing WSS** e informe o código do produto.
7. Envie. O SoapUI deverá gerar os valores de segurança em cada chamada.

O WSDL declara a operação e o fault de aplicação. A exigência de WS-Security está
configurada no servidor e documentada aqui; o WSDL não contém uma política WS-SecurityPolicy.

## Produtos disponíveis

| Código | Nome | Marca | Estoque |
| --- | --- | --- | ---: |
| 1 | Notebook | TechMax | 15 |
| 2 | Mouse | ClickPro | 40 |
| 3 | Teclado | KeyPlus | 0 |

Produto sem estoque continua sendo encontrado. O campo `codigo` é obrigatório e deve
ser um inteiro entre 1 e 2147483647.

## Exception e SOAP Fault

| Situação | SOAP Fault | Comportamento |
| --- | --- | --- |
| WS-Security ausente/inválido | `Client` | `Falha na autenticação WS-Security.`; não revela se o usuário existe |
| Produto inexistente | `Client` | `ProdutoNaoEncontradoException` convertida em fault com o código consultado |
| Código inválido ou ausente | `Client` | Validação XSD recusa o conteúdo após autenticar |
| Erro inesperado | `Server` | Mensagem genérica ao cliente; detalhes registrados apenas no servidor |

SOAP 1.1 retorna HTTP **500** nas falhas; confira `faultcode`, `faultstring` e `detail`.
Erros da aplicação são tratados por `ProdutoSoapFaultResolver`. O elemento
`consultarProdutoFault` é definido no XSD, gerado pelo JAXB e publicado no WSDL:

```xml
<SOAP-ENV:Fault xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
  <faultcode>SOAP-ENV:Client</faultcode>
  <faultstring xml:lang="pt-BR">Produto não encontrado para o código 999.</faultstring>
  <detail>
    <p:consultarProdutoFault xmlns:p="http://cps.sp.gov.br/produtos">
      <p:codigo>PRODUTO_NAO_ENCONTRADO</p:codigo>
      <p:mensagem>Produto não encontrado para o código 999.</p:mensagem>
      <p:codigoProduto>999</p:codigoProduto>
    </p:consultarProdutoFault>
  </detail>
</SOAP-ENV:Fault>
```

Erros internos usam `ERRO_INTERNO` no detalhe, sem stack trace nem mensagens confidenciais.
Falhas de segurança e de validação são produzidas pelos interceptadores e não usam
esse detalhe de aplicação. Os prefixos XML podem variar sem alterar o significado.

## Compilar e validar

Requer JDK 17 ou superior e internet no primeiro uso. O Maven Wrapper baixa Maven 3.9.11.

```powershell
# Windows
.\mvnw.cmd clean verify
```

```bash
# Linux / macOS
sh mvnw clean verify
```

As credenciais dos testes são fictícias e configuradas somente no contexto de teste;
não é necessário definir as variáveis de produção para executar a suíte.
O build gera as classes JAXB em `target/generated-sources/jaxb` e o executável
`target/produtos-soap.jar`. Não edite as classes geradas. Após alterar o código, atualize
o JAR pronto antes de usar `iniciar.bat`:

```powershell
Copy-Item target/produtos-soap.jar executavel/produtos-soap.jar
```

Os testes verificam o catálogo, entradas inválidas, autenticação, expiração, proteção
contra replay, HTTP 200/500, WSDL, detalhes de fault conforme o XSD e ocultação de erros
internos. O GitHub Actions executa `clean verify` nos pushes em `seguranca` e `main`
e nos pull requests destinados à `main`. Resultados da validação local em `VALIDACAO.txt`.

## Fluxo Git da atividade

```bash
git checkout -b seguranca
# Implementar, testar e criar commits por responsabilidade
git add pom.xml
git commit -m "Atualiza pom.xml com dependencia WS-Security"
# Mais commits de seguranca, exceptions, testes e documentacao
git push -u origin seguranca
# Validar os testes e revisar a branch antes de integrar
git switch main
git merge --no-ff seguranca -m "Integra seguranca WS-Security e SOAP Faults"
git push origin main
```

A branch `seguranca` é mantida no remoto para consulta e avaliação do histórico.

## Organização principal

- `config/SecurityConfig.java`: usuário/senha externos, ações WSS4J e cache de replay.
- `security/ProdutoSecurityInterceptor.java`: exige digest e padroniza falha de autenticação.
- `config/WebServiceConfig.java`: WSDL, ordem dos interceptadores e resolvedor de exceções.
- `exception/ProdutoNaoEncontradoException.java`: exceção de domínio com o código do produto.
- `exception/ProdutoSoapFaultResolver.java`: faults `Client`/`Server` e detalhes de aplicação.
- `src/main/resources/produtos.xsd`: contrato de consulta, resposta e fault.
- `src/test/java/`: testes de contrato e segurança, incluindo servidor HTTP real.
- `exemplos/`: cliente PowerShell, modelos XML, respostas e WSDL.

Os caminhos Java acima são relativos a `src/main/java/br/gov/sp/cps/produtos_soap/`.

## Referências

- [Guia oficial: produzir Web Service com Spring](https://spring.io/guides/gs/producing-web-service/)
- [Spring Web Services](https://docs.spring.io/spring-ws/docs/4.1.4/reference/html/)
- [Apache WSS4J: guia oficial](https://ws.apache.org/wss4j/user_guide.html)
- [Correção do cache de replay disponível no Spring-WS 4.1.4](https://spring.io/security/cve-2026-41000/)
