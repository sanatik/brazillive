package com.poker.brazillive.shell.shell;

import java.awt.image.BufferedImage;
import java.util.*;
import java.io.*;
import java.text.*;

import javax.imageio.ImageIO;

import com.poker.brazillive.shell.game.*;
import com.poker.brazillive.shell.util.*;

public class GameBuilder {

    public static class BuildRes {

        public Game game;
        public List<Event> evs = new ArrayList<Event>();
    }

    private Log log;
    private int heroLoc;
    private int tableMax;

    private String curGid;
    private Map<String, Integer> p2loc = new HashMap<String, Integer>();
    private Map<Integer, String> loc2p = new HashMap<Integer, String>();

    public GameBuilder(int heroLoc, Log log) {
        this.heroLoc = heroLoc;
        this.log = log;
    }

    private int incSeat(int seat) {
        return (seat + 1) % this.tableMax;
    }

    private boolean bothHoles(Card[] holes) {
        return holes != null && holes.length == 2 && holes[0] != null && holes[1] != null && holes[0].getIndex() != holes[1].getIndex();
    }
//	private int pn2loc(TableLook tl, String pn) {
//		for (int i = 0; i < tl.pnames.length; i++) if (pn.equals(tl.pnames[i]))
//			return i;
//		return -1;
//	}

    public GameEvs procTableLook(Game game, TableLook tl) throws Exception {

        GameEvs res = new GameEvs(null, new Event[0]);

        if (tl.isHeroSitout) {
            return res;
        }
        // Nobody in play, game is finished, nothing to do
        int inpc = 0;
        for (int i = 0; i < tl.tableMax; i++) {
            if (tl.isInPlay[i]) {
                inpc++;
            }
        }
        if (inpc == 0) {
            return res;
        }

        if (game == null && (1 == 2
                || !tl.isButtons
                || tl.board.length > 0
                || (tl.bets[this.heroLoc] != null && tl.bets[this.heroLoc] > tl.bb))) {
//			log.lo("Skip image. Waiting for the first game to start");
            return res; // Playing is not started at this table
        }

        if (tl.isBuyinDialog) {
            return res;
        }

        this.tableMax = tl.tableMax;

        boolean gameFinished = (game == null || (tl.gameId != null && !game.id.equals(tl.gameId)));

        if (!gameFinished && game != null && game.getPlayerFromAll(game.heroName) != null
                && game.getPlayerFromAll(game.heroName).holes != null
                && tl.heroPos != -1 && tl.holes[tl.heroPos] != null) {
            long ghm = Card.cards2Mask(game.getPlayerFromAll(game.heroName).holes);
            long lhm = Card.cards2Mask(tl.holes[tl.heroPos]);
            if (ghm != lhm) {
                gameFinished = true;
            }
        }

        boolean newGame = tl.isButtons && tl.holes[this.heroLoc] != null
                && tl.holes[this.heroLoc].length == 2
                && tl.stacks[this.heroLoc] != null && gameFinished;

        if (!newGame && gameFinished) {
            return res; // Game id is changed, but 1st buttons in the new game are not shown yet. Nothing to do
        }
        int sbLoc = -1, bbLoc = -1, dLoc = -1;
        int sb = tl.sb, bb = tl.bb;
        List<Integer> guestBbLocs = new ArrayList<Integer>();
        boolean gameIsBroken = false;

        if (newGame) {
            game = new Game();

            String nw = Shell.getNetworkSkin()[0];

            res.game = game;
            game.id = (tl.gameId == null ? "" + Misc.getTime() : tl.gameId);
            game.heroName = tl.heroName;
            gameIsBroken = false;

            game.sb = tl.sb;
            game.bb = tl.bb;

            this.p2loc.clear();
            this.loc2p.clear();
            this.curGid = game.id;

            boolean sbBbInPlayNotMoved = false;
            dLoc = tl.dealerPos;
            if (dLoc == -1) {
                throw new Exception("Cannot define dealer position");
            }

            boolean headsup = false;
            if (dLoc == this.heroLoc && tl.bets[this.heroLoc] != null && tl.bets[this.heroLoc] == game.sb) {
                headsup = true;
            }

            if (headsup) {
                sbLoc = dLoc;
                for (int loc = 0; loc < tl.tableMax; loc++) {
                    if (tl.bets[loc] != null && tl.bets[loc] == game.bb) {
                        bbLoc = loc;
                        break;
                    }
                }
            } else if (tl.bets[this.heroLoc] == null) { // Bb and sb are in play
                sbBbInPlayNotMoved = true;
            } else {
                int heroBet = tl.bets[this.heroLoc];
                if (heroBet == sb) {
                    sbBbInPlayNotMoved = true;
                } else if (heroBet == bb) { // Hero is on bb (possibly guest bb), sb folded

                    int bc = 0; // Bets count between dealer and hero
                    for (int loc = dLoc + 1; loc < 6; loc++) {
                        if (tl.bets[loc] != null) {
                            bc++;
                        }
                    }

                    if (bc >= 2) { // Hero is on guest bb, sb and bb are in play
                        sbBbInPlayNotMoved = true;
                    } else { // Hero is on bb, sb could fold
                        for (int loc = dLoc + 1; loc < 6; loc++) {
                            if (tl.bets[loc] != null) {
                                sbLoc = loc;
                                break;
                            }
                        }
                        if (sbLoc == -1) { // Sb folded or playing without sb, set any sb between dealer and bb
                            // Need to have player name who folded sb. If no name - play without sb
                            for (int loc = dLoc + 1; loc < 6; loc++) {
                                if (tl.pnames[loc] != null && tl.stacks[loc] != null
                                        && tl.stacks[loc] > bb) {
                                    sbLoc = loc;
                                }
                            }
                        }
                        bbLoc = this.heroLoc;
                    }
                }
            }
            if (sbBbInPlayNotMoved) {
                int loc = incSeat(dLoc);
                int c = 0;
                while (loc != dLoc) {
                    if (tl.bets[loc] != null) {
                        if (tl.bets[loc] == sb) {
                            sbLoc = loc;
                        } else if (tl.bets[loc] == bb) {
                            bbLoc = loc; // Playing without sb
                        }
                        break;
                    }
                    loc = incSeat(loc);
                    c++;
                    if (c > 10) {
                        throw new Exception("Infinite loop 1");
                    }
                }

                if (bbLoc == -1) {
                    loc = incSeat(sbLoc);
                    c = 0;
                    while (loc != sbLoc) {
                        if (tl.bets[loc] != null) {
                            bbLoc = loc;
                            break;
                        }
                        loc = incSeat(loc);
                        c++;
                        if (c > 10) {
                            throw new Exception("Infinite loop 2");
                        }
                    }
                }
            }

            if (this.heroLoc != sbLoc && this.heroLoc != bbLoc) {
                int loc = this.heroLoc;
                int c = 0;
                while (true) {
                    if (tl.bets[loc] != null && tl.bets[loc] == game.bb) {
                        guestBbLocs.add(loc);
                    }
                    if (loc == dLoc) {
                        break;
                    }
                    loc = incSeat(loc);
                    c++;
                    if (c > 10) {
                        throw new Exception("Infinite loop 3");
                    }
                }
            }

            int startLoc = sbLoc;
            if (sbLoc == -1) {
                startLoc = bbLoc; // Playing without sb
            }
            int loc = startLoc;
            boolean bbAdded = false, heroAdded = false;
            int c = 0;

            do {
                boolean skip = false;
                if ((tl.stacks[loc] == null || tl.stacks[loc] == 0) && (tl.bets[loc] == null || tl.bets[loc] == 0)) {
                    skip = true;
                }
                if (!bbAdded && loc != bbLoc && loc != sbLoc) {
                    skip = true; // Skip players between sb and bb seats
                }
                if (!tl.isSeatOccupied[loc] && tl.bets[loc] == null) {
                    skip = true; // if seat has bet then player posted sb and left the table
                }//				if (tl.isSeatout[loc]) skip = true; // Player could fold and then go to sitout. We need to add him
                if (heroAdded && this.heroLoc != bbLoc && this.heroLoc != sbLoc && !tl.isInPlay[loc]) {
                    skip = true; // Players between hero and sb didn't move yet but they are already not int play. So they are not in the game, skip
                }
                if (headsup && tl.bets[loc] == null) {
                    skip = true;
                }

                if (!skip) {
                    if (loc == bbLoc) {
                        bbAdded = true;
                    }
                    if (loc == this.heroLoc) {
                        heroAdded = true;
                    }

                    Integer stack = tl.stacks[loc];
                    if ((stack == null || stack == 0) && (tl.isSeatout[loc] || !tl.isSeatOccupied[loc])) {
                        stack = bb * 100; // Player is sitting out and his stack is unknown. We do not know if he folded or out of game. Let's think he is folded
                    }
                    Integer bet = tl.bets[loc];
                    if (bet == null) {
                        bet = 0;
                    }

                    String pn = tl.pnames[loc];
                    if (pn == null) {
                        pn = "fake name"; // Player left table but posted blind
                    }
                    Player p = new Player(pn, stack + bet);
                    game.addPlayer(p);
                    this.p2loc.put(pn, loc);
                    this.loc2p.put(loc, pn);
                }

                loc = incSeat(loc);

                c++;
                if (c > 10) {
                    throw new Exception("Infinite loop 4");
                }

            } while (loc != startLoc);

        } // end of new hand

        if (game != null && !gameIsBroken) {
            List<Event> evs = new ArrayList<Event>();
            Game gc = game.clone();

            Map<String, Integer> moveCount = new HashMap<String, Integer>();
            for (Player p : gc.players) {
                moveCount.put(p.name, 0);
            }

            int c = 0;
            do {
                if (gc.isFinished()) {
                    break;
                }

                Event ev = null;
                if (gc.stageFinished) {

                    if (gc.stage >= Game.STAGE_RIVER || tl.board.length < Game.getBoardLen(gc.stage + 1)) {
                        break;
                    }

                    Card[] bcs = null;
                    if (gc.stage == Game.STAGE_PREFLOP) {
                        bcs = new Card[]{tl.board[0], tl.board[1], tl.board[2]};
                    } else {
                        bcs = new Card[]{tl.board[gc.stage - Game.STAGE_FLOP + 3]};
                    }

//					if (bcs[0] == null) break;
                    int et = -1;
                    if (gc.stage == Game.STAGE_PREFLOP) {
                        et = Event.TYPE_FLOP;
                    } else if (gc.stage == Game.STAGE_FLOP) {
                        et = Event.TYPE_TURN;
                    } else if (gc.stage == Game.STAGE_TURN) {
                        et = Event.TYPE_RIVER;
                    } else {
                        throw new Exception("Incorrect game stage: " + Game.stage2StrLong[gc.stage]);
                    }

                    ev = new Event(et, Card.cards2Mask(bcs));
                } else if (!gc.sbPosted && sbLoc != -1) {
                    ev = new Event(Event.TYPE_SB, this.loc2p.get(sbLoc), sb);
                } else if (!gc.bbPosted) {
                    int t = Event.TYPE_BB;
                    if (tl.stacks[bbLoc] == 0) {
                        t = Event.TYPE_BB_ALLIN;
                    }
                    ev = new Event(t, this.loc2p.get(bbLoc), bb);
                } else if (gc.bbPosted && guestBbLocs.size() > 0) {
                    ev = new Event(Event.TYPE_BB, this.loc2p.get(guestBbLocs.remove(0)), bb);
                } else if (gc.bbPosted && gc.getPlayer(gc.heroName) != null && gc.getPlayer(gc.heroName).holes == null && tl.bets[this.heroLoc] != null && gc.getPlayer(gc.heroName).putToPotInStage == 0) {
                    // This is hero guest bb
                    ev = new Event(Event.TYPE_BB, this.loc2p.get(this.heroLoc), bb);
                } else if (gc.getPlayer(gc.heroName) != null && gc.getPlayer(gc.heroName).holes == null) {
                    ev = new Event(Event.TYPE_HOLES, this.loc2p.get(this.heroLoc), Card.cards2Mask(tl.holes[this.heroLoc]));
                } else {

                    Player p = gc.getCurMovePlayer();
                    if (!this.curGid.equals(game.id)) {
                        throw new Exception("Incorrect gid in info about player seats");
                    }
                    int loc = this.p2loc.get(p.name);

                    boolean inp = tl.isInPlay[loc];

                    if (!inp) {
                        ev = new Event(Event.TYPE_FOLD, p.name);
                    } else {
                        // if (isButtons && p.name.equals(gc.heroName)) break; // Produces error: new buttons are displayed, but previous moves need to be processed. Hero posted guest bb and smb sitting out after the hero game will be processed as hero already checked, but actually hero didn't move yet

                        int stack = tl.stacks[loc];

                        int curBet = p.stack - stack;
                        if (gc.stage == Game.STAGE_PREFLOP
                                && gc.getPlayer(gc.heroName) != null
                                && gc.getPlayer(gc.heroName).turnCountInStage == 0
                                && !gc.getCurMovePlayer().name.equals(gc.heroName)) {
                            // Bet can not to parse due to animation (bets go to table center). So parse bet only for preflop with buttons
                            Integer bet = tl.bets[loc];
                            if (bet == null) {
                                bet = 0;
                            }
                            curBet = bet - p.putToPotInStage; // Player could put blind before
                        }

                        if (curBet < 0) {
                            break; // Need to call but didn't call
                        }
                        if (Shell.room.getClass().getSimpleName().equals("RoomGg")
                                && gc.stage == Game.STAGE_PREFLOP && tl.isButtons
                                && !p.name.equals(gc.heroName)
                                && curBet == 0 && gc.getCall() > 0) {
                            // Workaround for client bug: player folded but his holes do not disappear
                            ev = new Event(Event.TYPE_FOLD, p.name);
                        } else if (curBet == 0) {
                            ev = new Event(Event.TYPE_CHECK, p.name);
                            if (gc.getCall() > 0) {
                                break;
                            }
                        } else if (curBet == gc.getCall()) {
                            if (gc.getCall() < p.stack) {
                                ev = new Event(Event.TYPE_CALL, p.name, curBet);
                            } else if (gc.getCall() == p.stack) {
                                ev = new Event(Event.TYPE_CALL_ALLIN, p.name, curBet);
                            }
                        } else if (curBet > gc.getCall()) {
                            int betLevel = curBet + p.putToPotInStage;
                            int t = 0;

                            if (gc.raisedVal > 0) {
                                if (curBet < p.stack) {
                                    t = Event.TYPE_RAISE;
                                } else if (curBet == p.stack) {
                                    t = Event.TYPE_RAISE_ALLIN;
                                } else {
                                    throw new Exception("Incorrect bet or stack. Player: " + p.name + ", bet: " + curBet + ", stack: " + p.stack);
                                }
                            } else if (gc.raisedVal == 0) {
                                if (curBet < p.stack) {
                                    t = Event.TYPE_BET;
                                } else if (curBet == p.stack) {
                                    t = Event.TYPE_BET_ALLIN;
                                } else {
                                    throw new Exception("Incorrect bet or stack. Player: " + p.name + ", bet: " + curBet + ", stack: " + p.stack);
                                }
                            } else {
                                throw new Exception("Cannot be");
                            }

                            ev = new Event(t, p.name, betLevel);
                        } else {
                            throw new Exception("Incorrect bet: " + curBet + ", call: " + gc.getCall());
                        }

                    }
                }

                gc.procEvent(ev);
                evs.add(ev);

                c++;
                if (c > 100) {
                    throw new Exception("Infinite loop 5");
                }

            } while (true);

            // Showed cards
            List<Event> showEvs = new ArrayList<Event>();
            for (int loc = 0; loc < tl.tableMax; loc++) {
                if (loc == this.heroLoc) {
                    continue;
                }
                Player p = gc.getPlayer(this.loc2p.get(loc));
                if (p == null) {
                    continue; // Some player images look like cards
                }
                if (p.holes != null) {
                    continue; // Player showed cards before, don't send show event more than once
                }
                if (tl.holes[loc] != null && bothHoles(tl.holes[loc])) {
                    Event ev = new Event(Event.TYPE_SHOW, this.loc2p.get(loc), Card.cards2Mask(tl.holes[loc]));
                    ev.sVal = Card.cards2Str(tl.holes[loc]);
                    showEvs.add(ev);
                }
            }
            // Hero shows if smb showed and hero is in play
            if (showEvs.size() > 0 && gc.getPlayer(gc.heroName) != null) {
                Player p = gc.getPlayer(gc.heroName);
                Event ev = new Event(Event.TYPE_SHOW, p.name, Card.cards2Mask(p.holes));
                ev.sVal = Card.cards2Str(p.holes);
                showEvs.add(ev);
            }

            if (showEvs.size() > 0 && game.stage < Game.STAGE_SHOWDOWN) {
                if (game.stage == Game.STAGE_RIVER && game.stageFinished) {
                    evs.add(new Event(Event.TYPE_SHOWDOWN, 0));
                }
                for (Event ev : showEvs) {
                    Player p = gc.getPlayerFromAll(ev.who);
//					if (p != null && p.holes != null) continue;
                    if (p == null) {
                        continue;
                    }
                    evs.add(ev);
                }
            }

            int lastEv = -1;
            if (tl.isButtons) {
                int evn = evs.size() - 1;
                c = 0;
                while (evn >= 0) {
                    if (evs.get(evn).isPutToPot() || evs.get(evn).isFold()) {
                        break;
                    }
                    if (!evs.get(evn).isMove()) {
                        break;
                    }
                    if (game.heroName.equals(evs.get(evn).who)) {
                        evn--;
                        break;
                    }
                    evn--;
                    c++;
                    if (c > 50) {
                        throw new Exception("Infinite loop 6");
                    }
                }
                lastEv = evn;

            } else if (tl.isRiverMovesFinished) {
                lastEv = evs.size() - 1;
            } else {
                // Take all but last checks because it's not clear if it was check or player didn't move
                lastEv = evs.size() - 1;
                while (lastEv >= 0 && evs.get(lastEv).type == Event.TYPE_CHECK) {
                    lastEv--;
                }
            }
            if (lastEv == -1) {
                evs.clear();
            } else {
                evs = evs.subList(0, lastEv + 1);
            }

            res.evs = evs.toArray(new Event[0]);

        } else {
            return res;
        }

        return res;
    }

    public static void main(String[] args) throws Exception {
        List<File> fs = FUtil.searchFiles("fgfg/imgs/20160704_111722_0x014F0308", null);
        Shell.cfg = new Properties();
        Shell.cfg.load(new FileInputStream("shell.cfg"));
        Shell.uname = "fgfg";

        GameBuilder gb = new GameBuilder(0, Log.defl);
        String title = "Toyota 05 - $0.05 / $0.10";

        Set<String> hwnds = new HashSet<String>();
        for (File f : fs) {
            String hwnd = f.getName().split("_")[2].split("\\.")[0];
            hwnds.add(hwnd);
        }
        Log.log("Found hwnds: %s", hwnds);

        for (String hwnd : hwnds) {

            TableLookBuilder tlb = new com.poker.brazillive.shell.shell.gg.TableLookBuilderGg(Log.defl, "hero", new ArrayList<String>());
            TableLook tl = null;
            Game g = null;

            for (File f : fs) {
                if (!f.getName().contains(hwnd)) {
                    continue;
                }

                if (f.getName().compareTo("20160704_120301.774_0x014F0308.bmp") < 0) {
                    continue;
                }
                if (f.getName().compareTo("20160704_120301.774_0x014F0308.bmp") > 0) {
                    break;
                }

                BufferedImage img = ImageIO.read(f);
                WndData wndData = new WndData();
                wndData.title = title;
                wndData.img = img;
                wndData.active = true;
                DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
                wndData.time = df.parse(f.getName().split("_")[0] + "_" + f.getName().split("_")[1]).getTime();

                Log.log("File: %s", f.getName());
                if (f.getName().equals("20160702_140659.099_0x002A0A34.bmp")) {
                    Misc.getTime();
                }

                TableLook ptl = tl;
                try {
                    tl = tlb.buildTableLook(wndData, ptl);
                } catch (Throwable e) {
                    Log.log("ERROR! Cannot build TableLook:\n%s", Misc.stacktrace2Str(e));
                    continue;
                }
                Log.log("%s", tl);

                if (tl.skipReason != null) {
                    continue;
                }

                GameEvs bres = null;
                try {
                    bres = gb.procTableLook(/*gameBroken ? null :*/g, tl);
                } catch (Throwable e) {
                    Log.log("ERROR! Cannot process TableLook:\n%s", Misc.stacktrace2Str(e));
                    continue;
                }
                if (bres.game != null) {
                    g = bres.game;
                    Log.log("New game", g);
                }

                for (Event ev : bres.evs) {
                    g.procEvent(ev);
                    Log.log("%s\n%s", ev, g);
                }
            }
        }
    }
}
