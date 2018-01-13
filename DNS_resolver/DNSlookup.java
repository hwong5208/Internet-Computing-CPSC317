
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * 
 */

/**
 * @author Donald Acton
 * This example is adapted from Kurose & Ross
 * Feel free to modify and rearrange code as you see fit
 */
public class DNSlookup {
    
    
    static final int MIN_PERMITTED_ARGUMENT_COUNT = 2;
    static final int MAX_PERMITTED_ARGUMENT_COUNT = 3;
	static int ttl = Integer.MAX_VALUE;
	static InetAddress rootNameServer;
	static int queryCount =0;
	static boolean tracingOn = false;
	static boolean IPV6Query = false;
	static String domainName;
	/**
     * @param args
     */
    public static void main(String[] args) throws Exception {
	String fqdn;
	DNSResponse response; // Just to force compilation
	int argCount = args.length;

		if (argCount < MIN_PERMITTED_ARGUMENT_COUNT || argCount > MAX_PERMITTED_ARGUMENT_COUNT) {
	    usage();
	    return;
	}
	
	rootNameServer = InetAddress.getByName(args[0]);
	domainName = args[1];
		fqdn = args[1];

	if (argCount == 3) {  // option provided
	    if (args[2].equals("-t"))
		tracingOn = true;
	    else if (args[2].equals("-6"))
		IPV6Query = true;
	    else if (args[2].equals("-t6")) {
		tracingOn = true;
		IPV6Query = true;
	    } else  { // option present but wasn't valid option
		usage();
		return;
	    }
	}

	// Start adding code here to initiate the lookup

		fqdn = args[1];

		try {
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(5000);

			try {
		        // query loop will provide the output result;
				queryLoop(fqdn, socket, false);

			} catch (SocketTimeoutException e) {
				// The lookup times out exception
				System.out.println(fqdn + " -2   A 0.0.0.0");
			} catch (TooManyQException e) {
				// Too many queries are attempted without resolving the address exception
				System.out.println(fqdn + " -3   A 0.0.0.0");
			} catch (ServerRespException e) {
				if (e.getMessage()=="6"){
					// The authoritative responese is true, but there is no answer in answer field exception
					System.out.println(fqdn + " -6   A 0.0.0.0");
				}else if (e.getMessage() != null)
					// Report a value of 3 in the RCODE of the header exception
					System.out.println(fqdn + " -1   A 0.0.0.0");
				else
					// Any other error that result in address not being resolved.
					System.out.println(fqdn + " -4   A 0.0.0.0");
			}

			socket.close();
			System.exit(0);
		} catch (Exception e) {
			// Any other error that result in address not being resolved.
			System.out.println(fqdn + " -4   A 0.0.0.0");
		}

	}

	private static InetAddress queryLoop(String fqdn, DatagramSocket socket,
										 boolean NSlookup) throws IOException, SocketTimeoutException,
			TooManyQException, ServerRespException {
		InetAddress server = rootNameServer;
		InetAddress timeoutServer = null;
		DatagramPacket packet;
		byte[] buf = new byte[512];
		byte[] response = null;
		DNSquery q;
		while (true) {
			if (queryCount >= 30)
				throw new TooManyQException();
			q = new DNSquery(fqdn, server ,IPV6Query);
			q.sendQuery(socket, tracingOn);
			queryCount++;
			packet = new DatagramPacket(buf, buf.length);
			try {
				boolean qIDmatch = false;
				while (!qIDmatch) {
					//keep receiving until the transaction ID in the response match with the query's ID
					socket.receive(packet);
					response = packet.getData();
					qIDmatch = compareID(q, response);
				}
			} catch (SocketTimeoutException e) {
				//resend the query if it is the first time timeout
				//otherwise throw an exception
				if (!server.equals(timeoutServer)) {
					timeoutServer = server;
					continue;
				} else {
					throw new SocketTimeoutException();
				}
			}

			DNSResponse r = new DNSResponse(response, response.length, q);

			if (tracingOn) {
				r.dumpResponse();
			}

			//check the respond code
			serverResponseCheck(r.getrCode(),r.getAuthoritative(),r.getAnsList());


			server = r.queryNext();

			// Get answer
			if (server == null) {
				ResourceRecord answer;
				if(IPV6Query) {
					answer = r.getIPv6Result();
				}else{
					answer=r.getIPv4Result();
				}
				if (answer != null) {
					// update ttl and return if getting an A record as the
					// answer
					if (!NSlookup) {
						int ttll = answer.getTtl();
						ttl = Math.min(ttl, ttll);

						//print out the answer result
						ResourceRecord[] anslist = r.getAnsList();
						for (int i=0; i<anslist.length;i++){
							anslist[i].printResult(domainName);
						}
					}
					return answer.getIPaddr();
				}


				if ((r.getName()) != null) {
					// call queryLoop recursively to search for the returned
					// CNAME
					// update ttl and return after the recursive call return
					if (!NSlookup) {
						int ttll = r.getTTL();
						ttl = Math.min(ttl, ttll);
						return queryLoop(r.getName(), socket, false);
					}
					return queryLoop(r.getName(), socket, true);
				}

				if (r.getNSName() == null)
					// throw exception if there is no answer and also no server
					// to ask for the answer
					throw new ServerRespException();

				// NS returned is a CNAME, search for the ip address for that
				// server
				server = queryLoop(r.getNSName(), socket, true);
			}

		}

	}
	//check if query's ID matches response's ID
	private static boolean compareID(DNSquery q, byte[] response) {
		int qID = q.getqID();
		int rID = (response[0] << 8) & 0xff00;
		rID = rID | (response[1] & 0xff);
		if (qID == rID)
			return true;
		return false;

	}
	//check the response's reply code
	//do nothing if it is 0, otherwise throw appropriate exception
	private static void serverResponseCheck(int replyCode,Boolean authoriative,ResourceRecord[] anslist)
			throws ServerRespException {
		if (replyCode == 0){
			// throw exception when the authoritative repsonse is true,
			// but there is no answer
			if (authoriative && (anslist.length==0)){
			 throw new ServerRespException("6");
			}
			return;}
		if (replyCode == 3)
			throw new ServerRespException("3");
		else
			throw new ServerRespException();

	}



    private static void usage() {
	System.out.println("Usage: java -jar DNSlookup.jar rootDNS name [-6|-t|t6]");
	System.out.println("   where");
	System.out.println("       rootDNS - the IP address (in dotted form) of the root");
	System.out.println("                 DNS server you are to start your search at");
	System.out.println("       name    - fully qualified domain name to lookup");
	System.out.println("       -6      - return an IPV6 address");
	System.out.println("       -t      - trace the queries made and responses received");
	System.out.println("       -t6     - trace the queries made, responses received and return an IPV6 address");
    }
}


