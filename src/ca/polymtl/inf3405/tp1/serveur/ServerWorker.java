package ca.polymtl.inf3405.tp1.serveur;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ServerWorker extends Thread {
    private final Socket clientSocket;//Connexion entrante avec un client cree par le programme principale
    private HashMap<String, String> donnees;
    private String nomUsager = "";

    public ServerWorker(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        if (gererConnexion()) {
            recevoirImage();
        }
    }

    /**
     * Gere la reception et l'application du filtre sur une image.
     */
    private void recevoirImage() {
        try {
            System.out.printf("En attente d'une image en provenance de %s\n", clientSocket.getInetAddress().getHostAddress());
            InputStream input = clientSocket.getInputStream();
            //on lit le nom du fichier ensuit l'image
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String nomFichier = reader.readLine();

            BufferedImage receveidImage = ImageIO.read(input);
            DataOutputStream outImage = new DataOutputStream(clientSocket.getOutputStream());
            if (receveidImage != null) {
                BufferedImage image = Sobel.process(receveidImage);//prend une BufferedImage la filtre puis retourne une autre Buffered image
                ImageIO.write(image, "PNG", outImage);
                outImage.flush();
                System.out.printf("[*] [%s - %s:%d - %s ] Image %s recue pour traitement",
                        nomUsager,
                        clientSocket.getInetAddress().getHostAddress(),
                        clientSocket.getPort(),
                        new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss").format(new Date()),
                        nomFichier);
            }
        } catch (IOException ioe) {
            System.out.println("Oups! Une erreur d'écriture du flux s'est produite!");
            ioe.printStackTrace();
        }
    }

    /**
     * Methode qui gere l'authentification via le socket
     */
    private boolean gererConnexion() {
        try {
            System.out.printf("Connexion du socket de %s sur le port %d afin d'authentifier l'utilisateur\n", clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            boolean authentificationReussie = false;
            initialiserBD();
            while (!authentificationReussie) {
                String credentials = in.readLine();
                authentificationReussie = connecterUtilisateur(credentials.split(",")[0], credentials.split(",")[1]);
                if (authentificationReussie) {
                    out.println("LOGIN_SUCCESSFUL");
                } else {
                    out.println("LOGIN_INVALID");
                }
            }
        } catch (IOException ioe) {
            System.out.println("Erreur d'ecriture ou de lecture dans le stream du socket");
            ioe.getStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Méthode qui valide les informations de connexion
     *
     * @param username
     * @param password
     * @return vrai si les informations de connexion sont valides ou si le compte de l'utilisateur a ete cree
     */
    private boolean connecterUtilisateur(String username, String password) {
        if (donnees.containsKey(username) && donnees.get(username).equals(password)) {
            nomUsager = username;
            return true;
        } else if (donnees.containsKey(username) && !donnees.get(username).equals(password)) {
            return false;
        } else {// Aucun utilisateur a ce nom existe et on lui cree un compte
            nomUsager = username;
            donnees.put(username, password);
            mettreAJourBaseDonnees();
            return true;
        }
    }

    /**
     * Prend le fichier et crée un HashMap à partir de ce dernier
     */
    private void initialiserBD() {
        donnees = new HashMap<>();
        try {
            File file = new File(Server.getDatabaseFile());
            if (!file.exists()) {
                file.createNewFile();
            }
            Scanner reader = new Scanner(file);
            while (reader.hasNextLine()) {
                String[] data = reader.nextLine().split(",");
                if (donnees != null) donnees.put(data[0], data[1]);
            }
            reader.close();
        } catch (FileNotFoundException e) {
            System.out.println("[-] Fichier de base de donnees introuvable");
        } catch (IOException ioe) {
            System.out.println("[-] Fichier n'a pas pu etre cree");
        }
    }

    /**
     * Quand un utlisateur n'apparait pas dans le fichier
     */
    private void mettreAJourBaseDonnees() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(Server.getDatabaseFile()));
            for (Map.Entry<String, String> entry : donnees.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }
            writer.close();
            System.out.println("[+] Nouvel utilisateur cree et ecrit dans le fichier");
        } catch (IOException ioe) {
            System.out.println("[-] Fichier de base de donnees introuvable pour la mise a jour des donnes");
        }
    }
}
