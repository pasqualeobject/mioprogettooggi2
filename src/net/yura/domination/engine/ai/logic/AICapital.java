//  Group D

package net.yura.domination.engine.ai.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.yura.domination.engine.core.Continent;
import net.yura.domination.engine.core.Country;
import net.yura.domination.engine.core.Player;

/**
 * @author Steven Hawkins
 */
public class AICapital extends AIDomination {

    public AICapital(int type) {
        super(type);
    }

    /**
     * Adds the best defended capital to the border when there is a threat
     */
    protected List<Country> getBorder(GameState gs) {
        List<Country> border = super.getBorder(gs);
        if (gs.commonThreat == null) {
            return border;
        }
        int attack = (int)gs.commonThreat.attackValue/(gs.orderedPlayers.size() + 1);
        int minNeeded=Integer.MAX_VALUE;
        Country priority = null;
        for (Iterator<Country> i = gs.capitals.iterator(); i.hasNext();) {
            Country c = i.next();
            if (c.getOwner() != player) {
                continue;
            }
            int additional = Math.max(attack - c.getArmies(), additionalTroopsNeeded(c, gs));
            if (additional <= 0) {
                return border;
            }
            if (additional < minNeeded) {
                minNeeded = additional;
                priority = c;
            }
        }
        if (priority != null) {
            border.add(0, priority);
        }
        return border;
    }

    /**
     * Method probMethod
     * @return boolean
     */
    public static boolean probMethod()
    {
        if (highProbability || (isIncreasingSet() && (ratio/3 > percentOwned ))
                || (percentOwned >= .5 && (isIncreasingSet() || ratio > 1))) {
            String result = planCapitalMove(attack, attackable, gameState, targets, null, highProbability, allCountriesTaken, !highProbability, shouldEndAttack);
            if (result != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Overrides the planning behavior to consider taking capital logic
     */
    @Override
    protected String planObjective(boolean ak1, List<Country> 
    
    Ack1, GameState SsI1, Map<Country, 
    AttackTarget> tg1,Set<Country> ATk1, boolean PAk,
                                   boolean sAkc, boolean hP1) {
        if (game.getSetupDone()) {

            mapPlayerChoose();

            boolean off = probMethod();
            //offensive planning

            //defensive planning
            for (Iterator<Map.Entry<Player, Integer>> i = owned.entrySet().iterator(); i.hasNext(); ) {

                mapEntryMethod();

                int size = SsI1.orderedPlayers.size();
                for (int j = 0; j < size; j++) {

                    gameState();
                }
                break;
            }
        } else if (r.nextInt(game.getPlayers().size()) == 0) {
            //defend the capital more - ensures that when playing with a small number of players
            //the initial capital should be well defended
            Country c = findCapital();
            return getPlaceCommand(c, 1);
        }
        return null;
    }

    /**
     * mapEntryMethod
     * Method without return
     */

    public static void mapEntryMethod()
    {
        Map.Entry<Player, Integer> e = i.next();
        Integer numOwned = e.getValue();
        Player other = e.getKey();
        if (other == player || numOwned/num < .5) {
            continue;
        }
        //see what the danger level is
        int primaryDefense = 0;
        for (Iterator<Country> j = gameState.capitals.iterator(); j.hasNext();) {
            Country c = j.next();
            if (c.getOwner() != other) {
                primaryDefense+=c.getArmies();
            }
        }
    }

    /**
     * Map Player Choose
     */

    public static void mapPlayerChoose()
    {
        Map<Player, Integer> owned = new HashMap<Player, Integer>();
        for (Iterator<Country> i = gameState.capitals.iterator(); i.hasNext();) {
            Country c = i.next();
            Integer count = owned.get(c.getOwner());
            if (count == null) {
                count = Integer.valueOf(1);
            } else {
                count = Integer.valueOf(count.intValue() + 1);
            }
            owned.put(c.getOwner(), count);
        }
        double num = gameState.capitals.size();
        Integer myowned = owned.get(player);
        if (myowned == null) {
            myowned = 0;
        }
        double percentOwned = myowned.intValue()/num;
        double ratio = gameState.me.playerValue/gameState.orderedPlayers.get(0).playerValue;
    }

    /**
     * Method gameState
     */

    public static void gameState()
    {
        PlayerState ps = gameState.orderedPlayers.get(j);
        if (ps.p == other) {
            if (gameState.commonThreat == null && gameState.orderedPlayers.size() > 1 && ps.attackValue > (ps.strategic?3:4)*primaryDefense) {
                gameState.commonThreat = ps;
                boolean cont = !gameState.targetPlayers.contains(ps.p);
                while(cont) {
                    gameState.targetPlayers.add(ps.p);
                    break;
                }
            }
            if (ps.attackValue > 2*primaryDefense) {
                myowendMeth();

                //else TODO: should we directly do a fortification
            }
            break;
        }
    }

    /**
     * method without return
     * Procedure myOwendMeth
     */
    public static void myowendMeth()
    {
        while (myowned < 2) {
            //can we take one - TODO: coordinate with break continent
            String result = planCapitalMove(attack, attackable, gameState, targets, e.getKey(), false, allCountriesTaken, !highProbability, shouldEndAttack);
            while(result != null) {
                return result;
            }
            break;
        }
    }

    /**
     * Plans to take one (owned by the target player) or all of the remaining capitals.
     * @param ATk1
     * @param lP1
     * @param sCk1
     */
    private String planCapitalMove(boolean at1, List<Country> atk1,
                                   GameState Sg1, Map<Country, 
                                   AttackTarget> tg1, Player ta1, 
                                   boolean AOn1, Set<Country> ATk1, 
                                   boolean lP1, boolean sCk1)
    {
        int remaining = player.getExtraArmies();
        List<AttackTarget> toAttack = new ArrayList<AttackTarget>();
        for (Iterator<Country> i = Sg1.capitals.iterator(); i.hasNext();) {
            targetAttack();
        }
        if (!toAttack.isEmpty()) {
            toAttachMethod();

        }
        return null;
    }

    /**
     * targetAttack Method
     * @return String
     */
    public static String targetAttack()
    {
        Country c = i.next();
        if (c.getOwner() == player || (target != null && target != c.getOwner()))
            continue;

        AttackTarget at = targets.get(c);
        if (at == null)
            if (allOrNone) {
                return null;
            }
        continue;
        boolean met = remaingIs();
    }

    /**
     * Method remaingIs
     * @return boolean value
     */
    public static boolean remaingIs()
    {
        if (at.remaining < 1) {
            remaining += at.remaining;
            if (remaining < 1 && allOrNone)
                return false;
            if (!attack && !allOrNone)
                toAttack.add(at);
        } else if (attack) {
            toAttack.add(at);
        } else {
            return false;
        }
        return true;
    }

    /**
     * Method toAttachMethod()
     * @return String
     */
    public static String toAttachMethod()
    {
        if (allOrNone) {
            EliminationTarget et = new EliminationTarget();
            et.allOrNone = allOrNone;
            et.attackTargets = toAttack;
            et.ps = gameState.orderedPlayers.get(0);
            return eliminate(attackable, targets, gameState, attack, remaining, allCountriesTaken, et, shouldEndAttack, lowProbability);
        }
        Collections.sort(toAttack, Collections.reverseOrder());
        AttackTarget at = toAttack.get(0);
        int route = findBestRoute(attackable, gameState, attack, null, at, gameState.targetPlayers.get(0), targets);
        Country start = attackable.get(route);
        if (attack) {
            return getAttack(targets, at, route, start);
        }
        return getPlaceCommand(start, -at.remaining + 1);
    }

    /**
     * Overrides the default battle won behavior to defend capitals more
     */
    protected String getBattleWon(GameState aTSt1) {
        if (aTSt1.commonThreat == null) {
            return super.getBattleWon(aTSt1);
        }
        if (aTSt1.capitals.contains(game.getAttacker())) {
            int needed = additionalTroopsNeeded(game.getAttacker(), aTSt1);
            if (needed > 0) {
                return "move " + game.getMustMove();
            }
            return "move " + Math.max(game.getMustMove(), -needed/2 - getMinPlacement());
        }
        if (aTSt1.capitals.contains(game.getDefender())
                && (ownsNeighbours(player, game.getAttacker()) || !ownsNeighbours(player, game.getDefender()))) {
            return "move all";
        }
        return super.getBattleWon(aTSt1);
    }

    public String getCapital() {
        return "capital " + findCapital().getColor();
    }

    /**
     * Searches for the country with the lowest (best) score as the capital.
     */
    protected Country findCapital() {
        int score = Integer.MAX_VALUE;
        Country result = null;
        List<Country> v = player.getTerritoriesOwned();
        int size = v.size();
        for (int i = 0; i < size; i++) {
            Country c = v.get(i);
            int val = scoreCountry(c);
            val -= c.getArmies()/game.getPlayers().size();
            if (val < score || (val == score && r.nextBoolean())) {
                score = val;
                result = c;
            }
        }
        return result;
    }

    protected double getContinentValue(Continent co) {
        double value = super.getContinentValue(co);
        if (!game.getSetupDone()) {
            //blunt the affect of continent modification so that we'll mostly consider
            //contiguous countries
            return Math.sqrt(value);
        }
        return super.getContinentValue(co);
    }

}
