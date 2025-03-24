package cool.circuit.decorativeNPCS.obj;

public class Config {
    private boolean requirePerm = true;
    private boolean doBlacklist = true;
    private boolean NPCOwnerLock = true;

    public boolean isNPCOwnerLock() {
        return NPCOwnerLock;
    }

    public void setNPCOwnerLock(boolean NPCOwnerLock) {
        this.NPCOwnerLock = NPCOwnerLock;
    }

    public boolean isRequirePerm() {
        return requirePerm;
    }

    public void setRequirePerm(boolean requirePerm) {
        this.requirePerm = requirePerm;
    }

    public boolean isDoBlacklist() {
        return doBlacklist;
    }

    public void setDoBlacklist(boolean doBlacklist) {
        this.doBlacklist = doBlacklist;
    }
}
