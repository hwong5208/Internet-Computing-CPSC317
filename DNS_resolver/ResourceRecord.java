import java.net.InetAddress;

/**
 * Created by hwong on 3/11/2017.
 */
public class ResourceRecord {



        private String rName;
        private int rtype;
        private int rclass;
        private int ttl;
        private String name;
        private InetAddress IPaddress;

        ResourceRecord(String name, int type, int c, int t, String n, InetAddress ip  ){
            rName = name;
            rtype = type;
            rclass = c;
            ttl = t;
            this.name = n;
            IPaddress = ip;
        }

        String getrName(){
            return rName;
        }
        int getRtype(){
            return rtype;
        }
        int getRclass(){
            return rclass;
        }
        int getTtl(){
            return ttl;
        }
        InetAddress getIPaddr(){
            return IPaddress;
        }
        String getName(){
            return name;
        }


        void print(){

            switch (rtype){
                case 1:
                    System.out.format("       %-30s %-10d %-4s %s\n", rName, ttl, "A", IPaddress.getHostAddress());
                    break;
                case 2:
                    System.out.format("       %-30s %-10d %-4s %s\n", rName, ttl, "NS", name);
                    break;
                case 5:
                    System.out.format("       %-30s %-10d %-4s %s\n", rName, ttl, "CN", name);
                    break;
                case 28:
                    System.out.format("       %-30s %-10d %-4s %s\n", rName, ttl, "AAAA", IPaddress.getHostAddress());
                    break;
                default:
                    System.out.format("       %-30s %-10d %-4s %s\n", rName, ttl, Integer.toString(rtype), "-----");
            }

        }


    void printResult(String nm ){

        switch (rtype){
            case 1:
                System.out.format("%s %d   %s %s\n", nm, ttl, "A", IPaddress.getHostAddress());
                break;
            case 28:
                System.out.format("%s %d   %s %s\n", nm, ttl, "AAAA", IPaddress.getHostAddress());
                break;
            default:
                System.out.format("");
        }

    }

    }



