from odoo import api, fields, models

# Política da DriveNow: todos os carros vão à oficina a cada 15 000 km (doc "Oficina, revisões e peças").
INTERVALO_REVISAO_KM = 15000
# A partir daqui a revisão aparece como "próxima" (aviso ao gestor de frota).
AVISO_REVISAO_KM = 1000


def proxima_revisao(km):
    """Primeiro múltiplo de 15 000 km acima dos km atuais (ex.: 12 500 -> 15 000; 15 000 -> 30 000)."""
    return (int((km or 0) // INTERVALO_REVISAO_KM) + 1) * INTERVALO_REVISAO_KM


class FleetVehicle(models.Model):
    _inherit = 'fleet.vehicle'

    next_service_km = fields.Float(
        'Km da próxima revisão', digits=(10, 0),
        help='Quando o conta-km chega a este valor, o carro sai do mapa até a revisão ser feita. '
             'Atualiza-se sozinho quando uma revisão é concluída (+15 000 km).')
    km_to_service = fields.Float('Km até à revisão', digits=(10, 0), compute='_compute_revisao')
    service_status = fields.Selection([
        ('ok', 'Em dia'),
        ('soon', 'Revisão próxima'),
        ('due', 'Revisão por fazer'),
    ], string='Revisão', compute='_compute_revisao')

    @api.depends('odometer', 'next_service_km')
    def _compute_revisao(self):
        for v in self:
            if not v.next_service_km:
                v.km_to_service = 0
                v.service_status = 'ok'
                continue
            falta = v.next_service_km - (v.odometer or 0)
            v.km_to_service = falta
            v.service_status = 'due' if falta <= 0 else 'soon' if falta <= AVISO_REVISAO_KM else 'ok'

    @api.model
    def _drivenow_inicializar_revisoes(self):
        """Corre em cada atualização do módulo: dá a próxima revisão aos carros que ainda não a têm."""
        for v in self.with_context(active_test=False).search([('next_service_km', '=', 0)]):
            v.next_service_km = proxima_revisao(v.odometer)


class FleetVehicleLogServices(models.Model):
    _inherit = 'fleet.vehicle.log.services'

    def _drivenow_revisoes_concluidas(self):
        """Revisão concluída: a próxima fica para daqui a 15 000 km (contados dos km atuais do carro)."""
        for s in self:
            if s.state == 'done' and s.service_type_id.is_revision and s.vehicle_id:
                km = max(s.vehicle_id.odometer or 0, s.odometer or 0)
                s.vehicle_id.next_service_km = km + INTERVALO_REVISAO_KM

    @api.model_create_multi
    def create(self, vals_list):
        registos = super().create(vals_list)
        registos._drivenow_revisoes_concluidas()
        return registos

    def write(self, vals):
        res = super().write(vals)
        if vals.get('state') == 'done':
            self._drivenow_revisoes_concluidas()
        return res
