import { CreditCard, Landmark, Lock } from 'lucide-react';
import { Campo } from './ui';
import {
  mascaraCartao,
  mascaraIban,
  mascaraValidade,
  soDigitos,
  type DadosPagamento,
} from '../lib/validacao';

type Erros = Partial<Record<keyof DadosPagamento, string>>;

export function CamposPagamento({
  valor,
  aoMudar,
  erros,
  aoFocar,
  mostrarTeste = true,
}: {
  valor: DadosPagamento;
  aoMudar: (v: DadosPagamento) => void;
  erros: Erros;
  aoFocar?: () => void;
  mostrarTeste?: boolean;
}) {
  const set = <K extends keyof DadosPagamento>(k: K, v: DadosPagamento[K]) => aoMudar({ ...valor, [k]: v });

  return (
    <div onFocus={aoFocar}>
      <div className="radios" role="radiogroup" aria-label="Tipo de meio de pagamento">
        <label className={`opcao-radio${valor.tipo === 'CARTAO' ? ' opcao-radio--ativa' : ''}`}>
          <input
            type="radio"
            name="tipo-pagamento"
            checked={valor.tipo === 'CARTAO'}
            onChange={() => set('tipo', 'CARTAO')}
          />
          <span className="opcao-radio__circ" />
          <span>Cartão</span>
          <CreditCard size={20} className="opcao-radio__ico" />
        </label>
        <label className={`opcao-radio${valor.tipo === 'CONTA_BANCARIA' ? ' opcao-radio--ativa' : ''}`}>
          <input
            type="radio"
            name="tipo-pagamento"
            checked={valor.tipo === 'CONTA_BANCARIA'}
            onChange={() => set('tipo', 'CONTA_BANCARIA')}
          />
          <span className="opcao-radio__circ" />
          <span>Conta bancária</span>
          <Landmark size={20} className="opcao-radio__ico" />
        </label>
      </div>

      {valor.tipo === 'CARTAO' ? (
        <>
          <Campo
            rotulo="Titular do cartão"
            placeholder="ANA SILVA"
            autoComplete="cc-name"
            value={valor.titular}
            onChange={(e) => set('titular', e.target.value.toUpperCase())}
            erro={erros.titular}
          />
          <Campo
            rotulo="Número do cartão"
            placeholder="0000 0000 0000 0000"
            inputMode="numeric"
            autoComplete="cc-number"
            value={valor.numero}
            onChange={(e) => set('numero', mascaraCartao(e.target.value))}
            erro={erros.numero}
            ajuda={
              mostrarTeste ? (
                <>
                  Cartões de teste: <b>4242 4242 4242 4242</b> aprova · <b>4000 0000 0000 0002</b> recusa
                </>
              ) : undefined
            }
          />
          <div className="linha">
            <Campo
              rotulo="Validade"
              placeholder="MM/AA"
              inputMode="numeric"
              autoComplete="cc-exp"
              value={valor.validade}
              onChange={(e) => set('validade', mascaraValidade(e.target.value, valor.validade))}
              erro={erros.validade}
            />
            <Campo
              rotulo="CVV"
              placeholder="123"
              inputMode="numeric"
              autoComplete="cc-csc"
              value={valor.cvv}
              onChange={(e) => set('cvv', soDigitos(e.target.value).slice(0, 4))}
              erro={erros.cvv}
            />
          </div>
        </>
      ) : (
        <>
          <Campo
            rotulo="Titular da conta"
            placeholder="Ana Silva"
            autoComplete="name"
            value={valor.titular}
            onChange={(e) => set('titular', e.target.value)}
            erro={erros.titular}
          />
          <Campo
            rotulo="IBAN"
            placeholder="PT50 0000 0000 0000 0000 0000 0"
            autoComplete="off"
            value={valor.iban}
            onChange={(e) => set('iban', mascaraIban(e.target.value))}
            erro={erros.iban}
            ajuda={
              mostrarTeste ? (
                <>
                  IBAN de teste: <b>PT50 0002 0123 1234 5678 9015 4</b>
                </>
              ) : undefined
            }
          />
        </>
      )}

      <p className="nota-cadeado">
        <Lock size={18} />
        <span>
          {valor.tipo === 'CARTAO'
            ? 'O valor de cada viagem é debitado automaticamente no fim da viagem.'
            : 'O valor de cada viagem é debitado por débito direto no fim da viagem.'}{' '}
          Só guardamos os últimos 4 dígitos.
        </span>
      </p>
    </div>
  );
}
