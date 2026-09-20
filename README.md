# Ecossistema de Microsserviços — Aluno / Matrícula / Notificação

Projeto didático de microsserviços com Spring Boot 3.3.4, Spring Cloud (Eureka +
Gateway + OpenFeign) e Apache Kafka. Cada pasta dentro de `backend/` é um
projeto Maven independente — não é um projeto multi-módulo com um `pom.xml`
pai, então cada um é importado e rodado separadamente.

## Arquitetura, em uma frase por serviço

| Serviço          | Porta | Papel                                                                 |
|-------------------|-------|------------------------------------------------------------------------|
| `eureka-server`   | 8761  | Catálogo de serviços — quem registra quem e onde encontrar cada um    |
| `api-gateway`     | 8080  | Porta única de entrada — roteia `/alunos/**`, `/matriculas/**` e `/disciplinas/**` |
| `ms-aluno`        | 8081  | Dono dos dados de aluno — só CRUD simples (id, nome, email)            |
| `ms-matricula`    | 8082  | Dono de **Matricula** e **Disciplina**. Chama `ms-aluno` via Feign e publica no Kafka |
| `ms-notificacao`  | 8083  | Escuta o Kafka e reage a matrículas confirmadas (simula envio de e-mail)|
| Kafka + Kafka UI  | 9092 / 8090 | Mensageria assíncrona entre `ms-matricula` e `ms-notificacao` (via Docker) |

**Por que `Disciplina` mora dentro do `ms-matricula`, e não do `ms-aluno`?**
Porque é o `ms-matricula` quem decide "cabe ou não cabe" na hora de matricular —
faz sentido ele já ter a informação de vagas no mesmo banco, sem precisar sair
pela rede pra perguntar pra outro serviço. Isso também permite um
relacionamento JPA de verdade (`@ManyToOne`) entre `Matricula` e `Disciplina`,
coisa que não seria possível se elas estivessem em bancos de dados diferentes
(veja a seção "Relacionamentos" mais abaixo).

**Fluxo de uma matrícula:**
```
Cliente → api-gateway → ms-matricula → (Feign, via rede) → ms-aluno
                              │             [só confirma que o aluno existe]
                              ↓
                    Disciplina (mesmo banco, via JPA)
                    [busca vaga e já decrementa, na mesma transação]
                              ↓
                         Kafka (tópico "matricula-realizada")
                              ↓
                        ms-notificacao   [assíncrono, reage depois que a matrícula já foi confirmada]
```

---

## Pré-requisitos

- **JDK 17** instalado e configurado (confira com `java -version`)
- **Docker Desktop** rodando
- **Spring Tool Suite (STS)** ou Eclipse com plugin m2e

## Como importar no STS

1. Abra o STS.
2. `File` → `Import...` → `Maven` → `Existing Maven Projects`.
3. Em "Root Directory", selecione a pasta `backend` (a que contém as 5
   subpastas de serviços). O STS varre recursivamente e encontra os 5
   `pom.xml` automaticamente — você pode selecionar todos de uma vez e
   importar juntos.
4. Aguarde o Maven baixar as dependências de cada projeto (primeira vez pode
   demorar um pouco).

---

## Mapa de pastas — onde encontrar cada arquivo

Sempre que este README (ou qualquer explicação anterior nossa) mencionar uma
classe, é aqui que ela está. Todos os caminhos abaixo começam da **raiz do
projeto** (a pasta que contém `backend/` e `infra/`).

### `infra/`
```
infra/docker-compose.yaml                         → Kafka + Kafka UI
```

### `backend/eureka-server/`
```
pom.xml
src/main/java/com/mztec/eureka_server/EurekaServerApplication.java
src/main/resources/application.yaml
```

### `backend/api-gateway/`
```
pom.xml
src/main/java/com/mztec/api_gateway/ApiGatewayApplication.java
src/main/resources/application.yaml               → rotas /alunos, /matriculas, /disciplinas
```

### `backend/ms-aluno/`
```
pom.xml
src/main/java/com/mztec/ms_aluno/
  ├── MsAlunoApplication.java
  ├── model/Aluno.java                             → entidade (id, nome, email)
  ├── dto/AlunoRequestDTO.java                      → o que o Postman manda
  ├── dto/AlunoResponseDTO.java                     → o que a API devolve
  ├── repository/AlunoRepository.java
  ├── service/AlunoService.java                     → regra de negócio
  └── controller/AlunoController.java                → endpoints REST
src/main/resources/application.yaml
src/test/java/com/mztec/ms_aluno/service/
  └── AlunoServiceTest.java                         → TESTE UNITÁRIO
```

### `backend/ms-matricula/`
```
pom.xml
src/main/java/com/mztec/ms_matricula/
  ├── MsMatriculaApplication.java
  ├── model/Matricula.java                          → entidade (alunoId + @ManyToOne Disciplina)
  ├── model/Disciplina.java                         → entidade (id, nome, vagasDisponiveis)
  ├── dto/MatriculaRequestDTO.java                  → { alunoId, disciplinaId }
  ├── dto/MatriculaResponseDTO.java
  ├── dto/DisciplinaRequestDTO.java                 → { nome, vagasDisponiveis }
  ├── dto/DisciplinaResponseDTO.java
  ├── dto/AlunoResponseDTO.java                     → cópia local do contrato do ms-aluno
  ├── client/AlunoClient.java                       → interface Feign
  ├── repository/MatriculaRepository.java
  ├── repository/DisciplinaRepository.java
  ├── service/MatriculaService.java                 → orquestra tudo (Feign + JPA + Kafka)
  ├── service/DisciplinaService.java                → regra de vaga
  ├── controller/MatriculaController.java           → POST/GET /matriculas
  ├── controller/DisciplinaController.java          → POST/GET /disciplinas
  ├── event/MatriculaRealizadaEvent.java             → contrato da mensagem Kafka
  └── producer/MatriculaEventProducer.java           → publica no Kafka
src/main/resources/application.yaml
src/test/java/com/mztec/ms_matricula/service/
  ├── DisciplinaServiceTest.java                    → TESTE UNITÁRIO
  └── MatriculaServiceTest.java                     → TESTE UNITÁRIO
```

### `backend/ms-notificacao/`
```
pom.xml
src/main/java/com/mztec/ms_notificacao/
  ├── MsNotificacaoApplication.java
  ├── event/MatriculaRealizadaEvent.java             → cópia local do contrato do ms-matricula
  └── consumer/MatriculaEventConsumer.java           → @KafkaListener, reage ao evento
src/main/resources/application.yaml
```

---

## Ordem de execução — SEMPRE nessa sequência

A ordem importa porque cada peça depende da anterior já estar de pé.

### 1) Suba o Kafka (Docker)

```bash
cd infra
docker compose up -d
```

Aguarde uns 10-15 segundos para o broker inicializar completamente. Confirme
acessando **http://localhost:8090** (Kafka UI) — se abrir sem erro, o Kafka
está pronto.

> **Por que o `docker-compose.yaml` tem dois listeners (`PLAINTEXT` e
> `INTERNAL`)?** Porque o Kafka precisa de um endereço diferente pra cada tipo
> de cliente: suas aplicações Spring Boot rodam *fora* do Docker (usam
> `localhost:9092`), mas o Kafka UI roda *dentro* da mesma rede Docker (usa
> `kafka:29092`). Sem essa separação, um dos dois lados nunca consegue se
> conectar — foi um dos erros mais difíceis de diagnosticar no
> desenvolvimento original deste projeto.

### 2) Suba o `eureka-server`

Rode a classe `EurekaServerApplication` (botão direito → `Run As` → `Spring
Boot App`), ou pelo terminal:
```bash
cd backend/eureka-server
./mvnw spring-boot:run
```

Confirme em **http://localhost:8761** — deve aparecer o dashboard do Eureka,
vazio (nenhuma instância registrada ainda).

### 3) Suba o `ms-aluno`

```bash
cd backend/ms-aluno
./mvnw spring-boot:run
```

Volte ao dashboard do Eureka (http://localhost:8761) e aguarde uns segundos —
`MS-ALUNO` deve aparecer na lista de instâncias registradas.

### 4) Suba o `ms-matricula`

```bash
cd backend/ms-matricula
./mvnw spring-boot:run
```

Confirme que `MS-MATRICULA` também aparece no Eureka.

### 5) Suba o `api-gateway`

```bash
cd backend/api-gateway
./mvnw spring-boot:run
```

Confirme `API-GATEWAY` no Eureka.

### 6) Suba o `ms-notificacao`

```bash
cd backend/ms-notificacao
./mvnw spring-boot:run
```

Esse serviço não tem endpoint REST próprio — ele só fica escutando o Kafka em
segundo plano. Confirme `MS-NOTIFICACAO` no Eureka mesmo assim (registramos
por consistência/monitoramento).

> Se preferir usar o Maven Wrapper (`./mvnw`), garanta que o `JAVA_HOME`
> aponte para o JDK 17 no seu terminal. Se estiver no Git Bash no Windows e
> ele "esquecer" a variável toda vez que abre um terminal novo, adicione o
> `export JAVA_HOME=...` no seu `~/.bashrc` para tornar permanente.

---

## Testando o fluxo completo

Com os 6 serviços de pé (Kafka + 5 aplicações Spring Boot), use o Postman (ou
similar):

**1. Crie um aluno**
```
POST http://localhost:8081/alunos
Content-Type: application/json

{
  "nome": "João Silva",
  "email": "joao@email.com"
}
```
Resposta esperada: `201 Created`, com o `id` gerado (provavelmente `1`).

**2. Crie uma disciplina**
```
POST http://localhost:8082/disciplinas
Content-Type: application/json

{
  "nome": "Cálculo I",
  "vagasDisponiveis": 5
}
```
Resposta esperada: `201 Created`, com o `id` gerado (provavelmente `1`).

> Repare que `Disciplina` roda na porta do `ms-matricula` (`8082`), não do
> `ms-aluno` — porque é lá que ela mora agora.

**3. Matricule o aluno na disciplina**
```
POST http://localhost:8082/matriculas
Content-Type: application/json

{
  "alunoId": 1,
  "disciplinaId": 1
}
```
Resposta esperada: `201 Created`, com o `MatriculaResponseDTO` (incluindo
`nomeDisciplina`). Por trás dos panos: o `ms-matricula` chamou o `ms-aluno` via
Feign (rede) só pra confirmar o aluno, e decrementou a vaga da disciplina
localmente (Java puro, mesma transação do salvamento da matrícula) — depois
publicou o evento no Kafka.

**4. Confirme a vaga decrementada**
```
GET http://localhost:8082/disciplinas/1
```
`vagasDisponiveis` deve ter caído de `5` para `4`.

**5. Confirme o evento no Kafka**

Abra **http://localhost:8090/ui/clusters/local/all-topics/matricula-realizada/messages**
e clique em "Submit" — deve aparecer 1 mensagem com o payload do
`MatriculaRealizadaEvent`.

**6. Confirme que o `ms-notificacao` reagiu**

Olhe o console/log do terminal onde o `ms-notificacao` está rodando — deve
aparecer algo como:
```
=== Novo evento de matrícula recebido ===
Matrícula ID: 1
Aluno: João Silva (id=1)
Disciplina: Cálculo I (id=1)
E-mail: joao@email.com
Data: ...
>>> [SIMULAÇÃO] E-mail enviado para João Silva (joao@email.com): você foi matriculado em 'Cálculo I'
```

Se você viu esse log, o fluxo ponta a ponta — HTTP síncrono via Feign +
mensageria assíncrona via Kafka — está funcionando de verdade.

---

## Rodando os testes automatizados

Este projeto tem **testes unitários** para as três classes de serviço que
concentram a lógica de negócio: `AlunoService`, `DisciplinaService` e
`MatriculaService`.

**Pelo STS:** clique com o botão direito na classe de teste (ex:
`MatriculaServiceTest.java`) → `Run As` → `JUnit Test`. Ou clique com botão
direito no projeto inteiro → `Run As` → `JUnit Test`, pra rodar todos de uma vez.

**Pelo terminal:**
```bash
cd backend/ms-aluno
./mvnw test
```
(e o mesmo dentro de `backend/ms-matricula`)

**O que esses testes verificam, resumidamente:**
- `AlunoServiceTest` — criar, buscar (encontrado/não encontrado), listar.
- `DisciplinaServiceTest` — criar, decrementar vaga com sucesso, decrementar
  vaga **sem sucesso** (disciplina lotada — a regra mais importante do projeto).
- `MatriculaServiceTest` — o fluxo feliz completo (incluindo a ORDEM correta
  das chamadas e o conteúdo exato do evento publicado no Kafka), e dois
  cenários de falha (aluno inexistente, disciplina sem vaga) confirmando que
  nada é salvo/publicado quando algo dá errado no meio do caminho.

Esses testes **não** dependem de Kafka, Eureka, Docker ou banco de dados
rodando — eles usam *mocks* (dublês) no lugar de tudo isso, por isso rodam em
milissegundos e podem ser executados a qualquer momento, mesmo sem a
infraestrutura de pé.

## Decisões de design (vale a pena entender o "porquê")

### Os dois tipos de relacionamento entre classes (o ponto mais importante deste projeto)

A classe `Matricula` (em `ms-matricula`) se relaciona com DUAS outras
entidades, de formas completamente diferentes — e entender essa diferença é
a base de qualquer sistema com bancos de dados distribuídos:

**1) Relacionamento com `Aluno` — guardando só o ID**
```java
private Long alunoId;
private String nomeAluno;   // cópia do nome, guardada de propósito
```
`Aluno` mora em **outro microsserviço** (`ms-aluno`), com seu **próprio banco
de dados**. Bancos diferentes não conseguem ter uma chave estrangeira de
verdade entre si — não existe "JOIN" entre dois bancos separados. Por isso:
- Guardamos apenas o `Long alunoId` como referência.
- Copiamos o `nome` na hora da matrícula (chamado de **denormalização**),
  porque não temos como "buscar de novo com um JOIN" depois — teríamos que
  fazer outra chamada de rede (Feign) toda vez que quiséssemos exibir o nome.

**2) Relacionamento com `Disciplina` — guardando o objeto inteiro**
```java
@ManyToOne
@JoinColumn(name = "disciplina_id", nullable = false)
private Disciplina disciplina;
```
`Disciplina` mora no **mesmo banco de dados** que `Matricula` (as duas estão
dentro do `ms-matricula`). Isso permite um relacionamento JPA de verdade:
- `@ManyToOne` significa "muitas Matriculas apontam para UMA Disciplina" —
  vários alunos podem se matricular na mesma disciplina, todos competindo
  pelo mesmo pool de vagas.
- `@JoinColumn` diz qual coluna na tabela `matricula` guarda o ID da
  disciplina (a chave estrangeira real, gerenciada pelo banco).
- Como é um relacionamento de verdade, `matricula.getDisciplina().getNome()`
  sempre traz o nome ATUAL da disciplina (o Hibernate faz o JOIN por trás dos
  panos) — não precisamos copiar/duplicar essa informação como fizemos com o
  aluno.

**Regra prática pra guardar:** se as duas entidades estão no mesmo banco de
dados (mesmo microsserviço), use um relacionamento JPA de verdade
(`@ManyToOne`, `@OneToMany`, etc). Se estão em bancos/serviços diferentes,
guarde só o ID e copie os campos que você precisa exibir depois — porque a
única forma de "buscar de novo" seria uma chamada de rede, o que não vale a
pena fazer toda vez.

### Cada serviço tem sua PRÓPRIA cópia do DTO/Event dos outros

O `ms-matricula` tem seu próprio `AlunoResponseDTO` (uma cópia do formato que
o `ms-aluno` expõe), e o `ms-notificacao` tem sua própria cópia do
`MatriculaRealizadaEvent`. Isso é proposital: evita acoplamento forte entre
os serviços. Se um dia o `ms-aluno` mudar seu DTO interno, o
`ms-matricula` não quebra sozinho — o "contrato" é o formato JSON, não a
classe Java compartilhada.

- **`eureka.client.enabled: false` NUNCA deve ser usado no `eureka-server`.**
  Isso desliga toda a autoconfiguração do client, incluindo peças internas
  que o próprio `EurekaServerAutoConfiguration` depende para funcionar. A
  configuração correta para standalone é `register-with-eureka: false` +
  `fetch-registry: false`.

- **Arquivo de configuração se chama `application.yaml`, não
  `application.yml`.** Os dois formatos existem e o Spring Boot reconhece
  ambos — mas se você criar um e editar o outro por engano, suas mudanças
  nunca vão ter efeito (foi um bug real e frustrante durante o
  desenvolvimento original deste projeto). Fique atento à extensão exata.

## Erros comuns e como resolver

| Sintoma | Causa provável | Solução |
|---|---|---|
| Eureka Server sobe mas dashboard dá "Whitelabel Error Page" | Falta `@EnableEurekaServer` na classe principal | Adicione a anotação |
| `ms-matricula` não acha `ms-aluno` | Um dos dois não subiu, ou não deu tempo de registrar no Eureka | Aguarde ~10s após o boot e confira o dashboard |
| Kafka UI fica em loop de erro/timeout | `docker-compose.yaml` sem listener `INTERNAL` separado | Use o `docker-compose.yaml` deste projeto, já corrigido |
| `ms-notificacao` não recebe mensagens | `spring.json.use.type.headers` não está `false`, ou o pacote da classe local está errado | Confira o `application.yaml` do `ms-notificacao` |
| YAML "quebra" silenciosamente (config não aplica) | Indentação com TAB em vez de espaços | YAML não aceita tabs — use sempre espaços |
| `taskkill /PID ... /F` dá erro no Git Bash | O Git Bash converte `/PID` como se fosse um caminho de arquivo Unix | Use barra dupla: `taskkill //PID ... //F` |
