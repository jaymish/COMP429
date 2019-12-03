package prakash.ram.model;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;


public class dv {
	
	
	static int time;
	
	public static List<SocketChannel> openChannels = new ArrayList<>();
	public static Selector read;
	public static Selector write;
	static String myIP = "";
	static int myID = Integer.MIN_VALUE+2;
	public static Node myNode = null;
	//public static Map<Node,Integer> routingTable = new HashMap<Node,Integer>();
	public static List<Node> nodes = new ArrayList<Node>();
	public static List<String> routingTableMessage = new ArrayList<String>();
	public static Map<Node,Integer> routingTable = new HashMap<Node,Integer>();
	public static Set<Node> neighbors = new HashSet<Node>();
	public static int numberOfPacketsReceived = 0;
	public static Map<Node,Node> nextHop = new HashMap<Node,Node>();
 	public static void main(String[] args) throws IOException{
		
		read = Selector.open();
		write = Selector.open();
		Server server = new Server(2000);
		server.start();
		System.out.println("Server started running...");
		Client client = new Client();
		client.start();
		System.out.println("Client started running...");
		myIP = getMyLanIP();
		
		Timer timer = new Timer();
		Scanner in = new Scanner(System.in);
		boolean run = true;
		boolean serverCommandInput = false;
		while(run) {
			System.out.println("\n");
			System.out.println("*********Distance Vector Routing Protocol**********");
			System.out.println("Help Menu");
			System.out.println("--> Commands you can use");
			System.out.println("1. server -t <topology-file> -i <time-interval-in-seconds>");
			System.out.println("2. update <server-id1> <server-id2> <new-cost>");
			System.out.println("3. step");
			System.out.println("4. display");
			System.out.println("5. disable <server-id>");
			System.out.println("6. crash");
			String line = in.nextLine();
			String[] arguments = line.split(" ");
			String command = arguments[0];
			switch(command) {
			case "server": //server -t <topology-file-name> -i <routing-update-interval>
				if(arguments.length!=5){
					System.out.println("Incorrect command. Please try again.");
					break;
				}
				try{
				if(Integer.parseInt(arguments[4])<15){
					System.out.println("Please input routing update interval above 15 seconds.");
					break;
				}
				}catch(NumberFormatException nfe){
					System.out.println("Please input an integer for routing update interval.");
					break;
				}
				if((arguments[2]=="" || arguments[3]=="" || !arguments[3].equals("-i") || arguments[4]=="")){
					System.out.println("Incorrect command. Please try again.");
					break;
				}
				else{
					serverCommandInput = true;
					String filename = arguments[2];
					time = Integer.parseInt(arguments[4]);
					readTopology(filename);
					timer.scheduleAtFixedRate(new TimerTask(){
						@Override
						public void run() {
							try {
								step();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}, time*1000, time*1000);
				}
				break;
			case "update": //update <server-id1> <server-id2> <link Cost>
				if(serverCommandInput)
					update(Integer.parseInt(arguments[1]),Integer.parseInt(arguments[2]),Integer.parseInt(arguments[3]));
				else
					System.out.println("Please input the server command. Thank you.");
				break;
			case "step":
				if(serverCommandInput)
					step();
				else
					System.out.println("Please input the server command. Thank you.");
				break;
			case "packets":
				if(serverCommandInput)
					System.out.println("Number of packets received yet = "+numberOfPacketsReceived);
				else
					System.out.println("Please input the server command. Thank you.");
				break;
			case "display":
				if(serverCommandInput)
					display();
				else
					System.out.println("Please input the server command. Thank you.");
				break;
			case "disable":
				if(serverCommandInput){
					int id = Integer.parseInt(arguments[1]);
					Node disableServer = getNodeById(id);
					disable(disableServer);
				}
				else
					System.out.println("Please input the server command. Thank you.");
				break;
			case "crash":
				if(serverCommandInput){
					run = false;
					for(Node eachNeighbor:neighbors){
						disable(eachNeighbor);
					}
					System.out.println("Bubyee!! Thank you.");
					timer.cancel();
					System.exit(1);
				}
				else
					System.out.println("Please input the server command. Thank you.");
				break;
				default:
					System.out.println("Wrong command! Please check again.");
			}
		}
		in.close();
	}
	
	private static String getMyLanIP() {
		try {
		    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		    while (interfaces.hasMoreElements()) {
		        NetworkInterface iface = interfaces.nextElement();
		        if (iface.isLoopback() || !iface.isUp() || iface.isVirtual() || iface.isPointToPoint())
		            continue;

		        Enumeration<InetAddress> addresses = iface.getInetAddresses();
		        while(addresses.hasMoreElements()) {
		            InetAddress addr = addresses.nextElement();

		            final String ip = addr.getHostAddress();
		            if(Inet4Address.class == addr.getClass()) return ip;
		        }
		    }
		} catch (SocketException e) {
		    throw new RuntimeException(e);
		}
		return null;
	}

	public static void readTopology(String filename) {
		File file = new File("src/"+filename);
		try {
			Scanner scanner = new Scanner(file);
			int numberOfServers = scanner.nextInt();
			int numberOfNeighbors = scanner.nextInt();
			scanner.nextLine();
			for(int i = 0 ; i < numberOfServers;i++) {
				String line = scanner.nextLine();
				String[] parts = line.split(" ");
				Node node = new Node(Integer.parseInt(parts[0]),parts[1],Integer.parseInt(parts[2]));
				nodes.add(node);
				int cost = Integer.MAX_VALUE-2;
				if(parts[1].equals(myIP)) {
					myID = Integer.parseInt(parts[0]);
					myNode = node;
					cost = 0;
					nextHop.put(node, myNode);
				}
				else{
					nextHop.put(node, null);
				}
				routingTable.put(node,cost);
				connect(parts[1], Integer.parseInt(parts[2]),myID);
			}
			for(int i = 0 ; i < numberOfNeighbors;i++) {
				String line = scanner.nextLine();
				String[] parts = line.split(" ");
				int fromID = Integer.parseInt(parts[0]);int toID = Integer.parseInt(parts[1]); int cost = Integer.parseInt(parts[2]);
				if(fromID == myID){
					Node to = getNodeById(toID);
					routingTable.put(to, cost);
					neighbors.add(to);
					nextHop.put(to, to);
				}
				if(toID == myID){
					Node from = getNodeById(fromID);
					routingTable.put(from, cost);
					neighbors.add(from);
					nextHop.put(from, from);
				}
			}
			System.out.println("Reading topology done.");
			scanner.close();
		} catch (FileNotFoundException e) {
			System.out.println(file.getAbsolutePath()+" not found.");
		}
        
	}
	
	
	public static Node getNodeById(int id){
		for(Node node:nodes) {
			if(node.getId() == id) {
				return node;
			}
		}
		return null;
	}
	public static void update(int serverId1, int serverId2, int cost) throws IOException {
		if(serverId1 == myID){
			Node to = getNodeById(serverId2);
			if(isNeighbor(to)){
				routingTable.put(to, cost);
				Message message = new Message(myNode.getId(),myNode.getIpAddress(),myNode.getPort(),"update");
				message.setRoutingTable(makeMessage());
				sendMessage(to,message);
				System.out.println("Message sent to "+to.getIpAddress());
				System.out.println("Update success");
			}
			else{
				System.out.println("You can only update cost to your own neigbour!");
			}
		}
		if(serverId2 == myID){
			Node to = getNodeById(serverId1);
			if(isNeighbor(to)){
				routingTable.put(to, cost);
				Message message = new Message(myNode.getId(),myNode.getIpAddress(),myNode.getPort(),"update");
				message.setRoutingTable(makeMessage());
				sendMessage(to,message);
				System.out.println("Message sent to "+to.getIpAddress());
				System.out.println("Update success");
			}
			else{
				System.out.println("You can only update cost to your own neigbour!");
			}
		}
	}
	
	public static boolean isNeighbor(Node server){
		if(neighbors.contains(server))
			return true;
		return false;
	}
	public static List<String> makeMessage(){
		List<String> message = new ArrayList<String>();
		for (Map.Entry<Node, Integer> entry : routingTable.entrySet()) {
		    Node key = entry.getKey();
		    Integer value = entry.getValue();
		    message.add(key.getId()+"#"+value);
		}
		return message;
	}
	public static void connect(String ip, int port, int id) {
		System.out.println("Connecting to ip:- "+ip);
		try {
			if(!ip.equals(myIP)) {

				SocketChannel socketChannel = SocketChannel.open();
				socketChannel.connect(new InetSocketAddress(ip,port));
				socketChannel.configureBlocking(false);
				socketChannel.register(read, SelectionKey.OP_READ);
				socketChannel.register(write,SelectionKey.OP_WRITE);
				openChannels.add(socketChannel);
				System.out.println(".......");
				System.out.println("Connected to "+ip);
			}
			else {
				System.out.println("You cannot connect to yourself!!!");
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public static Node getNodeByIP(String ipAddress){
		for(Node node:nodes){
			if(node.getIpAddress().equals(ipAddress)){
				return node;
			}
		}
		return null;
	}
	public static void step() throws IOException{
		if(neighbors.size()>=1){
			Message message = new Message(myNode.getId(),myNode.getIpAddress(),myNode.getPort(),"step");
			message.setRoutingTable(makeMessage());
			for(Node eachNeighbor:neighbors) {
				sendMessage(eachNeighbor,message); //sending message to each neighbor
				System.out.println("Message sent to "+eachNeighbor.getIpAddress()+"!");
			}
			System.out.println("Step SUCCESS");
		}
		else{
			System.out.println("Sorry. No neighbors found to execute the step command.");
		}
		
	}
	
	public static void sendMessage(Node eachNeighbor, Message message) throws IOException{
		int semaphore = 0;
		try {
			semaphore = write.select();
			if(semaphore>0) {
				Set<SelectionKey> keys = write.selectedKeys();
				Iterator<SelectionKey> selectedKeysIterator = keys.iterator();
				ByteBuffer buffer = ByteBuffer.allocate(5000);
				ObjectMapper mapper = new ObjectMapper();
				String msg = mapper.writeValueAsString(message);
				
				buffer.put(msg.getBytes());
				buffer.flip();
				while(selectedKeysIterator.hasNext())
				{
					SelectionKey selectionKey=selectedKeysIterator.next();
					if(parseChannelIp((SocketChannel)selectionKey.channel()).equals(eachNeighbor.getIpAddress()))
					{
						SocketChannel socketChannel=(SocketChannel)selectionKey.channel();
						socketChannel.write(buffer);
					}
					selectedKeysIterator.remove();
				}
			}
		}catch(Exception e) {
			System.out.println("Sending failed because "+e.getMessage());
		}
	}
	
	public static String parseChannelIp(SocketChannel channel){//parse the ip form the SocketChannel.getRemoteAddress();
		String ip = null;
		String rawIp =null;  
		try {
			rawIp = channel.getRemoteAddress().toString().split(":")[0];
			ip = rawIp.substring(1, rawIp.length());
		} catch (IOException e) {
			System.out.println("can't convert channel to ip");
		}
		return ip;
	}
	
	public static Integer parseChannelPort(SocketChannel channel){//parse the ip form the SocketChannel.getRemoteAddress();
		String port =null;  
		try {
			port = channel.getRemoteAddress().toString().split(":")[1];
		} catch (IOException e) {
			System.out.println("can't convert channel to ip");
		}
		return Integer.parseInt(port);
	}
	
	public static boolean disable(Node server) throws IOException{
		if(isNeighbor(server)){
			
			sendMessage(server,new Message(myNode.getId(),myNode.getIpAddress(),myNode.getPort(),"disable"));
			for(SocketChannel channel:openChannels){
				if(server.getIpAddress().equals(parseChannelIp(channel))){
					try {
						channel.close();
					} catch (IOException e) {
						System.out.println("Cannot close the connection;");
					}
					openChannels.remove(channel);
					break;
				}
			}
			routingTable.put(server,Integer.MAX_VALUE-2);
			neighbors.remove(server);
			System.out.println("Disabled connection with server "+server.getId()+"("+server.getIpAddress()+")");
			return true;
		}
		else{
			System.out.println("You can only disable connection with your neighbor!!");
			return false;
		}
	}
	public static void display() {
		TableBuilder tb = new TableBuilder();
		tb.addRow("Destination Server ID","Next Hop Server ID","Cost");
		Collections.sort(nodes,new NodeComparator());
		for(Node eachNode:nodes){
			int cost = routingTable.get(eachNode);
			String costStr = ""+cost;
			if(cost==Integer.MAX_VALUE-2){
				costStr = "infinity";
			}
			String nextHopID = "N.A";
			if(nextHop.get(eachNode)!=null){
				nextHopID = ""+nextHop.get(eachNode).getId(); 
			}
			tb.addRow(""+eachNode.getId(),""+nextHopID,costStr);
		}
		System.out.println(tb.toString());
	}
	
	
}

class NodeComparator implements Comparator<Node> {
    @Override
    public int compare(Node n1, Node n2) {
    	Integer id1 = n1.getId();
    	Integer id2 = n2.getId();
        return id1.compareTo(id2);
    }
}
class Message implements Serializable{

	private static final long serialVersionUID = 1L;
	private int id;
	private String ipAddress;
	private int port;
	private List<String> routingTable= new ArrayList<String>();
	private String type;
	public Message(){}
	public Message(int id, String ipAddress, int port,String type) {
		super();
		this.id = id;
		this.ipAddress = ipAddress;
		this.port = port;
		this.type = type;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public List<String> getRoutingTable() {
		return routingTable;
	}
	public void setRoutingTable(List<String> routingTable) {
		this.routingTable = routingTable;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	
}
class Node implements Serializable{
	private static final long serialVersionUID = 1L;
	private int id;
	private String ipAddress;
	private int port;
	
	public Node(int id, String ipAddress,int port) {
		this.id = id;
		this.ipAddress = ipAddress;
		this.port = port;
	}

	
	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
		result = prime * result + port;
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (id != other.id)
			return false;
		if (ipAddress == null) {
			if (other.ipAddress != null)
				return false;
		} else if (!ipAddress.equals(other.ipAddress))
			return false;
		if (port != other.port)
			return false;
		return true;
	}
	
}
class Server extends Thread{
	private int port = 0;
	public Server(int port)
    {
       this.port = port;
    }
	public void run() {
        try
        {
            dv.read = Selector.open();
            dv.write = Selector.open();
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            while(true)
			{
				SocketChannel socketChannel=serverSocketChannel.accept();
				if(socketChannel != null)
				{
					socketChannel.configureBlocking(false);
					socketChannel.register(dv.read, SelectionKey.OP_READ);
					socketChannel.register(dv.write, SelectionKey.OP_WRITE);
					dv.openChannels.add(socketChannel);
					System.out.println("The connection to peer "+dv.parseChannelIp(socketChannel)+" is succesfully established");
				}
			}
        }
        catch(IOException i)
        {
            System.out.println(i);
        }
	}
}
class TableBuilder
{
    List<String[]> rows = new LinkedList<String[]>();
 
    public void addRow(String... cols)
    {
        rows.add(cols);
    }
 
    private int[] colWidths()
    {
        int cols = -1;
 
        for(String[] row : rows)
            cols = Math.max(cols, row.length);
 
        int[] widths = new int[cols];
 
        for(String[] row : rows) {
            for(int colNum = 0; colNum < row.length; colNum++) {
                widths[colNum] =
                    Math.max(
                        widths[colNum],
                        StringUtils.length(row[colNum]));
            }
        }
 
        return widths;
    }
 
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
 
        int[] colWidths = colWidths();
 
        for(String[] row : rows) {
            for(int colNum = 0; colNum < row.length; colNum++) {
                buf.append(
                    StringUtils.rightPad(
                        StringUtils.defaultString(
                            row[colNum]), colWidths[colNum]));
                buf.append(' ');
            }
 
            buf.append('\n');
        }
 
        return buf.toString();
    }
 
}
class Client extends Thread
{
    Set<SelectionKey> keys;
    Iterator<SelectionKey> selectedKeysIterator;
    ByteBuffer buffer = ByteBuffer.allocate(5000);
    SocketChannel socketChannel;
    int bytesRead;
    public void run()
    {
        try {
        		while(true){
        			int channelReady = dv.read.selectNow();
        			keys = dv.read.selectedKeys();
        			selectedKeysIterator = keys.iterator();
        			if(channelReady!=0){
        				while(selectedKeysIterator.hasNext()){
        					SelectionKey key = selectedKeysIterator.next();
        					socketChannel = (SocketChannel)key.channel();
        					try{
        						bytesRead = socketChannel.read(buffer);
        					}catch(IOException ie){
        						selectedKeysIterator.remove();
        						String IP = dv.parseChannelIp(socketChannel);
        						Node node = dv.getNodeByIP(IP);
        						dv.disable(node);
        						System.out.println(IP+" remotely closed the connection!");
        						break;
        					}
        					String message = "";
        					while(bytesRead!=0){
        						buffer.flip();
        						while(buffer.hasRemaining()){
        							message+=((char)buffer.get());
        						}
    							ObjectMapper mapper = new ObjectMapper();
    							Message msg = null;
    							boolean messageReceived = false;
    							int fromID = 0;
    							try{
									msg = mapper.readValue(message,Message.class);
									messageReceived = true;
	    							dv.numberOfPacketsReceived++;
    			        			fromID = msg.getId();
    							}catch(JsonMappingException jme){
    								System.out.println("Server "+dv.parseChannelIp(socketChannel)+" crashed.");
    							}
    			        		Node fromNode = dv.getNodeById(fromID);
    			        		if(msg!=null){
    			        			
    			        			if(msg.getType().equals("update") && messageReceived){
    			        				List<String> receivedRT = msg.getRoutingTable();
	        			        		Map<Node,Integer> createdReceivedRT = makeRT(receivedRT);
	        			        		int presentCost = dv.routingTable.get(fromNode);
	        			        		int updatedCost = createdReceivedRT.get(dv.myNode);
	        			        		if(presentCost!=updatedCost){
	        			        			dv.routingTable.put(fromNode,updatedCost);
	        			        		}
    			        			}
	    			        		if(msg.getType().equals("step") && messageReceived) {
	    			        			List<String> receivedRT = msg.getRoutingTable();
	        			        		Map<Node,Integer> createdReceivedRT = makeRT(receivedRT);
	        			        		for(Map.Entry<Node, Integer> entry1 : dv.routingTable.entrySet()){
	        			        			if(entry1.getKey().equals(dv.myNode)){
	        			        				continue;
	        			        			}
	        			        			else{
	        			        				int presentCost = entry1.getValue();
	        			        				int costToReceipient = createdReceivedRT.get(dv.myNode); 
	        			        				int costToFinalDestination = createdReceivedRT.get(entry1.getKey());
        			        					if(costToReceipient+costToFinalDestination < presentCost){
        			        					dv.routingTable.put(entry1.getKey(),costToReceipient+costToFinalDestination);
        			        					dv.nextHop.put(entry1.getKey(),fromNode);
	        			        				
	        			        				/*if(dv.neighbors.contains(entry1.getKey())){
	        			        					int receivedCost = createdReceivedRT.get(dv.myNode);
	    			        						dv.routingTable.put(entry1.getKey(),receivedCost);
	    			        						System.out.println("Server "+entry1.getKey().getId()+" updated with cost "+receivedCost+".");
	        			        				}else{
	        			        					if(dv.routingTable.get(fromNode)+createdReceivedRT.get(entry1.getKey())<presentCost){
	        			        						dv.nextHop.put(entry1.getKey(), fromNode);
	        			        						dv.routingTable.put(entry1.getKey(),dv.routingTable.get(fromNode)+createdReceivedRT.get(entry1.getKey()));
	        			        						System.out.println("Server "+entry1.getKey().getId()+" updated with cost "+dv.routingTable.get(fromNode)+createdReceivedRT.get(entry1.getKey())+".");
	        			        					}
	        			        				}*/
	        			        			}
	        			        		}
	    			        		}
	        					
	    			        		if(msg.getType().equals("disable") || !messageReceived){
	    			        			dv.routingTable.put(fromNode, Integer.MAX_VALUE-2);
	    			        			System.out.println("Routing Table updated with Server "+fromID+"'s cost set to infinity");
	    			        			if(dv.isNeighbor(fromNode)){
	    			        				for(SocketChannel channel:dv.openChannels){
	    			        					if(fromNode.getIpAddress().equals(dv.parseChannelIp(channel))){
	    			        						try {
	    			        							channel.close();
	    			        						} catch (IOException e) {
	    			        							System.out.println("Cannot close the connection;");
	    			        						}
	    			        						dv.openChannels.remove(channel);
	    			        						break;
	    			        					}
	    			        				}
	    			        				dv.routingTable.put(fromNode, Integer.MAX_VALUE-2);
	    			        				dv.neighbors.remove(fromNode);
	    			        			}
	    			        		}
    			        		}
    			        		if(message.isEmpty()){
    			        			break;
    			        		}
    			        		else{
    			        			System.out.println("Message received from Server "+msg.getId()+" ("+dv.parseChannelIp(socketChannel)+")");
    			        			System.out.println("Current Routing Table:-");
    			        			dv.display();
    			        		}
    			        		buffer.clear();
                    			if(message.trim().isEmpty())
    								bytesRead =0;
    							else{
    								try{
    								bytesRead = socketChannel.read(buffer);
    								}catch(ClosedChannelException cce){
    									System.out.println("Channel closed for communication with Server "+fromID+".");
    								}
    							}
    								
    							bytesRead=0;
    							selectedKeysIterator.remove();
        					}
        				}
        			}
        			}
        		}
        }catch(Exception e) {
        		e.printStackTrace();
        }
        
    }
	private Map<Node, Integer> makeRT(List<String> receivedRT) {
		Map<Node,Integer> rt = new HashMap<Node,Integer>();
		for(String str:receivedRT){
			String[] parts = str.split("#");
			int id = Integer.parseInt(parts[0]);
			int cost = Integer.parseInt(parts[1]);
			rt.put(dv.getNodeById(id), cost);
		}
		return rt;
	}
 
}