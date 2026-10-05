import random
from datetime import date, datetime

import flet as ft

# Horários em minutos desde 00:00
ENTRADA_MIN = 6 * 60 + 55  # 06:55 (tolerância antecipada de 5 min)
ENTRADA_MAX = 7 * 60 + 15  # 07:15 (tolerância de 15 min da CCT)
SAIDA_ALMOCO_MIN = 11 * 60 + 55  # 11:55 (mínimo do RH)
SAIDA_ALMOCO_MAX = 12 * 60 + 10  # 12:10
ALMOCO_MIN = 60  # 1h00 -> retorno nunca antes de 12:55 (mínimo do RH)
ALMOCO_MAX = 70  # 1h10
SAIDA_MIN = 16 * 60 + 55  # 16:55 (mínimo do RH, exceto sexta)
VARIACAO = 10  # ±10 min

DIAS = ["Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"]


def eh_sexta(dia: date) -> bool:
    return dia.weekday() == 4


def jornada_do_dia(dia: date) -> int:
    """Minutos trabalhados: 8h na sexta, 9h nos demais dias."""
    return 8 * 60 if eh_sexta(dia) else 9 * 60


def gerar_horario(dia: date) -> tuple[int, int, int, int]:
    entrada = random.randint(ENTRADA_MIN, ENTRADA_MAX)
    saida_almoco = random.randint(SAIDA_ALMOCO_MIN, SAIDA_ALMOCO_MAX)
    retorno = saida_almoco + random.randint(ALMOCO_MIN, ALMOCO_MAX)
    # Jornada = manhã + tarde (sem o almoço)
    manha = saida_almoco - entrada
    jornada_min = jornada_do_dia(dia) - VARIACAO
    if not eh_sexta(dia):
        # Encurta a variação para baixo quando a saída cairia antes de 16:55
        jornada_min = max(jornada_min, SAIDA_MIN - retorno + manha)
    jornada = random.randint(jornada_min, jornada_do_dia(dia) + VARIACAO)
    saida = retorno + jornada - manha
    return entrada, saida_almoco, retorno, saida


def hhmm(minutos: int) -> str:
    return f"{minutos // 60:02d}:{minutos % 60:02d}"


def duracao(minutos: int) -> str:
    return f"{minutos // 60}h{minutos % 60:02d}"


COR = "#3949AB"
FUNDO = ["#E8EAF6", "#C5CAE9"]
ETAPAS = [
    ("Entrada", ft.Icons.LOGIN, "#2E7D32"),
    ("Saída almoço", ft.Icons.RESTAURANT, "#EF6C00"),
    ("Retorno", ft.Icons.KEYBOARD_RETURN, "#1565C0"),
    ("Saída", ft.Icons.LOGOUT, "#C62828"),
]


def main(page: ft.Page):
    page.title = "Ponto"
    page.padding = 0
    page.theme_mode = ft.ThemeMode.LIGHT
    page.theme = ft.Theme(color_scheme_seed=COR)
    pt_br = ft.Locale("pt", "BR")
    page.locale_configuration = ft.LocaleConfiguration(supported_locales=[pt_br], current_locale=pt_br)

    # None = data automática (sempre o dia de hoje)
    data_escolhida = None

    def dia_atual() -> date:
        return data_escolhida or date.today()

    texto_data = ft.Text(size=15, weight=ft.FontWeight.W_600, color=COR)
    texto_jornada = ft.Text(size=13, color=ft.Colors.BLUE_GREY_600)
    total = ft.Text("--", size=16, weight=ft.FontWeight.BOLD, color=COR)
    valores = []

    def linha(rotulo, icone, cor):
        valor = ft.Text("--:--", size=24, weight=ft.FontWeight.BOLD, color=ft.Colors.BLUE_GREY_900)
        valores.append(valor)
        return ft.Row(
            [
                ft.Container(
                    ft.Icon(icone, color=cor, size=22),
                    bgcolor=ft.Colors.with_opacity(0.12, cor),
                    border_radius=12,
                    padding=10,
                ),
                ft.Text(rotulo, size=16, color=ft.Colors.BLUE_GREY_700, expand=True),
                valor,
            ],
            spacing=14,
            vertical_alignment=ft.CrossAxisAlignment.CENTER,
        )

    def atualizar_data():
        dia = dia_atual()
        texto_data.value = f"{DIAS[dia.weekday()]}, {dia:%d/%m/%Y}"
        texto_jornada.value = f"Jornada de {jornada_do_dia(dia) // 60}h"
        if data_escolhida is None:
            texto_jornada.value += " · hoje"
        botao_hoje.visible = data_escolhida is not None

    def limpar_horarios():
        for valor in valores:
            valor.value = "--:--"
        total.value = "--"

    def definir_data(nova: date | None):
        nonlocal data_escolhida
        # Escolher o próprio dia de hoje volta para o modo automático
        data_escolhida = None if nova == date.today() else nova
        limpar_horarios()
        atualizar_data()
        page.update()

    def on_data_escolhida(e):
        valor = e.control.value
        if isinstance(valor, datetime):
            # O calendário devolve meia-noite local em UTC; converte de volta
            valor = (valor.astimezone() if valor.tzinfo else valor).date()
        definir_data(valor)

    seletor_data = ft.DatePicker(
        first_date=datetime(2020, 1, 1),
        last_date=datetime(2035, 12, 31),
        help_text="Escolha a data",
        cancel_text="Cancelar",
        confirm_text="OK",
        on_change=on_data_escolhida,
    )

    def abrir_calendario(e):
        seletor_data.value = dia_atual()
        page.show_dialog(seletor_data)

    def on_gerar(e):
        atualizar_data()
        entrada, saida_almoco, retorno, saida = h = gerar_horario(dia_atual())
        for valor, minutos in zip(valores, h):
            valor.value = hhmm(minutos)
        total.value = duracao((saida_almoco - entrada) + (saida - retorno))
        page.update()

    seletor = ft.Container(
        ft.Row(
            [
                ft.Icon(ft.Icons.CALENDAR_MONTH, size=20, color=COR),
                texto_data,
                ft.Icon(ft.Icons.EDIT, size=16, color=ft.Colors.BLUE_GREY_400),
            ],
            tight=True,
            spacing=8,
        ),
        on_click=abrir_calendario,
        tooltip="Alterar data",
        bgcolor=ft.Colors.WHITE,
        border_radius=20,
        padding=ft.Padding.symmetric(horizontal=16, vertical=8),
        ink=True,
    )

    botao_hoje = ft.TextButton(
        "Voltar para hoje",
        icon=ft.Icons.TODAY,
        on_click=lambda e: definir_data(None),
    )

    atualizar_data()

    cabecalho = ft.Column(
        [
            ft.Icon(ft.Icons.SCHEDULE, size=56, color=COR),
            ft.Text("Gerador de Ponto", size=28, weight=ft.FontWeight.BOLD, color=ft.Colors.BLUE_GREY_900),
            ft.Container(height=4),
            seletor,
            texto_jornada,
            botao_hoje,
        ],
        horizontal_alignment=ft.CrossAxisAlignment.CENTER,
        spacing=4,
    )

    botao = ft.FilledButton(
        "Gerar horário",
        icon=ft.Icons.AUTORENEW,
        on_click=on_gerar,
        width=400,
        height=56,
        style=ft.ButtonStyle(
            bgcolor=COR,
            shape=ft.RoundedRectangleBorder(radius=16),
            text_style=ft.TextStyle(size=17, weight=ft.FontWeight.W_600),
        ),
    )

    cartao = ft.Container(
        ft.Column(
            [
                *(linha(*etapa) for etapa in ETAPAS),
                ft.Divider(height=8, color=ft.Colors.BLUE_GREY_100),
                ft.Row(
                    [ft.Text("Total trabalhado", size=15, color=ft.Colors.BLUE_GREY_600), total],
                    alignment=ft.MainAxisAlignment.SPACE_BETWEEN,
                ),
            ],
            spacing=14,
        ),
        width=400,
        padding=22,
        bgcolor=ft.Colors.WHITE,
        border_radius=24,
        shadow=ft.BoxShadow(
            blur_radius=30,
            color=ft.Colors.with_opacity(0.15, ft.Colors.BLACK),
            offset=ft.Offset(0, 10),
        ),
    )

    conteudo = ft.Column(
        [cabecalho, botao, cartao],
        horizontal_alignment=ft.CrossAxisAlignment.CENTER,
        alignment=ft.MainAxisAlignment.CENTER,
        spacing=28,
    )

    rodape = ft.Text("Criado por Alexsandre JR", size=12, color=ft.Colors.BLUE_GREY_500)

    page.add(
        ft.Container(
            ft.SafeArea(
                ft.Column(
                    [ft.Container(conteudo, expand=True, alignment=ft.Alignment.CENTER), rodape],
                    horizontal_alignment=ft.CrossAxisAlignment.CENTER,
                ),
            ),
            expand=True,
            padding=20,
            gradient=ft.LinearGradient(
                begin=ft.Alignment.TOP_CENTER,
                end=ft.Alignment.BOTTOM_CENTER,
                colors=FUNDO,
            ),
        )
    )


if __name__ == "__main__":
    ft.run(main)
