package deliberative;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import logist.plan.Action;
import logist.plan.Plan;

public class BFS implements Algorithm {

	@Override
	public Plan plan(State initState) {
		Queue<State> queue = new LinkedList<State>();
		
		queue.add(initState);
		
		State goalState = initState;
		while(!goalState.isTerminal()) {
			for (State s: goalState.createChildren()) {
				queue.add(s);
			}
			goalState = queue.poll();
		}		

		return goalState.getPlan();
	}

}
