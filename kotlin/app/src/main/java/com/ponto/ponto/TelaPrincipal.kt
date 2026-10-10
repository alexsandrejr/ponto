package com.ponto.ponto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private class Etapa(val rotulo: String, val icone: ImageVector, val cor: Color)

private val ETAPAS = listOf(
    Etapa("Entrada", Icons.AutoMirrored.Filled.Login, Color(0xFF2E7D32)),
    Etapa("Saída almoço", Icons.Filled.Restaurant, Color(0xFFEF6C00)),
    Etapa("Retorno", Icons.AutoMirrored.Filled.KeyboardReturn, Color(0xFF1565C0)),
    Etapa("Saída", Icons.AutoMirrored.Filled.Logout, Color(0xFFC62828)),
)

private val FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// "sex., 9 de out.": o título padrão do calendário em português não cabe e fica cortado
private val FORMATO_TITULO = DateTimeFormatter.ofPattern("EEE, d 'de' MMM", Locale.forLanguageTag("pt-BR"))
private const val MS_POR_DIA = 24L * 60 * 60 * 1000

/** Fundo branco arredondado com sombra, usado nos cartões das duas telas. */
fun Modifier.cartao(): Modifier = this
    .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.15f), spotColor = Color.Black.copy(alpha = 0.15f))
    .background(Color.White, RoundedCornerShape(24.dp))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPrincipal(
    dia: LocalDate,
    automatica: Boolean,
    cfg: Config,
    horario: IntArray?,
    snackbar: SnackbarHostState,
    onAlterarData: (LocalDate?) -> Unit,
    onGerar: () -> Unit,
    onConfig: () -> Unit,
) {
    var mostrarCalendario by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Fundo, FundoBaixo)))) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onConfig) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configurações", tint = Cor)
                    }
                }

                // Centraliza o conteúdo e, em telas pequenas, deixa rolar
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Cabecalho(dia, automatica, cfg, onAbrirCalendario = { mostrarCalendario = true }, onHoje = { onAlterarData(null) })
                        Spacer(Modifier.height(28.dp))
                        Button(
                            onClick = onGerar,
                            modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Cor),
                        ) {
                            Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Gerar horário", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(28.dp))
                        CartaoHorario(horario)
                    }
                }

                Text("Criado por Alexsandre JR", fontSize = 12.sp, color = Cinza500)
            }
        }
    }

    if (mostrarCalendario) {
        val estado = rememberDatePickerState(
            // O calendário trabalha com meia-noite em UTC
            initialSelectedDateMillis = dia.toEpochDay() * MS_POR_DIA,
            yearRange = 2020..2035,
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { onAlterarData(LocalDate.ofEpochDay(it / MS_POR_DIA)) }
                    mostrarCalendario = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(
                state = estado,
                title = { Text("Escolha a data", Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) },
                headline = {
                    val escolhido = estado.selectedDateMillis?.let { LocalDate.ofEpochDay(it / MS_POR_DIA) }
                    Text(
                        escolhido?.format(FORMATO_TITULO) ?: "Nenhuma data",
                        Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
private fun Cabecalho(
    dia: LocalDate,
    automatica: Boolean,
    cfg: Config,
    onAbrirCalendario: () -> Unit,
    onHoje: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Schedule, contentDescription = null, tint = Cor, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(4.dp))
        Text("Gerador de Ponto", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Cinza900)
        Spacer(Modifier.height(12.dp))
        Surface(onClick = onAbrirCalendario, shape = RoundedCornerShape(20.dp), color = Color.White) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Cor, modifier = Modifier.size(20.dp))
                Text(
                    "${nomeDoDia(dia)}, ${dia.format(FORMATO_DATA)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Cor,
                )
                Icon(Icons.Filled.Edit, contentDescription = "Alterar data", tint = Cinza400, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Jornada de ${horas(cfg.jornada(dia))}" + if (automatica) " · hoje" else "",
            fontSize = 13.sp,
            color = Cinza600,
        )
        if (!automatica) {
            TextButton(onClick = onHoje) {
                Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Voltar para hoje")
            }
        }
    }
}

@Composable
private fun CartaoHorario(horario: IntArray?) {
    Column(
        Modifier.widthIn(max = 400.dp).fillMaxWidth().cartao().padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ETAPAS.forEachIndexed { i, etapa ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).background(etapa.cor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(etapa.icone, contentDescription = null, tint = etapa.cor, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(etapa.rotulo, fontSize = 16.sp, color = Cinza700, modifier = Modifier.weight(1f))
                Text(
                    horario?.let { hhmm(it[i]) } ?: "--:--",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Cinza900,
                )
            }
        }
        HorizontalDivider(color = Cinza100)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total trabalhado", fontSize = 15.sp, color = Cinza600)
            Text(
                horario?.let { duracao((it[1] - it[0]) + (it[3] - it[2])) } ?: "--",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Cor,
            )
        }
    }
}
