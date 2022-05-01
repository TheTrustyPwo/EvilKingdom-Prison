package net.minecraft.util;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class HttpUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ListeningExecutorService DOWNLOAD_EXECUTOR = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool((new ThreadFactoryBuilder()).setDaemon(true).setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER)).setNameFormat("Downloader %d").build()));

    private HttpUtil() {
    }

    public static String buildQuery(Map<String, Object> query) {
        StringBuilder stringBuilder = new StringBuilder();

        for(Entry<String, Object> entry : query.entrySet()) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append('&');
            }

            try {
                stringBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            } catch (UnsupportedEncodingException var6) {
                var6.printStackTrace();
            }

            if (entry.getValue() != null) {
                stringBuilder.append('=');

                try {
                    stringBuilder.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
                } catch (UnsupportedEncodingException var5) {
                    var5.printStackTrace();
                }
            }
        }

        return stringBuilder.toString();
    }

    public static String performPost(URL url, Map<String, Object> query, boolean ignoreError, @Nullable Proxy proxy) {
        return performPost(url, buildQuery(query), ignoreError, proxy);
    }

    private static String performPost(URL url, String content, boolean ignoreError, @Nullable Proxy proxy) {
        try {
            if (proxy == null) {
                proxy = Proxy.NO_PROXY;
            }

            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection(proxy);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpURLConnection.setRequestProperty("Content-Length", "" + content.getBytes().length);
            httpURLConnection.setRequestProperty("Content-Language", "en-US");
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
            dataOutputStream.writeBytes(content);
            dataOutputStream.flush();
            dataOutputStream.close();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();

            String string;
            while((string = bufferedReader.readLine()) != null) {
                stringBuilder.append(string);
                stringBuilder.append('\r');
            }

            bufferedReader.close();
            return stringBuilder.toString();
        } catch (Exception var9) {
            if (!ignoreError) {
                LOGGER.error("Could not post to {}", url, var9);
            }

            return "";
        }
    }

    public static CompletableFuture<?> downloadTo(File file, String url, Map<String, String> headers, int maxFileSize, @Nullable ProgressListener progressListener, Proxy proxy) {
        return CompletableFuture.supplyAsync(() -> {
            HttpURLConnection httpURLConnection = null;
            InputStream inputStream = null;
            OutputStream outputStream = null;
            if (progressListener != null) {
                progressListener.progressStart(new TranslatableComponent("resourcepack.downloading"));
                progressListener.progressStage(new TranslatableComponent("resourcepack.requesting"));
            }

            try {
                try {
                    byte[] bs = new byte[4096];
                    URL uRL = new URL(url);
                    httpURLConnection = (HttpURLConnection)uRL.openConnection(proxy);
                    httpURLConnection.setInstanceFollowRedirects(true);
                    float f = 0.0F;
                    float g = (float)headers.entrySet().size();

                    for(Entry<String, String> entry : headers.entrySet()) {
                        httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
                        if (progressListener != null) {
                            progressListener.progressStagePercentage((int)(++f / g * 100.0F));
                        }
                    }

                    inputStream = httpURLConnection.getInputStream();
                    g = (float)httpURLConnection.getContentLength();
                    int j = httpURLConnection.getContentLength();
                    if (progressListener != null) {
                        progressListener.progressStage(new TranslatableComponent("resourcepack.progress", String.format(Locale.ROOT, "%.2f", g / 1000.0F / 1000.0F)));
                    }

                    if (file.exists()) {
                        long l = file.length();
                        if (l == (long)j) {
                            if (progressListener != null) {
                                progressListener.stop();
                            }

                            return null;
                        }

                        LOGGER.warn("Deleting {} as it does not match what we currently have ({} vs our {}).", file, j, l);
                        FileUtils.deleteQuietly(file);
                    } else if (file.getParentFile() != null) {
                        file.getParentFile().mkdirs();
                    }

                    outputStream = new DataOutputStream(new FileOutputStream(file));
                    if (maxFileSize > 0 && g > (float)maxFileSize) {
                        if (progressListener != null) {
                            progressListener.stop();
                        }

                        throw new IOException("Filesize is bigger than maximum allowed (file is " + f + ", limit is " + maxFileSize + ")");
                    }

                    int k;
                    while((k = inputStream.read(bs)) >= 0) {
                        f += (float)k;
                        if (progressListener != null) {
                            progressListener.progressStagePercentage((int)(f / g * 100.0F));
                        }

                        if (maxFileSize > 0 && f > (float)maxFileSize) {
                            if (progressListener != null) {
                                progressListener.stop();
                            }

                            throw new IOException("Filesize was bigger than maximum allowed (got >= " + f + ", limit was " + maxFileSize + ")");
                        }

                        if (Thread.interrupted()) {
                            LOGGER.error("INTERRUPTED");
                            if (progressListener != null) {
                                progressListener.stop();
                            }

                            return null;
                        }

                        outputStream.write(bs, 0, k);
                    }

                    if (progressListener != null) {
                        progressListener.stop();
                        return null;
                    }
                } catch (Throwable var22) {
                    var22.printStackTrace();
                    if (httpURLConnection != null) {
                        InputStream inputStream2 = httpURLConnection.getErrorStream();

                        try {
                            LOGGER.error(IOUtils.toString(inputStream2));
                        } catch (IOException var21) {
                            var21.printStackTrace();
                        }
                    }

                    if (progressListener != null) {
                        progressListener.stop();
                        return null;
                    }
                }

                return null;
            } finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }, DOWNLOAD_EXECUTOR);
    }

    public static int getAvailablePort() {
        try {
            ServerSocket serverSocket = new ServerSocket(0);

            int var1;
            try {
                var1 = serverSocket.getLocalPort();
            } catch (Throwable var4) {
                try {
                    serverSocket.close();
                } catch (Throwable var3) {
                    var4.addSuppressed(var3);
                }

                throw var4;
            }

            serverSocket.close();
            return var1;
        } catch (IOException var5) {
            return 25564;
        }
    }
}
