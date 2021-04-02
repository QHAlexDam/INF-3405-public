package ca.polymtl.inf3405.tp1.serveur;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {

    private static final String DATABASE_FILE = "db.csv";
    private static final String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    private ServerSocket serverSocket;
    private int portNumber;

    public Server() {
        System.out.println("Bienvenue sur le serveur d'images Sobel");
        Scanner sc = new Scanner(System.in);
        String ipString;

        //Saisie de ip et port
        do {
            System.out.println("[+] Veuillez saisir l'adresse IP du serveur: ");
            ipString = sc.nextLine();
        }
        while (!validerFormatIp(ipString));

        do {//Doit-on verifier si le port est deja occupe?
            System.out.println("[+] Veuillez saisir le numero du port compris entre 5000 et 5050: ");
            String stringPortNumber = sc.nextLine();
            try {
                portNumber = Integer.parseInt(stringPortNumber);
            } catch (NumberFormatException nfe) {
                portNumber = 0;
            }
        } while (!(portNumber >= 5000 && portNumber <= 5050));

        try {
            this.serverSocket = new ServerSocket(this.portNumber);
        } catch (IOException ioe) {
            throw new RuntimeException("[-] Impossible d'ouvrir le port demande", ioe);
        }
        System.out.println("[*] Serveur en attente de connexions...");

        while (true) {
            Socket clientSocket = null;
            try {
                synchronized (serverSocket) {
                    clientSocket = serverSocket.accept();
                    new Thread(new ServerWorker(clientSocket)).start();
                }
            } catch (Exception ioe) {
                System.out.println("[-] Erreur survenue lors de l'ouverture d'une connexion");
            }
        }
    }

    private static boolean validerFormatIp(String ip) {
        Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();//Une gracieusete de https://mkyong.com/regular-expressions/how-to-validate-ip-address-with-regular-expression/
    }

    public static String getDatabaseFile() {
        return DATABASE_FILE;
    }

    public static void main(String[] args) {
        new Server();
    }
}
