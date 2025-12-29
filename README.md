# Zero-Trust File Transfer Protocol (ZT-FTP)

## Group Members
- Mohammad Jehad Dahamsheh 0224919
- Omar Jehad Dahamsheh 0223331


## Features Implemented
- Authentication (username/password)(login ansd registration )
- Session key issued after login/register; required for every command
- Commands: UPLOAD, DOWNLOAD, LIST, DELETE
- Role-based authorization: normal vs super
- Robust error handling (invalid command, file not found, permission denied, etc.)
- Security logging (failed login, invalid session key attempts, delete actions, disconnect after 3 bad keys)

## Project Structure
- `ServerMain.java` : starts server on port 4444
- `Server.java` : per-client thread handler
- `Client.java` / `clientMain.java` : client program
- `User.java` : user model
- `users.properties` : stored credentials (username=password,role)
- `server_files/` : server-side storage (per-user folders)
- `client_files /`: put the file you need to upload to the server .

## How to Compile and Run : 
1- run the ServerMain class first 
2- run the client main class (each inctance refer to one user )
3-when you use the upload file ,I recomended you to put the file in the client_files then when the termenal need you to put the name of the file you should put the real path for it (if you use my recomendation you need to put: client_files/FILE NAME...)

# If you need to add user to be log in the system you should put the : username , password , role in this pattern :
# (username=password,role) in the users.properties  file .




NOTE: this project was created by us for  85% - 90% of it ,put we use the google and AI models to help us in write some featshers
like the sisstion key and the pattern of loging in the server side but now we know how it implement and how to use it ..... 
