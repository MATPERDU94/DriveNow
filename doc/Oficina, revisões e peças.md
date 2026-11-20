# DriveNow — Oficina, revisões e peças

Nov 20, 2026 · @Mateus

## Resumo

Os 30 carros da DriveNow vão obrigatoriamente à oficina a cada **15 000 km**, com o mesmo intervalo para todos. A oficina própria faz as revisões e as reparações simples com peças do armazém; o resto vai para uma oficina externa.

As peças, os stocks, as compras (entradas) e os consumos (saídas) ficam no **Odoo**, nas apps Inventário e Compras, ao lado da Frota. Este documento define as regras e a lista de peças; o fluxo está nos diagramas 4b (reparação), 6 (revisões por km) e 7 (armazém e compras).

Todos os carros são elétricos: não há trocas de óleo, de filtro de óleo nem de velas.

## Política de revisões

Cada carro tem no Odoo os **km da próxima revisão**. Para começar, usa-se o múltiplo de 15 000 seguinte aos km atuais: um carro com 12 500 km fica com a revisão aos 15 000; um com 27 000 km, aos 30 000. Depois de cada revisão somam-se 15 000.

- **Aviso:** quando faltam menos de 1 000 km, o n8n avisa a oficina de que a revisão está para breve.
- **Bloqueio:** quando os km chegam ao limite, o carro sai do mapa com o motivo "Revisão por fazer". Uma viagem em curso termina normalmente.
- **Desbloqueio:** quando a oficina conclui o serviço "Revisão" no Odoo, o carro volta ao mapa.

O intervalo é metade do que os fabricantes indicam para uso particular (o Renault ZOE tem revisão anual ou a cada 30 000 km). Um carro partilhado faz muitas viagens curtas na cidade, com muitos condutores, e gasta pneus e travões mais depressa.

| O que se faz em cada revisão | Como |
| --- | --- |
| Pneus | Verificar o desgaste e a pressão, rodar; trocar se o piso estiver no limite |
| Pastilhas de travão | Verificar a espessura; trocar se for preciso |
| Filtro do habitáculo | Trocar uma vez por ano |
| Escovas limpa-para-brisas | Trocar uma vez por ano, ou antes se riscarem |
| Líquido de travões | Verificar; trocar a cada 4 anos |
| Líquido limpa-vidros | Atestar |
| Bateria de 12 V | Testar; trocar a cada 4 anos |
| Luzes, cabo de carregamento e interior | Verificar e limpar |

Fontes: [manual do Tesla Model 3](https://www.tesla.com/ownersmanual/model3/en_nz/GUID-E95DAAD9-646E-4249-9930-B109ED7B1D91.html) (rodar pneus a cada 10 000 km, filtro a cada 2 anos, líquido de travões a cada 4 anos) e [SicurAUTO sobre o Renault ZOE](https://www.sicurauto.it/ricambi-e-accessori/tecnica-e-manutenzione/tagliandi-auto-renault-clio-1-0-tce-vs-renault-zoe-r135/) (revisão anual ou a cada 30 000 km; bateria de 12 V e líquido de travões a cada 4 anos).

## O que a oficina DriveNow faz

A oficina própria faz as revisões e as reparações que só precisam de trocar uma peça do armazém. Chapa, pintura, vidros, acidentes e avarias vão para uma oficina externa, e o custo fica registado no Odoo. A inspeção periódica obrigatória é feita num centro de inspeção e está no diagrama 5.

Para cada tipo de dano da tabela do Odoo:

| Tipo de dano | Imobiliza o carro | Onde se repara | Peças do armazém |
| --- | --- | --- | --- |
| Pneu furado ou danificado | Sim | Oficina DriveNow | Pneu |
| Espelho retrovisor partido | Não | Oficina DriveNow | Espelho retrovisor (vidro e capa) |
| Farol ou farolim partido | Não | Oficina DriveNow | Farolim / lâmpadas |
| Cabo de carregamento danificado ou em falta | Não | Oficina DriveNow | Cabo de carregamento tipo 2 |
| Interior sujo | Não | Oficina DriveNow | Produtos de limpeza |
| Estofos danificados | Não | Oficina DriveNow (limpeza) ou externa (estofador) | Produtos de limpeza |
| Para-brisas lascado | Não | Externa (vidros) | — |
| Vidro partido ou fissurado | Sim | Externa (vidros) | — |
| Risco ligeiro na pintura | Não | Externa (chapa e pintura) | — |
| Risco profundo na pintura | Não | Externa (chapa e pintura) | — |
| Amolgadela pequena | Não | Externa (chapa e pintura) | — |
| Amolgadela grande | Sim | Externa (chapa e pintura) | — |
| Para-choques danificado | Não | Externa (chapa e pintura) | — |
| Jante riscada | Não | Externa (reparação de jantes) | — |
| Acidente com outro veículo | Sim | Externa | — |
| Avaria mecânica ou elétrica | Sim | Externa (concessionário da marca) | — |

## Peças em stock

O armazém da oficina tem 11 peças. O stock mínimo cobre cerca de um mês de consumo e o máximo dois a três meses. Os valores são estimativas para 30 carros, a validar pelo grupo.

| Peça | Usada em | Unidade | Consumo estimado por mês | Mínimo | Máximo |
| --- | --- | --- | --- | --- | --- |
| Pneu | Revisão, dano "pneu" | unidade | 5 a 6 | 8 | 16 |
| Pastilhas de travão | Revisão | jogo (um eixo) | 1 a 2 | 3 | 6 |
| Filtro do habitáculo | Revisão | unidade | 2 a 3 | 4 | 10 |
| Escovas limpa-para-brisas | Revisão | par | 2 a 3 | 4 | 10 |
| Líquido de travões DOT 4 | Revisão | litro | menos de 1 | 2 | 5 |
| Líquido limpa-vidros | Revisão | litro | cerca de 20 | 20 | 60 |
| Bateria de 12 V | Revisão | unidade | menos de 1 | 1 | 3 |
| Espelho retrovisor (vidro e capa) | Dano "espelho" | unidade | menos de 1 | 1 | 3 |
| Farolim / lâmpadas | Dano "farol" | unidade | 1 a 2 | 2 | 5 |
| Cabo de carregamento tipo 2 | Dano "cabo" | unidade | menos de 1 | 1 | 2 |
| Produtos de limpeza do interior | Dano "interior sujo" ou "estofos" | kit | 2 a 3 | 3 | 10 |

Pressupostos do cálculo:

- cada carro faz cerca de 1 500 km por mês, ou seja, 45 000 km por mês na frota e cerca de 3 revisões por mês;
- os pneus duram cerca de 40 000 km e as pastilhas cerca de 30 000 km;
- o filtro do habitáculo e as escovas trocam-se uma vez por ano em cada carro; o líquido de travões e a bateria de 12 V a cada 4 anos;
- o fornecedor entrega em cerca de uma semana, por isso o mínimo deixa margem.

Os pneus e as pastilhas mudam de referência conforme o modelo. No projeto usa-se um produto por peça; numa empresa real criava-se uma variante por medida ou por modelo.

## Fornecedores e compras

As compras são automáticas a partir do stock mínimo e só precisam da confirmação do gestor de frota.

1. **Encomenda:** todos os dias, o Odoo compara o stock com o mínimo de cada peça. Se ficar abaixo, cria um pedido de cotação ao fornecedor, com a quantidade que falta para chegar ao máximo.
2. **Aviso:** o n8n envia ao gestor um email com as peças em falta.
3. **Confirmação:** o gestor revê o pedido no Odoo e confirma-o, passando a pedido de compra.
4. **Receção (entrada):** quando as peças chegam, o armazém confere as quantidades e valida a receção. O stock sobe.
5. **Fatura do fornecedor:** fica registada no Odoo, ligada ao pedido de compra.
6. **Consumo (saída):** cada peça usada numa revisão ou reparação sai do stock, com a matrícula do carro como referência. Assim sabe-se quanto custou cada carro em peças.

Fornecedores (contactos fictícios a criar no Odoo):

| Fornecedor | Peças | Prazo de entrega |
| --- | --- | --- |
| Fornecedor de pneus | Pneus | 5 dias úteis |
| Fornecedor de peças auto | Todas as outras peças | 5 dias úteis |

## Como fica no Odoo

Tudo fica no mesmo Odoo Community da Frota, com duas apps gratuitas a mais: **Inventário** e **Compras**.

| Onde | O que se cria |
| --- | --- |
| Inventário → Armazéns | O armazém "Oficina DriveNow" (renomear o que o Odoo cria por omissão) |
| Inventário → Produtos | As 11 peças, como produtos com controlo de stock, com a unidade da tabela de peças |
| Produto → separador Compras | O fornecedor de cada peça, o preço e o prazo de entrega |
| Inventário → Regras de reaprovisionamento | O mínimo e o máximo de cada peça; o Odoo cria os pedidos de cotação sozinho |
| Inventário → Tipos de operação | "Consumo na oficina", uma saída do armazém com a matrícula do carro na origem |
| Contactos | Os dois fornecedores fictícios |
| Frota → Tipos de serviço | O serviço "Revisão" |
| Frota → Veículo | O campo novo "km da próxima revisão" (a acrescentar no módulo drivenow\_danos) |

O backend passa a tratar um serviço "Revisão" por concluir como trata hoje um dano que imobiliza o carro: o carro sai do mapa até o serviço ser concluído.

## Decisões em aberto e próximos passos

Por decidir:

- Os mínimos e máximos da tabela de peças, e o pressuposto de 1 500 km por carro por mês.
- Se a revisão é também obrigatória uma vez por ano, mesmo que o carro não chegue aos 15 000 km.
- O preço por km de cada modelo, no tarifário da Frota.

Próximos passos:

- [ ] Instalar as apps Inventário e Compras no Odoo
- [ ] Criar o armazém, as 11 peças, os fornecedores e as regras de mínimo e máximo
- [ ] Acrescentar o campo "km da próxima revisão" e o serviço "Revisão" na Frota
- [ ] Fazer o fluxo do n8n que verifica a revisão no fim de cada viagem
- [ ] Pôr o backend a esconder do mapa os carros com revisão por fazer
- [ ] Testar uma revisão completa: bloqueio, saída de peças, nova compra e desbloqueio
