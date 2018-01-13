import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

/**
 * Created by hwong on 2/22/2017.
 */
public class DNSquery {

    String domainName;
    InetAddress dnsServerAddress;
    byte[] query;
    byte [] qID;
    int packagelength;


    public DNSquery (String fqdn, InetAddress dnsAddress, Boolean ipv6Bool){
        domainName = fqdn;
        dnsServerAddress = dnsAddress;
        genID();
        encondingQuery(ipv6Bool);

    }

    private void encondingQuery(Boolean ipv6Bool){

        query = new byte[512];
        //queryID
        query[0]=qID[0];
        query[1]=qID[1];
        //QR ,Opcode,AA, TC,RD,RA,Z,RCODE
        query[2]=0x00;
        query[3]=0x00;
        //Query Count
        query[4]=0x00;
        query[5]=0x01;
        //Answer Count
        query[6]=0x00;
        query[7]=0x00;
        //Name Server Records
        query[8]=0x00;
        query[9]=0x00;
        //Additional Record Count
        query[10]=0x00;
        query[11]=0x00;

        packagelength = 11;
        //Start of QName

        packagelength++;
        int count = 0;
        int dot = 12;
        for(int i = 0; i<=domainName.length();i++){
            packagelength++;
            if(i== domainName.length()||domainName.charAt(i)== '.'){
                query[dot]= (byte)count;
                count= 0;
                dot = packagelength;
            }else{
                query[packagelength]=(byte) domainName.charAt(i);
                count++;
            }

        }



        //the end of the QName
        query[packagelength++]=0x00;
        //QTYPE

        query[packagelength++]=0x00;
   //
         if(ipv6Bool) { query[packagelength++]= 0x1c; }else{
            query[packagelength++]=0x01; }
  //
        //Qclass
        query[packagelength++]=0x00;
        query[packagelength++]=0x01;

    }


    public void sendQuery(DatagramSocket socket, Boolean tracingOn)throws IOException{
       byte[] query = getQuery();
        DatagramPacket packet = new DatagramPacket(query,packagelength,dnsServerAddress,53);
        socket.send(packet);
        if(tracingOn){
            printQuery();
        }

    }

    public  void printQuery(){
        System.out.println();
        System.out.println();
        System.out.println("Query ID     " + getqID()+" "+ getDomainName()+ "-->" + getDnsServerAddress().getHostAddress());
    }

    public InetAddress getDnsServerAddress(){ return dnsServerAddress;}
    public String getDomainName(){return  domainName;}

    public byte[] getQuery(){
        return query;
    }

    public void genID(){
        Random ra = new Random();
        qID = new byte[2];
        ra.nextBytes(qID);
    }

    public int getqID(){
       int qID = 	(query[0] << 8) & 0xff00; ;
       return qID | (query[1] & 0xff);
    }


}
