package br.com.ucsal.client.ui;

import br.com.ucsal.common.remote.BatalhaNavalServer;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Tela 1: o jogador informa onde está o servidor e o seu nome.
 *
 * Fluxo ao clicar em "Conectar":
 *   1. valida os campos;
 *   2. numa thread SEPARADA, pede ao RMI Registry o objeto "BatalhaNaval";
 *   3. deu certo  -> guarda o stub no app e vai para a tela de espera;
 *      deu errado -> mostra uma mensagem amigável.
 */
public class TelaConexao {

    // Precisa ser IGUAL ao nome usado em locateRegistry.rebind(...) no IniciarServidor
    private static final String NOME_NO_REGISTRY = "BatalhaNaval";

    private final BatalhaNavalApp app;
    private final StackPane raiz = new StackPane();

    private final TextField campoHost = new TextField("localhost");
    private final TextField campoPorta = new TextField("1099");
    private final TextField campoNome = new TextField();
    private final Button botaoConectar = new Button("Conectar");
    private final ProgressIndicator carregando = new ProgressIndicator();
    private final Label labelMensagem = new Label();

    public TelaConexao(BatalhaNavalApp app) {
        this.app = app;
        montarLayout();

        botaoConectar.setOnAction(e -> conectar());
        campoNome.setOnAction(e -> conectar());
    }

    public StackPane getRaiz() {
        return raiz;
    }

    // ------------------------------------------------------------------
    // Layout (igual à versão anterior)
    // ------------------------------------------------------------------

    private void montarLayout() {
        Label titulo = new Label("BATALHA NAVAL");
        titulo.getStyleClass().add("titulo");

        Label subtitulo = new Label("Conecte-se ao servidor para entrar na partida");
        subtitulo.getStyleClass().add("subtitulo");

        campoHost.setPromptText("ex.: localhost ou 192.168.0.10");
        campoPorta.setPromptText("1099");
        campoNome.setPromptText("Como quer ser chamado?");

        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(12);
        formulario.addRow(0, rotulo("Servidor"), campoHost);
        formulario.addRow(1, rotulo("Porta"), campoPorta);
        formulario.addRow(2, rotulo("Seu nome"), campoNome);

        botaoConectar.getStyleClass().add("botao-primario");
        botaoConectar.setMaxWidth(Double.MAX_VALUE);

        carregando.setPrefSize(22, 22);
        carregando.setVisible(false);

        labelMensagem.setWrapText(true);

        HBox linhaStatus = new HBox(10, carregando, labelMensagem);
        linhaStatus.setAlignment(Pos.CENTER_LEFT);
        linhaStatus.setMinHeight(28);

        VBox cartao = new VBox(18, titulo, subtitulo, formulario, botaoConectar, linhaStatus);
        cartao.getStyleClass().add("cartao");
        cartao.setMaxSize(420, Region.USE_PREF_SIZE);

        raiz.getStyleClass().add("fundo-oceano");
        raiz.getChildren().add(cartao);
    }

    private Label rotulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("rotulo");
        return l;
    }

    // ------------------------------------------------------------------
    // Conexão RMI
    // ------------------------------------------------------------------

    private void conectar() {
        String host = campoHost.getText().trim();
        String nome = campoNome.getText().trim();

        if (host.isEmpty()) {
            mostrarErro("Informe o endereço do servidor.");
            return;
        }
        if (nome.isEmpty()) {
            mostrarErro("Digite seu nome.");
            return;
        }

        int porta;
        try {
            porta = Integer.parseInt(campoPorta.getText().trim());
        } catch (NumberFormatException ex) {
            mostrarErro("A porta precisa ser um número (padrão: 1099).");
            return;
        }

        setOcupado(true);

        /*
         * CONCEITO: nunca fazer chamada de rede na thread do JavaFX.
         * A "JavaFX Application Thread" desenha a tela e trata cliques.
         * Se ela ficar esperando a rede, a janela congela ("Não está respondendo").
         *
         * Task = tarefa que roda o call() em OUTRA thread e depois executa
         * setOnSucceeded / setOnFailed DE VOLTA na thread do JavaFX,
         * onde é permitido mexer nos componentes da tela.
         */
        Task<BatalhaNavalServer> tarefa = new Task<>() {
            @Override
            protected BatalhaNavalServer call() throws Exception {
                // getRegistry NÃO conecta ainda: só cria uma referência local.
                Registry registry = LocateRegistry.getRegistry(host, porta);

                // lookup é a primeira ida à rede: pergunta ao Registry pelo
                // objeto "BatalhaNaval" e recebe de volta o STUB.
                return (BatalhaNavalServer) registry.lookup(NOME_NO_REGISTRY);
            }
        };

        // Rodam na thread do JavaFX -> podem mexer na tela
        tarefa.setOnSucceeded(e -> {
            app.setServidor(tarefa.getValue());
            app.setNomeJogador(nome);
            app.mostrarTela(new TelaAguardando(app).getRaiz());
        });

        tarefa.setOnFailed(e -> {
            setOcupado(false);
            mostrarErro(traduzirErro(tarefa.getException(), host, porta));
        });

        Thread thread = new Thread(tarefa, "conexao-rmi");
        thread.setDaemon(true); // não impede o programa de fechar
        thread.start();
    }

    /** Converte as exceções do RMI em mensagens que o jogador entende. */
    private String traduzirErro(Throwable erro, String host, int porta) {
        if (erro instanceof ConnectException) {
            return "Não foi possível conectar em " + host + ":" + porta
                    + ". O servidor está rodando?";
        }
        if (erro instanceof UnknownHostException) {
            return "Endereço \"" + host + "\" não encontrado.";
        }
        if (erro instanceof NotBoundException) {
            return "Servidor encontrado, mas o serviço \"" + NOME_NO_REGISTRY
                    + "\" não está registrado.";
        }
        if (erro instanceof RemoteException) {
            return "Erro de comunicação: " + erro.getMessage();
        }
        return "Erro inesperado: " + erro.getMessage();
    }

    /** Trava/destrava o formulário enquanto a conexão está em andamento. */
    private void setOcupado(boolean ocupado) {
        botaoConectar.setDisable(ocupado);
        campoHost.setDisable(ocupado);
        campoPorta.setDisable(ocupado);
        campoNome.setDisable(ocupado);
        carregando.setVisible(ocupado);
        if (ocupado) {
            mostrarOk("Conectando...");
        }
    }

    private void mostrarErro(String texto) {
        labelMensagem.getStyleClass().setAll("label", "mensagem-erro");
        labelMensagem.setText(texto);
    }

    private void mostrarOk(String texto) {
        labelMensagem.getStyleClass().setAll("label", "mensagem-ok");
        labelMensagem.setText(texto);
    }
}