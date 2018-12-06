package com.muxinzhi.www.cardSetServer;

import java.io.BufferedReader;    
import java.io.IOException;    
import java.io.InputStreamReader;    
import java.io.OutputStream;    
import java.net.Socket;     
import java.net.ServerSocket;   
import java.util.ArrayList;    
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class cardSetServer {
	static BufferedReader systemReader = null;
	static CardSetGameServer gameServer = null;
    public static Queue<Socket> socketList = new ConcurrentLinkedQueue<Socket>();  
    public static void main(String[] args){  
        ServerSocket server;
		try {
			server = new ServerSocket(9120);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
        new Thread(new LocalInputHandler(socketList,systemReader,server)).start();
        System.out.println("start listening port 9120."); 
        while (true){  
            Socket socket;
			try {
				socket = server.accept();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Server closed.");
				return;
			}  
            socketList.add(socket);  
            System.out.println("connect succeed. Now "+ socketList.size()+ " online");
            if(gameServer==null || gameServer.tryAddSocket(socket)==false){
            	gameServer = new CardSetGameServer(socket);
            	new Thread(gameServer).start();
            	
            }
            //new Thread(new MyServerRunnable(socket)).start();   
        }  
    }  
}
