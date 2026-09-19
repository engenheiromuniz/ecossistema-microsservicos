# Ecossistema de Microsserviços — Aluno / Matrícula / Notificação

Projeto didático de microsserviços com Spring Boot 3.3.4, Spring Cloud (Eureka +
Gateway + OpenFeign) e Apache Kafka. Cada pasta dentro de `backend/` é um
projeto Maven independente — não é um projeto multi-módulo com um `pom.xml`
pai, então cada um é importado e rodado separadamente.

## Arquitetura, em uma frase por serviço

| Serviço          | Porta | Papel                                                                 |
|-------------------|-------|------------------------------------------------------------------------|
| `eureka-server`   | 8761  | Catálogo de serviços — quem registra quem e onde encontrar cada um    |
| `api-gateway`     | 8080  | Porta única de entrada — roteia `/alunos/**` e `/matriculas/**`        |
| `ms-aluno`        | 8081  | Dono dos dados de aluno (CRUD + decremento de vaga)                    |
| `ms-matricula`    | 8082  | Orquestra a matrícula: chama `ms-aluno` via Feign e publica no Kafka   |
| `ms-notificacao`  | 8083  | Escuta o Kafka e reage a matrículas confirmadas (simula envio de e-mail)|
| Kafka + Kafka UI  | 9092 / 8090 | Mensageria assíncrona entre `ms-matricula` e `ms-notificacao` (via Docker) |

**Fluxo de uma matrícula:**
```
Cliente → api-gateway → ms-matricula → (Feign) → ms-aluno   [síncrono, decide se a matrícula pode acontecer]
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
  "email": "joao@email.com",
  "vagasDisponiveis": 5
}
```
Resposta esperada: `201 Created`, com o `id` gerado (provavelmente `1`).

**2. Confirme o aluno criado**
```
GET http://localhost:8081/alunos/1
```

**3. Matricule o aluno**
```
POST http://localhost:8082/matriculas
Content-Type: application/json

{
  "alunoId": 1
}
```
Resposta esperada: `201 Created`, com o `MatriculaResponseDTO`. Por trás dos
panos: o `ms-matricula` chamou o `ms-aluno` via Feign (duas vezes: buscar e
decrementar vaga), salvou a matrícula, e publicou o evento no Kafka.

**4. Confirme a vaga decrementada**
```
GET http://localhost:8081/alunos/1
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
E-mail: joao@email.com
>>> [SIMULAÇÃO] E-mail de confirmação enviado para João Silva (joao@email.com)
```

Se você viu esse log, o fluxo ponta a ponta — HTTP síncrono via Feign +
mensageria assíncrona via Kafka — está funcionando de verdade.

---

## Decisões de design (vale a pena entender o "porquê")

- **`vagasDisponiveis` está no `Aluno`, não numa `Disciplina`.** No mundo
  real, "vagas" costuma ser propriedade de um recurso disputado por vários
  alunos (uma turma, uma disciplina) — não do aluno em si. Aqui simplificamos
  para manter o projeto com só 2 entidades de negócio (`Aluno` e `Matricula`)
  e focar o aprendizado em Eureka/Feign/Kafka, sem a complexidade extra de um
  terceiro conceito. Fica registrado como uma evolução natural do projeto.

- **Cada serviço tem sua PRÓPRIA cópia do DTO/Event dos outros.** O
  `ms-matricula` tem seu próprio `AlunoResponseDTO` (uma cópia do formato que
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
