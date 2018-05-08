import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
/**
 * Classe que implementa a comunicacao entre os clientes e o servidor
 * 
 * @author G035 Catarina Fitas Carlos Brito Goncalo Lobo
 */
public class ManUsers {

	private static String PASSWORD;
	private static Scanner sc;

	public ManUsers() {
	}

	public static void main(String[] args) throws IOException {
		sc = new Scanner(System.in);

		// Password do MAC
		System.out.println("Insira a password:");
		PASSWORD = sc.nextLine();

		File repositorio_s = new File("REPOSITORIO_SERVIDOR");
		if (!repositorio_s.exists()) {
			repositorio_s.mkdirs();
		}
		File repositorio_c = new File("REPOSITORIO_CLIENTES");
		if (!repositorio_c.exists()) {
			repositorio_c.mkdirs();
		}

		// mac do ManUsers
		File mac = new File("REPOSITORIO_SERVIDOR/mac.txt");
		if (!mac.exists()) {
			System.out.println("Criacao de um MAC.");
			mac.createNewFile();
			generateMac("REPOSITORIO_SERVIDOR/mac.txt", "REPOSITORIO_SERVIDOR/users.txt");
			System.out.println(
					"Introduza a operacao: \nregistar utilizador: -r username password\n; -d delete username; -c changePassword username newPassword");
			String linha = sc.nextLine();
			String[] lSplit = linha.split(" ");
			switch (lSplit[0]) {
			case "-r": // registar utilizador
				if (registarUtilizador(lSplit[1], lSplit[2])) {
					System.out.println("Utilizador registado.");
				} else {
					System.err.println("Erro a registar o utilizador.");
				}
				break;
			case "-d": // remover utilizador
				if (removerUtilizador(lSplit[1])) {
					System.out.println("Utilizador removido.");
				} else {
					System.err.println("Erro a remover o utilizador.");
				}
				break;
			case "-c": // mudar a password de um utilizador
				if (changePassword(lSplit[1], lSplit[2])) {
					System.out.println("Password mudada com sucesso.");
				} else {
					System.err.println("Erro a mudar a password.");
				}
				break;
			default:
				System.err.println("Operacao invalida.");
				break;
			}
		} else {
			if (verificaMac("REPOSITORIO_SERVIDOR/mac.txt", "REPOSITORIO_SERVIDOR/users.txt") == -1) {
				terminaManUsers();
			} else {
				System.out.println(
						"Introduza a operacao:\nregistar utilizador: -r username password\nremover utilizador: -d username\nmudar a password: -c username newPassword\n");
				String linha = sc.nextLine();
				String[] lSplit = linha.split(" ");
				switch (lSplit[0]) {
				case "-r":
					if (registarUtilizador(lSplit[1], lSplit[2])) {
						System.out.println("Utilizador registado com sucesso.");
					} else {
						System.err.println("Erro a registar o utilizador.");
					}
					break;
				case "-d":
					if (removerUtilizador(lSplit[1])) {
						System.out.println("Utilizador removido.");
					} else {
						System.err.println("Erro a remover o utilizador.");
					}
					break;
				case "-c":
					if (changePassword(lSplit[1], lSplit[2])) {
						System.out.println("Password alterada com sucesso.");
					} else {
						System.err.println("Erro a alterar a password.");
					}
					break;
				default:
					System.err.println("Operacao invalida.");
					break;
				}
			}
		}
	}

	// faz a cena dele
	

	/**
	 * MEtodo que remove um utilizador username
	 * @param username nome do utilizador
	 * @return true se removeu; false caso contrario
	 * @throws IOException
	 */
	public static boolean removerUtilizador(String username) throws IOException {
		if (verificaMac("REPOSITORIO_SERVIDOR/mac.txt", "REPOSITORIO_SERVIDOR/users.txt") == 0) {
			BufferedReader br = new BufferedReader(new FileReader("REPOSITORIO_SERVIDOR/users.txt"));
			File follow = new File("REPOSITORIO_SERVIDOR/users.txt");
			File fAux = new File("REPOSITORIO_SERVIDOR/temp.txt");
			if (!fAux.exists())
				fAux.createNewFile();

			FileWriter aux = new FileWriter(fAux, true);
			String linha;
			String[] linhaSp;
			StringBuilder sb = new StringBuilder();
			while ((linha = br.readLine()) != null) {
				linhaSp = linha.split(":");
				if (linhaSp[0].equals(username)) {
					sb = new StringBuilder();
				} else {
					sb.append(linha);
					aux.write(sb.toString());
					sb = new StringBuilder();
				}
				aux.write("\n");
			}
			br.close();
			aux.close();
			Files.delete(follow.toPath());
			fAux.renameTo(new File("REPOSITORIO_SERVIDOR/users.txt"));
			generateMac("REPOSITORIO_SERVIDOR/mac.txt", "REPOSITORIO_SERVIDOR/users.txt");
			return true;
		} else {
			terminaManUsers();
			return false;
		}
	}

	/**
	 * MEtodo que regista um utilizador user com a password password
	 * @param user nome do utilizador
	 * @param password password
	 * @return true se registou, false caso contrario
	 * @throws IOException
	 */
	public static boolean registarUtilizador(String user, String password) throws IOException {
		if (verificaMac("REPOSITORIO_SERVIDOR/mac.txt", "REPOSITORIO_SERVIDOR/users.txt") == 0) {
			File passwords = new File("REPOSITORIO_SERVIDOR/users.txt");
			String conv;
			String salt;
			if (!passwords.exists()) {
				passwords.createNewFile();
				FileWriter fw = new FileWriter(new File("REPOSITORIO_SERVIDOR/users.txt"), true);
				try {
					MessageDigest md = MessageDigest.getInstance("SHA-256");
					byte buf[] = password.getBytes();
					byte hash[] = md.digest(buf);
					conv = DatatypeConverter.printBase64Binary(hash);
					salt = saltGenerator();
					fw.append(user + ":" + salt + ":" + conv + "\n");
					fw.close();
					generateMac("REPOSITORIO_SERVIDOR/mac.txt", "REPOSITORIO_SERVIDOR/users.txt");
					File myRep = new File("myRep");
					if (!myRep.exists()) {
						myRep.mkdirs();
					}
					
					return true;
				} catch (NoSuchAlgorithmException e) {
					System.err.println(e.getMessage());
				}
				fw.close();
			} else { // ficheiro existe
				FileWriter fw = new FileWriter(new File("REPOSITORIO_SERVIDOR/users.txt"), true);
				try {
					MessageDigest md = MessageDigest.getInstance("SHA-256");
					byte buf[] = password.getBytes();
					byte hash[] = md.digest(buf);
					conv = DatatypeConverter.printBase64Binary(hash);
					salt = saltGenerator();
					fw.append(user + ":" + salt + ":" + conv + "\n");
					fw.close();
					generateMac("REPOSITORIO_SERVIDOR/mac.txt", "REPOSITORIO_SERVIDOR/users.txt");
					return true;
				} catch (NoSuchAlgorithmException e) {
					System.err.println(e.getMessage());
				}
				fw.close();
			}
		} else {
			terminaManUsers();
		}
		return false;
	}

	/**
	 * Metodo que muda a password de um utilizador
	 * @param username nome do utilizador a mudar a password
	 * @param newPassword nova password
	 * @return true se alterou; false caso contrario
	 * @throws IOException
	 */
	public static boolean changePassword(String username, String newPassword) throws IOException {
		if (verificaMac("REPOSITORIO_SERVIDOR/mac.txt", "REPOSITORIO_SERVIDOR/users.txt") == 0) {
			String conv = null;
			try {
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				byte buf[] = newPassword.getBytes();
				byte hash[] = md.digest(buf);
				conv = DatatypeConverter.printBase64Binary(hash);
				changePasswordAux(username, newPassword, conv);
			} catch (NoSuchAlgorithmException e) {
				System.err.println(e + e.getMessage());
			}
		} else {
			terminaManUsers();
			return false;
		}
		return true;

	}

	/**
	 * Metodo auxiliar que muda a password de um utilizador
	 * @param username utilizador a mudar a password
	 * @param newPassword nova password
	 * @param newMac nova mac
	 * @throws IOException
	 */
	private static void changePasswordAux(String username, String newPassword, String newMac) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("REPOSITORIO_SERVIDOR/users.txt"));
		File follow = new File("REPOSITORIO_SERVIDOR/users.txt");
		File fAux = new File("REPOSITORIO_SERVIDOR/temp.txt");
		if (!fAux.exists())
			fAux.createNewFile();

		FileWriter aux = new FileWriter(fAux, true);
		String linha;
		String[] linhaS;
		StringBuilder sb = new StringBuilder();
		while ((linha = br.readLine()) != null) {
			linhaS = linha.split(":");
			if (linhaS[0].equals(username)) {
				sb.append(linhaS[0] + ":" + linhaS[1] + ":" + newMac);
				aux.write(sb.toString());
				sb = new StringBuilder();
			} else {
				sb.append(linha);
				aux.write(sb.toString());
				sb = new StringBuilder();
			}
			aux.write("\n");
		}
		br.close();
		aux.close();
		Files.delete(follow.toPath());
		fAux.renameTo(new File("REPOSITORIO_SERVIDOR/users.txt"));
		generateMac("REPOSITORIO_SERVIDOR/mac.txt", "REPOSITORIO_SERVIDOR/users.txt");
	}

	/**
	 * MEtodo que gera o salt
	 * @return salt
	 */
	private static String saltGenerator() {
		int max = 999999;
		int min = 0;
		int salt = (int) Math.round(Math.random() * (max - min + 1) + min);
		return Integer.toString(salt);
	}

	/**
	 * Metodo que gera o mac que protege o ficheiro fileToProtect e guarda-o em fileMac
	 * @param fileMac ficheiro onde ira ficar guardado o mac
	 * @param fileToProtect ficheiro a ser protegido pelo mac
	 */
	private static void generateMac(String fileMac, String fileToProtect) {
		File f2 = new File(fileToProtect);
		Mac mac;
		FileWriter fw;
		BufferedWriter bw;
		FileInputStream fis;
		try {
			fw = new FileWriter(fileMac);
			bw = new BufferedWriter(fw);
			mac = Mac.getInstance("HmacSHA256");
			byte[] pass = PASSWORD.getBytes();
			SecretKey key = new SecretKeySpec(pass, "HmacSHA256");
			mac.init(key);

			if (f2.exists()) {
				fis = new FileInputStream(f2);
				long tamanhoFicheiro = f2.length(); // Tamanho do ficheiro

				byte[] byteFicheiro = new byte[1024];

				int nBytes = 0;// copia para o buffer os bytes do ficheiro e guarda o numero de bytes copiados
				do {
					// Enquanto houver bytes para enviar
					nBytes = fis.read(byteFicheiro, 0, byteFicheiro.length);
					mac.update(byteFicheiro);
				} while ((tamanhoFicheiro -= nBytes) > 0);

				fis.close();
			}

			bw.append(DatatypeConverter.printBase64Binary(mac.doFinal()));
			bw.close();
		} catch (NoSuchAlgorithmException | InvalidKeyException | IllegalStateException | IOException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Metodo que verifica o mac
	 * @param fileMac ficheiro onde o mac esta guardado
	 * @param fileToProtect ficheiro a proteger
	 * @return true se mac eh igual; false caso contrario
	 * @throws IOException
	 */
	private static int verificaMac(String fileMac, String fileToProtect) throws IOException {
		// Ler MAC do ficheiro
		FileReader fr = new FileReader(fileMac);
		BufferedReader br = new BufferedReader(fr);
		String MAC = br.readLine();

		File f2 = new File(fileToProtect);
		FileInputStream fis;
		Mac mac = null;
		try {
			// Gerar chave secreta para a password inserida no servidor
			byte[] passM = PASSWORD.getBytes();
			SecretKey key = new SecretKeySpec(passM, "HmacSHA256");
			mac = Mac.getInstance("HmacSHA256");
			mac.init(key);

			if (f2.exists()) {
				fis = new FileInputStream(fileToProtect);
				long tamanhoFicheiro = fileToProtect.length(); // Tamanho do ficheiro

				byte[] byteFicheiro = new byte[1024];

				int nBytes = 0;// copia para o buffer os bytes do ficheiro e guarda o numero de bytes copiados
				do {
					// Enquanto houver bytes para enviar
					nBytes = fis.read(byteFicheiro, 0, byteFicheiro.length);
					mac.update(byteFicheiro);
				} while ((tamanhoFicheiro -= nBytes) > 0);

				fis.close();
			}

		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			System.err.println(e.getMessage());
		}
		br.close();
		return MAC.equals(DatatypeConverter.printBase64Binary(mac.doFinal())) ? 0 : -1;
	}

	/**
	 * Metodo que termina a execucao da classe ManUsers se o mac estiver errado
	 * @throws IOException
	 */
	private static void terminaManUsers() throws IOException {
		System.err.println("Ocorreu um erro no MAC!");
		System.exit(-1);
	}
}