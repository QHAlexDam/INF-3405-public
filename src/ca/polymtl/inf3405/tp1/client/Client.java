package ca.polymtl.inf3405.tp1.client;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {

    private static Socket socket;

    public Client() {
        Scanner sc = new Scanner(System.in);
        // Demander Adresse et port du serveur
        String serverAddress = "";

        // Creation d'une nouvelle connexion avec le serveur
        boolean connexionValide;
        do {
            do {
                System.out.println("[+] Veuillez saisir l'adresse IP du serveur: ");
                serverAddress = sc.nextLine();
            } while (!isValidIP(serverAddress));

            int port = 0;
            do {
                System.out.println("[+] Veuillez saisir le numero du port compris entre 5000 et 5050: ");
                port = sc.nextInt();
            } while (!isValidPort(port));

            try {
                socket = new Socket(serverAddress, port);
                connexionValide = true;
            } catch (UnknownHostException uhe) {
                System.out.println("[-] Erreur: Le serveur auquel vous tentez de connecter semble inrejoignable");
                connexionValide = false;
            } catch (IOException ioe) {
                System.out.println("[-] Erreur: création du socket impossible");
                ioe.printStackTrace();
                connexionValide = false;
            }
            connexionValide = false;
        }
        while (!connexionValide);

        // Creation des canals entrants et sortants pour communiquer avec le serveur
        BufferedReader in;
        PrintWriter out;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            //Demander nom d'utilisateur et mot de passe
            String userName = "";
            String password = "";
            boolean accountIsValid = true;
            do {
                sc = new Scanner(System.in);
                do {
                    System.out.println("[+] Veuillez saisir votre nom d'utilisateur: ");
                    userName = sc.nextLine();
                    System.out.println("[+] Veuillez saisir votre mot de passe: ");
                    password = sc.nextLine();
                } while (!(infoIsValidFormat(userName) && infoIsValidFormat(password)));
                //Envoi des donnees vers le serveur
                out.println(userName + "," + password);

                //Reponse du serveur
                String serverResp = in.readLine();
                switch (serverResp) {
                    case "LOGIN_SUCCESSFUL": //user existe et pass est correct
                        System.out.println("[*] Vous etes connecte en tant que: " + userName);
                        accountIsValid = true;
                        break;
                    case "LOGIN_INVALID": //user existe, mais pass incorrect
                        System.out.println("[-] Erreur: mot de passe incorrect pour cet usager");
                        accountIsValid = false;
                        break;
                    default:
                        System.out.println("[-] Erreur: aucune reponse du serveur");
                        accountIsValid = false;
                        break;
                }
            } while (!accountIsValid);
        } catch (IOException ioe) {
            System.out.println("[-] Erreur: communication avec le socket perdue");
            return;//rien a faire pour reparer l'erreur, c'est du cote serveur
        }

        //Envoie de l'image
        Path imgPath;
        String saisie;
        do {
            System.out.println("[+] Entrez le chemin de l'image: ");
            saisie = sc.nextLine();
            imgPath = Paths.get(saisie);
        } while (saisie.equals("") || !Files.exists(imgPath));

        DataOutputStream outImage;
        BufferedImage buffImg;
        try {
            outImage = new DataOutputStream(socket.getOutputStream());
            File sourceImg = imgPath.toFile();
            out.println(sourceImg.getName());//Envoi du nom du fichier
            buffImg = ImageIO.read(sourceImg);
            ImageIO.write(buffImg, "PNG", outImage);
            out.flush();
        } catch (IOException ioe) {
            System.out.println("[-] Erreur: echec de l'envoi de l'image ");
        }

        System.out.println("[+] Image envoyee au serveur pour qu'il applique le filtre sur celle-ci");

        File file;
        try {
            //Reception de l'image modifie
            InputStream input = socket.getInputStream();
            BufferedImage image = ImageIO.read(input);
            String filename = "filtre-" + new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date()) + ".png";
            file = new File(System.getProperty("user.dir"), filename);
            file.createNewFile();
            ImageIO.write(image, "PNG", file);
            System.out.println("[+] Image filtree recue et sauvegardee sous " + file.getAbsolutePath());
            System.out.println("[*] Déconnexion... ");
            // Fermeture de la connexion avec le serveur
            out.close();
            in.close();
            socket.close();
            sc.close();
        } catch (IOException ioe) {
            System.out.println("[-] Erreur: sauvegarde du fichier impossible");
            return;
        }
    }

    public static void main(String[] args) {
        new Client();
    }

    public static boolean isValidIP(String ip) {
        String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

        Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(ip);
        if (!matcher.matches()) {
            System.out.println("[-] Erreur: addresse IP non valide.");
        }
        return matcher.matches();// Source: Java IP Address (IPv4) regex examples [disponible à https://mkyong.com/regular-expressions/how-to-validate-ip-address-with-regular-expression/] (octobre 2020)
    }

    public static boolean isValidPort(int port) {
        if (port >= 5000 && port <= 5050)
            return true;
        else
            System.out.println("[-] Erreur: port non valide.");
        return false;
    }

    public static boolean infoIsValidFormat(String s) {
        boolean isValid = true;
        for (char c : s.toCharArray()) {
            if (c == ' ') {
                System.out.println("[-] Erreur: votre nom d'utilisateur et mot de passe ne peut pas contenir d'espaces)");
                isValid = false;
            }
        }
        return isValid;
    }

}




	

	






