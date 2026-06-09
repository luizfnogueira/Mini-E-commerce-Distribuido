# Mini E-commerce Distribuido - Execucao

-- Requisitos

- Java 17 ou superior
- Maven 3.8 ou superior
- Portas livres: 5000, 5001, 5002 e 5003

-- Estrutura

- `api-gateway`: API Gateway na porta 5000
- `users-service`: usuarios na porta 5001
- `products-service`: produtos na porta 5002, com duas replicas SQLite (`products_1.db` e `products_2.db`)
- `orders-service`: pedidos na porta 5003

-- Compilar

Execute na raiz do projeto:

```powershell
mvn -q -DskipTests package -f users-service/pom.xml
mvn -q -DskipTests package -f products-service/pom.xml
mvn -q -DskipTests package -f orders-service/pom.xml
mvn -q -DskipTests package -f api-gateway/pom.xml
```

-- Rodar os servicos

Abra quatro terminais, um para cada comando:

```powershell
java -jar users-service/target/users-service-0.0.1-SNAPSHOT.jar
java -jar products-service/target/products-service-0.0.1-SNAPSHOT.jar
java -jar orders-service/target/orders-service-0.0.1-SNAPSHOT.jar
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar
```

No Windows, tambem e possivel iniciar tudo em segundo plano com:

```powershell
.\run-all.ps1
```

Para encerrar os processos que estiverem usando as portas do projeto:

```powershell
.\stop-all.ps1
```

O ponto de entrada para o cliente e sempre o Gateway:

```text
http://localhost:5000
```

-- Chave JWT

Todos os servicos usam a chave padrao abaixo se a variavel de ambiente `JWT_SECRET` nao for definida:

```text
mini-ecommerce-secret-key-with-at-least-32-chars
```

Para usar outra chave, defina a mesma variavel em todos os processos antes de iniciar.

-- Teste manual com PowerShell

### 1. Registrar admin

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:5000/users/register" `
  -ContentType "application/json" `
  -Body '{"name":"Admin","email":"admin@test.com","password":"123456","role":"admin"}'
```

### 2. Login

```powershell
$login = Invoke-RestMethod -Method Post -Uri "http://localhost:5000/users/login" `
  -ContentType "application/json" `
  -Body '{"email":"admin@test.com","password":"123456"}'

$headers = @{ Authorization = "Bearer $($login.token)" }
```

### 3. Criar produto

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:5000/products" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body '{"name":"Notebook","description":"Produto de teste","price":2999.90,"stock":5}'
```

### 4. Listar produtos

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:5000/products" -Headers $headers
```

### 5. Criar pedido

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:5000/orders" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body '{"userId":1,"productId":1,"quantity":1}'
```

### 6. Listar pedidos de um usuario

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:5000/orders/1" -Headers $headers
```

-- Healthcheck e heartbeat

Cada microsservico tem:

```text
GET /health -> {"status":"ok"}
```

O Gateway consulta os healthchecks a cada 5 segundos. Se um servico falhar mais de 2 vezes, o Gateway registra erro no log e retorna `503 Service Unavailable` para rotas daquele servico. Quando voltar, registra recuperacao.
