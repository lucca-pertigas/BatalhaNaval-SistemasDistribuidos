package br.com.ucsal.client.ui;

import br.com.ucsal.client.ClienteCallbackImpl;
import br.com.ucsal.common.remote.BatalhaNavalServer;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.rmi.NoSuchObjectException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Aplicação JavaFX do cliente.
 *
 * Uma janela (Stage), uma cena (Scene). Para trocar de tela,
 * trocamos só a raiz da cena.
 *
 * Guarda o estado compartilhado entre as telas:
 * stub do servidor, nome, id do jogador e o callback.
 */
public class BatalhaNavalApp extends Application {

    private Scene cena;

    private BatalhaNavalServer servidor;     // stub: representa o servidor remoto
    private String nomeJogador;
    private int idJogador = -1;              // devolvido por entrarNaPartida
    private ClienteCallbackImpl callback;    // objeto que o servidor chama

    @Override
    public void start(Stage palco) {
        cena = new Scene(new TelaConexao(this).getRaiz(), 960, 640);
        cena.getStylesheets().add(
                getClass().getResource("estilo.css").toExternalForm());

        palco.setTitle("Batalha Naval");
        palco.setMinWidth(720);
        palco.setMinHeight(520);
        palco.setScene(cena);
        palco.show();
    }

    /**
     * Chamado pelo JavaFX quando a janela é fechada.
     * O callback exportado mantém threads do RMI vivas: sem isso,
     * o processo continuaria rodando "invisível" depois de fechar a janela.
     */
    @Override
    public void stop() {
        liberarCallback();
        System.exit(0);
    }

    /** Troca a tela exibida na janela. */
    public void mostrarTela(Parent raiz) {
        cena.setRoot(raiz);
    }

    /** Para de receber chamadas do servidor (desexporta o objeto remoto). */
    public void liberarCallback() {
        if (callback != null) {
            try {
                UnicastRemoteObject.unexportObject(callback, true);
            } catch (NoSuchObjectException ignorado) {
                // já não estava exportado: nada a fazer
            }
            callback = null;
        }
    }

    public BatalhaNavalServer getServidor() { return servidor; }
    public void setServidor(BatalhaNavalServer servidor) { this.servidor = servidor; }

    public String getNomeJogador() { return nomeJogador; }
    public void setNomeJogador(String nomeJogador) { this.nomeJogador = nomeJogador; }

    public int getIdJogador() { return idJogador; }
    public void setIdJogador(int idJogador) { this.idJogador = idJogador; }

    public ClienteCallbackImpl getCallback() { return callback; }
    public void setCallback(ClienteCallbackImpl callback) { this.callback = callback; }

    public static void main(String[] args) {
        launch(args);
    }
}