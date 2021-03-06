/**
 * Copyright 2016 Gash.
 *
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package crimewatch.router.server;

import java.beans.Beans;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

import crimewatch.messaging.Message;
import crimewatch.messaging.Node;
import crimewatch.router.container.RoutingConf;
import crimewatch.router.server.resources.RouteResource;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import routing.Pipe.Route;

/**
 * The message handler processes json messages that are delimited by a 'newline'
 * 
 * TODO replace println with logging!
 * 
 * @author gash
 * 
 */
public class ServerHandler extends /*SimpleChannelInboundHandler<Route>*/ ChannelInboundHandlerAdapter {
	protected static Logger logger = LoggerFactory.getLogger("connect");
	protected static MongoClient mongoClient;
	protected static DBCollection dbCollection;

	private HashMap<String, String> routing;
	
	public Node n;

	public ServerHandler(RoutingConf conf,Node n) {
		this.n=n;
		if (conf != null)
			routing = conf.asHashMap();
		
		if (mongoClient == null) {
			try {
				//mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
				mongoClient = new MongoClient("localhost", 27017);
				DB messageDB = mongoClient.getDB("messagesDB");
				dbCollection = messageDB.getCollection("messages");
			}
			catch(Exception e) {
				System.out.println(e.getMessage());
			}
}
	}

	/**
	 * override this method to provide processing behavior. This implementation
	 * mimics the routing we see in annotating classes to support a RESTful-like
	 * behavior (e.g., jax-rs).
	 * 
	 * @param msg
	 */
	public void handleMessage(Route msg, Channel channel) {
		if (msg == null) {
			// TODO add logging
			System.out.println("ERROR: Unexpected content - " + msg);
			return;
		}

		System.out.println("---> " + msg.getId() + ": " + msg.getPath() + ", " + msg.getPayload());

		try {
			String clazz = routing.get(msg.getPath().toLowerCase());
			if (clazz != null) {
				RouteResource rsc = (RouteResource) Beans.instantiate(RouteResource.class.getClassLoader(), clazz);
				try {
					String reply = rsc.process(msg.getPayload());
					System.out.println("---> reply: " + reply);
					if (reply != null) {
						Route.Builder rb = Route.newBuilder(msg);
						rb.setPayload(reply);
						channel.write(rb.build());

						// Uncomment to query the database.
						queryDB(msg.getPayload());
						
						
						// Code to insert data in mongoDB
						/*String uniqueID = UUID.randomUUID().toString();					
						DBObject messageObject = new BasicDBObject("_id", uniqueID).append("messageID", msg.getId()).append("payload", msg.getPayload());
						System.out.println(msg.getAllFields().toString());
						dbCollection.insert(messageObject);*/
					}
				} catch (Exception e) {
					// TODO add logging
					Route.Builder rb = Route.newBuilder(msg);
					rb.setPayload("Error: " + e.getMessage());
					channel.write(rb.build());
				}
			} else {
				// TODO add logging
				System.out.println("ERROR: unknown path - " + msg.getPath());
			}
		} catch (Exception ex) {
			// TODO add logging
			System.out.println("ERROR: processing request - " + ex.getMessage());
		}

		System.out.flush();
	}

	/**
	 * a message was received from the server. Here we extract the from and to date time,
	 * query the database to fetch records based on the query and display it.
	 * 
	 * @param payload
	 *            The message payload received
	 */
	public void queryDB(String payload) {
		// Assuming we get from and to date in a single message divided by ',' 
		String[] filters = payload.split(",");
		try {
		Date fromTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(filters[0]);
		Date toTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(filters[1]);
		
		BasicDBObject query = new BasicDBObject("time", new BasicDBObject("$gte", fromTime).append("$lte", toTime));
		DBCursor cursor = dbCollection.find(query);
		while(cursor.hasNext()) {
	        System.out.println(cursor.next());
	    }
		
		}
		catch(Exception ex) {
			System.out.println(ex.getMessage());
		}
	}
	
	/**
	 * a message was received from the server. Here we dispatch the message to
	 * the client's thread pool to minimize the time it takes to process other
	 * messages.
	 * 
	 * @param ctx
	 *            The channel the message was received from
	 * @param msg
	 *            The message
	 */
	/*@Override
	protected void channelRead0(ChannelHandlerContext ctx, Route msg) throws Exception {
		System.out.println("------------");
		handleMessage(msg, ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("Unexpected exception from downstream.", cause);
		ctx.close();
	}*/
	
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //ByteBuf in = (ByteBuf) msg;
        System.out.println(
            "Server received: " + msg);//in.toString(CharsetUtil.UTF_8));
       // ctx.writeAndFlush(Unpooled.copiedBuffer("Netty MAY JUNE rock!", CharsetUtil.UTF_8));
        
        
        Route.Builder rb = Route.newBuilder();
		rb.setId(10);
		rb.setPath("/message");
		rb.setPayload("Passing vote request");
		
        //Route.Builder rb = Route.newBuilder();
       //((Route.Builder)msg).getPayload();
		
		Message m=new Message(10,((Route)msg).getPayload());
		this.n.process(m);
        //ctx.channel().writeAndFlush(rb.build());
    }

    public void channelReadComplete(ChannelHandlerContext ctx) {
    	Route.Builder rb = Route.newBuilder();
		rb.setId(10);
		rb.setPath("/message");
		rb.setPayload("Yo Yo Yo");
        ctx.writeAndFlush(rb.build())
            .addListener(ChannelFutureListener.CLOSE);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}