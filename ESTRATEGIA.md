# Estratégia — Equipe Guaraná (`AgenteGuarana`)



## 1. Arquitetura Enxuta e Ciclo de Vida

O agente opera sem alocações dinâmicas em tempo de execução (`HashMap`, `ArrayList`, `HashSet`) e utiliza um protocolo minimalista de apenas **duas mensagens** (`H` e `C`), dividido em três etapas:

### Etapa 1 — Abertura em Leque e Handshake (`Turno 1`)
* No primeiro turno de vida, cada agente transmite `"H|" + getId()` para os aliados no alcance e executa `abreEmLequeNoSpawn()`.
* Testando as quatro direções cardeais a partir de `getId() % 4` com `podeMoverPara()`, o grupo se abre imediatamente pelas direções livres, evitando empilhamento e dano em área caso nasça na mesma coordenada de uma equipe adversária.
* Ao receber mensagens `H|<idAliado>`, cada agente conta quantos aliados possuem ID menor que o seu, descobrindo seu **índice exato dentro da equipe (`0` a `14`)** independentemente de quantas equipes existam na arena.
* *Fallback para divisões:* agentes gerados posteriormente via `divide()` não recebem respostas de *handshake* no turno 1; nesses casos (`recebeuHandshake == false`), o índice base utiliza o próprio `getId()`.

### Etapa 2 — Setorização Relativa ao Spawn (`Turno 2`)
* O mapa é dividido em uma grade lógica de **5 colunas × 3 linhas** (`15` setores).
* Em vez de mapear índices fixos a coordenadas absolutas (o que forçaria agentes nascidos na direita a atravessar o mapa inteiro até a esquerda), cada agente calcula a distância Manhattan do seu ponto de nascimento `(spawnX, spawnY)` até o centro de cada um dos 15 setores e os ordena do mais próximo ao mais distante.
* O agente de `rank 0` assume o setor imediato da base, os seguintes ocupam os setores adjacentes e apenas os últimos avançam para o lado oposto da arena, minimizando o gasto energético de deslocamento inicial em qualquer posição de spawn.

### Etapa 3 — Operação Contínua, Coleta e Conservação
* **Ritmo econômico (`2:1`):** tanto no deslocamento até o setor quanto na patrulha interna, o agente alterna **2 turnos andando e 1 turno parado** (`turno % 3 == 0`). Isso reduz em ~33% o gasto de movimento mantendo deslocamento constante para evitar a detecção de inatividade.
* **Coleta de cogumelos e micro-passo anti-ceifador:** ao receber energia (`recebeuEnergia()`), o agente memoriza a coordenada `(cogumeloX, cogumeloY)` e anuncia `"C|x|y"` a cada 6 turnos. Enquanto carrega, permanece sobre a fonte, mas executa um passo lateral a cada 4 turnos (`turnosNoCogumelo % 4 == 0`) e retorna à coordenada memorizada caso saia da borda do ponto de energia.
* **Atendimento local:** aliados que ainda não estão carregando e estejam a até `180` pixels de um cogumelo anunciado deslocam-se até ele para maximizar a absorção de energia da equipe.

---

## 2. Combate, Fuga Tática e Divisão

* **Conservação de energia em combate:** o agente não persegue inimigos pelo mapa. Se entrar em confronto (`tomouDano()`), mantém pressão local sem gastar turnos fugindo à toa.
* **Fuga tática:** caso enfrente um adversário com vantagem substancial (`energiaInimigo > getEnergia() + 150` após 2 turnos de troca), aciona `iniciaFuga()`, recuando por `6` ticks na direção oposta para preservar o agente.
* **Critérios estritos de divisão (`divide()`):**
  * **Divisão pacífica:** permitida apenas quando `getEnergia() >= 1550`, garantindo que os dois agentes resultantes tenham reserva elevada (`>= 775`) para sobreviver até o fim da partida.
  * **Divisão ofensiva:** permitida uma única vez por combate (`!jaDividiu`), a partir do 2º turno de troca, exclusivamente na janela `700 <= getEnergia() <= 1300` contra inimigos enfraquecidos (`energiaInimigo <= 350` e `getEnergia() >= energiaInimigo * 2 + 100`).

---

## 3. Protocolo de Mensagens

| Prefixo | Formato | Emissor | Finalidade |
| :--- | :--- | :--- | :--- |
| `H` | `H\|<id>` | Todos no Turno 1 | Descobrir o índice relativo (`0..14`) da equipe sem depender do `getId()` global. |
| `C` | `C\|<x>\|<y>` | Agente sobre fonte | Compartilhar coordenada ativa de energia com aliados em raio de até `180` pixels. |