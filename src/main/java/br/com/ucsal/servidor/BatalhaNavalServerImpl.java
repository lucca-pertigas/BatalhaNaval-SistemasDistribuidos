package br.com.ucsal.servidor;

import br.com.ucsal.comum.BatalhaNavalServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class BatalhaNavalServerImpl extends UnicastRemoteObject implements BatalhaNavalServer{

    public BatalhaNavalServerImpl() throws RemoteException {
        super();
    }

    @Override
    public String retornarString() throws RemoteException {
        System.out.println("Chamada Recebida");
        return "Ola mundo";
    }
}
