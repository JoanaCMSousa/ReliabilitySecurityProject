NOTAS:
	-No diretorio recebido vao existir duas pastas, a pasta dos clientes e a pagina do servidor
	- Ao fazermos pull de um ficheiro de outro user vai ser criado uma diretoria para esse user, isto eh,
	ao fazermos pull de maria/myrep se a directoria maria/myrep nao existir localmente vai passar a existir
	(e vamos guardar la os ficheiros todos)
	-Deixamos os printds do lado do servidor porque achamos desnecessario estar a remove los 
	e facilita o teste

Como Executar O Servidor:
	1. Abrir na directoria do servidor (/servidor) a linha de comandos
	2. escrever java *.java para compilar todos os ficheiros java
	3. escrever java myGitServer <porto> onde <porto> vai ser o porto do servidor, que neste caso vai ser 23456
 
Como Exectuar O Cliente:
	1. Abrir, na directoria do cliente (/clientes) a linha de comandos
	2. escrever java *.java para compilar todos os ficheiros java
	3  e escrever o comando desejado

Comandos - existem diferencas importantes porfavor ler:

	myGit -init <rep_name> -> criar uma diretoria com nome rep_name localmente
	myGit <localUser> <serverAddress> [-p <password>] -> criar um utilizador,
		caso esse utilizador nao existia vai ser criado, ocorre sempre
	myGit <localUser> <serverAddress> [-p <password>] -push <file_name> 
		-> Envia o ficheiro file_name para o servidor, mete o no repositorio onde 
		o ficheiro se encontra. Esse diretorio eh necessario existir no lado do
		servidor.
	myGit <localUser> <serverAddress> [-p <password>] -push <rep_name> 
		-> Envia o repositorio rep_name para o servidor, cria o se nao existir e copia
		todos os ficheiros para la (se foram mudificados desde a ultima vez).
	myGit <localUser> <serverAddress> [-p <password>] -pull <rep_name> 
		-> Mesma coisa que o push so que do servidor para o cliente. Ao fazermos pull
		de um repositorio de outro utilizador mete o num repositorio ha parte com o nome
		do outro utilizador, se esse repositorio nao existe cria o.
	myGit <localUser> <serverAddress> [-p <password>] -pull <file_name>
		-> Mesma coisa que o push de um ficheiro, o diretorio onde o ficheiro file_name existe no lado do servidor 
		tem de existir no lado do cliente 
	myGit <localUser> <serverAddress> [ -p <password> ] -share <rep_name> <userId> 
		-> Partilha o repositorio <rep_name> do utilizador localUser com o utilizador userId,
		o repositorio rep_name tem de existir e o utilizador userId tambem	
	myGit <localUser> <serverAddress> [ -p <password> ] -remove <rep_name> <userId>
		-> Remove a partilha feita pelo share, mesmas condicoes 
		
NECESSIDADES ANTES DE EXECUTAR:
	-- Para o programa funcionar como deve ser eh necessario executar uns certos comandos antes de iniciar o programa
	-- Por cada cliente eh necesseraio executar o comando das keytools mostrado por baixo
		>keytool -genkeypair -keyalg RSA -keysize 2048 -keystore clientkeystore.dd -alias client
	-- A primeira password a inserir, ao executar este comando, eh 'qwerty'. A seguir, pode inserir os dados que quiser as perguntas
	que serao feitas. A segunda password a inserir depois eh igual.
	-- Para o servidor tambem eh necessario executar o seguinte comando antes de iniciar para criar a keystore
		>keytool -genkeypair -keyalg RSA -keysize 2048 -keystore serverkeystore.dd -alias server
	-- A seguir, deve executar os mesmos passos que fez anteriormente para a keystore do cliente, com as mesmas passwords.
	-- Vai ser necessario executar mais uns comandos para guardar certos certificados em certos truststores
	-- Para exportar o certificado auto-assinado do servidor, eh suposto executar o seguinte comando:
		>keytool -exportcert -alias server -file server.cer -keystore serverkeystore.dd
	-- Para exportar o certificado auto-assinado do cliente, eh suposto executar o seguinte comando:
		>keytool -exportcert -alias client -file client.cer -keystore clientkeystore.dd
	-- Para importar os certificados nas truststores do servidor e do cliente, tem de executar os seguintes comandos:
		>keytool -importcert -alias server -keystore clientkeystore.dd -file server.cer
		>keytool -importcert -alias client -keystore serverkeystore.dd -file client.cer
	-- Os ficheiros clientkeystore.dd e client.cer tem de ser no lado no cliente. O ficheiros serverkeystore.dd e server.cer tem 
	   de estar no lado do servidor.
