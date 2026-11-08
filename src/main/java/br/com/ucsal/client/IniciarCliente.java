package br.com.ucsal.client;

import br.com.ucsal.common.remote.BatalhaNavalServer;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class IniciarCliente {
    public static void main(String[] args) throws Exception{

        Registry atualRegistro = LocateRegistry.getRegistry("localhost", 1099);

        BatalhaNavalServer batalhaNavalServer = (BatalhaNavalServer) atualRegistro.lookup("BatalhaNaval");

        System.out.println(batalhaNavalServer.retornarString());


    }
}
