package br.com.ucsal.common.remote;

import br.com.ucsal.common.model.MotivoFim;
import br.com.ucsal.common.model.Posicao;
import br.com.ucsal.common.model.ResultadoTiro;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClienteCallback extends Remote {

    void partidaIniciada(String nomeAdversario) throws RemoteException;

    void suaVez() throws RemoteException;

    void tiroRecebido(Posicao posicao, ResultadoTiro resultadoTiro) throws RemoteException;

    void fimDeJogo(String nomeVencedor, MotivoFim motivoFim) throws RemoteException;
}
