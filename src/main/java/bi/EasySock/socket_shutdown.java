package bi.EasySock;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

public class socket_shutdown {
    public static void main(String[] args) throws Exception {
        try{
            Socket s = new Socket(InetAddress.getByName("192.168.1.101"), 8979);
            OutputStream out = s.getOutputStream();
            //    System.out.println("*** sending jetty stop request");
            out.write(("shutdown@a~@#()d834f46573206@54^%&579a\r\n").getBytes());
            out.flush();
            s.close();
        }catch(ConnectException e){
            System.out.println(e.getMessage());
        }
    }
}
