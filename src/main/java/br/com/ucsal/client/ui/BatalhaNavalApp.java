package br.com.ucsal.client.ui;

import br.com.ucsal.common.remote.BatalhaNavalServer;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Aplicação JavaFX do cliente.
 *
 * Usamos UMA janela (Stage) e UMA cena (Scene). Para trocar de tela,
 * trocamos só a "raiz" da cena com setRoot(). Assim o tamanho da janela
 * e o CSS continuam valendo.
 *
 * Esta classe também guarda o que as telas precisam compartilhar:
 * o stub do servidor e o nome do jogador.
 */
public class BatalhaNavalApp extends Application {

    private Scene cena;

    // Stub RMI: objeto LOCAL que representa o servidor REMOTO.
    // Chamar um método nele = enviar uma requisição pela rede.
    private BatalhaNavalServer servidor;
    private String nomeJogador;

    @Override
    public void start(Stage palco) {
        cena = new Scene(new TelaConexao(this).getRaiz(), 960, 640);

        // Procura o estilo.css no MESMO pacote desta classe
        // (por isso criamos resources/br/com/ucsal/client/ui).
        cena.getStylesheets().add(
                getClass().getResource("estilo.css").toExternalForm());

        palco.setTitle("Batalha Naval");
        palco.setMinWidth(720);
        palco.setMinHeight(520);
        palco.setScene(cena);
        palco.show();
    }

    /** Troca a tela exibida na janela. */
    public void mostrarTela(Parent raiz) {
        cena.setRoot(raiz);
    }

    public BatalhaNavalServer getServidor() {
        return servidor;
    }

    public void setServidor(BatalhaNavalServer servidor) {
        this.servidor = servidor;
    }

    public String getNomeJogador() {
        return nomeJogador;
    }

    public void setNomeJogador(String nomeJogador) {
        this.nomeJogador = nomeJogador;
    }

    public static void main(String[] args) {
        launch(args);
    }
}