import random
from datetime import date

import flet as ft

# Horários em minutos desde 00:00
ENTRADA_MIN = 6 * 60 + 55  # 06:55 (tolerância antecipada de 5 min)
ENTRADA_MAX = 7 * 60 + 15  # 07:15 (tolerância de 15 min da CCT)
SAIDA_ALMOCO_BASE = 12 * 60  # 12:00
ALMOCO_MIN = 60  # 1h00
ALMOCO_MAX = 70  # 1h10
VARIACAO = 10  # ±10 min

DIAS = ["Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"]


def jornada_do_dia(dia: date) -> int:
    """Minutos trabalhados: 8h na sexta, 9h nos demais dias."""
    return 8 * 60 if dia.weekday() == 4 else 9 * 60


def gerar_horario(dia: date) -> tuple[int, int, int, int]:
    entrada = random.randint(ENTRADA_MIN, ENTRADA_MAX)
    saida_almoco = SAIDA_ALMOCO_BASE + random.randint(-VARIACAO, VARIACAO)
    retorno = saida_almoco + random.randint(ALMOCO_MIN, ALMOCO_MAX)
    jornada = jornada_do_dia(dia) + random.randint(-VARIACAO, VARIACAO)
    # Jornada = manhã + tarde (sem o almoço)
    saida = retorno + jornada - (saida_almoco - entrada)
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

    subtitulo = ft.Text(size=14, color=ft.Colors.BLUE_GREY_600)
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

    def atualizar_subtitulo(hoje: date):
        subtitulo.value = (
            f"{DIAS[hoje.weekday()]}, {hoje:%d/%m/%Y} · jornada de {jornada_do_dia(hoje) // 60}h"
        )

    def on_gerar(e):
        hoje = date.today()
        entrada, saida_almoco, retorno, saida = h = gerar_horario(hoje)
        for valor, minutos in zip(valores, h):
            valor.value = hhmm(minutos)
        total.value = duracao((saida_almoco - entrada) + (saida - retorno))
        atualizar_subtitulo(hoje)
        page.update()

    atualizar_subtitulo(date.today())

    cabecalho = ft.Column(
        [
            ft.Icon(ft.Icons.SCHEDULE, size=56, color=COR),
            ft.Text("Gerador de Ponto", size=28, weight=ft.FontWeight.BOLD, color=ft.Colors.BLUE_GREY_900),
            subtitulo,
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
