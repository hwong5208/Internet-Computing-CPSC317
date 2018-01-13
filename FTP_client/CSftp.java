


import java.io.*;
import java.lang.System;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

//
// This is an implementation of a simplified version of a command
// line ftp client. The program always takes two arguments
//


public class CSftp
{
    static final int MAX_LEN = 255;
    static final int ARG_CNT = 2;



    public static void main(String [] args)
    {
        byte cmdString[] = new byte[MAX_LEN];
        Scanner scan = new Scanner(new BufferedReader(new InputStreamReader(System.in)));
        FTPConnection ftp = new FTPConnection();


        // Get command line arguments and connected to FTP

        // If the arguments are invalid or there aren't enough of them
        // then exit.

	    if (args.length != ARG_CNT) {
	    System.out.print("Usage: cmd ServerAddress ServerPort\n");
	    return;
	    }

	    // Establish the FPT connection
        if (args.length == 2){
            ftp.open(args[0],args[1]);
            ftp.read("");
        }


        for (int len = 1; len > 0;) {
            System.out.print("csftp> ");

            /// / Start processing the command here.

            // Read user's input
            String input = scan.nextLine();


            // if user input is not empty and don't include "#", process input
            if(!input.isEmpty()&&!input.contains("#")){

                // Allow the command is separated by one or more spaces or tabs
                String[] fullcmd = input.trim().split("\\s+");

                String cmd = fullcmd[0].toLowerCase().trim();

                // check user provided the valid command
                if (!cmd.matches("open|close|quit|user|pw|dir|cd|get|features|pwd")){
                    ftp.getError("0x001","","");
                }

                if (fullcmd.length > 2  ){
                   ftp.getError("0x002","","");
                }


                switch(cmd){
                    case "open":
                       if (fullcmd.length == 2){
                           if(fullcmd[1]!=null) {
                               ftp.open(fullcmd[1], "21");
                               ftp.read("");
                           }
                        }else if(fullcmd.length ==3){

                               ftp.open(fullcmd[1], fullcmd[2]);
                               ftp.read("");
                       }else{
                           ftp.getError("0x002","","");
                       } ;
                        break;
                    case "quit":
                        if(fullcmd.length==1) {
                        System.exit(0);
                        }else{
                            ftp.getError("0x002","","");
                        }
                        break;
                    case "user":
//                        ftp.write("USER","anonymous");
                        if(fullcmd.length==2) {
                            ftp.write("USER", fullcmd[1]);
                            ftp.read("");
                            ftp.write("PASS", "");
                            ftp.read("");
                        }else{
                            ftp.getError("0x002","","");
                        }
                        break;
                    case "pw":
                        if(fullcmd.length==2) {
                            ftp.write("PASS", fullcmd[1]);
                            ftp.read("");
                        }else{
                            ftp.getError("0x002","","");
                        }
                        break;
                    case "features":
                        if(fullcmd.length==1) {
                            ftp.write("FEAT", "");
                            ftp.read("");
                        }else{
                            ftp.getError("0x002","","");
                            }
                        break;
                    case "cd":
                        if(fullcmd.length==2) {
                            ftp.write("CWD", fullcmd[1]);
                            ftp.read(fullcmd[1]);
                            //ftp.read();
                        }else{
                            ftp.getError("0x002","","");
                        }
                        break;
                    case "dir":
                        if(fullcmd.length==1) {
                            ftp.write("PASV", "");
                            ftp.read("");
                            ftp.write("LIST", "");
                            ftp.read("");
                            ftp.dataSocketRead();
                            ftp.read("");
                        }else{
                            ftp.getError("0x002","","");
                        }
                        break;
                    case "get":
                        if(fullcmd.length==2) {
                        ftp.write("TYPE","I");
                        ftp.read("");
                        ftp.write("PASV","");
                        ftp.read("");
                        ftp.write("RETR",fullcmd[1]);
                        // if the file cannot access, it will out error.
                        if(ftp.read(fullcmd[1])){
                            ftp.dataSocketSaveFile(fullcmd[1]);
                            ftp.read("");
                        }
                        }else{
                            ftp.getError("0x002","","");
                        }

                        break;
                    case "pwd":
                        if(fullcmd.length==1) {
                        ftp.write("PWD","");
                        ftp.read("");
                        }else{
                            ftp.getError("0x002","","");
                        }
                        break;
                    default:
                    continue;
                }

            }

        };
    }
    private static class FTPConnection{
        private Socket controlSocket;
        private Socket dataSocket;
        private BufferedReader in;
        private PrintWriter out;
        private BufferedReader dataSocket_in;
        private PrintWriter dataSocket_out;

        public FTPConnection(){
            controlSocket = new Socket();
        }

        // Establish connection in control connection
        public Boolean open( String hostname,String port ){
            try {
                controlSocket = new Socket();
                controlSocket.connect(new InetSocketAddress(hostname, Integer.parseInt(port)),20000 );
                out = new PrintWriter(controlSocket.getOutputStream(),true);
                in = new BufferedReader(new InputStreamReader(controlSocket.getInputStream() ));
                System.out.println("Making connecting to "+hostname);
            }catch(IOException e){ getError("0xFFFC",hostname,port);
                return false;
            }catch(Exception e){
                getError("0xFFFC",hostname,port);
                return false;
            }

            return true;
        }

        // write in control connection
        public void write(String str1,String str2){
            try {
                out.write(str1 + " " + str2 + "\r\n");
                out.flush();
                String s = str1 + " " + str2 ;
                System.out.println("--> " + s);
            }catch(Exception e){
                getError("0xFFFD","","");
            }
        }

        // Read server's respond in control connection
        public Boolean read(String str){
            try{
                String s = null;
                // provide time for BufferedReader to store all value;
                Thread.sleep(100);
                if(in != null) {
                    while (in.ready()) {
                        s = in.readLine();
                        System.out.println("<-- " + s);

                        if(s.contains("550")&& str!="" ){
                            getError("0x38E",str,"");
                            return false;
                        }

                        if (s.contains("Entering Passive Mode")) {
                            String x = s.split("\\(")[1];
                            String y = x.split("\\)")[0];
                            String a = y.split("\\,")[0];
                            String b = y.split("\\,")[1];
                            String c = y.split("\\,")[2];
                            String d = y.split("\\,")[3];
                            String e = y.split("\\,")[4];
                            String f = y.split("\\,")[5];
                            String hostname = a + "." + b + "." + c + "." + d;

                            Integer port = Integer.parseInt(e) * 256 + Integer.parseInt(f);
                            dataSocketOpen(InetAddress.getByName(hostname), port);
                        }
                    }
                }

            }catch (IOException e)   {
                getError("0xFFFD","","");
            } catch (InterruptedException e) {
                getError("0xFFFF","","");
            }catch(Exception e){ getError("0xFFFD","","");}
            return true;
        }

        // Establish connection in data transfer connection
        public void dataSocketOpen(InetAddress hostname, Integer port){
            try {
                dataSocket = new Socket();
                dataSocket.connect(new InetSocketAddress(hostname, port),10000 );
                dataSocket_out = new PrintWriter(dataSocket.getOutputStream(),true);
                dataSocket_in = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));

            }catch(IOException e){ getError("0x3A2",hostname.toString(),Integer.toString(port));
            } catch(Exception e){ getError("0x3A2",hostname.toString(),Integer.toString(port));}
        }


        // Read server's respond in data transfer connection
        public void dataSocketRead(){
            try{
                // provide time for BufferedReader to store all value;
                Thread.sleep(100);
                String s = null;
                if(dataSocket_in != null) {
                    while (dataSocket_in.ready()) {
                        s = dataSocket_in.readLine();
                        System.out.println(  s);
                    }
                }
            }catch (IOException e)   {
                getError("0x3A7","","");
            }catch (InterruptedException e){
                getError("0xFFFF",e.getMessage(),"");
            }catch (Exception e){
                getError("0x3A7",e.getMessage(),"");
            }

        }

        // save file in local drive
        public void dataSocketSaveFile(String filename){

            try {
                DataInputStream in_file = new DataInputStream(dataSocket.getInputStream());
                FileOutputStream out_file = new FileOutputStream(filename);
                byte[] buf = new byte[4096];
                int length;
                while ((length = in_file.read(buf)) > 0) {
                    out_file.write(buf, 0, length);
                }
                out_file.close();
                dataSocket.close();
            }catch(IOException e){
                getError("0x3A7","","");
            }catch (Exception e){
                getError("0xFFFF",e.getMessage(),"");
            }
        }


        public  String getErrorString(String str1,String str2, String str3){

            switch (str1){
                case "0x001":
                    return str1 + " Invalid command " ;
                case "0x002":
                    return str1 + " Incorrect number of arguments";
                case "0x38E":
                    return str1 + " Access to local " + str2 + " denied";
                case "0xFFFC":
                    return str1 + " Control connection to "+ str2 + " on port " +str3 + " failed to open";
                case "0xFFFD":
                    return str1 + " Control connection I/O error, closing control connection";
                case "0x3A2":
                    return str1 + " Data transfer connection to "+str2 +" on port "+ str3 + " failed to open";
                case "0x3A7":
                    return str1 + " Data transfer connection I/O error, closing data connection";
                case "0xFFFE":
                    return str1 + " Input error while reading commands, terminating";
                case "0xFFFF":
                    return str1 + "Processing error " +str2;
            }
            return "0x001 Invalid command";
        }



     public void getError(String str1, String str2,String str3){

         if(str1 =="0xFFFC"|str1 =="0xFFDD" ){
             System.out.println(getErrorString(str1,str2,str3 ));
             System.exit(0);}
         else{
             System.out.println(getErrorString(str1,str2,str3 ));
         }
     }
    }
}




