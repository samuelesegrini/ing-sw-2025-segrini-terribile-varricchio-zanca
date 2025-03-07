package it.polimi.ingsw.model.ship;

public class Shield extends Component{
    public Set<Direction> getProtectedDirections(){};
    public boolean protectsFromDirection(Direction){};
}
