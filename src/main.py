from dataclasses import replace
from datetime import date, datetime

import flet as ft

from regras import DIAS, PADRAO, Config, duracao, gerar_horario, hhmm, ler_hhmm

CHAVE_CONFIG = "ponto.config"

COR = "#3949AB"
FUNDO = ["#E8EAF6", "#C5CAE9"]
ETAPAS = [
    ("Entrada", ft.Icons.LOGIN, "#2E7D32"),
    ("Saída almoço", ft.Icons.RESTAURANT, "#EF6C00"),
    ("Retorno", ft.Icons.KEYBOARD_RETURN, "#1565C0"),
    ("Saída", ft.Icons.LOGOUT, "#C62828"),
]

# Campos da tela de configurações: (seção, [(campo do Config, rótulo, tipo)])
# tipo "hora" é digitado como HH:MM; "min" é um número de minutos
SECOES = [
    ("Entrada", ft.Icons.LOGIN, [
        ("entrada_min", "Mais cedo", "hora"),
        ("entrada_max", "Mais tarde", "hora"),
    ]),
    ("Almoço", ft.Icons.RESTAURANT, [
        ("saida_almoco_min", "Saída a partir de", "hora"),
        ("saida_almoco_max", "Saída até", "hora"),
        ("almoco_min", "Duração mínima", "min"),
        ("almoco_max", "Duração máxima", "min"),
        ("retorno_min", "Retorno a partir de", "hora"),
    ]),
    ("Jornada", ft.Icons.WORK_OUTLINE, [
        ("jornada_semana", "Segunda a quinta", "hora"),
        ("jornada_sexta", "Sexta", "hora"),
        ("variacao", "Variação (±)", "min"),
    ]),
    ("Saída mínima", ft.Icons.LOGOUT, [
        ("saida_min_semana", "Segunda a quinta", "hora"),
    ]),
]


def horas(minutos: int) -> str:
    """9h, 8h30..."""
    return f"{minutos // 60}h" if minutos % 60 == 0 else duracao(minutos)


def cartao_branco(conteudo: ft.Control, **kwargs) -> ft.Container:
    return ft.Container(
        conteudo,
        bgcolor=ft.Colors.WHITE,
        border_radius=24,
        shadow=ft.BoxShadow(
            blur_radius=30,
            color=ft.Colors.with_opacity(0.15, ft.Colors.BLACK),
            offset=ft.Offset(0, 10),
        ),
        **kwargs,
    )


async def main(page: ft.Page):
    page.title = "Ponto"
    page.theme_mode = ft.ThemeMode.LIGHT
    page.theme = ft.Theme(color_scheme_seed=COR)
    pt_br = ft.Locale("pt", "BR")
    page.locale_configuration = ft.LocaleConfiguration(supported_locales=[pt_br], current_locale=pt_br)

    prefs = ft.SharedPreferences()
    try:
        cfg = Config.de_json(await prefs.get(CHAVE_CONFIG))
    except Exception:
        cfg = PADRAO

    # None = data automática (sempre o dia de hoje)
    data_escolhida = None

    def dia_atual() -> date:
        return data_escolhida or date.today()

    def avisar(mensagem: str):
        page.show_dialog(ft.SnackBar(ft.Text(mensagem)))

    async def salvar_config(novo: Config):
        nonlocal cfg
        cfg = novo
        await prefs.set(CHAVE_CONFIG, novo.para_json())
        # Os horários na tela foram gerados com as regras antigas
        limpar_horarios()
        atualizar_data()

    # ---------- Tela principal ----------

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
        texto_jornada.value = f"Jornada de {horas(cfg.jornada(dia))}"
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
        entrada, saida_almoco, retorno, saida = h = gerar_horario(dia_atual(), cfg)
        for valor, minutos in zip(valores, h):
            valor.value = hhmm(minutos)
        total.value = duracao((saida_almoco - entrada) + (saida - retorno))
        page.update()

    async def abrir_config(e):
        await page.push_route("/config")

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

    cartao = cartao_branco(
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
    )

    conteudo = ft.Column(
        [cabecalho, botao, cartao],
        horizontal_alignment=ft.CrossAxisAlignment.CENTER,
        alignment=ft.MainAxisAlignment.CENTER,
        spacing=28,
    )

    rodape = ft.Text("Criado por Alexsandre JR", size=12, color=ft.Colors.BLUE_GREY_500)

    topo = ft.Row(
        [
            ft.IconButton(
                ft.Icons.SETTINGS,
                icon_color=COR,
                tooltip="Configurações",
                on_click=abrir_config,
            )
        ],
        alignment=ft.MainAxisAlignment.END,
    )

    tela_principal = ft.View(
        route="/",
        padding=0,
        controls=[
            ft.Container(
                ft.SafeArea(
                    ft.Column(
                        [topo, ft.Container(conteudo, expand=True, alignment=ft.Alignment.CENTER), rodape],
                        horizontal_alignment=ft.CrossAxisAlignment.CENTER,
                    ),
                ),
                expand=True,
                padding=ft.Padding.only(left=20, right=20, top=8, bottom=20),
                gradient=ft.LinearGradient(
                    begin=ft.Alignment.TOP_CENTER,
                    end=ft.Alignment.BOTTOM_CENTER,
                    colors=FUNDO,
                ),
            )
        ],
    )

    # ---------- Tela de configurações ----------

    def tela_config() -> ft.View:
        campos: dict[str, ft.TextField] = {}

        def campo(nome, rotulo, tipo) -> ft.TextField:
            valor = getattr(cfg, nome)
            campos[nome] = ft.TextField(
                label=rotulo,
                value=hhmm(valor) if tipo == "hora" else str(valor),
                hint_text="HH:MM" if tipo == "hora" else None,
                suffix="min" if tipo == "min" else None,
                keyboard_type=ft.KeyboardType.DATETIME if tipo == "hora" else ft.KeyboardType.NUMBER,
                data=tipo,
                dense=True,
                border=ft.OutlineInputBorder(border_radius=12),
                expand=True,
            )
            return campos[nome]

        def secao(titulo, icone, linhas: list[ft.Control]) -> ft.Container:
            return cartao_branco(
                ft.Column(
                    [
                        ft.Row(
                            [
                                ft.Icon(icone, size=20, color=COR),
                                ft.Text(titulo, size=16, weight=ft.FontWeight.W_600, color=ft.Colors.BLUE_GREY_900),
                            ],
                            spacing=8,
                        ),
                        *linhas,
                    ],
                    spacing=14,
                ),
                padding=18,
            )

        def em_pares(controles: list[ft.Control]) -> list[ft.Row]:
            if len(controles) % 2:
                # Campo sozinho na linha fica com meia largura, como os outros
                controles = controles + [ft.Container(expand=True)]
            return [ft.Row(controles[i : i + 2], spacing=12) for i in range(0, len(controles), 2)]

        secoes = [
            secao(titulo, icone, em_pares([campo(*c) for c in itens]))
            for titulo, icone, itens in SECOES
        ]

        # Saída mínima da sexta: opcional, desligada por padrão
        campo_sexta = campo("saida_min_sexta", "Sexta", "hora")

        def on_switch_sexta(e):
            campo_sexta.disabled = not e.control.value
            page.update()

        switch_sexta = ft.Switch(
            label="Usar saída mínima na sexta",
            value=cfg.saida_min_sexta_ativa,
            active_color=COR,
            on_change=on_switch_sexta,
        )
        campo_sexta.disabled = not switch_sexta.value
        secoes[-1].content.controls += [switch_sexta, *em_pares([campo_sexta])]

        def ler_formulario() -> Config | None:
            valores, erros = {}, {}
            for nome, tf in campos.items():
                tf.error = None
                if tf.disabled:
                    valores[nome] = getattr(cfg, nome)
                    continue
                texto = (tf.value or "").strip()
                try:
                    valores[nome] = ler_hhmm(texto) if tf.data == "hora" else int(texto)
                except ValueError:
                    erros[nome] = "Use HH:MM" if tf.data == "hora" else "Número inválido"
            if not erros:
                novo = replace(cfg, saida_min_sexta_ativa=switch_sexta.value, **valores)
                erros = novo.validar()
            for nome, mensagem in erros.items():
                campos[nome].error = mensagem
            return None if erros else novo

        async def on_salvar(e):
            novo = ler_formulario()
            if novo is None:
                page.update()
                avisar("Corrija os campos destacados")
                return
            await salvar_config(novo)
            await page.push_route("/")
            avisar("Configurações salvas")

        async def restaurar(e):
            page.pop_dialog()
            await salvar_config(PADRAO)
            for nome, tf in campos.items():
                valor = getattr(PADRAO, nome)
                tf.value = hhmm(valor) if tf.data == "hora" else str(valor)
                tf.error = None
            switch_sexta.value = PADRAO.saida_min_sexta_ativa
            campo_sexta.disabled = not switch_sexta.value
            page.update()
            avisar("Padrões restaurados")

        def on_restaurar(e):
            page.show_dialog(
                ft.AlertDialog(
                    title=ft.Text("Restaurar padrões?"),
                    content=ft.Text("Todas as configurações voltam para os valores originais do app."),
                    actions=[
                        ft.TextButton("Cancelar", on_click=lambda e: page.pop_dialog()),
                        ft.TextButton("Restaurar", on_click=restaurar),
                    ],
                )
            )

        botoes = ft.Column(
            [
                ft.FilledButton(
                    "Salvar",
                    icon=ft.Icons.SAVE,
                    on_click=on_salvar,
                    height=52,
                    style=ft.ButtonStyle(bgcolor=COR, shape=ft.RoundedRectangleBorder(radius=16)),
                ),
                ft.OutlinedButton(
                    "Restaurar padrões",
                    icon=ft.Icons.RESTORE,
                    on_click=on_restaurar,
                    height=52,
                    style=ft.ButtonStyle(color=ft.Colors.RED_700, shape=ft.RoundedRectangleBorder(radius=16)),
                ),
            ],
            horizontal_alignment=ft.CrossAxisAlignment.STRETCH,
            spacing=10,
        )

        return ft.View(
            route="/config",
            appbar=ft.AppBar(title=ft.Text("Configurações"), bgcolor=FUNDO[0]),
            bgcolor=FUNDO[0],
            scroll=ft.ScrollMode.AUTO,
            padding=16,
            controls=[
                ft.Text(
                    "Os valores ficam salvos neste celular. Horários no formato HH:MM.",
                    size=13,
                    color=ft.Colors.BLUE_GREY_600,
                ),
                *secoes,
                botoes,
                ft.Container(height=8),
            ],
            spacing=16,
        )

    # ---------- Navegação ----------

    def on_rota(e=None):
        page.views.clear()
        page.views.append(tela_principal)
        if page.route == "/config":
            page.views.append(tela_config())
        page.update()

    async def on_voltar(e):
        page.views.pop()
        await page.push_route(page.views[-1].route)

    page.on_route_change = on_rota
    page.on_view_pop = on_voltar
    on_rota()


if __name__ == "__main__":
    ft.run(main)
