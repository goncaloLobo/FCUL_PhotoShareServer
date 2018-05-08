import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * Classe cliente
 * 
 * @author G035 Catarina Fitas Carlos Brito Goncalo Lobo
 *
 */
public class PhotoShare {

	private static Socket echoSocket;
	private static ObjectOutputStream out;
	private static ObjectInputStream in;
	private static FileInputStream input = null;
	private static FileOutputStream output = null;
	private static String inicial = null;
	private static String[] comando = null;
	private static String[] hostnameIP = null;
	private static Scanner sc;
	private static String userCorrente;

	public static void main(String[] args) throws IOException, ClassNotFoundException {

		System.out.println("Bem vindo ao sistema de partilha de fotos PhotoShare!\n"
				+ "Aqui pode partilhar, armazenar, comentar e colocar likes e dislikes nas fotos dos utilizadores que segue!");
		sc = new Scanner(System.in);
		while (!(inicial = sc.nextLine()).equals("quit")) {
			comando = inicial.split(" ");
			if (validarInput(comando)) {
				if (!comando[1].startsWith("-")) { // registar ou autenticar
					userCorrente = comando[1];
					if (comando.length == 3) { // PhotoShare user serverAddress
						hostnameIP = comando[2].split(":");
						establishConnections();
						// pedir password novamente e autenticar
						System.out.println("Falta introduzir a password.");
						String opcao = sc.nextLine();
						// autenticar
						out.writeObject(inicial);
						out.writeObject(opcao);
						int teste = (int) in.readObject();
						switch (teste) {
						case 2: // user autenticado com sucesso
							System.out.println("Esta autenticado no sistema PhotoShare.");
							break;
						case 1: // user nao existe
							System.out.println("Nao esta registado no sistema PhotoShare.");
							break;
						case -1: // user existe mas pwd errada
							System.out.println("Password errada, tente novamente.");
							break;
						case -2: // user nao existe
							System.out.println("Nao existe esse utilizador no sistema, tente novamente.");
							break;
						}
					} else if (comando.length == 4) {
						hostnameIP = comando[3].split(":");
						establishConnections();
						out.writeObject(inicial);
						int teste = (int) in.readObject();
						switch (teste) {
						case 2: // user autenticado com sucesso
							System.out.println("Esta autenticado no sistema PhotoShare.");
							break;
						case 1: // user nao existe
							System.out.println("Nao esta registado no sistema PhotoShare.");
							break;
						case -1: // user existe mas pwd errada
							System.out.println("Password errada, tente novamente.");
							break;
						case -2: // user nao existe
							System.out.println("Nao existe esse utilizador no sistema, tente novamente.");
							break;
						}
					}
				} else {
					// estamos perante as operacoes -a, -l, -g, -f, -r, -D, -i, -L ou -c
					establishConnections();
					out.writeObject(inicial);
					switch (comando[1]) { // switch case operacoes
					case "-a":
						out.writeObject(userCorrente);
						addCopy(comando[2]);
						break;
					case "-l":
						out.writeObject(userCorrente);
						listarFotos();
						break;
					case "-g":
						out.writeObject(userCorrente);
						copiaPhoto(comando[2]);
						break;
					case "-f":
						out.writeObject(userCorrente);
						followers(comando[2]);
						break;
					case "-r":
						out.writeObject(userCorrente);
						removeFollower(comando[2]);
						break;
					case "-D":
						out.writeObject(userCorrente);
						dislike(comando[2], comando[3]);
						break;
					case "-i":
						out.writeObject(userCorrente);
						getInfoPhotos(comando[2], comando[3]);
						break;
					case "-L":
						out.writeObject(userCorrente);
						addLike(comando[2], comando[3]);
						break;
					case "-c":
						out.writeObject(userCorrente);
						addComent();
						break;
					default:
						System.out.println("Operação não existe.");
						break;
					}
				}
			} else {
				System.out.println("Comando incorreto! Tente de novo.");
			}
		}
		closeAll(); // fechar outputstream, inputstream e socket
		sc.close();
	}

	/**
	 * Metodo que adiciona um comentario a uma fotografia
	 * 
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private static void addComent() throws ClassNotFoundException, IOException {
		int valor = (int) in.readObject();
		String nome;
		switch (valor) {
		case 1:
			System.out.println("A foto nao existe no sistema.");
			break;
		case 2:
			System.out.println("O seu comentario foi adicionado com sucesso.");
			break;
		case 0:
			nome = (String) in.readObject();
			System.out.println("Nao eh follower do utilizador " + nome + ".");
			break;
		case -1:
			nome = (String) in.readObject();
			System.out.println("O utilizador " + nome + " nao existe no sistema.");
		}

	}

	/**
	 * Metodo que remove um follower de uma dado utilizador
	 * 
	 * @param users
	 *            utilizador a remover
	 * @throws IOException
	 */
	private static void removeFollower(String users) throws IOException {
		out.writeObject(users);
		String nome;
		int valor;
		try {
			// numero de users a remover recebido do servidor
			int size = (int) in.readObject();
			for (int i = 0; i < size; i++) {
				valor = (int) in.readObject();
				switch (valor) {
				case 1:
					nome = (String) in.readObject();
					System.out.println("O utilizador " + nome + " nao faz parte da lista de seguidores que o seguem.");
					break;
				case 2:
					nome = (String) in.readObject();
					System.out.println("O utilizador " + nome
							+ " foi removido da lista de utilizadores que o seguem com sucesso.");
					break;
				case 0:
					nome = (String) in.readObject();
					System.out.println("O utilizador " + nome + " nao existe no sistema.");
					break;
				}
			}
		} catch (ClassNotFoundException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Metodo que adiciona um follower
	 * 
	 * @param users
	 *            utilizador a adicionar
	 * @throws IOException
	 */
	private static void followers(String users) throws IOException {
		out.writeObject(users);
		int valor;
		String nome;
		try {
			int size = (int) in.readObject();
			for (int i = 0; i < size; i++) {
				valor = (int) in.readObject();
				switch (valor) {
				case 1:
					nome = (String) in.readObject();
					System.out.println("O utilizador " + nome + " ja faz parte da lista de seguidores que o seguem.");
					break;
				case 2:
					nome = (String) in.readObject();
					System.out.println("O utilizador " + nome + " foi adicionado como seu seguidor com sucesso.");
					break;
				case 0:
					nome = (String) in.readObject();
					System.out.println("O utilizador " + nome + " nao existe no sistema.");
					break;
				case -1:
					nome = (String) in.readObject();
					System.out.println("Nao eh possivel ser seguidor de si proprio.");
				}
			}
		} catch (ClassNotFoundException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Metodo que copia uma foto ou mais fotos de um dado utilizador para o nosso
	 * repositorio
	 * 
	 * @param user
	 *            utilizador corrente
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private static void copiaPhoto(String user) throws ClassNotFoundException, IOException {
		int numeroFiles = 0;
		String nomeFicheiro;
		String nome;
		File ficheiro = new File("myRep");
		if (!ficheiro.exists()) {
			ficheiro.mkdirs();
		}

		int recebido = (int) in.readObject();
		if (recebido == 1) { // se o user existe
			recebido = (int) in.readObject();
			if (recebido == 100) { // user.equals(userCorrente)
				numeroFiles = (int) in.readObject();
				if (numeroFiles == -1) {
					System.out.println("Nao existem ficheiros seus no servidor.");
				}
				// ciclo para receber os .keys dos ficheiros
				for (int j = 0; j < numeroFiles; j++) {
					nomeFicheiro = (String) in.readObject();
					output = new FileOutputStream(ficheiro + File.separator + nomeFicheiro);
					receiveFile();
				}
				//ciclo para receber os .cifs e decifrar
				for (int j = 0; j < numeroFiles; j++) {
					nomeFicheiro = (String) in.readObject();
					output = new FileOutputStream(ficheiro + File.separator + nomeFicheiro);
					receiveFile();
					decipherFiles(new File(ficheiro + File.separator + nomeFicheiro));
				}
				System.out.println("Ficheiros recebidos com sucesso.");

			} else { // follower a sacar as coisas
				recebido = (int) in.readObject();
				if (recebido == 200) { // follower segue o user
					File folder = new File("myRep" + File.separator + user);
					folder.mkdirs();

					numeroFiles = (int) in.readObject();
					if (numeroFiles == -1) {
						System.out.println("Nao existem ficheiros desse utilizador no servidor.");
					}
					// ciclo para receber os .keys dos ficheiros
					for (int j = 0; j < numeroFiles; j++) {
						nomeFicheiro = (String) in.readObject();
						output = new FileOutputStream(ficheiro + File.separator + nomeFicheiro);
						receiveFile();
					}
					//ciclo para receber os .cifs e decifrar
					for (int j = 0; j < numeroFiles; j++) {
						nomeFicheiro = (String) in.readObject();
						output = new FileOutputStream(ficheiro + File.separator + nomeFicheiro);
						receiveFile();
						decipherFiles(new File(ficheiro + File.separator + nomeFicheiro));
					}
					System.out.println("Ficheiros recebidos com sucesso.");
					
				} else {
					nome = (String) in.readObject();
					System.out.println("Nao eh follower do user " + nome + ".");
				}
			}
		} else {
			nome = (String) in.readObject();
			System.out.println("O utilizador " + nome + " nao existe.");
		}
	}

	/**
	 * Metodo que vai receber um dado ficheiro da parte do servidor
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private static void receiveFile() throws IOException, ClassNotFoundException {
		long tamanho = (long) in.readObject();
		byte[] buffer = new byte[1024];
		int n = -1;
		output.flush();
		do {
			if (tamanho - 1024 >= 0) {
				// Se existirem 1024 bytes para receber
				n = in.read(buffer, 0, 1024);
			} else {
				n = in.read(buffer, 0, (int) tamanho);
			}
			tamanho -= 1024;
			output.write(buffer, 0, n);
			output.flush();
		} while (tamanho > 0);
	}

	/**
	 * Metodo que lista todas as fotografias de um dado utilizador
	 * 
	 * @throws IOException
	 */
	private static void listarFotos() throws IOException {
		String valor;
		int size;
		try {
			size = (int) in.readObject();
			valor = (String) in.readObject();
			switch (valor) {
			case "inexistente":
				System.err.println("O utilizador nao existe no sistema.");
				break;
			case "info":
				for (int i = 0; i < size; i++) {
					String nomeFile = (String) in.readObject();
					String novoNomeFile = nomeFile.substring(0, nomeFile.length() - 4) + ".jpg";
					String dataFile = (String) in.readObject();
					System.out.println("Fotografia: " + novoNomeFile + " \nData de publicacao: " + dataFile);
				}
				break;
			case "vazio":
				valor = (String) in.readObject();
				System.err.println("O utilizador " + valor + " nao tem ficheiros no servidor.");
				break;
			case "nao eh follower":
				System.err.println("Nao segue este utilizador.");
			}
		} catch (ClassNotFoundException e) {
			System.err.println(e.getMessage());
		}

	}

	/**
	 * Metodo que adiciona/copia fotos
	 * 
	 * @param listaFotos
	 *            lista de fotografias de um dado utilizador
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private static void addCopy(String listaFotos) throws IOException, ClassNotFoundException {
		String[] fotos = listaFotos.split(":");
		String nome;
		out.writeObject(fotos.length);
		for (int i = 0; i < fotos.length; i++) { // envio de fotos separadamente
			// p servidor
			// enviar nome da foto
			out.writeObject(fotos[i]);
			if ((int) in.readObject() == 100) {
				int verifica = (int) in.readObject();
				if (verifica == 5) {
					System.err.println("Ja existe essa foto no servidor.");
				} else {
					File temp = new File("myRep" + File.separator + fotos[i]);
					if (!temp.exists() && !temp.isDirectory()) {
						// nao existe o ficheiro, nao cria o input
						out.writeObject(false);
						System.err.println("Nao tem nenhum ficheiro no seu rep com esse nome.");
					} else {
						out.writeObject(true);
						File photoToSend = new File("myRep" + File.separator + fotos[i]);
						input = new FileInputStream(photoToSend);

						// criar a assinatura do ficheiro
						createSignatureAndSend(photoToSend);

						// cifrar o ficheiro a enviar
						cipherFile(photoToSend);
						File ficheiroCifrado = new File("myRep" + File.separator + fotos[i] + ".cif");
						File keyFicheiroCifrado = new File("myRep" + File.separator + fotos[i] + ".key");

						// ler a chave usada para cifrar o ficheiro e enviar para o servidor
						readKey(keyFicheiroCifrado);
						input = new FileInputStream(ficheiroCifrado);

						// enviar o ficheiro cifrado
						sendFile();
						System.out.println("Ficheiro enviado com sucesso.");
						input.close();
						Files.delete(ficheiroCifrado.toPath());
						Files.delete(keyFicheiroCifrado.toPath());
					}
				}
			} else {
				nome = (String) in.readObject();
				System.err.println("O nome " + nome + " nao e um nome de foto valido. Envie novamente.");
			}
		}
	}

	/**
	 * Metodo que le a chave usada para cifrar o ficheiro e a envia para o servidor
	 * @param keyFicheiroCifrado chave do ficheiro cifrado
	 */
	private static void readKey(File keyFicheiroCifrado) {
		byte[] v = null;
		FileInputStream fis;
		try {
			v = new byte[16];
			fis = new FileInputStream(keyFicheiroCifrado);
			// tamanho do ficheiro
			long tamanho = fis.available();
			out.writeObject(tamanho);
			int n = -1;
			do {
				if (tamanho - 16 >= 0) {
					n = fis.read(v, 0, 16);
				} else {
					n = fis.read(v, 0, (int) tamanho);
				}
				tamanho -= 16;
				out.write(v, 0, n);
				out.flush();
			} while (tamanho > 0);
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Metodo que envia um dado ficheiro para o servidor
	 * 
	 * @throws IOException
	 */
	private static void sendFile() throws IOException {
		long tamanho = input.available();
		System.out.println("tamanho do ficheiro: " + tamanho);
		// enviar tamanho da foto
		out.writeObject(tamanho);
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
			out.write(buffer, 0, n);
			out.flush();
		} while (tamanho > 0);
	}

	/**
	 * Metodo que adiciona um like a uma fotografia
	 * 
	 * @param user
	 *            utilizador a que pertence a fotografia
	 * @param foto
	 *            fotografia em vai ser adicionada um like
	 * @throws IOException
	 */
	private static void addLike(String user, String foto) throws IOException {
		out.writeObject(user); // enviar o user para o lado do server
		out.writeObject(foto); // enviar nome da fotos

		int valor;
		String nome;
		try {
			valor = (int) in.readObject();
			switch (valor) {
			case 2:
				System.out.println("Adicionou like na foto com sucesso.");
				break;
			case 1:
				System.err.println("A foto em que quer colocar like nao existe.");
				break;
			case 0:
				nome = (String) in.readObject();
				System.err.println("Nao segue o utilizador " + nome + ".");
				break;
			case -1:
				nome = (String) in.readObject();
				System.err.println("O user " + nome + " nao existe no sistema.");
			}
		} catch (ClassNotFoundException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Metodo que obtem toda a informação de uma fotografia de um dado utilizador
	 * 
	 * @param user
	 *            utilizador
	 * @param foto
	 *            fotografia de onde se vai obter toda a informação
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private static void getInfoPhotos(String user, String foto) throws IOException, ClassNotFoundException {
		out.writeObject(user);
		out.writeObject(foto);
		String[] splitComment;
		String[] superSplit;

		int recebido = (int) in.readObject();
		String recebi;
		if (recebido == 4) {
			recebido = (int) in.readObject();
			if (recebido == 5) {
				recebido = (int) in.readObject();
				if (recebido == 1) {
					String nomeFoto = (String) in.readObject();
					System.out.println("Nome da fotografia: " + nomeFoto);
					int likes = (int) in.readObject();
					System.out.println("Numero de likes da fotografia: " + likes);
					int dislikes = (int) in.readObject();
					System.out.println("Numero de dislikes da fotografia: " + dislikes);
					String comment = (String) in.readObject();
					if (comment.equals("")) { // a foto nao tem comentarios
						System.out.println("A fotografia nao tem comentarios para mostrar.");
					} else {
						splitComment = comment.split(",");
						for (int i = 0; i < splitComment.length; i++) {
							superSplit = splitComment[i].split(":");
							System.out.println("O utilizador " + superSplit[0] + " comentou: " + superSplit[1]);
						}
					}
				} else {
					recebi = (String) in.readObject();
					System.err.println("A fotografia nao existe.");
				}
			} else if (recebido == -5) {
				recebi = (String) in.readObject();
				System.err.println("Nao eh follower do user " + recebi + ".");
			}
		} else if (recebido == -4) {
			recebi = (String) in.readObject();
			System.err.println("O utilizador " + recebi + " nao existe no sistema.");
		}
	}

	/**
	 * Metodo que adiciona um dislike a uma determinada fotografia de um dado
	 * utilizador
	 * 
	 * @param user
	 *            utilizador que vai adicionar um dislike
	 * @param foto
	 *            fotografia onde vai ser colocado o dislike
	 * @throws IOException
	 */
	private static void dislike(String user, String foto) throws IOException {
		out.writeObject(user); // enviar o user para o lado do server
		out.writeObject(foto); // enviar nome da fotos

		int valor;
		String nome;
		try {
			valor = (int) in.readObject();
			switch (valor) {
			case 2:
				System.out.println("Adicionou dislike com sucesso.");
				break;
			case 1:
				System.err.println("A foto em que quer colocar dislike nao existe.");
				break;
			case 0:
				nome = (String) in.readObject();
				System.err.println("Nao segue o utilizador " + nome + ".");
				break;
			case -1:
				nome = (String) in.readObject();
				System.err.println("O user " + nome + " nao existe no sistema.");
				break;
			}
		} catch (ClassNotFoundException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Metodo que vai validar o input que o cliente vai introduzir na consola
	 * 
	 * @param comando
	 *            comando introduzido na consola
	 * @return true caso o comando seja aceite; false caso contrario
	 */
	private static boolean validarInput(String[] comando) {
		String[] serverAddress;
		if (comando.length < 3) {
			System.err.println("Input incorrecto. Tente de novo.\nExemplo: PhotoShare -f follower");
		} else {
			if (comando[0].equals("PhotoShare")) {
				if (comando[1].equals("-a") || comando[1].equals("-l") || comando[1].equals("-g")
						|| comando[1].equals("-f") || comando[1].equals("-r")) {
					if (comando.length > 3) {
						System.err.println("Operacao Incorrecta.");
					} else {
						return true;
					}
				}
				if (comando[1].equals("-i") || comando[1].equals("-L") || comando[1].equals("-D")) {
					if (comando.length > 4) {
						System.err.println("Operacao Incorrecta.");
					} else {
						return true;
					}
				}
				if (comando[1].equals("-c")) {
					if (comando.length < 4) {
						System.err.println("Operacao Incorrecta.");
					} else {
						return true;
					}
				}
				if (!comando[1].startsWith("-") && comando.length == 3) {
					serverAddress = comando[2].split(":");
					if (serverAddress.length == 1) {
						// se o serverAddress tem apenas 1 elemento eh porque
						// foi inserida a password
						// e nao a info sobre o servidor
						System.err.println("Operacao Incorreta. Nao inseriu informacao sobre o servidor.");
					} else {
						if (Integer.parseInt(serverAddress[1]) == 23232) {
							return true;
						} else {
							System.err.println("O porto inserido esta incorreto.");
						}
					}
				}
				if (!comando[1].startsWith("-") && comando.length == 4) {
					serverAddress = comando[3].split(":");
					if (Integer.parseInt(serverAddress[1]) == 23232) {
						return true;
					} else {
						System.err.println("O porto inserido esta incorreto.");
					}
				}

			} else {
				System.err.println("Input incorrecto. Tente de novo.\nExemplo: PhotoShare -f follower");
			}
		}
		return false;
	}

	/**
	 * Metodo que vai estabelecer a ligação entre o cliente e o servidor
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private static void establishConnections() throws UnknownHostException, IOException {
		System.setProperty("javax.net.ssl.trustStore", "clientKeyStore.keyStore");
		SocketFactory sf = SSLSocketFactory.getDefault();
		echoSocket = sf.createSocket(hostnameIP[0], Integer.parseInt(hostnameIP[1]));
		out = new ObjectOutputStream(echoSocket.getOutputStream());
		in = new ObjectInputStream(echoSocket.getInputStream());
	}

	/**
	 * Metodo que vai fechar as sockets, inputStream e outputStream
	 * 
	 * @throws IOException
	 */
	private static void closeAll() throws IOException {
		out.close();
		in.close();
		echoSocket.close();
	}

	/**
	 * Metodo que cifra um ficheiro file
	 * @param file ficheiro a cifrar
	 */
	public static void cipherFile(File file) {
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
					"C:/Users/fc44870/eclipse-workspace/SC/" + "serverKeyStore.keystore");

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
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IOException
				| CertificateException | KeyStoreException | IllegalBlockSizeException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Metodo para decifrar o ficheiro file
	 * @param file ficheiro a decifrar
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
		} catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
				| KeyStoreException | UnrecoverableKeyException | CertificateException e) {
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * Metodo que cria a assinatura e envia para o servidor
	 * @param file ficheiro 
	 */
	private static void createSignatureAndSend(File file) {
		try {
			// chave privada do servidor vem da keystore do cliente
			String alias = "client";
			String password = "client";

			// nome do projeto tem de ser SC
			FileInputStream fileInputStream = new FileInputStream(
					"C:/Users/fc44870/eclipse-workspace/SC/" + "clientKeyStore.keystore");

			KeyStore keystore = KeyStore.getInstance("JCEKS");
			keystore.load(fileInputStream, password.toCharArray());
			PrivateKey pk = (PrivateKey) keystore.getKey(alias, password.toCharArray());
			Signature s = Signature.getInstance("SHA256withRSA");

			byte[] b = file.getName().getBytes();
			s.initSign(pk);
			s.update(b);
			out.writeObject(s.sign());
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException
				| UnrecoverableKeyException | InvalidKeyException | SignatureException e) {
			System.err.println(e.getMessage());
		}
	}
}