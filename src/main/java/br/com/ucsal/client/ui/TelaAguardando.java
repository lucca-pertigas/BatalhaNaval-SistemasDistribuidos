package br.com.ucsal.client.ui;

import br.com.ucsal.client.remote.ClienteCallbackImpl;
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
 * Tela 2: cria o callback e chama entrarNaPartida.
 * Deu certo -> (próxima etapa) vai para o posicionamento dos navios.
 */
public class TelaAguardando {

    private final BatalhaNavalApp app;
    private final StackPane raiz = new StackPane();

    private final Label titulo = new Label("Entrando na partida...");
    private final Label info = new Label();
    private final ProgressIndicator carregando = new ProgressIndicator();
    private final Button botaoVoltar = new Button("Voltar");

    public TelaAguardando(BatalhaNavalApp app) {
        this.app = app;
        montarLayout();
        entrarNaPartida();
    }

    public StackPane getRaiz() {
        return raiz;
    }

    private void montarLayout() {
        titulo.getStyleClass().add("titulo");
        info.getStyleClass().add("subtitulo");
        info.setWrapText(true);

        carregando.setPrefSize(48, 48);

        botaoVoltar.getStyleClass().add("botao-primario");
        botaoVoltar.setMaxWidth(Double.MAX_VALUE);
        botaoVoltar.setOnAction(e -> voltar());
        botaoVoltar.setVisible(false);  // só aparece se der erro
        botaoVoltar.setManaged(false);  // e, escondido, não ocupa espaço

        VBox cartao = new VBox(20, titulo, info, carregando, botaoVoltar);
        cartao.setAlignment(Pos.CENTER);
        cartao.getStyleClass().add("cartao");
        cartao.setMaxSize(420, Region.USE_PREF_SIZE);

        raiz.getStyleClass().add("fundo-oceano");
        raiz.getChildren().add(cartao);
    }

    private void entrarNaPartida() {
        // 1) Cria o callback DA DUPLA, entregando o nosso OuvinteFX.
        //    O construtor exporta o objeto (abre uma porta para o servidor chamar).
        ClienteCallbackImpl callback;
        try {
            callback = new ClienteCallbackImpl(app.getOuvinteFX());
        } catch (RemoteException e) {
            mostrarErro("Não foi possível preparar o recebimento de avisos: " + e.getMessage());
            return;
        }
        app.setCallback(callback);

        BatalhaNavalServer servidor = app.getServidor();
        String nome = app.getNomeJogador();

        // 2) Chamada remota fora da thread do JavaFX
        Task<Integer> tarefa = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return servidor.entrarNaPartida(nome, callback);
            }
        };

        tarefa.setOnSucceeded(e -> {
            int id = tarefa.getValue();
            app.setIdJogador(id);
            carregando.setVisible(false);
            titulo.setText("Você está na partida!");
            info.setText("Olá, " + nome + ". Você é o jogador " + (id + 1) + ".");
            // Próxima etapa: app.mostrarTela(new TelaPosicionamento(app).getRaiz());
        });

        tarefa.setOnFailed(e -> mostrarErro(traduzirErro(tarefa.getException())));

        Thread thread = new Thread(tarefa, "entrar-na-partida");
        thread.setDaemon(true);
        thread.start();
    }

    private String traduzirErro(Throwable erro) {
        if (erro instanceof PartidaIndisponivelException) {
            // Exceção de negócio do backend (ex.: "A partida ja esta cheia !")
            return erro.getMessage();
        }
        if (erro instanceof RemoteException) {
            return "Erro de comunicação com o servidor: " + erro.getMessage();
        }
        return "Erro inesperado: " + erro.getMessage();
    }

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