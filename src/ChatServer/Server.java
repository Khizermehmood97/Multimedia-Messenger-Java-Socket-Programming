/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ChatServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Khizer Mehmood
 */
public class Server extends Thread {

    private final int serverPort;
    private ArrayList<ServerWorker> workerList = new ArrayList();

    Server(int serverPort) {
        this.serverPort = serverPort;
    }
    
    public List<ServerWorker> getWorkerList() {
        return workerList;
    }
    
    public void run (){
        try {
            ServerSocket ss = new ServerSocket(serverPort);
            while (true) {
            System.out.println("Server is waiting for Client connection: ");
            Socket clientSocket = ss.accept();
            System.out.println("Connection Established with "+ clientSocket);
            ServerWorker worker = new ServerWorker(this,clientSocket);
            workerList.add(worker);
            worker.start();
            }
                     
        } 
        catch (IOException ex) {
            Logger.getLogger(ServerMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void removeWorker(ServerWorker aThis) {
        workerList.remove(aThis);
    }
    
}
