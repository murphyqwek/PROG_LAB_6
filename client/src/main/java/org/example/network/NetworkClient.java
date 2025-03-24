package org.example.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.base.response.ClientCommandRequest;
import org.example.base.response.ServerResponse;
import org.example.base.response.ServerResponseType;
import org.example.exception.CouldnotConnectException;
import org.example.exception.InvalidPortException;
import org.example.exception.SerializationException;
import org.example.exception.ServerErrorResponseExcpetion;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.TimeoutException;

/**
 * NetworkClient - класс для общения с сервером.
 *
 * @version 1.0
 */

public class NetworkClient {
    private final Logger logger = LogManager.getLogger();
    private final InetSocketAddress serverAddress;
    private final DatagramChannel channel;

    /**
     * Конструктор класса
     * @param ip адрес сервера
     * @param port порт подключения
     * @throws CouldnotConnectException если не удалось подключиться к серверу
     */
    public NetworkClient(String ip, int port) throws CouldnotConnectException {
        this.serverAddress = new InetSocketAddress(ip, port);

        try {
            this.channel = ClientConnection.connect(this.serverAddress);
        } catch (SocketException e) {
            throw new CouldnotConnectException(e.getMessage());
        } catch (InvalidPortException e) {
            throw new CouldnotConnectException("Неверный порт");
        }
    }

    public void sendUserCommand(ClientCommandRequest clientCommandRequest) {
        NetworkSender.sendUserCommand(clientCommandRequest, channel);
    }

    /**
     * Метод для получения ответа сервера
     */
    public ServerResponse getServerResponse() throws ServerErrorResponseExcpetion {
        try {
            var buffer = NetworkReceiver.receiveBuffer(this.channel, 500, 10);

            ByteArrayInputStream bais = new ByteArrayInputStream(buffer.array());
            ObjectInputStream ois = new ObjectInputStream(bais);

            ServerResponse serverResponse = (ServerResponse) ois.readObject();

            if(serverResponse.getType() == ServerResponseType.ERROR) {
                logger.warn("Ответ сервера содержит ошибку");
                logger.warn(serverResponse.toString());
                throw new ServerErrorResponseExcpetion(serverResponse.getMessage(), false);
            }


            logger.info(serverResponse.toString());
            return serverResponse;
        }
        catch (IOException e) {
            throw new ServerErrorResponseExcpetion("Не удалось получить ответ от сервера", true);
        }
        catch (InterruptedException ex) {
            throw new ServerErrorResponseExcpetion(ex.getMessage(), true);
        }
        catch (TimeoutException ex) {
            throw new ServerErrorResponseExcpetion("Timeout - сервер не отвечает", true);
        }
        catch (ClassNotFoundException ex) {
            throw new ServerErrorResponseExcpetion("Сервер вернул неожиданный ответ", true);
        }
    }
}
