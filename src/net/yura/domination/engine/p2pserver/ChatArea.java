package net.yura.domination.engine.p2pserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import net.yura.domination.engine.RiskController;

/**
 * <p> Chat Area </p>
 * @author Yura Mamyrin
 */

public class ChatArea extends Thread {
    private ServerSocket serverSocket = null;
    private RiskController gui;
    // You could make this more dynamic, but it's a little
    // simpler to keep the simple array approach
    private ChatServerThread chatArr[]= new ChatServerThread[100];
    private boolean stopFlag = false;

    public ChatArea(RiskController g,int port) throws Exception {
        gui = g;
        InetAddress iaddr = InetAddress.getLocalHost();
        serverSocket = new ServerSocket(port);
        gui.sendMessage("getHostName = " + iaddr.getHostName() , false, false);
        gui.sendMessage("getHostAddress = " + iaddr.getHostAddress() , false, false);
        gui.sendMessage("port = " + port , false, false);
        start();
    }
    public void run() {
        Socket nextSock;
        int nThreadCount=0;
        ChatServerThread childThread;
        try {
            while(true) {
                nextSock = serverSocket.accept();
                gui.sendMessage( "Another Client has joined: " + nThreadCount, false, false);
                chatArr[nThreadCount] = childThread = new ChatServerThread(nextSock, this, nThreadCount++);
                if (childThread != null) childThread.start();
            }
        }
        catch (IOException e) {
            System.out.println("error");
        }
        gui.sendMessage("no one can join now",false,false);
    }
    public void closeSocket() throws IOException {
        synchronized(this) {
            serverSocket.close();
            stopFlag = true;
            notifyAll();
        }
    }
    public boolean isOff() {
        return serverSocket.isClosed();
    }
    // Add a new string to all linked lists
     void putString(int index, String s) {
        synchronized(this) {
            for (int i = 0; i < chatArr.length; i++)
                if (chatArr[i] != null)
                    chatArr[i].m_lList.addLast(s);
            notifyAll();
            // kill the serverSocket so noone can join the game now
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("error");
            }
        }
    }

    // called to get the list of strings awaiting any given
    // thread
    String getStrings(int index) {
        if (chatArr[index]==null) return null;
        int i, num;
        String str;
        StringBuffer sb = new StringBuffer("");
        LinkedList lList = chatArr[index].m_lList;
        num=lList.size();
        try {
            for (i=0; i < num; i++) {
                str = (String)lList.removeFirst();
                sb.append( str);
                sb.append("\n");
            }
        }
        catch (NoSuchElementException e) {
            System.err.println("Our List Count is Messed Up???");
        }
        return sb.toString();
    }
    // called to wait for any new messages for a given thread
    String waitForString(int index) {
        String str = null;
        int leng = str.length();
        synchronized(this) {
            do {
                str = getStrings(index);
                synchronized(str) {
                    if (str == null) return null;
                }
                try {
                    if (str.length() == 0)
                        wait();
                } catch (InterruptedException e) {
                    System.out.println("error");
                }
                if (stopFlag)
                    return null;
                leng = str.length();
            } while (leng == 0);
            return str;
        }
    }
    void imDead(int index) {
        synchronized(this) {
            chatArr[index] = null;
            notifyAll();
        }
    }
}
