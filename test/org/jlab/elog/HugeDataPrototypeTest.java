package org.jlab.elog;

import javax.net.ssl.SSLSocketFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jlab.elog.util.IOUtil;
import org.jlab.elog.util.SecurityUtil;

/**
 *
 * @author ryans
 */
public class HugeDataPrototypeTest {

    static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "error");

        //System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        //System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
        //System.setProperty("sun.security.ssl.allowLegacyHelloMessages", "true");        
    }

    public static void main(String[] args) throws Exception {

        LogEntry entry = new LogEntry("Testing Large File", "TLOG");

        StringBuilder builder = new StringBuilder();

        //Inefficiently create a large string!
        for (int i = 0; i < 39452672; i++) {
            builder.append(0);
        }

        entry.setBody(builder.toString());

        String xml = entry.getXML();

        HttpClient client = new DefaultHttpClient();
        try {
            SSLSocketFactory factory = SecurityUtil.getClientCertSocketFactoryPEM(entry.getDefaultCertificatePath(), false);
            Scheme scheme = new Scheme("https", 443, new org.apache.http.conn.ssl.SSLSocketFactory(factory, null));
            client.getConnectionManager().getSchemeRegistry().register(scheme);
            HttpPut put = new HttpPut(entry.buildHttpPutUrl());
            HttpEntity data = new StringEntity(xml);
            put.setEntity(data);
            put.getParams().setBooleanParameter("http.protocol.expect-continue", true);
            put.setHeader("Content-Type", "application/xml; charset=UTF-8");
            HttpResponse response = client.execute(put);
            HttpEntity entity = response.getEntity();

            System.out.println("------------");
            System.out.println(response.getStatusLine());
            if (entity != null) {
                System.out.println("Response content length: " + entity.getContentLength());
                System.out.println("------------");
                System.out.println(IOUtil.streamToString(entity.getContent(), "UTF-8"));
            }
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
}
