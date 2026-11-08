package br.com.ucsal.client.ui;

import br.com.ucsal.client.ClienteCallbackImpl;
import br.com.ucsal.client.EventosDoJogo;
import br.com.ucsal.common.exception.PartidaIndisponivelException;
import br.com.ucsal.common.remote.BatalhaNavalServer;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.rmi.RemoteException;

/**
 * Tela 2: entra na partida e espera o adversário.
 *
 * implements EventosDoJogo -> esta tela é o "ouvinte" do callback
 * enquanto estiver aberta.
 */
public class TelaAguardando implements EventosDoJogo {

    private final BatalhaNavalApp app;
    private final StackPane raiz = new StackPane();

    private final Label titulo = new Label("Entrando na partida...");
    private final Label info = new Label();
    private final ProgressIndicator carregando = new ProgressIndicator();
    private final Button botaoVoltar = new Button("Voltar");

    // O aviso do servidor pode chegar ANTES da resposta do entrarNaPartida
    // (ver comentário em entrarNaPartida). Esta flag evita sobrescrever a tela.
    private boolean adversarioChegou = false;

    public TelaAguardando(BatalhaNavalApp app) {
        this.app = app;
        montarLayout();
        entrarNaPartida();
    }

    public StackPane getRaiz() {
        return raiz;
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private void montarLayout() {
        titulo.getStyleClass().add("titulo");
        info.getStyleClass().add("subtitulo");
        info.setWrapText(true);

        carregando.setPrefSize(48, 48);

        botaoVoltar.getStyleClass().add("botao-primario");
        botaoVoltar.setMaxWidth(Double.MAX_VALUE);
        botaoVoltar.setOnAction(e -> voltar());
        // Escondido até acontecer um erro.
        // setManaged(false) = além de invisível, não ocupa espaço no layout.
        botaoVoltar.setVisible(false);
        botaoVoltar.setManaged(false);

        VBox cartao = new VBox(20, titulo, info, carregando, botaoVoltar);
        cartao.setAlignment(Pos.CENTER);
        cartao.getStyleClass().add("cartao");
        cartao.setMaxSize(420, Region.USE_PREF_SIZE);

        raiz.getStyleClass().add("fundo-oceano");
        raiz.getChildren().add(cartao);
    }

    // ------------------------------------------------------------------
    // Chamada remota: entrarNaPartida
    // ------------------------------------------------------------------

    private void entrarNaPartida() {
        // 1) Cria e exporta o callback (rápido, é local: só abre uma porta)
        ClienteCallbackImpl callback;
        try {
            callback = new ClienteCallbackImpl();
        } catch (RemoteException e) {
            mostrarErro("Não foi possível preparar o recebimento de avisos: " + e.getMessage());
            return;
        }
        callback.setOuvinte(this); // esta tela passa a receber os avisos
        app.setCallback(callback);

        BatalhaNavalServer servidor = app.getServidor();
        String nome = app.getNomeJogador();

        // 2) Chamada remota em outra thread (mesmo motivo do lookup)
        Task<Integer> tarefa = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return servidor.entrarNaPartida(nome, callback);
            }
        };

        /*
         * Detalhe de concorrência: se você for o 2º jogador, o servidor pode
         * chamar pastidaIniciada() ENQUANTO entrarNaPartida ainda não voltou.
         * Os dois eventos entram na fila da thread do JavaFX, e o aviso pode
         * ser processado antes deste setOnSucceeded. Por isso usamos a flag.
         */
        tarefa.setOnSucceeded(e -> {
            app.setIdJogador(tarefa.getValue());
            if (!adversarioChegou) {
                titulo.setText("Você está na partida!");
                info.setText("Olá, " + nome + ". Aguardando adversário...");
            }
        });

        tarefa.setOnFailed(e -> mostrarErro(traduzirErro(tarefa.getException())));

        Thread thread = new Thread(tarefa, "entrar-na-partida");
        thread.setDaemon(true);
        thread.start();
    }

    private String traduzirErro(Throwable erro) {
        if (erro instanceof PartidaIndisponivelException) {
            // Exceção de negócio do backend (ex.: partida cheia)
            return "Partida indisponível: " + erro.getMessage();
        }
        if (erro instanceof RemoteException) {
            return "Erro de comunicação com o servidor: " + erro.getMessage();
        }
        return "Erro inesperado: " + erro.getMessage();
    }

    // ------------------------------------------------------------------
    // Avisos do servidor (EventosDoJogo) — já chegam na thread do JavaFX
    // ------------------------------------------------------------------

    @Override
    public void partidaIniciada(String nomeAdversario) {
        adversarioChegou = true;
        carregando.setVisible(false);
        titulo.setText("Adversário encontrado!");
        info.setText(app.getNomeJogador() + " vs " + nomeAdversario + "\nPrepare sua frota...");
        // Próxima etapa: app.mostrarTela(new TelaPosicionamento(app).getRaiz());
    }

    // ------------------------------------------------------------------
    // Erro / voltar
    // ------------------------------------------------------------------

    private void mostrarErro(String texto) {
        carregando.setVisible(false);
        titulo.setText("Não foi possível entrar");
        info.getStyleClass().setAll("label", "mensagem-erro");
        info.setText(texto);
        botaoVoltar.setVisible(true);
        botaoVoltar.setManaged(true);
    }

    private void voltar() {
        app.liberarCallback();
        app.mostrarTela(new TelaConexao(app).getRaiz());
    }
}