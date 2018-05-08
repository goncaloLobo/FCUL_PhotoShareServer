import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.xml.bind.DatatypeConverter;

//Servidor PhotoShareServer

/**
 * Classe que implementa a comunicacao entre os clientes e o servidor
 * 
 * @author G035 Catarina Fitas Carlos Brito Goncalo Lobo
 */
public class PhotoShareServer {

	private static Scanner sc;
	private static ServerSocket sSoc;
	private static String PASSWORD;

	public static void main(String[] args) throws IOException {
		sc = new Scanner(System.in);
		System.out.println("servidor: main");

		// Password do MAC
		System.out.println("Insira a password:");
		PASSWORD = sc.nextLine();

		File repositorio = new File("REPOSITORIO_SERVIDOR");
		if (!repositorio.exists()) {
			repositorio.mkdirs();
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
		} else {
			if (verificaMac("REPOSITORIO_SERVIDOR/mac.txt", "REPOSITORIO_SERVIDOR/users.txt") == -1) {
				System.err.println("Mac errado!");
				System.exit(-1);
			}
			PhotoShareServer server = new PhotoShareServer();
			server.startServer();
		}
		sc.close();
	}

	public void startServer() throws IOException {
		System.setProperty("javax.net.ssl.keyStore", "serverKeyStore.keyStore");
		System.setProperty("javax.net.ssl.keyStorePassword", "server");
		try {
			ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
			sSoc = ssf.createServerSocket(23232);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		while (true) {
			Socket inSoc = null;
			try {
				inSoc = sSoc.accept();
			} catch (IOException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
			ServerThread newServerThread = new ServerThread(inSoc);
			newServerThread.start();
		}
	}

	/**
	 * Classe ServerThread que extende a classe Thread e que implementa as threads
	 * utilizadas para a comunicacao entre os clientes
	 */
	static class ServerThread extends Thread {

		private static ObjectOutputStream outStream;
		private static FileOutputStream output;
		private static ObjectInputStream inStream;
		private static String userCorrente = null;
		private static String[] fotos = null;
		private static String[] splitDoComentario;
		private static String[] comandoAux;

		ServerThread(Socket inSoc) throws IOException {
			System.out.println("thread do server para cada cliente");
			outStream = new ObjectOutputStream(inSoc.getOutputStream());
			inStream = new ObjectInputStream(inSoc.getInputStream());
		}

		/**
		 * Metodo que trata da comunicacao entre o servidor e o cliente
		 */
		public void run() {
			try {
				// comando inserido pelo cliente
				String comando = (String) inStream.readObject();
				comandoAux = comando.split(" ");
				splitDoComentario = comando.split("\"");
				String[] userAndFoto;
				// autenticar
				if (comando.equals("quit")) {
					outStream.close();
					inStream.close();
				} else if (!comandoAux[1].startsWith("-")) {
					userCorrente = comandoAux[1];
					if (comandoAux.length == 3) {
						String pwd = (String) inStream.readObject();
						int autentica = autenticarUtilizador(userCorrente, pwd);
						switch (autentica) {
						case 1: // user nao existe
							outStream.writeObject(1);
							break;
						case 2: // user autenticado
							outStream.writeObject(2);
							break;
						case -2: // user nao existe
							outStream.writeObject(-2);
							break;
						case -1: // user existe mas pwd errada
							outStream.writeObject(-1);
							break;
						}
					}
					if (comandoAux.length == 4) {
						userCorrente = comandoAux[1];
						int autentica = autenticarUtilizador(userCorrente, comandoAux[2]);
						switch (autentica) {
						case 1: // user nao existe
							outStream.writeObject(1);
							break;
						case 2: // user autenticado
							outStream.writeObject(2);
							break;
						case -2: // user nao existe
							outStream.writeObject(-2);
							break;
						case -1: // user existe mas pwd errada
							outStream.writeObject(-1);
							break;
						}
					}
				} else {
					switch (comandoAux[1]) {
					case "-f":
						// como nao eh possivel manter registo do user corrente
						// eh recebido do lado do cliente que consegue manter
						// esse registo
						userCorrente = (String) inStream.readObject();
						followers(userCorrente);
						break;
					case "-r":
						userCorrente = (String) inStream.readObject();
						removeFollowers(userCorrente);
						break;
					case "-a":
						userCorrente = (String) inStream.readObject();
						fotos = comandoAux[2].split(":");
						addCopy(userCorrente, fotos);
						break;
					case "-c":
						userCorrente = (String) inStream.readObject();
						userAndFoto = splitDoComentario[2].split(" ");
						addComment(splitDoComentario[1], userAndFoto[1], userAndFoto[2]);
						break;
					case "-L":
						userCorrente = (String) inStream.readObject();
						fotos = comandoAux[2].split(":");
						addLike(userCorrente, fotos);
						break;
					case "-D":
						userCorrente = (String) inStream.readObject();
						fotos = comandoAux[2].split(":");
						dislike(userCorrente, fotos);
						break;
					case "-l":
						userCorrente = (String) inStream.readObject();
						listarFotos(comandoAux[2]);
						break;
					case "-i":
						userCorrente = (String) inStream.readObject();
						fotos = comandoAux[3].split(":");
						getInfoFotos(userCorrente, fotos);
						break;
					case "-g":
						userCorrente = (String) inStream.readObject();
						copiaPhoto(comandoAux[2]);
						break;
					default:
						System.out.println("Operação não existe.");
						break;
					}
				}
			} catch (IOException | ClassNotFoundException e) {
				System.err.println(e.getMessage());
			}
		}

		/**
		 * Metodo que adiciona um follower ao utilizador local Caso o utilizador local
		 * seja o follower, nao eh possivel ser-se follower de si proprio
		 * 
		 * @param user
		 *            utilizador local
		 * @throws ClassNotFoundException
		 * @throws IOException
		 * @requires existsUser(user) == true
		 */
		private void followers(String user) throws ClassNotFoundException, IOException {
			File followersCif = new File("REPOSITORIO_SERVIDOR/followers.txt.cif");
			File followers = new File("REPOSITORIO_SERVIDOR/followers.txt");
			String users = (String) inStream.readObject();
			String[] utilizadores = users.split(":");
			outStream.writeObject(utilizadores.length);
			// enviar num de utilizadores para o ciclo no cliente
			for (int i = 0; i < utilizadores.length; i++) {
				if (!utilizadores[i].equals(user)) {
					if (existsUser(utilizadores[i])) {
						decipherFiles(followersCif);
						if (!isMyFollower(utilizadores[i], user)) {
							addFollower(utilizadores[i], user);
							outStream.writeObject(2); // yeaaaaay adicionei
							// follower
							outStream.writeObject(utilizadores[i]);
							cipherFiles(followers);
						} else {
							outStream.writeObject(1); // ja eh follower
							outStream.writeObject(utilizadores[i]);
							cipherFiles(followers);
						}
					} else {
						outStream.writeObject(0); // follower nao existe
						outStream.writeObject(utilizadores[i]);
					}
				} else {
					outStream.writeObject(-1); // nao se pode seguir a si
					// proprio
					outStream.writeObject(utilizadores[i]);
				}
			}
		}

		/**
		 * Metodo auxiliar que adiciona um follower ao utilizador local
		 * 
		 * @param follower
		 *            follower a adicionar
		 * @param user
		 *            utilizador a adicionar um follower
		 * @throws IOException
		 */
		private void addFollower(String follower, String user) throws IOException {
			File follow = new File("REPOSITORIO_SERVIDOR/followers.txt");
			FileReader fr = new FileReader(follow);
			BufferedReader br = new BufferedReader(fr);
			File fAux = new File("REPOSITORIO_SERVIDOR/temp.txt");
			if (!fAux.exists())
				fAux.createNewFile();
			FileWriter aux = new FileWriter(fAux);
			String linha;
			String[] linhaAux;
			StringBuilder sb = new StringBuilder();
			while ((linha = br.readLine()) != null) {
				linhaAux = linha.split(":");
				if (linhaAux[0].equals(user) && !user.equals(follower)) {
					sb.append(linha);
					sb.append(follower + ":");
					aux.append(sb.toString());
					sb = new StringBuilder();
				} else {
					sb.append(linha);
					aux.append(sb.toString());
					sb = new StringBuilder();
				}
				aux.append("\n");
			}
			aux.close();
			fr.close();
			br.close();
			Files.delete(follow.toPath());
			fAux.renameTo(follow);
		}

		/**
		 * Metodo que verifica se um utilizador eh seguidor de outro
		 * 
		 * @param follower
		 *            utilizador a verificar se eh follower do user
		 * @param user
		 *            utilizador corrente
		 * @return true se follower eh seguidor de user; false caso contrario
		 * @throws IOException
		 */
		private boolean isMyFollower(String follower, String user) throws IOException {
			BufferedReader br = new BufferedReader(new FileReader("REPOSITORIO_SERVIDOR/followers.txt"));
			String linha;
			String[] linhaAux;
			while ((linha = br.readLine()) != null) {
				linhaAux = linha.split(":");
				if (user.equals(linhaAux[0])) {
					boolean found = false;
					int i = 1;
					while (!found && i < linhaAux.length) {
						if (linhaAux[i].equals(follower))
							found = true;
						i++;
					}
					br.close();
					return found;
				}
			}
			br.close();
			return false;
		}

		/**
		 * Metodo que remove um seguidor de um user
		 * 
		 * @param user
		 *            utilizador
		 * @throws ClassNotFoundException
		 * @throws IOException
		 */
		private void removeFollowers(String user) throws ClassNotFoundException, IOException {
			String users = (String) inStream.readObject();
			File followersCif = new File("REPOSITORIO_SERVIDOR/followers.txt.cif"); // followers cifrado
			File followers = new File("REPOSITORIO_SERVIDOR/followers.txt"); // followers nao cifrado
			String[] utilizadores = users.split(":");
			outStream.writeObject(utilizadores.length);
			for (int i = 0; i < utilizadores.length; i++) {
				if (existsUser(utilizadores[i])) {
					decipherFiles(followersCif); // decifrar o ficheiro dos followers para verificar se eh follower
					if (isMyFollower(utilizadores[i], userCorrente)) {
						removeFollower(utilizadores[i], userCorrente);
						outStream.writeObject(2); // removi follower
						outStream.writeObject(utilizadores[i]);
						cipherFiles(followers); // cifrar ficheiro dos followers
					} else {
						outStream.writeObject(1); // nunca foi follower
						outStream.writeObject(utilizadores[i]);
						cipherFiles(followers); // cifrar ficheiro dos followers
					}
				} else {
					outStream.writeObject(0); // user nao existe!!!!
					outStream.writeObject(utilizadores[i]);
				}
			}
		}

		/**
		 * Metodo auxiliar para a remocao de um seguidor. Efectua as alteracoes no
		 * respectivo ficheiro
		 * 
		 * @param follower
		 *            utilizador a remover
		 * @param user
		 *            utilizador corrente
		 * @throws IOException
		 */
		private void removeFollower(String follower, String user) throws IOException {
			File follow = new File("REPOSITORIO_SERVIDOR/followers.txt");
			BufferedReader br = new BufferedReader(new FileReader(follow));
			File fAux = new File("REPOSITORIO_SERVIDOR/temp.txt");
			if (!fAux.exists())
				fAux.createNewFile();
			FileWriter aux = new FileWriter(fAux, true);
			String linha;
			String[] linhaAux;
			StringBuilder sb = new StringBuilder();
			while ((linha = br.readLine()) != null) {
				linhaAux = linha.split(":");
				if (linhaAux[0].equals(user)) {
					sb.append(linhaAux[0] + ":");
					for (int i = 1; i < linhaAux.length; i++) {
						if (!linhaAux[i].equals(follower)) {
							sb.append(linhaAux[i] + ":");
						}
					}
					aux.write(sb.toString());
					sb = new StringBuilder();
					// eliminar o conteudo que esta no StringBuilder
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
			fAux.renameTo(new File("REPOSITORIO_SERVIDOR/followers.txt"));
		}

		/**
		 * Metodo que adiciona/copia fotografias no repositorio do servidor. Por cada
		 * fotografia adicionada/copiada, eh criado um ficheiro de informacoes da mesma
		 * 
		 * @param user
		 *            utilizador corrente
		 * @param fotos
		 *            fotografias a adicionar/copiar
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private void addCopy(String user, String[] fotos) throws IOException, ClassNotFoundException {
			// receber numero de fotos
			int numFotos = (int) inStream.readObject();

			String nomeFoto;
			for (int i = 0; i < numFotos; i++) {
				// receber nome da foto
				nomeFoto = (String) inStream.readObject();
				if (checkIfJPG(nomeFoto)) {
					outStream.writeObject(100);
					File ficheiro = new File(
							"REPOSITORIO_CLIENTES" + File.separator + user + File.separator + nomeFoto + ".cif");
					if (ficheiro.exists() && !ficheiro.isDirectory()) {
						outStream.writeObject(5);
						// se a foto existir entao nao eh enviada de novo
					} else {
						outStream.writeObject(6);
						ficheiro = new File(
								"REPOSITORIO_CLIENTES" + File.separator + user + File.separator + nomeFoto + ".cif");
						ficheiro.getParentFile().mkdirs();
						output = new FileOutputStream(ficheiro);
						if ((boolean) inStream.readObject() == false) {
							// se receber false eh porque nao vai enviar nada.
							// fechar o output e fazer delete do ficheiro criado
							// acima
							// (mas fica a pasta com o nome do user)
							output.close();
							Files.delete(ficheiro.toPath());
						} else {
							// recebe a assinatura
							receiveSign(ficheiro);

							// recebe chave enviada do cliente
							receiveKey(ficheiro);

							// recebe o ficheiro
							receiveFile();
							output.close();

							DateFormat dateFor = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
							Date date = new Date();
							String data = dateFor.format(date);
							String novoNome = convertToTXT(nomeFoto);
							File infoFotos = new File(
									"REPOSITORIO_CLIENTES" + File.separator + user + File.separator + novoNome);
							if (!infoFotos.exists()) {
								infoFotos.createNewFile();
								preencheFicheiro(infoFotos, data);
							}
							// cifrar o ficheiro de informacao de fotos
							cipherFiles(infoFotos);
						}
					}
				} else {
					outStream.writeObject(-1);
					outStream.writeObject(nomeFoto);
				}
			}
		}

		/**
		 * Metodo que recebe a assinatura do cliente do ficheiro file
		 * 
		 * @param file
		 *            ficheiro da assinatura
		 */
		private void receiveSign(File file) {
			FileOutputStream fos;
			try {
				String fileName = file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - 4);
				// ficheiro .sig
				fos = new FileOutputStream(fileName + ".sig");
				// recebe a assinatura e escreve-a no ficheiro .sig
				byte signature[] = (byte[]) inStream.readObject();
				fos.write(signature);
				fos.close();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Metodo que recebe a chave usada para cifrar o ficheiro file
		 * 
		 * @param ficheiro
		 *            ficheiro sobre o qual a chave foi usada para cifrar
		 */
		private void receiveKey(File ficheiro) {
			byte[] v = null;
			FileOutputStream fos;
			try {
				String fileName = ficheiro.getAbsolutePath().substring(0, ficheiro.getAbsolutePath().length() - 4);
				v = new byte[16];
				fos = new FileOutputStream(fileName + ".key");

				// tamanho do ficheiro
				long tamanho = (long) inStream.readObject();
				int n = -1;
				do {
					if (tamanho - 16 >= 0) {
						n = inStream.read(v, 0, 16);
					} else {
						n = inStream.read(v, 0, (int) tamanho);
					}
					tamanho -= 16;
					// escreve o conteudo recebido no ficheiro .key
					fos.write(v, 0, n);
					outStream.flush();
				} while (tamanho > 0);
				fos.close();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Metodo que adiciona comentario numa fotografia de um dado utilizador
		 * 
		 * @param comentario
		 *            comentario a adicionar
		 * @param user
		 *            utilizador a que pertence a fotografia
		 * @param nomeFoto
		 *            nome da fotografia a comentar
		 * @throws IOException
		 */
		private void addComment(String comentario, String user, String nomeFoto) throws IOException {
			String nomeFotoTXT = convertToTXT(nomeFoto);
			File infoFoto = new File("REPOSITORIO_CLIENTES/" + user + File.separator + nomeFotoTXT + ".cif");
			File followersCif = new File("REPOSITORIO_SERVIDOR/followers.txt.cif");
			File followers = new File("REPOSITORIO_SERVIDOR/followers.txt");
			if (existsUser(user)) { // se o user existe
				decipherFiles(followersCif);
				if (verifyServerSignature(followers) == 0) {
					if (isMyFollower(userCorrente, user)) {
						// se o follower eh follower do user atual
						if (existsPhoto(user, nomeFoto)) {
							decipherFiles(infoFoto);
							registaComment(comentario, user, nomeFotoTXT);
							String pathName = infoFoto.getAbsolutePath().substring(0,
									infoFoto.getAbsolutePath().length() - 4);
							cipherFiles(new File(pathName));
							outStream.writeObject(2); // adicionou comentario
							cipherFiles(followers);
						} else {
							cipherFiles(followersCif);
							outStream.writeObject(1); // a foto nao existe
						}
					} else if (user.equals(userCorrente)) {
						// eu a comentar as minhas fotos
						if (existsPhoto(user, nomeFoto)) {
							decipherFiles(infoFoto);
							// se o user tem a foto nomeFoto
							registaComment(comentario, user, nomeFotoTXT);
							String pathName = infoFoto.getAbsolutePath().substring(0,
									infoFoto.getAbsolutePath().length() - 4);
							cipherFiles(new File(pathName));
							outStream.writeObject(2); // adicionou comentario
							cipherFiles(followers);
						} else {
							cipherFiles(followersCif);
							outStream.writeObject(1); // a foto nao existe
						}
					} else {
						outStream.writeObject(0);
						// o user nao eh follower logo nao pode comentar
						outStream.writeObject(user);
						cipherFiles(followersCif);
					}
				} else {
					cipherFiles(followers);
					System.err.println("Assinatura incorreta.");
					terminaServidor();
				}
			} else { // user nao existe
				outStream.writeObject(-1);
				outStream.writeObject(user);
			}
		}

		/**
		 * Metodo auxiliar para a adicao de comentarios. Efectua as alteracoes no
		 * respectivo ficheiro.
		 * 
		 * @param comentario
		 *            comentario a adicionar
		 * @param user
		 *            utilizador a quem pertence a fotografia
		 * @param nomeFoto
		 *            nome da fotografia
		 * @throws IOException
		 */
		private void registaComment(String comentario, String user, String nomeFoto) throws IOException {
			BufferedReader br = new BufferedReader(
					new FileReader("REPOSITORIO_CLIENTES" + File.separator + user + File.separator + nomeFoto));
			File comment = new File("REPOSITORIO_CLIENTES" + File.separator + user + File.separator + nomeFoto);
			File fAux = new File("REPOSITORIO_SERVIDOR/temp.txt");
			if (!fAux.exists())
				fAux.createNewFile();
			FileWriter aux = new FileWriter(fAux);
			String linha;
			String[] linhaAux;
			StringBuilder sb = new StringBuilder();
			while ((linha = br.readLine()) != null) {
				linhaAux = linha.split(":");
				if (linhaAux[0].equals("Comentarios")) {
					sb.append(linha);
					sb.append(userCorrente + ":" + comentario + ",");
					aux.append(sb.toString());
					sb = new StringBuilder();
				} else {
					sb.append(linha);
					aux.append(sb.toString());
					sb = new StringBuilder();
				}
				aux.append("\n");
			}
			br.close();
			aux.close();
			Files.delete(comment.toPath());
			fAux.renameTo(new File("REPOSITORIO_CLIENTES" + File.separator + user + File.separator + nomeFoto));
		}

		/**
		 * Metodo que adiciona um like a uma fotografia de um dado utilizador
		 * 
		 * @param userLocal
		 *            utilizador a quem pertence a fotografia
		 * @param fotos
		 *            fotografia a adicionar um like
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private void addLike(String userLocal, String[] fotos) throws IOException, ClassNotFoundException {
			String follower = (String) inStream.readObject();
			String nomeFoto = (String) inStream.readObject();
			String nomeFotoTXT = convertToTXT(nomeFoto);
			File followersCif = new File("REPOSITORIO_SERVIDOR/followers.txt.cif");
			File followers = new File("REPOSITORIO_SERVIDOR/followers.txt");
			File infoFoto = new File("REPOSITORIO_CLIENTES/" + follower + File.separator + nomeFotoTXT + ".cif");

			if (existsUser(follower)) { // existe no sistema
				decipherFiles(followersCif);
				if (isMyFollower(userLocal, follower)) {
					// se o follower eh follower do user atual
					if (existsPhoto(follower, nomeFoto)) {
						decipherFiles(infoFoto);
						registaLike(follower, userLocal, nomeFotoTXT);
						outStream.writeObject(2); // adicionei like
						String pathName = infoFoto.getAbsolutePath().substring(0,
								infoFoto.getAbsolutePath().length() - 4);
						cipherFiles(new File(pathName));
						cipherFiles(followers);
					} else {
						outStream.writeObject(1); // a foto nao existe
						cipherFiles(followers);
					}
				} else {
					outStream.writeObject(0); // o user nao eh follower, logo
					// nao pode meter like
					outStream.writeObject(follower);
					cipherFiles(followers);
				}
			} else {
				outStream.writeObject(-1);
				outStream.writeObject(follower);
			}
		}

		/**
		 * Metodo que verifica se uma fotografia existe
		 * 
		 * @param follower
		 *            utilizador cuja fotografia se quer adicionar like
		 * @param nomeFoto
		 *            nome da fotografia
		 * @return true se existe; false caso contrario
		 */
		private boolean existsPhoto(String follower, String nomeFoto) {
			File teste = new File("REPOSITORIO_CLIENTES" + File.separator + follower + File.separator);
			File[] listaFicheiros = teste.listFiles();
			String newName = nomeFoto.substring(0, nomeFoto.length() - 4);
			for (int i = 0; i < listaFicheiros.length; i++) {
				if (listaFicheiros[i].getName().startsWith(newName)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Metodo auxiliar para a adicao de um like. Efectua as alteracoes no ficheiro
		 * respectivo.
		 * 
		 * @param follower
		 *            utilizador cuja fotografia se quer adicionar like
		 * @param userLocal
		 *            utilizador que pretende adicionar um like
		 * @param nomeFoto
		 *            nome da fotografia
		 * @throws IOException
		 */
		private void registaLike(String follower, String userLocal, String nomeFoto) throws IOException {
			BufferedReader br = new BufferedReader(
					new FileReader("REPOSITORIO_CLIENTES" + File.separator + follower + File.separator + nomeFoto));
			File follow = new File("REPOSITORIO_CLIENTES" + File.separator + follower + File.separator + nomeFoto);
			File fAux = new File("REPOSITORIO_SERVIDOR/temp.txt");
			if (!fAux.exists())
				fAux.createNewFile();
			FileWriter aux = new FileWriter(fAux, true);
			String linha;
			String[] linhaAux;
			StringBuilder sb = new StringBuilder();
			while ((linha = br.readLine()) != null) {
				linhaAux = linha.split(":");
				if (linhaAux[0].equals("Likes")) {
					sb.append(linha);
					sb.append(userLocal + ":");
					aux.append(sb.toString());
					sb = new StringBuilder();
				} else {
					sb.append(linha);
					aux.append(sb.toString());
					sb = new StringBuilder();
				}
				aux.append("\n");
			}
			br.close();
			aux.close();
			Files.delete(follow.toPath());
			fAux.renameTo(new File("REPOSITORIO_CLIENTES" + File.separator + follower + File.separator + nomeFoto));
		}

		/**
		 * Metodo que obtem as informacoes de uma fotografia
		 * 
		 * @param userLocal
		 *            utilizador que tem a fotografia
		 * @param fotos
		 *            fotografia a obter informacoes
		 * @throws ClassNotFoundException
		 * @throws IOException
		 */
		private void getInfoFotos(String userLocal, String[] fotos) throws ClassNotFoundException, IOException {
			String follower = (String) inStream.readObject();
			String nomeFoto = (String) inStream.readObject();
			String nomeFotoTXT = convertToTXT(nomeFoto);
			File followersCif = new File("REPOSITORIO_SERVIDOR/followers.txt.cif");
			File followers = new File("REPOSITORIO_SERVIDOR/followers.txt");
			File infoFoto = new File("REPOSITORIO_CLIENTES/" + follower + File.separator + nomeFotoTXT + ".cif");
			if (existsUser(follower)) {
				outStream.writeObject(4);
				decipherFiles(followersCif);
				if (isMyFollower(follower, userLocal)) {
					// userLocal eh follower do follower
					outStream.writeObject(5);
					if (existsPhoto(follower, nomeFoto)) {
						decipherFiles(infoFoto);
						outStream.writeObject(1);
						outStream.writeObject(nomeFoto);
						int n_likes = getLikesFromPhoto(nomeFoto, follower);
						outStream.writeObject(n_likes);
						int n_dislikes = getDislikesFromPhoto(nomeFoto, follower);
						outStream.writeObject(n_dislikes);
						String comentarios = getCommentsFromPhoto(nomeFoto, follower);
						outStream.writeObject(comentarios);
						String fileName = infoFoto.getAbsolutePath().substring(0,
								infoFoto.getAbsolutePath().length() - 4);
						cipherFiles(new File(fileName));
						cipherFiles(followers);
					} else {
						outStream.writeObject(-1); // a foto nao existe
						outStream.writeObject(nomeFoto);
						cipherFiles(followers);
					}
				} else if (follower.equals(userLocal)) {
					// estamos a fazer -i de nos proprios
					outStream.writeObject(5);
					if (existsPhoto(follower, nomeFoto)) {
						decipherFiles(infoFoto);
						outStream.writeObject(1);
						outStream.writeObject(nomeFoto);
						int n_likes = getLikesFromPhoto(nomeFoto, follower);
						outStream.writeObject(n_likes);
						int n_dislikes = getDislikesFromPhoto(nomeFoto, follower);
						outStream.writeObject(n_dislikes);
						String comentarios = getCommentsFromPhoto(nomeFoto, follower);
						outStream.writeObject(comentarios);
						String fileName = infoFoto.getAbsolutePath().substring(0,
								infoFoto.getAbsolutePath().length() - 4);
						cipherFiles(new File(fileName));
						cipherFiles(followers);
					} else {
						outStream.writeObject(-1); // a foto nao existe
						outStream.writeObject(nomeFoto);
						cipherFiles(followers);
					}
				} else {
					// o userLocal nao eh follower de follower
					outStream.writeObject(-5);
					outStream.writeObject(follower);
					cipherFiles(followers);
				}
			} else {
				// nao existe o user que tamos a tentar fazer -i
				outStream.writeObject(-4);
				outStream.writeObject(follower);
			}
		}

		/**
		 * Metodo que obtem os comentarios de uma determinada fotografia
		 * 
		 * @param nomeFoto
		 *            nome da fotografia
		 * @param follower
		 *            utilizador a quem pertence a fotografia
		 * @return comentarios. Se nao existirem, retorna String vazia.
		 * @throws IOException
		 */
		private String getCommentsFromPhoto(String nomeFoto, String follower) throws IOException {

			String nomeFotoTXT = convertToTXT(nomeFoto);
			String linha;
			String retorno = null;
			String[] linhaAux = null;
			BufferedReader br = new BufferedReader(
					new FileReader("REPOSITORIO_CLIENTES" + File.separator + follower + File.separator + nomeFotoTXT));
			while ((linha = br.readLine()) != null) {
				linhaAux = linha.split(":");
				if (linhaAux[0].equals("Comentarios")) {
					retorno = linha.substring(12, linha.length() - 1);
				}
			}
			br.close();
			return retorno;
		}

		/**
		 * Metodo que obtem o numero de dislikes de uma determinada fotografia
		 * 
		 * @param nomeFoto
		 *            nome da fotografia
		 * @param follower
		 *            utilizador a quem pertence a fotografia
		 * @return numero de dislikes de uma fotografia
		 * @throws IOException
		 */
		private int getDislikesFromPhoto(String nomeFoto, String follower) throws IOException {

			String nomeFotoTXT = convertToTXT(nomeFoto);
			String linha;
			String[] linhaAux = null;
			BufferedReader br = new BufferedReader(
					new FileReader("REPOSITORIO_CLIENTES" + File.separator + follower + File.separator + nomeFotoTXT));
			int contador = 0;
			while ((linha = br.readLine()) != null) {
				linhaAux = linha.split(":");
				if (linhaAux[0].equals("Dislikes")) {
					contador += linhaAux.length - 1;
				}
			}
			br.close();
			return contador;
		}

		/**
		 * Metodo que obtem o numero de likes de uma determinada fotografia
		 * 
		 * @param nomeFoto
		 *            nome da fotografia
		 * @param follower
		 *            utilizador a quem pertence a fotografia
		 * @return numero de likes de uma fotografia
		 * @throws IOException
		 */
		private int getLikesFromPhoto(String nomeFoto, String follower) throws IOException {

			String nomeFotoTXT = convertToTXT(nomeFoto);
			BufferedReader br = new BufferedReader(
					new FileReader("REPOSITORIO_CLIENTES" + File.separator + follower + File.separator + nomeFotoTXT));
			String linha;
			int contador = 0;
			String[] stuff = null;
			while ((linha = br.readLine()) != null) {
				stuff = linha.split(":");
				if (stuff[0].equals("Likes")) {
					contador += stuff.length - 1;
				}
			}
			br.close();
			return contador;
		}

		/**
		 * Metodo que adiciona um dislike a uma fotografia de um dado utilizador
		 * 
		 * @param userLocal
		 *            utilizador a quem pertence a fotografia
		 * @param fotos
		 *            fotografia a adicionar um dislike
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private void dislike(String userLocal, String[] fotos) throws ClassNotFoundException, IOException {
			String follower = (String) inStream.readObject();
			String nomeFoto = (String) inStream.readObject();
			String nomeFotoTXT = convertToTXT(nomeFoto);
			File followersCif = new File("REPOSITORIO_SERVIDOR/followers.txt.cif");
			File followers = new File("REPOSITORIO_SERVIDOR/followers.txt");
			File infoFoto = new File("REPOSITORIO_CLIENTES/" + follower + File.separator + nomeFotoTXT + ".cif");

			if (existsUser(follower)) { // existe no sistema
				decipherFiles(followersCif);
				if (isMyFollower(userLocal, follower)) {
					// se o follower eh follower do user atual
					if (existsPhoto(follower, nomeFoto)) {
						decipherFiles(infoFoto);
						registaDislike(follower, userLocal, nomeFotoTXT);
						outStream.writeObject(2); // adicionei dislike
						String pathName = infoFoto.getAbsolutePath().substring(0,
								infoFoto.getAbsolutePath().length() - 4);
						cipherFiles(new File(pathName));
						cipherFiles(followers);
					} else {
						outStream.writeObject(1); // a foto nao existe
						cipherFiles(followers);
					}
				} else {
					outStream.writeObject(0); // o user nao eh follower, logo
					// nao pode meter dislike
					outStream.writeObject(follower);
					cipherFiles(followers);
				}
			} else
				outStream.writeObject(-1);
			outStream.writeObject(follower);
		}

		/**
		 * Metodo auxiliar para a adicao de um dislike. Efectua as alteracoes no
		 * ficheiro respectivo.
		 * 
		 * @param follower
		 *            utilizador cuja fotografia se quer adicionar dislike
		 * @param userLocal
		 *            utilizador que pretende adicionar um dislike
		 * @param nomeFoto
		 *            nome da fotografia
		 * @throws IOException
		 */
		private void registaDislike(String follower, String userLocal, String nomeFoto) throws IOException {
			BufferedReader br = new BufferedReader(
					new FileReader("REPOSITORIO_CLIENTES" + File.separator + follower + File.separator + nomeFoto));
			File follow = new File("REPOSITORIO_CLIENTES" + File.separator + follower + File.separator + nomeFoto);
			File fAux = new File("REPOSITORIO_SERVIDOR/temp.txt");
			if (!fAux.exists())
				fAux.createNewFile();
			FileWriter aux = new FileWriter(fAux, true);
			String linha;
			String[] linhaAux;
			StringBuilder sb = new StringBuilder();
			while ((linha = br.readLine()) != null) {
				linhaAux = linha.split(":");
				if (linhaAux[0].equals("Dislikes")) {
					sb.append(linha);
					sb.append(userLocal + ":");
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
			fAux.renameTo(new File("REPOSITORIO_CLIENTES" + File.separator + follower + File.separator + nomeFoto));

		}

		/**
		 * Metodo que verifica e um utilizador existe no ficheiro dos utilizadores
		 * 
		 * @param user
		 *            utilizador a verificar
		 * @return true se existe; false caso contrario
		 * @throws IOException
		 */
		private boolean existsUser(String user) throws IOException {
			BufferedReader br = new BufferedReader(new FileReader("REPOSITORIO_SERVIDOR/users.txt"));
			String linha;
			String[] linhaAux;
			while ((linha = br.readLine()) != null) {
				linhaAux = linha.split(":");
				if (user.equals(linhaAux[0])) {
					br.close();
					return true;
				}
			}
			br.close();
			return false;
		}

		/**
		 * Metodo que copia as fotografias de um utilizador para um repositorio local
		 * 
		 * @param user
		 *            utilizador de quem se quer copiar as fotografias
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private void copiaPhoto(String user) throws IOException, ClassNotFoundException {
			File followersCif = new File("REPOSITORIO_SERVIDOR/followers.txt.cif"); // ficheiro followers cifrado
			File followers = new File("REPOSITORIO_SERVIDOR/followers.txt"); // ficheiro followers normal
			File auxiliar;
			ArrayList<File> userFilesKey; // lista para os ficheiros .key
			ArrayList<File> userFilesCif; // lista para os ficheiros .cif
			File[] listaAuxiliar; // lista que contem todos os ficheiros
			FileInputStream input = null;
			if (existsUser(user)) { // se o user existe
				outStream.writeObject(1);
				if (user.equals(userCorrente)) {
					outStream.writeObject(100);// se for o user corrente
					auxiliar = new File("REPOSITORIO_CLIENTES" + File.separator + userCorrente + File.separator);
					userFilesKey = new ArrayList<>();
					userFilesCif = new ArrayList<>();
					listaAuxiliar = auxiliar.listFiles();
					if (listaAuxiliar == null) {
						// se nao tem ficheiros
						outStream.writeObject(-1);
						outStream.writeObject("vazio");
					} else {
						for (int i = 0; i < listaAuxiliar.length; i++) {
							// separar os ficheiros .key e os .cif
							// e enviar os .key primeiro que os .cif
							if (listaAuxiliar[i].getName().contains(".key")) {
								userFilesKey.add(listaAuxiliar[i]);
							}
							if (listaAuxiliar[i].getName().contains(".cif")) {
								userFilesCif.add(listaAuxiliar[i]);
							}
						}

						// envia o numero de ficheiros a enviar (numero de .key == numero de .cif)
						outStream.writeObject(userFilesKey.size());

						// ciclo para enviar os .key
						for (File file : userFilesKey) {
							input = new FileInputStream("REPOSITORIO_CLIENTES" + File.separator + userCorrente
									+ File.separator + file.getName());
							outStream.writeObject(file.getName());
							sendFile(input);
							input.close();
						}

						// ciclo para enviar os .cif
						for (File file : userFilesCif) {
							input = new FileInputStream("REPOSITORIO_CLIENTES" + File.separator + userCorrente
									+ File.separator + file.getName());
							outStream.writeObject(file.getName());
							sendFile(input);
							input.close();
						}
					}
				} else { // nao eh o user corrente, verificar se eh follower
					outStream.writeObject(101);
					decipherFiles(followersCif);
					if (isMyFollower(userCorrente, user)) { // se eh meu follower
						outStream.writeObject(200);
						// se for o user corrente
						auxiliar = new File("REPOSITORIO_CLIENTES" + File.separator + user + File.separator);
						userFilesKey = new ArrayList<>();
						userFilesCif = new ArrayList<>();
						listaAuxiliar = auxiliar.listFiles();
						if (listaAuxiliar == null) { // se nao tem ficheiros
							outStream.writeObject(-1);
							outStream.writeObject("vazio");
							cipherFiles(followers);
						} else {
							for (int i = 0; i < listaAuxiliar.length; i++) {
								// separar os ficheiros .key e os .cif
								// e enviar os .key primeiro que os .cif
								if (listaAuxiliar[i].getName().contains(".key")) {
									userFilesKey.add(listaAuxiliar[i]);
								}
								if (listaAuxiliar[i].getName().contains(".cif")) {
									userFilesCif.add(listaAuxiliar[i]);
								}
							}

							// envia o numero de ficheiros a enviar (numero de .key == numero de .cif)
							outStream.writeObject(userFilesKey.size());

							// ciclo para enviar os .key
							for (File file : userFilesKey) {
								input = new FileInputStream("REPOSITORIO_CLIENTES" + File.separator + user
										+ File.separator + file.getName());
								outStream.writeObject(file.getName());
								sendFile(input);
								input.close();
							}

							// ciclo para enviar os .cif
							for (File file : userFilesCif) {
								input = new FileInputStream("REPOSITORIO_CLIENTES" + File.separator + user
										+ File.separator + file.getName());
								outStream.writeObject(file.getName());
								sendFile(input);
								input.close();
							}
							cipherFiles(followers);
						}
					} else {
						outStream.writeObject(201);
						outStream.writeObject(user);
						cipherFiles(followers);
					}
				}
			} else {
				outStream.writeObject(-1);
				outStream.writeObject(user);
			}
		}

		/**
		 * Metodo que lista as fotografias de um dado utilizador com as respectivas
		 * datas de publicacao
		 * 
		 * @param user
		 *            utilizador a quem pertencem as fotografias
		 * @throws IOException
		 */
		private void listarFotos(String user) throws IOException {
			ArrayList<File> userFiles;
			File auxiliar;
			File[] listaAuxiliar;
			String data;
			File followersCif = new File("REPOSITORIO_SERVIDOR/followers.txt.cif");
			File followers = new File("REPOSITORIO_SERVIDOR/followers.txt");
			if (existsUser(user)) {
				if (user.equals(userCorrente)) { // se for o user corrente
					auxiliar = new File("REPOSITORIO_CLIENTES" + File.separator + userCorrente + File.separator);
					userFiles = new ArrayList<>();
					// lista com todos os ficheiros do utilizador
					// 1 - imagens cifradas (.jpg.cif)
					// 2 - chave aleatoria usada para cifrar a imagem (.key)
					// 3 - assinatura do cliente (.sig)
					// 4 - ficheiro de info da foto cifrado (.txt.cif)
					// 5 - chave aleatoria usada para cifrar o ficheiro (.txt.key)
					listaAuxiliar = auxiliar.listFiles();
					if (listaAuxiliar == null) {
						// eu nao tenho ficheiros no servidor
						outStream.writeObject(-1);
						outStream.writeObject("vazio");
						outStream.writeObject(user);
					} else {
						for (int i = 0; i < listaAuxiliar.length; i++) {
							if (listaAuxiliar[i].getName().contains(".txt.cif")) {
								userFiles.add(listaAuxiliar[i]);
							}
							// .txt.cif sao os cifrados dos ficheiros de info
						}
						outStream.writeObject(userFiles.size());
						outStream.writeObject("info");
						for (File file : userFiles) {
							outStream.writeObject(file.getName().substring(0, file.getName().length() - 4));
							decipherFiles(file);
							File newFile = new File("REPOSITORIO_CLIENTES" + File.separator + userCorrente
									+ File.separator + file.getName().substring(0, file.getName().length() - 4));
							data = getDataFromFile(newFile);
							outStream.writeObject(data);
							cipherFiles(newFile);
						}
					}

				} else {
					decipherFiles(followersCif); // decifrar ficheiro dos followers
					if (isMyFollower(userCorrente, user)) {
						auxiliar = new File("REPOSITORIO_CLIENTES" + File.separator + user + File.separator);
						userFiles = new ArrayList<>();
						// lista com todos os ficheiros do utilizador
						// 1 - imagens cifradas (.jpg.cif)
						// 2 - chave aleatoria usada para cifrar a imagem (.key)
						// 3 - assinatura do cliente (.sig)
						// 4 - ficheiro de info da foto cifrado (.txt.cif)
						// 5 - chave aleatoria usada para cifrar o ficheiro (.txt.key)
						listaAuxiliar = auxiliar.listFiles();
						if (listaAuxiliar == null) {
							// o user que quero fazer -l nao tem ficheiros no
							// servidor
							outStream.writeObject(-1);
							outStream.writeObject("vazio");
							outStream.writeObject(user);
						} else {
							for (int i = 0; i < listaAuxiliar.length; i++) {
								if (listaAuxiliar[i].getName().contains(".txt.cif")) {
									userFiles.add(listaAuxiliar[i]);
								}
							}
							outStream.writeObject(userFiles.size());
							outStream.writeObject("info");
							for (File file : userFiles) {
								outStream.writeObject(file.getName().substring(0, file.getName().length() - 4));
								decipherFiles(file);
								File newFile = new File("REPOSITORIO_CLIENTES" + File.separator + user + File.separator
										+ file.getName().substring(0, file.getName().length() - 4));
								data = getDataFromFile(newFile);
								outStream.writeObject(data);
								cipherFiles(newFile);
							}
						}
						cipherFiles(followers);
					} else {
						outStream.writeObject(-1);
						outStream.writeObject("nao eh follower");
						cipherFiles(followers);
					}
				}
			} else {
				outStream.writeObject(-1);
				outStream.writeObject("inexistente"); // user nao existe
			}
		}

		/**
		 * Metodo que obtem a data de um ficheiro
		 * 
		 * @param file
		 *            ficheiro de que se quer obter a data
		 * @return a data de um ficheiro
		 * @throws IOException
		 */
		private String getDataFromFile(File file) throws IOException {
			String data = null;
			BufferedReader br = new BufferedReader(new FileReader(file));
			String linha;
			String[] linhaAux;
			while ((linha = br.readLine()) != null) {
				linhaAux = linha.split(" ");
				if (linhaAux[0].startsWith("Data:")) {
					data = linhaAux[0].substring(5, linhaAux[0].length());
				}
			}
			br.close();
			return data;
		}

		/**
		 * Metodo que inicializa as informacoes de uma fotografia
		 * 
		 * @param infoFotos
		 *            ficheiro de ifnromacoes da fotografia
		 * @param data
		 *            data de criacao do ficheiro
		 * @throws IOException
		 */
		private void preencheFicheiro(File infoFotos, String data) throws IOException {
			FileWriter fw = new FileWriter(infoFotos);
			fw.write("Data:" + data + "\n");
			fw.write("Likes:" + "\n");
			fw.write("Dislikes:" + "\n");
			fw.write("Comentarios: " + "\n");
			fw.close();
		}

		/**
		 * Metodo que autentica um utilizador
		 * 
		 * @param user
		 *            utilizador a autenticar
		 * @param passwd
		 *            password do utilizador
		 * @return 1 se user eh o primeiro utilizador a autenticar-se; 2 se user eh
		 *         autenticado com sucesso; 0 caso contrario; 3 se user se autentica
		 *         pela primeira vez
		 * @throws IOException
		 */
		private int autenticarUtilizador(String user, String passwd) {
			int result = 0;
			File passwords = new File("REPOSITORIO_SERVIDOR/users.txt");
			try {
				if (verificaMac("REPOSITORIO_SERVIDOR/mac.txt", "REPOSITORIO_SERVIDOR/users.txt") == 0) {
					result = autenticarUtilizadorAux(user, passwd, result, passwords);
				} else {
					terminaServidor();
				}
			} catch (NoSuchAlgorithmException | IOException e) {
				System.err.println(e.getMessage());
			}
			return result;
		}

		/**
		 * Metodo auxiliar que autentica um utilizador. Depois de o autenticar regista-o
		 * no ficheiro dos followers
		 * 
		 * @param user
		 *            utilizador a autenticar
		 * @param passwd
		 *            password do utilizador
		 * @param result
		 *            valor a retornar
		 * @param passwords
		 *            password do utilizador
		 * @return1 se autenticado, -2 se nao autenticado ou -1 se o utilizador nao
		 *          existe
		 * @throws NoSuchAlgorithmException
		 * @throws IOException
		 */
		private int autenticarUtilizadorAux(String user, String passwd, int result, File passwords)
				throws NoSuchAlgorithmException, IOException {
			BufferedReader br = new BufferedReader(new FileReader(passwords));
			String linha;
			String[] linhaAux;
			while ((linha = br.readLine()) != null) {
				linhaAux = linha.split(":");
				if (linhaAux[0].equals(user)) {
					MessageDigest md = MessageDigest.getInstance("SHA-256");
					String pwd = linhaAux[2]; // password no ficheiro

					// calcular hash com a password fornecida
					byte buf[] = passwd.getBytes();
					byte hash[] = md.digest(buf);
					String h = DatatypeConverter.printBase64Binary(hash);

					if (h.equals(pwd)) { // passwords iguais, autenticado
						result = 2;
						break;
					} else { // passwords diferentes
						result = -2;
						break;
					}
				} else { // user nao existe no ficheiro
					result = -1;
				}
			}
			registarUtilizadorFollowers();
			br.close();
			return result;
		}

		/**
		 * Metodo auxiliar para registar um seguidor. Efectua as alteracoes no ficheiro.
		 * Se o ficheiro dos followers nao existir entao cria-o, regista o utilizador,
		 * cria a sua assinatura e cifra-o. Se existir, entao decifra-o, verifica a
		 * assinatura do servidor e regista o utilizador. Depois, cria novamente a
		 * assinatura e cifra o ficheiro.
		 * 
		 * @throws IOException
		 */
		private void registarUtilizadorFollowers() throws IOException {
			File followersCif = new File("REPOSITORIO_SERVIDOR/followers.txt.cif");
			File follow = new File("REPOSITORIO_SERVIDOR/followers.txt");
			if (!followersCif.exists()) {
				// criacao do followers.txt
				follow.createNewFile();
				FileWriter fw2 = new FileWriter(follow, true);
				if (!verificaUtilizadorFicheiroFollowers()) {
					// quando alteramos alguma coisa temos de criar a assinatura
					// e cifrar
					fw2.append(userCorrente + ":" + "\n");
					fw2.close();
				}
				fw2.close();
				createSignature(follow);
				cipherFiles(follow);
			} else {
				decipherFiles(followersCif);
				if (verifyServerSignature(follow) == 0) {
					FileWriter fw3 = new FileWriter(follow, true);
					if (!verificaUtilizadorFicheiroFollowers()) {
						// quando alteramos alguma coisa temos de criar a assinatura
						// e cifrar
						fw3.append(userCorrente + ":" + "\n");
						fw3.close();
					}
					fw3.close();
					createSignature(follow);
					cipherFiles(follow);
				} else {
					cipherFiles(follow);
					System.err.println("A assinatura falhou.");
					terminaServidor();
				}
			}
		}

		/**
		 * Metodo que verifica se o utilizador existe no ficheiro dos followers
		 * 
		 * @return true se existe; falso caso contrario
		 * @throws IOException
		 */
		private static boolean verificaUtilizadorFicheiroFollowers() throws IOException {
			File followers = new File("REPOSITORIO_SERVIDOR/followers.txt");
			BufferedReader br = new BufferedReader(new FileReader(followers));
			String linha;
			String[] linhaAux;
			while ((linha = br.readLine()) != null) {
				linhaAux = linha.split(":");
				if (linhaAux[0].equals(userCorrente)) {
					br.close();
					return true;
				}
			}
			br.close();
			return false;
		}

		/**
		 * Metodo que envia um ficheiro para o cliente
		 * 
		 * @param input
		 *            stream de abertura do ficheiro
		 * @throws IOException
		 */
		private void sendFile(FileInputStream input) throws IOException {
			long tamanho = input.available();
			// int verifica = 0;
			outStream.writeObject(tamanho);// enviar tamanho
			byte[] buffer = new byte[1024];
			int n = -1;
			do {
				if (tamanho - 1024 >= 0) {
					// Se existirem 1024 bytes para receber
					n = input.read(buffer, 0, 1024);
				} else {
					n = input.read(buffer, 0, (int) tamanho);
				}
				tamanho -= 1024;
				outStream.write(buffer, 0, n);
				outStream.flush();
			} while (tamanho > 0);
		}

		/**
		 * Metodo que recebe um ficheiro do cliente
		 * 
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private void receiveFile() throws IOException, ClassNotFoundException {
			// receber tamanho da foto
			long tamanho = (long) inStream.readObject();
			byte[] buffer = new byte[1024];
			int n = -1;
			output.flush();
			do {
				if (tamanho - 1024 >= 0) {
					// Se existirem 1024 bytes para receber
					n = inStream.read(buffer, 0, 1024);
				} else {
					n = inStream.read(buffer, 0, (int) tamanho);
				}
				tamanho -= 1024;
				outStream.flush();
				output.write(buffer, 0, n);
			} while (tamanho > 0);
		}

		/**
		 * Metodo que converte o nome de uma fotografia em formato .jpg para .txt
		 * 
		 * @param nomeFoto
		 *            nome da fotografia
		 * @return nome da fotografia.txt
		 */
		private String convertToTXT(String nomeFoto) {
			String[] novoNomeSplit = nomeFoto.split("\\.");
			String nomeFotoTXT = novoNomeSplit[0].concat(".txt");
			return nomeFotoTXT;
		}

		/**
		 * MEtodo que verifica se o formato de uma fotografia eh valido (termina em
		 * .jpg)
		 * 
		 * @param nomeFoto
		 *            nome da fotografia
		 * @return true se eh valido; false caso contrario
		 */
		public boolean checkIfJPG(String nomeFoto) {
			return nomeFoto.endsWith(".jpg");
		}
	}

	/**
	 * Metodo que cifra um ficheiro file
	 * 
	 * @param file
	 *            ficheiro a ser cifrado
	 */
	public static void cipherFiles(File file) {
		String alias = "server";
		String password = "server";
		byte[] chaveCifrada = null;
		FileInputStream fis;
		FileOutputStream fos;
		CipherOutputStream cos;
		try {
			// gerar uma chave aleatoria para utilizar com o AES
			KeyGenerator kg = KeyGenerator.getInstance("AES");
			kg.init(128);
			SecretKey key = kg.generateKey();

			// instancia para cifrar e modo
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, key);

			// stream para ler do ficheiro
			fis = new FileInputStream(file);
			// stream para escrever o ficheiro cifrado
			fos = new FileOutputStream(file + ".cif");
			cos = new CipherOutputStream(fos, c);

			// escreve o ficheiro cifrado
			int tamanho = 256;
			byte[] buffer = new byte[tamanho];
			int lidos = 0;
			while ((lidos = fis.read(buffer)) != -1) {
				cos.write(buffer, 0, lidos);
			}
			cos.close();
			fos.close();
			fis.close();

			// inputStream para a keystore
			FileInputStream fileInputStream = new FileInputStream(
					"C:/Users/fc44870/eclipse-workspace/SC/" + "serverKeystore.keystore");

			// instancia da keystore
			KeyStore kStore = KeyStore.getInstance("JKS");
			kStore.load(fileInputStream, password.toCharArray());
			// certificado com a chave privada
			Certificate cert = kStore.getCertificate(alias);
			Cipher c1 = Cipher.getInstance("RSA");
			c1.init(Cipher.WRAP_MODE, cert);
			// wrap para ficar com a chave cifrada
			chaveCifrada = c1.wrap(key);

			// guardar a chave cifrada no ficheiro nome.key
			FileOutputStream kos = new FileOutputStream(file + ".key");
			ObjectOutputStream oos = new ObjectOutputStream(kos);
			oos.write(chaveCifrada);
			fileInputStream.close();
			oos.close();

			file.delete();
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IOException
				| CertificateException | KeyStoreException | IllegalBlockSizeException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Metodo que decifra o ficheiro file
	 * 
	 * @param file
	 *            ficheiro a decifrar
	 */
	public static void decipherFiles(File file) {
		String alias = "server";
		String password = "server";
		FileInputStream fis2;
		FileOutputStream fos;
		CipherOutputStream cos;
		try {
			// ler a chave do ficheiro
			String fileName = file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - 4);
			FileInputStream fis = new FileInputStream(fileName + ".key");
			ObjectInputStream ois = new ObjectInputStream(fis);
			byte[] buffer = new byte[256];
			ois.read(buffer);
			ois.close();

			// inputStream para a keystore
			FileInputStream fileInputStream = new FileInputStream(
					"C:/Users/fc44870/eclipse-workspace/SC/" + "serverKeyStore.keystore");
			KeyStore kStore = KeyStore.getInstance("JKS");
			kStore.load(fileInputStream, password.toCharArray());

			// tipo de cifra da chave, chave privada, unwrap e obtem-se a chave decifrada
			Cipher cifraUser = Cipher.getInstance("RSA");
			PrivateKey pk = (PrivateKey) kStore.getKey(alias, password.toCharArray());
			cifraUser.init(Cipher.UNWRAP_MODE, pk);
			Key k = cifraUser.unwrap(buffer, "AES", Cipher.SECRET_KEY);
			Cipher c1 = Cipher.getInstance("AES");
			c1.init(Cipher.DECRYPT_MODE, k);

			// ler do ficheiro cifrado e decifrar
			fis2 = new FileInputStream(file);
			fileName = file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - 4);
			fos = new FileOutputStream(fileName);
			cos = new CipherOutputStream(fos, c1);
			int tamanho = 256;
			byte[] buffer2 = new byte[tamanho];
			int lidos = 0;
			while ((lidos = fis2.read(buffer2)) != -1) {
				cos.write(buffer2, 0, lidos);
			}

			cos.close();
			fos.close();
			fis2.close();

			file.delete();
		} catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
				| KeyStoreException | UnrecoverableKeyException | CertificateException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Metodo que termina o PhotoShareServer
	 * 
	 * @throws IOException
	 */
	public static void terminaServidor() throws IOException {
		System.err.println("Ocorreu um erro no MAC!");
		sSoc.close();
		System.exit(1);
	}

	/**
	 * Metodo que gera um mac sobre o ficheiro fileToProtect e o guarda no ficheiro
	 * fileMac
	 * 
	 * @param fileMac
	 *            ficheiro a guardar o mac criado
	 * @param fileToProtect
	 *            ficheiro sobre o qual se cria o mac
	 * @throws IOException
	 */
	private static void generateMac(String fileMac, String fileToProtect) throws IOException {
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

				int nBytes = 0;// copia para o buffer os bytes do ficheiro e
				// guarda o numero de bytes copiados
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
	 * Metodo que verifica usado para proteger o ficheiro fileToProtect guardado no
	 * ficheiro fileMac
	 * 
	 * @param fileMac
	 *            ficheiro onde esta guardado o mac
	 * @param fileToProtect
	 *            ficheiro protegido pelo mac
	 * @return 0 se o mac eh igual; -1 caso contrario
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
				long tamanhoFicheiro = fileToProtect.length();
				byte[] byteFicheiro = new byte[1024];
				int nBytes = 0;
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
	 * Metodo que cria a assinatura sobre o ficheiro file com a chave privada do
	 * servidor
	 * 
	 * @param file
	 *            ficheiro a ser criada a assinatura
	 */
	private static void createSignature(File file) {
		// chave privada do servidor vem da keystore
		String alias = "server";
		String password = "server";
		try {

			FileOutputStream fos = new FileOutputStream(file + ".sig");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			// nome do projeto tem de ser SC
			FileInputStream fileInputStream = new FileInputStream(
					"C:/Users/fc44870/eclipse-workspace/SC/" + "serverKeyStore.keystore");

			// keystore do servidor
			KeyStore keystore = KeyStore.getInstance("JKS");
			keystore.load(fileInputStream, password.toCharArray());

			// chave privada do servidor
			PrivateKey pk = (PrivateKey) keystore.getKey(alias, password.toCharArray());
			Signature s = Signature.getInstance("SHA256withRSA");
			fileInputStream.close();

			byte[] b = file.getName().getBytes();
			s.initSign(pk);
			s.update(b);
			oos.write(s.sign());
			oos.close();
			fos.close();
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException
				| UnrecoverableKeyException | InvalidKeyException | SignatureException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Metodo que verifica a assinatura de um ficheiro file
	 * 
	 * @param file
	 *            ficheiro da assinatura
	 * @return 0 se a assinatura esta correta; -1 caso contrario
	 */
	public static int verifyServerSignature(File file) {
		File follow = new File("REPOSITORIO_SERVIDOR/followers.txt");
		String alias = "server";
		String password = "server";
		int ret = 0;
		try {
			// ler a chave do ficheiro
			FileInputStream fis = new FileInputStream(follow + ".sig");
			ObjectInputStream ois = new ObjectInputStream(fis);
			byte[] buffer = new byte[256];
			ois.read(buffer);
			ois.close();

			// keystore do servidor
			KeyStore keystore = KeyStore.getInstance("JKS");
			FileInputStream fileInputStream = new FileInputStream(
					"C:/Users/fc44870/eclipse-workspace/SC/" + "serverKeyStore.keystore");
			keystore.load(fileInputStream, password.toCharArray());

			// obter chave publica do servidor e verificar a assinatura
			Certificate c = keystore.getCertificate(alias);
			PublicKey pubKey = c.getPublicKey();
			Signature s = Signature.getInstance("SHA256withRSA");
			byte b[] = file.getName().getBytes();
			s.initVerify(pubKey);
			s.update(b);
			if (s.verify(buffer)) { // assinatura correta
				fileInputStream.close();
				fis.close();
			} else { // assinatura incorreta
				fileInputStream.close();
				fis.close();
				ret = -1;
			}
		} catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException | InvalidKeyException
				| SignatureException e) {
			e.printStackTrace();
		}
		return ret;
	}
}