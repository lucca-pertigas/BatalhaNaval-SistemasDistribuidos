package br.com.ucsal.server.remote;

import br.com.ucsal.common.exception.JogadaInvalidaException;
import br.com.ucsal.common.exception.NaoESuaVezException;
import br.com.ucsal.common.exception.PartidaIndisponivelException;
import br.com.ucsal.common.model.Direcao;
import br.com.ucsal.common.model.Posicao;
import br.com.ucsal.common.model.ResultadoTiro;
import br.com.ucsal.common.model.TipoNavio;
import br.com.ucsal.common.remote.BatalhaNavalServer;
import br.com.ucsal.common.remote.ClienteCallback;
import br.com.ucsal.server.domain.Partida;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BatalhaNavalServerImpl extends UnicastRemoteObject implements BatalhaNavalServer{
    private final Partida partida = new Partida();
    private final Map<Integer, ClienteCallback> callbacks = new ConcurrentHashMap<>();

    public BatalhaNavalServerImpl() throws RemoteException {
        super();
    }

    @Override
    public String retornarString() throws RemoteException {
        System.out.println("Chamada Recebida");
        return "Ola mundo";
    }

    @Override
    public int entrarNaPartida(String nome, ClienteCallback clienteCallback) throws
            RemoteException, PartidaIndisponivelException {
        int id = partida.entrarNaPartida(nome);
        callbacks.put(id, clienteCallback);
        return id;
    }

    @Override
    public void posicionarNavio(int id, TipoNavio tipoNavio, Posicao inicial, Direcao direcao) throws RemoteException, JogadaInvalidaException {

    }

    @Override
    public ResultadoTiro fazerJogada(int id, Posicao tiro) throws RemoteException, JogadaInvalidaException, NaoESuaVezException, PartidaIndisponivelException {
        return null;
    }
}
