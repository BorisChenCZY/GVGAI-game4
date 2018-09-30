package tracks.singlePlayer.agent11510237;


import java.awt.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import javafx.geometry.Pos;
import ontology.Types;
import ontology.sprites.Door;
import tools.ElapsedCpuTimer;
import tools.Pair;
import tools.Vector2d;


/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 14/11/13
 * Time: 21:45
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Agent extends AbstractPlayer {
    /**
     * Random generator for the agent.
     */
    protected Random randomGenerator;
    /**
     * List of available actions for the agent
     */
    protected ArrayList<Types.ACTIONS> actions;

    private class Position{
        protected int x;
        protected int y;

        Position(int x, int y)
        {
            this.x = x;
            this.y = y;
        }

        protected Position copy()
        {
            return new Position( this.x, this.y );
        }

        protected Position copy( int x, int y )
        {
            return new Position( this.x + x, this.y + y);
        }

        protected boolean equals( int x, int y )
        {
            return this.x == x && this.y == y;
        }

        protected boolean equals( Position p )
        {
            return this.equals( p.x, p.y );
        }

        protected Types.ACTIONS to( Position p ){
            int x = this.x - p.x;
            int y = this.y - p.y;
            if ( Math.abs(x) == 1 && Math.abs(y) == 1 )
                return null;

            if ( y == 1 )
            {
                return Types.ACTIONS.ACTION_UP;
            }
            else if ( y == -1 )
            {
                return Types.ACTIONS.ACTION_DOWN;
            }
            else if ( x == 1 )
            {
                return Types.ACTIONS.ACTION_LEFT;
            }
            else if ( x == -1 )
            {
                return Types.ACTIONS.ACTION_RIGHT;
            }

            return null;
        }

        protected Position left()   {return this.copy(-1, 0 );}
        protected Position right()  {return this.copy( 1, 0 );}
        protected Position up()     {return this.copy( 0, -1 );}
        protected Position down()   {return this.copy( 0, 1 );}

        protected char on( Status charMap ) { return charMap.at( this ); }

        protected Position sym( Position p )
        {
            if ( this.equals( p.left() ) ) return p.right();
            else if ( this.equals( p.right() ) ) return p.left();
            else if ( this.equals( p.up() ) ) return  p.down();
            else if ( this.equals( p.down() ) ) return p.up();
            else return null;
        }

        protected Position[] around(){
            Position[] positions = new Position[4];
            positions[0] = this.up();
            positions[1] = this.down();
            positions[2] = this.left();
            positions[3] = this.right();
            return positions;
        }

        @Override
        public String toString() {
            return String.format( "(%d, %d)", this.x, this.y );
        }

        @Override
        public int hashCode() {
            return (this.x) | (this.y);
        }


    }

    private class Status implements Comparable<Status>{
        protected char[][] charMap;
        protected ArrayList<Position> Stones = new ArrayList<>();
        protected ArrayList<Position> Holes = new ArrayList<>();
        protected boolean finished = false;
        protected Position player;
        protected Position door_pos;
        protected Position key_pos;
        protected Position mushroom_pos = null;
        protected Position[] from_to = new Position[2];
        protected int dis = 0;

        protected Status( ArrayList<Observation>[][] map, Position player ) {
            charMap = new char[maxCol][maxRow];
            for ( int index = 0; index < map.length; index++ )
            {
                ArrayList<Observation>[] line = map[index];
                char[] charLine = new char[maxRow];
                for ( int j = 0; j < maxRow; j++) {
                    ArrayList<Observation> obj = line[j];
                    
                    if ( obj.size() == 1 && obj.get( 0 ).itype != 4 )
                        charLine[j] = Integer.toString( obj.get(0).itype ).charAt( 0 );
                    else
                        charLine[j] = SPACE_UNREACH;
                }
                charMap[index] = charLine;
            }
            update( player );
        }

        protected void update(Position position) {
            dis = 0;
            player = position;
            Stones = new ArrayList<>();
            Holes = new ArrayList<>();
            mushroom_pos = null;
            finished = false;

            for ( int i = 0; i < maxCol; i++ )
            {
                for ( int j = 0; j < maxRow; j++ )
                {
                    if ( this.at( i, j ) == SPACE_REACH )
                        this.set( i, j, SPACE_UNREACH );
                    else if ( this.at( i, j ) == STONE )
                        this.Stones.add( new Position( i, j ) );
                    else if ( this.at( i, j ) == HOLE )
                        this.Holes.add( new Position( i, j ) );
                    else if ( this.at( i, j ) == MUSHROOM )
                        this.mushroom_pos = new Position( i, j );
                    else if ( this.at( i, j ) == DOOR )
                        this.door_pos = new Position( i, j );
                    else if ( this.at( i, j ) == KEY )
                        this.key_pos = new Position( i, j );

                }
            }

            ArrayDeque<Position> queue = new ArrayDeque<Position>();
            queue.addFirst(position);
            this.set( position, SPACE_REACH );

            while ( !queue.isEmpty() )
            {
                Position cur = queue.removeFirst();
                for ( Position tmp: cur.around() )
                    if ( this.setReachable( tmp ) ) queue.addFirst( tmp );

            }

            if ( hasPathTo( this.door_pos ) && hasPathTo( this.key_pos ) ) finished = true;

//            System.out.println( this );
        }

        protected Status( Status s )
        {
            this.charMap = new char[maxCol][maxRow];
            for ( int i = 0; i < maxCol; i++ )
                this.charMap[i] = s.charMap[i].clone();

            for ( Position pos: s.Stones )
                Stones.add( pos );

            for ( Position pos: s.Holes )
                Holes.add( pos );

            finished  = s.finished;
            player = s.player;
            door_pos = s.door_pos;
            key_pos = s.key_pos;
            mushroom_pos = s.mushroom_pos;
            
        }

        protected Status( char[][] charMap, Position player )
        {
            this.charMap = new char[maxCol][maxRow];
            for ( int i = 0; i < maxCol; i++ )
                this.charMap[i] = charMap[i].clone();
            update( player );
        }

        protected boolean isReachable(Position pos)
        {

            return this.at( pos ) == SPACE_REACH ||
                    this.at( pos ) == MUSHROOM ||
                    this.at( pos ) == KEY ||
                    this.at( pos ) == DOOR ;
        }

        protected boolean hasPathTo( Position pos )
        {
            for ( Position p: pos.around() )
            {
                if ( at( p ) == SPACE_REACH )
                    return true;
            }

            return false;
        }

        protected boolean isEmpty(Position pos)
        {
            if ( pos.x >= 0 && pos.y >= 0 && pos.x < maxCol && pos.y < maxRow )
                return this.at(pos) == SPACE_UNREACH;
            else
                return false;
        }

        protected boolean setReachable(Position tmp)
        {
            if ( isEmpty( tmp ) )
            {
                this.set( tmp, SPACE_REACH );
                return true;
            }
            return false;
        }
        
        protected Status copy()
        {
            return new Status( this ) ;
        }
        
        protected char at(Position pos)
        {
            return charMap[pos.x][pos.y];
        }

        protected char at(int x, int y)
        {
            return this.at( new Position( x, y ) );
        }

        protected void set(Position pos, char c) {
            charMap[pos.x][pos.y] = c;
        }

        protected void set(int x, int y, char c)
        {
            this.set( new Position( x, y ), c );
        }

//        protected boolean equals( Status o )
//        {
//            return this.hashCode() == o.hashCode();
//        }


        @Override
        public int compareTo(Status o) {
            if ( this.dis < o.dis ) return -1;
            else if ( this.dis > o.dis ) return 1;

            if ( this.Stones.size() < o.Stones.size() ) return -1;
            else if ( this.Stones.size() > o.Stones.size() ) return 1;

            if ( this.Holes.size() < o.Holes.size() ) return -1;
            else if ( this.Holes.size() > o.Holes.size() ) return 1;

            if ( dist( this.player, door_pos ) < dist( o.player, door_pos ) ) return -1;
            else if ( dist( this.player, door_pos ) > dist( o.player, door_pos ) ) return 1;

            if ( this.mushroom_pos == null && o.mushroom_pos != null ) return -1;
            else if ( this.mushroom_pos != null && o.mushroom_pos == null) return 1;


            return 1;
        }

        @Override
        public int hashCode() {
            int total = 0;
            for ( int index = 0; index < maxCol; index++ )
            {
                char[] line = charMap[index];
                int hash = 0;
                for ( int j = 0; j < maxRow; j++ )
                    hash += line[j] * ( j + 1 );

                total += hash * ( index + 1 );
            }
            return total;
        }

        @Override
        public String toString() {
            String a = "";

            for( int i = 0; i < maxRow; i++ )
            {
                for ( int j = 0; j < maxCol; j++)
                {
                    a += String.format("%c ", charMap[j][i]);
                }

                a += "\n";
            }
            return a;
        }
    }

    private class CompareDist implements Comparator<Pair<Pair<Integer, Integer>, Position>>
    {

        @Override
        public int compare(Pair<Pair<Integer, Integer>, Position> o1, Pair<Pair<Integer, Integer>, Position> o2) {
            return (0 > (o1.first.first + o1.first.second - o2.first.first - o2.first.second)) ? -1 : 1;
        }
    }



    private static int dist(Position orig, Position dest){
        return Math.abs( orig.x - dest.x ) + Math.abs( orig.y - dest.y );
    }

    private int maxRow;
    private int maxCol;
    private Status charMap;
    ArrayDeque<Types.ACTIONS> ins = new ArrayDeque<>();

    private final static char WALL = '0';
    private final static char KEY = '7';
    private final static char HOLE = '3';
    private final static char SPACE_REACH = '1';
    private final static char SPACE_UNREACH = '2';
    private final static char MUSHROOM = '6';
    private final static char STONE = '9';
    private final static char DOOR = '8';


    /**
     * Public constructor with state observation and time due.
     * @param stateObs state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer)
    {
        ArrayList<Observation>[][] map = stateObs.getObservationGrid();
        Vector2d posv = stateObs.getAvatarPosition();
        Position pos = new Position( (int) posv.x / 50, (int) posv.y / 50 );
        randomGenerator = new Random();
        actions = stateObs.getAvailableActions();
        maxCol = stateObs.getObservationGrid().length;
        maxRow = stateObs.getObservationGrid()[0].length;



        charMap = new Status( map, pos );

        for ( Status status: search(charMap) )
        {
            for ( Types.ACTIONS act: pathTo( pos, status.from_to[0], charMap ) )
            {
                ins.addLast( act );
            }
            ins.addLast( status.from_to[0].to( status.from_to[1] ) );
            pos = status.player;
            charMap = status;
        }

        for ( Types.ACTIONS act: pathTo( pos, charMap.key_pos, charMap ) ) ins.addLast( act );
        pos = charMap.key_pos;

        for ( Types.ACTIONS act: pathTo( pos, charMap.door_pos, charMap ) ) ins.addLast( act );

    }


    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        // actions:
        // -> Types.ACTIONS.ACTION_RIGHT
        // <- Types.ACTIONS.ACTION_LEFT
        // etc.


        while ( !ins.isEmpty() )
        {
            Types.ACTIONS a = ins.removeFirst();
            return a;
        }

        return null;

    }

    private void sleep(int time)
    {
        try {
            TimeUnit.SECONDS.sleep(time);
        }
        catch (Exception e)
        {
            ;
        }
    }

    private ArrayDeque<Types.ACTIONS> pathTo(Position start, Position dest, Status charMap)
    {
        ArrayDeque<Types.ACTIONS> list = new ArrayDeque<Types.ACTIONS>();
        ArrayDeque<Position> path = new ArrayDeque<Position>();
        PriorityQueue<Pair<Pair<Integer, Integer>, Position>> heap = new PriorityQueue<Pair<Pair<Integer, Integer>, Position>>( new CompareDist() );
        HashMap<Position, Position> map = new HashMap<Position, Position>();
        Position d = new Position(0, 0 );

        do{
            if ( charMap.at( dest ) == STONE && moveable( dest, charMap ).size() == 0 )
                break;
            else if ( charMap.at( dest ) != STONE && !charMap.isReachable( dest ) )
                break;

            heap.add( new Pair<>( new Pair<>( 0 , dist( start, dest ) ) , start ) );

            while ( heap.size() != 0 && d.equals( 0, 0 ) )
            {

                Pair<Pair<Integer, Integer>, Position> item = heap.poll();
                Position pos = item.second;

                if ( pos.equals( dest ) )
                {
                    d = pos;
                    break;
                }

                for ( Position tmp: pos.around() ) {
                    if ( charMap.isReachable( tmp ) ) {
                        if ( charMap.at (dest) != DOOR && charMap.at( tmp ) == DOOR )
                            continue;
                        heap.add(new Pair<>(new Pair<>(item.first.first + 1, dist(tmp, dest)), tmp));
                        map.put(tmp, pos);
                    }
                }

            }

            if ( d.equals( 0, 0 ) )
                break;

            Position cur = d;
            Position pre;
            path.addFirst( cur );
            while ( !cur.equals( start ) ){
                pre = map.get( cur );
                list.addFirst( pre.to(cur) );
                cur = map.get( cur );
                path.addFirst( cur );
            }

        }while(false);


        return list;
    }

    private ArrayDeque<Status> search( Status charMap )
    {
        Status cur = charMap;
        Status end = null;
        boolean found = false;
        HashMap<Status, Status> map = new HashMap<>();
        HashSet<Integer> set = new HashSet<>();
        PriorityQueue<Status> queue = new PriorityQueue<>();
        ArrayDeque<Status> path = new ArrayDeque<>();

        queue.add(cur);
        while ( !queue.isEmpty() && !found )
        {
            cur = queue.poll();
            if ( set.contains( cur.hashCode() ) ) {
                continue;
            }



            for (Position pos : cur.Stones) {
                for (Position tmp : moveable(pos, cur)) {
                    Status s = cur.copy();
                    move( pos, tmp, s );
                    map.put( s, cur );
                    queue.add( s );

                    set.add( cur.hashCode() );
//                    System.out.println(s);

                    if (s.finished) {
                        found = true;
                        end = s;
//                        System.out.println("found a solution");
                        break;
                    }

                    else if ( s.mushroom_pos != null && s.hasPathTo( s.mushroom_pos ) )
                    {
                        Status s_ = s.copy();
                        for ( Position p: s.mushroom_pos.around() )
                        {
                            if ( s.at( p ) == SPACE_REACH )
                            {
                                s_.from_to[0] = p;
                                break;
                            }
                        }
                        s_.from_to[1] = s.mushroom_pos;
                        s_.set( s.mushroom_pos, SPACE_REACH );
                        s_.update( s.mushroom_pos );
                        map.put( s_ , s );
                        queue.add( s_ );

                        if (s_.finished) {
                            found = true;
                            end = s_;
//                            System.out.println("found a solution");
                            break;
                        }
                    }

                }

                if (found) break;
            }





        }

        if ( end != null )
        {
            cur = end;
            do {
                path.addFirst( cur );
                cur = map.get( cur );
            } while ( cur != charMap );
        }

        return path;
    }

    private ArrayList<Position> moveable( Position pos, Status charMap )
    {
        ArrayList<Position> list = new ArrayList<>();
        if ( charMap.at(pos) != STONE ) return list;

        for ( Position tmp: pos.around() )
        {
            if( charMap.at( tmp ) == SPACE_REACH && ( charMap.at( tmp.sym( pos ) ) == HOLE || charMap.at( tmp.sym( pos ) ) == SPACE_REACH || charMap.at( tmp.sym( pos ) ) == SPACE_UNREACH ) )
                list.add( tmp.sym( pos ) );
        }

        return list;
    }

    private void move( Position from, Position to, Status map)
    {
        if( map.at( from ) != STONE ) return;

        if ( map.at( to ) == HOLE )
        {
            map.set( to, SPACE_REACH );
            map.set( from, SPACE_REACH );
        }
        else if ( ( map.at( to ) == SPACE_REACH || map.at( to ) == SPACE_UNREACH ) )
        {
            map.set( to, STONE );
            map.set( from, SPACE_REACH );
        } else {
//            System.out.println( " here is a bug " );
            return;
        }

//        System.out.println( "[move] stone@" + from + "->" + to);

        //debug
//        if ( from.equals( 3, 4 ) )
//            System.out.println( "Stop here" );

        map.update( from );
        map.from_to[0] = to.sym( from );
        map.from_to[1] = from;
    }

//    private ArrayDeque<Status> preProcess( Status charMap )
//    {
//
//        for ( int x = 0; x < maxCol; x++ )
//        {
//            for ( int y = 0; y < maxRow; y++ )
//            {
//                checkStone( new Position( x, y ), charMap );
//            }
//        }
//
//        ArrayDeque<Status> list = new ArrayDeque<>();
//
//        return list;
//    }

//    private void checkStone( Position position, Status charMap )
//    {
//        int cnt = 0;
//        Position[] around = position.around();
//        Position up = around[3];
//        Position down = around[2];
//        Position left = around[1];
//        Position right = around[0];
//        Position last;
//
//        if ( up.on( charMap ) == '3' && down.on( charMap ) == '1' )
//        {
//            cnt++;
////            last =
//        }
//    }

}
