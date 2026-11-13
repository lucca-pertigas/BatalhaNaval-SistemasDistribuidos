package br.com.ucsal.client.remote;

import br.com.ucsal.client.OuvinteDoJogo;
import br.com.ucsal.common.model.MotivoFim;
import br.com.ucsal.common.model.Posicao;
import br.com.ucsal.common.model.ResultadoTiro;
import br.com.ucsal.common.remote.ClienteCallback;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClienteCallbackImpl extends UnicastRemoteObject implements ClienteCallback {
    private final OuvinteDoJogo ouvinte;

    public ClienteCallbackImpl(OuvinteDoJogo ouvinte) throws RemoteException {
        super();
        this.ouvinte = ouvinte;
    }

    @Override
    public void partidaIniciada(String nomeAdversario) throws RemoteException {
        ouvinte.partidaIniciada(nomeAdversario);
    }

    @Override
    public void suaVez() throws RemoteException {
        ouvinte.suaVez();
    }

    @Override
    public void tiroRecebido(Posicao posicao, ResultadoTiro resultadoTiro) throws RemoteException {
        ouvinte.tiroRecebido(posicao, resultadoTiro);
    }

    @Override
    public void fimDeJogo(String nomeVencedor, MotivoFim motivoFim) throws RemoteException {
        ouvinte.fimDeJogo(nomeVencedor, motivoFim);
    }

    @Override
    public void ping() throws RemoteException {
    }
}