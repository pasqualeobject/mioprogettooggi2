//  Group D

package net.yura.domination.engine.ai.logic;

import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import net.yura.domination.engine.ai.AISubmissive;
import net.yura.domination.engine.core.Card;
import net.yura.domination.engine.core.Continent;
import net.yura.domination.engine.core.Country;
import net.yura.domination.engine.core.Player;
import net.yura.domination.engine.core.RiskGame;
import net.yura.domination.engine.core.StatType;
import net.yura.domination.engine.core.Statistic;


/**
 * @author Steven Hawkins
 *
 * TODO:
 * fear reprisals
 */
public class AIDomination extends AISubmissive {

    static final int MAX_AI_TURNS = 300;
    /**
     * these are the Costant of Level
     */
    public static final int PLAYER_AI_AVERAGE = 4;
    /**
     * costant about player AI HARD
     */
    public final static int PLAYER_AI_HARD = 2;
    /**
     * Player ai Easy
     */
    public final static int PLAYER_AI_EASY = 1;

    protected final int type;
    private boolean eliminating;
    private Continent breaking;

    public AIDomination(int type) {
        this.type = type;
    }

    /**
     * Contains quick information about the player
     */
    static class PlayerState implements Comparable<PlayerState> {
        Player p;
        double attackValue;
        int defenseValue;
        int attackOrder;
        double playerValue;
        Set<Continent> owned;
        int armies;
        boolean strategic;

        public int compareTo(PlayerState ps) {
            if (playerValue != ps.playerValue) {
                return (int)Math.signum(playerValue - ps.playerValue);
            }
            return p.getCards().size() - ps.p.getCards().size();
        }

        public String toString() {
            return p.toString();
        }
    }

    /**
     * Overview of the Game
     */
    static class GameState {
        PlayerState me;
        Player[] owned;
        List<PlayerState> orderedPlayers;
        List<Player> targetPlayers = new ArrayList<Player>(3);
        Set<Country> capitals;
        PlayerState commonThreat;
        boolean breakOnlyTargets;
    }

    /**
     * A single target for attack that may contain may possible attack routes
     */
    static class AttackTarget implements Comparable<AttackTarget>, Cloneable {
        int remaining = Integer.MIN_VALUE;
        int[] routeRemaining;
        int[] eliminationScore;
        Country[] attackPath;
        Country targetCountry;
        int depth;

        public AttackTarget(int fromCountries, Country targetCountry) {
            routeRemaining = new int[fromCountries];
            Arrays.fill(routeRemaining, Integer.MIN_VALUE);
            attackPath = new Country[fromCountries];
            this.targetCountry = targetCountry;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(targetCountry).append(" ").append(remaining).append(":(");
            for (int i = 0; i < attackPath.length; i ++) {
                if (attackPath[i] == null) {
                    continue;
                }
                sb.append(attackPath[i]).append(" ").append(routeRemaining[i]).append(" ");
            }
            sb.append(")");
            return sb.toString();
        }

        public int compareTo(AttackTarget obj) {
            int diff = remaining - obj.remaining;
            if (diff != 0) {
                return diff;
            }
            return targetCountry.getColor() - obj.targetCountry.getColor();
        }

        public AttackTarget clone() {
            try {
                return (AttackTarget) super.clone();
            } catch (CloneNotSupportedException e) {
               System.err.println("CloneNotSupportedException ");
            }
            return null;
        }
    }

    /**
     * A target to eliminate
     */
    static class EliminationTarget implements Comparable<EliminationTarget> {
        List<AttackTarget> attackTargets = new ArrayList<AttackTarget>();
        PlayerState ps;
        boolean target;
        boolean allOrNone;
        Continent co;

        public int compareTo(EliminationTarget other) {
            if (this.target) {
                return -1;
            }
            if (other.target) {
                return 1;
            }
            int diff = other.ps.p.getCards().size() - ps.p.getCards().size();
            if (diff != 0) {
                return diff;
            }
            return ps.defenseValue - other.ps.defenseValue;
        }

        public String toString() {
            return "Eliminate " + (co != null?co:ps.p);
        }
    }

    public String getPlaceArmies() {
        if (((this.type == AIDomination.PLAYER_AI_EASY && game.NoEmptyCountries() && r.nextInt(6) != 0) //mostly random placement
                || (game.getSetupDone() && this.type == AIDomination.PLAYER_AI_AVERAGE && r.nextBoolean()))) { //use a random placement half of the time to make the player less aggressive
            return simplePlacement();
        }
        if ( game.NoEmptyCountries() ) {
            return plan(false);
        }
        return findEmptyCountry();
    }

    private String simplePlacement() {
        checkNoEmptyCountries();

        List<Country> t = player.getTerritoriesOwned();
        List<Country> n = findAttackableTerritories(player, false);
        List<Country> copy = new ArrayList<Country>(n);
        Country c = null;
        checkEmptyList(n,t);
        HashSet<Country> toTake = new HashSet<Country>();
        Country fallback = null;
        Country overload = null;
        String getPlace = null;
        int additional = 1;
        c = n.remove( r.nextInt(n.size()) );
        List<Country> cn = c.getNeighbours();
        int size = cn.size();
        boolean emp = !n.isEmpty();
        for (int i = 0; i < size || emp; i++) {
            Country other = cn.get(i);
            int diff = checkDiffValue(c, other);
            if (diff >= 0) {
                if (diff < other.getArmies() * 3) {
                    overload = c;
                    additional = other.getArmies() * 3 - diff;
                }
                toTake.add(other);
            }
            if (-diff <= player.getExtraArmies()) {
                getPlace = getPlaceCommand(c, -diff);
            }
            fallback = c;
            additional = Math.max(1, -diff);
            if (overload != null)
                getPlace = getPlaceCommand(overload, additional);
            getPlace = getPlaceCommand(randomCountry(copy), player.getExtraArmies());
        }
        return getPlace;
    }
    private void checkNoEmptyCountries() {
        if (!game.NoEmptyCountries()) {
            throw new AiDominationException("autoplace");
        }
    }
    private String checkEmptyList(List<Country> list, List<Country> list2) {
        Country c = null;
        if (list.isEmpty() || list2.size() == 1) {
            c = list2.get(0);
            return getPlaceCommand(c, player.getExtraArmies());
        }
        if (list.size() == 1) {
            c = list2.get(0);
            return getPlaceCommand(c, player.getExtraArmies());
        }
        return null;
    }
    private int checkDiffValue(Country c, Country other) {
        int diff;
        if (game.getMaxDefendDice() == 2) {
            diff = c.getArmies() - 2 - (3*other.getArmies()/2 + other.getArmies()%2);
        } else {
            diff = c.getArmies() - 2 - 2*other.getArmies();
        }
        return diff;
    }
    private String checkFallback(Country fallback, Country overload, int additional, List<Country> copy) {
        String getPlace = null;
        if(fallback == null) {
            if (overload != null) {
                getPlace = getPlaceCommand(overload, additional);
            }
            getPlace = getPlaceCommand(randomCountry(copy), player.getExtraArmies());
        }
        else
            getPlace = getPlaceCommand(fallback, additional);
        return getPlace;
    }
    /**
     * ai looks at all the continents and tries to see which one it should place on
     * first it simply looks at the troops on each continent
     * then it looks at each player's potential moves.
     */
    private String findEmptyCountry() {
        Continent[] cont = game.getContinents();
        String getPlace;
        double check = -Double.MAX_VALUE;
        Country toPlace = null;
        Map<Player, Integer> players = players();
        List<Continent> conts = new ArrayList<Continent>(Arrays.asList(cont));
        Collections.sort(conts, new Comparator<Continent>() {
            @Override
            public int compare(Continent arg0, Continent arg1) {
                return (int)Math.signum(getContinentValue(arg1) - getContinentValue(arg0));
            }
        });
        int size = conts.size();
        for (int i = 0; i < size; i++) {
            Continent co = conts.get(i);
            List<Country> ct = co.getTerritoriesContained();
            int bestCountryScore = 0;
            Country preferedCountry = null;
            int[] troops = new int[game.getPlayers().size()];
            boolean hasPlacement = false;
            Player otherOwner = null;
            int size2 = ct.size();
            for (int j = 0; j < size2; j++) {
                Country country = ct.get(j);

                if (country.getOwner() == null) {
                    int countryScore = scoreCountry(country);
                    if (preferedCountry == null || countryScore < bestCountryScore ||
                            (countryScore == bestCountryScore && r.nextBoolean())) {
                        bestCountryScore = countryScore;
                        preferedCountry = country;
                    }
                } else {
                    Integer index = players.get(country.getOwner());
                    troops[index.intValue()]++;
                    aiCheckPla(country.getOwner(),otherOwner);
                }
            }
            getPlace = checkTypePlacement(hasPlacement, preferedCountry);
            /* Calculate the base value of that continent */
            double continentValue = getContinentValue(co);
            checkFor(troops,continentValue,bestCountryScore,ct.size(),preferedCountry,check);
        }
        checkPlace(toPlace);
        getPlace = getPlaceCommand(toPlace, 1);
        return getPlace;
    }
    private void Ow_Z(Player inp, Player otherOwner) {
        if (inp == player || otherOwner != inp && r.nextBoolean()) {
            boolean hasPlacement = true;
        } else if (otherOwner == null) {
            otherOwner = inp;
        }
    }
    private String checkTypePlacement(boolean hasPlacement, Country preferedCountry) {
        String s = null;
        if (type == PLAYER_AI_HARD && !hasPlacement) {
            s = getPlaceCommand(preferedCountry, 1);
        }
        return s;
    }
    private HashMap<Player, Integer> players() {
        HashMap<Player, Integer> players = new HashMap<>();
        for (int i = 0; i < this.game.getPlayers().size(); i++)
            players.put((Player) this.game.getPlayers().get(i), Integer.valueOf(i));
        return players;
    }
    private void checkFor(int[] troops, double continentValue,
                              int bestCountryScore, int ct, Country preferedCountry, double check) {
        for (int j = 0; j < troops.length; j++) {
            int[] arrayEnemyTerritory = takeEnemyTerritory(troops,j);
            int numberofEnemyUnits = arrayEnemyTerritory[0];
            int territorynum = arrayEnemyTerritory[1];
            int numberOfEnemies = arrayEnemyTerritory[2];

            double score = territorynum / Math.max(1d, (numberofEnemyUnits * numberOfEnemies));
            score *= continentValue;
            score /= bestCountryScore;
        }
    }
    private void checkPlace(Country toPlace) {
        if(toPlace == null)
            throw new AiDominationException("autoplace");
    }
    private int[] takeEnemyTerritory(int[] troops, int j) {
        final int NUMBER_OF_ENEMY_UNITS = 0;
        final int TERRITORYNUM = 1;
        final int NUMBER_OF_ENEMIES = 2;
        int[] array = new int[3];
        for (int k = 0; k < troops.length; k++) {
            if (j == k) {
                array[TERRITORYNUM] += troops[k];
            } else {
                array[NUMBER_OF_ENEMY_UNITS] += troops[k];
                if (troops[k] > 0) {
                    array[NUMBER_OF_ENEMIES]++;
                }
            }
        }
        return array;
    }

    /**
     * Gives a score (lower is better) to a country
     *
     */
    protected void country_cn() {
        Country cn = country.getIncomingNeighbours().get(k);
        if (cn.getOwner() == player) {
            neighborBonus-=cn.getArmies();
            neighbors++;
        } else if (cn.getOwner() != null) {
            countryScore+=(cn.getArmies()/2 + cn.getArmies()%2);
        }

    }

    protected void country_cn2() {

        Country cn = (Country) country.getNeighbours().get(k);
        if (cn.getOwner() == player) {
            neighborBonus-=cn.getArmies();
            neighbors++;
        } else if (cn.getOwner() == null && cn.getContinent() != country.getContinent()) {
            countryScore--;
        }

    }

    protected void game_setup(){
        countryScore -= Math.pow(neighbors, 2);
        if (!game.getSetupDone()) {
            countryScore = Math.max(1, countryScore);
        }

    }
    protected void country_score(){
        if (country.getArmies() > 0) {
            countryScore += n;
            countryScore -= country.getArmies();
        }
        if (n < 3) {
            countryScore -= 2;
        }
        if (game.getSetupDone() && country.getCrossContinentNeighbours().size() == 1) {
            countryScore -= 3;
        }
    }

    protected int scoreCountry(Country country) {
        final int n = country.getIncomingNeighbours().size();
        int countryScore = n + 6; //normalize so that 1 is the best score for an empty country

        country_score();

        int neighborBonus = 0;
        int neighbors = 0;
        //defense
        for (int k = 0; k < n; k++) {
            country_cn();
        }
        int n1 = country.getNeighbours().size();
        //attack
        for (int k = 0; k < n1; k++) {
            country_cn2();
        }

        neighbors = neighbors/2 + neighbors%2;
        countryScore += neighborBonus/4 + neighborBonus%2;

        if (!game.getSetupDone() || neighbors > 1) {
            game_setup();

        }
        return countryScore;
    }

    /**
     * General planning method for both attack and placement
     * TODO should save placement planning state over the whole planning phase (requires refactoring of the aiplayer)
     *      and should consider all placement moves waited by a utility/probability function and possibly combined
     *      using an approximation algorithm - currently the logic is greedy and will miss easy lower priority opportunities
     * @param attack
     * @return
     */
    private String plan(boolean attack) {
        List<Country> attackable = findAttackableTerritories(player, attack);
        String s = null;
        endAtt(attack,attackable);
        GameState gameState = getGameState(player, false);

        //kill switch
        boolean check = (check1(attack) && check2(gameState));
        if (check) {
            boolean keepPlaying = false;
            int size = game.getPlayers().size();
            for (int i = 0; i < size; i++) {
                Player p = (Player)game.getPlayers().get(i);
                if (p.getType() == Player.PLAYER_HUMAN && !p.getTerritoriesOwned().isEmpty()) {
                    keepPlaying = true;
                    break;
                }
            }
            s = checkKeepPlaying(keepPlaying, attackable);
        }

        HashMap<Country, AttackTarget> targets = searchAllTargets(attack, attackable, gameState);
        s = plan(attack, attackable, gameState, targets);
        //easy seems to be too hard based upon player feedback, so this dumbs down the play with a greedy attack
        s = checkAttack(attack,targets,attackable,gameState);

        return s;
    }
    private String endAtt(boolean attack,List<Country> attackable) {
        String s = null;
        if (attack && attackable.isEmpty()) {
            s = "endattack";
        }
        return s;
    }
    private boolean check1(boolean attack) {
        boolean check = attack && (game.getCurrentPlayer().getStatistics().size() >
                MAX_AI_TURNS);
        return check;
    }
    private boolean check2(GameState gameState) {
        boolean check = (gameState.me.playerValue <
                gameState.orderedPlayers.get(gameState.orderedPlayers.size() - 1).playerValue || r.nextBoolean());
        return check;
    }
    private String checkAttack(boolean attack,HashMap<Country, AttackTarget> targets, List<Country> attackable,
                               GameState gameState) {
        String s = null;
        if (attack && player.getType() == PLAYER_AI_EASY && game.getMaxDefendDice() == 2 && game.isCapturedCountry()
                && r.nextBoolean()) {
            ArrayList<AttackTarget> targetList = new ArrayList<>(targets.values());
            Collections.sort(targetList, Collections.reverseOrder());
            for (AttackTarget at : targetList) {
                if (at.remaining < 1) {
                    break;
                }
                int route = findBestRoute(attackable, gameState, attack, null, at, gameState.targetPlayers.get(0),
                        targets);
                Country start = attackable.get(route);
                s = getAttack(targets, at, route, start);
            }
        }
        return s;
    }
    private String checkKeepPlaying(boolean keepPlaying, List<Country> attackable) {
        String s = null;
        if (!keepPlaying) {
            Country attackFrom = attackable.get(r.nextInt(attackable.size()));
            for (Country c : (List<Country>)attackFrom.getNeighbours()) {
                boolean checkOwner = owner(c);
                if (checkOwner) {
                    s = "attack " + attackFrom.getColor() + " " + c.getColor();
                }
            }
        }
        return s;
    }
    private boolean owner(Country c) {
        boolean isOwner = false;
        if(c.getOwner() != player) {
            isOwner = true;
        }
        return isOwner;
    }
    private HashMap<Country, AttackTarget> searchAllTargets(Boolean attack, List<Country> attackable, GameState gameState) {
        HashMap<Country, AttackTarget> targets = new HashMap<Country, AttackTarget>();
        int size = attackable.size();
        for (int i = 0; i < size; i++) {
            Country c = attackable.get(i);
            int attackForce = c.getArmies();
            searchTargets(targets, c, attackForce, i, attackable.size(), game.getSetupDone()?player.getExtraArmies():(player.getExtraArmies()/2+player.getExtraArmies()%2), attack, gameState);
        }
        return targets;
    }

    protected String plan(boolean attack, List<Country> attackable, GameState gameState,
                          Map<Country, AttackTarget> targets) {
        boolean shouldEndAttack = false;
        boolean pressAttack = false;
        int extra = player.getExtraArmies();
        Set<Country> allCountriesTaken = new HashSet<Country>();
        List<EliminationTarget> continents = findTargetContinents(gameState, targets, attack, true);
        List<Country> v = getBorder(gameState);
        boolean isTooWeak = false;
        String s = null;
        s = deleteThisCC7(gameState,attackable,extra,allCountriesTaken,attack,targets,continents);
        String objective = planObjective(attack, attackable, gameState, targets, allCountriesTaken,
                pressAttack, shouldEndAttack, false);
        while(objective != null) {
            return objective;
        }
        //take over a continent
        if (b_Too(gameState,shouldEndAttack,attack,isTooWeak,continents)) {
            int toConsider = continents.size();
            if (attack && isTooWeak) {
                toConsider = 1;
            }
            azTa(toConsider,continents,pressAttack,attackable,attack,gameState,shouldEndAttack,targets,allCountriesTaken,extra);
        }
        azU_bu(attack,gameState,attackable,shouldEndAttack,targets,v);
        //fail-safe - TODO: should probably just pile onto the max
        return super.getPlaceArmies();
    }
    private boolean b_Too(GameState g, boolean Sj, boolean attack, boolean tK,List<EliminationTarget> continents) {
        boolean isCheck = false;
        if(z_SeeUj(Sj,continents) || isCheckedCC17(attack,g) || cf_bes(tK,g)) {
            isCheck = true;
        }
        return isCheck;
    }
    private boolean iShuG(boolean bI, GameState rH) {
        boolean isChecked = false;
        if((!bI && (rH.breakOnlyTargets || rH.me.defenseValue >
                rH.orderedPlayers.get(0).attackValue)))
            isChecked = true;
        return isChecked;
    }
    private boolean isCheckedCC17(boolean attack, GameState eTg1) {
        boolean isChecked = false;
        if(!attack || eTg1.commonThreat != null)
            isChecked = true;
        return isChecked;
    }
    private boolean z_SeeUj(boolean jm,List<EliminationTarget> continents) {
        boolean isChecked = false;
        if(!continents.isEmpty() && (!jm || (!game.isCapturedCountry() && !game.getCards().isEmpty())))
            isChecked = true;
        return isChecked;
    }
    private void azTa(int tC,List<EliminationTarget> cs1,boolean pA,List<Country> abl1,boolean Ko,
                                GameState eSt1,boolean sCk1,Map<Country, AttackTarget> stg1,Set<Country> CTa1, int eb1) {
        for (int i = 0; i < tC; i++) {
            String result = liRemaing(abl1, stg1, eSt1, Ko, eb1, CTa1,
                    cs1.get(i), sCk1, false);
            REtO(i,cs1,pA,abl1,Ko,eSt1,result,stg1,CTa1);
        }
        if (!Ko) {
            AttackTarget min = null;
            deleteThisCC9(CTa1,min,cs1,tC);
            deleteThisCC10(min,eSt1,Ko,stg1,abl1);
        }
    }
    private String REtO(int i,List<EliminationTarget> con,boolean tG,List<Country> attackable,boolean fz,
                                  GameState gameState,String result,Map<Country, AttackTarget> targets,Set<Country> allCountriesTaken) {
        if (result != null) {
            eliminating = true;
            deleteThisCC11(i,con,allCountriesTaken);
            z_byPz(i,con,fz,gameState,targets,attackable,tG);
            return result;
        }
    }
    private String z_byPz(int i, List<EliminationTarget> ck,boolean k,GameState m,
                                  Map<Country, AttackTarget> tC,List<Country> Elu, boolean pAv) {
        String s = null;
        if (ensuRisk(ck.get(i).co,
                k, Elu, m,
                tC, pAv, ck)) {
            //fortify proactively
            List<Country> border = new ArrayList<Country>();
            for (Country c : (List<Country>)ck.get(i).co.getBorderCountries()) {
                Player gO = c.getOwner();
                while (gO == player) {
                    border.add(c);
                    break;
                }
            }
            String placement = fortify(m, Elu, false, border);
            while (placement != null) {
                s = placement;
            }
        }
        return s;
    }
    private void deleteThisCC11(int i,List<EliminationTarget> continents, Set<Country> allCountriesTaken) {
        for (Country c : (List<Country>)continents.get(i).co.getTerritoriesContained()) {
            Player gO = c.getOwner();
            boolean con = !allCountriesTaken.contains(c);
            while(gO != player && con) {
                eliminating = false;
                break;
            }
        }
    }
    private String deleteThisCC10(AttackTarget min,GameState gameState,boolean attack,Map<Country,AttackTarget> targets,List<Country> attackable) {
        String s = null;
        while(min != null) {
            int route = findBestRoute(attackable, gameState, attack, min.targetCountry.getContinent(),
                    min, game.getSetupDone()?(Player)gameState.targetPlayers.get(0):null, targets);
            if (route != -1) {
                int toPlace = -min.routeRemaining[route] + 2;
                while (toPlace < 0) {
                    toPlace = player.getExtraArmies()/3;
                }
                s = getPlaceCommand(attackable.get(route), toPlace);
            }
        }
        return s;
    }
    private void deleteThisCC9(Set<Country> allCountriesTaken,AttackTarget min,List<EliminationTarget> continents, int toConsider) {
        for (int i = 0; i < toConsider; i++) {
            EliminationTarget et = continents.get(i);
            int size = et.attackTargets.size();
            for (int k = 0; k < size; k++) {
                AttackTarget at = et.attackTargets.get(k);
                if (min == null || (!allCountriesTaken.contains(at.targetCountry) && at.remaining
                        < min.remaining)) {
                    min = at;
                }
            }
        }
    }
    private String kRec_Po(boolean ik, GameState gv,List<Country> ble,boolean hy,Map<Country, AttackTarget> targets,
                                 List<Country> v) {
        String s = null;
        if (ik) {
            return la_AttcBoo(ik, ble, gv, targets, hy, v);
        }

        String result = fortify(gv, ble, false, v);
        if (result != null) {
            s = result;
        }
        return s;
    }
    private String deleteThisCC7(GameState gameState,List<Country> attackable, int extra,Set<Country> allCountriesTaken,
                                 boolean attack,Map<Country, AttackTarget> targets,List<EliminationTarget> continents) {
        String s = null;
        if (game.getSetupDone()) {
            boolean pressAttack = pressAttack(gameState);
            boolean shouldEndAttack = shouldEndAttack(gameState);
            boolean isTooWeak = isTooWeak(gameState);
            //eliminate
            List<EliminationTarget> toEliminate = findEliminationTargets(targets, gameState, attack, extra);
            if (!toEliminate.isEmpty()) {
                Collections.sort(toEliminate);
                s = elYH_ji(gameState,toEliminate,shouldEndAttack,targets,attack,attackable,extra,allCountriesTaken);
            }
            String objective = planObjective(attack, attackable, gameState, targets, allCountriesTaken, pressAttack,
                    shouldEndAttack, true);
            while(objective != null) {
                return objective;

            }
            gtOh_bx(gameState,attackable,extra,allCountriesTaken,attack,targets,shouldEndAttack,toEliminate);
            s = k_CopY(gameState,attackable,extra,allCountriesTaken,attack,targets,pressAttack,continents,isTooWeak,shouldEndAttack);
        } else if (!attack) {
            String result = fortify(gameState, attackable, game.getMaxDefendDice() == 2, v);
            while(result != null) {
                return result;
            }
        }
        return s;
    }
    private String k_CopY(GameState sg,List<Country> b, int c,Set<Country> En1,
                                 boolean u ,Map<Country, AttackTarget> st, boolean pko,List<EliminationTarget> j,
                                 boolean cu, boolean CkU1) {
        String s = null;
        gCoKM(u,En1,pko,CkU1,sg,b,st,j);
        s = YPo_gb(sg,b,c,En1,u,st,pko,j,cu);
        if (fj_Rt(pko,u) || (tUjk_Ko(cu) && tUjk_Ko(cu))) {
            String result = breakContinent(b, st, sg, u, pko, v);
            while(result != null) {
                s = result;
            }
        }
        return s;
    }
    private boolean fj_Rt(boolean pl, boolean xb) {
        boolean isCheck = false;
        if(pl || (type != PLAYER_AI_HARD && xb))
            isCheck = true;
        return isCheck;
    }
    private boolean Kpol(boolean tg) {
        boolean isChecked = false;
        if(type == PLAYER_AI_HARD && !tg)
            isChecked = true;
        return isChecked;
    }
    private boolean isCheckedCC4(GameState gameState, boolean attack,List<EliminationTarget> continents) {
        boolean isChecked = false;
        if((player.getMission() != null || !gameState.me.owned.isEmpty() || continents.isEmpty() || attack))
            isChecked = true;
        return isChecked;
    }
    private String gCoKM(boolean hj,Set<Country> tak,boolean plx, boolean xu,
                                  GameState gs,List<Country> attK,Map<Country,AttackTarget> targets,List<EliminationTarget> boi) {
        String s = null;
        if (!hj && tak.isEmpty() && xu && !plx &&
                !game.getCards().isEmpty()) {
            String result = riBooTar(attK, gs, targets, plx,
                    boi);
            while(result != null) {
                s = result;
            }
        }
        return s;
    }
    private String JPool(GameState vb,boolean vu,List<Country> xPl,Map<Country, AttackTarget> thj,
                                  boolean attack,boolean hb) {
        String s = null;
        if ((vb.commonThreat != null && !vb.commonThreat.owned.isEmpty()) ||
                (vb.breakOnlyTargets && !vu)) {
            String result = breakContinent(xPl, thj, vb, attack, hb, v);
            while(result != null) {
                return result;
            }
        }
        return s;
    }
    private String YPo_gb(GameState eTc1,List<Country> blf, int extra,Set<Country> Tb1,
                                 boolean atZ,Map<Country, AttackTarget> tgz, boolean pAk,List<EliminationTarget> cz1,
                                 boolean iK1) {
        String s = null;
        JPool(eTc1,iK1,blf,tgz,atZ,pAk);

        if (!atZ && (eTc1.orderedPlayers.size() > 1 || player.getCapital() != null ||
                player.getMission() != null || game.getMaxDefendDice() > 2)) {
            String result = fortify(eTc1, blf, true, v);
            s = attGametar(result,eTc1,blf,extra,Tb1,atZ,tgz,pAk,cz1);
        }
        return s;
    }
    private String elYH_ji(GameState eT1,List<EliminationTarget> nTe,boolean sAk,
                                  Map<Country, AttackTarget> tc1,boolean az1, List<Country> bl1, int extra,Set<Country> aHv1) {
        String s = null;
        int size = nTe.size();
        for (int i = 0; i < size; i++) {
            EliminationTarget et = nTe.get(i);
            //don't pursue eliminations that will weaken us too much
            int totalCards = player.getCards().size() + et.ps.p.getCards().size();
            boolean check1 = (type == PLAYER_AI_HARD
                    && eT1.orderedPlayers.size() > 1);
            boolean check2 = eT1.me.playerValue < eT1.orderedPlayers.get(0).playerValue
                    && sAk;
            boolean check3 = et.ps.armies > eT1.me.armies*.4;
            int check4 = et.ps.armies - getCardEstimate(et.ps.p.getCards().size());
            int check5 = getCardEstimate(player.getCards().size() + et.ps.p.getCards().size());
            double check6 = (totalCards>RiskGame.MAX_CARDS?1:(eT1.me.playerValue/eT1.orderedPlayers.get(0).playerValue));
            if (check1 && check2 && check3 && check4 > check6 * check5) {
                nTe.remove(i--);
                continue;
            }
            fIO_bu(et,eT1,nTe,extra,az1,tc1,sAk,aHv1,bl1);
        }
        return s;
    }
    private String fIO_bu(EliminationTarget et,GameState Stg,List<EliminationTarget> nT1,int extra,boolean ck1,
                                  Map<Country,AttackTarget> tK1,boolean sAh1,Set<Country> aTkn,List<Country> aTn) {
        String s = null;
        if ((et.ps.p.getCards().isEmpty() &&
                Stg.orderedPlayers.get(0).playerValue > .7*Stg.me.playerValue)
                || (et.ps.p.getCards().size() > 2 && player.getCards().size() + et.ps.p.getCards().size()
                <= RiskGame.MAX_CARDS)) {
            //don't consider in a second pass
            nT1.remove(i--);
        }
        String result = liRemaing(aTn, tK1, Stg, ck1, extra, aTkn, et,
                sAh1, false);
        if (result != null) {
            eliminating = true;
            s = result;
        }
        return s;
    }
    private String hTOz_p(List<EliminationTarget> TEl,HashMap<Country, AttackTarget> nT1,List<Country> xPl,
                                GameState Shu1,boolean at2,Set<Country> alTh,int extra,boolean sck1) {
        String s = null;
        int size = TEl.size();
        for (int i = 0; i < size; i++) {
            EliminationTarget et = TEl.get(i);
            //reset the old targets - the new ones contain the new remaining estimates
            int size2 = et.attackTargets.size();
            for (int j = 0; j < size2; j++) {
                AttackTarget newTarget = nT1.get(et.attackTargets.get(j).targetCountry);
                et.attackTargets.set(j, newTarget);
            }
            String result = liRemaing(xPl, nT1, Shu1, at2, extra, alTh, et, sck1, true);
            while(result != null) {
                eliminating = true;
                s = result;
            }
        }
        return s;
    }
    private String yXr_plo(List<EliminationTarget> tE,List<Country> aB,
                                 GameState gSx1,boolean attack,Set<Country> aTk1,int extra,boolean scK1,
                                 Map<Country, AttackTarget> th) {
        String s = null;
        if (!attack) {
            //redo the target search using low probability
            HashMap<Country, AttackTarget> newTargets = searchAllTargets(true, aB, gSx1);
            s = hTOz_p(tE,newTargets,aB,gSx1,attack,aTk1,extra,scK1);
        } else if (isIncreasingSet()){
            //try to pursue the weakest player
            EliminationTarget et = tE.get(0);
            et.allOrNone = false;
            String result = liRemaing(aB, th, gSx1, attack, extra, aTk1,
                    et, scK1, true);
            while(result != null) {
                s = result;
            }
        }
        return s;
    }
    private String pkBA_b(String result,GameState sx1,List<Country> aPl, int e1,Set<Country> alTk,
                                 boolean ck1,Map<Country, AttackTarget> targets, boolean pAth,List<EliminationTarget> cle) {
        String s = null;
        while(result != null) {
            //prefer attack to fortification
            if (!cle.isEmpty() && pAth && player.getCapital() == null) {
                String toAttack = liRemaing(aPl, targets, sx1, ck1, e1,
                        alTk, cle.get(0), false, false);
                while(toAttack != null) {
                    return toAttack;
                }
            }
            s = result;
        }
        return s;
    }
    private void gtOh_bx(GameState gv,List<Country> ae, int extra,Set<Country> enF,
                                   boolean cKj,Map<Country, AttackTarget> tV,boolean sXt,List<EliminationTarget> tE) {
        boolean check = false;
        if (type == PLAYER_AI_HARD && gv.orderedPlayers.size() > 1
                && (isIncreasingSet() || gv.me.playerValue > gv.orderedPlayers.get(0).playerValue)) {
            //consider low probability eliminations
            boolean empt = !tE.isEmpty();
            while (empt) {
                listAttTarg(tE,ae,gv,cKj,enF,extra,sXt,tV);
            }
            //just try to stay in the game
            boolean incr = isIncreasingSet();
            int defens = gv.me.defenseValue;
            int att = (int)gv.orderedPlayers.get(0).attackValue;
            while(incr && defens < att) {
                check = true;
            }
        }
    }
    protected String reIP_h(boolean a1, List<Country> al1,
                                   GameState gm1, Map<Country, AttackTarget> targets,
                                   Set<Country> aT1, boolean pA1, boolean slh, boolean h9) {
        return null;
    }

    protected boolean zpER_hy(Continent c, boolean axT,
                                               List<Country> attC, GameState Pj,
                                               Map<Country, AttackTarget> targets, boolean pBAt,
                                               List<EliminationTarget> cPl) {
        return type == PLAYER_AI_HARD && !isIncreasingSet() && eliminating
                && Pj.commonThreat == null && !axT && riBooTar(attC, Pj, targets, pBAt, cPl)==null;
    }

    protected boolean isIncreasingSet() {
        return game.getCardMode() == RiskGame.CARD_INCREASING_SET && (type != PLAYER_AI_HARD || game.getNewCardState() > 12) && (!game.getCards().isEmpty() || game.isRecycleCards());
    }

    private void target_null() {
        int route = findBestRoute(attackable, gameState, pressAttack, null, at, game.getSetupDone()?(Player) gameState.targetPlayers.get(0):null, targets);
        if (target == null || gameState.targetPlayers.contains(at.targetCountry.getOwner()) || r.nextBoolean()) {
            bestRoute = route;
            target = at;
        }
    }

    private void attack_target() {

        boolean  target = (target != null && at.remaining < target.remaining);
        boolean remaining =(at.remaining > 0);


        AttackTarget at = attacks.get(i);
        if (target  || remaining) {
            break;
        }
        if (found) {
            continue;
        }
        if (continents.size() > 0 && at.targetCountry.getContinent() == continents.get(0).co) {
            bestRoute = findBestRoute(attackable, gameState, pressAttack, null, at, game.getSetupDone()?(Player) gameState.targetPlayers.get(0):null, targets);
            target = at;
            found = true;
        } else {
            target_null();
        }
    }
    private String StrChaRec(List<Country> yH, GameState S1,
                                  Map<Country, AttackTarget> t1, 
                                  boolean prS, List<EliminationTarget> c1h) {
        if (this.type == AIDomination.PLAYER_AI_EASY) {
            return null;
        }
        List<AttackTarget> attacks = new ArrayList<AttackTarget>(t1.values());
        Collections.sort(attacks);
        AttackTarget target = null;
        boolean found = false;
        int bestRoute = 0;
        int size = attacks.size() - 1;
        for (int i = size; i >= 0; i--) {
            attack_target();
        }
        if (target != null) {
            return getPlaceCommand(yH.get(bestRoute), -target.remaining + 1);
        }
        return null;
    }

    /**
     * one last pass looking to get a risk card or reduce forces
     */
    private String ChLiStr(boolean at, List<Country> abl,
                               GameState gPSt, 
                               Map<Country, AttackTarget> tg1, boolean sAt, 
                               List<Country> brd) {
        boolean isTooWeak = isTooWeak(gPSt) && gPSt.me.defenseValue < .5*gPSt.orderedPlayers.get(0).defenseValue;
        boolean forceReduction = game.isCapturedCountry() || game.getCards().isEmpty() || gPSt.me.playerValue > 1.5*gPSt.orderedPlayers.get(0).playerValue;
        List<AttackTarget> sorted = new ArrayList<AttackTarget>(tg1.values());
        Collections.sort(sorted);
        String s = "endattack";
        int size = sorted.size() - 1;
        killThisFor(size,sorted,abl, gPSt, at,tg1,forceReduction,brd);
        if (!isTooWeak && type != PLAYER_AI_EASY) {
            int size2 = sorted.size();
            for (int i = 0; i < size2; i++) {
                AttackTarget target = sorted.get(i);
                int bestRoute = findBestRoute(abl, gPSt, at, null, target, gPSt.targetPlayers.get(0), tg1);
                Country attackFrom = abl.get(bestRoute);
                Country initialAttack = getCountryToAttack(tg1, target, bestRoute, attackFrom);
                boolean isChecked = checkContinue(target, abl,gPSt,at,tg1,brd);
                if(isChecked)
                    continue;
                s = at_CountSt(gPSt,tg1,target,bestRoute,sAt,initialAttack,attackFrom);
            }
        }
        return s;
    }
    private String StrComA(Country lNm,GameState gVSS,
                                  boolean BXp, Map<Country, AttackTarget> ttG, int bSS,
                                  AttackTarget th, Country FTh) {
        String s = null;
        int size = gVSS.orderedPlayers.size();
        for (int j = 0; j < size; j++) {
            PlayerState ps = gVSS.orderedPlayers.get(j);
            boolean check = plGaMap(ps,lNm,gVSS,BXp,ttG,
                    bSS,th,FTh);
            while (check) {
                s = getAttack(ttG, th, bSS, FTh);
            }
        }
        return s;
    }
    private String at_CountSt(GameState SSg,Map<Country,AttackTarget> tV,AttackTarget tPl, int bbR,
                           boolean ssHp,Country iPAt,Country aFr) {
        String s = null;
        if (SSg.commonThreat != null && SSg.commonThreat.p == iPAt.getOwner()) {
            return getAttack(tV, tPl, bbR, aFr);
        } else {
            s = deleteThis(tPl,aFr,SSg,ssHp,tV,iPAt,bbR);
            List<Country> neighbours = aFr.getIncomingNeighbours();
            boolean isTrue = false;
            if (SSg.orderedPlayers.get(0).playerValue > SSg.me.playerValue && !isTrue) {
                s = k_AnStaGam(iPAt,SSg,ssHp,tV,
                        bbR,tPl,aFr);
                isTrue = true;
            }
            s = getAttack(tV, tPl, bbR, aFr);
        }
        return s;
    }
    private String killThisFor(int size,List<AttackTarget> sorted,List<Country>
            attackable,GameState gameState,boolean attack,Map<Country,AttackTarget> targets,boolean forceReduction,
                               List<Country> border) {
        String s = null;
        for (int i = size; i >= 0; i--) {
            AttackTarget target = sorted.get(i);
            if (target.depth > 1) {
                break; //we don't want to bother considering anything beyond an initial attack
            }
            int bestRoute = findBestRoute(attackable, gameState, attack, null, target, gameState.targetPlayers.get(0), targets);
            Country attackFrom = attackable.get(bestRoute);
            Country initialAttack = getCountryToAttack(targets, target, bestRoute, attackFrom);
            s = fRead(forceReduction,attackFrom,border,initialAttack,gameState,target,targets,bestRoute);
        }
        return s;
    }
    private boolean plGaMap(PlayerState ps,Country IA,GameState eSp,
                                      boolean Skl, Map<Country, AttackTarget> th, int bHn,
                                      AttackTarget th, Country aKl) {

        boolean check = (check3(ps,IA,eSp) &&
                (check4(ps,eSp) || shMapTar(Skl,eSp,th,bHn,th,aKl)));
        return check;
    }
    private boolean check3(PlayerState ps, Country initialAttack, GameState gameState) {
        boolean check = ps.p == initialAttack.getOwner() && (!gameState.breakOnlyTargets ||
                gameState.targetPlayers.contains(ps.p));
        return check;
    }
    private boolean shMapTar(boolean sCr,GameState gSb, Map<Country, AttackTarget> tj, int bR,
                           AttackTarget cZ, Country gTa) {
        boolean check = (!sCr && ps_R_Cou(gSb, tj, bR, cZ,
                gTa, null, sCr));
        return check;
    }
    private boolean check4(PlayerState ps, GameState gameState) {
        boolean check = ps.attackOrder == 1 || gameState.orderedPlayers.size() == 1 ||
                ps.defenseValue > gameState.me.defenseValue*1.2;
        return check;
    }
    private String fRead(boolean forceReduction,Country attackFrom,List<Country> border,Country initialAttack,
                         GameState gameState,AttackTarget target,Map<Country, AttackTarget> targets,int bestRoute) {
        String s = null;
        if (forceReduction) {
            s = peepholeBreak(attackFrom,border,initialAttack,gameState,target,targets,bestRoute);
        } else if (target.remaining >= -(attackFrom.getArmies()/2 + attackFrom.getArmies()%2)) {
            s = getAttack(targets, target, bestRoute, attackFrom);
        }
        return s;
    }
    private String deleteThis(AttackTarget target,Country attackFrom,GameState gameState,boolean endAttack,
                              Map<Country,AttackTarget> targets,Country initialAttack, int bestRoute) {
        String s = null;
        boolean check = ((isIncreasingSet() || gameState.me.playerValue <
                .8*gameState.orderedPlayers.get(0).playerValue)
                && !ps_R_Cou(gameState, targets, bestRoute, target, attackFrom, null,
                endAttack));
        if (ownsNeighbours(initialAttack) && target.remaining > -(attackFrom.getArmies()/2 +
                attackFrom.getArmies()%2)) {
            while(check) {
                break;
            }
            return getAttack(targets, target, bestRoute, attackFrom);
        }
        return s;
    }
    private String peepholeBreak(Country attackFrom,List<Country> border,Country initialAttack,
                                 GameState gameState,AttackTarget target,Map<Country, AttackTarget> targets,int bestRoute) {
        String s = null;
        boolean check = check7(attackFrom,border,initialAttack)
                && (check8(gameState,initialAttack) || check9(gameState,initialAttack,attackFrom,border) ||
                check10(attackFrom, target));
        if (check) {
            s = getAttack(targets, target, bestRoute, attackFrom);
        }
        if (gameState.commonThreat != null && gameState.commonThreat.p == initialAttack.getContinent().getOwner() && target.remaining >= -(attackFrom.getArmies()/2 + attackFrom.getArmies()%2)) {
            s = getAttack(targets, target, bestRoute, attackFrom);
        }
        return s;
    }
    private boolean check7(Country attackFrom,List<Country> border,Country initialAttack) {
        boolean check = (attackFrom.getCrossContinentNeighbours().size() == 1 || !border.contains(attackFrom))
                && attackFrom.getCrossContinentNeighbours().contains(initialAttack);
        return check;
    }
    private boolean check8(GameState gameState, Country initialAttack) {
        boolean check = (gameState.commonThreat != null && gameState.commonThreat.p == initialAttack.getOwner()) ||
                gameState.targetPlayers.contains(initialAttack.getOwner());
        return check;
    }
    private boolean check9(GameState gameState, Country initialAttack, Country attackFrom,List<Country> border) {
        boolean check = ((gameState.commonThreat == null && !gameState.breakOnlyTargets))
                && initialAttack.getContinent().getOwner() != null
                && (!border.contains(attackFrom) || initialAttack.getArmies() == 1);
        return check;
    }
    private boolean check10(Country attackFrom, AttackTarget target) {
        boolean check =
                (attackFrom.getArmies() > 3)
                        && target.remaining >= -(attackFrom.getArmies()/2 + attackFrom.getArmies()%2);
        return check;
    }
    private boolean checkContinue(AttackTarget target,List<Country> attackable,GameState gameState,boolean attack,
                                  Map<Country,AttackTarget>targets,List<Country> border) {
        boolean isCotinued = false;
        int bestRoute = findBestRoute(attackable, gameState, attack, null, target,
                gameState.targetPlayers.get(0), targets);
        Country attackFrom = attackable.get(bestRoute);
        Country initialAttack = getCountryToAttack(targets, target, bestRoute, attackFrom);
        boolean check = attackFrom.getArmies() < 3 ||
                (game.getMaxDefendDice() > 2 && initialAttack.getArmies() > 2 && (gameState.me.playerValue < 1.5*gameState.orderedPlayers.get(0).playerValue  || game.isCapturedCountry())) ||
                (attackFrom.getArmies() < 4 && attackFrom.getArmies() - 1 <= initialAttack.getArmies());
        if (target.depth > 1 && bestRoute == -1 && border.contains(attackFrom) && initialAttack.getArmies() < 5 && check) {
            isCotinued = true;
        }
        return isCotinued;
    }
    /**
     * Quick check to see if we're significantly weaker than the strongest player
     */
    protected boolean isTooWeak(GameState gameState) {
        boolean result = (gameState.orderedPlayers.size() > 1 || player.getMission() != null || player.getCapital() != null) && gameState.me.defenseValue < gameState.orderedPlayers.get(0).attackValue / Math.max(2, gameState.orderedPlayers.size() - 1);
        //early in the game the weakness assessment is too generous as a lot can happen in between turns
        if (checkAnother(gameState,result)
                || (checkAnother2(gameState) || player.getCards().size() < 2)
                && checkAnother3(gameState) && shouldEndAttack(gameState)) {
            return true;
        }
        return result;
    }
    private boolean checkAnother(GameState gameState, boolean result) {
        boolean check = !result && type == PLAYER_AI_HARD
                && gameState.orderedPlayers.size() > 2
                && (gameState.me.defenseValue < 1.2*gameState.orderedPlayers.get(gameState.orderedPlayers.size() - 1).
                defenseValue);
        return check;
    }
    private boolean checkAnother2(GameState gameState) {
        boolean check = ((gameState.commonThreat != null || player.getStatistics().size() < 4));
        return check;
    }
    private boolean checkAnother3(GameState gameState) {
        boolean check = (gameState.me.defenseValue < (game.getMaxDefendDice()==2?1.2:1)*gameState.orderedPlayers.get(0).
                attackValue);
        return check;
    }
    /**
     * Stops non-priority attacks if there is too much pressure
     * @param gameState
     * @return
     */
    protected boolean shouldEndAttack(GameState gameState) {
        if (gameState.orderedPlayers.size() < 2 || type == PLAYER_AI_EASY) {
            return false;
        }
        int defense = gameState.me.defenseValue;
        double sum = 0;
        int size = gameState.orderedPlayers.size();
        for (int i = 0; i < size; i++) {
            sum += gameState.orderedPlayers.get(i).attackValue;
        }
        if (defense > sum) {
            return false;
        }
        double ratio = defense/sum;
        if (ratio < .5) {
            return true;
        }
        //be slightly probabilistic about this decision
        return r.nextDouble() > (ratio-.5)*2;
    }

    /**
     * If the ai should be more aggressive
     * @param gameState
     * @return
     */
    protected boolean pressAttack(GameState gameState) {
        if (this.type == AIDomination.PLAYER_AI_EASY) {
            return r.nextBoolean();
        }
        if (gameState.orderedPlayers.size() < 2) {
            return true;
        }
        int defense = gameState.me.defenseValue;
        double sum = 0;
        int size = gameState.orderedPlayers.size();
        for (int i = 0; i < size; i++) {
            sum += gameState.orderedPlayers.get(i).attackValue;
        }
        return defense > sum;
    }

    /**
     * Find the continents that we're interested in competing for.
     * This is based upon how much we control the continent and weighted for its value.
     */
    private void country_different_player() {

        AttackTarget t = targets.get(country);
        if (t != null) {
            at.add(t);
        }
        enemyTerritories++;
        int toAttack = 0;
        if (gameState.commonThreat == null || gameState.commonThreat.p != country.getOwner()) {
            toAttack += country.getArmies();
        } else {
            //this will draw the attack toward continents mostly controlled by the common threat
            toAttack += country.getArmies()/2;
        }
        if (toAttack >= game.getMaxDefendDice() && (t == null || t.remaining <= 0)) {
            if (game.getMaxDefendDice() == 2) {
                toAttack = 3*toAttack/2;
            } else {
                toAttack *= 2;
            }
        }
        enemyTroops += toAttack;

    }

    private void country_ccn() {

        Country ccn = country.getCrossContinentNeighbours().get(k);
        if (seen.add(ccn)) { //prevent counting the same neighbor multiple times
            if (ccn.getOwner() == player) {
                Player gOw = country.getOwner();
                while(gOw != player) {
                    troops += ccn.getArmies()-1;
                    break;
                }
            } else if (gameState.commonThreat == null) {
                enemyTroops += ccn.getArmies()*.8;
            }
        }
    }


    private void country_owns() {

        Country country = ct.get(j);
        if (country.getOwner() == player) {
            territories++;
            troops += country.getArmies();
        } else {
            country_different_player();
        }
        //account for the immediate neighbours
        if (!country.getCrossContinentNeighbours().isEmpty()) {
            int size = country.getCrossContinentNeighbours().size();
            for (int k = 0; k < size; k++) {
                country_ccn();
            }
        }
    }

    private void ratio_conditions() {
        double ratio = Math.max(1, territories + 2d*troops + player.getExtraArmies()/(game.getSetupDone()?2:3))/(enemyTerritories + 2*enemyTroops);
        int pow = 2;
        if (!game.getSetupDone()) {
            pow = 3;
        }
        if (ratio < .5) {
            if (gameState.commonThreat != null) {
                continue;
            }
            //when we have a low ratio, further discourage using a divisor
            ratio/=Math.pow(Math.max(1, enemyTroops-enemyTerritories), pow);
        } else {
            targetContinents++;
        }
        if (gameState.commonThreat == null) {
            //lessen the affect of the value modifier as you control more continents
            ratio *= Math.pow(getContinentValue(co), 1d/(gameState.me.owned.size() + 1));
        }
    }

    private void list_result() {

        List<Country> ct = co.getTerritoriesContained();
        List<AttackTarget> at = new ArrayList<AttackTarget>();
        int territories = 0;
        int troops = 0;
        int enemyTerritories = 0;
        int enemyTroops = 0;
        seen.clear();
        //look at each country to see who owns it
        int size = ct.size();
        for (int j = 0; j < size; j++) {
            country_owns();
        }
        if (at.isEmpty() && filterNoAttacks) {
            continue; //nothing to attack this turn
        }
        int needed = enemyTroops + enemyTerritories + territories - troops + (attack?game.getMaxDefendDice()*co.getBorderCountries().size():0);
        if (attack && game.isCapturedCountry() && (needed*.8 > troops)) {
            continue; //should build up, rather than attack
        }
        ratio_conditions();
        Double key = Double.valueOf(-ratio);
        int index = Collections.binarySearch(vals, key);
        if (index < 0) {
            index = -index-1;
        }
        vals.add(index, key);
        EliminationTarget et = new EliminationTarget();
        et.allOrNone = false;
        et.attackTargets = at;
        et.co = co;
        et.ps = gameState.orderedPlayers.get(0);
        result.add(index, et);

    }


    private List<EliminationTarget> findTargetContinents(GameState gameState, Map<Country, AttackTarget> targets, boolean attack, boolean filterNoAttacks) {
        Continent[] c = game.getContinents();
        int targetContinents = Math.max(1, c.length - gameState.orderedPlayers.size());
        //step 1 examine continents
        List<Double> vals = new ArrayList<Double>();
        List<EliminationTarget> result = new ArrayList<EliminationTarget>();
        HashSet<Country> seen = new HashSet<Country>();
        int leng = c.length;
        for (int i = 0; i < leng; i++) {
            Continent co = c[i];
            if (gameState.owned[i] != null && (gameState.owned[i] == player || (gameState.commonThreat != null && gameState.commonThreat.p != gameState.owned[i]))) {
                continue;
            }
            list_result();
        }
        if (result.size() > targetContinents) {
            result = result.subList(0, targetContinents);
        }
        return result;
    }


    /**
     * Find the best route (the index in attackable) for the given target selection
     */
    protected int findBestRoute(List<Country> attackable, GameState gameState,
                                boolean attack, Continent targetCo, AttackTarget selection, Player targetPlayer, Map<Country, AttackTarget> targets) {
        int bestRoute = 0;
        Set<Country> bestPath = null;
        int leng = selection.routeRemaining.length;
        for (int i = 1; i < leng; i++) {
            int diff = selection.routeRemaining[bestRoute] - selection.routeRemaining[i];
            Country start = attackable.get(i);
            if (selection.routeRemaining[bestRoute] == Integer.MIN_VALUE && selection.routeRemaining[i] == Integer.MIN_VALUE) {
                bestRoute = i;
            }
            //short sighted check to see if we're cutting off an attack line
            if (attack && selection.routeRemaining[i] >= 0 && diff != 0 && selection.routeRemaining[bestRoute] >= 0) {
                HashSet<Country> path = getPath(selection, targets, i, start);
                if (bestPath == null) {
                    bestPath = getPath(selection, targets, bestRoute, attackable.get(bestRoute));
                }
                HashSet<Country> path1 = new HashSet<Country>(path);
                nextOperation3(bestPath,path1);
                bestRoute = nextOperation2(diff,bestRoute,selection,targetPlayer,i,attack,start,targetCo,attackable);
                nextOperation4(diff,path1,gameState,start,attack,i,path);
            }
        }
        if (selection.routeRemaining[bestRoute] == Integer.MIN_VALUE) {
            return -1;
        }
        return bestRoute;
    }
    private void nextOperation3(Set<Country> bestPath,HashSet<Country> path1) {
        for (Iterator<Country> iter = path1.iterator(); iter.hasNext();) {
            Country attacked = iter.next();
            if (!bestPath.contains(attacked) || attacked.getArmies() > 4) {
                iter.remove();
            }
        }
    }
    private void nextOperation4(int diff,HashSet<Country> path1,GameState gameState,Country start,boolean attack,int i,HashSet<Country> path) {
        Set<Country> bestPath = null;
        int bestRoute = 0;
        if (diff < 0 && !path1.isEmpty()) {
            HashMap<Country, AttackTarget> specificTargets = new HashMap<Country, AttackTarget>();
            searchTargets(specificTargets, start, start.getArmies(), 0, 1, player.getExtraArmies(), attack, Collections.EMPTY_SET, path1, gameState);
            int forwardMin = getMinRemaining(specificTargets, start.getArmies(), false, gameState);
            if (forwardMin > -diff) {
                bestRoute = i;
                bestPath = path;
            }
        } else if (diff > 0 && path1.isEmpty() && start.getArmies() >= 3) {
            bestRoute = i;
            bestPath = path;
        }
    }
    private int nextOperation2(int diff, int bestRoute,AttackTarget selection, Player targetPlayer,int i,boolean attack,Country start,Continent targetCo,
                               List<Country> attackable) {
        int bestR = 0;
        if (diff == 0 && attack) {
            //range planning during attack is probably too greedy, we try to counter that here
            Country start1 = attackable.get(bestRoute);
            int adjustedCost1 = start1.getArmies() - selection.routeRemaining[bestRoute];
            int adjustedCost2 = start.getArmies() - selection.routeRemaining[i];
            if (adjustedCost2 < adjustedCost1 && adjustedCost1 < adjustedCost2) {
                bestRoute = i;
                bestR = nextOperation(diff,bestRoute,selection,targetPlayer,i,attack,start,targetCo);
            }
        }
        return bestR;
    }
    private int nextOperation(int diff, int bestRoute,AttackTarget selection, Player targetPlayer,int i,boolean attack,Country start,Continent targetCo) {
        int bestRout = 0;
        if (isCheckedIf(diff,attack,bestRoute,selection) || (diff == 0 && (isCheckedIf2(selection,targetPlayer,i) ||
                isCheckedIf3(targetPlayer,selection,bestRoute) && start.getContinent() == targetCo))) {
            bestRout = i;
        }
        return bestRout;
    }
    private boolean isCheckedIf3(Player targetPlayer, AttackTarget selection, int bestRoute) {
        boolean isChecked = false;
        if((targetPlayer == null || selection.attackPath[bestRoute].getOwner() != targetPlayer))
            isChecked = true;
        return isChecked;
    }
    private boolean isCheckedIf(int diff, boolean attack, int bestRoute, AttackTarget selection) {
        boolean isChecked = false;
        if((diff < 0 && (!attack || selection.routeRemaining[bestRoute] < 0)))
            isChecked = true;
        return isChecked;
    }
    private boolean isCheckedIf2(AttackTarget selection,Player targetPlayer, int i) {
        boolean isChecked = false;
        if((selection.attackPath[i] != null && selection.attackPath[i].getOwner() == targetPlayer))
            isChecked = false;
        return isChecked;
    }
    /**
     * Get a set of the path from start (exclusive) to the given target
     */
    private HashSet<Country> getPath(AttackTarget at, Map<Country, AttackTarget> targets, int i,
                                     Country start) {
        HashSet<Country> path = new HashSet<Country>();
        Country toAttack = at.targetCountry;
        path.add(toAttack);
        boolean isNei = !start.isNeighbours(toAttack);
        while (isNei) {
            at = targets.get(at.attackPath[i]);
            toAttack = at.targetCountry;
            path.add(toAttack);
        }
        return path;
    }

    /**
     * Return the attack string for the given selection
     */
    protected String getAttack(Map<Country, AttackTarget> targets, AttackTarget selection, int best,
                               Country start) {
        Country toAttack = getCountryToAttack(targets, selection, best, start);
        return "attack " + start.getColor() + " " + toAttack.getColor();
    }

    /**
     * Gets the initial country to attack given the final selection
     */
    private Country getCountryToAttack(Map<Country, AttackTarget> targets, AttackTarget selection,
                                       int best, Country start) {
        Country toAttack = selection.targetCountry;
        boolean isNei = !start.isNeighbours(toAttack);
        while (isNei) {
            selection = targets.get(selection.attackPath[best]);
            toAttack = selection.targetCountry;
        }
        return toAttack;
    }

    /**
     * Simplistic fortification
     * TODO: should be based upon pressure/continent value
     */
    protected String country_borders2() {

        Country c = borders.get(i);
        //this is a hotspot, at least match the immediate troop level
        int diff = additionalTroopsNeeded(c, gs);
        if (diff > 0) {
            return getPlaceCommand(c, Math.min(player.getExtraArmies(), diff));
        }
        if (!minimal && -diff < c.getArmies() + 2) {
            return getPlaceCommand(c, Math.min(player.getExtraArmies(), c.getArmies() + 2 + diff));
        }

    }

    protected String country_borders1() {
        Country c = borders.get(i);
        if (c.getArmies() < min) {
            return getPlaceCommand(c, min - c.getArmies());
        }
    }

    protected String fortify(GameState gs, List<Country> attackable, boolean minimal, List<Country> borders) {
        int min = Math.max(game.getMaxDefendDice(), getMinPlacement());
        //at least put 2, which increases defensive odds
        int size = borders.size();
        for (int i = 0; i < size; i++) {
            String borders1 = country_borders1();
        }
        if (minimal && (!game.getSetupDone() || (isIncreasingSet() && player.getCards().size() > 1))) {
            return null;
        }
        int size2 = borders.size();
        for (int i = 0; i < size2; i++) {
            String borders2= country_borders2();
        }
        return null;
    }


    /**
     * Simplistic (immediate) guess at the additional troops needed.
     */
    protected void country_v() {

        Country n = v.get(j);
        if (n.getOwner() != player) {
            if (minimal) {
                needed = Math.max(needed, n.getArmies());
            } else {
                needed += (n.getArmies() -1);
            }
        }
    }

    protected void country_contains() {

        if (gs.me.owned.contains(cont.getContinent()) && needed > 0) {
            needed += cont.getContinent().getArmyValue();
        } else {
            needed = Math.max(needed, cont.getContinent().getArmyValue()/2);
        }
        break;

    }

    protected int additionalTroopsNeeded(Country c, GameState gs) {
        int needed = 0;
        boolean minimal = !gs.capitals.contains(c);
        List<Country> v = c.getIncomingNeighbours();
        int size = v.size();
        for (int j = 0; j < size; j++) {
            country_v();
        }
        if (!isIncreasingSet() && type != PLAYER_AI_EASY && gs.commonThreat == null && gs.me.playerValue < gs.orderedPlayers.get(0).playerValue) {
            for (Country cont : c.getCrossContinentNeighbours()) {
                if (!gs.me.owned.contains(c.getContinent()) && cont.getArmies() < cont.getContinent().getArmyValue()) {
                    country_contains();
                }
            }
        }
        int diff = needed - c.getArmies();
        return diff;
    }

    protected int getMinPlacement() {
        return 1;
    }

    /**
     * Get the border of my continents, starting with actual borders then the front
     */
    protected List<Country> getBorder(GameState gs) {
        List<Country> borders = new ArrayList<Country>();
        if (gs.me.owned.isEmpty()) {
            //TODO: could look to build a front
            return borders;
        }
        Set<Country> front = new HashSet<Country>();
        Set<Country> visited = new HashSet<Country>();
        for (Iterator<Continent> i = gs.me.owned.iterator(); i.hasNext();) {
            Continent myCont = i.next();
            List<Country> v = myCont.getBorderCountries();
            int size = v.size();
            for (int j = 0; j < size; j++) {
                Country border = v.get(j);
                if (!ownsNeighbours(border) || isAttackable(border)) {
                    borders.add(border);
                } else {
                    if (border.getCrossContinentNeighbours().size() == 1) {
                        Country country = border.getCrossContinentNeighbours().get(0);
                        if (country.getOwner() != player) {
                            borders.add(country);
                            continue;
                        }
                    }
                    List<Country> n = border.getCrossContinentNeighbours();
                    findFront(gs, front, myCont, visited, n);
                }
            }
        }
        borders.addAll(front); //secure borders first, then the front
        return borders;
    }

    private boolean ownsNeighbours(Country c) {
        return ownsNeighbours(player, c);
    }

    /**
     * return true if the country can be attacked
     */
    private boolean isAttackable(Country c) {
        for (Country country : c.getIncomingNeighbours()) {
            if (country.getOwner() != player) {
                return true;
            }
        }
        return false;
    }

    /**
     * Search for the front of my continent
     */
    private void findFront(GameState gs, Set<Country> front, Continent myCont,
                           Set<Country> visited, List<Country> n) {
        Stack<Country> c = new Stack<Country>();
        c.addAll(n);
        boolean emp = !c.isEmpty();
        while (emp) {
            Country b = c.pop();
            if (!visited.add(b)) {
                continue;
            }
            if (b.getOwner() == player && b.getContinent() != myCont) {
                if (gs.me.owned.contains(b.getContinent())) {
                    continue;
                }
                if (isAttackable(b)) {
                    front.add(b);
                } else {
                    c.addAll(b.getNeighbours());
                }
            }
        }
    }

    /**
     * Estimates a baseline value for a continent
     * @param co
     * @return
     */
    protected double getContinentValue(Continent co) {
        int players = 0;
        int size = game.getPlayers().size();
        for (int i = 0; i < size; i++) {
            if (!((Player)game.getPlayers().get(i)).getTerritoriesOwned().isEmpty()) {
                players++;
            }
        }
        int freeContinents = game.getContinents().length - players;
        double continentValue = co.getArmyValue() + co.getTerritoriesContained().size()/3;
        int neighbors = 0;
        int size2 = co.getBorderCountries().size();
        for (int i = 0; i < size2; i++) {
            //TODO: update for 1-way
            neighbors += ((Country)co.getBorderCountries().get(i)).getCrossContinentNeighbours().size();
        }
        continentValue /= Math.pow(2*neighbors - co.getBorderCountries().size(), 2);
        if (freeContinents > co.getBorderCountries().size()) {
            continentValue *= co.getBorderCountries().size();
        }
        return continentValue;
    }

    /**
     * Break continents starting with the strongest player
     */
    private String breakContinent(List<Country> attackable, Map<Country, AttackTarget> targets, GameState gameState, boolean attack, boolean press,
                                  List<Country> borders) {
        List<Continent> toBreak = getContinentsToBreak(gameState);
        if (!attack && type == PLAYER_AI_EASY) {
            return null;
        }
        int size = toBreak.size();
        for (int i = 0; i < size; i++) {
            Continent c = toBreak.get(i);
            Player tp = ((Country)c.getTerritoriesContained().get(0)).getOwner();
            PlayerState ps = null;
            ps = createPS(gameState,tp);
            //find the best territory to attack
            List<Country> t = c.getTerritoriesContained();
            int best = Integer.MAX_VALUE;
            AttackTarget selection = null;
            int bestRoute = 0;
            if (selection != null) {
                Country attackFrom = attackable.get(bestRoute);
                if (best > (3*c.getArmyValue() + 2*selection.targetCountry.getArmies()) && game.getMaxDefendDice() == 2) {
                    //ensure that breaking doesn't do too much collateral damage
                    int value = 3*c.getArmyValue();
                    int collateral = 0;
                    Set<Country> path = getPath(selection, targets, bestRoute, attackFrom);
                    collateral = valueFor(value,gameState,selection,path);
                }
                String result = getMove(targets, attack, selection, bestRoute, attackFrom);
                breaking = c;
                return result;
            }
        }
        return null;
    }
    private int valueFor(int value,GameState gameState,AttackTarget selection,Set<Country> path) {
        int collateral = 0;
        for (Iterator<Country> j = path.iterator(); j.hasNext();) {
            Country attacked = j.next();
            value++;
            if (attacked.getOwner() == selection.targetCountry.getOwner() || gameState.targetPlayers.contains(attacked.getOwner())) {
                value = createValue(attacked);
            } else {
                collateral = createCollateral(attacked);
            }
        }
        return collateral;
    }
    private PlayerState createPS(GameState gameState,Player tp) {
        PlayerState ps = null;
        int size = gameState.orderedPlayers.size();
        for (int j = 0; j < size; j++) {
            ps = gameState.orderedPlayers.get(j);
            if (ps.p == tp) {
                break;
            }
        }
        return ps;
    }
    private int createValue(Country attacked) {
        int value = 0;
        int getMax = game.getMaxDefendDice();
        int gArm = attacked.getArmies();
        while (getMax == 2 || gArm < 3) {
            value += 3*attacked.getArmies()/2 + attacked.getArmies()%2;
            break;
        }

        while(!(getMax == 2 || gArm < 3))  {
            value += 2*attacked.getArmies();
            break;
        }
        return value;
    }
    private int createCollateral(Country attacked) {
        int collateral = 0;
        int getMax = game.getMaxDefendDice();
        int gArmies = attacked.getArmies();
        while (getMax == 2 || gArmies < 3) {
            collateral += 3*attacked.getArmies()/2 + attacked.getArmies()%2;
        }
        while(!(getMax == 2 || gArmies < 3)) {
            collateral += 2*attacked.getArmies();
        }
        return collateral;
    }
    private void cKSzPO(List<Country> t,Map<Country,AttackTarget> t1, 
    		GameState LSt1, boolean CLy1,List<Country> borders,int best,
                              List<Country> b1Y) {
        int size = t.size();
        for (int j = 0; j < size; j++) {
            Country target = t.get(j);
            AttackTarget attackTarget = t1.get(target);
            int route = findBestRoute(b1Y, LSt1, CLy1, null, attackTarget, LSt1.orderedPlayers.get(0).p, t1);
            Country attackFrom = b1Y.get(route);
            int cost = attackFrom.getArmies() - attackTarget.routeRemaining[route];
            createCost(borders,attackFrom);
            createCostOther(cost,best,route,attackTarget);
        }
    }
    private int createCost(List<Country> borders,Country attackFrom) {
        int cost = 0;
        if (borders.contains(attackFrom)) {
            cost += game.getMaxDefendDice();
        }
        return cost;
    }
    /**
     * Get a list of continents to break in priority order
     */

    protected List<Continent> getContinentsToBreak(GameState gs) {
        List<Continent> result = new ArrayList<Continent>();
        List<Double> vals = new ArrayList<Double>();
        int leng = gs.owned.length;
        for (int i = 0; i < leng; i++) {
            if (gs.owned[i] != null && gs.owned[i] != player) {
                Continent co = game.getContinents()[i];
                Double val = Double.valueOf(-getContinentValue(co) * game.getContinents()[i].getArmyValue());
                int index = Collections.binarySearch(vals, val);
                if (index < 0) {
                    index = -index-1;
                }
                vals.add(index, val);
                result.add(index, co);
            }
        }
        return result;
    }

    /**
     * Determine if elimination is possible.  Rather than performing a more
     * advanced combinatorial search, this planning takes simple heuristic passes
     */
    protected String liRemaing(List<Country> Le, Map<Country, AttackTarget> fg, GameState xF,
                               boolean ip, int vTg, Set<Country> aZx, EliminationTarget et,
                               boolean sGv, boolean lowProbability) {
        AttackTarget selection = null;
        int bestRoute = 0;
        String s = null;
        s = r_tyTar(fg,sGv,ip,xF,bestRoute,et,Le);
        //otherwise we use more logic to plan a more complete attack
        //we start with the targets from easiest to hardest and build up the attack paths from there
        Set<Country> countriesTaken = new HashSet<Country>(aZx);
        Set<Country> placements = new HashSet<Country>();
        int bestCost = Integer.MAX_VALUE;
        Collections.sort(et.attackTargets, Collections.reverseOrder());
        HashSet<Country> toTake = new HashSet<Country>();
        toTakeAdd(et,aZx,toTake);
        int size = et.attackTargets.size();
        boolean emp = !toTake.isEmpty();
        for (int i = 0; i < size && emp; i++) {
            AttackTarget attackTarget = et.attackTargets.get(i);
            Country attackFrom = null;
            int route = 0;
            boolean clone = true;
            Set<Country> path = null;
            int pathRemaining = 0;
            boolean closeWhile = true;
            while (closeWhile) {
                route = findBestRoute(Le, xF, ip, null, attackTarget, et.ps.p, fg);
                co_SelRema(vTg,ip,xF,attackTarget,toTake,countriesTaken,route,lowProbability,et,fg,path,attackFrom,placements,
                        pathRemaining,Le);
                cl_reRoute(attackTarget,clone);
                attackTarget.routeRemaining[route] = Integer.MIN_VALUE;
            }
            //process the path found and update the countries take and what to take
           removeToTake(toTake,countriesTaken,path);
            nested(pathRemaining,vTg,selection,attackFrom,ip,attackTarget,bestCost,route);
            placements.add(attackFrom);
        }
        Country attackFrom = Le.get(bestRoute);
        String result = getMove(fg, ip, selection, bestRoute, attackFrom);
        if (result != null) {
            aZx.addAll(countriesTaken);
        }
        return result;
    }
    private String co_SelRema(int kxu,boolean aT,GameState gBh, AttackTarget ik,
                              HashSet<Country> toTake,Set<Country> zE,int route, boolean hX,
                              EliminationTarget et,Map<Country,AttackTarget> kN,Set<Country> pT,Country mFr,Set<Country> plG,
                                int pyIn,List<Country> cV) {
        String s = null;
        if (route == -1) {
            s = null;
        }
        mFr = cV.get(route);
        if (!plG.contains(mFr)) {
            pyIn = ik.routeRemaining[route];
            routMapRe(pyIn,kxu,aT,gBh,ik,toTake,zE,route,hX,et,kN,pT,mFr);
        }
        return s;
    }
    private void toTakeAdd(EliminationTarget et,Set<Country> allCountriesTaken,HashSet<Country> toTake) {
        int size = et.attackTargets.size();
        for (int i = 0; i < size; i++) {
            AttackTarget at = et.attackTargets.get(i);
            if (!allCountriesTaken.contains(at.targetCountry)) {
                toTake.add(at.targetCountry);
            }
        }
    }
    private void cl_reRoute(AttackTarget tVz,boolean Cl) {
        if (Cl) {
            //clone the attack target so that the find best route logic can have a path excluded
            tVz = tVz.clone();
            tVz.routeRemaining = ArraysCopyOf(tVz.routeRemaining, tVz.routeRemaining.length);
            Cl = false;
        }
    }
    private void routMapRe(int pIfg, int rTin,boolean Apl,GameState bSt, AttackTarget uJk,
                         HashSet<Country> cPo,Set<Country> cTu,int route, boolean yLk,
                         EliminationTarget et,Map<Country,AttackTarget> targets,Set<Country> path,Country kBy) {
        if (paRemaArmi(pIfg,rTin,uJk,kBy)
                && checkAnotherPath(bSt,targets,route,et,Apl,uJk,kBy)) {
            //TODO this is a choice point if there is more than 1 valid path
            path = getPath(uJk, targets, route, kBy);
            colGTak(pIfg,rTin,Apl,bSt,uJk,cPo,cTu,route,yLk,path);
        } else if (attackAllIn(et) && t_Va(uJk,rTin,bSt)) {
            //allow hard players to always pursue a single country elimination
            path = getPath(uJk, targets, route, kBy);
        }
    }
    private void colGTak(int fx, int rnm,boolean sxc,GameState paz, AttackTarget yv,
                              HashSet<Country> ttK,Set<Country> vT,int rtth, boolean lPp,Set<Country> path) {
        if (Collections.disjoint(path, vT)) {
            //check to see if we can append this path with a nearest neighbor path
            exHaCo_Set(fx,rnm,sxc,paz,yv,ttK,vT,path,rtth,lPp);
        }
    }
    private boolean iEmaGe(GameState g1,Map<Country,AttackTarget> tv1,
    		int route,EliminationTarget et,boolean v8f,
                                     AttackTarget k9z,Country xt) {
        boolean check = false;
        if((et.allOrNone || ps_R_Cou(g1, tv1, route, k9z, xt, et, v8f)))
            check = true;
        return check;
    }
    private boolean paRemaArmi(int pin, int rg,AttackTarget ac,Country xy) {
        boolean check = false;
        if((pin + rg >= 1
                || (ac.remaining + rg >= 2 && xy.getArmies() + rg >= 4)))
            check = true;
        return check;
    }
    private void exHaCo_Set(int zs, int r1,boolean l9,GameState x2, AttackTarget th5,
                         HashSet<Country> toTake,Set<Country> d,Set<Country> ptg, int route, boolean lw) {
        int sum = zs + r1;
        while (sum >= 3) {
            HashSet<Country> exclusions = new HashSet<Country>(d);
            exclusions.addAll(ptg);
            Map<Country, AttackTarget> newTargets = new HashMap<Country, AttackTarget>();
            searchTargets(newTargets, th5.targetCountry, zs, 0, 1, r1,
                    lw?true:l9, toTake, exclusions, x2);
            //find the best fit new path if one exists
            AttackTarget newTarget = null;
            attackTarget(zs,r1,newTargets,toTake);
            v_AlidS(newTarget,ptg,newTargets,th5,route,zs);
        }
    }
    private boolean t_Va(AttackTarget tKo,int rImn, GameState gMi) {
        boolean check = false;
        if(tKo.remaining + rImn > -tKo.targetCountry.getArmies()
                && gMi.me.playerValue < gMi.orderedPlayers.get(0).playerValue)
            check = true;
        return check;
    }
    private boolean attackAllIn(EliminationTarget et) {
        boolean check = false;
        if(et.allOrNone && et.attackTargets.size() == 1
                && type == PLAYER_AI_HARD)
            check = true;
        return check;
    }
    private void v_AlidS(AttackTarget ng,Set<Country> path,Map<Country, AttackTarget> tj8,
                                AttackTarget tj1x, int route, int pH7) {
        while (ng != null) {
            path.addAll(getPath(ng, tj8, 0, tj1x.targetCountry));
            tj1x.routeRemaining[route] = pH7;
            break;
        }
    }
    private void RAttXh(int pRe, int rm,AttackTarget sn,Country Af,
    		boolean k1,AttackTarget gT, int bestCost,
                        int route) {
        if (pRe < 1) {
            rm += pRe -1;
        }
        int cost = Af.getArmies() - pRe;
        if (sn == null || (k1 && cost < bestCost && cost > 0)) {
            sn = gT;
            bestCost = cost;
            int bestRoute = route;
        }
    }
    private void attackTarget(int pathRemaining,int remaining,Map<Country, AttackTarget> newTargets,HashSet<Country> toTake) {
        for (Iterator<AttackTarget> j = newTargets.values().iterator(); j.hasNext();) {
            AttackTarget next = j.next();
            boolean targ = toTake.contains(next.targetCountry);
            int rout = next.routeRemaining[0];
            int sum = rout + remaining;
            while(targ && rout < pathRemaining && sum >= 1) {
                pathRemaining = next.routeRemaining[0];
                AttackTarget newTarget = next;
                break;
            }
        }
    }
    private void removeToTake(HashSet<Country> toTake,Set<Country> countriesTaken,Set<Country> path) {
        for (Iterator<Country> j = path.iterator(); j.hasNext();) {
            Country c = j.next();
            countriesTaken.add(c);
            toTake.remove(c);
        }
    }
    private String r_tyTar(Map<Country,AttackTarget> tj, boolean hn, boolean Jp,GameState gJ1,
                             int bestRoute, EliminationTarget et,List<Country> Bl) {
        String s = null;
        AttackTarget selection = null;
        if (type == PLAYER_AI_EASY || (type == PLAYER_AI_AVERAGE && !et.allOrNone && r.nextInt(3) != 0) ||
                (!et.allOrNone && !et.target && hn && Jp)) {
            //just be greedy, take the best (least costly) attack first
            int size = et.attackTargets.size();
            for (int i = 0; i < size; i++) {
                AttackTarget at = et.attackTargets.get(i);
                int route = findBestRoute(Bl, gJ1, Jp, null, at, et.ps.p, tj);
                Country attackFrom = Bl.get(route);
                if ((checkRouteRem(at,route,selection,bestRoute) || checkRem(at,gJ1,selection,attackFrom,route,tj,et,Jp))) {
                    selection = at;
                    bestRoute = route;
                }
            }
            s = getMove(tj, Jp, selection, bestRoute, Bl.get(bestRoute));
        }
        return s;
    }
    private boolean checkRem(AttackTarget at,GameState gameState, AttackTarget selection,Country attackFrom,int route,Map<Country,AttackTarget> targets,
                             EliminationTarget et,boolean attack) {
        boolean check = false;
        if((at.remaining > 1 && attackFrom.getArmies() > 3 && (selection != null && at.remaining < selection.remaining))
                        && ps_R_Cou(gameState, targets, route, at, attackFrom, et, attack))
            check = true;
        return check;
    }
    private boolean checkRouteRem(AttackTarget at,int route,AttackTarget selection,int bestRoute) {
        boolean check = false;
        if((at.routeRemaining[route] > 0 && (selection == null || at.routeRemaining[route] <
                selection.routeRemaining[bestRoute] || selection.routeRemaining[bestRoute] < 1)))
            check = true;
        return check;
    }
    /**
     * @see Arrays#copyOf(int[], int)
     */
    public static int[] ArraysCopyOf(int[] original, int newLength) {
        int[] copy = new int[newLength];
        System.arraycopy(original, 0, copy, 0,
                Math.min(original.length, newLength));
        return copy;
    }

    /**
     * ensure that we're not doing something stupid like breaking using too many troops for too little reward or pushing a player to elimination
     */

    protected boolean get_owner() {
        int size = c.getOwner().getCards().size();
        int p = ps.attackOrder;
        while(p == 1 && size > 3) {
            return true;
        }
        boolean isInc = isIncreasingSet();
        int plaV = gameState.me.playerValue;
        int playV = gameState.orderedPlayers.get(0).playerValue;
        int val = game.getNewCardState();
        int val2 = gameState.me.defenseValue;
        while(type == PLAYER_AI_HARD && isInc
                && plaV < playV
                && val > val2) {
            return true; //you're loosing so just do whatever
        }
        PlayerState top = gameState.orderedPlayers.get(0);
        int val3 = ps.defenseValue - 5*c.getArmies()/4 - c.getArmies()%4 - 1;
        int val4 = (int)(2*(top.attackValue - top.armies/3)/3);
        while(val3 < val4) {
            return false;
        }
        break;

    }

    protected void get_mission() {
        int size = gameState.orderedPlayers.size() - 1;
        for (int i = size; i >= 0; i--) {
            PlayerState ps = gameState.orderedPlayers.get(i);
            if (ps.playerValue >= gameState.me.playerValue) {
                break;
            }
            if (ps.p == c.getOwner()) {
                boolean owner= get_owner();
            }
        }
    }

    protected void player_mission() {

        if (player.getMission() != null || ((attack|| isIncreasingSet()) && (c.getOwner().getCards().size() > 1 || (c.getOwner().getCards().size() == 1 && game.getCards().isEmpty())))) {
            get_mission();
        }
    }

    protected boolean game_conditons() {

        if (player.getMission() == null && game.getCardMode() == RiskGame.CARD_ITALIANLIKE_SET && c.getOwner().getCards().size() < 4|| gameState.commonThreat != null && c.getOwner().getCards().size() <= 2) {
            return true;
        }
        if (gameState.commonThreat != null && c.getOwner() != gameState.commonThreat.p && c.getContinent().getOwner() != null) {
            return false;
        }
        player_mission();

    }

    protected boolean ps_R_Cou(GameState gUb, Map<Country, AttackTarget> zN, int route, AttackTarget uG, Country fgr, EliminationTarget et, boolean v) {
        Country c = getCountryToAttack(zN, uG, route, fgr);
        if (gUb.orderedPlayers.size() > 1 && (et == null || et.ps == null || c.getOwner() != et.ps.p) && !gUb.targetPlayers.contains(c.getOwner())) {
            game_conditions();
        }
        return true;
    }


    /**
     * Gets the move (placement or attack) or returns null if it's not a good attack
     */
    private String getMove(Map<Country, AttackTarget> targets, boolean attack, AttackTarget selection,
                           int route, Country attackFrom) {
        if (selection == null) {
            return null;
        }
        if (attack) {
            if (attackFrom.getArmies() < 5 && selection.remaining < 1) {
                Country toAttack = getCountryToAttack(targets, selection, route, attackFrom);
                if (toAttack.getArmies() >= attackFrom.getArmies()) {
                    return null;
                }
            }
            return getAttack(targets, selection, route, attackFrom);
        }
        if (selection.remaining < 1 || selection.routeRemaining[route] < 2) {
            return getPlaceCommand(attackFrom, -selection.routeRemaining[route] + 2);
        }
        return null;
    }

    /**
     * find the possible elimination targets in priority order
     * will filter out attacks that seem too costly or if the target has no cards
     */
    private void check_troops() {

        Country target = targetCountries.get(j);
        AttackTarget attackTarget = targets.get(target);
        if (attackTarget == null
                || attackTarget.remaining == Integer.MIN_VALUE
                || (!attack && -attackTarget.remaining > remaining)) {
            continue;
        }
        et.attackTargets.add(attackTarget);
    
    }
    
    private void if_target1() {
    	if ((!isIncreasingSet() || game.getNewCardState() < gameState.me.defenseValue/8) && (!attack || player2.getTerritoriesOwned().size() > 1) && !game.getCards().isEmpty() && cardCount < 3 && (game.getCardMode()==RiskGame.CARD_ITALIANLIKE_SET||(cardCount+player.getCards().size()<RiskGame.MAX_CARDS))) {
            divisor+=(.5*Math.max(0, isIncreasingSet()?2:4 - cardCount));
        }
    }
    
    private void if_target2() {
    	if ((!isIncreasingSet() || game.getNewCardState() < gameState.me.defenseValue/8) && (!attack || player2.getTerritoriesOwned().size() > 1) && !game.getCards().isEmpty() && cardCount < 3 && (game.getCardMode()==RiskGame.CARD_ITALIANLIKE_SET||(cardCount+player.getCards().size()<RiskGame.MAX_CARDS))) {
            divisor+=(.5*Math.max(0, isIncreasingSet()?2:4 - cardCount));
    	}
    }
    
    
    private void elimination_target() {

        PlayerState ps = gameState.orderedPlayers.get(i);
        Player player2 = ps.p;

        if ((player2.getCards().isEmpty() && player2.getTerritoriesOwned().size() > 1) || ps.defenseValue > gameState.me.attackValue + player.getExtraArmies()) {
            continue;
        }

        boolean isTarget = gameState.targetPlayers.size() > 1 && gameState.targetPlayers.get(0) == player2;
        double divisor = 1;
        int cardCount = player2.getCards().size();
        if_target1();
        if_target2();

        List<Country> targetCountries = player2.getTerritoriesOwned();
        EliminationTarget et = new EliminationTarget();
        et.ps = ps;
        //check for sufficient troops on critical path
        int size = targetCountries.size();
        for (int j = 0; j < size; j++) {
        	check_troops();
        }
        et.target = isTarget;
        et.allOrNone = true;
        toEliminate.add(et);
    
    }
    
    private List<EliminationTarget> findEliminationTargets(Map<Country, AttackTarget> targets, GameState gameState,
                                                           boolean attack, int remaining) {
        List<EliminationTarget> toEliminate = new ArrayList<EliminationTarget>();
        int size = gameState.orderedPlayers.size();
        for (int i = 0; i < size; i++) {
        	elimination_target();
        }
        return toEliminate;
    }

    private void searchTargets(Map<Country, AttackTarget> targets, Country startCountry, int startArmies, final int start, int totalStartingPoints, int extra, boolean attack, GameState gs) {
        searchTargets(targets, startCountry, startArmies, start, totalStartingPoints, extra, attack, Collections.EMPTY_SET, Collections.EMPTY_SET, gs);
    }

    /**
     * search using Dijkstra's algorithm
     * If the way points are set, then we're basically doing a traveling salesman nearest neighbor heuristic.
     * the attack parameter controls cost calculations
     *  - true neutral
     *  - false slightly pessimistic
     */
    private void searchTargets(Map<Country, AttackTarget> targets, Country startCountry, int startArmies, final int start,
                               int totalStartingPoints, int extra, boolean attack, final Set<Country> wayPoints,
                               final Set<Country> exclusions, GameState gameState) {
        PriorityQueue<AttackTarget> remaining = new PriorityQueue<AttackTarget>(11, new Comparator<AttackTarget>() {
            @Override
            public int compare(AttackTarget o1, AttackTarget o2) {
                int diff = o2.routeRemaining[start] - o1.routeRemaining[start];
                diff = checkCompare(o1,o2,wayPoints,exclusions,start);
                return diff;
            }
        });
        method(gameState);
        AttackTarget at = new AttackTarget(totalStartingPoints, startCountry);
        at.routeRemaining[start] = startArmies;
        remaining.add(at);
        boolean empt = !remaining.isEmpty();
        while (empt) {
            AttackTarget current = remaining.poll();
            method2(targets,wayPoints,current,start,exclusions,startCountry,remaining);
            int attackForce = current.routeRemaining[start];
            attackForce -= getMinPlacement();
            attackForce -= Math.min(current.targetCountry.getArmies()/(attack?3:2), current.depth);
            if (attackForce + extra < 1) {
                break;
            }
            List<Country> v = current.targetCountry.getNeighbours();
            int size = v.size();
            for (int i = 0; i < size; i++) {
                Country c = v.get(i);
                AttackTarget cumulativeForces = targets.get(c);
                if (cumulativeForces == null && exclusions.contains(c)) {
                    cumulativeForces = new AttackTarget(totalStartingPoints, c);
                    targets.put(c, cumulativeForces);
                }
                cumulativeForces.depth = current.depth+1;
                int available = attackForce;
                int toAttack = c.getArmies();
                int[] toArray = createAvailableToAttack(attack,available,toAttack,gameState);
                available = toArray[0];
                toAttack = toArray[1];
                available = createAvailable(attack,available,toAttack);
                cumulativeForces.attackPath[start] = current.targetCountry;
                cumulativeForces.routeRemaining[start] = available;
                cumulativeChack(cumulativeForces, available);
                remaining.add(cumulativeForces);
            }
        }
    }
    private int compare1(Set<Country> wayPoints,Set<Country> exclusions,int start){
        AttackTarget o1 = null;
        AttackTarget o2 = null;
        int returnValue = -1;
        if (wayPoints.contains(o2.targetCountry)) {
            int outs1 = neighboursOpen(o1.targetCountry,exclusions);
            int outs2 = neighboursOpen(o2.targetCountry,exclusions);
            returnValue = checkReturnValue(o1,o2,start,outs1,outs2);
            int diff = o2.routeRemaining[start] - o1.routeRemaining[start];
            return diff + 2*(outs1 - outs2);
        }
        return returnValue;
    }
    private int checkCompare(AttackTarget o1,AttackTarget o2,Set<Country> wayPoints,Set<Country> exclusions, int start) {
        int value = 0;
        while(type == PLAYER_AI_HARD) {
            //heuristic improvement for hard players.
            //give preference to waypoints based upon presumed navigation order
            if(wayPoints.contains(o1.targetCountry)) {
                compare1(wayPoints, exclusions,start);
            }
            if (wayPoints.contains(o2)) {
                value = 1;
            }
        }
        return value;
    }
    private int neighboursOpen(Country c,Set<Country> exclusions) {
        List<Country> neighbours = c.getNeighbours();
        int count = 0;
        int size = neighbours.size();
        for (int i=0; i<size; i++) {
            if ( neighbours.get(i).getOwner() != player && !exclusions.contains(c)) {
                count++;
            }
        }
        return count;
    }
    private int checkReturnValue(AttackTarget o1, AttackTarget o2,int start, int outs1,int outs2) {
        int value = 0;
        while((outs1 == 1)&&(outs2 == 1)) {
            if (outs2 == 1) {
                int diff = o2.routeRemaining[start] - o1.routeRemaining[start];
                //TODO: handle terminal navigation better
                value = -diff; //hardest first
            }
            value = 1;
        } while(outs2 == 1) {
            value = -1;
        }
        return value;
    }
    private int[] createAvailableToAttack(boolean attack, int available, int toAttack, GameState gameState) {
        int[] array = new int[2];
        final int TO_ATTACK = 0;
        final int AVAILABLE = 1;
        if (checkDefence2() || checkDefence(gameState) || checkDefence3(gameState)) {
            int[] toArray = createAttack(attack,toAttack,available);
            array[TO_ATTACK] = toArray[0];
            array[AVAILABLE] = toArray[1];
        } else {
            //assume 3
            int[] toArray = createAttack2(attack,toAttack,available);
            array[TO_ATTACK] = toArray[0];
            array[AVAILABLE] = toArray[1];
        }
        return array;
    }
    private boolean checkDefence3(GameState gameState) {
        boolean check = false;
        if(gameState.me.p.getType() == PLAYER_AI_EASY)
            check = true;
        return check;
    }
    private boolean checkDefence(GameState gameState) {
        boolean check = false;
        if(gameState.me.playerValue > gameState.orderedPlayers.get(0).playerValue)
            check = true;
        return check;
    }
    private boolean checkDefence2() {
        boolean check = false;
        if(game.getMaxDefendDice() == 2)
            check = true;
        return check;
    }
    private void cumulativeChack(AttackTarget cumulativeForces, int available) {
        if (cumulativeForces.remaining>=0 && available>=0) {
            cumulativeForces.remaining = cumulativeForces.remaining += available;
        } else {
            cumulativeForces.remaining = Math.max(cumulativeForces.remaining, available);
        }
    }
    private int createAvailable(boolean attack, int available, int toAttack) {
        int ava = 0;
        if (attack && available == toAttack + 1 && toAttack <= 2) {
            ava = 1; //special case to allow 4 on 2 and 3 on 1 attacks
        } else {
            if (game.getMaxDefendDice() == 2 || toAttack <= 2) {
                ava = available - 3*toAttack/2 - toAttack%2;
            } else {
                ava = available - 2*toAttack;
            }
        }
        return ava;
    }
    private int[] createAttack(boolean attack, int toAttack, int available) {
        int[] array = new int[2];
        final int TO_ATTACK = 0;
        final int AVAILABLE = 1;
        if (attack) {
            while (toAttack >= 10 || (available >= 10 && toAttack >= 5)) {
                array[TO_ATTACK] -= 4;
                array[AVAILABLE] -= 3;
            }
        }
        while (toAttack >= 5 || (available >= 5 && toAttack >= 2)) {
            array[TO_ATTACK] -= 2;
            array[AVAILABLE] -= 2;
        }
        return array;
    }
    private int[] createAttack2(boolean attack, int toAttack, int available) {
        int[] array = new int[2];
        final int TO_ATTACK = 0;
        final int AVAILABLE = 1;
        if (attack) {
            int rounds = (toAttack - 3)/3;
            if (rounds > 0) {
                array[TO_ATTACK] -= 3*rounds;
                array[AVAILABLE] -= 3*rounds;
            }
        }
        return array;
    }
    private void method2(Map<Country,AttackTarget> targets,Set<Country> wayPoints,AttackTarget current,int start,Set<Country> exclusions,Country startCountry,
                         PriorityQueue<AttackTarget> remaining) {
        if (wayPoints.contains(current)) {
            Set<Country> path = getPath(current, targets, start, startCountry);
            exclusions.addAll(path);
            startCountry = current.targetCountry;
            targets.keySet().retainAll(exclusions);
            remaining.clear();
            remaining.add(current);
        }
    }
    private void method(GameState gameState) {
        boolean attack = true;
        if (type == PLAYER_AI_HARD) {
            double ratio = gameState.me.playerValue / gameState.orderedPlayers.get(0).playerValue;
            if (ratio < .4) {
                attack = false; //we're loosing, so be more conservative
            }
        } else if (type == PLAYER_AI_EASY) {
            attack = false; //over estimate
        }
    }
    public String getBattleWon() {
        GameState gameState = getGameState(player, false);
        return getBattleWon(gameState);
    }

    /**
     * Compute the battle won move.  We are just doing a quick reasoning here.
     * Ideally we would consider the full state of move all vs. move min vs. some mix.
     */ 
    protected String getBattleWon5(){
        GameState gameState = getGameState(player, false);
          Continent cont = null;
           int needed = -game.getAttacker().getArmies();
           boolean specialCase = false;
         if(cont != null) {
                if (cont.getBorderCountries().size() > 2) {
                    needed += cont.getArmyValue();
                } else {
                    int card = game.getCardMode();
                    int fix = RiskGame.CARD_FIXED_SET;
                    while(specialCase && card == fix) {
                        needed = additionalTroopsNeeded(game.getAttacker(), gameState);
                        break;
                    }
                    needed += (4 * cont.getArmyValue())/Math.max(1, cont.getBorderCountries().size());
                }
            } else if (specialCase) {
                needed += game.getMaxDefendDice();
            }
          return getBattleWon(gameState);
    }
    protected String getBattleWon4(){
        GameState gameState = getGameState(player, false);
         Continent cont = null;
         boolean specialCase = false;
         List<Country> border = getBorder(gameState);
            if (!border.contains(game.getDefender()) || !gameState.me.owned.contains(game.getDefender().getContinent())) {
                //check if the attacker neighbours one of our continents
                for (Country c : game.getAttacker().getCrossContinentNeighbours()) {
                    if (gameState.me.owned.contains(c.getContinent())) {
                        cont = c.getContinent();
                        specialCase = true;
                        break;
                    }
                }
            }
             return getBattleWon(gameState);
    }
    protected String getBattleWon3(){
        GameState gameState = getGameState(player, false);
        if (game.getAttacker().getArmies() - 1 > game.getMustMove()) {
             int forwardMin = 0;
            Country defender = game.getDefender();
            HashMap<Country, AttackTarget> targets = new HashMap<Country, AttackTarget>();
            searchTargets(targets, defender, game.getAttacker().getArmies() - 1, 0, 1, player.getExtraArmies(), true, gameState);
            forwardMin = getMinRemaining(targets,  game.getAttacker().getArmies() - 1, border.contains(game.getAttacker()), gameState);
            if (forwardMin == Integer.MAX_VALUE) {
                return "move " + game.getMustMove();
            }
        }
        return getBattleWon(gameState);
    }
    protected String getBattleWon2(){
        GameState gameState = getGameState(player, false);
        List<Country> border = getBorder(gameState);
        Continent cont = null;
        boolean specialCase = false;
        if (border.contains(game.getAttacker())) {
                specialCase = true;
                if (cont != null && game.getCardMode() == RiskGame.CARD_FIXED_SET && border.contains(game.getDefender()) && game.getDefender().getContinent() == game.getAttacker().getContinent()) {
                    cont = null;
                    specialCase = false;
                } else if (gameState.me.owned.contains(game.getAttacker().getContinent())) {
                    cont = game.getAttacker().getContinent();
                }
            }
        return getBattleWon(gameState);
    }
    protected String getBattleWon1(){
        GameState gameState = getGameState(player, false);
        boolean specialCase = false;
        int needed = -game.getAttacker().getArmies();
        if (specialCase && ((breaking != null && breaking.getOwner() != null) || gameState.commonThreat != null)) {
            needed/=2;
        }
        
        return getBattleWon(gameState);
    }
    protected String getBattleWon0(){
         GameState gameState = getGameState(player, false);
        boolean specialCase = false;
        int needed = -game.getAttacker().getArmies();
        if (!specialCase && ownsNeighbours(game.getAttacker())) {
            return "move " + Math.max(game.getMustMove(), game.getAttacker().getArmies() - getMinPlacement());
        }
        if (!specialCase && game.getMaxDefendDice() == 3 && !ownsNeighbours(game.getAttacker()) && gameState.me.playerValue > gameState.orderedPlayers.get(0).playerValue) {
            needed += game.getMaxDefendDice(); //make getting cards more difficult
        }
    }
    protected String getBattleWon(GameState gameState) {
        if (ownsNeighbours(game.getDefender())) {
            return "move " + game.getMustMove();
        }
        int needed = -game.getAttacker().getArmies();
        List<Country> border = getBorder(gameState);

        boolean specialCase = false;
        if (!isIncreasingSet() && !eliminating && type != PLAYER_AI_EASY) {
            /*
             * we're not in the middle of a planned attack, so attempt to fortify on the fly
             */
            getBattleWon4();
            getBattleWon2();
            int getCa = game.getCardMode();
            int fix = RiskGame.CARD_FIXED_SET;
            while(specialCase && getCa != fix) {
                needed = additionalTroopsNeeded(game.getAttacker(), gameState);
                break;
            }
            getBattleWon5();
           
        }
         getBattleWon1();
         getBattleWon0();
         getBattleWon3();
        
        return "move " + Math.max(Math.min(-needed, game.getAttacker().getArmies() - Math.max(getMinPlacement(), forwardMin)), game.getMustMove());
    }

    /**
     * Get an estimate of the remaining troops after taking all possible targets
     */

    private void attack_target() {

        AttackTarget attackTarget = i.next();
        if (attackTarget.remaining < 0 && !isBorder) {
            return 0;
        }
        //estimate a cost for the territory
        total += 1;
        if (game.getMaxDefendDice() == 2 || attackTarget.targetCountry.getArmies() < 3) {
            total += attackTarget.targetCountry.getArmies();
            if (attackTarget.targetCountry.getArmies() < 2) {
                total += attackTarget.targetCountry.getArmies();
            }
        } else {
            total += 2*attackTarget.targetCountry.getArmies();
        }
    }

    private int if_attack_target() {

        if (game.getMaxDefendDice() == 2) {
            forwardMin -= (total *= 1.3);
        } else {
            forwardMin -= total;
        }
        if (type == PLAYER_AI_HARD && !isIncreasingSet() && isBorder && isTooWeak(gameState)) {
            //TODO: let the hard player lookahead further, alternatively just call to plan(true) and mark if we are doing an elimination or something
            return Integer.MAX_VALUE;
        }
    }

    private int getMinRemaining(HashMap<Country, AttackTarget> targets, int forwardMin, boolean isBorder, GameState gameState) {
        int total = 0;
        for (Iterator<AttackTarget> i = targets.values().iterator(); i.hasNext();) {
            attack_target();
        }
        
        int if_attack =if_attack_target();

        return Math.max(isBorder?game.getMaxDefendDice():0, forwardMin);
    }


    /**
     * Takes several passes over applicable territories to determine the tactical move.
     * 1. Find all countries with more than the min placement and do the best border fortification possible.
     *  1.a. If there is a common threat see if we can move off of a continent we don't want
     * 2. Move the most troops to the battle from a non-front country.
     * 3. just move from the interior - however this doesn't yet make a smart choice.
     */
    public void target_null() {

        List<EliminationTarget> co = findTargetContinents(gs, Collections.EMPTY_MAP, false, false);
        targetContinents = new ArrayList<Continent>();
        int size = co.size();
        for (int k = 0; k < size; k++) {
            EliminationTarget et = co.get(k);
            targetContinents.add(et.co);
        }
    
    }
    
    public String other_if() {
    	 
         if ((indexOther > -1 && (index == -1 || index > indexOther)) || ((index == -1 || index > 0) && n.getContinent().getOwner() == player)) {
             int toSend = c.getArmies() - getMinPlacement();
             return getMoveCommand(c, n, toSend);
         }
    }
    
    public void if_check_operations() {
    	 if (targetContinents == null) {
    		 target_null();
    	 }
    	 int index = targetContinents.indexOf(c.getContinent());
         if (index == -1 && c.getContinent().getOwner() == player) {
             break;
         }
         int indexOther = targetContinents.indexOf(n.getContinent());
        String other_if = other_if();
        
    }
    
    public void check_cooperations() {

        Country n = (Country)c.getNeighbours().get(j);
        if (n.getOwner() == player && n.getContinent() != c.getContinent()) {
            //we have another continent to go to, ensure that the original continent is not desirable
           if_check_operations();
    
        }
    
    }
    
    public void get_neighbours() {

        Country n = (Country)c.getNeighbours().get(j);
        if (n.getOwner() != player || !v.contains(n) || additionalTroopsNeeded(n, gs) < -1) {
            continue;
        }
        int total = -score + scoreCountry(n);
        if (total < lowestScore) {
            sender = c;
            receiver = n;
            lowestScore = total;
        }
    
    }
    
    public void cooperation_check() {

    	if (c.getArmies() > 2 && gs.commonThreat != null && c.getCrossContinentNeighbours().size() > 0 && !ownsNeighbours(c)) {
    	    int size = c.getNeighbours().size();
            for (int j = 0; j < size; j++) {
            	check_cooperations();
            }
        }
    }
    
    public void move_battle() {

        Country c = t.get(i);
        if (c.getArmies() <= getMinPlacement() || gs.capitals.contains(c)) {
            continue;
        }
        //cooperation check to see if we should leave this continent
        cooperation_check();
        if (v.contains(c) && additionalTroopsNeeded(c, gs)/2 + getMinPlacement() >= 0) {
            continue;
        }
        filtered.add(c);
        int score = scoreCountry(c);
        int size = c.getNeighbours().size();
        for (int j = 0; j < size; j++) {
        	get_neighbours();
        }
    
    }
    
    public void move_to_battle() {

        Country c = filtered.get(i);
        if (!ownsNeighbours(c)) {
            filtered.remove(i);
            continue;
        }
        if (max == null || c.getArmies() > max.getArmies()) {
            max = c;
        }
        int score = scoreCountry(c);
        int size = c.getNeighbours().size();
        for (int j = 0; j < size; j++) {
            Country n = (Country)c.getNeighbours().get(j);
            if (n.getOwner() != player || ownsNeighbours(n)) {
                continue;
            }
            int total = -score + scoreCountry(n);
            if (total < lowestScore) {
                sender = c;
                receiver = n;
                lowestScore = total;
            }
        }
    
    }
    
   public String move_interior() {

       int least = Integer.MAX_VALUE;
       int size = max.getNeighbours().size();
       for (int j = 0; j < size; j++) {
           Country n = (Country)max.getNeighbours().get(j);
           if (max.getOwner() != player) {
               continue;
           }
           if (n.getArmies() < least) {
               receiver = n;
               least = n.getArmies();
           }
       }
       if (receiver != null) {
           return getMoveCommand(max, receiver,  (max.getArmies() - getMinPlacement() - 1));
       }
   
   }
   
   
    public String getTacMove() {
        List<Country> t = player.getTerritoriesOwned();
        Country sender = null;
        Country receiver = null;
        int lowestScore = Integer.MAX_VALUE;
        GameState gs = getGameState(player, false);
        //fortify the border
        List<Country> v = getBorder(gs);
        List<Country> filtered = new ArrayList<Country>();

        List<Continent> targetContinents = null;
        int size = t.size();
        for (int i = 0; i < size; i++) {
        	move_battle();
        }
        //move to the battle
        Country max = null;
        int size2 = filtered.size() - 1;
        for (int i = size2; i >= 0; i--) {
        	move_to_battle();
        }
        //move from the interior (not very smart)
        if (max != null && max.getArmies() > getMinPlacement() + 1) {
        	String move_interior=move_interior();
        }
        return "nomove";
    }

    private String getMoveCommand(Country sender, Country receiver, int toSend) {
        return "movearmies " + sender.getColor() + " "
                + receiver.getColor() + " " + toSend;
    }

    public String getAttack() {
        eliminating = false;
        breaking = null;
        return plan(true);
    }

    /**
     * Will roll the maximum, but checks to see if the attack is still the
     * best plan every 3rd roll
     */
    public String rewrite() {
    	 if (result.equals("endattack")) {
             return "retreat";
         }
         StringTokenizer st = new StringTokenizer(result);
         st.nextToken();
         if (game.getAttacker().getColor() != Integer.parseInt(st.nextToken())
                 || game.getDefender().getColor() != Integer.parseInt(st.nextToken())) {
             return "retreat";
         }
    }
    
    public void check_spot() {
    	if (type != AIDomination.PLAYER_AI_EASY && (game.getBattleRounds()%3 == 2 || (game.getBattleRounds() > 0 && (n - Math.min(m, game.getMaxDefendDice()) <= 0)))) {
            String result = plan(true);
            //TODO: rewrite to not use string parsing
            String rewrite = rewrite();
        }
    }
    
    public String getRoll() {
        int n=game.getAttacker().getArmies() - 1;
        int m=game.getDefender().getArmies();

        if (n < 3 && game.getBattleRounds() > 0 && (n < m || (n == m && game.getDefender().getOwner().getTerritoriesOwned().size() != 1))) {
            return "retreat";
        }

        //spot check the plan
        check_spot();
        return "roll " + Math.min(3, n);
    }

    /**
     * Get a quick overview of the game state - capitals, player ordering, if there is a common threat, etc.
     * @param p
     * @param excludeCards
     * @return
     */
    public void set_capitals() {
        int size = players.size();
    	  for (int i = 0; i < size; i++) {
              Player player2 = players.get(i);
              if (player2.getCapital() != null) {
                  g.capitals.add(player2.getCapital());
              }
              if (player2.getTerritoriesOwned().isEmpty()) {
                  continue;
              }
              if (player2 == p) {
                  index = i;
              } else {
                  playerCount++;
              }
          }
    }
    
    public void available_attack() {

        Country country = t.get(j);
        noArmies += country.getArmies();
        int available = country.getArmies() - 1;
        if (ownsNeighbours(player2, country)) {
            available = country.getArmies()/2;
        }
        //quick multipliers to prevent turtling/concentration
        if (available > 4) {
            if (available > 8 && strategic) {
                if (available > 13) {
                    available *= 1.3;
                }
                available += 2;
            }
            available += 1;
        }
        attackable += available;
    
    }
    
    public void attack_update() {

        if (g.owned[j] == player2) {
            attack += c[j].getArmyValue();
            if (strategic) {
                ps.playerValue += 3*c[j].getArmyValue();
            } else {
                ps.playerValue += 1.5 * c[j].getArmyValue() + 1;
            }
            owned.add(c[j]);
        }
    
    }
    
    public void  determinate_update_attack() {
        int size = t.size();
    	 for (int j = 0; j < size; j++) {
         	available_attack();
         }
         int reenforcements = Math.max(3, player2.getNoTerritoriesOwned()/3) + cardEstimate;
         if (reenforcements > 8 && strategic) {
             reenforcements *= 1.3;
         }
         int attack = attackable + reenforcements;
         HashSet<Continent> owned = new HashSet<Continent>();
         //update the attack and player value for the continents owned
        int leng = g.owned.length;
         for (int j = 0; j < leng; j++) {
         	attack_update();
         }
    }
    
    public void small_multiplayer() {
    	ps.defenseValue = 5*noArmies/4 + noArmies%4 + player2.getNoTerritoriesOwned();
        ps.p = player2;
        if (i == 0) {
            g.me = ps;
        } else {
            g.orderedPlayers.add(ps);
        }
        ps.playerValue += ps.attackValue + ((game.getMaxDefendDice() == 2 && !isIncreasingSet())?1:game.getMaxDefendDice()>2?3:2)*ps.defenseValue;
        attackOrder++;
    }
    
    public void get_players() {

        Player player2 = players.get((index + i)%players.size());
        if (player2.getTerritoriesOwned().isEmpty()) {
            continue;
        }
        //estimate the trade-in
        int cards = player2.getCards().size() + 1;
        int cardEstimate = (i==0&&excludeCards)?0:getCardEstimate(cards);
        PlayerState ps = new PlayerState();
        List<Country> t = player2.getTerritoriesOwned();
        int noArmies = 0;
        int attackable = 0;
        boolean strategic = isStrategic(player2);
        if (strategic) {
            strategicCount++;
        }
        //determine what is available to attack with, discounting if land locked
       
        determinate_update_attack();
        
        
        ps.strategic = strategic;
        ps.armies = noArmies;
        ps.owned = owned;
        ps.attackValue = attack;
        ps.attackOrder = attackOrder;
        //use a small multiplier for the defensive value
        small_multiplier();
    
    }
    
    public void top_player_multiplier() {
    	 if (type == AIDomination.PLAYER_AI_EASY) {
             multiplier *= 1.6; //typically this waits too long in the end game
         } else if (type == AIDomination.PLAYER_AI_HARD && player.getStatistics().size() > 3) {
    	     boolean inc = !isIncreasingSet();
             while (inc) {
                 //we can be more lenient with more players
                 multiplier = Math.max(1, multiplier - .4 + g.orderedPlayers.size()*.1);
                 break;
             }
             int gCa = game.getCardMode();
             int ita = RiskGame.CARD_ITALIANLIKE_SET;
             while (gCa != ita) {
                 //don't want to pursue the lowest player if there's a good chance someone else will eliminate
                 multiplier *= 1.5;
                 break;
             }
         } else if (type == AIDomination.PLAYER_AI_AVERAGE) {
             multiplier *= 1.2;
         }
         g.targetPlayers.add(topPlayer.p);
    }
    
    public void see_player_multiplier() {
    	 if (g.orderedPlayers.size() > 1 && topPlayer.playerValue > multiplier * g.me.playerValue) {
             g.breakOnlyTargets = game.getMaxDefendDice() == 2;
             PlayerState ps = g.orderedPlayers.get(1);
             if (topPlayer.playerValue > multiplier * ps.playerValue) {
                 g.commonThreat = topPlayer;
             } else {
                 //each of the top players is a target
                 g.targetPlayers.add(ps.p);
             }
         } else if (type == AIDomination.PLAYER_AI_HARD && isIncreasingSet() && g.orderedPlayers.get(g.orderedPlayers.size()-1).defenseValue/topPlayer.attackValue > .3) {
             //play for the elimination
             g.targetPlayers.clear();
             g.targetPlayers.add(g.orderedPlayers.get(g.orderedPlayers.size()-1).p);
         }
    }
    
    public void game_alliance_treaties() {

        //base top player multiplier
        double multiplier = game.getCards().isEmpty()?(game.isRecycleCards()?1.2:1.1):(player.getMission()!=null||player.getCapital()!=null)?1.1:1.3;
        PlayerState topPlayer = g.orderedPlayers.get(0);
       top_player_multiplier();
       
        //look to see if you and the next highest player are at the multiplier below the highest
       see_player_multiplier();
    
    }
    
    
    public GameState getGameState(Player p, boolean excludeCards) {
        List<Player> players = game.getPlayers();
        GameState g = new GameState();
        Continent[] c = game.getContinents();
        if (player.getCapital() == null) {
            g.capitals = Collections.EMPTY_SET;
        } else {
            g.capitals = new HashSet<Country>();
        }
        g.owned = new Player[c.length];
        int len = c.length;
        for (int i = 0; i < len; i++) {
            g.owned[i] = c[i].getOwner();
        }
        int index = -1;
        int playerCount = 1;
        //find the set of capitals
        set_capitals();
        
        g.orderedPlayers = new ArrayList<PlayerState>(playerCount);
        int attackOrder = 0;
        int strategicCount = 0;
        int size = players.size();
        for (int i = 0; i < size; i++) {
        	get_players();
        }
        //put the players in order of strongest to weakest
        Collections.sort(g.orderedPlayers, Collections.reverseOrder());
        //check to see if there is a common threat
        //the logic will allow the ai to team up against the strongest player
        //TODO: similar logic could be expanded to understand alliances/treaties
        if (game.getSetupDone() && !g.orderedPlayers.isEmpty()) {
        	game_alliance_treaties();
        }
        return g;
    }

    private int getCardEstimate(int cards) {
        int tradeIn = game.getCardMode() != RiskGame.CARD_INCREASING_SET?8:game.getNewCardState();
        int cardEstimate = cards < 3?0:(int)((cards-2)/3.0*tradeIn);
        return cardEstimate;
    }

    /**
     * Provides a quick measure of how the player has performed
     * over the last several turns
     */
    private boolean isStrategic(Player player2) {
        if (player2 == this.player) {
            return false;
        }
        List<Statistic> stats = player2.getStatistics();
        if (stats.size() < 4) {
            return false;
        }
        //look over the last 4 turns
        int end = 4;
        int reenforcements = 0;
        int kills = 0;
        int casualities = 0;
        int size = stats.size();
        for (int i = size - 1; i >= end; i--) {
            Statistic s = stats.get(i);
            reenforcements += s.get(StatType.REINFORCEMENTS);
            kills += s.get(StatType.KILLS);
            casualities += s.get(StatType.CASUALTIES);
            if (s.get(StatType.CONTINENTS) == 0) {
                return false;
            }
        }
        return reenforcements + kills/((player2.getCards().size() > 2)?1:2) > 2*casualities;
    }

    /**
     * Delay trading in cards when sensible
     * TODO: this should be more strategic, such as looking ahead for elimination
     */
    public String getTrade() {
        if (!game.getTradeCap() && type != AIDomination.PLAYER_AI_EASY) {
            if (game.getCardMode() != RiskGame.CARD_ITALIANLIKE_SET && player.getCards().size() >= RiskGame.MAX_CARDS) {
                return super.getTrade();
            }
            GameState gs = getGameState(player, true);
            if (gs.commonThreat == null && gs.orderedPlayers.size() > 1 && !pressAttack(gs) && !isTooWeak(gs)) {
                return "endtrade";
            }
        }
        return super.getTrade();
    }

    /**
     * Finds all countries that can be attacked from.
     * @param p player object
     * @param attack true if this is durning attack, which requires the territority to have 2 or more armies
     * @return a Vector of countries, never null
     */
    public List<Country> findAttackableTerritories(Player p, boolean attack) {
        List<Country> countries = p.getTerritoriesOwned();
        List<Country> result = new ArrayList<Country>();
        int size = countries.size();
        for (int i=0; i<size; i++) {
            Country country = countries.get(i);
            if ((!attack || country.getArmies() > 1) && !ownsNeighbours(p, country)) {
                result.add(country);
            }
        }
        return result;
    }

    /**
     * Checks whether a country owns its neighbours
     * @param p player object, c Country object
     * @return boolean True if the country owns its neighbours, else returns false
     */
    public boolean ownsNeighbours(Player p, Country c) {
        List<Country> neighbours = c.getNeighbours();
        int size = neighbours.size();
        for (int i=0; i<size; i++) {
            if ( neighbours.get(i).getOwner() != p) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected String getTrade(Card[] result) {
        String s = null;
        if (type != PLAYER_AI_EASY) {
            boolean[] owns = new boolean[result.length];
            int ownsCount = 0;
            int leng = result.length;
            for (int i = 0; i < leng; i++) {
                if (result[i].getCountry() != null && player.getTerritoriesOwned().contains(result[i].getCountry())) {
                    owns[i] = true;
                    ownsCount++;
                }
            }
            //swap for a single owned country - TODO: be smarter about which territory to retain
            if (ownsCount != 1 && player.getCards().size() > 3) {
                List<Card> toTrade = Arrays.asList(result);
                s = killThisAnotherFor(result,ownsCount);
            }
        }
        return s;
    }
    private String killThisWhile(Card[] result, int ownsCount, Card card) {
        String s = null;
        int leng = result.length;
        for (int i = 0; i < leng; i++) {
            boolean eq = result[i].getName().equals(card.getName());
            while(eq) {
                result[i] = card;
                while(--ownsCount == 1) {
                    s = super.getTrade(result);
                }
                break;
            }
        }
        return s;
    }
    private String killThisWhile2(Card[] result, int ownsCount, Card card) {
        String s = null;
        Country gCoun = card.getCountry();
        boolean cont = player.getTerritoriesOwned().contains(card.getCountry());
        while(gCoun != null && cont) {
            int leng = result.length;
            for (int i = 0; i < leng; i++) {
                boolean eq = result[i].getName().equals(card.getName());
                while (eq) {
                    result[i] = card;
                    s = super.getTrade(result);
                }
            }
        }
        return s;
    }
    private String killThisAnotherFor(Card[] result, int ownsCount) {
        String s = null;
        for (Card card : (List<Card>)player.getCards()) {
            if (ownsCount > 1) {
                Country gCoun = card.getCountry();
                boolean cont = !player.getTerritoriesOwned().contains(card.getCountry());
                while(gCoun == null || cont) {
                    s = killThisWhile(result,ownsCount,card);
                }
            } else {
                s = killThisWhile2(result,ownsCount,card);
            }
        }
        return s;
    }

}
