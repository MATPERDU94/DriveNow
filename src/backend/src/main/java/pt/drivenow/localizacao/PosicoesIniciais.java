package pt.drivenow.localizacao;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import pt.drivenow.frota.FrotaService;
import pt.drivenow.frota.ViaturaDto;

/**
 * Dados de arranque: ao iniciar, as viaturas do Odoo que ainda não têm posição
 * ficam estacionadas em pontos de Guimarães (coordenadas aproximadas). Depois, a posição só muda no fim de cada viagem.
 */
@Component
public class PosicoesIniciais {

    private static final Logger log = LoggerFactory.getLogger(PosicoesIniciais.class);

    private record Ponto(String nome, double lat, double lng) { }

    private static final List<Ponto> PONTOS = List.of(
            new Ponto("Largo do Toural", 41.4417, -8.2955),
            new Ponto("Largo da Oliveira", 41.4434, -8.2938),
            new Ponto("Castelo de Guimarães", 41.4480, -8.2905),
            new Ponto("Paço dos Duques", 41.4466, -8.2906),
            new Ponto("Largo da Mumadona", 41.4458, -8.2922),
            new Ponto("Estação de Guimarães", 41.4378, -8.2934),
            new Ponto("Universidade do Minho (Azurém)", 41.4525, -8.2898),
            new Ponto("Estádio D. Afonso Henriques", 41.4459, -8.3012),
            new Ponto("Teleférico da Penha", 41.4436, -8.2868),
            new Ponto("Centro Cultural Vila Flor", 41.4389, -8.2958),
            new Ponto("Plataforma das Artes", 41.4423, -8.2988),
            new Ponto("Mercado Municipal", 41.4447, -8.2985),
            new Ponto("Parque da Cidade", 41.4368, -8.3050),
            new Ponto("GuimarãeShopping", 41.4500, -8.2990),
            new Ponto("Espaço Guimarães", 41.4570, -8.3110),
            new Ponto("Rua de Santa Maria", 41.4450, -8.2937),
            new Ponto("Igreja de São Francisco", 41.4426, -8.2916),
            new Ponto("Multiusos de Guimarães", 41.4400, -8.3080),
            new Ponto("Hospital Senhora da Oliveira", 41.4350, -8.3020),
            new Ponto("Avenida de Londres", 41.4430, -8.3030),
            new Ponto("Avenida D. Afonso Henriques", 41.4405, -8.2942),
            new Ponto("Azurém", 41.4535, -8.2850),
            new Ponto("Creixomil", 41.4330, -8.3085),
            new Ponto("Urgezes", 41.4310, -8.2950),
            new Ponto("Mesão Frio", 41.4425, -8.2720),
            new Ponto("Costa", 41.4455, -8.2775),
            new Ponto("Santuário da Penha", 41.4320, -8.2650),
            new Ponto("Silvares", 41.4490, -8.3170),
            new Ponto("Fermentões", 41.4600, -8.3050),
            new Ponto("Polvoreira", 41.4280, -8.3200));

    private final FrotaService frota;
    private final LocalizacaoService localizacao;

    public PosicoesIniciais(FrotaService frota, LocalizacaoService localizacao) {
        this.frota = frota;
        this.localizacao = localizacao;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void preencher() {
        try {
            Map<Long, PosicaoViatura> existentes = localizacao.todas();
            List<ViaturaDto> viaturas = frota.listarDoOdoo();
            int i = 0;
            int criadas = 0;
            for (ViaturaDto v : viaturas) {
                Ponto p = PONTOS.get(i++ % PONTOS.size());
                if (!existentes.containsKey(v.id())) {
                    localizacao.atualizar(v.id(), p.lat(), p.lng());
                    criadas++;
                }
            }
            if (criadas > 0) {
                log.info("Posições iniciais atribuídas a {} viaturas (Guimarães)", criadas);
            }
        } catch (RuntimeException e) {
            // Se o Odoo ainda não estiver disponível, a API arranca na mesma.
            log.warn("Não foi possível atribuir posições iniciais: {}", e.getMessage());
        }
    }
}
