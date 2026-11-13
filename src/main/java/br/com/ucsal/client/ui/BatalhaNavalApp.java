package br.com.ucsal.client.ui;

import br.com.ucsal.client.OuvinteDoJogo;
import br.com.ucsal.client.remote.ClienteCallbackImpl;
import br.com.ucsal.common.remote.BatalhaNavalServer;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.rmi.NoSuchObjectException;
import java.rmi.server.UnicastRemoteObject;

public class BatalhaNavalApp extends Application {

    public static final int TAMANHO_TABULEIRO = 10;

    private Scene cena;

    private BatalhaNavalServer servidor;
    private String nomeJogador;
    private int idJogador = -1;
    private ClienteCallbackImpl callback;

    private final OuvinteFX ouvinteFX = new OuvinteFX();

    @Override
    public void start(Stage palco) {
        cena = new Scene(new TelaConexao(this).getRaiz(), 1100, 720);
        cena.getStylesheets().add(
                getClass().getResource("estilo.css").toExternalForm());

        palco.setTitle("Batalha Naval");
        palco.setMinWidth(960);
        palco.setMinHeight(620);
        palco.setScene(cena);
        palco.show();
    }

    @Override
    public void stop() {
        if (servidor != null && callback != null && idJogador >= 0) {
            try {
                servidor.sairDaPartida(idJogador, callback);
            } catch (Exception ignorado) {
            }
        }
        liberarCallback();
        System.exit(0);
    }

    public void mostrarTela(Parent raiz) {
        cena.setRoot(raiz);
    }

    public void ouvirAvisos(OuvinteDoJogo tela) {
        ouvinteFX.setTelaAtual(tela);
    }

    public void liberarCallback() {
        ouvinteFX.setTelaAtual(null);
        if (callback != null) {
            try {
                UnicastRemoteObject.unexportObject(callback, true);
            } catch (NoSuchObjectException ignorado) {
            }
            callback = null;
        }
    }

    public OuvinteFX getOuvinteFX() { return ouvinteFX; }

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