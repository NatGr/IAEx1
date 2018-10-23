package deliberative;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import logist.plan.Plan;

public class BFS implements Algorithm {

	@Override
	public Plan plan(State initState) {
		
		/*
		 * Datastructure to iterate over states in Breadth First Order
		 * */
		Queue<State> queue = new LinkedList<State>();
		
		/*
		 * Datastructure to see if we have handled a duplicate state before with lower cost.
		 * If so, it doesn't make sense to add it to the queue.
		 * */
		Map<State, Double> seenStates = new HashMap<State, Double>();
		
		/*
		 * Keep track of bestFinalState seen yet. In this application, all final nodes of the BFS algorithm should be on the same depth (see report)
		 * */
		State bestFinalState = null;
		
		State state = initState;
		// BFS
		while(state != null) {
			if (state.isTerminal()) {
				if (bestFinalState == null || state.cost < bestFinalState.cost) {
					bestFinalState = state;
				}
			} else {
				for (State s: state.createChildren(false)) {
					Double prevScore = seenStates.get(s);  // will get a state that is equal in term of city, 
					// availableTasks and pickedUpTasks but not necessarily cost
					if (prevScore == null) {
						seenStates.put(s, s.cost);
						queue.add(s);
					} else if (s.cost < prevScore) {
						seenStates.put(s, s.cost);
						queue.remove(s); // removes the state equivalent to s but with a lower score
						queue.add(s);
					}
				}
			}
			state = queue.poll();
		}
		if (bestFinalState != null) {
			return bestFinalState.getPlan();
		} else { // should not happen
			return null;
		}
	}

}
