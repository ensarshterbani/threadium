# Threadium Chat System

Threadium is a full-featured, distributed Java chatting application that utilizes Multithreading, Sockets, Blocking Queues, and a sleek JavaFX frontend. It supports persistent messaging across network sessions, native user registration processes (stored securely in binary format), and real-time push-based message streaming.

## How to Run the Program

The application relies on pure Java and JavaFX! It has been packaged into a standard Maven project to easily pull dependencies.

### Running from an IDE (Recommended - IntelliJ IDEA)
1. **Load Project:** Open the `Threadium` folder in IntelliJ IDEA. If prompted, click **"Load Maven Project"** so IntelliJ can automatically install the JavaFX packages.
2. **Start the Server:** Open `src/main/java/com/threadium/chat/server/ChatServer.java` and hit the green **Play** button on the `main` method. The server will bind to port `8080`.
3. **Start the Clients:** Open `src/main/java/com/threadium/chat/client/ChatClientApp.java` and hit **Play**.
    * *Note on playing with friends locally:* When you log in or register via the app, a connection window will explicitly prompt you to choose between `Localhost` or connecting to a custom `IPv4 Address`. If you are hosting a server that friends on the same Wi-Fi want to connect to, they simply select `Other Address (IPv4)` and type in your host computer's IP! 
    * *Note on multiple clients:* By default, IntelliJ prevents running multiple identical application windows. To test multiple users simultaneously on your own computer, click the "Edit Configurations" dropdown next to your play button in the top right, go to "Modify Options", and check **"Allow multiple instances"**. Follow this up by clicking play 2 or 3 times to get several windows at once!

### Running via Terminal (Maven)
You can directly compile and run the UI through the terminal in the root directory:
```bash
mvn clean compile javafx:run
```

## Architecture Overview

The system strictly adheres to the Client/Server Programming paradigm, neatly split into three layers:
- **`chat.server`**: Contains the `ChatServer` that spins up an infinite Thread Pool (`ExecutorService`) to continuously accept incoming user connections. Each room instantiated runs on its own dedicated `Thread` and utilizes natively synced `LinkedBlockingQueue`s to catch fast-moving messages without dropping data. `UserManager` handles binary parsing to permanently store account info!
- **`chat.client`**: Contains the `ServerConnection` daemon thread which spins indefinitely to read packets as asynchronously as possible. `ChatClientApp` then uses `Platform.runLater()` to inject those packets directly into the front-end interface!
- **`chat.common`**: Contains `Message`, the serializable DTO (Data Transfer Object) bridging byte traffic seamlessly between the server and the UI.

## Development Problems & Solutions

While structuring this distributed application, we successfully maneuvered around a couple of unique architectural problems:

1. **JavaFX Module Check Restrictions**
    * *The Problem:* Since Java 11, JavaFX was removed from the standard JDK. Attempting to directly run a class extending `Application` usually violently breaks because the Java runtime demands explicit VM pathing arguments indicating the module libraries.
    * *The Solution:* We initially injected a `MainLauncher` proxy wrapper class to trick the Java launcher into bypassing its module checks. Ultimately, we migrated the legacy project into a tightly coupled **Maven (`pom.xml`)** architecture which natively controls all JavaFX injections without manual configuration.
2. **Spontaneous Connection Dropping (Ghosting)**
    * *The Problem:* Initially, standard chatting systems correlate physical Socket connections with active Room involvement. Thus, if a GUI window abruptly crashed or was closed, the socket died, and the user was painfully kicked out of all their chat logs and the active room context.
    * *The Solution:* We decoupled Socket connections from Room Anchoring. We rewrote the arrays to track Strings (`username`) instead of physical Streams (`ObjectOutputStream`). Consequently, if your connection dies, the server leaves you actively seated in your room inside its memory map. When you reboot your app and login, the server forcefully seizes the GUI, reinserts you into the visual room, and prints every single missed bubble seamlessly using the synchronized room `messageHistory` list!
