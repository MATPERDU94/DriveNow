from odoo import fields, models


class FleetVehicleModel(models.Model):
    _inherit = 'fleet.vehicle.model'

    entry_fee = fields.Float('Taxa de entrada (€)', digits=(10, 2),
                             help='Valor cobrado ao cliente quando reserva um carro deste modelo.')
    price_per_minute = fields.Float('Preço por minuto (€)', digits=(10, 2))
    price_per_km = fields.Float('Preço por km (€)', digits=(10, 2),
                                help='Valor cobrado por cada km da viagem (km finais - km iniciais).')


class FleetVehicle(models.Model):
    _inherit = 'fleet.vehicle'

    entry_fee = fields.Float(related='model_id.entry_fee', string='Taxa de entrada (€)')
    price_per_minute = fields.Float(related='model_id.price_per_minute', string='Preço por minuto (€)')
    price_per_km = fields.Float(related='model_id.price_per_km', string='Preço por km (€)')
