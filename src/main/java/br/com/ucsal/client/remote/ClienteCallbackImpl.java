package br.com.ucsal.client.remote;

import br.com.ucsal.common.model.MotivoFim;
import br.com.ucsal.common.model.Posicao;
import br.com.ucsal.common.model.ResultadoTiro;
import br.com.ucsal.common.remote.ClienteCallback;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClienteCallbackImpl extends UnicastRemoteObject implements ClienteCallback {
    public ClienteCallbackImpl() throws RemoteException {
        super();
    }

    @Override
    public void partidaIniciada(String nomeAdversario) throws RemoteException {
        System.out.println("A partida começou! Seu adversário é " + nomeAdversario);
    }

    @Override
    public void suaVez() throws RemoteException {
        System.out.println("Sua vez de jogar");
    }

    @Override
    public void tiroRecebido(Posicao posicao, ResultadoTiro resultadoTiro) throws RemoteException {
        System.out.println("O adversário atirou em " + posicao + ": " + resultadoTiro);
    }

    @Override
    public void fimDeJogo(String nomeVencedor, MotivoFim motivoFim) throws RemoteException {
        System.out.println("Fim de jogo! Vencedor: " + nomeVencedor + " (" + motivoFim + ")");
    }
}
