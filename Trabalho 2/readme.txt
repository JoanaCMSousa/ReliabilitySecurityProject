NOTES:
	- In the directory there are two folders, one for the clients and one for the servers
	- When we make pull of a file of another user, a directory will be created for that user, i.e.,
	if we pull of maria/myrep and this does not exist, it will exist after pulling (and we save there all the files).
	- We left the system.out.print on the server side to make our tests easier.

How to execute The Server:
	1. Open the directory of the server (/servidor) on the terminal
	2. Write java *.java to compile all java files
	3. Write java myGitServer <port>, where <port> will be the port of the server, in our case it will be 23456
 
How to execute the Client:
	1. Open in the terminal the directory of the client (/cients)
	2. Write java *.java to compile all java files
	3. Write the desired command. The possible comands are below.

Commands - It exists important differences between each command, please read:

	myGit -init <rep_name> -> create a local directory with name rep_name
	myGit <localUser> <serverAddress> [-p <password>] -> create user, if the suer does not exist, it will be created
	myGit <localUser> <serverAddress> [-p <password>] -push <file_name> 
		-> Sends the filfile_name to the server, puts in the repository 
		where the file is. It is necessary for the directory exist in the server side  
	myGit <localUser> <serverAddress> [-p <password>] -push <rep_name> 
		-> Send the repository rep_name to the server, it creates it if it does not 
		exist in the server side and copies the all the files in there (if they were modified since last time).
	myGit <localUser> <serverAddress> [-p <password>] -pull <rep_name> 
		-> Same thing as the 'push' command but from the server to the client. 
		When executing the 'pull' command of a repository from another user, it creates 
		a repository on the side (?) with the name of other user, if it doesn't exist, creates it.
	myGit <localUser> <serverAddress> [-p <password>] -pull <file_name>
		-> Same thing as the 'push' command of a file, the directory whe the file 
		file_name exists on the server side must exist on the client side. 
	myGit <localUser> <serverAddress> [ -p <password> ] -share <rep_name> <userId> 
		-> Shares the repository rep_name of localUser with another user userId, 
		the repository rep_name must exist and the userId as well
	myGit <localUser> <serverAddress> [ -p <password> ] -remove <rep_name> <userId>
		-> Removes the sharing done previously, it runs in the same conditions.
		
***STEPS TO PERFORM BEFORE EXECUTING***		
	-- For the program to work how it is supposed to, you must execute some steps before initializing the program
	-- For each client it is necessary execute the command of keytools shown below:
		>keytool -genkeypair -keyalg RSA -keysize 2048 -keystore clientkeystore.dd -alias client
	-- The first password to insert, when executing the command, it is 'qwerty'. 
	Afterwards, you can insert the data you want and some questiosn will be asked (?). The second password is the same.
	-- For the server it is also necessary to execute the command before before the program to create a keystore.
		>keytool -genkeypair -keyalg RSA -keysize 2048 -keystore serverkeystore.dd -alias server
	-- Afterwards, you must follow the sames steps as before for the clients keystore, with the same passwords.
	-- It is also necessary to execute some commands to save some certificates to certain truststores.
	-- To export the auto-signed certificate from the server, you must execute the command below:
		>keytool -exportcert -alias server -file server.cer -keystore serverkeystore.dd
	-- To export the auto-signed certificate from the client, it is necessary to execute the command below:
		>keytool -exportcert -alias client -file client.cer -keystore clientkeystore.dd
	-- To import the certificates to the truststores of server and client, you must execute the following commands:
		>keytool -importcert -alias server -keystore clientkeystore.dd -file server.cer
		>keytool -importcert -alias client -keystore serverkeystore.dd -file client.cer
	-- The files clientkeystore.dd and client.cer must be on the client side. 
	The files serverkeystore.dd and server.cer must be on the server side.
