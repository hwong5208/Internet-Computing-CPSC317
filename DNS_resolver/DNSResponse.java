
import java.net.InetAddress;
import java.net.UnknownHostException;


// Lots of the action associated with handling a DNS query is processing
// the response. Although not required you might find the following skeleton of
// a DNSreponse helpful. The class below has bunch of instance data that typically needs to be 
// parsed from the response. If you decide to use this class keep in mind that it is just a 
// suggestion and feel free to add or delete methods to better suit your implementation as 
// well as instance variables.



public class DNSResponse {
    private int queryID;                  // this is for the response it must match the one in the request 
    private int answerCount = 0;          // number of answers  
    private boolean decoded = false;      // Was this response successfully decoded
    private int nsCount = 0;              // number of nscount response records
    private int additionalCount = 0;      // number of additional (alternate) response records
    private boolean authoritative = false;// Is this an authoritative record

    //added
    private DNSquery query;
    private int rCode;  // Response code
    private int byteNum =0;
    private int qCount;
    private ResourceRecord ansList[];
    private ResourceRecord nsList[];
    private ResourceRecord additionalList[];
    private String fqdn;


    // Note you will almost certainly need some additional instance variables.

    // When in trace mode you probably want to dump out all the relevant information in a response

	void dumpResponse() {
     System.out.println("Response ID: "+ queryID +" Authoritative " + authoritative );


    System.out.println("  Answers ["+ answerCount+"] ");
        for(int i = 0; i< answerCount; i++){
            ansList[i].print();
        }


    System.out.println("  Nameservers ["+ nsCount+"] ");
        for(int i = 0; i< nsCount; i++){
            nsList[i].print();
        }


    System.out.println("  Additional Information ["+ additionalCount+"] ");
        for(int i = 0; i< additionalCount; i++){
            additionalList[i].print();
        }

    }

    public InetAddress queryNext(){

        if(decoded &&(answerCount ==0 ) &&(additionalCount >0) ){
            for(int i = 0;i< additionalCount; i++){
                if(additionalList[i].getRtype() ==1){
                    return additionalList[i].getIPaddr();

                }
            }
        }
        return null;
    }

    // The constructor: you may want to add additional parameters, but the two shown are 
    // probably the minimum that you need.

    public ResourceRecord getIPv4Result(){
        if(answerCount >= 1){

            for(int i = 0; i< answerCount;i++){
                if(ansList[i].getRtype()==1 ){
                    return ansList[i];
                }
            }
        }
        return null;

    }

    public ResourceRecord getIPv6Result(){
        if(answerCount >= 1){

            for(int i = 0; i< answerCount;i++){
            if(ansList[i].getRtype()==28 ){
                return ansList[i];
            }
            }
        }
        return null;
    }


    public int getTTL(){
        if (answerCount>= 1){
            return ansList[0].getTtl();

        }
        return 0;
    }

     // get Cname
    public  String getName(){
        if(answerCount >=1){
            if (ansList[0].getRtype() ==5){
                return  ansList[0].getName();
            }
        }
        return null;
    }

    public boolean getAuthoritative(){
        return authoritative;
    }

    public ResourceRecord[] getAnsList(){
        return ansList;
    }

    public int getrCode(){
        return  rCode;
    }
    public  String getNSName(){
        if (nsCount >0){
            return  nsList[0].getName();
        }
        return null;
    }


	public DNSResponse (byte[] data, int len, DNSquery q) throws UnknownHostException {
	    
	    // The following are probably some of the things 
	    // you will need to do.

         query = q;
	    // Extract the query ID
         queryID = (data[byteNum]<<8) & 0xff00 ;
         byteNum++; // byteNum = 1;
         queryID = queryID | (data[byteNum]) & 0xff ;

	    // Make sure the message is a query response and determine
        byteNum++;    // byteNum = 2;
        if ((data[byteNum]& 0xc0)!=0x80){
            return;
        }
	    // if it is an authoritative response or note
        if ((data[byteNum]& 0x4)!= 0){
            authoritative = true;
        }

        byteNum++;  // byteNum = 3;
        rCode = data[byteNum] & 0xf;
        if(rCode !=0){
            return;
        }
        // determine question count
        byteNum++; // byteNum = 4;
        qCount = (data[byteNum]<<8)&0xff00;
        byteNum++; // byteNum = 5;
        qCount = qCount | (data[byteNum]&0xff);

        if(qCount != 1)
        {return;}

        // determine answer count
        byteNum++; // byteNum = 6;
        answerCount = (data[byteNum]<<8)& 0xff00;
        byteNum++; //byteNum = 7;
        answerCount = answerCount|(data[byteNum]&0xff);
        ansList = new ResourceRecord[answerCount];


	    // determine NS Count
        byteNum++; //byteNum = 8;
        nsCount = (data[byteNum]<<8)& 0xff00;
        byteNum++; //byteNum = 9;
        nsCount = nsCount| (data[byteNum]&0xff);
        nsList = new ResourceRecord[nsCount];

	    // determine additional record count
        byteNum++; //byteNum = 10;
        additionalCount = (data[byteNum]<<8)& 0xff00;
        byteNum++; //byteNum = 11;
        additionalCount = additionalCount| (data[byteNum]&0xff);
        additionalList = new ResourceRecord[additionalCount];

	    // Extract list of answers, name server, and additional information response 
	    // records

        fqdn = "";
        byteNum++;
        for(int cnt= (data[byteNum++]&0xff);cnt != 0; cnt=(data[byteNum++])&0xff){

            if(fqdn!=""){
                fqdn +=".";
            }
            if((cnt&0xC0 )>0){
                cnt = (cnt& 0x3f)<<8;
                cnt = cnt| (data[byteNum++]&0xff);
                fqdn = getfqdnHepler(fqdn, data, cnt);
                break;
            }else{
                for (int i = 0 ; i < cnt ; i++ ){

                    fqdn += (char) data[byteNum++];
                }
            }

        }

        byteNum+=4;
        for( int i = 0; i< answerCount; i++){
            ansList[i] = getResourceRecord(data);
        }

        for( int i = 0; i< nsCount; i++){
            nsList[i] = getResourceRecord(data);
        }
        for( int i = 0; i< additionalCount; i++){
            additionalList[i] = getResourceRecord(data);
        }

        decoded = true;

	}


    // You will probably want a methods to extract a compressed FQDN, IP address
    // cname, authoritative DNS servers and other values like the query ID etc.




    // get the compressed Fqdn
    private String getfqdnHepler(String fqdn, byte[] data, int offset){
        boolean flag = true;

        for(int cnt = (data[offset++] &0xff);cnt !=0; cnt = (data[offset++]&0xff)){
            if(!flag){
                fqdn +='.';
            }else{
                flag = false;
            }

            if((cnt & 0xc0)>0){
                cnt = (cnt & 0x3f) <<8;
                cnt = cnt| (data[offset++] & 0xff);
                fqdn = getfqdnHepler(fqdn,data, cnt);
                break;
            }else{
                for(int i = 0; i<cnt; i++){
                    fqdn = fqdn + (char) data[offset++];
                }
            }

        }

        return fqdn;
    }

    private ResourceRecord getResourceRecord(byte[] data)throws UnknownHostException{

        String fqdn = "";


        for(int cnt= (data[byteNum++]&0xff);cnt != 0; cnt=(data[byteNum++])&0xff){

            if(fqdn!=""){
                fqdn +=".";
            }
            if((cnt&0xC0 )>0){
                cnt = (cnt& 0x3f)<<8;
                cnt = cnt| (data[byteNum++]&0xff);
                fqdn = getfqdnHepler(fqdn, data, cnt);
                break;
            }else{
                for (int i = 0 ; i < cnt ; i++ ){
                    fqdn += (char) data[byteNum++];
                }
            }

        }

        int rtype =(data[byteNum++]<< 8 ) & 0xffff;
        rtype = rtype | (data[byteNum++]& 0xff);

        int rclass =(data[byteNum++]<< 8 ) & 0xffff;
        rclass = rclass | (data[byteNum++]& 0xff);


        int ttl = 0;
        for(int i= 0; i< 4; i++){
            ttl = ttl <<8;
            ttl = ttl | (data[byteNum++]& 0xff);
        }

        int rdlength =(data[byteNum++]<< 8 ) & 0xffff;
        rdlength = rdlength | (data[byteNum++]& 0xff);


        if(rclass ==1){
            if(rtype ==1 || rtype==28){
                byte ipAddress[] = new byte[rdlength];

                for(int i = 0; i<rdlength;i++){
                    ipAddress[i]=data[byteNum];
                    byteNum++;
                }

                return new ResourceRecord(fqdn,rtype,rclass,ttl,null,InetAddress.getByAddress(ipAddress));
            }else if(rtype ==2 || rtype==5){

                ResourceRecord rr = new ResourceRecord(fqdn,rtype,rclass,ttl,getfqdnHepler("",data,byteNum),null);
                for(int i = 0; i<rdlength;i++){
                    byteNum++;
                }
                return rr ;
            }



        }else{
            return new ResourceRecord(fqdn,rtype,rclass,ttl,null,null);
        }
        return null;

    }



    // You will also want methods to extract the response records and record
    // the important values they are returning. Note that an IPV6 reponse record
    // is of type 28. It probably wouldn't hurt to have a response record class to hold
    // these records.




}


