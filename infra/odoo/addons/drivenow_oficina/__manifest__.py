{
    'name': 'DriveNow - Oficina e peças',
    'version': '19.0.1.0.0',
    'summary': 'DriveNow: armazém da oficina, 11 peças, fornecedores e compras automáticas (diagrama 7)',
    'description': """
Armazém "Oficina DriveNow" com as peças das revisões e das reparações simples.

- 11 peças com stock, preço de compra e fornecedor (documento "Oficina, revisões e peças").
- Regras de mínimo e máximo: quando uma peça desce abaixo do mínimo, o Odoo cria sozinho
  um pedido de cotação ao fornecedor, com a quantidade que falta para chegar ao máximo.
- Tipo de operação "Consumo na oficina": saída de peças do armazém, com a matrícula do carro.
- Menu Frota -> Oficina.
""",
    'category': 'Human Resources/Fleet',
    'depends': ['drivenow_danos', 'stock', 'purchase_stock'],
    'data': [
        'data/oficina_data.xml',
        'data/pecas_data.xml',
        'views/oficina_views.xml',
    ],
    'post_init_hook': 'post_init_hook',
    'installable': True,
    'license': 'LGPL-3',
}
