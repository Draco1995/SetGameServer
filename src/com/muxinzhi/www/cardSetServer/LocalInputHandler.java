package com.muxinzhi.www.cardSetServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue; 
public class LocalInputHandler implements Runnable{
	Queue<Socket> socketList;
	BufferedReader systemReader;
	ServerSocket server;
	LocalInputHandler(Queue<Socket> socketList,BufferedReader systemReader, ServerSocket s){
		this.socketList = socketList;
		this.systemReader = systemReader;
		server = s;
	}
	public void run(){
		systemReader = new BufferedReader(new InputStreamReader(System.in));
		String str;
		try {
			System.out.print(">>>");
			str = systemReader.readLine();
			while(!str.equals("exit")){
				if(str.equals("inf")){
					System.out.println("Now "+socketList.size()+" online");
				}else{
					for (Socket socket:cardSetServer.socketList) {  
	                    OutputStream outputStream;  
	                    try {  
	                        outputStream = socket.getOutputStream();    
	                        outputStream.write((str+"\n").getBytes("utf-8"));   
	                    }catch (IOException e){  
	                        e.printStackTrace();
	                    }  
	                }
				}
				System.out.print(">>>");
				str = systemReader.readLine();
    		}
			for (Socket socket:cardSetServer.socketList) { 
				socket.close();
			}
			systemReader.close();
			server.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
}
