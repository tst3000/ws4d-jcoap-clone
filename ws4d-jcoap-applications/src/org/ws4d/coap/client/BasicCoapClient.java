package org.ws4d.coap.client;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.ws4d.coap.Constants;
import org.ws4d.coap.connection.BasicCoapChannelManager;
import org.ws4d.coap.interfaces.CoapChannelManager;
import org.ws4d.coap.interfaces.CoapClient;
import org.ws4d.coap.interfaces.CoapClientChannel;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapResponse;
import org.ws4d.coap.messages.CoapMediaType;
import org.ws4d.coap.messages.CoapRequestCode;

/**
 * @author Christian Lerche <christian.lerche@uni-rostock.de>
 */

public class BasicCoapClient implements CoapClient {
    private static final int PORT = Constants.COAP_DEFAULT_PORT;
    static int counter = 0;
    CoapChannelManager channelManager = null;
    CoapClientChannel clientChannel = null;

    public static void main(String[] args) {
        System.out.println("Start CoAP Client");
        CommandLineParser cmdParser = new GnuParser();
        Options options = new Options();
        options.addOption("t", true, "Content type for given resource for PUT/POST");
        options.addOption("e", true, "Include text as payload");
        options.addOption("m", true, "Request method (get|put|post|delete), default is 'get'");
        options.addOption("N", false, "Send NON-confirmable message");
        options.addOption("P", true, "Use proxy (adds Proxy-Uri option)");
        options.addOption("T", true, "Include specified token");
        CommandLine cmd = null;
        try {
            cmd = cmdParser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Unexpected exception: " + e.getMessage() );
            showHelp(options);
        }

        // Make sure we have at least the URI argument
        if (cmd == null || cmd.getArgs().length != 1) {
            showHelp(options);
        }

        BasicCoapClient client = new BasicCoapClient();
        client.channelManager = BasicCoapChannelManager.getInstance();
        client.runTestClient(cmd);
    }

    public static void showHelp(Options options) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("jCoAP-Client URI", "", options,
                    "Examples: \n"
                    + "    jCoAP-Client -m get coap://[::1]/.well-known/core\n");
            System.exit(-1);
    }

    public void runTestClient(CommandLine cmd){
        try {
            String urlOrig = cmd.getArgs()[0];
            // Lame, but easier than doing a new full blown URL parser. TODO: Fix.
            urlOrig = urlOrig.replaceFirst("coap", "http");
            URL url = new URL(urlOrig);
            int port = url.getPort();
            if (port < 0) port = PORT;

            clientChannel = channelManager.connect(this, InetAddress.getByName(url.getHost()), port);
            CoapRequest coapRequest;

            boolean confirm = !cmd.hasOption("N");
            switch (cmd.getOptionValue("m", "get").toLowerCase()) {
                case "put":
                    coapRequest = clientChannel.createRequest(confirm, CoapRequestCode.PUT);
                    break;
                case "post":
                    coapRequest = clientChannel.createRequest(confirm, CoapRequestCode.POST);
                    break;
                case "delete":
                    coapRequest = clientChannel.createRequest(confirm, CoapRequestCode.DELETE);
                    break;
                default:
                    coapRequest = clientChannel.createRequest(confirm, CoapRequestCode.GET);
            }

            if (cmd.hasOption("e")) coapRequest.setPayload(cmd.getOptionValue("e"));
            if (cmd.hasOption("T")) coapRequest.setToken(cmd.getOptionValue("T").getBytes());
            if (cmd.hasOption("P")) coapRequest.setProxyUri(cmd.getOptionValue("P"));
            if (cmd.hasOption("t")) {
                Integer type = Integer.parseInt(cmd.getOptionValue("t"));
                coapRequest.setContentType(CoapMediaType.parse(type));
            }

            coapRequest.setUriHost(url.getHost());
            coapRequest.setUriPort(port);
            coapRequest.setUriPath(url.getPath());
            coapRequest.setUriQuery(url.getQuery());

//			coapRequest.setContentType(CoapMediaType.octet_stream);
//			coapRequest.setToken("ABCD".getBytes());
//			coapRequest.setUriHost("123.123.123.123");
//			coapRequest.setUriPort(1234);
//			coapRequest.setUriPath("/sub1/sub2/sub3/");
//			coapRequest.setUriQuery("a=1&b=2&c=3");
//			coapRequest.setProxyUri("http://proxy.org:1234/proxytest");

			clientChannel.sendMessage(coapRequest);
			System.out.println("Sent Request");
        } catch (UnknownHostException e) {
            Logger.getLogger(BasicCoapClient.class.getName()).log(Level.SEVERE, null, e);
            e.printStackTrace();
        } catch (MalformedURLException ex) {
            Logger.getLogger(BasicCoapClient.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-2);
        }
    }

	@Override
	public void onConnectionFailed(CoapClientChannel channel, boolean notReachable, boolean resetByServer) {
		System.out.println("Connection Failed");
	}

	@Override
	public void onResponse(CoapClientChannel channel, CoapResponse response) {
		System.out.println("Received response");
	}
}
