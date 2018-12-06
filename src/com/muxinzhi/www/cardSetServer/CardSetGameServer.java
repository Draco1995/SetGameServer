package com.muxinzhi.www.cardSetServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.HashMap;
import java.util.Queue;
import java.util.Random;
import  java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class CardSetGameServer implements Runnable{
	
	static int serverNumbers = 0;
	final int serverNumber;
	String initialGame = null;
	Object lock = new Object();
	int playerNumber = 0;
	class SocketMessage{
		Socket socket;
		String message;
		SocketMessage(Socket s,String m){
			socket = s;
			message = m;
		}
	}
	BlockingQueue<SocketMessage> messageQueue = new LinkedBlockingQueue<SocketMessage>();
	public Queue<Socket> localSocketList = new ConcurrentLinkedQueue<Socket>();
	ConcurrentHashMap<Socket, Integer> map = new ConcurrentHashMap<Socket,Integer>();
	
	
	Random random = new Random(42);
    boolean[] hash = new boolean[81];
    boolean[] taken = new boolean[81];
	
    private volatile boolean status = false; 
    
    GameStarter gs;
    class GameStarter implements Runnable{
    	private int playersReady = 0;
		@Override
		public void run() {
			synchronized(lock){
				while(true){
					Boolean flag;
					int number;
					synchronized(this){
						flag = playersReady>=2&&playersReady==localSocketList.size();
						number = localSocketList.size()-playersReady;
					}
					if(flag){
						status = true;
						for(int i=5;i>=1;i--){
							sendMessage("System:The game will be started in "+i);
							try {
								TimeUnit.SECONDS.sleep(1);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						sendMessage("System:Go!");
						break;
					}else{
						try {
							String s = "System:Waiting for players. "+localSocketList.size()+
									" players(s) in all. "+number+" player(s) not ready";
							sendMessage(s);
							TimeUnit.MILLISECONDS.sleep(500);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
		public void ready(){
			synchronized(this){
				playersReady++;
			}
		}
    	
    }
    
	CardSetGameServer(Socket socket){
		addSocket(socket);
		serverNumbers++;
		serverNumber = serverNumbers;
		gs = new GameStarter();
		new Thread(gs).start();
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true){
			try {
				SocketMessage s = messageQueue.take();
				handleMessage(s);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
			
		}
		
	}
	
	private void handleMessage(SocketMessage s){
		System.out.println(map.get(s.socket)+": "+s.message);
		//TODO
		
		String[] msg = s.message.split(":");
		
		msg = msg[1].split(",");
		
		Class<?> classType = this.getClass(); 
		Method method;
		try {
			System.out.println(msg[0]);
			method = CardSetGameServer.class.getDeclaredMethod(msg[0],SocketMessage.class);
			method.invoke(this, s);
			
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		
		
	}
	
	private void sendMessage(String msg){
		for(Socket s:localSocketList){
			OutputStream outputStream;  
            try {  
                outputStream = s.getOutputStream();    
                outputStream.write((msg+"\n").getBytes("utf-8"));
                System.out.println("Sent to All"+":"+msg);
            }catch (IOException e){  
                e.printStackTrace();
            } 
		}
	}
	
	private void sendMessageExcept(Socket socket,String msg){
		for(Socket s:localSocketList){
			if(s.equals(socket)){
				continue;
			}
			OutputStream outputStream;  
            try {  
                outputStream = s.getOutputStream();    
                outputStream.write((msg+"\n").getBytes("utf-8"));
                System.out.println("Sent to all except "+map.get(socket)+":"+msg);
            }catch (IOException e){  
                e.printStackTrace();
            } 
		}
	}
	
	private void sendMessage(Socket s,String msg){
		OutputStream outputStream;  
        try {  
            outputStream = s.getOutputStream();    
            outputStream.write((msg+"\n").getBytes("utf-8")); 
            System.out.println("Sent to Socket "+map.get(s)+":"+msg);
        }catch (IOException e){  
            e.printStackTrace();
        } 
	}
	
	private void initialGame(SocketMessage s){
		if(status==true){
			sendMessage(s.socket,"invalid request");
			System.out.println("Error in initialGame");
		}
		String[] msg = s.message.split(":");
		if(initialGame==null){
			String cn = msg[1].split(",")[1];
			int[] numbers = getNewValidCards(Integer.valueOf(cn));
			String res = ""+numbers[0];
			for(int i = 1;i<numbers.length;i++){
				res+=","+numbers[i];
			}
			initialGame = res;
		}
		String res = msg[0]+":"+initialGame;
		gs.ready();
		new Thread(){
			public void run(){
				synchronized(lock){
					sendMessage(s.socket,res);
				}
			}
		}.start();
	}
	
	private void requestConnection(SocketMessage s){
		String[] msg = s.message.split(":");
		sendMessage(s.socket,msg[0]+":"+localSocketList.size()+","+map.get(s.socket));
	}
	
	private void requestRemoval(SocketMessage s){
		String[] msg = s.message.split(":");
		String[] numbers = msg[1].split(",");
		int[] num = new int[3];
		num[0] = Integer.valueOf(numbers[1]);
		num[1] = Integer.valueOf(numbers[2]);
		num[2] = Integer.valueOf(numbers[3]);
		if(!taken[num[0]]&&!taken[num[1]]&&!taken[num[2]]){
			int[] newCards = getNewValidCards(3);
			String res = msg[0];
			String nc = ""+newCards[0];
			for(int i = 1;i<newCards.length;i++){
				nc+=","+newCards[i];
			}
			for(int i = 4;i<=6;i++){
				nc+=","+numbers[i];
			}
			sendMessage(s.socket,res+":"+nc);
			sendMessageExcept(s.socket,"SCORE:"+map.get(s.socket)+" "+nc);
		}else{
			sendMessage(s.socket,msg[0]+":"+"refused");
		}
	}
	
	private int[] getNewValidCards(int cn) {
        int[] numbers = new int[cn];
        int count = 0;
        while(count<cn){
            int ran = random.nextInt(81);
            while(hash[ran%81]==true){
                ran++;
            }
            hash[ran%81] = true;
            numbers[count] = ran;
            count++;
        }
        return numbers;
    }
	
	
	
	synchronized public boolean tryAddSocket(Socket socket){
		if(status == false){
			addSocket(socket);
			return true;
		}else{
			return false;
		}
	}
	
	class SocketListener implements Runnable{
		
		Socket socket = null;
		BufferedReader buf = null;
		
		SocketListener(Socket s){
			socket = s;
			 try {  
				 buf = new BufferedReader(new InputStreamReader(socket.getInputStream()));  
	         }catch (IOException e){  
	        	 e.printStackTrace();  
	         } 
		}
		
		@Override
		public void run() {
			String content = null;  
            while ((content = readFromClient()) != null){
            	//System.out.println(content);
            	messageQueue.add(new SocketMessage(socket,content));
            }  
			
		}
		
		public String readFromClient() {    
            try {    
                return buf.readLine();    
            } catch (Exception e) {    
                e.printStackTrace();    
            }    
            return null;    
        } 
		
	}
	
	synchronized private void addSocket(Socket s){
		localSocketList.add(s);
		map.put(s, playerNumber);
		playerNumber++;
		new Thread(new SocketListener(s)).start();
	}
}
