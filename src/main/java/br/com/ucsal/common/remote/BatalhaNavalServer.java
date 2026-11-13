package br.com.ucsal.common.remote;

import br.com.ucsal.common.exception.JogadaInvalidaException;
import br.com.ucsal.common.exception.NaoESuaVezException;
import br.com.ucsal.common.exception.PartidaIndisponivelException;
import br.com.ucsal.common.model.Direcao;
import br.com.ucsal.common.model.Posicao;
import br.com.ucsal.common.model.ResultadoTiro;
import br.com.ucsal.common.model.TipoNavio;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BatalhaNavalServer extends Remote {
    String retornarString() throws RemoteException;

    int entrarNaPartida(String nome, ClienteCallback clienteCallback) throws RemoteException,
            PartidaIndisponivelException;

    void posicionarNavio(int id, TipoNavio tipoNavio, Posicao inicial, Direcao direcao) throws RemoteException,
            JogadaInvalidaException, PartidaIndisponivelException;

    ResultadoTiro fazerJogada(int id, Posicao tiro) throws RemoteException,
            JogadaInvalidaException, NaoESuaVezException, PartidaIndisponivelException;

    void sairDaPartida(int id, ClienteCallback clienteCallback) throws RemoteException;
}