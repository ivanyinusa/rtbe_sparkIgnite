package bi.EasySock;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;


@WebSocket
public class session_other {
    public Session session;
    public String myUniqueId;
    public String ip;
    public String page="";
    public String bu="";
    public String category="";
    public String subcategory="";
    public String pagetag="";
    public String token="";
    public String action="";
    private final static HashMap<String, session_other> sockets = new HashMap<String, session_other>();

    private String getMyUniqueId() {
        // unique ID from this class' hash code
        return Integer.toHexString(this.hashCode());
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        // this unique ID
        this.myUniqueId = this.getMyUniqueId();
        this.ip=session.getRemoteAddress().getAddress().toString().replace("/","");
        this.ip=(this.ip.equals("192.168.1.101")?"10.1.59.198":this.ip);
        sockets.put(this.myUniqueId, this);

        try {
            String str="default:today:"+ (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
            session.getRemote().sendString(str);
            logwriter(this.ip +" connected");
        } catch (IOException e) {
// TODO Auto-generated catch block
            System.out.println(e.getMessage());
        }
    }


    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        //System.out.println("Close: statusCode=" + statusCode + ", reason=" + reason);
        if (session_other.sockets.containsKey(this.myUniqueId)) {
            // remove connection
            try{
                session_other.sockets.remove(this.myUniqueId);
                logwriter(this.ip +" disconnected");
            } catch (RuntimeException e) {
// TODO Auto-generated catch block
                System.out.println(e.getMessage());
            }
        }
    }

    public void logwriter(String content){
        System.out.println(content);
        if (this.ip.equals("10.1.54.41")||this.ip.equals("10.1.54.42")||this.ip.equals("10.1.54.43")||this.ip.equals("10.1.54.10")) return;
        Date timestamp=new Date();
        String dt=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
        //common.writelog(dt+":" + content);
        System.out.println(dt+":" + content);
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        logwriter("Exception: " + t.getMessage());
    }


    @OnWebSocketMessage
    public void onMessage(String message) {
        //client:dashboard:gis:usa:
        System.out.println(message);
        mensaje m=new mensaje(message);
        if(m.messagetype.equals(messagetypeset.itemalarm)){
            broadcast(m);
        }
        else if(m.messagetype.equals(messagetypeset.ap_itemalarm_client)){
            broadcast(m);
        }
        else if(m.messagetype.equals(messagetypeset.ap_itemalarm_server)){
            send2Server(m);
        }else if(m.messagetype.equals(messagetypeset.buybox_rst_summary)){
            broadcast(m);
        }else if(m.messagetype.equals(messagetypeset.buybox_rst_subcategory)){
            broadcast(m);
        }else if(m.messagetype.equals(messagetypeset.clientregistration)){
            //info:client:page=dashboard
            //info:client:page=overview:bu=inc
            //info:client:page=category:bu=inc
            //info:client:page=subcategory:bu=inc|category=CPU
            //info:client:page=item:bu=inc|subcategory=CPU Gifts
            //info:client:page=dashboard
            String pagestr=message.replace("info:client:page=", "");
            String[] param_array0=pagestr.split(":");

            this.pagetag=pagestr;

            //dashboard
            //overview
            //category
            //subcategory
            //item
            this.page=param_array0[0];

            if(param_array0.length>1){
                //bu=inc
                //bu=inc|category=CPU
                //bu=inc|subcategory=CPU
                String[] param_array2=param_array0[1].trim().split("\\|");
                for (String entry : param_array2){
                    if(entry.startsWith("bu"))
                        this.bu=entry.split("=")[1].trim();
                    if(entry.startsWith("category")) this.category=entry.split("=")[1].trim();
                    if(entry.startsWith("subcategory")) this.subcategory=entry.split("=")[1].trim();
                    if(entry.startsWith("token")) this.token=entry.split("=")[1].trim();
                    if(entry.startsWith("action")) this.action=entry.split("=")[1].trim();
                }
            }
            send2Server(m);//0.0019764291263689726
        }
    }

    private void send2Server(mensaje m){
        String message=m.messageContext;
//     message+=":"+this.myUniqueId;

        for (session_other dstSocket : session_other.sockets.values()) {
//            if (dstSocket == this) {
//                continue;
//            }
            //||dstSocket.ip.equals("10.1.59.198")
            if(dstSocket.ip.equals("10.1.54.42")||dstSocket.ip.equals("10.1.54.44")||dstSocket.ip.equals("10.1.54.45")) {
                dstSocket.session.getRemote().sendStringByFuture(message);
            }
        }
    }

    private void broadcast(mensaje m){
        for (session_other dstSocket : session_other.sockets.values()) {
            if (dstSocket == this) {
                continue;
            }
            send(dstSocket,m);
        }
    }

    private void send(session_other dstSocket,mensaje m){
        if(dstSocket.page.equals("buybox")&&(m.messagetype.equals(messagetypeset.itemalarm))){
            dstSocket.session.getRemote().sendStringByFuture(m.messageContext);
        }
        else if(dstSocket.page.equals("buybox")&&m.messagetype.equals(messagetypeset.ap_itemalarm_client)){
            dstSocket.session.getRemote().sendStringByFuture(m.messageContext);

        }else if(dstSocket.page.equals("buybox")&&m.messagetype.equals(messagetypeset.buybox_rst_summary)){
            if(m.token.equals(dstSocket.token)){
                dstSocket.session.getRemote().sendStringByFuture(m.messageContext);
            }
        }else if(dstSocket.page.equals("buybox")&&m.messagetype.equals(messagetypeset.buybox_rst_subcategory)){
            if(m.token.equals(dstSocket.token)){
                dstSocket.session.getRemote().sendStringByFuture(m.messageContext);
            }
        }
    }
}



