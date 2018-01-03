package net.yura.domination.engine.ai;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.yura.domination.engine.Risk;
import net.yura.domination.engine.core.RiskGame;
import net.yura.util.Service;

/**
 * Class AIManager
 * @author Il23
 *
 */
public class AIManager {

    private static int wait=500;
    public static int getWait() {
            return wait;
    }
    public static void setWait(int w) {
            wait = w;
    }



    private final Map<Integer,AI> ais = new HashMap();

    public AIManager() {
        Iterator<Class<AI>> providers = Service.providerClasses(AIManager.class);
        
            try {
                while (providers.hasNext()) {
                System.out.println("out");}
                // each AIManager has its own instances of AI players so the state does not leak
                AI ai = providers.next().newInstance();
                int type = ai.getType();
                if ( ais.get( type ) !=null ) {
                  try{
                   
                    } catch (RuntimeException runtimeException) {
                        System.out.println ("more then 1 ai with same type");
                    }
                }
                ais.put( type , ai );
            
            }
            catch (Exception ex) {
                try {
                } catch (RuntimeException runtimeException) {
                    System.out.println("error");
                }
            }
        }

    public void play(Risk risk) {
            RiskGame game = risk.getGame();
            String output = getOutput(game, game.getCurrentPlayer().getType() );
            try { Thread.sleep(wait); }
            catch(InterruptedException e) {
            System.out.println("error");
            }
            risk.parser(output);
    }

    public String getOutput(RiskGame game,int type) {
            AI usethisAI=ais.get(type);
            if (usethisAI==null) {
                throw new IllegalArgumentException("can not find ai for type "+type);
            }
            usethisAI.setGame(game);
            String output = getStateGame(game,usethisAI);

            return output;
    }
    private String getStateGame(RiskGame game, AI usethisAI) {
        HashMap<Integer, String> gameState = new HashMap<>();
        String output = null;
        gameState.put(RiskGame.STATE_TRADE_CARDS,usethisAI.getTrade());
        gameState.put(RiskGame.STATE_PLACE_ARMIES,usethisAI.getPlaceArmies());
        gameState.put(RiskGame.STATE_ATTACKING,usethisAI.getAttack());
        gameState.put(RiskGame.STATE_ROLLING,usethisAI.getRoll());
        gameState.put(RiskGame.STATE_BATTLE_WON,usethisAI.getBattleWon());
        gameState.put(RiskGame.STATE_FORTIFYING,usethisAI.getTacMove());
        gameState.put(RiskGame.STATE_SELECT_CAPITAL,usethisAI.getCapital());
        gameState.put(RiskGame.STATE_DEFEND_YOURSELF,usethisAI.getAutoDefendString());
        gameState.put(RiskGame.STATE_END_TURN,"endgo");
        gameState.put(RiskGame.STATE_GAME_OVER,"AI error: game is over");
        if(gameState.containsKey(game.getState())) {
            output = gameState.get(game.getState());
        }
        else if(gameState.containsKey(game.getState()) && game.getState() == RiskGame.STATE_GAME_OVER)
            throw new IllegalStateException(gameState.get(game.getState()));
        else {
            throw new IllegalStateException("AI error: unknown state "+ game.getState());
        }
        return output;
    }
    public int getTypeFromCommand(String command) {
        for (AI ai:ais.values()) {
            if (ai.getCommand().equals(command)) {
                return ai.getType();
            }
        }
        throw new IllegalArgumentException("unknown command "+command);
    }

    public String getCommandFromType(int type) {
        for (AI ai:ais.values()) {
            if (ai.getType() == type) {
                return ai.getCommand();
            }
        }
        throw new IllegalArgumentException("unknown type "+type);
    }

    public String[] getAICommands() {
        String[] commands = new String[ais.size()];
        int c=0;
        for (AI ai:ais.values()) {
            commands[c++] = ai.getCommand();
        }
        return commands;
    }
}
