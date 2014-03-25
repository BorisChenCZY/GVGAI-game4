package core.game;

import tools.Vector2d;

/**
 * Created by Diego on 19/03/14.
 */
public class Observation implements Comparable<Observation>
{
    /**
     * Type of sprite of this observation.
     */
    public int itype;

    /**
     * unique ID for this observation
     */
    public int obsID;

    /**
     * Position of the observation.
     */
    public Vector2d position;

    /**
     * Reference to the position used for comparing this
     * observation with others.
     */
    public Vector2d reference;

    /**
     * Distance from this observation to the reference.
     */
    public double sqDist;

    /**
     * New observation. It is the observation of a sprite, recording its ID and position.
     * @param itype type of the sprite of this observation
     * @param id ID of the observation.
     * @param pos position of the sprite.
     * @param posReference reference to compare this position to others.
     */
    public Observation(int itype, int id, Vector2d pos, Vector2d posReference)
    {
        this.itype = itype;
        this.obsID = id;
        this.position = pos;
        this.reference = posReference;
        sqDist = pos.sqDist(posReference);
    }

    /**
     * Compares this observation to others, using distances to the reference position.
     * @param o other observation.
     * @return -1 if this precedes o, 1 if same distance or o is closer to reference.
     */
    @Override
    public int compareTo(Observation o) {
        double oSqDist = o.position.sqDist(reference);
        if(sqDist < oSqDist)        return -1;
        else if(sqDist > oSqDist)   return 1;
        return 0;
    }

}