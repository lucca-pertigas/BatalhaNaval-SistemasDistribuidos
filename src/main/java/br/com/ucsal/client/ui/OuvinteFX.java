package br.com.ucsal.client.ui;

import br.com.ucsal.client.OuvinteDoJogo;
import br.com.ucsal.common.model.MotivoFim;
import br.com.ucsal.common.model.Posicao;
import br.com.ucsal.common.model.ResultadoTiro;
import javafx.application.Platform;

import java.util.function.Consumer;

/**
 * Adaptador entre o callback RMI (ClienteCallbackImpl, feito pela dupla)
 * e as telas JavaFX.
 *
 * O ClienteCallbackImpl chama estes métodos numa THREAD DO RMI.
 * Tela só pode ser alterada na THREAD DO JAVAFX. Então cada aviso é
 * reagendado com Platform.runLater e entregue à tela aberta no momento.
 */
public class OuvinteFX implements OuvinteDoJogo {

    // volatile: escrito pela thread do JavaFX (troca de tela),
    // lido pelas threads do RMI. Garante que todos vejam o valor atual.
    private volatile OuvinteDoJogo telaAtual;

    /** Cada tela que quer receber avisos se registra aqui ao abrir. */
    public void setTelaAtual(OuvinteDoJogo tela) {
        this.telaAtual = tela;
    }

    @Override
    public void partidaIniciada(String nomeAdversario) {
        entregar(tela -> tela.partidaIniciada(nomeAdversario));
    }

    @Override
    public void suaVez() {
        entregar(tela -> tela.suaVez());
    }

    @Override
    public void tiroRecebido(Posicao posicao, ResultadoTiro resultadoTiro) {
        entregar(tela -> tela.tiroRecebido(posicao, resultadoTiro));
    }

    @Override
    public void fimDeJogo(String nomeVencedor, MotivoFim motivoFim) {
        entregar(tela -> tela.fimDeJogo(nomeVencedor, motivoFim));
    }

    /**
     * Agenda o aviso na thread do JavaFX.
     * A tela é lida DENTRO do runLater (na hora de executar), e não antes:
     * assim, se uma tela trocar para outra e logo depois chegar um aviso,
     * ele vai para a tela NOVA.
     * runLater não espera: o servidor não fica travado aguardando a tela.
     */
    private void entregar(Consumer<OuvinteDoJogo> aviso) {
        Platform.runLater(() -> {
            OuvinteDoJogo tela = telaAtual;
            if (tela != null) {
                aviso.accept(tela);
            }
        });
    }
}