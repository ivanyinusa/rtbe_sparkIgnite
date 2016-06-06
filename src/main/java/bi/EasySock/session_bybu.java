package bi.EasySock;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;


@WebSocket
public class session_bybu {
    public Session session;
    public String myUniqueId;
    public String ip;
    public String page="";
    public String bu="";
    public String category="";
    public String subcategory="";
    public String pagetag="";
    public String token="";
    private final static HashMap<String, session_bybu> sockets = new HashMap<String, session_bybu>();
    private final static HashMap<String, mensaje> messagecache = new HashMap<String, mensaje>();

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
        if (session_bybu.sockets.containsKey(this.myUniqueId)) {
            // remove connection
            try{
                session_bybu.sockets.remove(this.myUniqueId);
                logwriter(this.ip +" disconnected");
            } catch (RuntimeException e) {
                // TODO Auto-generated catch block
                System.out.println(e.getMessage());
            }
        }
    }

    public void logwriter(String content){
        System.out.println(content);
        if (this.ip.equals("10.1.54.41")||this.ip.equals("10.1.54.43")||this.ip.equals("10.1.54.44")||this.ip.equals("10.1.54.10")) return;
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
        if(m.messagetype.equals(messagetypeset.bu_overview)){
            session_bybu.messagecache.put("overview:"+m.bu,m);
            broadcast(m);
        }else if(m.messagetype.equals(messagetypeset.bu_category)){
            session_bybu.messagecache.put("category:"+m.bu,m);
            broadcast(m);
        }else if(m.messagetype.equals(messagetypeset.bu_subcategory)){
            session_bybu.messagecache.put("subcategory:"+m.bu+":"+m.parent,m);
            if(HitTestPoolSubcategory(m)){
                broadcast(m);
            }else{
                send2Server(new mensaje("info:clear:subcategories:"+m.bu+":"+m.parent));
            }
        }else if(m.messagetype.equals(messagetypeset.bu_item)){
            session_bybu.messagecache.put("item:"+m.bu+":"+m.parent,m);
            if(HitTestPoolItem(m)){
                broadcast(m);
            }else{
                send2Server(new mensaje("info:clear:items:"+m.bu+":"+m.preparent+":"+m.parent));
            }
        }else if(m.messagetype.equals(messagetypeset.bu_topcateitem)){
            broadcast(m);
        }else if(m.messagetype.equals(messagetypeset.bu_gis)){
            session_bybu.messagecache.put("gis_states:",m);
            broadcast(m);
        }else if(m.messagetype.equals(messagetypeset.gis_usa_states)){
            session_bybu.messagecache.put("gis_states_usa:",m);
            broadcast(m);
        }else if(m.messagetype.equals(messagetypeset.gis_can_states)){
            session_bybu.messagecache.put("gis_states_can:",m);
            broadcast(m);
        }else if(m.messagetype.equals(messagetypeset.gis_usa_counties)){
            session_bybu.messagecache.put("gis_counties_usa:",m);
            broadcast(m);
        }else if(m.messagetype.equals(messagetypeset.runningstatus)){
            broadcast(m);
        }else if (m.messagetype.equals(messagetypeset.soretriever)){
            session_bybu.messagecache.put("generator",m);
            broadcast(m);
        }else if(m.messagetype.equals(messagetypeset.clientregistration)){
            //info:client:page=dashboard
            //info:client:page=console
            //info:client:page=overview:bu=inc
            //info:client:page=category:bu=inc
            //info:client:page=subcategory:bu=inc|category=CPU
            //info:client:page=item:bu=inc|subcategory=CPU Gifts
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
                }
            }


            if(!(m.messagetype.equals(messagetypeset.bu_subcategory)
                    ||m.messagetype.equals(messagetypeset.bu_item))){

                for (mensaje msg : session_bybu.messagecache.values()){

//session.getRemote().sendString(m.messageContext);

                    send(this,msg);

                }
            }

            send2Server(m);
        }else if(m.messagetype.equals(messagetypeset.dataready)){
            send2Server(m);
        }else if(m.messagetype.equals(messagetypeset.client2server)){
            send2Server(m);
        }else if(m.messagetype.equals(messagetypeset.server2client)){
            send2Client(m);
        }else if(m.messagetype.equals(messagetypeset.users)){
            GetConnectedUsers();
        }
    }

    private void GetConnectedUsers(){
        String lines="users:";
        for (session_bybu dstSocket : session_bybu.sockets.values()) {
            if(!dstSocket.ip.equals("10.1.54.41")&&!dstSocket.ip.equals("10.1.54.43")&&!dstSocket.ip.equals("10.1.54.44")&&!dstSocket.ip.equals("10.1.54.10")) {
                String row=dstSocket.ip+";"+dstSocket.pagetag;
                lines+=(lines.equals("")?"":"\n")+row;
            }
        }

        this.session.getRemote().sendStringByFuture(lines);
    }

    private void send2Server(mensaje m){
        String message=m.messageContext;
//     message+=":"+this.myUniqueId;

        for (session_bybu dstSocket : session_bybu.sockets.values()) {
//            if (dstSocket == this) {
//                continue;
//            }
            //||dstSocket.ip.equals("10.1.59.198")
            if(dstSocket.ip.equals("10.1.54.41")||dstSocket.ip.equals("10.1.54.43")||dstSocket.ip.equals("10.1.54.44")) {
                dstSocket.session.getRemote().sendStringByFuture(message);
            }
        }
    }

    //client:response:"+data.ip+":data"
    private void send2Client(mensaje m){
        String clientip="";
        try{

            if (!m.messageContext.contains(":")) return;

            clientip=m.messageContext.split(":")[1];

            clientip=clientip.split("\\|\\|")[0];
        }catch(RuntimeException e){}

        if(clientip.equals("")) return;
        String[] ar=clientip.split(",");
        String ip;
        for (session_bybu dstSocket : session_bybu.sockets.values()) {
            if (dstSocket == this) {
                // skip me
                continue;
            }
            ip=(dstSocket.ip.equals("192.168.1.101"))?"10.1.59.198":dstSocket.ip;
            if(Arrays.asList(ar).contains(ip)){
                dstSocket.session.getRemote().sendStringByFuture(m.messageContext);
            }
        }
    }

    private void broadcast(mensaje m){
        for (session_bybu dstSocket : session_bybu.sockets.values()) {
            if (dstSocket == this) {
                continue;
            }
            send(dstSocket,m);
        }
    }

    private Boolean HitTestPoolSubcategory(mensaje m){
        Integer counter=0;
        for (session_bybu dstSocket : session_bybu.sockets.values()) {
            if (dstSocket == this) {
                continue;
            }
            if(dstSocket.page.equals("subcategory")
                    &&dstSocket.bu.equals(m.bu)
                    &&m.messagetype.equals(messagetypeset.bu_subcategory)
                    &&m.parent.equals(dstSocket.category)){
                counter+=1;
            }
        }
        return (counter>0);
    }

    private Boolean HitTestPoolItem(mensaje m){
        Integer counter=0;
        for (session_bybu dstSocket : session_bybu.sockets.values()) {
            if (dstSocket == this) {
                continue;
            }
            if(dstSocket.page.equals("item")
                    &&dstSocket.bu.equals(m.bu)
                    &&m.messagetype.equals(messagetypeset.bu_item)
                    &&m.parent.equals(dstSocket.subcategory)){
                counter+=1;
            }
        }
        return (counter>0);
    }

    private void send(session_bybu dstSocket,mensaje m){
        if(dstSocket.page.equals("dashboard")&&m.messagetype.equals(messagetypeset.bu_overview)){
            dstSocket.session.getRemote().sendStringByFuture(m.messageContext);
        }else if(dstSocket.page.equals("dashboard")&&m.messagetype.equals(messagetypeset.bu_gis)){
            dstSocket.session.getRemote().sendStringByFuture(m.messageContext);
        }else if(dstSocket.page.equals("us_map_counties")&&m.messagetype.equals(messagetypeset.gis_usa_counties)){
            dstSocket.session.getRemote().sendStringByFuture(m.messageContext);
        }else if(dstSocket.page.equals("can_map_states")&&m.messagetype.equals(messagetypeset.gis_can_states)){
            dstSocket.session.getRemote().sendStringByFuture(m.messageContext);
        }else if((dstSocket.page.equals("usa_map_states")||dstSocket.page.equals("us_map_counties"))&&m.messagetype.equals(messagetypeset.gis_usa_states)){
            dstSocket.session.getRemote().sendStringByFuture(m.messageContext);
        }else if(dstSocket.page.equals("overview")&&dstSocket.bu.equals(m.bu)&&(m.messagetype.equals(messagetypeset.bu_overview)||m.messagetype.equals(messagetypeset.bu_category))){
            dstSocket.session.getRemote().sendStringByFuture(m.messageContext);
        }else if(dstSocket.page.equals("subcategory")
                &&dstSocket.bu.equals(m.bu)
                &&m.messagetype.equals(messagetypeset.bu_subcategory)
                &&m.parent.equals(dstSocket.category)){
            dstSocket.session.getRemote().sendStringByFuture(m.messageContext);
        }else if(dstSocket.page.equals("querycateitem")
                &&dstSocket.bu.equals(m.bu)
                &&m.messagetype.equals(messagetypeset.bu_topcateitem)
                &&m.parent.equals(dstSocket.category)
                &&m.token.equals(dstSocket.token)){
            dstSocket.session.getRemote().sendStringByFuture(m.messageContext);
        }else if(dstSocket.page.equals("item")
                &&dstSocket.bu.equals(m.bu)
                &&m.messagetype.equals(messagetypeset.bu_item)
                &&m.parent.equals(dstSocket.subcategory)){
            dstSocket.session.getRemote().sendStringByFuture(m.messageContext);
        }else if(m.messagetype.equals(messagetypeset.runningstatus)||m.messagetype.equals(messagetypeset.soretriever)){
            dstSocket.session.getRemote().sendStringByFuture(m.messageContext);
        }
    }
}


