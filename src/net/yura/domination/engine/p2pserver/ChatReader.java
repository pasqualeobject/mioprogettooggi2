// Yura Mamyrin, Group D

package net.yura.domination.engine.p2pserver;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * <p> Chat Reader </p>
 * @author Yura Mamyrin
 */

/**
 * the chatReader thread reads incoming socket data and puts it
 * @author Il23
 *
 */
public class ChatReader extends Thread{
    BufferedReader mySocketInput;
    int myIndex;
    ChatArea myChatArea;

    ChatReader(BufferedReader in,  ChatArea cArea, int index) {
        super("ChatReaderThread");
        mySocketInput = in;
        myIndex = index;
        myChatArea = cArea;
    }

    public void run() {

        String inputLine;

        try {
            String inp = (inputLine = mySocketInput.readLine());
            while (inp != null) {

                myChatArea.putString(myIndex, inputLine);

            }
        }
        catch (IOException e) {
            System.out.println("error");
            //System.out.println("ChatReader IOException: "+
            //    e.getMessage());
            //RiskUtil.printStackTrace(e);

        }

        myChatArea.imDead(myIndex);
        //System.out.println("ChatReader Terminating: " + myIndex);
    }
}
