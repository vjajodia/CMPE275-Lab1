package crimewatch.leaderelection;

import crimewatch.leaderelection.raft.Raft;
import crimewatch.leaderelection.raft.Raft.RaftNode;
import crimewatch.leaderelection.raft.RaftMessage;
import org.junit.Test;

public class RaftTest
{
    @Test
    public void testStartup() throws Exception
    {
        int numOfServers = 9;
        Raft raft = new Raft();
        for (int i = 0; i < numOfServers; i++) {
            RaftNode<RaftMessage> raftNode = new RaftNode<>(i);
            raft.addNode(raftNode);
        }
        Thread.sleep(30000);
    }
}
