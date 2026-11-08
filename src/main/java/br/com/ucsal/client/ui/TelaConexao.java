package br.com.ucsal.client.ui;

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

/**
 * Tela 1: o jogador informa onde está o servidor e o seu nome.
 * Nesta versão, só o visual + validação dos campos.
 * A conexão RMI entra na próxima etapa, dentro de conectar().
 */
public class TelaConexao {

    private final BatalhaNavalApp app;
    private final StackPane raiz = new StackPane();

    // Os componentes são atributos porque conectar() precisa ler/alterar eles
    private final TextField campoHost = new TextField("localhost");
    private final TextField campoPorta = new TextField("1099");
    private final TextField campoNome = new TextField();
    private final Button botaoConectar = new Button("Conectar");
    private final ProgressIndicator carregando = new ProgressIndicator();
    private final Label labelMensagem = new Label();

    public TelaConexao(BatalhaNavalApp app) {
        this.app = app;
        montarLayout();

        // Evento: o que acontece ao clicar (ou apertar Enter no nome)
        botaoConectar.setOnAction(e -> conectar());
        campoNome.setOnAction(e -> conectar());
    }

    public StackPane getRaiz() {
        return raiz;
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private void montarLayout() {
        Label titulo = new Label("BATALHA NAVAL");
        titulo.getStyleClass().add("titulo");

        Label subtitulo = new Label("Conecte-se ao servidor para entrar na partida");
        subtitulo.getStyleClass().add("subtitulo");

        // Texto cinza que aparece quando o campo está vazio
        campoHost.setPromptText("ex.: localhost ou 192.168.0.10");
        campoPorta.setPromptText("1099");
        campoNome.setPromptText("Como quer ser chamado?");

        // GridPane = tabela. addRow(linha, coluna0, coluna1)
        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(12);
        formulario.addRow(0, rotulo("Servidor"), campoHost);
        formulario.addRow(1, rotulo("Porta"), campoPorta);
        formulario.addRow(2, rotulo("Seu nome"), campoNome);

        botaoConectar.getStyleClass().add("botao-primario");
        botaoConectar.setMaxWidth(Double.MAX_VALUE); // ocupa a largura toda

        carregando.setPrefSize(22, 22);
        carregando.setVisible(false); // só aparece enquanto conecta

        labelMensagem.setWrapText(true);

        // HBox = fila horizontal: [bolinha girando] [mensagem]
        HBox linhaStatus = new HBox(10, carregando, labelMensagem);
        linhaStatus.setAlignment(Pos.CENTER_LEFT);
        linhaStatus.setMinHeight(28);

        // VBox = pilha vertical, com 18px de espaço entre os itens
        VBox cartao = new VBox(18, titulo, subtitulo, formulario, botaoConectar, linhaStatus);
        cartao.getStyleClass().add("cartao");
        // Não deixa o cartão esticar até a borda da janela
        cartao.setMaxSize(420, Region.USE_PREF_SIZE);

        raiz.getStyleClass().add("fundo-oceano");
        raiz.getChildren().add(cartao); // StackPane centraliza o cartão
    }

    private Label rotulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("rotulo");
        return l;
    }

    // ------------------------------------------------------------------
    // Ação do botão
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

        // Provisório: na próxima etapa, aqui entra a conexão RMI
        mostrarOk("Tudo certo! Conectaria em " + host + ":" + porta + " como " + nome + ".");
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