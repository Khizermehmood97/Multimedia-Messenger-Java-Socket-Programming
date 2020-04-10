/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ChatServer;

import com.sun.xml.internal.ws.util.StringUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.*;
import java.io.*;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Khizer Mehmood
 */
public class ServerWorker extends Thread {

    private final Socket clientSocket;
    private final Server server;
    private String loginId = null;
    private HashSet<String> topicSet = new HashSet<>();
   // private InputStream is;
    private OutputStream os;
    private FileInputStream fi;
    private FileOutputStream fo;

    public ServerWorker(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    public void run() {
        try {
            handleClientSocket();
        } catch (InterruptedException ex) {
            Logger.getLogger(ServerWorker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ServerWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void handleClientSocket() throws InterruptedException, IOException {
        InputStream is = clientSocket.getInputStream();
        this.os = clientSocket.getOutputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String Msg1 = "Enter Username and Passsword:\nEg: Login username password\n\n";
        os.write(Msg1.getBytes());
        String line;
        while ((line = reader.readLine()) != null ) {
            String[] tokens = line.split(" ");
            if (tokens != null && tokens.length > 0) {
                String cmd = tokens[0];
                if (cmd.equalsIgnoreCase("quit") || cmd.equalsIgnoreCase("logout")) {
                    handleLogout();
                    break;
                } else if (cmd.equalsIgnoreCase("login")) {
                    handleLogin(os, tokens);
                } else if (cmd.equalsIgnoreCase("msg")) {
                    String[] tokensmsg = line.split(" ", 3);
                    handleMessage(tokensmsg);
                } else if (cmd.equalsIgnoreCase("join")) {
                    handleJoin(tokens);
                } else if (cmd.equalsIgnoreCase("leave")) {
                    handleLeave(tokens);
                } else if (cmd.equalsIgnoreCase("file")) {
                    handleFile(tokens);
                }
                else {
                    String message = "Unknown Command: " + cmd + "\n";
                    os.write(message.getBytes());
                }
            }
        }
        clientSocket.close();
    }

    public String getLogin() {
        return loginId;
    }

    private void handleFile(String[] tokens) throws FileNotFoundException, IOException {
        String sendTo = tokens[1]; 
        String Fname = tokens[2];
        
                this.fi = new FileInputStream ("E:\\"+Fname+".txt");
                byte b[] = new byte[2002];
                fi.read(b,0,b.length);
                String msgg = "File: '"+Fname+ "' -- sent to: "+sendTo;
                this.os.write(msgg.getBytes());
                
                List<ServerWorker> workerList = server.getWorkerList();  
               
               for (ServerWorker worker : workerList) {
                     if (sendTo.equalsIgnoreCase(worker.getLogin())) {
                         worker.fo = new FileOutputStream ("E:\\"+Fname+"0.txt");
                         worker.fo.write(b, 0, b.length);
                         String ms = "File: '"+Fname+ "' -- Recieved in E Directory";
                         worker.os.write(ms.getBytes());
                     }
                 }
                
                
    }
    
    private void handleLogin(OutputStream os, String[] tokens) throws IOException {
        
        if (tokens.length == 3) {
            String loginId = tokens[1];
            String password = tokens[2];

            if ((loginId.equalsIgnoreCase("guest") && password.equals("guest"))
                    || (loginId.equalsIgnoreCase("khizer") && password.equals("khizer"))
                    || (loginId.equalsIgnoreCase("demo") && password.equals("demo"))) {

                this.loginId = loginId;
                String message = "User Logged in Successfully: " + loginId + "\n";
              //  String message = "ok login\n";
                os.write(message.getBytes());
                System.out.println("User Logged in Successfully: " + loginId);

                List<ServerWorker> workerList = server.getWorkerList();

                //send to current user all other users online logins
                for (ServerWorker worker : workerList) {
                    if (worker.getLogin() != null) {
                        if (!loginId.equals(worker.getLogin())) {
                            String Msg = "Online " + worker.getLogin() + "\n";
                            send(Msg);
                        }
                    }

                }
                //send to other online users the current user's status
                String onlineMsg = "Online " + loginId + "\n";
                for (ServerWorker worker : workerList) {
                    if (!loginId.equals(worker.getLogin())) {
                        worker.send(onlineMsg);
                    }
                }
            } else {
                String message = "Login Error : Invalid Username or Password.\n";
                os.write(message.getBytes());
                System.err.println("Login failed for "+ loginId);
            }
        }
    }

    private void send(String onlineMsg) throws IOException {
        if (loginId != null) {
            os.write(onlineMsg.getBytes());
        }
    }

    private void handleLogout() throws IOException {
        server.removeWorker(this);
        System.out.println("User Logged out Successfully: " + loginId);
        List<ServerWorker> workerList = server.getWorkerList();
        String LogoutMsg = "Offline " + loginId + "\n";
        for (ServerWorker worker : workerList) {
            if (!loginId.equals(worker.getLogin())) {
                worker.send(LogoutMsg);
            }
        }
        clientSocket.close();
    }

    //format: msg user body..  priate chat
    //format: msg #topic body..  group chat
    private void handleMessage(String[] tokens) throws IOException {
        String sendTo = tokens[1];
        String body = tokens[2];

        boolean isTopic = sendTo.charAt(0) == '#';

        List<ServerWorker> workerList = server.getWorkerList();

        for (ServerWorker worker : workerList) {
            if (isTopic) {
                if( (worker.isMemberOfTopic(sendTo)) && (this.isMemberOfTopic(sendTo)) && (!loginId.equals(worker.getLogin()))) {
                    String outMsg = "--Group Message--\n" +"Group Name: "+ sendTo+"\nSender: "+loginId + "\nMessage Body: " + body + "\n";
                    worker.send(outMsg);
                }

            } else {
                if (sendTo.equalsIgnoreCase(worker.getLogin())) {
                    String outMsg = "msg " + loginId + " " + body + "\n";
                    worker.send(outMsg);
                }
            }
        }

    }

    public boolean isMemberOfTopic(String topic) {
        return topicSet.contains(topic);
    }

    private void handleJoin(String[] tokens) {

        if (tokens.length > 1) {
            String topic = tokens[1];
            topicSet.add(topic);
        }
    }

    private void handleLeave(String[] tokens) {
    if (tokens.length > 1) {
            String topic = tokens[1];
            topicSet.remove(topic);
        }
    }

    

}
