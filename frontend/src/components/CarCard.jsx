function CarCard({ viatura, hub }) {
  return (
    <div className="car-card">
      <h3>{viatura.marca} {viatura.modelo}</h3>
      <p>{viatura.ano} · {viatura.lugares} lugares · {viatura.transmissao}</p>
      <p>{viatura.matricula} · {viatura.km.toLocaleString("pt-PT")} km</p>
      <p>📍 {hub.nome} ({hub.cidade})</p>
      <p className="preco">{viatura.precoDia} € / dia</p>
      <p>
        {viatura.precoEntrada > 0
          ? `Entrada: ${viatura.precoEntrada} €`
          : "Sem entrada"}
      </p>
      <span className="estado">{viatura.estado}</span>
    </div>
  );
}

export default CarCard;