import json
import random
from dataclasses import asdict, dataclass, fields
from datetime import date
from typing import Optional

DIAS = ["Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"]


def hhmm(minutos: int) -> str:
    return f"{minutos // 60:02d}:{minutos % 60:02d}"


def duracao(minutos: int) -> str:
    return f"{minutos // 60}h{minutos % 60:02d}"


def ler_hhmm(texto: str) -> int:
    """Converte "7:05", "07:05" ou "0705" em minutos desde 00:00."""
    texto = texto.strip()
    if ":" in texto:
        h, _, m = texto.partition(":")
    elif texto.isdigit() and 3 <= len(texto) <= 4:
        h, m = texto[:-2], texto[-2:]
    else:
        raise ValueError(texto)
    if not (h.isdigit() and m.isdigit() and len(m) == 2):
        raise ValueError(texto)
    h, m = int(h), int(m)
    if h > 23 or m > 59:
        raise ValueError(texto)
    return h * 60 + m


@dataclass(frozen=True)
class Config:
    """Regras de geração. Horários e durações em minutos."""

    entrada_min: int = 6 * 60 + 55  # 06:55 (tolerância antecipada de 5 min)
    entrada_max: int = 7 * 60 + 15  # 07:15 (tolerância de 15 min da CCT)
    saida_almoco_min: int = 11 * 60 + 55  # 11:55 (mínimo do RH)
    saida_almoco_max: int = 12 * 60 + 10  # 12:10
    almoco_min: int = 60  # 1h00
    almoco_max: int = 70  # 1h10
    retorno_min: int = 12 * 60 + 55  # 12:55 (mínimo do RH)
    jornada_semana: int = 9 * 60  # segunda a quinta (e fim de semana)
    jornada_sexta: int = 8 * 60
    variacao: int = 10  # ±10 min na jornada
    saida_min_semana: int = 16 * 60 + 55  # 16:55 (mínimo do RH)
    saida_min_sexta_ativa: bool = False
    saida_min_sexta: int = 15 * 60 + 55

    def jornada(self, dia: date) -> int:
        return self.jornada_sexta if dia.weekday() == 4 else self.jornada_semana

    def saida_minima(self, dia: date) -> Optional[int]:
        if dia.weekday() != 4:
            return self.saida_min_semana
        return self.saida_min_sexta if self.saida_min_sexta_ativa else None

    def validar(self) -> dict[str, str]:
        """Erros por campo; vazio quando tudo está certo."""
        erros = {}
        if self.entrada_max < self.entrada_min:
            erros["entrada_max"] = "Antes do horário mais cedo"
        if self.saida_almoco_max < self.saida_almoco_min:
            erros["saida_almoco_max"] = "Antes da saída mínima"
        if self.saida_almoco_min <= self.entrada_max:
            erros["saida_almoco_min"] = "Precisa ser depois da entrada"
        if not 0 <= self.almoco_min <= 240:
            erros["almoco_min"] = "Entre 0 e 240 min"
        if not 0 <= self.almoco_max <= 240:
            erros["almoco_max"] = "Entre 0 e 240 min"
        elif self.almoco_max < self.almoco_min:
            erros["almoco_max"] = "Menor que a mínima"
        for campo in ("jornada_semana", "jornada_sexta"):
            if not 60 <= getattr(self, campo) <= 16 * 60:
                erros[campo] = "Entre 01:00 e 16:00"
        if not 0 <= self.variacao <= 60:
            erros["variacao"] = "Entre 0 e 60 min"
        return erros

    def para_json(self) -> str:
        return json.dumps(asdict(self))

    @classmethod
    def de_json(cls, texto: Optional[str]) -> "Config":
        """Lê a configuração salva; campos ausentes ou inválidos voltam ao padrão."""
        try:
            salvo = json.loads(texto or "{}")
            padrao = cls()
            valores = {
                f.name: salvo[f.name]
                for f in fields(cls)
                if f.name in salvo and type(salvo[f.name]) is type(getattr(padrao, f.name))
            }
            cfg = cls(**valores)
        except (ValueError, TypeError):
            return cls()
        return cls() if cfg.validar() else cfg


PADRAO = Config()


def sortear(minimo: int, maximo: int, piso: Optional[int] = None) -> int:
    """Sorteia entre minimo e maximo, nunca abaixo do piso (o piso vence o máximo)."""
    if piso is not None:
        minimo = max(minimo, piso)
    return random.randint(minimo, max(minimo, maximo))


def gerar_horario(dia: date, cfg: Config = PADRAO) -> tuple[int, int, int, int]:
    entrada = sortear(cfg.entrada_min, cfg.entrada_max)
    saida_almoco = sortear(cfg.saida_almoco_min, cfg.saida_almoco_max)
    almoco = sortear(cfg.almoco_min, cfg.almoco_max, piso=cfg.retorno_min - saida_almoco)
    retorno = saida_almoco + almoco
    # Jornada = manhã + tarde (sem o almoço)
    manha = saida_almoco - entrada
    base = cfg.jornada(dia)
    saida_min = cfg.saida_minima(dia)
    # Encurta a variação para baixo quando a saída cairia antes do mínimo
    piso = None if saida_min is None else saida_min - retorno + manha
    jornada = sortear(base - cfg.variacao, base + cfg.variacao, piso)
    saida = retorno + jornada - manha
    return entrada, saida_almoco, retorno, saida
