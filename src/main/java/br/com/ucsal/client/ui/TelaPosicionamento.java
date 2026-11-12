package br.com.ucsal.client.ui;

import br.com.ucsal.client.OuvinteDoJogo;
import br.com.ucsal.common.exception.JogadaInvalidaException;
import br.com.ucsal.common.exception.PartidaIndisponivelException;
import br.com.ucsal.common.model.Direcao;
import br.com.ucsal.common.model.MotivoFim;
import br.com.ucsal.common.model.Posicao;
import br.com.ucsal.common.model.ResultadoTiro;
import br.com.ucsal.common.model.TipoNavio;
import br.com.ucsal.common.remote.BatalhaNavalServer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Tela 3: o jogador posiciona os 5 navios.
 *
 * Fluxo:
 *  - escolhe o navio na lista à direita;
 *  - passa o mouse no tabuleiro -> prévia verde (cabe) ou vermelha (não cabe);
 *  - botão direito (ou o botão "Girar") troca horizontal/vertical;
 *  - clica -> chamada remota posicionarNavio(...).
 *
 * Quando os DOIS jogadores completam a frota, o servidor chama
 * partidaIniciada(...) pelo callback, e a batalha começa.
 *
 * Importante: a validação feita aqui (prévia verde/vermelha) é só para
 * ajudar o jogador. Quem decide de verdade é o SERVIDOR: se ele recusar,
 * chega uma JogadaInvalidaException e mostramos a mensagem dele.
 */
public class TelaPosicionamento implements OuvinteDoJogo {

    private static final int N = BatalhaNavalApp.TAMANHO_TABULEIRO;

    /** Um navio que o jogador pediu para posicionar. */
    private record Navio(TipoNavio tipo, Posicao inicio, Direcao direcao, List<Posicao> casas) { }

    private final BatalhaNavalApp app;
    private final StackPane raiz = new StackPane();
    private final GradeTabuleiro grade = new GradeTabuleiro();

    private final Map<TipoNavio, ToggleButton> botoesNavio = new EnumMap<>(TipoNavio.class);
    private final Button botaoGirar = new Button();
    private final Button botaoAleatorio = new Button("Posicionar o resto aleatoriamente");
    private final ProgressIndicator carregando = new ProgressIndicator();
    private final Label labelMensagem = new Label();

    // ---- Estado da tela (só mexido na thread do JavaFX) ----
    private final TipoNavio[][] meusNavios = new TipoNavio[N][N]; // cópia local do meu tabuleiro
    private final Set<TipoNavio> posicionados = EnumSet.noneOf(TipoNavio.class);
    private final List<Navio> enviados = new ArrayList<>(); // enviados ao servidor, sem resposta ainda
    private TipoNavio selecionado;
    private Direcao direcao = Direcao.HORIZONTAL;
    private Posicao mouseSobre;       // célula onde o mouse está (para redesenhar a prévia)
    private boolean ocupado;          // há uma chamada remota em andamento
    private boolean partidaComecou;

    public TelaPosicionamento(BatalhaNavalApp app) {
        this.app = app;
        montarLayout();
        ligarEventos();

        app.ouvirAvisos(this); // a partir de agora, os avisos do servidor vêm para esta tela
        selecionarProximo();
    }

    public StackPane getRaiz() {
        return raiz;
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private void montarLayout() {
        Label titulo = new Label("Posicione sua frota");
        titulo.getStyleClass().add("titulo");

        Label subtitulo = new Label("Jogador " + (app.getIdJogador() + 1) + " · " + app.getNomeJogador());
        subtitulo.getStyleClass().add("subtitulo");

        VBox cabecalho = new VBox(4, titulo, subtitulo);
        cabecalho.setAlignment(Pos.CENTER);

        // Esquerda: o tabuleiro
        VBox cartaoGrade = new VBox(grade.getNo());
        cartaoGrade.getStyleClass().add("cartao");

        // Direita: lista de navios + botões + mensagens
        Label tituloFrota = new Label("Sua frota");
        tituloFrota.getStyleClass().add("titulo-secao");

        VBox listaNavios = new VBox(8);
        for (TipoNavio tipo : TipoNavio.values()) {
            ToggleButton botao = new ToggleButton(textoDoNavio(tipo, false));
            botao.getStyleClass().add("item-navio");
            botao.setMaxWidth(Double.MAX_VALUE);
            botao.setOnAction(e -> selecionar(tipo));
            botoesNavio.put(tipo, botao);
            listaNavios.getChildren().add(botao);
        }

        botaoGirar.getStyleClass().add("botao-secundario");
        botaoGirar.setMaxWidth(Double.MAX_VALUE);
        atualizarTextoGirar();

        botaoAleatorio.getStyleClass().add("botao-secundario");
        botaoAleatorio.setMaxWidth(Double.MAX_VALUE);

        Label dica = new Label("Dica: clique com o botão direito no tabuleiro para girar.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);

        carregando.setPrefSize(22, 22);
        carregando.setVisible(false);
        labelMensagem.setWrapText(true);

        HBox linhaStatus = new HBox(10, carregando, labelMensagem);
        linhaStatus.setAlignment(Pos.CENTER_LEFT);

        VBox cartaoFrota = new VBox(14, tituloFrota, listaNavios, botaoGirar, botaoAleatorio, dica, linhaStatus);
        cartaoFrota.getStyleClass().add("cartao");
        cartaoFrota.setPrefWidth(300);
        cartaoFrota.setMaxWidth(300);

        HBox conteudo = new HBox(24, cartaoGrade, cartaoFrota);
        conteudo.setAlignment(Pos.CENTER);

        VBox tudo = new VBox(20, cabecalho, conteudo);
        tudo.setAlignment(Pos.CENTER);

        raiz.getStyleClass().add("fundo-oceano");
        raiz.getChildren().add(tudo);
    }

    private void ligarEventos() {
        grade.setClicavel(true);
        grade.setAoEntrar(posicao -> {
            mouseSobre = posicao;
            atualizarPrevia();
        });
        grade.setAoSair(() -> {
            mouseSobre = null;
            grade.limparPrevia();
        });
        grade.setAoClicar(posicao -> {
            if (!ocupado && !partidaComecou && selecionado != null) {
                posicionar(selecionado, posicao, direcao);
            }
        });
        grade.setAoClicarDireito(this::girar);

        botaoGirar.setOnAction(e -> girar());
        botaoAleatorio.setOnAction(e -> posicionarRestoAleatorio());
    }

    /** "Porta-aviões  ■■■■■" (com ✓ quando já posicionado) */
    private String textoDoNavio(TipoNavio tipo, boolean pronto) {
        String blocos = "■".repeat(tipo.getTamanho());
        return (pronto ? "✓ " : "") + Textos.nomeNavio(tipo) + "   " + blocos;
    }

    private void atualizarTextoGirar() {
        botaoGirar.setText(direcao == Direcao.HORIZONTAL ? "Girar  (horizontal ↔)" : "Girar  (vertical ↕)");
    }

    // ------------------------------------------------------------------
    // Seleção, giro e prévia (só visual, nada de rede aqui)
    // ------------------------------------------------------------------

    private void selecionar(TipoNavio tipo) {
        if (posicionados.contains(tipo)) {
            return;
        }
        selecionado = tipo;
        // Só o botão escolhido fica "marcado"
        botoesNavio.forEach((t, botao) -> botao.setSelected(t == tipo));
        mostrarInfo("Posicione o " + Textos.nomeNavio(tipo) + " (" + tipo.getTamanho() + " casas).");
        atualizarPrevia();
    }

    /** Seleciona o primeiro navio que ainda falta. */
    private void selecionarProximo() {
        for (TipoNavio tipo : TipoNavio.values()) {
            if (!posicionados.contains(tipo)) {
                selecionar(tipo);
                return;
            }
        }
        selecionado = null;
    }

    private void girar() {
        direcao = (direcao == Direcao.HORIZONTAL) ? Direcao.VERTICAL : Direcao.HORIZONTAL;
        atualizarTextoGirar();
        atualizarPrevia();
    }

    private void atualizarPrevia() {
        if (mouseSobre == null || selecionado == null || ocupado || partidaComecou) {
            grade.limparPrevia();
            return;
        }
        List<Posicao> casas = calcularCasas(selecionado, mouseSobre, direcao);
        grade.mostrarPrevia(casas, cabe(casas, meusNavios));
    }

    /** As casas que um navio ocuparia a partir de "inicio" (mesma regra do servidor). */
    private static List<Posicao> calcularCasas(TipoNavio tipo, Posicao inicio, Direcao direcao) {
        List<Posicao> casas = new ArrayList<>();
        for (int i = 0; i < tipo.getTamanho(); i++) {
            if (direcao == Direcao.HORIZONTAL) {
                casas.add(new Posicao(inicio.getLinha(), inicio.getColuna() + i));
            } else {
                casas.add(new Posicao(inicio.getLinha() + i, inicio.getColuna()));
            }
        }
        return casas;
    }

    /** Todas as casas dentro do tabuleiro e livres? */
    private static boolean cabe(List<Posicao> casas, TipoNavio[][] ocupacao) {
        for (Posicao p : casas) {
            if (!GradeTabuleiro.dentro(p) || ocupacao[p.getLinha()][p.getColuna()] != null) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Chamadas remotas: posicionarNavio
    // ------------------------------------------------------------------

    private void posicionar(TipoNavio tipo, Posicao inicio, Direcao direcao) {
        List<Posicao> casas = calcularCasas(tipo, inicio, direcao);
        if (!cabe(casas, meusNavios)) {
            mostrarErro("O " + Textos.nomeNavio(tipo) + " não cabe a partir de "
                    + Textos.nomeDaCasa(inicio) + ".");
            return;
        }
        enviarAoServidor(List.of(new Navio(tipo, inicio, direcao, casas)));
    }

    /** Sorteia posições para os navios que faltam e envia todos. */
    private void posicionarRestoAleatorio() {
        if (ocupado || partidaComecou) {
            return;
        }
        // Trabalha numa CÓPIA da ocupação, para os sorteados não se sobreporem
        TipoNavio[][] copia = new TipoNavio[N][N];
        for (int l = 0; l < N; l++) {
            copia[l] = meusNavios[l].clone();
        }

        Random sorteio = new Random();
        List<Navio> lista = new ArrayList<>();
        for (TipoNavio tipo : TipoNavio.values()) {
            if (posicionados.contains(tipo)) {
                continue;
            }
            while (true) { // tenta posições até achar uma que cabe
                Direcao d = sorteio.nextBoolean() ? Direcao.HORIZONTAL : Direcao.VERTICAL;
                Posicao inicio = new Posicao(sorteio.nextInt(N), sorteio.nextInt(N));
                List<Posicao> casas = calcularCasas(tipo, inicio, d);
                if (cabe(casas, copia)) {
                    for (Posicao p : casas) {
                        copia[p.getLinha()][p.getColuna()] = tipo;
                    }
                    lista.add(new Navio(tipo, inicio, d, casas));
                    break;
                }
            }
        }
        enviarAoServidor(lista);
    }

    /**
     * Envia um ou mais navios, um por vez, numa thread separada.
     * Cada navio aceito é confirmado na tela com Platform.runLater.
     */
    private void enviarAoServidor(List<Navio> lista) {
        setOcupado(true);
        mostrarInfo("Enviando ao servidor...");
        enviados.addAll(lista);

        BatalhaNavalServer servidor = app.getServidor();
        int id = app.getIdJogador();

        Task<Void> tarefa = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (Navio navio : lista) {
                    // Chamada remota: se o servidor recusar, lança exceção e o laço para
                    servidor.posicionarNavio(id, navio.tipo(), navio.inicio(), navio.direcao());
                    Platform.runLater(() -> confirmar(navio));
                }
                return null;
            }
        };

        tarefa.setOnSucceeded(e -> setOcupado(false));
        tarefa.setOnFailed(e -> {
            enviados.clear(); // os que não foram confirmados não valeram
            setOcupado(false);
            mostrarErro(traduzirErro(tarefa.getException()));
        });

        Thread thread = new Thread(tarefa, "posicionar-navio");
        thread.setDaemon(true);
        thread.start();
    }

    /** O servidor aceitou: pinta o navio e passa para o próximo. */
    private void confirmar(Navio navio) {
        if (posicionados.contains(navio.tipo())) {
            return; // já confirmado (ver partidaIniciada)
        }
        for (Posicao p : navio.casas()) {
            meusNavios[p.getLinha()][p.getColuna()] = navio.tipo();
            grade.marcar(p, "navio", "");
        }
        posicionados.add(navio.tipo());
        enviados.remove(navio);

        ToggleButton botao = botoesNavio.get(navio.tipo());
        botao.setText(textoDoNavio(navio.tipo(), true));
        botao.setSelected(false);
        botao.setDisable(true);

        if (posicionados.size() == TipoNavio.values().length) {
            frotaPronta();
        } else if (selecionado == navio.tipo()) {
            selecionarProximo();
        }
    }

    private void frotaPronta() {
        selecionado = null;
        grade.setClicavel(false);
        grade.limparPrevia();
        botaoGirar.setDisable(true);
        botaoAleatorio.setDisable(true);
        if (!partidaComecou) {
            carregando.setVisible(true);
            mostrarOk("Frota pronta! Aguardando o adversário posicionar a frota...");
        }
    }

    private void setOcupado(boolean ocupado) {
        this.ocupado = ocupado;
        boolean frotaCompleta = posicionados.size() == TipoNavio.values().length;
        carregando.setVisible(ocupado || (frotaCompleta && !partidaComecou));
        botaoGirar.setDisable(ocupado || frotaCompleta);
        botaoAleatorio.setDisable(ocupado || frotaCompleta);
        atualizarPrevia();
    }

    private String traduzirErro(Throwable erro) {
        if (erro instanceof JogadaInvalidaException) {
            return "O servidor recusou a posição: " + erro.getMessage();
        }
        if (erro instanceof PartidaIndisponivelException) {
            return erro.getMessage();
        }
        if (erro instanceof RemoteException) {
            return "Falha de comunicação com o servidor: " + erro.getMessage();
        }
        return "Erro inesperado: " + erro.getMessage();
    }

    // ------------------------------------------------------------------
    // Avisos do servidor (OuvinteDoJogo) — chegam na thread do JavaFX
    // ------------------------------------------------------------------

    @Override
    public void partidaIniciada(String nomeAdversario) {
        partidaComecou = true;

        /*
         * Detalhe de concorrência: para quem posiciona o ÚLTIMO navio, o
         * servidor chama partidaIniciada() DENTRO do posicionarNavio, antes
         * de responder. Esse aviso chega à tela antes da confirmação do
         * último navio. Como a partida só começa com a frota completa,
         * tudo o que foi enviado com certeza foi aceito: confirmamos aqui.
         */
        for (Navio navio : new ArrayList<>(enviados)) {
            confirmar(navio);
        }

        carregando.setVisible(false);
        mostrarOk("Partida iniciada contra " + nomeAdversario + "!");
        // Próxima etapa: app.mostrarTela(new TelaJogo(app, nomeAdversario, meusNavios).getRaiz());
    }

    // Os avisos abaixo só fazem sentido durante a batalha (próxima tela)
    @Override public void suaVez() { }
    @Override public void tiroRecebido(Posicao posicao, ResultadoTiro resultadoTiro) { }
    @Override public void fimDeJogo(String nomeVencedor, MotivoFim motivoFim) { }

    // ------------------------------------------------------------------
    // Mensagens
    // ------------------------------------------------------------------

    private void mostrarInfo(String texto) {
        labelMensagem.getStyleClass().setAll("label", "mensagem-info");
        labelMensagem.setText(texto);
    }

    private void mostrarOk(String texto) {
        labelMensagem.getStyleClass().setAll("label", "mensagem-ok");
        labelMensagem.setText(texto);
    }

    private void mostrarErro(String texto) {
        labelMensagem.getStyleClass().setAll("label", "mensagem-erro");
        labelMensagem.setText(texto);
    }
}