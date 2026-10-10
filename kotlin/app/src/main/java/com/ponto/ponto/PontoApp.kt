package com.ponto.ponto

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Estado do app e navegação entre a tela principal e a de configurações. */
@Composable
fun PontoApp(store: ConfigStore) {
    var cfg by remember { mutableStateOf(store.carregar()) }
    var emConfig by rememberSaveable { mutableStateOf(false) }
    // null = data automática (sempre o dia de hoje); senão, o dia escolhido (epochDay)
    var dataEscolhida by rememberSaveable { mutableStateOf<Long?>(null) }
    var horario by rememberSaveable { mutableStateOf<IntArray?>(null) }
    var hoje by remember { mutableStateOf(LocalDate.now()) }

    // Se o app ficou aberto de um dia para o outro, a data automática acompanha
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hoje = LocalDate.now() }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun avisar(mensagem: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(mensagem)
        }
    }

    fun aplicar(novo: Config) {
        cfg = novo
        store.salvar(novo)
        // Os horários na tela foram gerados com as regras antigas
        horario = null
    }

    val dia = dataEscolhida?.let(LocalDate::ofEpochDay) ?: hoje

    if (emConfig) {
        BackHandler { emConfig = false }
        TelaConfig(
            cfg = cfg,
            snackbar = snackbar,
            onSalvar = {
                aplicar(it)
                emConfig = false
                avisar("Configurações salvas")
            },
            onRestaurar = {
                aplicar(Config.PADRAO)
                avisar("Padrões restaurados")
            },
            onErro = { avisar("Corrija os campos destacados") },
            onVoltar = { emConfig = false },
        )
    } else {
        TelaPrincipal(
            dia = dia,
            automatica = dataEscolhida == null,
            cfg = cfg,
            horario = horario,
            snackbar = snackbar,
            onAlterarData = { nova ->
                // Escolher o próprio dia de hoje volta para o modo automático
                dataEscolhida = nova?.takeIf { it != hoje }?.toEpochDay()
                horario = null
            },
            onGerar = {
                hoje = LocalDate.now()
                val diaAtual = dataEscolhida?.let(LocalDate::ofEpochDay) ?: hoje
                horario = gerarHorario(diaAtual, cfg).lista().toIntArray()
            },
            onConfig = { emConfig = true },
        )
    }
}
