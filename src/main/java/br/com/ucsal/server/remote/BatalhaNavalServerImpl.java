package br.com.ucsal.server.remote;

import br.com.ucsal.common.exception.JogadaInvalidaException;
import br.com.ucsal.common.exception.NaoESuaVezException;
import br.com.ucsal.common.exception.PartidaIndisponivelException;
import br.com.ucsal.common.model.*;
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
    public void posicionarNavio(int id, TipoNavio tipoNavio, Posicao inicial, Direcao direcao) throws
            RemoteException, JogadaInvalidaException, PartidaIndisponivelException {

        boolean comecou = partida.posicionarNavio(id, tipoNavio, inicial, direcao);

        if (comecou) {
            callbacks.get(0).partidaIniciada(partida.getNome(1));
            callbacks.get(1).partidaIniciada(partida.getNome(0));
            callbacks.get(partida.getDeQuemVez()).suaVez();
        }
    }

    @Override
    public ResultadoTiro fazerJogada(int id, Posicao tiro) throws
            RemoteException, JogadaInvalidaException, NaoESuaVezException, PartidaIndisponivelException {
        ResultadoTiro resultado = partida.atirar(id, tiro);
        int adversario = 1 - id;

        callbacks.get(adversario).tiroRecebido(tiro, resultado);

        if(partida.isEncerrada()) {
            callbacks.get(0).fimDeJogo(partida.getNome(id), MotivoFim.AFUNDOU_EMBARCACOES);
            callbacks.get(1).fimDeJogo(partida.getNome(id), MotivoFim.AFUNDOU_EMBARCACOES);
        } else {
            callbacks.get(adversario).suaVez();
        }

        return resultado;
    }
}
