package com.ponto.ponto

import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

val DIAS = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")

fun nomeDoDia(dia: LocalDate): String = DIAS[dia.dayOfWeek.value - 1]

private fun doisDigitos(n: Int) = n.toString().padStart(2, '0')

/** 425 -> "07:05" */
fun hhmm(minutos: Int): String = "${doisDigitos(minutos / 60)}:${doisDigitos(minutos % 60)}"

/** 531 -> "8h51" */
fun duracao(minutos: Int): String = "${minutos / 60}h${doisDigitos(minutos % 60)}"

/** 540 -> "9h", 510 -> "8h30" */
fun horas(minutos: Int): String = if (minutos % 60 == 0) "${minutos / 60}h" else duracao(minutos)

/** Converte "7:05", "07:05" ou "0705" em minutos desde 00:00; null se for inválido. */
fun lerHhmm(texto: String): Int? {
    val t = texto.trim()
    val (h, m) = when {
        ":" in t -> t.substringBefore(":") to t.substringAfter(":")
        t.length in 3..4 && t.all(Char::isDigit) -> t.dropLast(2) to t.takeLast(2)
        else -> return null
    }
    if (h.length !in 1..2 || !h.all(Char::isDigit) || m.length != 2 || !m.all(Char::isDigit)) return null
    val hora = h.toInt()
    val minuto = m.toInt()
    if (hora > 23 || minuto > 59) return null
    return hora * 60 + minuto
}

enum class Tipo { HORA, MINUTOS }

/** Campos editáveis na tela de configurações. [chave] é o nome salvo em JSON. */
enum class Campo(
    val chave: String,
    val rotulo: String,
    val tipo: Tipo,
    val ler: (Config) -> Int,
    val gravar: (Config, Int) -> Config,
) {
    ENTRADA_MIN("entrada_min", "Mais cedo", Tipo.HORA, { it.entradaMin }, { c, v -> c.copy(entradaMin = v) }),
    ENTRADA_MAX("entrada_max", "Mais tarde", Tipo.HORA, { it.entradaMax }, { c, v -> c.copy(entradaMax = v) }),
    SAIDA_ALMOCO_MIN("saida_almoco_min", "Saída a partir de", Tipo.HORA, { it.saidaAlmocoMin }, { c, v -> c.copy(saidaAlmocoMin = v) }),
    SAIDA_ALMOCO_MAX("saida_almoco_max", "Saída até", Tipo.HORA, { it.saidaAlmocoMax }, { c, v -> c.copy(saidaAlmocoMax = v) }),
    ALMOCO_MIN("almoco_min", "Duração mínima", Tipo.MINUTOS, { it.almocoMin }, { c, v -> c.copy(almocoMin = v) }),
    ALMOCO_MAX("almoco_max", "Duração máxima", Tipo.MINUTOS, { it.almocoMax }, { c, v -> c.copy(almocoMax = v) }),
    RETORNO_MIN("retorno_min", "Retorno a partir de", Tipo.HORA, { it.retornoMin }, { c, v -> c.copy(retornoMin = v) }),
    JORNADA_SEMANA("jornada_semana", "Segunda a quinta", Tipo.HORA, { it.jornadaSemana }, { c, v -> c.copy(jornadaSemana = v) }),
    JORNADA_SEXTA("jornada_sexta", "Sexta", Tipo.HORA, { it.jornadaSexta }, { c, v -> c.copy(jornadaSexta = v) }),
    VARIACAO("variacao", "Variação (±)", Tipo.MINUTOS, { it.variacao }, { c, v -> c.copy(variacao = v) }),
    SAIDA_MIN_SEMANA("saida_min_semana", "Segunda a quinta", Tipo.HORA, { it.saidaMinSemana }, { c, v -> c.copy(saidaMinSemana = v) }),
    SAIDA_MIN_SEXTA("saida_min_sexta", "Sexta", Tipo.HORA, { it.saidaMinSexta }, { c, v -> c.copy(saidaMinSexta = v) }),
}

private const val CHAVE_SEXTA_ATIVA = "saida_min_sexta_ativa"

/** Regras de geração. Horários e durações em minutos. */
data class Config(
    val entradaMin: Int = 6 * 60 + 55, // 06:55 (tolerância antecipada de 5 min)
    val entradaMax: Int = 7 * 60 + 15, // 07:15 (tolerância de 15 min da CCT)
    val saidaAlmocoMin: Int = 11 * 60 + 55, // 11:55 (mínimo do RH)
    val saidaAlmocoMax: Int = 12 * 60 + 10, // 12:10
    val almocoMin: Int = 60, // 1h00
    val almocoMax: Int = 70, // 1h10
    val retornoMin: Int = 12 * 60 + 55, // 12:55 (mínimo do RH)
    val jornadaSemana: Int = 9 * 60, // segunda a quinta (e fim de semana)
    val jornadaSexta: Int = 8 * 60,
    val variacao: Int = 10, // ±10 min na jornada
    val saidaMinSemana: Int = 16 * 60 + 55, // 16:55 (mínimo do RH)
    val saidaMinSextaAtiva: Boolean = false,
    val saidaMinSexta: Int = 15 * 60 + 55,
) {
    private fun ehSexta(dia: LocalDate) = dia.dayOfWeek == DayOfWeek.FRIDAY

    fun jornada(dia: LocalDate): Int = if (ehSexta(dia)) jornadaSexta else jornadaSemana

    fun saidaMinima(dia: LocalDate): Int? = when {
        !ehSexta(dia) -> saidaMinSemana
        saidaMinSextaAtiva -> saidaMinSexta
        else -> null
    }

    /** Erros por campo; vazio quando tudo está certo. */
    fun validar(): Map<Campo, String> {
        val erros = linkedMapOf<Campo, String>()
        if (entradaMax < entradaMin) erros[Campo.ENTRADA_MAX] = "Antes do horário mais cedo"
        if (saidaAlmocoMax < saidaAlmocoMin) erros[Campo.SAIDA_ALMOCO_MAX] = "Antes da saída mínima"
        if (saidaAlmocoMin <= entradaMax) erros[Campo.SAIDA_ALMOCO_MIN] = "Precisa ser depois da entrada"
        if (almocoMin !in 0..240) erros[Campo.ALMOCO_MIN] = "Entre 0 e 240 min"
        if (almocoMax !in 0..240) {
            erros[Campo.ALMOCO_MAX] = "Entre 0 e 240 min"
        } else if (almocoMax < almocoMin) {
            erros[Campo.ALMOCO_MAX] = "Menor que a mínima"
        }
        if (jornadaSemana !in 60..16 * 60) erros[Campo.JORNADA_SEMANA] = "Entre 01:00 e 16:00"
        if (jornadaSexta !in 60..16 * 60) erros[Campo.JORNADA_SEXTA] = "Entre 01:00 e 16:00"
        if (variacao !in 0..60) erros[Campo.VARIACAO] = "Entre 0 e 60 min"
        return erros
    }

    /** Mesmo formato JSON da versão em Python. */
    fun paraJson(): String {
        val json = JSONObject()
        Campo.entries.forEach { json.put(it.chave, it.ler(this)) }
        json.put(CHAVE_SEXTA_ATIVA, saidaMinSextaAtiva)
        return json.toString()
    }

    companion object {
        val PADRAO = Config()

        /** Lê a configuração salva; campos ausentes ou inválidos voltam ao padrão. */
        fun deJson(texto: String?): Config {
            if (texto.isNullOrBlank()) return PADRAO
            val json = try {
                JSONObject(texto)
            } catch (e: Exception) {
                return PADRAO
            }
            var cfg = PADRAO
            for (campo in Campo.entries) {
                val valor = json.opt(campo.chave)
                if (valor is Int) cfg = campo.gravar(cfg, valor)
            }
            val ativa = json.opt(CHAVE_SEXTA_ATIVA)
            if (ativa is Boolean) cfg = cfg.copy(saidaMinSextaAtiva = ativa)
            return if (cfg.validar().isEmpty()) cfg else PADRAO
        }
    }
}

data class Horario(val entrada: Int, val saidaAlmoco: Int, val retorno: Int, val saida: Int) {
    val trabalhado: Int get() = (saidaAlmoco - entrada) + (saida - retorno)

    fun lista(): List<Int> = listOf(entrada, saidaAlmoco, retorno, saida)
}

/** Sorteia entre minimo e maximo, nunca abaixo do piso (o piso vence o máximo). */
fun sortear(minimo: Int, maximo: Int, piso: Int? = null, random: Random = Random.Default): Int {
    val de = if (piso != null) maxOf(minimo, piso) else minimo
    return random.nextInt(de, maxOf(de, maximo) + 1)
}

fun gerarHorario(dia: LocalDate, cfg: Config = Config.PADRAO, random: Random = Random.Default): Horario {
    val entrada = sortear(cfg.entradaMin, cfg.entradaMax, random = random)
    val saidaAlmoco = sortear(cfg.saidaAlmocoMin, cfg.saidaAlmocoMax, random = random)
    val almoco = sortear(cfg.almocoMin, cfg.almocoMax, piso = cfg.retornoMin - saidaAlmoco, random = random)
    val retorno = saidaAlmoco + almoco
    // Jornada = manhã + tarde (sem o almoço)
    val manha = saidaAlmoco - entrada
    val base = cfg.jornada(dia)
    // Encurta a variação para baixo quando a saída cairia antes do mínimo
    val piso = cfg.saidaMinima(dia)?.let { it - retorno + manha }
    val jornada = sortear(base - cfg.variacao, base + cfg.variacao, piso, random)
    return Horario(entrada, saidaAlmoco, retorno, retorno + jornada - manha)
}
