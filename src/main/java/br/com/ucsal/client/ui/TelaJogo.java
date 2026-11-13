package br.com.ucsal.client.ui;

import br.com.ucsal.client.OuvinteDoJogo;
import br.com.ucsal.common.exception.JogadaInvalidaException;
import br.com.ucsal.common.exception.NaoESuaVezException;
import br.com.ucsal.common.exception.PartidaIndisponivelException;
import br.com.ucsal.common.model.MotivoFim;
import br.com.ucsal.common.model.Posicao;
import br.com.ucsal.common.model.ResultadoTiro;
import br.com.ucsal.common.model.TipoNavio;
import br.com.ucsal.common.remote.BatalhaNavalServer;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;

/**
 * Tela 4: a batalha.
 *
 *   [ Seu mar ]            [ Mar inimigo ]
 *   seus navios +          onde VOCÊ atira
 *   tiros que você levou   (clicável só na sua vez)
 *
 * Chamada remota usada: fazerJogada(id, posicao) -> ResultadoTiro
 * Avisos recebidos (callback): suaVez, tiroRecebido, fimDeJogo
 */
public class TelaJogo implements OuvinteDoJogo {

    private static final int N = BatalhaNavalApp.TAMANHO_TABULEIRO;
    private static final int TOTAL_NAVIOS = TipoNavio.values().length;

    private final BatalhaNavalApp app;
    private final String nomeAdversario;
    private final TipoNavio[][] meusNavios;

    private final StackPane raiz = new StackPane();
    private final GradeTabuleiro meuMar = new GradeTabuleiro(28);
    private final GradeTabuleiro marInimigo = new GradeTabuleiro(28);

    private final Label labelStatus = new Label();
    private final Label labelPlacar = new Label();
    private final VBox historico = new VBox(4);

    // Painel de fim de jogo (fica por cima de tudo, escondido até o fim)
    private final StackPane camadaFim = new StackPane();
    private final Label tituloFim = new Label();
    private final Label textoFim = new Label();

    // ---- Estado (só mexido na thread do JavaFX) ----
    private boolean minhaVez;
    private boolean aguardandoResposta; // um tiro meu está "em voo"
    private boolean terminou;
    private int inimigosAfundados;
    private int meusAfundados;

    public TelaJogo(BatalhaNavalApp app, String nomeAdversario, TipoNavio[][] meusNavios) {
        this.app = app;
        this.nomeAdversario = nomeAdversario;
        this.meusNavios = meusNavios;

        montarLayout();
        desenharMinhaFrota();
        atualizarPlacar();
        mostrarVezDoAdversario();

        marInimigo.setAoClicar(this::atirar);

        // A partir de agora, os avisos do servidor vêm para esta tela.
        // (O suaVez() de quem começa chega logo depois do partidaIniciada.)
        app.ouvirAvisos(this);
    }

    public StackPane getRaiz() {
        return raiz;
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private void montarLayout() {
        Label titulo = new Label(app.getNomeJogador() + "  vs  " + nomeAdversario);
        titulo.getStyleClass().add("titulo");

        labelStatus.getStyleClass().add("status");
        labelStatus.setMaxWidth(Double.MAX_VALUE);
        labelStatus.setAlignment(Pos.CENTER);

        labelPlacar.getStyleClass().add("subtitulo");

        VBox cabecalho = new VBox(10, titulo, labelStatus, labelPlacar);
        cabecalho.setAlignment(Pos.CENTER);
        cabecalho.setMaxWidth(640);

        VBox cartaoMeu = cartaoComGrade("Seu mar", meuMar);
        VBox cartaoInimigo = cartaoComGrade("Mar de " + nomeAdversario, marInimigo);

        HBox grades = new HBox(24, cartaoMeu, cartaoInimigo);
        grades.setAlignment(Pos.CENTER);

        Label tituloHistorico = new Label("Últimos lances");
        tituloHistorico.getStyleClass().add("rotulo");
        VBox caixaHistorico = new VBox(6, tituloHistorico, historico);
        caixaHistorico.setAlignment(Pos.CENTER);
        historico.setAlignment(Pos.CENTER);

        VBox tudo = new VBox(18, cabecalho, grades, caixaHistorico);
        tudo.setAlignment(Pos.CENTER);

        montarCamadaFim();

        raiz.getStyleClass().add("fundo-oceano");
        raiz.getChildren().addAll(tudo, camadaFim); // camadaFim fica POR CIMA
    }

    private VBox cartaoComGrade(String titulo, GradeTabuleiro grade) {
        Label label = new Label(titulo);
        label.getStyleClass().add("titulo-secao");
        VBox cartao = new VBox(12, label, grade.getNo());
        cartao.setAlignment(Pos.CENTER);
        cartao.getStyleClass().addAll("cartao", "cartao-compacto");
        return cartao;
    }

    private void montarCamadaFim() {
        tituloFim.getStyleClass().add("titulo-fim");
        textoFim.getStyleClass().add("subtitulo");
        textoFim.setWrapText(true);

        Label nota = new Label("Clique abaixo para reniciar o jogo");
        nota.getStyleClass().add("dica");
        nota.setWrapText(true);

        Button botaoInicio = new Button("Voltar ao início");
        botaoInicio.getStyleClass().add("botao-primario");
        botaoInicio.setMaxWidth(Double.MAX_VALUE);
        botaoInicio.setOnAction(e -> voltarAoInicio());

        VBox cartao = new VBox(18, tituloFim, textoFim, botaoInicio, nota);
        cartao.setAlignment(Pos.CENTER);
        cartao.getStyleClass().add("cartao");
        cartao.setMaxSize(440, Region.USE_PREF_SIZE);

        camadaFim.getStyleClass().add("camada-escura");
        camadaFim.getChildren().add(cartao);
        camadaFim.setVisible(false);
    }

    /** Pinta os meus navios no "Seu mar". */
    private void desenharMinhaFrota() {
        for (int l = 0; l < N; l++) {
            for (int c = 0; c < N; c++) {
                if (meusNavios[l][c] != null) {
                    meuMar.marcar(new Posicao(l, c), "navio", "");
                }
            }
        }
    }


    private void atirar(Posicao alvo) {
        if (!minhaVez || aguardandoResposta || terminou) {
            return; // não é a minha vez: o clique é ignorado
        }
        if (marInimigo.getEstado(alvo) != null) {
            mostrarErroNoStatus("Você já atirou em " + Textos.nomeDaCasa(alvo) + ". Escolha outra casa.");
            return;
        }

        // A vez "sai da minha mão" assim que eu atiro
        minhaVez = false;
        aguardandoResposta = true;
        marInimigo.setClicavel(false);
        mostrarStatus("Disparando em " + Textos.nomeDaCasa(alvo) + "...", "status-espera");

        BatalhaNavalServer servidor = app.getServidor();
        int id = app.getIdJogador();

        Task<ResultadoTiro> tarefa = new Task<>() {
            @Override
            protected ResultadoTiro call() throws Exception {
                return servidor.fazerJogada(id, alvo);
            }
        };

        tarefa.setOnSucceeded(e -> {
            aguardandoResposta = false;
            ResultadoTiro resultado = tarefa.getValue();
            pintarResultado(marInimigo, alvo, resultado);
            registrar("Você atirou em " + Textos.nomeDaCasa(alvo) + ": " + textoResultado(resultado));
            if (resultado == ResultadoTiro.AFUNDOU) {
                inimigosAfundados++;
                atualizarPlacar();
            }
            /*
             * Ordem dos avisos: o servidor chama os callbacks (suaVez do
             * adversário, ou fimDeJogo) ANTES de devolver o resultado do
             * meu tiro. Então o fimDeJogo pode já ter chegado aqui.
             * Só mudo o status se nada disso aconteceu.
             */
            if (!terminou && !minhaVez) {
                mostrarVezDoAdversario();
            }
        });

        tarefa.setOnFailed(e -> {
            aguardandoResposta = false;
            tratarErroDoTiro(tarefa.getException());
        });

        Thread thread = new Thread(tarefa, "fazer-jogada");
        thread.setDaemon(true);
        thread.start();
    }

    private void tratarErroDoTiro(Throwable erro) {
        if (erro instanceof JogadaInvalidaException) {
            // Ex.: casa já atacada. O servidor NÃO passou a vez: tento de novo.
            suaVez();
            mostrarErroNoStatus("Jogada recusada: " + erro.getMessage() + " Tente outra casa.");
        } else if (erro instanceof NaoESuaVezException) {
            mostrarErroNoStatus(erro.getMessage());
        } else if (erro instanceof PartidaIndisponivelException) {
            mostrarErroNoStatus(erro.getMessage());
        } else if (erro instanceof RemoteException) {
            // Servidor caiu, ou o servidor não conseguiu avisar o adversário
            // (ele fechou o jogo). Nos dois casos a partida não continua.
            encerrar("Partida interrompida",
                    "Falha de comunicação: o servidor ou o adversário saiu do jogo.\n("
                            + erro.getClass().getSimpleName() + ")");
        } else {
            mostrarErroNoStatus("Erro inesperado: " + erro.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Avisos do servidor (OuvinteDoJogo) — chegam na thread do JavaFX
    // ------------------------------------------------------------------

    @Override
    public void partidaIniciada(String nomeAdversario) {
        // Já tratado na tela de posicionamento.
    }

    @Override
    public void suaVez() {
        if (terminou) {
            return;
        }
        minhaVez = true;
        marInimigo.setClicavel(true);
        mostrarStatus("Sua vez! Clique numa casa do mar de " + nomeAdversario + ".", "status-vez");
    }

    @Override
    public void tiroRecebido(Posicao posicao, ResultadoTiro resultado) {
        if (resultado == ResultadoTiro.AFUNDOU) {
            // Eu sei quais casas são do navio afundado: pinto todas
            TipoNavio afundado = meusNavios[posicao.getLinha()][posicao.getColuna()];
            for (int l = 0; l < N; l++) {
                for (int c = 0; c < N; c++) {
                    if (meusNavios[l][c] == afundado) {
                        meuMar.marcar(new Posicao(l, c), "afundado", "✕");
                    }
                }
            }
            meusAfundados++;
            atualizarPlacar();
            registrar(nomeAdversario + " afundou seu " + Textos.nomeNavio(afundado) + "!");
        } else {
            pintarResultado(meuMar, posicao, resultado);
            registrar(nomeAdversario + " atirou em " + Textos.nomeDaCasa(posicao) + ": "
                    + textoResultado(resultado));
        }
    }

    @Override
    public void fimDeJogo(String nomeVencedor, MotivoFim motivo) {
        boolean venci;
        String detalhe;
        if (motivo == MotivoFim.AFUNDOU_EMBARCACOES) {
            // Mais confiável que comparar nomes (os dois podem ter o mesmo nome):
            // se toda a minha frota afundou, eu perdi.
            venci = meusAfundados < TOTAL_NAVIOS;
            detalhe = venci
                    ? "Você afundou toda a frota de " + nomeAdversario + "."
                    : nomeAdversario + " afundou toda a sua frota.";
        } else {
            venci = nomeVencedor.equals(app.getNomeJogador());
            detalhe = "O adversário desconectou.";
        }
        encerrar(venci ? "Vitória!" : "Derrota", detalhe);
    }

    // ------------------------------------------------------------------
    // Auxiliares de tela
    // ------------------------------------------------------------------

    private void pintarResultado(GradeTabuleiro grade, Posicao p, ResultadoTiro resultado) {
        switch (resultado) {
            case AGUA -> grade.marcar(p, "agua", "•");
            case ACERTOU -> grade.marcar(p, "acerto", "✕");
            case AFUNDOU -> grade.marcar(p, "afundado", "✕");
        }
    }

    private String textoResultado(ResultadoTiro resultado) {
        return switch (resultado) {
            case AGUA -> "água";
            case ACERTOU -> "acertou!";
            case AFUNDOU -> "afundou um navio!";
        };
    }

    private void mostrarVezDoAdversario() {
        marInimigo.setClicavel(false);
        mostrarStatus("Vez de " + nomeAdversario + ". Aguarde o tiro dele...", "status-espera");
    }

    private void mostrarStatus(String texto, String estilo) {
        labelStatus.getStyleClass().setAll("label", "status", estilo);
        labelStatus.setText(texto);
    }

    private void mostrarErroNoStatus(String texto) {
        labelStatus.getStyleClass().setAll("label", "status", "status-erro");
        labelStatus.setText(texto);
    }

    private void atualizarPlacar() {
        labelPlacar.setText("Navios inimigos afundados: " + inimigosAfundados + "/" + TOTAL_NAVIOS
                + "     ·     Seus navios perdidos: " + meusAfundados + "/" + TOTAL_NAVIOS);
    }

    /** Coloca uma linha no topo do histórico (mantém só as 3 últimas). */
    private void registrar(String texto) {
        Label linha = new Label(texto);
        linha.getStyleClass().add("linha-historico");
        historico.getChildren().add(0, linha);
        if (historico.getChildren().size() > 3) {
            historico.getChildren().remove(3);
        }
    }

    private void encerrar(String titulo, String texto) {
        terminou = true;
        minhaVez = false;
        marInimigo.setClicavel(false);
        tituloFim.setText(titulo);
        textoFim.setText(texto);
        camadaFim.setVisible(true);
    }

    private void voltarAoInicio() {
        app.liberarCallback();
        app.mostrarTela(new TelaConexao(app).getRaiz());
    }
}