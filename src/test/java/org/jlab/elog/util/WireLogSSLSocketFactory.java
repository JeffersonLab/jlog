package org.jlab.elog.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * This is a utility class useful for debugging.  You can wrap an 
 * SSLSocketFactory to enable logging of the messages written to the peer.  This
 * class is not for use in production and is known to cause Socket connection
 * issues (but usually after it has logged a fair amount of data such as HTTP 
 * headers).
 * 
 * @author ryans
 */
public class WireLogSSLSocketFactory extends SSLSocketFactory {

    private SSLSocketFactory delegate;
    
    public WireLogSSLSocketFactory(SSLSocketFactory delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket socket, String string, int i, boolean bln) throws IOException {
        return new WireLogSocket((SSLSocket)delegate.createSocket(socket, string, i, bln));
    }

    @Override
    public Socket createSocket(String string, int i) throws IOException, UnknownHostException {
        return new WireLogSocket((SSLSocket)delegate.createSocket(string, i));
    }

    @Override
    public Socket createSocket(String string, int i, InetAddress ia, int i1) throws IOException, UnknownHostException {
        return new WireLogSocket((SSLSocket)delegate.createSocket(string, i, ia, i1));
    }

    @Override
    public Socket createSocket(InetAddress ia, int i) throws IOException {
        return new WireLogSocket((SSLSocket)delegate.createSocket(ia, i));
    }

    @Override
    public Socket createSocket(InetAddress ia, int i, InetAddress ia1, int i1) throws IOException {
        return new WireLogSocket((SSLSocket)delegate.createSocket(ia, i, ia1, i1));
    }
}

class WireLogSocket extends SSLSocket {

    private SSLSocket delegate;
    
    public WireLogSocket(SSLSocket delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        delegate.connect(endpoint);
    }
    
    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        delegate.connect(endpoint, timeout);
    }
    
    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }
    
    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
    
    @Override
    public void close() throws IOException {
        delegate.close();
    }
    
    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        delegate.bind(bindpoint);
    }
    
    @Override
    public SocketChannel getChannel() {
        return delegate.getChannel();
    }
    
    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }
    
    @Override
    public boolean isBound() {
        return delegate.isBound();
    }
    
    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }
    
    @Override
    public boolean isInputShutdown() {
        return delegate.isInputShutdown();
    }
    
    @Override
    public boolean isOutputShutdown() {
        return delegate.isOutputShutdown();
    }
    
    @Override
    public InetAddress getInetAddress() {
        return delegate.getInetAddress();
    }
    
    @Override
    public InetAddress getLocalAddress() {
        return delegate.getLocalAddress();
    }
    
    @Override
    public boolean getKeepAlive() throws SocketException {
        return delegate.getKeepAlive();
    }
    
    @Override
    public OutputStream getOutputStream() throws IOException {
        return new LoggingOutputStream(delegate.getOutputStream());
    }
    
    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return delegate.getEnabledCipherSuites();
    }

    @Override
    public void setEnabledCipherSuites(String[] strings) {
        delegate.setEnabledCipherSuites(strings);
    }

    @Override
    public String[] getSupportedProtocols() {
        return delegate.getSupportedProtocols();
    }

    @Override
    public String[] getEnabledProtocols() {
        return delegate.getEnabledProtocols();
    }

    @Override
    public void setEnabledProtocols(String[] strings) {
        delegate.setEnabledProtocols(strings);
    }

    @Override
    public SSLSession getSession() {
        return delegate.getSession();
    }

    @Override
    public void addHandshakeCompletedListener(HandshakeCompletedListener hl) {
        delegate.addHandshakeCompletedListener(hl);
    }

    @Override
    public void removeHandshakeCompletedListener(HandshakeCompletedListener hl) {
        delegate.removeHandshakeCompletedListener(hl);
    }

    @Override
    public void startHandshake() throws IOException {
        delegate.startHandshake();
    }

    @Override
    public void setUseClientMode(boolean bln) {
        delegate.setUseClientMode(bln);
    }

    @Override
    public boolean getUseClientMode() {
        return delegate.getUseClientMode();
    }

    @Override
    public void setNeedClientAuth(boolean bln) {
        delegate.setNeedClientAuth(bln);
    }

    @Override
    public boolean getNeedClientAuth() {
        return delegate.getNeedClientAuth();
    }

    @Override
    public void setWantClientAuth(boolean bln) {
        delegate.setWantClientAuth(bln);
    }

    @Override
    public boolean getWantClientAuth() {
        return delegate.getWantClientAuth();
    }

    @Override
    public void setEnableSessionCreation(boolean bln) {
        delegate.setEnableSessionCreation(bln);
    }

    @Override
    public boolean getEnableSessionCreation() {
        return delegate.getEnableSessionCreation();
    }
}

class LoggingOutputStream extends FilterOutputStream {
    private static final Logger logger = Logger.getLogger(
            LoggingOutputStream.class.getName());    
    private static final int MAX_LOG_MESSAGE_BYTES = 1024;
    private static final String ENCODING = "UTF-8";
    private static final Level level = Level.INFO;
    
    public LoggingOutputStream(OutputStream out) {
        super(out);
    }
    
    @Override
    public void write(byte[] b) throws IOException {
        logger.log(level, "write(byte[]): {0}", new String(b, ENCODING));
        out.write(b);
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if(len > MAX_LOG_MESSAGE_BYTES) {
            logger.log(level, "write(byte[], int, int) [truncated]: {0}", new String(b, off, MAX_LOG_MESSAGE_BYTES, ENCODING));
        } else {
            logger.log(level, "write(byte[], int, int): {0}", new String(b, off, len, ENCODING));
        }
        out.write(b, off, len);
    }
    
    @Override
    public void write(int b) throws IOException {
        // This could be unintelligable if byte is part of multi-byte character
        logger.log(level, "write(int): {0}", new String(new byte[] {(byte)(b & 0xFF)}, ENCODING));
        out.write(b);
    }
}
