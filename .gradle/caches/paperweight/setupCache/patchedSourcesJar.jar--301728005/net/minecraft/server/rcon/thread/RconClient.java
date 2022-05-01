package net.minecraft.server.rcon.thread;

import com.mojang.logging.LogUtils;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.rcon.PktUtils;
import org.slf4j.Logger;

public class RconClient extends GenericThread {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SERVERDATA_AUTH = 3;
    private static final int SERVERDATA_EXECCOMMAND = 2;
    private static final int SERVERDATA_RESPONSE_VALUE = 0;
    private static final int SERVERDATA_AUTH_RESPONSE = 2;
    private static final int SERVERDATA_AUTH_FAILURE = -1;
    private boolean authed;
    private final Socket client;
    private final byte[] buf = new byte[1460];
    private final String rconPassword;
    private final ServerInterface serverInterface;

    RconClient(ServerInterface server, String password, Socket socket) {
        super("RCON Client " + socket.getInetAddress());
        this.serverInterface = server;
        this.client = socket;

        try {
            this.client.setSoTimeout(0);
        } catch (Exception var5) {
            this.running = false;
        }

        this.rconPassword = password;
    }

    @Override
    public void run() {
        while(true) {
            try {
                if (!this.running) {
                    return;
                }

                BufferedInputStream bufferedInputStream = new BufferedInputStream(this.client.getInputStream());
                int i = bufferedInputStream.read(this.buf, 0, 1460);
                if (10 <= i) {
                    int j = 0;
                    int k = PktUtils.intFromByteArray(this.buf, 0, i);
                    if (k != i - 4) {
                        return;
                    }

                    j += 4;
                    int l = PktUtils.intFromByteArray(this.buf, j, i);
                    j += 4;
                    int m = PktUtils.intFromByteArray(this.buf, j);
                    j += 4;
                    switch(m) {
                    case 2:
                        if (this.authed) {
                            String string2 = PktUtils.stringFromByteArray(this.buf, j, i);

                            try {
                                this.sendCmdResponse(l, this.serverInterface.runCommand(string2));
                            } catch (Exception var15) {
                                this.sendCmdResponse(l, "Error executing: " + string2 + " (" + var15.getMessage() + ")");
                            }
                            continue;
                        }

                        this.sendAuthFailure();
                        continue;
                    case 3:
                        String string = PktUtils.stringFromByteArray(this.buf, j, i);
                        int var10000 = j + string.length();
                        if (!string.isEmpty() && string.equals(this.rconPassword)) {
                            this.authed = true;
                            this.send(l, 2, "");
                            continue;
                        }

                        this.authed = false;
                        this.sendAuthFailure();
                        continue;
                    default:
                        this.sendCmdResponse(l, String.format("Unknown request %s", Integer.toHexString(m)));
                        continue;
                    }
                }
            } catch (IOException var16) {
                return;
            } catch (Exception var17) {
                LOGGER.error("Exception whilst parsing RCON input", (Throwable)var17);
                return;
            } finally {
                this.closeSocket();
                LOGGER.info("Thread {} shutting down", (Object)this.name);
                this.running = false;
            }

            return;
        }
    }

    private void send(int sessionToken, int responseType, String message) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1248);
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        byte[] bs = message.getBytes(StandardCharsets.UTF_8);
        dataOutputStream.writeInt(Integer.reverseBytes(bs.length + 10));
        dataOutputStream.writeInt(Integer.reverseBytes(sessionToken));
        dataOutputStream.writeInt(Integer.reverseBytes(responseType));
        dataOutputStream.write(bs);
        dataOutputStream.write(0);
        dataOutputStream.write(0);
        this.client.getOutputStream().write(byteArrayOutputStream.toByteArray());
    }

    private void sendAuthFailure() throws IOException {
        this.send(-1, 2, "");
    }

    private void sendCmdResponse(int sessionToken, String message) throws IOException {
        int i = message.length();

        do {
            int j = 4096 <= i ? 4096 : i;
            this.send(sessionToken, 0, message.substring(0, j));
            message = message.substring(j);
            i = message.length();
        } while(0 != i);

    }

    @Override
    public void stop() {
        this.running = false;
        this.closeSocket();
        super.stop();
    }

    private void closeSocket() {
        try {
            this.client.close();
        } catch (IOException var2) {
            LOGGER.warn("Failed to close socket", (Throwable)var2);
        }

    }
}
