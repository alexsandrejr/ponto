package com.ponto.ponto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class RegrasTest {
    private val segunda = LocalDate.of(2026, 10, 5)
    private val sexta = LocalDate.of(2026, 10, 9)
    private val sabado = LocalDate.of(2026, 10, 10)

    private class Faixas(val entrada: IntRange, val saidaAlmoco: IntRange, val retorno: IntRange, val saida: IntRange, val jornada: IntRange)

    private fun faixas(dia: LocalDate, cfg: Config = Config.PADRAO): Faixas {
        val random = Random(42)
        val h = List(100_000) { gerarHorario(dia, cfg, random) }
        fun faixa(f: (Horario) -> Int) = h.minOf(f)..h.maxOf(f)
        return Faixas(faixa { it.entrada }, faixa { it.saidaAlmoco }, faixa { it.retorno }, faixa { it.saida }, faixa { it.trabalhado })
    }

    private fun h(texto: String) = lerHhmm(texto)!!

    @Test
    fun `padrao de segunda a quinta segue as regras do RH`() {
        val f = faixas(segunda)
        assertEquals(h("06:55")..h("07:15"), f.entrada)
        assertEquals(h("11:55")..h("12:10"), f.saidaAlmoco)
        assertEquals(h("12:55")..h("13:20"), f.retorno)
        assertEquals(h("16:55")..h("17:10"), f.saida)
        // A faixa da saída vence a jornada: entrada 07:15 + almoço 1h10 só cabe 8h45 até 17:10
        assertEquals(h("08:45")..h("09:10"), f.jornada)
    }

    @Test
    fun `padrao de sexta tem 8h e saida entre 15h55 e 16h10`() {
        val f = faixas(sexta)
        assertEquals(h("15:55")..h("16:10"), f.saida)
        assertEquals(h("07:45")..h("08:10"), f.jornada)
    }

    @Test
    fun `fim de semana usa a regra de segunda a quinta`() {
        assertEquals(h("16:55")..h("17:10"), faixas(sabado).saida)
    }

    @Test
    fun `com faixa de saida larga a jornada fica dentro da variacao`() {
        val cfg = Config.PADRAO.copy(saidaMaxSemana = h("18:00"), saidaMinSexta = h("15:00"), saidaMaxSexta = h("17:00"))
        assertEquals(h("08:50")..h("09:10"), faixas(segunda, cfg).jornada)
        assertEquals(h("07:50")..h("08:10"), faixas(sexta, cfg).jornada)
    }

    @Test
    fun `retorno minimo vale mesmo com saida do almoco mais cedo`() {
        val cfg = Config.PADRAO.copy(saidaAlmocoMin = h("11:40"))
        assertEquals(h("12:55"), faixas(segunda, cfg).retorno.first)
    }

    @Test
    fun `lerHhmm aceita os formatos esperados`() {
        assertEquals(425, lerHhmm("7:05"))
        assertEquals(425, lerHhmm("0705"))
        assertEquals(425, lerHhmm("705"))
        assertEquals(1015, lerHhmm(" 16:55 "))
        listOf("25:00", "7", "abc", "7:5", "", "12:60", "123:00").forEach { assertNull(it, lerHhmm(it)) }
    }

    @Test
    fun `formatacao`() {
        assertEquals("07:05", hhmm(425))
        assertEquals("8h51", duracao(531))
        assertEquals("9h", horas(540))
        assertEquals("8h30", horas(510))
        assertEquals("Sexta", nomeDoDia(sexta))
    }

    @Test
    fun `validacao aponta os campos errados`() {
        assertTrue(Config.PADRAO.validar().isEmpty())
        assertEquals(setOf(Campo.ENTRADA_MAX), Config.PADRAO.copy(entradaMax = h("06:00")).validar().keys)
        assertEquals(
            setOf(Campo.ALMOCO_MAX, Campo.JORNADA_SEXTA, Campo.VARIACAO),
            Config.PADRAO.copy(almocoMax = 30, variacao = 90, jornadaSexta = 0).validar().keys,
        )
        assertEquals(
            setOf(Campo.SAIDA_MAX_SEMANA, Campo.SAIDA_MAX_SEXTA),
            Config.PADRAO.copy(saidaMaxSemana = h("16:55"), saidaMaxSexta = h("15:00")).validar().keys,
        )
    }

    @Test
    fun `json da versao em Python e lido corretamente`() {
        // Gerado por Config.para_json() do regras.py
        val python = """{"entrada_min": 415, "entrada_max": 435, "saida_almoco_min": 715, "saida_almoco_max": 730,
            "almoco_min": 60, "almoco_max": 70, "retorno_min": 775, "jornada_semana": 510, "jornada_sexta": 480,
            "variacao": 10, "saida_min_semana": 1015, "saida_min_sexta_ativa": true, "saida_min_sexta": 955}"""
        val cfg = Config.deJson(python)
        assertEquals(510, cfg.jornadaSemana)
        assertEquals(955, cfg.saidaMinSexta)
        // Campos que não existiam na versão em Python ficam com o padrão
        assertEquals(Config.PADRAO.saidaMaxSemana, cfg.saidaMaxSemana)
        assertEquals(cfg, Config.deJson(cfg.paraJson()))
    }

    @Test
    fun `json invalido volta ao padrao`() {
        assertEquals(Config.PADRAO, Config.deJson(null))
        assertEquals(Config.PADRAO, Config.deJson("lixo"))
        assertEquals(Config.PADRAO, Config.deJson("""{"variacao": "x"}"""))
        assertEquals(5, Config.deJson("""{"variacao": 5}""").variacao)
        // Valores que não passam na validação também voltam ao padrão
        assertEquals(Config.PADRAO, Config.deJson("""{"entrada_max": 300}"""))
    }
}
