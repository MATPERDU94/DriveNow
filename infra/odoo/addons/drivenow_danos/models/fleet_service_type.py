from odoo import fields, models


class FleetServiceType(models.Model):
    _inherit = 'fleet.service.type'

    is_damage = fields.Boolean('É um dano', default=False, index=True)
    damage_cost = fields.Float('Custo (€)', digits=(10, 2))
    damage_severity = fields.Selection([
        ('ligeiro', 'Ligeiro'),
        ('moderado', 'Moderado'),
        ('grave', 'Grave'),
    ], string='Gravidade', default='ligeiro')
    blocks_vehicle = fields.Boolean('Imobiliza o carro')
    damage_description = fields.Text('Descrição para o cliente')
