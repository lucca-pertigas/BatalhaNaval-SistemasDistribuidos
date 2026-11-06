package br.com.ucsal.comum;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BatalhaNavalServer extends Remote {
    String retornarString() throws RemoteException;
}
