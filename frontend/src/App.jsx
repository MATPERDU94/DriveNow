import { useState } from "react";
import { frota } from "./data/frota";
import { hubs } from "./data/hubs";
import CarCard from "./components/CarCard";
import logo from "./assets/logo.png";
import "./App.css";

function App() {
  const [hubId, setHubId] = useState("TODOS");
  const [precoMax, setPrecoMax] = useState(150);

  const visiveis = frota.filter(
    (v) => (hubId === "TODOS" || v.hubId === hubId) && v.precoDia <= precoMax
  );

  return (
    <div className="app">
      <header className="header">
        <img src={logo} alt="DriveNow" className="logo" />
        <h1>DriveNow</h1>
      </header>

      <div className="filtros">
        <select value={hubId} onChange={(e) => setHubId(e.target.value)}>
          <option value="TODOS">Todos os hubs</option>
          {hubs.map((h) => (
            <option key={h.id} value={h.id}>
              {h.cidade} - {h.nome}
            </option>
          ))}
        </select>

        <label>
          Preço máx: {precoMax} €/dia{" "}
          <input
            type="range"
            min="40"
            max="150"
            value={precoMax}
            onChange={(e) => setPrecoMax(Number(e.target.value))}
          />
        </label>
      </div>

      <p>{visiveis.length} viaturas</p>

      <div className="grid">
        {visiveis.map((v) => (
          <CarCard
            key={v.id}
            viatura={v}
            hub={hubs.find((h) => h.id === v.hubId)}
          />
        ))}
      </div>
    </div>
  );
}

export default App;