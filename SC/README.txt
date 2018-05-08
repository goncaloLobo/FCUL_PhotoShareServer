Grupo 035
Catarina Fitas 41677
Carlos Brito 45500
Goncalo Lobo 44870

Criar keystore do PhotoShare:
keytool -genkeypair -keyalg RSA -keysize 2048 -alias client -keystore clientKeyStore.keystore

Criar keystore do PhotoShareServer:
keytool -genkeypair -keyalg RSA -keysize 2048 -alias server -keystore serverKeyStore.keystore

Exportar o certificado do PhotoShareServer:
keytool -exportcert -alias server -file server.cer -keystore serverKeyStore.keystore

Importar certificado do servidor para a trustore do PhotoShare:
keytool -importcert -alias server -file server.cer -keystore clientKeyStore.keystore

-------------------------------------------------------------------------------------

Para configurar o ficheiro de politicas do servidor:
- RUN CONFIGURATIONS -> PhotoShareServer -> VM Arguments -> -Djava.security.manager -Djava.security.policy=server.policy

Para executar o PhotoShareServer ou o ManUsers eh necessaria uma password criada na primeira vez que se executa qualquer uma dessas classes.

Para configurar o ficheiro de politicas do cliente
- RUN CONFIGURATIONS -> PhotoShare -> VM Arguments -> -Djava.security.manager -Djava.security.policy=client.policy

Para executar o cliente eh executar normalmente:
- O serverAddress deve ser composto por: enderecoIP:porto
- PhotoShare <localUserId> <password> <serverAddress>
     [ -a <photos> | -l <userId> | -i <userId> <photo> | -g <userId> |
     -c <comment> <userId> <photo> | -L <userId> <photo> |
     -D <userId> <photo> | -f <followUserIds> | -r <followUserIds> ] 
- No comando -a, se forem introduzidas varias fotografias, devem terminar em .jpg e separadas por ":"
  (ex.: PhotoShare -a foto1.jpg:foto2.jpg)
  Para enviar fotografias através do comando -a, as fotografias deverao ser colocadas na pasta "myRep",
  criada automaticamente quando um novo cliente se regista
- No comando -f, se forem introduzidos varios utilizadores, devem estar separados por ":"
  (ex.: PhotoShare -f utilizador1:utilizador2)
- No comando -r, se forem introduzidos varios utilizadores, devem estar separados por ":"
  (ex.: PhotoShare -r utilizador1:utilizador2)
- No comando -c o comentario deve estar introduzido entre aspas (")
  (ex.: PhotoShare -c "comentario" utilizador foto)

Para executar o ManUsers eh necessario colocar a password:
-r username password: registar utilizador
-d username: remover utilizador
-c username newPassword: mudar password do utilizador
