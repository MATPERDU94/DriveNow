from odoo import fields, models


class FleetVehicleModel(models.Model):
    _inherit = 'fleet.vehicle.model'

    entry_fee = fields.Float('Taxa de entrada (€)', digits=(10, 2),
                             help='Valor cobrado ao cliente quando reserva um carro deste modelo.')
    price_per_minute = fields.Float('Preço por minuto (€)', digits=(10, 2))


class FleetVehicle(models.Model):
    _inherit = 'fleet.vehicle'

    entry_fee = fields.Float(related='model_id.entry_fee', string='Taxa de entrada (€)')
    price_per_minute = fields.Float(related='model_id.price_per_minute', string='Preço por minuto (€)')
