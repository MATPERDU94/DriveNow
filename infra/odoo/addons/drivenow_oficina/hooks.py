"""Stock inicial das peças, criado uma só vez, quando o módulo é instalado.

Fica um pouco acima do mínimo de cada peça: assim a primeira revisão já baixa algumas peças
para baixo do mínimo e mostra a compra automática (diagrama 7). Muda-se depois em
Inventário -> Operações -> Inventário físico.
"""

# xml id da peça -> quantidade inicial no armazém "Oficina DriveNow"
STOCK_INICIAL = {
    'peca_pneu': 10,
    'peca_pastilhas': 4,
    'peca_filtro': 5,
    'peca_escovas': 5,
    'peca_liquido_travoes': 3,
    'peca_limpa_vidros': 25,
    'peca_bateria': 2,
    'peca_espelho': 2,
    'peca_farolim': 3,
    'peca_cabo': 2,
    'peca_limpeza': 4,
}


def post_init_hook(env):
    armazem = env.ref('stock.warehouse0')
    quants = env['stock.quant'].with_context(inventory_mode=True)
    novos = quants.browse()
    for xml_id, quantidade in STOCK_INICIAL.items():
        peca = env.ref(f'drivenow_oficina.{xml_id}')
        novos |= quants.create({
            'product_id': peca.id,
            'location_id': armazem.lot_stock_id.id,
            'inventory_quantity': quantidade,
        })
    novos.action_apply_inventory()
