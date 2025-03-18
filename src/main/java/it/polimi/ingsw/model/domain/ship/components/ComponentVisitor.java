package it.polimi.ingsw.model.domain.ship.components;

public interface ComponentVisitor {
    public void visitBattery(Battery battery, int quantity);
    public void visitCabin(Cabin cabin);
    public void visitCargoHold(CargoHold cargoHold);
    public void visitEngine(Engine engine);
    // public void visitLifeSupportSystem(LifeSupportSystem lifeSupportSystem);
    public void visitShield(Shield shield);
    // public void visitStructuralModule(StructuralModule structuralModule);
}