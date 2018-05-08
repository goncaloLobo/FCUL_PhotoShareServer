import java.io.BufferedReader;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

//Servidor PhotoShareServer

/**
 * Classe que implementa a comunicacao entre os clientes e o servidor
 * 
 * @author G035 Catarina Fitas Carlos Brito Goncalo Lobo
 *
 */
public class PhotoShareServer {

	public static void main(String[] args) throws IOException {
		System.out.println("servidor: main");
		PhotoShareServer server = new PhotoShareServer();

		File repositorio = new File("REPOSITORIO_SERVIDOR");
		if (!repositorio.exists()) {
			repositorio.mkdirs();
		}
		File repositorio_c = new File("REPOSITORIO_CLIENTES");
		if (!repositorio_c.exists()) {
			repositorio_c.mkdirs();
		}

		server.startServer();
	}

	public void startServer() throws IOException {
		ServerSocket sSoc = null;
		try {
			sSoc = new ServerSocket(23232);
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
		// sSoc.close();
	}

	/**
	 * Classe ServerThread que extende a classe Thread e que implementa as
	 * threads utilizadas para a comunicacao entre os clientes
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
				String comando = (String) inStream.readObject();
				comandoAux = comando.split(" ");
				splitDoComentario = comando.split("\"");
				String[] userAndFoto;
				// autenticar/registar
				if (comando.equals("quit")) {
					outStream.close();
					inStream.close();
				} else if (!comandoAux[1].startsWith("-")) {
					userCorrente = comandoAux[1];
					if (comandoAux.length == 3) {
						String pwd = (String) inStream.readObject();
						int autentica = autenticarUtilizador(userCorrente, pwd);
						switch (autentica) {
						case 1:
							outStream.writeObject(1);
							break;
						case 2:
							outStream.writeObject(2);
							break;
						case 3:
							outStream.writeObject(3);
							break;
						case 0:
							outStream.writeObject(0);
							break;
						}
					}

					if (comandoAux.length == 4) {
						userCorrente = comandoAux[1];
						int autentica = autenticarUtilizador(userCorrente, comandoAux[2]);
						switch (autentica) {
						case 1:
							outStream.writeObject(1);
							break;
						case 2:
							outStream.writeObject(2);
							break;
						case 3:
							outStream.writeObject(3);
							break;
						case 0:
							outStream.writeObject(0);
							break;
						}
					}
				} else {
					switch (comandoAux[1]) {
					case "-f": // DONE
						// como nao eh possivel manter registo do user corrente
						// eh recebido do lado do cliente que consegue manter
						// esse registo
						userCorrente = (String) inStream.readObject();
						followers(userCorrente);
						break;
					case "-r": // DONE
						userCorrente = (String) inStream.readObject();
						removeFollowers(userCorrente);
						break;
					case "-a": // DONE
						userCorrente = (String) inStream.readObject();
						fotos = comandoAux[2].split(":");
						addCopy(userCorrente, fotos);
						break;
					case "-c": // DONE
						userCorrente = (String) inStream.readObject();
						userAndFoto = splitDoComentario[2].split(" ");
						addComment(splitDoComentario[1], userAndFoto[1], userAndFoto[2]);
						break;
					case "-L": // DONE
						userCorrente = (String) inStream.readObject();
						fotos = comandoAux[2].split(":");
						addLike(userCorrente, fotos);
						break;
					case "-D": // DONE
						userCorrente = (String) inStream.readObject();
						fotos = comandoAux[2].split(":");
						dislike(userCorrente, fotos);
						break;
					case "-l": // DONE
						userCorrente = (String) inStream.readObject();
						listarFotos(comandoAux[2]);
						break;
					case "-i": // DONE
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
		 * Metodo que adiciona um follower ao utilizador local
		 * Caso o utilizador local seja o follower, nao eh possivel ser-se follower de si proprio
		 * @param user utilizador local
		 * @throws ClassNotFoundException
		 * @throws IOException
		 * @requires existsUser(user) == true
		 */
		private void followers(String user) throws ClassNotFoundException, IOException {
			String users = (String) inStream.readObject();
			String[] utilizadores = users.split(":");

			outStream.writeObject(utilizadores.length);
			// enviar num de utilizadores para o ciclo no cliente
			for (int i = 0; i < utilizadores.length; i++) {
				if (!utilizadores[i].equals(user)) {
					if (existsUser(utilizadores[i])) {
						if (!isMyFollower(utilizadores[i], user)) {
							addFollower(utilizadores[i], user);
							outStream.writeObject(2); // yeaaaaay adicionei
							// follower
							outStream.writeObject(utilizadores[i]);
						} else {
							outStream.writeObject(1); // ja eh follower
							outStream.writeObject(utilizadores[i]);
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
		 * @param follower follower a adicionar
		 * @param user utilizador a adicionar um follower
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
		 * @param follower utilizador a verificar se eh follower do user 
		 * @param user utilizador corrente
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
		 * @param user utilizador
		 * @throws ClassNotFoundException
		 * @throws IOException
		 */
		private void removeFollowers(String user) throws ClassNotFoundException, IOException {
			String users = (String) inStream.readObject();
			String[] utilizadores = users.split(":");
			outStream.writeObject(utilizadores.length);
			for (int i = 0; i < utilizadores.length; i++) {
				if (existsUser(utilizadores[i])) {
					if (isMyFollower(utilizadores[i], userCorrente)) {
						removeFollower(utilizadores[i], userCorrente);
						outStream.writeObject(2); // removi follower
						outStream.writeObject(utilizadores[i]);
					} else {
						outStream.writeObject(1); // nunca foi follower
						outStream.writeObject(utilizadores[i]);
					}
				} else {
					outStream.writeObject(0); // user nao existe!!!!
					outStream.writeObject(utilizadores[i]);
				}
			}
		}

		/**
		 * Metodo auxiliar para a remocao de um seguidor. Efectua as alteracoes no respectivo ficheiro
		 * @param follower utilizador a remover
		 * @param user utilizador corrente
		 * @throws IOException
		 */
		private void removeFollower(String follower, String user) throws IOException {
			BufferedReader br = new BufferedReader(new FileReader("REPOSITORIO_SERVIDOR/followers.txt"));
			File follow = new File("REPOSITORIO_SERVIDOR/followers.txt");
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
		 * Metodo que adiciona/copia fotografias no repositorio do servidor.
		 * Por cada fotografia adicionada/copiada, eh criado um ficheiro de informacoes
		 * da mesma
		 * @param user utilizador corrente
		 * @param fotos fotografias a adicionar/copiar
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
							"REPOSITORIO_CLIENTES" + File.separator + user + File.separator + nomeFoto);
					if (ficheiro.exists() && !ficheiro.isDirectory()) {
						outStream.writeObject(5);
						// se a foto existir entao nao eh enviada de novo
					} else {
						outStream.writeObject(6);
						ficheiro = new File("REPOSITORIO_CLIENTES" + File.separator + user + File.separator + nomeFoto);
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
							receiveFile(); // enviar o ficheiro

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
						}
					}
				} else {
					outStream.writeObject(-1);
					outStream.writeObject(nomeFoto);
				}
			}
		}


		/**
		 * Metodo que adiciona comentario numa fotografia de um dado utilizador
		 * @param comentario comentario a adicionar
		 * @param user utilizador a que pertence a fotografia
		 * @param nomeFoto nome da fotografia a comentar
		 * @throws IOException
		 */
		private void addComment(String comentario, String user, String nomeFoto) throws IOException {
			String nomeFotoTXT = convertToTXT(nomeFoto);
			if (existsUser(user)) { // se o user existe
				if (isMyFollower(userCorrente, user)) {
					// se o follower eh follower do user atual
					if (existsPhoto(user, nomeFoto)) {
						// se o user tem a foto nomeFoto
						registaComment(comentario, user, nomeFotoTXT);
						outStream.writeObject(2); // adicionou comentario
					} else {
						outStream.writeObject(1); // a foto nao existe
					}
				} else if (user.equals(userCorrente)) {
					// eu a comentar as minhas fotos
					if (existsPhoto(user, nomeFoto)) {
						// se o user tem a foto nomeFoto
						registaComment(comentario, user, nomeFotoTXT);
						outStream.writeObject(2); // adicionou comentario
					} else {
						outStream.writeObject(1); // a foto nao existe
					}
				} else {
					outStream.writeObject(0);
					// o user nao eh follower logo nao pode comentar
					outStream.writeObject(user);
				}
			} else { // user nao existe
				outStream.writeObject(-1);
				outStream.writeObject(user);
			}
		}

		/**
		 * Metodo auxiliar para a adicao de comentarios. Efectua as alteracoes no respectivo ficheiro.
		 * @param comentario comentario a adicionar
		 * @param user utilizador a quem pertence a fotografia
		 * @param nomeFoto nome da fotografia
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
		 * @param userLocal utilizador a quem pertence a fotografia
		 * @param fotos fotografia a adicionar um like
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private void addLike(String userLocal, String[] fotos) throws IOException, ClassNotFoundException {
			String follower = (String) inStream.readObject();
			String nomeFoto = (String) inStream.readObject();
			String nomeFotoTXT = convertToTXT(nomeFoto);

			if (existsUser(follower)) { // existe no sistema
				if (isMyFollower(userLocal, follower)) { // se o follower eh
					// follower do user
					// atual
					if (existsPhoto(follower, nomeFoto)) {
						registaLike(follower, userLocal, nomeFotoTXT);
						outStream.writeObject(2); // adicionei like
					} else {
						outStream.writeObject(1); // a foto nao existe
					}
				} else {
					outStream.writeObject(0); // o user nao eh follower, logo
					// nao pode meter like
					outStream.writeObject(follower);
				}
			} else {
				outStream.writeObject(-1);
				outStream.writeObject(follower);
			}
		}

		/**
		 * Metodo que verifica se uma fotografia existe
		 * @param follower utilizador cuja fotografia se quer adicionar like
		 * @param nomeFoto  nome da fotografia
		 * @return true se existe; false caso contrario
		 */
		private boolean existsPhoto(String follower, String nomeFoto) {
			File teste = new File("REPOSITORIO_CLIENTES" + File.separator + follower + File.separator + nomeFoto);
			return teste.exists();
		}

		/**
		 * Metodo auxiliar para a adicao de um like. Efectua as alteracoes no ficheiro respectivo.
		 * @param follower utilizador cuja fotografia se quer adicionar like
		 * @param userLocal utilizador que pretende adicionar um like
		 * @param nomeFoto nome da fotografia
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
		 * @param userLocal utilizador que tem a fotografia
		 * @param fotos fotografia a obter informacoes
		 * @throws ClassNotFoundException
		 * @throws IOException
		 */
		private void getInfoFotos(String userLocal, String[] fotos) throws ClassNotFoundException, IOException {
			String follower = (String) inStream.readObject();
			String nomeFoto = (String) inStream.readObject();
			if (existsUser(follower)) {
				outStream.writeObject(4);
				if (isMyFollower(follower, userLocal)) {
					// userLocal eh follower do follower
					outStream.writeObject(5);
					if (existsPhoto(follower, nomeFoto)) {
						outStream.writeObject(1);
						outStream.writeObject(nomeFoto);
						int n_likes = getLikesFromPhoto(nomeFoto, follower);
						outStream.writeObject(n_likes);
						int n_dislikes = getDislikesFromPhoto(nomeFoto, follower);
						outStream.writeObject(n_dislikes);
						String comentarios = getCommentsFromPhoto(nomeFoto, follower);
						outStream.writeObject(comentarios);
					} else {
						outStream.writeObject(-1); // a foto nao existe
						outStream.writeObject(nomeFoto);
					}
				} else if (follower.equals(userLocal)) {
					// estamos a fazer -i de nos proprios
					outStream.writeObject(5);
					if (existsPhoto(follower, nomeFoto)) {
						outStream.writeObject(1);
						outStream.writeObject(nomeFoto);
						int n_likes = getLikesFromPhoto(nomeFoto, follower);
						outStream.writeObject(n_likes);
						int n_dislikes = getDislikesFromPhoto(nomeFoto, follower);
						outStream.writeObject(n_dislikes);
						String comentarios = getCommentsFromPhoto(nomeFoto, follower);
						outStream.writeObject(comentarios);
					} else {
						outStream.writeObject(-1); // a foto nao existe
						outStream.writeObject(nomeFoto);
					}
				} else {
					// o userLocal nao eh follower de follower
					outStream.writeObject(-5);
					outStream.writeObject(follower);
				}
			} else {
				// nao existe o user que tamos a tentar fazer -i
				outStream.writeObject(-4);
				outStream.writeObject(follower);
			}
		}

		/**
		 * Metodo que obtem os comentarios de uma determinada fotografia
		 * @param nomeFoto nome da fotografia
		 * @param follower utilizador a quem pertence a fotografia 
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
		 * @param nomeFoto nome da fotografia
		 * @param follower utilizador a quem pertence a fotografia
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
		 * @param nomeFoto nome da fotografia
		 * @param follower utilizador a quem pertence a fotografia
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
		 * @param userLocal utilizador a quem pertence a fotografia
		 * @param fotos fotografia a adicionar um dislike
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private void dislike(String userLocal, String[] fotos) throws ClassNotFoundException, IOException {
			String follower = (String) inStream.readObject();
			String nomeFoto = (String) inStream.readObject();
			String nomeFotoTXT = convertToTXT(nomeFoto);

			if (existsUser(follower)) { // existe no sistema
				if (isMyFollower(userLocal, follower)) { // se o follower eh
					// follower do user
					// atual
					if (existsPhoto(follower, nomeFoto)) {
						registaDislike(follower, userLocal, nomeFotoTXT);
						outStream.writeObject(2); // adicionei dislike
					} else {
						outStream.writeObject(1); // a foto nao existe
					}
				} else {
					outStream.writeObject(0); // o user nao eh follower, logo
					// nao pode meter dislike
					outStream.writeObject(follower);
				}
			} else
				outStream.writeObject(-1);
			outStream.writeObject(follower);
		}

		/**
		 * Metodo auxiliar para a adicao de um dislike. Efectua as alteracoes no ficheiro respectivo.
		 * @param follower utilizador cuja fotografia se quer adicionar dislike
		 * @param userLocal utilizador que pretende adicionar um dislike
		 * @param nomeFoto nome da fotografia
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
		 * @param user utilizador a verificar
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
		 * @param user utilizador de quem se quer copiar as fotografias
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private void copiaPhoto(String user) throws IOException, ClassNotFoundException {
			File auxiliar;
			ArrayList<File> userFiles;
			File[] listaAuxiliar;
			FileInputStream input = null;
			if (existsUser(user)) { // se o user existe
				outStream.writeObject(1);
				if (user.equals(userCorrente)) {
					outStream.writeObject(100);// se for o user corrente
					auxiliar = new File("REPOSITORIO_CLIENTES" + File.separator + userCorrente + File.separator);
					userFiles = new ArrayList<>();
					listaAuxiliar = auxiliar.listFiles();
					if (listaAuxiliar == null) {
						// se nao tem ficheiros
						outStream.writeObject(-1);
						outStream.writeObject("vazio");
					} else {
						for (int i = 0; i < listaAuxiliar.length; i++) {
							userFiles.add(listaAuxiliar[i]);
						}

						outStream.writeObject(userFiles.size());
						for (File file : userFiles) {
							input = new FileInputStream("REPOSITORIO_CLIENTES" + File.separator + userCorrente
									+ File.separator + file.getName());
							outStream.writeObject(file.getName());

							String verificacao = (String) inStream.readObject();
							if (!verificacao.equals("existe")) {
								// se o ficheiro nao existe na pasta do cliente
								// entao envia a foto e o txt
								sendFile(input);
								input.close();
							} else {
								// se existe nao vai enviar a fotografia, apenas
								// o txt
								for (int i = 0; i < listaAuxiliar.length; i++) {
									if (!listaAuxiliar[i].getName().endsWith(".txt"))
										userFiles.add(listaAuxiliar[i]);
								}
								outStream.writeObject(userFiles.size());
								// enviar numero de ficheiros a enviar para o
								// cliente
								for (File ficheiro : userFiles) {
									input = new FileInputStream("REPOSITORIO_CLIENTES" + File.separator + userCorrente
											+ File.separator + ficheiro.getName());
									outStream.writeObject(ficheiro.getName());
									sendFile(input);
									input.close();
								}
							}
						}
					}
				} else {
					outStream.writeObject(101);
					if (isMyFollower(userCorrente, user)) {
						outStream.writeObject(200);
						// se for o user corrente
						auxiliar = new File("REPOSITORIO_CLIENTES" + File.separator + user + File.separator);
						userFiles = new ArrayList<>();
						listaAuxiliar = auxiliar.listFiles();
						if (listaAuxiliar == null) { // se nao tem ficheiros
							outStream.writeObject(-1);
							outStream.writeObject("vazio");
						} else {
							for (int i = 0; i < listaAuxiliar.length; i++) {
								userFiles.add(listaAuxiliar[i]);
							}

							outStream.writeObject(userFiles.size());
							for (File file : userFiles) {
								input = new FileInputStream("REPOSITORIO_CLIENTES" + File.separator + user
										+ File.separator + file.getName());
								outStream.writeObject(file.getName());

								String verificacao = (String) inStream.readObject();
								if (!verificacao.equals("existe")) {
									sendFile(input);
									input.close();
								} else {
									// se existe nao vai enviar a fotografia,
									// apenas
									// o txt
									for (int i = 0; i < listaAuxiliar.length; i++) {
										if (!listaAuxiliar[i].getName().endsWith(".txt"))
											userFiles.add(listaAuxiliar[i]);
									}
									outStream.writeObject(userFiles.size());
									// enviar numero de ficheiros a enviar para
									// o
									// cliente
									for (File ficheiro : userFiles) {
										input = new FileInputStream("REPOSITORIO_CLIENTES" + File.separator + user
												+ File.separator + ficheiro.getName());
										outStream.writeObject(ficheiro.getName());
										sendFile(input);
										input.close();
									}
								}
							}
						}
					} else {
						outStream.writeObject(201);
						outStream.writeObject(user);
					}
				}
			} else {
				outStream.writeObject(-1);
				outStream.writeObject(user);
			}
		}

		/**
		 * Metodo que lista as fotografias de um dado utilizador com as respectivas datas
		 * de publicacao
		 * @param user utilizador a quem pertencem as fotografias
		 * @throws IOException
		 */
		private void listarFotos(String user) throws IOException {
			ArrayList<File> userFiles;
			File auxiliar;
			File[] listaAuxiliar;
			String data;
			if (existsUser(user)) {
				if (user.equals(userCorrente)) { // se for o user corrente
					auxiliar = new File("REPOSITORIO_CLIENTES" + File.separator + userCorrente + File.separator);
					userFiles = new ArrayList<>();
					listaAuxiliar = auxiliar.listFiles();
					if (listaAuxiliar == null) {
						// eu nao tenho ficheiros no servidor
						outStream.writeObject(-1);
						outStream.writeObject("vazio");
						outStream.writeObject(user);
					} else {
						for (int i = 0; i < listaAuxiliar.length; i++) {
							if (listaAuxiliar[i].getName().endsWith(".txt"))
								userFiles.add(listaAuxiliar[i]);
						}
						outStream.writeObject(userFiles.size());
						outStream.writeObject("info");
						for (File file : userFiles) {
							outStream.writeObject(file.getName().substring(0, file.getName().length() - 4));
							data = getDataFromFile(file);
							outStream.writeObject(data);
						}
					}

				} else {
					if (isMyFollower(userCorrente, user)) {
						auxiliar = new File("REPOSITORIO_CLIENTES" + File.separator + user + File.separator);
						userFiles = new ArrayList<>();
						listaAuxiliar = auxiliar.listFiles();
						if (listaAuxiliar == null) {
							// o user que quero fazer -l nao tem ficheiros no
							// servidor
							outStream.writeObject(-1);
							outStream.writeObject("vazio");
							outStream.writeObject(user);
						} else {
							for (int i = 0; i < listaAuxiliar.length; i++) {
								if (listaAuxiliar[i].getName().endsWith(".txt"))
									userFiles.add(listaAuxiliar[i]);
							}
							outStream.writeObject(userFiles.size());
							outStream.writeObject("info");
							for (File file : userFiles) {
								outStream.writeObject(file.getName().substring(0, file.getName().length() - 4));
								data = getDataFromFile(file);
								outStream.writeObject(data);
							}
						}
					} else {
						outStream.writeObject(-1);
						outStream.writeObject("nao eh follower");
					}
				}
			} else {
				outStream.writeObject(-1);
				outStream.writeObject("inexistente"); // user nao existe
			}
		}

		/**
		 * Metodo que obtem a data de um ficheiro
		 * @param file ficheiro de que se quer obter a data
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
				if (linhaAux[0].equals("Data:")) {
					data = linhaAux[1];
				}
			}
			br.close();
			return data;
		}

		/**
		 * Metodo que inicializa as informacoes de uma fotografia
		 * @param infoFotos ficheiro de ifnromacoes da fotografia
		 * @param data data de criacao do ficheiro
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
		 * @param user utilizador a autenticar
		 * @param passwd password do utilizador
		 * @return 1 se user eh o primeiro utilizador a autenticar-se; 2 se user eh autenticado com sucesso;
		 * 0 caso contrario; 3 se user se autentica pela primeira vez
		 * @throws IOException
		 */
		private int autenticarUtilizador(String user, String passwd) throws IOException {
			File passwords = new File("REPOSITORIO_SERVIDOR/users.txt");
			if (!passwords.exists()) {
				passwords.createNewFile();
				registarUtilizadorUsers(user, passwd);
				registarUtilizadorFollowers();
				return 1;
			} else {
				BufferedReader br = new BufferedReader(new FileReader(passwords));
				String linha;
				String[] linhaAux;
				boolean found = false;
				while ((linha = br.readLine()) != null) {
					linhaAux = linha.split(":");
					if (linhaAux[0].equals(user)) {
						found = true; // encontrou user, e nao podem haver 2
						// users com o mesmo nome
						if (linhaAux[1].equals(passwd)) {
							br.close();
							return 2;
						} else {
							br.close();
							return 0;
						}
					}
				}
				if (!found) {
					registarUtilizadorUsers(user, passwd);
					registarUtilizadorFollowers();
					br.close();
					return 3;
				}
				br.close();
			}
			return -1;
		}

		/**
		 * Metodo auxiliar para registar um seguidor. Efectua as alteracoes no ficheiro
		 * @throws IOException
		 */
		private void registarUtilizadorFollowers() throws IOException {
			FileWriter fw2 = new FileWriter(new File("REPOSITORIO_SERVIDOR/followers.txt"), true);
			fw2.append(userCorrente + ":" + "\n");
			fw2.close();
		}

		/**
		 * Metodo que regista um novo utilizador
		 * @param user utilizador a registar
		 * @param passwd password de user
		 * @throws IOException
		 */
		private void registarUtilizadorUsers(String user, String passwd) throws IOException {
			FileWriter fw = new FileWriter(new File("REPOSITORIO_SERVIDOR/users.txt"), true);
			fw.append(user + ":" + passwd + "\n");
			fw.close();
		}

		/**
		 * Metodo que envia um ficheiro para o cliente
		 * @param input stream de abertura do ficheiro
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
		 * @param nomeFoto nome da fotografia
		 * @return nome da fotografia.txt
		 */
		private String convertToTXT(String nomeFoto) {
			String[] novoNomeSplit = nomeFoto.split("\\.");
			String nomeFotoTXT = novoNomeSplit[0].concat(".txt");
			return nomeFotoTXT;
		}

		/**
		 * MEtodo que verifica se o formato de uma fotografia eh valido (termina em .jpg)
		 * @param nomeFoto nome da fotografia
		 * @return true se eh valido; false caso contrario
		 */
		public boolean checkIfJPG(String nomeFoto) {
			return nomeFoto.endsWith(".jpg");
		}
	}
}