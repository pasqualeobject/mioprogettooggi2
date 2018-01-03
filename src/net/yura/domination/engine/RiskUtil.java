package net.yura.domination.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import net.yura.domination.engine.core.Player;
import net.yura.domination.engine.core.RiskGame;
import net.yura.domination.engine.translation.MapTranslator;

/**
 * RiskUtil Class
 * @author Il23
 *
 */
public class RiskUtil {
    /**
     * these are a Costant of Success and Error
     */
    public static final Object SUCCESS = "SUCCESS";
    /**
     * Costant of Object
     */
    public static final Object ERROR = "ERROR";
    /**
     * costant of Risk Version Url string
     */
    public static final String RISK_VERSION_URL;
    /**
     * Costant of String Risk Lobby
     */
    public static final String RISK_LOBBY_URL;
//	public static final String RISK_POST_URL; // look in Grasshopper.jar now
    /**
     * Costant of String Game Name
     */
    public static final String GAME_NAME;
    /**
     * Costant of String Risk Version
     */
    public static final String RISK_VERSION;
//	private static final String DEFAULT_MAP;

    private static final Logger logger = Logger.getLogger(RiskUtil.class.getName());
    /**
     * RiskIO streamOpener
     */
    public static RiskIO streamOpener = 0;

    static {

        Properties settings = new Properties();

        try {
            settings.load(RiskUtil.class.getResourceAsStream("settings.ini"));
        }
        catch (IOException ex) {
            System.out.println("can not find settings.ini file!");
        }

        RISK_VERSION_URL = settings.getProperty("VERSION_URL");
        RISK_LOBBY_URL = settings.getProperty("LOBBY_URL");
//		RISK_POST_URL = settings.getProperty("POST_URL");
        GAME_NAME = settings.getProperty("name");
        //DEFAULT_MAP = settings.getProperty("defaultmap");
        RISK_VERSION = settings.getProperty("version");

        String dmap = settings.getProperty("defaultmap");
        String dcards = settings.getProperty("defaultcards");

        RiskGame.setDefaultMapAndCards( dmap , dcards );

    }

    public static InputStream openMapStream(String a) throws IOException {
        return streamOpener.openMapStream(a);
    }

    public static InputStream openStream(String a) throws IOException {
        return streamOpener.openStream(a);
    }
<<<<<<< HEAD

    public static ResourceBundle getResourceBundle(Class c,String n,Locale l) {
        return streamOpener.getResourceBundle(c, n, l);
    }

    public static void openURL(URL url) throws Exception {
        streamOpener.openURL(url);
    }

    public static void openDocs(String docs) throws Exception {
        streamOpener.openDocs(docs);
    }
    public static void saveFile(String file, RiskGame aThis) throws Exception {
        streamOpener.saveGameFile(file, aThis);
    }
    public static InputStream getLoadFileInputStream(String file) throws Exception {
        return streamOpener.loadGameFile(file);
    }

=======

    public static ResourceBundle getResourceBundle(Class c,String n,Locale l) {
        return streamOpener.getResourceBundle(c, n, l);
    }

    public static void openURL(URL url) throws Exception {
        streamOpener.openURL(url);
    }

    public static void openDocs(String docs) throws Exception {
        streamOpener.openDocs(docs);
    }
    public static void saveFile(String file, RiskGame aThis) throws Exception {
        streamOpener.saveGameFile(file, aThis);
    }
    public static InputStream getLoadFileInputStream(String file) throws Exception {
        return streamOpener.loadGameFile(file);
    }

>>>>>>> 2290264b552aa68c481db9320ffc4f67e1ba5c6f
    /**
     * option string looks like this:
     *
     *   0
     *   2
     *   2
     *   choosemap luca.map
     *   startgame domination increasing
     */
    public static String GameString(){
        int gameMode = 0;
        switch(gameMode) {
            case RiskGame.MODE_DOMINATION: type = "domination"; break;
            case RiskGame.MODE_CAPITAL: type = "capital"; break;
            case RiskGame.MODE_SECRET_MISSION: type = "mission"; break;
        }
        int cardsMode = 0;

        switch(cardsMode) {
            case RiskGame.CARD_INCREASING_SET: type += " increasing"; break;
            case RiskGame.CARD_FIXED_SET: type += " fixed"; break;
            case RiskGame.CARD_ITALIANLIKE_SET: type += " italianlike"; break;
        }
        return null;

    }
    public static String createGameString(int easyAI, int averageAI, int hardAI, int gameMode, int cardsMode, boolean AutoPlaceAll, boolean recycle, String mapFile) {

        String players = averageAI + "\n" + easyAI + "\n" + hardAI + "\n";

        String type="";

        if ( AutoPlaceAll ) type += " autoplaceall";
        if ( recycle ) type += " recycle";

        return players+ "choosemap "+mapFile +"\nstartgame " + type;
    }
    public static String getMapNameFromLobbyStartGameOption(String options) {
        String[] lines = options.split( RiskUtil.quote("\n") );
        String choosemap = lines[3];
        return choosemap.substring( "choosemap ".length() ).intern();
    }

    /**
     * @see #createGameString(int, int, int, int, int, boolean, boolean, java.lang.String)
     * @see net.yura.domination.lobby.server.ServerGameRisk#startGame(java.lang.String, java.lang.String[])
     */
    public static String getGameDescriptionFromLobbyStartGameOption(String options) {
        String[] lines = options.split( RiskUtil.quote("\n") );
        int aiTotal=0;
        for (int c=0;c<3;c++) {
            aiTotal = aiTotal + Integer.parseInt(lines[c]);
        }
        String aiInfo;
        if (aiTotal == 0) {
            aiInfo = "0";
        }
        else {
            // easy,average,hard for historic reasons, they are stored as 'average \n easy \n hard'
            aiInfo = lines[1]+","+lines[0]+","+lines[2];
        }
        return "AI:"+aiInfo+" "+lines[4].substring( "startgame ".length() );
    }

    public static void printStackTrace(Throwable ex) {
        logger.log(Level.WARNING, null, ex);
    }

    public static void donate() throws Exception {
        openURL(new URL("http://domination.sourceforge.net/donate.shtml"));
    }

    public static void donatePayPal() throws Exception {
        openURL(new URL("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=yura%40yura%2enet&item_name="+GAME_NAME+"%20Donation&no_shipping=0&no_note=1&tax=0&currency_code=GBP&lc=GB&bn=PP%2dDonationsBF&charset=UTF%2d8"));
    }

    public static Properties getPlayerSettings(final Risk risk,Class uiclass) {
        Preferences prefs=null;
        try {
            prefs = Preferences.userNodeForPackage( uiclass );
        }
        catch(Throwable th) {
            System.out.println("error");
        } // security
        final Preferences theprefs = prefs;
        return new Properties() {
            public String getProperty(String key) {
                String value = risk.getRiskConfig(key);
                if (theprefs!=null) {
                    value = theprefs.get(key, value);
                }
                return value;
            }
        };
    }

    public static void loadPlayers(Risk risk,Class uiclass) {
        Properties playerSettings = getPlayerSettings(risk, uiclass);
        for (int cc=1;cc<=RiskGame.MAX_PLAYERS;cc++) {
            String name = playerSettings.getProperty("default.player"+cc+".name");
            String color = playerSettings.getProperty("default.player"+cc+".color");
            String type = playerSettings.getProperty("default.player"+cc+".type");
            if (!"".equals(name)&&!"".equals(color)&&!"".equals(type)) {
                risk.parser("newplayer " + type+" "+ color+" "+ name );
            }
        }
    }
<<<<<<< HEAD

    public static void savePlayers(Risk risk,Class uiclass) {

=======

    public static void savePlayers(Risk risk,Class uiclass) {

>>>>>>> 2290264b552aa68c481db9320ffc4f67e1ba5c6f
        Preferences prefs=null;
        try {
            prefs = Preferences.userNodeForPackage( uiclass );
        }
        catch(Throwable th) {
            System.out.println("error");
        } // security

        if (prefs!=null) {

            List players = risk.getGame().getPlayers();

            for (int cc=1;cc<=RiskGame.MAX_PLAYERS;cc++) {
                String nameKey = "default.player"+cc+".name";
                String colorKey = "default.player"+cc+".color";
                String typeKey = "default.player"+cc+".type";

                String name = "";
                String color = "";
                String type = "";

                Player player = (cc<=players.size())?(Player)players.get(cc-1):null;

                if (player!=null) {
                    name = player.getName();
                    color = ColorUtil.getStringForColor( player.getColor() );
                    type = risk.getType( player.getType() );
                }
                prefs.put(nameKey, name);
                prefs.put(colorKey, color);
                prefs.put(typeKey, type);

            }

            // on android this does not work, god knows why
            // whats the point of including a class if its
            // most simple and basic operation does not work?
            try {
                prefs.flush();
            }
            catch(Exception ex) {
                logger.log(Level.INFO, "can not flush prefs", ex);
            }

        }
    }

    public static void savePlayers(List players,Class uiclass) {

        Preferences prefs=null;
        try {
            prefs = Preferences.userNodeForPackage( uiclass );
        }
        catch(Throwable th) {
            System.out.println("error");
        } // security

        if (prefs!=null) {

            for (int cc=1;cc<=RiskGame.MAX_PLAYERS;cc++) {
                String nameKey = "default.player"+cc+".name";
                String colorKey = "default.player"+cc+".color";
                String typeKey = "default.player"+cc+".type";

                String name = "";
                String color = "";
                String type = "";

                String[] player = (cc<=players.size())?(String[])players.get(cc-1):null;

                if (player!=null) {
                    name = player[0];
                    color = player[1];
                    type = player[2];
                }
                prefs.put(nameKey, name);
                prefs.put(colorKey, color);
                prefs.put(typeKey, type);

            }

            // on android this does not work, god knows why
            // whats the point of including a class if its
            // most simple and basic operation does not work?
            try {
                prefs.flush();
            }
            catch(Exception ex) {
                logger.log(Level.INFO, "can not flush prefs", ex);
            }

        }
    }

    public static BufferedReader readMap(InputStream in) throws IOException {

        PushbackInputStream pushback = new PushbackInputStream(in,3);

        int first = pushback.read();
        if (first == 0xEF) {
            int second = pushback.read();
            if (second == 0xBB) {
                int third = pushback.read();
                if (third == 0xBF) {
                    return new BufferedReader(new InputStreamReader( pushback, "UTF-8" ) );
                }
                pushback.unread(third);
            }
            pushback.unread(second);
        }
        pushback.unread(first);

        return new BufferedReader(new InputStreamReader( pushback, "ISO-8859-1" ) );
    }

    /**
     * gets the info for a map or cards file
     * in the case of map files it will get the "name" "crd" "prv" "pic" "map" and any "comment" and number of "countries"
     * and for cards it will have a "missions" that will contain the String[] of all the missions
     */
    public static java.util.Map RiskUtil1(){
        BufferedReader bufferin=null;
        String fileName = null;
        try {
            bufferin= RiskUtil.readMap(RiskUtil.openMapStream(fileName));
        } catch (IOException ex) {
            Logger.getLogger(RiskUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        Vector misss=null;
        boolean cards = false;
        if (cards) {
            MapTranslator.setCards( fileName );
            misss = new Vector();
        }
        return null;
    }
    public static java.util.Map RiskUtil2(){
        try {
            BufferedReader bufferin=null;
            String input = bufferin.readLine();
            if (input.equals("")) {
                // do nothing
                //System.out.print("Nothing\n"); // testing
            }
            else if (input.charAt(0)==';') {
                Hashtable info = new Hashtable();
                String comment = (String)info.get("comment");
                String com = input.substring(1).trim();
                if (comment==null) {
                    comment = com;
                }
                else {
                    comment = comment +"\n"+com;
                }

                info.put("comment", comment);
            }

        } catch (IOException ex) {
            Logger.getLogger(RiskUtil.class.getName()).log(Level.SEVERE, null, ex);
<<<<<<< HEAD
        }
        return null;
    }
    public static java.util.Map RiskUtil3(){
        BufferedReader bufferin=null;
        String input = null;
        try {
            input = bufferin.readLine();
        } catch (IOException ex) {
            Logger.getLogger(RiskUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
=======
        }
        return null;
    }
    public static java.util.Map RiskUtil3(){
        BufferedReader bufferin=null;
        String input = null;
        try {
            input = bufferin.readLine();
        } catch (IOException ex) {
            Logger.getLogger(RiskUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
>>>>>>> 2290264b552aa68c481db9320ffc4f67e1ba5c6f
        if(input.charAt(0)!='[' || input.charAt( input.length()-1 )!=']') {
        } else {
            String mode = "newsection";
        }
        Object mode = null;

        if ("files".equals(mode)) {

            int space = input.indexOf(' ');

            String fm = input.substring(0,space);
            String val = input.substring(space+1);
            Hashtable info = new Hashtable();
            info.put( fm , val);

        }
        return null;
    }
    public static java.util.Map RiskUtil4(){
        Object mode = null;
        if ("borders".equals(mode)) {
            // we dont care about anything in or after the borders section

        }
        else if ("countries".equals(mode)) {
            Hashtable info = new Hashtable();
            String input = null;
            info.put("countries", Integer.parseInt(input.substring(0,input.indexOf(' '))));
        }
        else if ("missions".equals(mode)) {
            String input = null;

            StringTokenizer st = new StringTokenizer(input);

            String description=MapTranslator.getTranslatedMissionName(st.nextToken()+"-"+st.nextToken()+"-"+st.nextToken()+"-"+st.nextToken()+"-"+st.nextToken()+"-"+st.nextToken());

            while (description==null) {

                StringBuffer d = new StringBuffer();
<<<<<<< HEAD

                while (st.hasMoreElements()) {

                    d.append( st.nextToken() );
                    d.append( " " );
                }

                description = d.toString();
                break;

            }
            Vector misss=null;
            misss.add( description );

=======

                while (st.hasMoreElements()) {

                    d.append( st.nextToken() );
                    d.append( " " );
                }

                description = d.toString();
                break;

            }
            Vector misss=null;
            misss.add( description );

>>>>>>> 2290264b552aa68c481db9320ffc4f67e1ba5c6f
        }
        return null;
    }
    public static java.util.Map RiskUtil5(){
        Object mode = null;
        if ("newsection".equals(mode)) {
            BufferedReader bufferin=null;
            String input = null;
            try {
                input = bufferin.readLine();
            } catch (IOException ex) {
                Logger.getLogger(RiskUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
            mode = input.substring(1, input.length()-1); // set mode to the name of the section

        }
        else if (mode == null) {
            BufferedReader bufferin=null;
            String input = null;
            try {
                input = bufferin.readLine();
            } catch (IOException ex) {
                Logger.getLogger(RiskUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
            int ind = input.indexOf(' ');
            while (ind>0) {
                Hashtable info = new Hashtable();
                info.put( input.substring(0,input.indexOf(' ')) , input.substring(input.indexOf(' ')+1) );



            }
        }
        return null;
    }
    public static java.util.Map RiskUtil6(){
        boolean cards = false;
        Hashtable info = new Hashtable();
        if (cards) {

            Vector misss=null;
            info.put("missions", (String[])misss.toArray(new String[misss.size()]) );

        }
        return null;
    }
    public static java.util.Map loadInfo(String fileName,boolean cards) {

        Hashtable info = new Hashtable();

        for (int c=0;true;c++) {

            BufferedReader bufferin=null;

            try {

                RiskUtil1();
                bufferin= RiskUtil.readMap(RiskUtil.openMapStream(fileName));
                Vector misss=null;



                String input = bufferin.readLine();
                String mode = null;

                while(input != null) {


                    RiskUtil2();{

                        RiskUtil3();
                        RiskUtil4();
                        RiskUtil5();
                        // if "continents" or "cards" then just dont do anything in those sections

                    }

                    input = bufferin.readLine(); // get next line
                }

                RiskUtil6();

                break;
            }
            catch(IOException ex) {
                System.out.println("Error trying to load: "+fileName);
                //RiskUtil.printStackTrace(ex);
                if (c < 5) { // retry
                    try { Thread.sleep(1000); } catch(InterruptedException ex2) {
                        System.out.println("error");
                    }
                }
                else { // give up
                    break;
                }
            }
            finally {
                if (bufferin!=null) {
                    try { bufferin.close(); } catch(Exception ex2) {
                        System.out.println("error");
                    }
                }
            }
        }

        return info;

    }

    public static void saveGameLog(File logFile, RiskGame game) throws IOException {
        FileWriter fileout = new FileWriter(logFile);
        BufferedWriter buffer = new BufferedWriter(fileout);
        PrintWriter printer = new PrintWriter(buffer);
        List commands = game.getCommands();
        int size = commands.size();
        for (int line = 0; line < size; line++) {
            printer.println(commands.get(line));
        }
        printer.close();
    }

    public static OutputStream getOutputStream(File dir,String fileName) throws Exception {
        File outFile = new File(dir,fileName);
        // as this could be dir=.../maps fileName=preview/file.jpg
        // we need to make sure the preview dir exists, and if it does not, we must make it
        File parent = outFile.getParentFile();
        OutputStream oS = null;
        if (!parent.isDirectory() && !parent.mkdirs()) { // if it does not exist and i cant make it
            try {
            
<<<<<<< HEAD
            } catch{
            	
            }
            finally (RuntimeException runtimeException) {
                oS = new FileOutputStream(outFile);
                System.err.println("can not create dir " + parent);
            }
           
=======
            } catch (RuntimeException runtimeException) {
                oS = new FileOutputStream(outFile);
                System.err.println("can not create dir " + parent);
            }
>>>>>>> 2290264b552aa68c481db9320ffc4f67e1ba5c6f
        }

        return oS;
    }

    public static void rename(File oldFile,File newFile) {
        if (newFile.exists() && !newFile.delete()) {
            try {
            
            } catch (RuntimeException runtimeException) {
                System.err.println("can not del dest file: " + newFile);
            }
        }
        if (!oldFile.renameTo(newFile)) {
            try {
                copy(oldFile, newFile);
                if (!oldFile.delete()) {
                    // this is not so bad, but still very strange
                    System.err.println("can not del source file: "+oldFile);
                }
            }
            catch(Exception ex) {
                try {
                   
                } catch (RuntimeException runtimeException) {
                    System.err.println("rename failed: from: " + oldFile + " to: " + newFile);
                }
            }
        }
    }



    public static Vector asVector(java.util.List list) {
        return list instanceof Vector?(Vector)list:new Vector(list);
    }

    public static Hashtable asHashtable(java.util.Map map) {
        return map instanceof Hashtable?(Hashtable)map:new Hashtable(map);
    }


    public static String replaceAll(String string, String notregex, String replacement) {
        return string.replaceAll( quote(notregex) , quoteReplacement(replacement));
    }

    /**
     * @see java.util.regex.Pattern#quote(java.lang.String)
     */
    public static String quote(String s) {
        int slashEIndex = s.indexOf("\\E");
        if (slashEIndex == -1)
            return "\\Q" + s + "\\E";

        StringBuilder sb = new StringBuilder(s.length() * 2);
        sb.append("\\Q");
        slashEIndex = 0;
        int current = 0;
        int val = (slashEIndex = s.indexOf("\\E", current));
        while (val != -1) {
            sb.append(s.substring(current, slashEIndex));
            current = slashEIndex + 2;
            sb.append("\\E\\\\E\\Q");
        }
        sb.append(s.substring(current, s.length()));
        sb.append("\\E");
        return sb.toString();
    }

    /**
     * @see java.util.regex.Matcher#quoteReplacement(java.lang.String)
     */
    public static String quoteReplacement(String s) {
        if ((s.indexOf('\\') == -1) && (s.indexOf('$') == -1))
            return s;
        StringBuffer sb = new StringBuffer();
        int leng = s.length();
        for (int i=0; i<leng; i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                sb.append('\\'); sb.append('\\');
            } else if (c == '$') {
                sb.append('\\'); sb.append('$');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }





    public static void copy(File src, File dest) throws IOException {

        if(src.isDirectory()){

            //if directory not exists, create it
            if(!dest.exists()){
                dest.mkdir();
                System.out.println("Directory copied from "
                        + src + "  to " + dest);
            }

            //list all the directory contents
            String files[] = src.list();
            int leng = files.length;
            for (int c=0;c<leng;c++) {
                //construct the src and dest file structure
                File srcFile = new File(src, files[c]);
                File destFile = new File(dest, files[c]);
                //recursive copy
                copy(srcFile,destFile);
            }

        }else{
            //if file, then copy it
            //Use bytes stream to support all file types
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];

            int length;
            //copy the file content in bytes
            int buff = (length = in.read(buffer));
            while (buff > 0){
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
            System.out.println("File copied from " + src + " to " + dest);
        }
    }

    public static String getAtLeastOne(StringTokenizer stringT) {
        StringBuilder text = new StringBuilder(stringT.nextToken());
        while ( stringT.hasMoreTokens() ) {
            text.append(' ');
            text.append(stringT.nextToken());
        }
        return text.toString();
    }
}
