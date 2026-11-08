package br.com.ucsal.client;

import br.com.ucsal.common.model.MotivoFim;
import br.com.ucsal.common.model.Posicao;
import br.com.ucsal.common.model.ResultadoTiro;
import br.com.ucsal.common.remote.ClienteCallback;
import javafx.application.Platform;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.Consumer;

/**
 * Objeto remoto do CLIENTE: é ele que o servidor chama para dar avisos.
 *
 * - extends UnicastRemoteObject: ao ser criado, o objeto é "exportado",
 *   ou seja, o RMI abre uma porta neste processo para receber chamadas.
 * - implements ClienteCallback: o contrato definido no backend.
 *
 * As chamadas do servidor chegam numa thread do RMI. Este objeto só
 * repassa cada aviso para a thread do JavaFX, com Platform.runLater.
 */
public class ClienteCallbackImpl extends UnicastRemoteObject implements ClienteCallback {

    // volatile: escrito pela thread do JavaFX e lido pelas threads do RMI.
    // Garante que a thread do RMI enxergue sempre o valor mais recente.
    private volatile EventosDoJogo ouvinte;

    public ClienteCallbackImpl() throws RemoteException {
        super(); // exporta o objeto
    }

    /** Define qual tela recebe os avisos (troca a cada mudança de tela). */
    public void setOuvinte(EventosDoJogo ouvinte) {
        this.ouvinte = ouvinte;
    }

    // ---- Métodos chamados PELO SERVIDOR, pela rede ----
    // O nome "pastidaIniciada" precisa ser igual ao da interface ClienteCallback.

    @Override
    public void pastidaIniciada(String nomeAdversario) throws RemoteException {
        entregar(o -> o.partidaIniciada(nomeAdversario));
    }

    @Override
    public void suaVez() throws RemoteException {
        entregar(o -> o.suaVez());
    }

    @Override
    public void tiroRecebido(Posicao posicao, ResultadoTiro resultadoTiro) throws RemoteException {
        entregar(o -> o.tiroRecebido(posicao, resultadoTiro));
    }

    @Override
    public void fimDeJogo(String nomeVencedor, MotivoFim motivoFim) throws RemoteException {
        entregar(o -> o.fimDeJogo(nomeVencedor, motivoFim));
    }

    /**
     * Agenda o aviso na thread do JavaFX.
     * runLater NÃO espera: devolve na hora, e o servidor não fica travado
     * esperando a tela se atualizar.
     */
    private void entregar(Consumer<EventosDoJogo> aviso) {
        Platform.runLater(() -> {
            EventosDoJogo atual = ouvinte;
            if (atual != null) {
                aviso.accept(atual);
            }
        });
    }
}