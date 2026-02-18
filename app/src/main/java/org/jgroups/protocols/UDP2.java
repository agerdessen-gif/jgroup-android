package org.jgroups.protocols;

public class UDP2 extends UDP {
    public void reconnect() throws Exception {

        destroySockets();
        createSockets();

        //ensure we recreate the receivers, otherwise start threads wont do anything
        recreateReceivers();

        startThreads();

        stopDiagnostics();
        startDiagnostics();
    }

    public void recreateReceivers() {
        mcast_receivers=createReceivers(multicast_receiver_threads, mcast_sock, MCAST_NAME);
        ucast_receivers=createReceivers(unicast_receiver_threads, sock, UCAST_NAME);
    }
}
