# Perguntas para o store:


# 0. explicar as alterações efetuadas:
    - retirar os hubs e so na cidade de Guimarães
    - retirar as reservas (acabou os slots)
    - utilização de GPS, e ver os carros mais pertos
    - questao de grupo: se o objetivo é procurar o carro mais perto, nao fazia sentido ter tantas variedades de carros
    - implementamos o odoo comunity (via ubuntu)
    - 


# 1. Vendas no SAP
Qual API devemos usar para registar as vendas? Ou a integração com o SAP pode ficar no Business Partner (criar e consultar) e nas consultas de produtos e vendas?”


# 2. A arquitetura (mostrar o diagrama v2)
Propomos: 
    - SAP Build Apps como aplicação
    - Spring Boot como middleware só para as regras de negócio (reservas, preço, danos, pagamentos)
    - n8n para orquestrar os fluxos entre o SAP, o Moloni e o Odoo. 


# 3. A frota no Odoo e não no SAP
Queria saber se as viaturas devem existir também no SAP?
porque os produtos do SAP são da Global Bike, por isso pusemos o Odoo como registo mestre das viaturas (frota, tarifário, danos, documentos) e o SAP como referência dos clientes. 

