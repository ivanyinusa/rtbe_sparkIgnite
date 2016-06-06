package bi.EasySock;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.websocket.server.ServerEndpoint;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

@ServerEndpoint("/debug")

public class socket_start{
    private static Server server;
    private static Integer manage_port=8979;
    public static void main(String[] args) {
// TODO Auto-generated method stub
        Integer port=8889;
        try {
            port=Integer.parseInt(args[0]);
        }catch (RuntimeException e) {


        }

        server = new Server(port);

        HandlerCollection hc = new HandlerCollection();
        try {
/*-------------------ByBU-------------------*/
            WebSocketHandler wsHandler_bybu = new WebSocketHandler() {
                @Override
                public void configure(WebSocketServletFactory factory) {

                    factory.getPolicy().setIdleTimeout(1000 * 3600 * 240);

                    factory.getPolicy().setMaxTextMessageSize(5*1024*1024);
                    factory.register(session_bybu.class);
                }
            };
            ContextHandler context_bybu = new ContextHandler();
            context_bybu.setContextPath("/so_bybu");
            context_bybu.setHandler(wsHandler_bybu);
            hc.addHandler(context_bybu);
/*-------------------ByBU-------------------*/
/*-------------------Other-------------------*/
            WebSocketHandler wsHandler_other = new WebSocketHandler() {
                @Override
                public void configure(WebSocketServletFactory factory) {

                    factory.getPolicy().setIdleTimeout(1000 * 3600 * 240);

                    factory.getPolicy().setMaxTextMessageSize(5*1024*1024);
                    factory.register(session_other.class);
                }
            };
            ContextHandler context_other = new ContextHandler();
            context_other.setContextPath("/so_other");
            context_other.setHandler(wsHandler_other);
            hc.addHandler(context_other);
/*-------------------Other-------------------*/

            server.setHandler(hc);
/*-------------------Monitor-------------------*/
            Thread monitor = new MonitorThread();
            monitor.start();
       /*-------------------Monitor-------------------*/
       /*-------------------Server-------------------*/
            server.start();
            server.setStopAtShutdown(true);
            server.join();
/*-------------------Server-------------------*/

        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            try{
                server.destroy();
            }catch(IllegalStateException e){
                e.printStackTrace();
            }
        }
    }



    public static class MonitorThread extends Thread {
        public ServerSocket listener;
        private Socket socket;
        public MonitorThread() {
            setName("StopMonitor");
            try {
                listener = new ServerSocket(manage_port, 1, InetAddress.getByName("192.168.1.101"));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                boolean done=false;
                while(!done){
                    socket = listener.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String msg=reader.readLine();
                    System.out.println(socket.getRemoteSocketAddress().toString().replace("/", "") + ": " + msg);
                    if(msg!=null&&msg.equals("shutdown@a~@#()d834f46573206@54^%&579a")){

                        System.out.println("shutdown command executed");
                        server.stop();
                        listener.close();
                        done=true;
                    }
                    socket.close();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}


